# S06 — Identidad causal de intervalos materiales

Estado: **REABIERTO por membresía espacial causal same-tick**. Segundo sprint de G2. La identidad/replay/binding de intervalos está reparada y revalidada, pero el broadphase live todavía puede reutilizar un índice que omitió a un soporte `UNAVAILABLE` aunque ese mismo provider pase a `AVAILABLE` dentro del mismo authority tick.

## 1. Tesis y scope

S06 garantiza que **cada cambio material aceptado pertenece a una historia causal monotónica y que los consumidores de esa historia no pueden confundir replay, gap, unavailable, rebind ni varias contribuciones dentro del mismo tick**.

Requisitos primarios: FR-049, FR-050, FR-051 y FR-055. Requisitos de apoyo: NFR-001, NFR-002, NFR-004, NFR-007, NFR-015, NFR-017 y NFR-018.

La resolución física pertenece a S07. La membresía del collider actual sí forma parte de la frontera causal: un consumer no puede tratar una ausencia antigua como si siguiera siendo el estado vigente.

## 2. Invariantes

- `materialSerial` es monotónico dentro de una `GeometryIdentity` y cambia sólo con una contribución causal real.
- Replay exacto no ejecuta trabajo físico de nuevo.
- Un mismo `frameSerial` no puede representar dos payloads distintos.
- Gap/out-of-order no se rellena inventando historia.
- `MotionSnapshot` permite varias contribuciones reales dentro del mismo tick, con tiempo no decreciente.
- Support, dimensión, UUID, entity id, epoch, revision, model, pose provider, binding generation y local registration generation forman parte de la certificación activa cuando corresponden.
- `UNAVAILABLE`, gravity discontinuity, teleport, dimension change, rebind/revision/epoch change y rewind son barreras, no intervalos continuos.
- Staging y cualquier historia causal tienen bounds explícitos y saturación distinguible.
- Un soporte registrado que cambia de `UNAVAILABLE` a `AVAILABLE` en el mismo tick debe entrar en las consultas espaciales actuales aunque no haya una llamada previa a `queryFrame` u otro observador que casualmente invalide el caché.
- No se introduce un escaneo global por query para resolver lo anterior.

## 3. Estado de implementación

- [x] I1 `MaterialIntervalTracker` distingue `ADVANCED`, `UNCHANGED`, `REPLAY`, `GAP_OR_STALE` y `DISCONTINUITY`.
- [x] I2 Reuse conflictivo de `frameSerial` falla cerrado.
- [x] I3 `MotionSnapshot` admite same-tick y rechaza rewind/non-finite.
- [x] I4 `GeometryProvider.interval(entity, handle)` es fail-closed por defecto.
- [x] I5 `ModelGeometryProvider.interval` deriva sólo de `before/after`, nunca de live state posterior.
- [x] I6 Root capture/commit conserva el `QueryFrame before` real.
- [x] I7 Joint cadence conserva múltiples contribuciones deterministas del mismo tick.
- [x] I8 `Pending` valida support ↔ handle y el runtime valida binding activo completo.
- [x] I9 Provenance ROOT/JOINT viaja por la cola canónica one-shot.
- [x] I10 `MaterialIntervalRuntime.MAX_PENDING_INTERVALS=512`; saturación sticky/observable y fail-closed.
- [x] I11 Holdouts de replay, payload conflictivo, wrong-support, stale binding, gap y overflow permanecen cubiertos.
- [ ] I12 La cache `SpatialIndex` debe representar también cambios de **membresía** de providers omitidos.
- [ ] I13 Repetir suite ordinaria + lane preparada y hacer revisión cero-cambios antes de re-cerrar S06.

## 4. Historia adversarial ya cerrada

El cierre provisional inicial fue reabierto por holdouts que demostraron:

- payload diferente reutilizando `frameSerial`;
- handle de una entidad certificado usando otra;
- `Pending` con support/handle incongruentes;
- stale binding de la misma entidad física con otra generación/epoch/model/pose/revision;
- gap adelantado tratado de forma demasiado permisiva;
- staging potencialmente ilimitado.

Las reparaciones posteriores endurecieron serial/payload, identidad completa del binding, replay fences y staging. La lane preparada dejó de fallar por esos ejes. No son blockers actuales salvo nueva evidencia.

## 5. Blocker vigente — `UNAVAILABLE → AVAILABLE` sin observador previo

`S06SpatialMembershipCausalityTests.sameTickUnavailableToAvailableSupportMustEnterBroadphaseWithoutRebind` usa una frontera causal real:

