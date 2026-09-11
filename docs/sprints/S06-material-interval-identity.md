# S06 — Identidad causal de intervalos materiales

Estado: **REABIERTO por evidencia adversarial de membresía espacial causal**. Segundo sprint de G2. La identidad/replay de intervalos que motivó la reapertura anterior ha sido reparada y revalidada, incluido stale binding en la lane preparada. Sin embargo, un holdout posterior demuestra que un soporte puede avanzar de `UNAVAILABLE` a `AVAILABLE` dentro del mismo authority tick y quedar fuera del broadphase por reutilización de un índice que lo había omitido.

## 1. Tesis y scope

S06 tiene una tesis única: **cada cambio material aceptado de un soporte debe producir un estado causal monotónico del binding, y todas las fronteras que consumen ese estado deben distinguir correctamente avance, replay, gap/stale, unavailable y varias contribuciones dentro del mismo tick**.

Requisitos primarios: FR-049, FR-050, FR-051 y FR-055. Requisitos de apoyo: NFR-001, NFR-002, NFR-004, NFR-007, NFR-015, NFR-017 y NFR-018.

La resolución física de cuerpos contra intervalos pertenece a S07. S06 sí es responsable de que availability/identity/frame serial no puedan dejar a consumidores posteriores trabajando con una membresía causal vieja o incompleta.

## 2. Invariantes

- `materialSerial` es monotónico dentro de una `GeometryIdentity`/binding y se reinicia sólo cuando cambia esa identidad causal.
- Un intervalo válido conecta exactamente el frame material actualmente aceptado con un frame posterior del mismo identity; nunca reconstruye un `before` desde estado vivo posterior.
- Misma authority tick es legal si frame serial/root/joints avanzan: el reloj de Minecraft no colapsa dos mutaciones causales distintas.
- Replay exacto no ejecuta trabajo físico de nuevo.
- Un gap/out-of-order no se rellena inventando historia; produce barrera/reanchor conservador sin publicar el tramo perdido.
- Un mismo `frameSerial` no puede identificar dos payloads causales distintos.
- La entidad soporte usada para certificar o encolar un intervalo debe coincidir con `GeometryIdentity` y con el binding activo completo, no sólo UUID/revisión.
- unavailable, cambio de gravity, cambio de binding/epoch/revision, teleport/rebind y rewind son barreras, no intervalos continuos.
- Un soporte causal que cambia de `UNAVAILABLE` a `AVAILABLE` dentro del mismo tick debe aparecer inmediatamente en cualquier índice/query que represente el estado actual. La ausencia anterior no es un frame válido que pueda cachearse por omisión.
- El tracker no posee entidades ni geometría; sólo identidad/serial/fence y se limpia por lifecycle.
- La captura root del mixin sigue siendo síncrona en world thread.

## 3. Implementación y estado actual

- [x] I1 `MaterialIntervalTracker` distingue `ADVANCED`, `UNCHANGED`, `REPLAY`, `GAP_OR_STALE` y `DISCONTINUITY` con serial material monotónico.
- [x] I2 Identidad + frame serial identifican un único payload causal; reuse conflictivo falla cerrado.
- [x] I3 `MotionSnapshot` admite contribuciones same-tick con tiempo no decreciente y sigue rechazando rewinds/non-finite.
- [x] I4 `GeometryProvider.interval(entity, handle)` es fail-closed por defecto.
- [x] I5 `ModelGeometryProvider.interval` deriva sólo de `before/after` y rechaza support/identity ajenos.
- [x] I6 Root capture/commit conserva `QueryFrame before` y produce el frame posterior sin reconstrucción live.
- [x] I7 Joint cadence publica handles deterministas y conserva varias contribuciones reales del mismo tick.
- [x] I8 `Pending` valida support ↔ handle y la certificación runtime valida dimensión, UUID, entity id y binding activo completo.
- [x] I9 La cola canónica one-shot expone provenance ROOT/JOINT sin rededucir causalidad.
- [x] I10 Staging previo al dispatcher está acotado (`MAX_PENDING_INTERVALS=512`) y la saturación es explícita/fail-closed.
- [x] I11 Los holdouts anteriores de replay, payload conflictivo, wrong-support y stale binding permanecen cubiertos; la lane preparada dejó de fallar por esos ejes.
- [ ] I12 Corregir invalidación/membresía del `SpatialIndex` para que un provider registrado que era `UNAVAILABLE` y pasa a `AVAILABLE` en el mismo tick no pueda quedar omitido por reutilización vacua de `current.frames()`.
- [ ] I13 Repetir suite ordinaria + lane preparada y hacer revisión estructural cero-cambios antes de volver a cerrar S06.

## 4. Historia adversarial de identidad ya reparada

El cierre provisional histórico en `2e8aee0…` quedó invalidado por `466c666f…`, que añadió tres holdouts: payload distinto con el mismo `frameSerial`, `ModelGeometryProvider.interval(B, handleDeA)` y `Pending(B, handleDeA)`. El run `34613126182`, job `103308317180`, falló exactamente esos tres casos.

