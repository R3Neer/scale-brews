# S01 — Frontera pública y backend explícito

Estado: **CERRADO de nuevo tras reapertura adversarial**. El estado global de G1 vive en `ENTITY_COLLISIONS_PLAN.md`.

## 1. Scope

S01 pertenece a G1 y tiene una sola tesis: `collision.api` no conoce implementación interna; una implementación Scale-owned queda detrás de un backend explícito y la ausencia de backend falla cerrada.

Quedan fuera binding/policy canónico, registries de engines, codecs versionados, migración de planos legacy y body adapters. También quedan fuera G2+: división de `AnatomyMovement`, pipeline Q2, catálogo/lifecycle final y prediction/reconciliation.

Contribuye directamente a FR-001..004 y FR-072..074, y a NFR-021, NFR-025 y NFR-034.

## 2. Estado inicial e invariantes

S00 clasificó `AnatomyApi` como REWORK porque importaba owners físicos internos. `AnatomySession` conservaba DISABLED/BINDING/READY y `AnatomyMovement` seguía siendo el owner temporal de contacto/query/carry hasta G2.

Invariantes: `BINDING` fail-closed, owner físico único Scale, backend sin estado físico propio, semántica pública de `GravityFrame`/`AnatomyMode` estable y motor legacy no retirado antes de G5.

## 3. Plan de implementación

- [x] I1 Introducir un contrato backend mínimo para las operaciones de `AnatomyApi`.
- [x] I2 Proporcionar backend inerte fail-closed cuando el runtime Scale no esté disponible.
- [x] I3 Implementar un único backend Scale-owned que delegue en owners existentes sin duplicar estado.
- [x] I4 Descubrir el backend mediante Java services y rechazar cardinalidad distinta de uno.
- [x] I5 Reescribir `AnatomyApi` sin dependencias de `collision.internal`.
- [x] I6 Mantener el contrato backend fuera de la superficie pública de consumer.
- [x] I7 Eliminar cualquier ciclo de alto nivel `collision.api ↔ collision.runtime`.
- [x] I8 Ejecutar holdouts, suite servidor y cliente real tras la reparación common/API.

Las revisiones descartaron un `installBackend` público y evitaron adelantar responsabilidades de G2/G3. El último cambio de plan fue puramente correctivo: el backend runtime debía usar tipos neutrales y dejar la traducción a DTOs públicos en la façade.

## 4. Modelo adversarial

La base cubre backend ausente/único/duplicado, DISABLED/BINDING/READY, gravity adapter y ausencia de imports `collision.internal` en la façade.

La campaña adversarial amplió dos fronteras:

1. el contrato backend no puede ser API pública de consumer;
2. moverlo a `collision.runtime` tampoco basta si `api` y `runtime` se referencian mutuamente.

El holdout `S01PublicApiBoundaryTests.publicApiAndRuntimeDoNotFormALayerCycle` inspecciona las firmas de `AnatomyApi`, localiza tipos `collision.runtime` alcanzados desde la API y exige que ninguno dependa a su vez de tipos `collision.api`.

## 5. Implementación final

- [x] `AnatomyApi` conserva la façade pública mínima y el ServiceLoader.
- [x] `collision.runtime.AnatomyBackend` es wiring Scale-owned, no API pública de consumer.
- [x] `AnatomyBackend` ya no referencia `AnatomyMode`, `GravityFrame`, `SurfaceContact` ni `AnatomyApi.RayHit`.
- [x] el contrato runtime usa únicamente tipos neutrales/JDK/Minecraft más `Mode`, `ContactData` y `RayHit` internos al propio contrato.
- [x] `AnatomyApi` convierte explícitamente esos datos neutrales a los DTOs públicos y viceversa en el borde.
- [x] `ScaleAnatomyBackend` adapta owners internos al contrato neutral sin adquirir estado físico propio.
- [x] el descriptor ServiceLoader continúa apuntando a una única implementación Scale-owned.

La reparación definitiva es `a09c881a6a526bb735fe9e5f9f4a26d74325e615` (`fix(collision): break api runtime layer cycle`). La dirección de dependencias queda `collision.api → collision.runtime`; `collision.runtime` ya no depende de `collision.api`.

