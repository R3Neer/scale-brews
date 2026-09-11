# S02 — SPI y registries de engines reutilizables

Estado: **CERRADO**.

## 1. Scope

Tesis única: crear las extensiones reutilizables que permiten registrar tecnologías de geometría, pose y root transform sin introducir Java específico por especie.

Incluye FR-015..018, FR-025..032 y FR-034 en su dimensión de contrato/registro; contribuye a NFR-019..022 y NFR-024/025. No implementa todavía engines ModelPart/Mojang completos, bindings de especies, catálogo final ni consumer Clinging.

Excluye expresamente G2 solver/material pipeline, G3 cobertura/model extraction completa, G4 networking final y G5 categorías especiales.

## 2. Estado inicial

S00 dejó `PoseProvider`/`PoseProviders` como infraestructura útil pero no como API final de familia; no existían `GeometryEngine`, `PoseEngine` ni `RootTransformProvider` públicos registrables. El plan G1 exige introducir esas fronteras antes de que G3 pueda implementar familias concretas.

## 3. Plan y convergencia

- [x] definir `GeometryEngine` con request bounded y resultado `ModelGeometry`;
- [x] definir `PoseEngine` determinista con DTO público de inputs/channels;
- [x] definir `RootTransformProvider` separado de joints y con DTO de rotación inmutable;
- [x] crear registry público por `Identifier`, sin fallback y con duplicate rejection;
- [x] probar registro de fixture externo y ausencia de fallback por id desconocido;
- [x] probar validación/bounds de DTOs.

P1 separó root orientation de gravity. P2 evitó hacer que `PoseEngine` heredase del legacy `PoseProvider`. P3 eliminó cualquier built-in por especie del scope. La siguiente pasada no cambió el plan.

## 4. Modelo adversarial

Se reservaron id desconocido, registro duplicado, parámetros geometry inválidos/oversize, pose inválida y quaternion root degenerado. La propiedad metamórfica fue que registrar una engine no puede crear por sí solo un binding de especie ni alterar física existente. El holdout exigió recuperar una fixture sólo por su id exacto y mantener unresolved un id ausente.

## 5. Implementación

- [x] `collision.api.spi.GeometryEngine`;
- [x] `collision.api.spi.PoseEngine`;
- [x] `collision.api.spi.RootTransformProvider`;
- [x] `collision.api.CollisionEngines`;
- [x] `S02EngineRegistryTests`.

No se modificó `AnatomyMovement`, `WorldAnatomyCatalog` ni la ruta física legacy.

## 6. Test matrix

| Propiedad | Nivel | Resultado |
| --- | --- | --- |
| fixture registra 3 familias por API | GameTest | PASS dentro de `./gradlew build` |
| id desconocido no tiene fallback | GameTest | PASS |
| duplicate id rechazado | GameTest | PASS |
| DTOs inválidos rechazados | GameTest | PASS |
| suite previa | GitHub Actions `./gradlew build` | PASS |

La segunda pasada adversarial no encontró registro implícito, fallback ni acoplamiento por especie. Las mutaciones de duplicate-accept, missing-id fallback y DTO laxos tienen oráculos directos en la suite.

## 7. Fallos/bucles

Ninguno. El candidato pasó a la primera ejecución CI.

## 8. Revisión final

Se recorrieron límites API, mutabilidad de DTO, duplicate semantics, ausencia de mods externos, separación root/joints/gravity y exclusiones G2/G3. La pasada completa no produjo cambios.

## 9. Cierre

- [x] CI verde;
- [x] segunda pasada adversarial;
- [x] revisión completa sin cambios;
- [x] S02 cerrado.

Commit candidato: `a5bb8cc6622d73ec9c4a1cc34bc0b84e1743ada3`.

GitHub Actions run `34583088612`, job `103210955751`, finalizó **success** el 2026-09-11. El paso `build` completó `./gradlew build`; wrapper validation, Java 25 y artefactos también quedaron verdes. La evidencia se consolidará en `VALIDATION.md` al cierre de G1.
