# S06 — Identidad causal de intervalos materiales

Estado: **REABIERTO por evidencia adversarial posterior**. Segundo sprint de G2. La implementación original alcanzó un cierre provisional verde en `2e8aee0…`, pero el holdout independiente `466c666f…` demostró tres defectos de identidad causal. S07 puede conservar su plan de pre-implementación, pero su implementación depende de reparar y revalidar S06.

## 1. Tesis y scope

S06 tiene una tesis única: **cada cambio material aceptado de un soporte debe producir un intervalo certificado con identidad monotónica propia del binding, y esa identidad debe permitir distinguir avance, replay, gap/stale y varias contribuciones dentro del mismo tick antes de entrar al resolver físico**.

Requisitos primarios: FR-049, FR-050, FR-051 y FR-055. Requisitos de apoyo: NFR-001, NFR-002, NFR-004, NFR-007, NFR-015, NFR-017 y NFR-018.

S06 no resuelve todavía cuerpos contra el intervalo ni integra derived carry: `MaterialEventDispatcher` sigue siendo el scheduler de S07. Tampoco cierra sliding/multicontacto/separation recovery, chains o receipts de transporte.

## 2. Estado inicial

- `GeometryProvider.MotionIntervalHandle` ya unía dos `QueryFrame` bajo una `GeometryIdentity` y un `materialSerial`, pero nadie poseía todavía la asignación/replay fence de ese serial en runtime.
- `MaterialEventDispatcher.EventId` era un contador del scheduler. Reingerir el mismo `MotionIntervalHandle` creaba otro `EventId`; por sí solo no demostraba FR-050.
- `ModelGeometryProvider.motion()` conservaba un único intervalo tick→tick y `MotionSnapshot` exigía `toTick == fromTick + 1`; eso no podía representar dos root mutations materiales durante el mismo tick.
- `PlatformEntityMixin.move` ya capturaba y observaba root antes/después, pero sólo como `RootFrame`; no retenía el `QueryFrame` material anterior.
- `AnatomyMovement.tick` avanzaba providers a 20 Hz, pero no publicaba un batch de handles para las mutaciones de joints detectadas en esa cadencia.

## 3. Invariantes

- `materialSerial` es monotónico dentro de una `GeometryIdentity`/binding y se reinicia sólo cuando cambia esa identidad causal.
- Un intervalo válido conecta exactamente el frame material actualmente aceptado con un frame posterior del mismo identity; nunca reconstruye un before desde estado vivo posterior.
- Misma authority tick es legal si frame serial/root/joints avanzan: el reloj de Minecraft no colapsa dos mutaciones causales distintas.
- Replay exacto no ejecuta trabajo físico de nuevo.
- Un gap/out-of-order no se rellena inventando historia; produce `GAP_OR_STALE` y sólo puede reanclar conservadoramente sin publicar el tramo perdido.
- **Un mismo `frameSerial` no puede identificar dos payloads causales distintos.** Reutilizar el serial con root/sample/snapshot diferente debe fallar cerrado.
- **La entidad soporte usada para certificar o encolar un intervalo debe coincidir con `GeometryIdentity` en dimensión, UUID y network/entity id.** No basta con que revisión/modelo coincidan.
- unavailable, cambio de gravity, cambio de binding/epoch/revision, teleport/rebind y rewind son barreras, no intervalos continuos.
- El tracker no posee entidades ni geometría; sólo identidad/serial/fence y se limpia por lifecycle.
- La captura root del mixin sigue siendo síncrona en world thread.

## 4. Implementación y estado actual

- [x] I1 Añadir `MaterialIntervalTracker` con outcomes `ADVANCED`, `UNCHANGED`, `REPLAY`, `GAP_OR_STALE`, `DISCONTINUITY` y serial material monotónico.
- [ ] I2 Garantizar que identidad + frame serial identifican un único payload causal; el holdout adversarial demuestra que un payload distinto con el mismo serial se acepta actualmente como `UNCHANGED`.
- [x] I3 Permitir `MotionSnapshot` con `toTick == fromTick` y tiempos no decrecientes, manteniendo prohibidos rewinds/non-finite data.
- [x] I4 Añadir `GeometryProvider.interval(entity, handle)` fail-closed por defecto.
- [ ] I5 Hacer que `ModelGeometryProvider.interval` rechace handles cuya identidad no corresponda a la entidad soporte suministrada, además de derivar exclusivamente de `before/after`.
- [x] I6 Añadir captura/commit root explícita mediante `MaterialIntervalRuntime.RootCapture`, conservando el `QueryFrame` before y produciendo el intervalo después del root mutation.
- [x] I7 Observar frames post-tick de joints desde `AnatomyRuntime.publish`, ordenando soportes por UUID/id para no depender del orden de weak/identity maps.
- [ ] I8 Reforzar la frontera runtime para que un `Pending` no pueda emparejar un soporte con un handle de otra identidad.
- [x] I9 Exponer cola canónica one-shot `MaterialIntervalRuntime.poll` y `AnatomyRuntime.pollIntervals` para que S07 conecte el dispatcher sin rededucir identidad.
- [x] I10 Mantener registrados los siete holdouts originales y los tres holdouts adversariales posteriores.
- [ ] I11 Repetir suite completa y revisión estructural final después de las reparaciones; una nueva pasada cero-cambios es obligatoria antes de volver a cerrar S06.