La reparación posterior endureció serial/payload, support identity y pending identity. Una campaña adicional encontró stale binding con la misma entidad física: dimensión, UUID y entity id seguían coincidiendo pero `bindingGeneration`/epoch/model/pose/revision pertenecían a otra realidad causal. La lane preparada reprodujo el fallo y la implementación pasó a validar la identidad activa completa. Los reruns posteriores dejaron de fallar por stale binding.

También se cerraron dos defectos secundarios de la frontera S06:

- un gap adelantado no puede convertirse silenciosamente en `ADVANCED`; sólo puede cortar continuidad y reanclar de forma conservadora;
- la staging queue ya no es ilimitada: tiene bound explícito, saturación observable y no reutiliza seriales invalidados.

Estos fallos son historia cerrada, no blockers actuales.

## 5. Nueva reapertura — membresía espacial same-tick

Holdout adversarial añadido en:

- `335b148312fab6937fbe06ac3153257405b4b0fb` — `S06SpatialMembershipCausalityTests`;
- `2d4a73ae462e176e5bc6c9ee3c9b0acb38c2de06` — registro del test.

El escenario usa una **frontera causal real**, no un provider legacy de conveniencia:

1. se registra un soporte con `GeometryIdentityDescriptor` y un provider que publica `frameSerial=1`, mismo authority tick, `UNAVAILABLE`, sin snapshot;
2. `AnatomyMovement.spaceClear(...)` materializa el spatial index mientras ese soporte está legítimamente ausente;
3. sin rebind y sin avanzar el tick, el mismo provider publica `frameSerial=2`, `AVAILABLE`, con snapshot convexo válido;
4. `AnatomyMovement.queryFrame(support)` está presente, demostrando que la transición causal sí fue aceptada;
5. una segunda `spaceClear(...)` debería ver el collider nuevo, pero reutiliza el índice que lo omitió y devuelve mundo libre.

Evidencia: GitHub Actions run **`34622750287`**, job **`103340432608`**, sobre `2d4a73ae…`:

- **304 GameTests ejecutados**;
- el test falla exactamente en la segunda query con: `A support that becomes AVAILABLE in the same tick must enter the broadphase immediately; an index that omitted it cannot be reused vacuously`;
- la precondición `queryFrame(support).isPresent()` pasa antes del fallo;
- no hay rebind, cambio de tick ni incertidumbre de fixture que expliquen la omisión.

La causa estructural visible es que `AnatomyMovement.spatial(level)` valida sólo `current.frames().entrySet()`. Un soporte que no estaba en `frames` cuando era unavailable no participa en `allMatch`, por lo que el índice vacío/incompleto puede considerarse vigente aunque la membresía actual haya cambiado.

Clasificación: **bug causal de frontera S06/S05**, con impacto físico S07. No invalida el kernel acotado de S05, pero sí invalida la suposición de que el índice live representa siempre el conjunto causal actual.

## 6. Criterio de reparación aceptable

No basta con limpiar `SPATIAL` desde un único callsite oportunista. La reparación debe hacer verdadera una de estas propiedades equivalentes para todos los caminos causales soportados:

- el cache key/validation del índice representa también la **membresía completa** de providers/bindings actuales, incluidos soportes antes omitidos; o
- toda transición que pueda cambiar si un soporte pertenece al índice invalida el índice de forma garantizada antes de una query posterior.

Debe conservar:

- weak/identity semantics de entidad;
- ausencia de escaneo global por query;
- determinismo del orden;
- fail-closed ante unavailable/quarantine/budget;
- mismo-tick root/joint/availability semantics.

El holdout `S06SpatialMembershipCausalityTests` debe permanecer intacto tras la reparación.

## 7. Relación con S07

S07 puede seguir implementándose en paralelo, pero **no puede cerrarse** mientras el broadphase live pueda omitir un collider causalmente disponible. La resolución CCD más exquisita del mundo sirve de decoración si el candidato nunca entra en la consulta.

Los blockers físicos actuales de S07 se documentan en `S07-live-material-dispatcher-ccd.md`; no deben mezclarse con este defecto de identidad/membresía.

## 8. Reconciliación obligatoria antes de cerrar G2

`main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` contiene el workstream de gravedad de Tiny Mounts. G2 no podrá considerarse cerrado hasta reconciliar `chatgpt-editing` con ese main y revalidar server+client.

La reconciliación debe dejar `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como única autoridad transversal Scale para leer gravedad efectiva. `collision.internal.GravityFrames` deberá desaparecer como autoridad duplicada o quedar únicamente como delegación transitoria sin ownership propio. `collision.api.GravityFrame` conserva el tipo/contrato de frame de Entity Collisions; `RootTransformProvider` conserva la separación conceptual entre root transform y body gravity. No se introducirá ninguna API Tiny-Mount-específica y G7/Clinging no se adelanta.

**S06 permanece reabierto exclusivamente por la membresía espacial causal same-tick demostrada. Los defectos anteriores de identidad/replay/binding se consideran reparados hasta que nueva evidencia los contradiga.**