1. un soporte se registra con `GeometryIdentityDescriptor`;
2. el provider publica `frameSerial=1`, authority tick T, `UNAVAILABLE`, sin snapshot;
3. `AnatomyMovement.spaceClear(...)` materializa un índice que legítimamente omite ese soporte;
4. sin rebind ni cambio de tick, el provider pasa a `frameSerial=2`, `AVAILABLE`, con snapshot convexo;
5. **la segunda `spaceClear(...)` es el primer consumidor runtime tras el cambio**;
6. debe descubrir el collider nuevo.

### Hotfix parcial `debc74bf…`

`debc74bf787a0d692811219c68a953f44b786100` invalida `SPATIAL` cuando `serverEndpoint`/`acceptEndpoint` aceptan un endpoint causal nuevo. Esa reparación es válida para transiciones que ya han sido observadas por la frontera causal.

El primer holdout parecía quedar verde porque llamaba a `AnatomyMovement.queryFrame(support)` antes de la segunda `spaceClear`. Esa llamada aceptaba el endpoint nuevo y, por tanto, disparaba exactamente la invalidación añadida por el hotfix. El test estaba ayudando accidentalmente a la implementación.

`82a519d5072db0ec506bdefe5aae92c3e0396ad7` endureció el holdout: después de cambiar el provider a `AVAILABLE`, sólo inspecciona directamente el provider para demostrar la precondición; **no llama a `AnatomyMovement.queryFrame` antes de `spaceClear`**.

### Evidencia definitiva actual

GitHub Actions run **`34624485241`**, job **`103346141995`**, sobre `82a519d5…`:

- **306 GameTests ejecutados**;
- 304 pasan;
- falla exactamente el holdout S06 y un holdout S07 independiente de budget uncertainty;
- el fallo S06 es:
  `A support that becomes AVAILABLE in the same tick must enter the broadphase when spaceClear is the first runtime consumer; an index that omitted it cannot be reused vacuously`;
- las precondiciones del provider `AVAILABLE` + snapshot presente pasan;
- no hay rebind ni cambio de tick.

### Causa estructural

`AnatomyMovement.spatial(level)` valida la cache recorriendo únicamente `current.frames().entrySet()`. Un support omitido cuando estaba `UNAVAILABLE` no está en ese mapa, así que `allMatch(...)` puede ser verdadero de forma vacua. La query devuelve el índice viejo sin consultar a ese provider y, por tanto, nunca llega a `acceptEndpoint`, donde `debc74bf…` habría invalidado la cache.

Clasificación: **bug causal S06/S05-live integration**, no defecto del kernel `MaterialBroadphase` de S05.

## 6. Criterio de reparación aceptable

La reparación debe garantizar al menos una de estas propiedades:

- la validez del `SpatialIndex` incluye una representación acotada de la **membresía completa** de providers/bindings potencialmente indexables, incluidos los omitidos; o
- existe un mecanismo causal que invalida el índice cuando la disponibilidad/membresía cambia **antes** de que una query pueda reutilizar la cache, sin requerir que un observador haya llamado primero a `queryFrame`.

No son aceptables:

- `level.getAllEntities()` o un escaneo global por movimiento/query;
- reconstruir siempre todo el broadphase sin bound/razón causal;
- convertir `UNAVAILABLE` en collider stale;
- depender del orden incidental de observers;
- eliminar el holdout o volver a introducir la llamada previa a `queryFrame`.

Debe conservar identity/weak semantics, orden determinista, budgets de S05 y fail-closed en uncertainty.

## 7. Relación con S07

S07 no puede cerrarse mientras este falso negativo exista: un solver CCD correcto no sirve si el collider nunca entra en candidatos. El blocker S07 de budget uncertainty se documenta por separado en `S07-live-material-dispatcher-ccd.md`.

La lane preparada posterior a los fixes causales está verde: workflow run **`34624096154`**, job **`103344876628`**, con export cliente y servidor preparado completos. Eso no sustituye al holdout same-tick específico, que sigue rojo.

## 8. Reconciliación obligatoria con `main`

Antes de cerrar G2 debe reconciliarse `chatgpt-editing` con `main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284`.

`io.github.r3neer.scalebrews.integration.gravity.GravityFrames` debe quedar como única autoridad transversal de Scale para la gravedad efectiva. `collision.internal.GravityFrames` no puede seguir como segunda autoridad; debe eliminarse o quedar como delegación sin ownership propio. `collision.api.GravityFrame` conserva el tipo de frame de Entity Collisions y `RootTransformProvider` sigue separado conceptualmente de body gravity. Scale debe cargar sin Gravity Changer, no se crea API Tiny-Mount-específica y G7/Clinging no se adelanta.

**S06 permanece REABIERTO por el falso negativo de membresía espacial same-tick.**
