# S01 — Frontera pública y backend explícito

Estado: **CERRADO**.

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

## 5. Implementación

- [x] `AnatomyApi` como façade pública mínima.
- [x] contrato backend fail-closed fuera de la superficie pública de consumer.
- [x] `ScaleAnatomyBackend` como implementación Scale-owned única.
- [x] service descriptor correspondiente al contrato runtime.
- [x] tests S01 registrados y ejecutados.

La implementación inicial colocó `AnatomyBackend` en `collision.api`. Eso satisfacía la inversión de dependencias de `AnatomyApi`, pero no el requisito más fuerte de mantener el backend como wiring interno/runtime.

## 6. Test matrix

| Ataque / propiedad | Nivel | Resultado final |
| --- | --- | --- |
| backend ausente falla cerrado | GameTest | PASS |
| provider único / duplicado | GameTest | PASS |
| façade pública sin `collision.internal` | reflection/GameTest | PASS |
| backend contract no es API pública de consumer | adversarial reflection/GameTest | PASS tras reparación |
| suite servidor completa | `./gradlew build` | PASS, 266/266 |
| client/integrated/dedicated | `runClientGameTest` | PASS |

## 7. Fallos encontrados y bucles

El candidato inicial `f87e4e1d406fb7c58bf1df80d3b8f8cd831806dd` pasó run `34582769682`, pero S01 todavía no estaba registrado como entrypoint GameTest. Ese verde demuestra build/regresión, no sus aserciones. Tras registrar S01-S04, snapshots posteriores ejecutaron la suite real.

La campaña adversarial final sobre `19626950569110963cd47611b245234c650f2ca0` produjo un fallo real en run `34592683287`, job `103241351742`: `S01PublicApiBoundaryTests.backendContractIsNotPublicConsumerApi` encontró `io.github.r3neer.scalebrews.collision.api.AnatomyBackend` público. Se clasificó como **bug de implementación**, no como defecto del test o del requisito.

`3ba014dc2e19b6b700a707bd3177c55ac443328f` movió el contrato a `collision.runtime.AnatomyBackend`, actualizó `AnatomyApi`, `ScaleAnatomyBackend` y el descriptor ServiceLoader, y eliminó el tipo `collision.api.AnatomyBackend`.

## 8. Revisión final

La pasada posterior a la campaña revisó façade, service loading, ownership, fail-closed, gravedad y superficie de tipos. El backend Scale queda fuera de `collision.api`; no se creó un segundo ledger/solver y no se tocaron responsabilidades Q2. La repetición completa no produjo cambios adicionales de S01.

## 9. Cierre

- [x] implementación del scope;
- [x] tests S01 realmente registrados;
- [x] campaña adversarial integrada;
- [x] fallo adversarial clasificado y reparado;
- [x] suite final servidor verde;
- [x] client/integrated/dedicated final verde;
- [x] revisión final posterior a la campaña sin cambios;
- [x] S01 cerrado.

Evidencia final compartida de G1: implementación/test `3ba014dc2e19b6b700a707bd3177c55ac443328f`; GitHub Actions run `34593762577`, job `103244714934`, **266/266 required GameTests passed**. El trigger code-identical `23a1072ef451a14f59f707019415e1f4ac60dbab` repitió build y ejecutó client/integrated/dedicated en run `34593970131`, job `103245364717`, con conclusión **success**.