## 5. Modelo adversarial y resultado previo

1. **same-tick double root**: frameSerial 1→2→3 en el mismo tick genera materialSerial 1 y 2.
2. **exact replay**: repetir before/after devuelve `REPLAY` y no incrementa serial.
3. **stale/out-of-order**: un before anterior al accepted no genera nuevo intervalo.
4. **gap**: saltar un frame devuelve `GAP_OR_STALE`; la reanudación no publica historia fabricada.
5. **identity reuse**: nueva binding identity inicia otro stream y no hereda replay fence.
6. **gravity discontinuity**: cambio de gravity frame no genera intervalo continuo.
7. **unavailable/lifecycle barrier**: corta continuidad; el siguiente frame válido sólo la vuelve a sembrar.
8. **root+joints same tick**: contribuciones causales distintas conservan seriales propios.
9. **joint batch permutation**: el batch debe conservar simultaneidad y orden canónico al exponerse a S07.
10. **provider live-state trap**: mover, rotar y escalar la entidad después de capturar el handle no cambia el motion certificado por `ModelGeometryProvider.interval`.
11. **rebind/teleport**: las barreras invalidan pending work que no puede cruzar lifecycle.
12. **serial overflow**: incremento usa `Math.incrementExact`; frame serial máximo no se convierte en wraparound ambiguo.

Durante la revisión previa apareció un defecto antes de la campaña final: una versión intermedia del runtime podía reanclar silenciosamente un `before` adelantado y convertir un gap en un avance. `f1d6290b598733459654b3df20808161074c78f7` lo corrigió: un gap nunca publica intervalo; como máximo reancla en el `after` como barrera conservadora. También se fijó que un serial ya asignado a pending work y luego invalidado por lifecycle no se reutiliza.

## 6. Cierre provisional histórico

Snapshot de cierre provisional: `2e8aee0fa679705673f60ecfc38f6088f3c6bfff` (`test(collision): register S06 material interval holdouts`).

GitHub Actions run `34612764313`, job `103307096858`:

- **284 tests registrados y ejecutados**;
- **284/284 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 25s`;
- artifact `10268749377`;
- artifact SHA-256 `6944ffc9c3ac9d204c6a9739dd1f3fc3d58b9b231e6ccd9d50190ad9fad975db`.

Esa evidencia sigue siendo histórica y válida para los casos que ejecutó, pero ya no demuestra el cierre actual de S06.

## 7. Reapertura adversarial posterior

Commit adversarial **`466c666f948d839e5a4d437d45674448b6d82631`** añadió tres holdouts sin modificar producción:

1. `conflictingPayloadCannotReuseFrameSerial`: mismo `GeometryIdentity` y mismo `frameSerial`, pero payload/root distinto, debe fallar cerrado en vez de convertirse en `UNCHANGED`.
2. `providerRejectsHandleBelongingToAnotherSupport`: `ModelGeometryProvider.interval(B, handleDeA)` no puede certificar el movimiento de A usando B como key/runtime support.
3. `pendingCannotPairHandleWithDifferentSupport`: la cola runtime no puede representar `Pending(B, handleDeA)`.

GitHub Actions run **`34613126182`**, job **`103308317180`** ejecutó **287 tests**:

- **284 pasaron**;
- fallaron **exactamente esos tres holdouts**;
- no hubo fallo de compilación ni regresión ajena;
- `runGameTest` terminó con los tres fallos de identidad esperados.

Clasificación: **bugs de implementación S06**, no defectos de los tests ni cambio de requisitos. La tesis de identidad causal exige que serial, soporte e identidad no puedan contradecirse silenciosamente.

## 8. Revisión adicional abierta

Además de reparar los tres fallos demostrados, la siguiente revisión S06 debe verificar antes del cierre:

- que la staging queue previa al dispatcher tenga el tratamiento acotado exigido por NFR-007 o que su límite/ownership se cierre explícitamente en la frontera S07 sin dejar una cola causal potencialmente ilimitada;
- que la promesa de “joint batch” no sea sólo un `List<Pending>` ordenado: S07 necesita distinguir simultaneidad/source sin rededucir causalidad desde estado vivo;
- que ninguna validación nueva se limite a UUID y omita dimensión, entity id o binding identity cuando esos ejes ya existen en `GeometryIdentity`;
- que los tres tests adversariales permanezcan intactos después de la reparación.

## 9. Reconciliación obligatoria antes de cerrar G2

`main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` contiene el workstream de gravedad de Tiny Mounts. G2 no podrá considerarse cerrado hasta reconciliar `chatgpt-editing` con ese main y revalidar server+client.

La reconciliación debe dejar `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como única autoridad transversal Scale para leer gravedad efectiva. `collision.internal.GravityFrames` deberá desaparecer como autoridad duplicada o quedar únicamente como delegación transitoria sin ownership propio. `collision.api.GravityFrame` conserva el tipo/contrato de frame de Entity Collisions; `RootTransformProvider` conserva la separación conceptual entre root transform y body gravity. No se introducirá ninguna API Tiny-Mount-específica y G7/Clinging no se adelanta.

**S06 permanece reabierto. La implementación de S07 queda bloqueada hasta reparar y revalidar esta frontera causal; su plan puede conservarse como trabajo preparatorio.**
