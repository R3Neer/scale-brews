# S02 — SPI y registries de engines reutilizables

Estado: **implementación y CI real verificadas; cierre formal pendiente de la campaña adversarial paralela**.

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

## 4. Modelo adversarial base

Se reservaron id desconocido, registro duplicado, parámetros geometry inválidos/oversize, pose inválida y quaternion root degenerado. La propiedad metamórfica fue que registrar una engine no puede crear por sí solo un binding de especie ni alterar física existente. El holdout base exigió recuperar una fixture sólo por su id exacto y mantener unresolved un id ausente.

La campaña adversarial ampliada está delegada al agente paralelo del propietario y puede reabrir S02.

## 5. Implementación

- [x] `collision.api.spi.GeometryEngine`;
- [x] `collision.api.spi.PoseEngine`;
- [x] `collision.api.spi.RootTransformProvider`;
- [x] `collision.api.CollisionEngines`;
- [x] `S02EngineRegistryTests`.

No se modificó `AnatomyMovement`, `WorldAnatomyCatalog` ni la ruta física legacy.

## 6. Test matrix

| Propiedad | Nivel | Resultado ejecutado |
| --- | --- | --- |
| fixture registra 3 familias por API | GameTest | PASS en suite G1 registrada |
| id desconocido no tiene fallback | GameTest | PASS |
| duplicate id rechazado | GameTest | PASS |
| DTOs inválidos rechazados | GameTest | PASS |
| suite previa | GitHub Actions `./gradlew build` | PASS |

## 7. Fallos/bucles

El candidato inicial `a5bb8cc6622d73ec9c4a1cc34bc0b84e1743ada3` pasó run `34583088612`, pero `S02EngineRegistryTests` aún no estaba registrado en el test mod. Ese verde se conserva como evidencia de build, no de las aserciones S02.

Tras corregir el manifest, run `34584717556` intento 2/job `103216390687` ejecutó **256/256** GameTests. El snapshot G1 posterior `1a19ec31cdaeb8c0d98cc86b327adb1661e09632` repitió **256/256** en run `34585112975`, job `103217403352`.

## 8. Revisión final

La revisión de implementación recorrió límites API, mutabilidad de DTO, duplicate semantics, ausencia de mods externos, separación root/joints/gravity y exclusiones G2/G3. No produjo cambios específicos de S02. Debe repetirse si la campaña adversarial cambia el árbol.

## 9. Cierre

- [x] implementación del scope;
- [x] CI con tests S02 realmente ejecutados;
- [ ] campaña adversarial paralela integrada o sin fallos pendientes;
- [ ] revisión final posterior a esa campaña;
- [ ] S02 cerrado formalmente.

Candidato lógico inicial: `a5bb8cc6622d73ec9c4a1cc34bc0b84e1743ada3`.

Evidencia vigente: `1a19ec31cdaeb8c0d98cc86b327adb1661e09632`, run `34585112975`, job `103217403352`, **256/256 required GameTests passed**.
