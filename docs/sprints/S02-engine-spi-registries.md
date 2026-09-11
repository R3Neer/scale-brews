# S02 — SPI y registries de engines reutilizables

Estado: **candidato de implementación; pendiente de CI**.

## 1. Scope

Tesis única: crear las extensiones reutilizables que permiten registrar tecnologías de geometría, pose y root transform sin introducir Java específico por especie.

Incluye FR-015..018, FR-025..032 y FR-034 en su dimensión de contrato/registro; contribuye a NFR-019..022 y NFR-024/025. No implementa todavía engines ModelPart/Mojang completos, bindings de especies, catálogo final ni consumer Clinging.

Excluye expresamente G2 solver/material pipeline, G3 cobertura/model extraction completa, G4 networking final y G5 categorías especiales.

## 2. Estado inicial

S00 dejó `PoseProvider`/`PoseProviders` como infraestructura útil pero no como API final de familia; no existían `GeometryEngine`, `PoseEngine` ni `RootTransformProvider` públicos registrables. El plan G1 exige introducir esas fronteras antes de que G3 pueda implementar familias concretas.

## 3. Plan

- [x] definir `GeometryEngine` con request bounded y resultado `ModelGeometry`;
- [x] definir `PoseEngine` determinista con DTO público de inputs/channels;
- [x] definir `RootTransformProvider` separado de joints y con DTO de rotación inmutable;
- [x] crear registry público por `Identifier`, sin fallback y con duplicate rejection;
- [x] probar registro de fixture externo y ausencia de fallback por id desconocido;
- [x] probar validación/bounds de DTOs.

### Revisión iterativa

P1 separó root orientation de gravity: el root provider expresa TRS del soporte y no escribe la gravedad del body.

P2 evitó hacer que `PoseEngine` heredase del legacy `PoseProvider`; eso habría convertido un adapter temporal en contrato público.

P3 eliminó cualquier built-in por especie del scope. G3 registrará engines de familia; S02 sólo crea el mecanismo. La siguiente pasada no cambió el plan.

## 4. Modelo adversarial

Ataques reservados: id desconocido, registro duplicado, request geometry con parámetros inválidos/oversize, pose con NaN o walkAmount negativo, root quaternion degenerado. Propiedad metamórfica: registrar una engine no puede crear por sí solo ningún binding de especie ni alterar física existente.

Holdout: comprobar que una engine registrada por fixture externo sólo es recuperable por su id exacto y que un id ausente queda unresolved.

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
| fixture registra 3 familias por API | GameTest | pendiente CI |
| id desconocido no tiene fallback | GameTest | pendiente CI |
| duplicate id rechazado | GameTest | pendiente CI |
| DTOs inválidos rechazados | GameTest | pendiente CI |
| suite previa | `./gradlew build` | pendiente CI |

## 7. Fallos/bucles

Pendiente de CI.

## 8. Revisión final

Pendiente después de CI.

## 9. Cierre

- [ ] CI verde;
- [ ] segunda pasada adversarial;
- [ ] revisión completa sin cambios;
- [ ] S02 cerrado.
