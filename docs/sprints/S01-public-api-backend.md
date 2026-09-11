# S01 — Frontera pública y backend explícito

Estado: **REABIERTO por evidencia adversarial posterior**. El estado global de G1 vive en `ENTITY_COLLISIONS_PLAN.md`.

## 1. Scope

S01 pertenece a G1 y tiene una sola tesis: `collision.api` no conoce implementación interna; una implementación Scale-owned queda detrás de un backend explícito y la ausencia de backend falla cerrada.

Quedan fuera el binding/policy canónico, registries de engines, codecs versionados, migración de planos legacy y body adapters. También quedan fuera G2+: división de `AnatomyMovement`, pipeline Q2, catálogo/lifecycle final y prediction/reconciliation.

Contribuye directamente a FR-001..004 y FR-072..074, y a NFR-021, NFR-025 y NFR-034.

## 2. Estado inicial e investigación

S00 clasificó `AnatomyApi` como REWORK porque importaba `AnatomyMovement`, `AnatomySession` y `GravityFrames`. La fachada pública dependía de la implementación que debía ocultar. `AnatomySession` conservaba la semántica DISABLED/BINDING/READY y `AnatomyMovement` seguía siendo el owner temporal de contacto/query/carry hasta G2.

Invariantes conservados: `BINDING` fail-closed, owner físico único Scale, backend sin estado físico propio, semántica de `GravityFrame`/`AnatomyMode` intacta y motor legacy no retirado antes de G5.

## 3. Plan de implementación

- [x] I1 Introducir un contrato de backend mínimo con las operaciones ya expuestas por `AnatomyApi`.
- [x] I2 Proporcionar un backend inerte fail-closed cuando el runtime Scale no esté disponible.
- [x] I3 Implementar un único backend Scale-owned que delegue en owners existentes sin duplicar estado.
- [x] I4 Descubrir el backend mediante Java services y rechazar cardinalidad distinta de uno.
- [x] I5 Reescribir `AnatomyApi` sin dependencias de `collision.internal`.
- [x] I6 Añadir tests de fail-closed, unicidad y frontera pública.

P1 sustituyó un posible `installBackend` público por service discovery. P2 dejó el backend stateless. P3 descartó adelantar responsabilidades de G2/G3. La siguiente revisión no cambió el plan.

## 4. Modelo adversarial

La base cubrió backend único/ausente/duplicado, DISABLED/BINDING/READY y gravity adapter presente/ausente. El holdout inicial inspeccionó por reflexión las firmas de `AnatomyApi` para impedir fugas de `collision.internal`.

La campaña adversarial paralela amplió la frontera: no basta con que `AnatomyApi` no importe internals; el **contrato del backend tampoco puede ser API pública de consumer**.

Una pasada posterior al cierre añadió un holdout estructural de NFR-025: mover el contrato fuera de `collision.api` tampoco basta si `collision.api` y una capa de alto nivel como `collision.runtime` quedan dependiendo mutuamente.

## 5. Implementación

- [x] `AnatomyApi` como façade pública mínima.
- [x] contrato backend fail-closed fuera de la superficie pública de consumer.
- [x] `ScaleAnatomyBackend` como implementación Scale-owned única.
- [x] service descriptor correspondiente al contrato runtime.
- [x] tests S01 registrados y ejecutados.

La implementación inicial colocó `AnatomyBackend` en `collision.api`. Eso satisfacía la inversión de dependencias de `AnatomyApi`, pero no el requisito más fuerte de mantener el backend como wiring interno/runtime.

La primera reparación movió el contrato a `collision.runtime`. Una revisión posterior comprobó que `AnatomyApi` referencia ese contrato y el contrato runtime referencia a su vez tipos de `collision.api`, por lo que quedó un ciclo de capas todavía incompatible con NFR-025.

## 6. Test matrix

| Ataque / propiedad | Nivel | Resultado vigente |
| --- | --- | --- |
| backend ausente falla cerrado | GameTest | PASS histórico |
| provider único / duplicado | GameTest | PASS histórico |
| façade pública sin `collision.internal` | reflection/GameTest | PASS |
| backend contract no es API pública de consumer | adversarial reflection/GameTest | PASS tras primera reparación |
| `collision.api` ↔ `collision.runtime` sin ciclo de alto nivel | adversarial reflection/GameTest | **FAIL vigente** |
| suite servidor completa | `./gradlew build` | **267/268 PASS; 1 FAIL vigente** |
| client/integrated/dedicated | `runClientGameTest` | PASS histórico anterior al nuevo bloqueo; debe repetirse tras reparación common/API |

## 7. Fallos encontrados y bucles

El candidato inicial `f87e4e1d406fb7c58bf1df80d3b8f8cd831806dd` pasó run `34582769682`, pero S01 todavía no estaba registrado como entrypoint GameTest. Ese verde demuestra build/regresión, no sus aserciones. Tras registrar S01-S04, snapshots posteriores ejecutaron la suite real.

La campaña adversarial sobre `19626950569110963cd47611b245234c650f2ca0` produjo un fallo real en run `34592683287`, job `103241351742`: `S01PublicApiBoundaryTests.backendContractIsNotPublicConsumerApi` encontró `io.github.r3neer.scalebrews.collision.api.AnatomyBackend` público. Se clasificó como **bug de implementación**, no como defecto del test o del requisito.

`3ba014dc2e19b6b700a707bd3177c55ac443328f` movió el contrato a `collision.runtime.AnatomyBackend`, actualizó `AnatomyApi`, `ScaleAnatomyBackend` y el descriptor ServiceLoader, y eliminó el tipo `collision.api.AnatomyBackend`. Esa reparación pasó la suite disponible entonces.

La revisión adversarial posterior añadió `publicApiAndRuntimeDoNotFormALayerCycle`. El snapshot `94f2db96277f02c9e9d4903bdb02046b7878f4c2` ejecutó 267 GameTests en run `34595220211`, job `103249293688`: **266 pasaron y sólo falló** ese nuevo holdout, señalando `collision.api ↔ collision.runtime` a través de `collision.runtime.AnatomyBackend`. `be41003bbceaecd27a36dc1d13e4bef0f8bde114` añadió además el holdout S03 de límites; run `34595505502`, job `103250215902`, ejecutó 268 tests y volvió a dejar exactamente el mismo único fallo S01.

El fallo vigente se clasifica como **bug de arquitectura/implementación de la frontera S01 respecto de NFR-025**. No se relaja el test ni se adelanta G2.

## 8. Revisión final

La antigua pasada de cierre verificó façade, service loading, ownership, fail-closed, gravedad y superficie pública, pero no detectó la dependencia mutua creada al mover el backend a `collision.runtime`. El nuevo holdout invalida esa convergencia para NFR-025.

La revisión podrá cerrarse de nuevo sólo después de que una reparación elimine el ciclo, la suite completa vuelva a verde, se repita la evidencia cliente/integrated/dedicated aplicable por tratarse de una frontera common/API y una pasada completa posterior no produzca cambios.

## 9. Cierre

- [x] implementación original del scope;
- [x] tests S01 realmente registrados;
- [x] campaña adversarial inicial integrada;
- [x] backend retirado de `collision.api`;
- [ ] ciclo `collision.api ↔ collision.runtime` eliminado;
- [ ] suite servidor final verde con los holdouts actuales;
- [ ] client/integrated/dedicated repetido tras la reparación common/API;
- [ ] revisión final posterior sin cambios;
- [ ] S01 cerrado de nuevo.

La evidencia roja vigente y el estado global se registran respectivamente en `VALIDATION.md` y `ENTITY_COLLISIONS_PLAN.md`.
