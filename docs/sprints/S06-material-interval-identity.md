# S06 — Identidad causal de intervalos materiales

Estado: **CERRADO respecto a su tesis causal/identity; última reapertura retirada tras revisión contractual**. Segundo sprint de G2. Los defectos demostrados de serial/replay, wrong-support, stale binding, gap y staging están reparados y cubiertos. El problema live de coste del broadphase pertenece a S05/NFR-008; la incertidumbre física por budget pertenece a S07/NFR-004.

## 1. Tesis y scope

S06 garantiza que cada cambio material aceptado pertenece a una historia causal monotónica y que los consumidores distinguen correctamente avance, replay, gap/stale, unavailable, rebind y varias contribuciones dentro del mismo tick.

Requisitos primarios: FR-049, FR-050, FR-051 y FR-055. Requisitos de apoyo: NFR-001, NFR-002, NFR-004, NFR-007, NFR-015, NFR-017 y NFR-018.

La resolución física pertenece a S07. La complejidad del índice live pertenece a S05.

## 2. Invariantes cerrados

- `materialSerial` es monotónico dentro de una `GeometryIdentity` y sólo avanza con una contribución causal real.
- Replay exacto no ejecuta trabajo físico de nuevo.
- Un mismo `frameSerial` no puede representar dos payloads distintos.
- Gap/out-of-order no se rellena inventando historia.
- `MotionSnapshot` permite varias contribuciones reales dentro del mismo authority tick y rechaza rewinds/non-finite.
- Support, dimensión, UUID, entity id, epoch, revision, model, pose provider, binding generation y local registration generation forman parte de la certificación activa cuando corresponden.
- `UNAVAILABLE`, cambio de gravity, teleport, dimension change, rebind/revision/epoch change y rewind son barreras, no intervalos continuos.
- `GeometryProvider.interval(entity, handle)` es fail-closed por defecto y `ModelGeometryProvider.interval` deriva sólo del `before/after` certificado.
- Root capture conserva el `QueryFrame before` real; joint cadence conserva contribuciones same-tick deterministas.
- Staging previo al dispatcher está acotado (`MAX_PENDING_INTERVALS=512`) y la saturación es explícita/fail-closed.

## 3. Historia adversarial demostrada y reparada

La reapertura anterior encontró, mediante holdouts rojos independientes:

- payload distinto reutilizando `frameSerial`;
- handle de una entidad certificado usando otra;
- `Pending` con support/handle incongruentes;
- stale binding de la misma entidad física con otra generación/epoch/model/pose/revision;
- gap adelantado tratado de forma demasiado permisiva;
- staging potencialmente ilimitado.

Las reparaciones posteriores endurecieron identidad, replay fences y límites. La lane preparada dejó de fallar por esos ejes y los holdouts permanecen en la suite.

## 4. Holdout retirado — mutación oculta del provider entre queries

`S06SpatialMembershipCausalityTests` se añadió para exigir que una segunda `spaceClear(...)` detectase un provider registrado que pasaba de `UNAVAILABLE` a `AVAILABLE` dentro del mismo tick **sin `GeometryProvider.tick`, sin rebind, sin packet-binding y sin ningún callback al runtime**.

El test produjo rojos reproducibles, pero una revisión posterior del contrato demostró que estaba exigiendo una transición que los providers productivos no pueden realizar de esa forma:

- `GeometryProvider` es internal, no SPI pública;
- providers server-side avanzan por `GeometryProvider.tick(...)`/root hooks conocidos, y `AnatomyMovement.tick` reconstruye el índice tras el avance;
- el `ClientGeometryProvider` se alimenta de frames de red aceptados; `UNAVAILABLE` descarta material/provider y un `AVAILABLE` posterior vuelve a enlazarse mediante `bindPhysics` antes de `tickGeometry`;
- register/rebind/lifecycle/endpoint acceptance ya son puntos causales conocidos de invalidación.

Descubrir una mutación interna clandestina sin ningún hook y, simultáneamente, cumplir NFR-008 exigiría sondear globalmente todos los providers en cada query. Eso no es un requisito del sistema sino telepatía con sintaxis Java.

Por tanto:

- el holdout se **retira como no contractual**;
- se eliminó `S06SpatialMembershipCausalityTests` y su registro en GameTests;
- los runs donde falló se conservan como historia de un test descartado, no como evidencia de defecto productivo;
- no se usa para reabrir S06 ni como prerequisite de S07.

## 5. Problema espacial real separado

La auditoría sí encontró un defecto contractual distinto: una segunda query local podía volver a muestrear todos los providers lejanos al validar `SpatialIndex`. `S05LiveBroadphaseLocalityTests.repeatedLocalQueryDoesNotResampleFarWorldSupports` observó **64 samples lejanos** en una consulta local estable, violando NFR-008.

Ese blocker se documenta y repara en **S05**, no en S06. El hotfix de producción `dceddae0e359aa743e8e59d9c706fcc77e661c6c` elimina la revalidación global por query; su aceptación corresponde al holdout S05 y a la evidencia posterior de CI.

## 6. Evidencia especial

La lane preparada posterior a los fixes causales está verde, entre otros:

- run `34624096154`, job `103344876628`;
- run `34623128548`, job `103341673751` con pair-suspension determinista.

La evidencia ordinaria final del commit candidato se registra exclusivamente en `docs/VALIDATION.md`, según `AGENTS.md`.

## 7. Reconciliación pendiente de cierre global

S06 no vuelve a abrirse por la cuestión de gravedad, pero el cierre global de G1/S04/G2 sigue condicionado a reconciliar `chatgpt-editing` con `main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` y dejar una única autoridad transversal de body gravity: `io.github.r3neer.scalebrews.integration.gravity.GravityFrames`.

`collision.api.GravityFrame` conserva su contrato y `RootTransformProvider` sigue siendo un concepto distinto de body gravity. Scale debe cargar sin Gravity Changer y no se introduce API Tiny-Mount-específica.

**S06 no tiene blocker adversarial activo en este momento.**