## 6. Test matrix vigente

| Ataque / propiedad | Nivel | Resultado |
| --- | --- | --- |
| backend ausente falla cerrado | GameTest | PASS |
| provider único / duplicado | GameTest | PASS |
| façade pública sin `collision.internal` | reflection/GameTest | PASS |
| backend contract no es API pública de consumer | adversarial reflection/GameTest | PASS |
| `collision.api` ↔ `collision.runtime` sin ciclo | adversarial reflection/GameTest | **PASS** |
| holdout S03 constructor→codec en límites máximos | adversarial GameTest | PASS |
| suite servidor completa | `./gradlew build` | **268/268 PASS** |
| client/integrated/dedicated | `xvfb-run -a ./gradlew runClientGameTest` | **PASS** |

## 7. Red-before-green y reparación

El candidato inicial de S01 pasó builds en los que sus GameTests todavía no estaban registrados; esa evidencia se conserva sólo como build/regresión.

La campaña adversarial sobre `19626950569110963cd47611b245234c650f2ca0` encontró primero que `AnatomyBackend` seguía dentro de `collision.api`. `3ba014dc2e19b6b700a707bd3177c55ac443328f` lo movió a `collision.runtime`, reparando aquella fuga de superficie.

Una pasada posterior añadió el holdout de ciclo. `94f2db96277f02c9e9d4903bdb02046b7878f4c2`, run `34595220211`, job `103249293688`, ejecutó 267 GameTests: 266 pasaron y falló únicamente el ciclo de NFR-025. `be41003bbceaecd27a36dc1d13e4bef0f8bde114` añadió además el holdout S03 de round-trip máximo; run `34595505502`, job `103250215902`, ejecutó 268 tests y volvió a fallar sólo S01.

`a09c881a6a526bb735fe9e5f9f4a26d74325e615` sustituyó las referencias runtime→API por DTOs neutrales del backend. GitHub Actions run `34600301662`, job `103265686046`, ejecutó **268/268 required GameTests passed** y terminó `BUILD SUCCESSFUL`. Artifact `10264495705`, SHA-256 `8676c2ddc0099ac0a34d647c426539b88b323fee53d9ccab552e34630f9534b4`.

La prueba cliente se ejecutó sobre código de producción/test idéntico mediante el workflow temporal del commit `5d725ae700c812f7864a22dc0459b9617f52e822`. Run `34600577409`, job `103266578934`, terminó `BUILD SUCCESSFUL`: export/pose original de cow y player wide/slim, 640 comparaciones vanilla-family adicionales, `S00_CLIENT_RECEIPT_AUTHORITY PASS`, `S00_OBSERVER_AUTHORITY PASS` y dedicated a **0/100/200 ms RTT, 120 ticks cada uno**. El workflow temporal se retiró en `1c591578f2fac8406bd69fa81dd08000b19b5b7e` sin cambiar producción ni tests.

## 8. Revisión final

La revisión posterior a la prueba cliente verificó:

- el holdout adversarial no fue modificado ni relajado;
- `AnatomyBackend` no contiene referencias a `collision.api`;
- la façade sigue traduciendo a sus DTOs públicos sin exponer implementación;
- la semántica fail-closed y el único owner físico no cambian;
- ningún código G2 fue adelantado durante la reparación;
- el workflow temporal fue retirado.

No apareció otro cambio de implementación para S01. **S01 converge y queda cerrado de nuevo.**

## 9. Cierre

- [x] implementación original del scope;
- [x] tests S01 realmente registrados;
- [x] campañas adversariales integradas;
- [x] backend retirado de `collision.api`;
- [x] ciclo `collision.api ↔ collision.runtime` eliminado;
- [x] suite servidor final 268/268;
- [x] client/integrated/dedicated repetido tras la reparación common/API;
- [x] workflow temporal retirado;
- [x] revisión final posterior sin cambios;
- [x] S01 cerrado de nuevo.

La evidencia exacta vive en `VALIDATION.md`.