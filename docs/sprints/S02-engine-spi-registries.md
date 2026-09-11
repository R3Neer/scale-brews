# S02 — SPI y registries de engines reutilizables

Estado: **CERRADO**.

## 1. Scope

Tesis única: crear extensiones reutilizables que permitan registrar tecnologías de geometría, pose y root transform sin introducir Java específico por especie.

Incluye FR-015..018, FR-025..032 y FR-034 en su dimensión de contrato/registro; contribuye a NFR-019..022 y NFR-024/025. No implementa todavía engines ModelPart/Mojang completos, bindings de especies, catálogo final ni consumer Clinging. G2 solver/material pipeline, G3 cobertura/model extraction, G4 networking final y G5 categorías especiales quedan fuera.

## 2. Estado inicial

S00 dejó `PoseProvider`/`PoseProviders` como infraestructura útil pero no como API final de familia; no existían `GeometryEngine`, `PoseEngine` ni `RootTransformProvider` públicos registrables.

## 3. Plan y convergencia

- [x] definir `GeometryEngine` con request bounded y resultado `ModelGeometry`;
- [x] definir `PoseEngine` determinista con DTO público de inputs/channels;
- [x] definir `RootTransformProvider` separado de joints y con DTO de rotación inmutable;
- [x] crear registry público por `Identifier`, sin fallback y con duplicate rejection;
- [x] probar registro de fixture externo y ausencia de fallback por id desconocido;
- [x] probar validación/bounds y orden determinista de DTOs/registries.

P1 separó root orientation de gravity. P2 evitó hacer que `PoseEngine` heredase del legacy `PoseProvider`. P3 eliminó built-ins por especie del scope. Revisiones G1 posteriores añadieron canonical ordering y validación de referencias antes de consumo. La pasada final no cambió el contrato S02.

## 4. Modelo adversarial

Se cubrieron id desconocido, registro duplicado, parámetros geometry inválidos/oversize, pose inválida, quaternion root degenerado, orden de registros y referencias a engines ausentes. Registrar una engine no crea por sí solo binding de especie ni altera física existente.

La campaña adversarial paralela se integró completa. No produjo un fallo residual atribuible a S02 después de las reparaciones transversales de G1.

## 5. Implementación

- [x] `collision.api.spi.GeometryEngine`;
- [x] `collision.api.spi.PoseEngine`;
- [x] `collision.api.spi.RootTransformProvider`;
- [x] `collision.api.CollisionEngines`;
- [x] tests S02 registrados;
- [x] ordering determinista de snapshots;
- [x] `CollisionBindingCatalog` rechaza geometry/pose/root no registrados antes de aceptar el binding para uso.

No se modificó `AnatomyMovement`, no se implementaron engines por especie y no se adelantó G3.

## 6. Test matrix

| Propiedad | Nivel | Resultado final |
| --- | --- | --- |
| fixture registra engines por API | GameTest | PASS |
| id desconocido no tiene fallback | GameTest | PASS |
| duplicate id rechazado | GameTest | PASS |
| DTOs inválidos rechazados | GameTest | PASS |
| snapshots/requests canónicos | adversarial GameTest | PASS |
| binding con engine/provider ausente | adversarial GameTest | PASS, fail-closed |
| suite servidor completa | `./gradlew build` | PASS, 266/266 |
| client/integrated/dedicated | `runClientGameTest` | PASS |

## 7. Fallos/bucles

El candidato inicial `a5bb8cc6622d73ec9c4a1cc34bc0b84e1743ada3` pasó run `34583088612`, pero S02 aún no estaba registrado como entrypoint del test mod. Tras corregir el manifest, run `34584717556` intento 2/job `103216390687` ejecutó **256/256**. Revisiones posteriores elevaron la suite con ataques adicionales sin encontrar un defecto final específico de S02.

Durante S04 se descubrió que el catálogo aceptaba referencias a geometry/pose/root no registrados. Se clasificó como bug de integración de G1/FR-033 y `8f4a682232fe39e385f6279352430f326f982711` añadió rechazo explícito y pruebas de las tres referencias. Las campañas posteriores conservaron el comportamiento.

## 8. Revisión final

Se revisaron límites API, mutabilidad, duplicate semantics, ausencia de mods externos, separación root/joints/gravity, determinismo, engine-reference validation y exclusiones G2/G3. La campaña adversarial final no dejó fallos S02 pendientes y la pasada posterior no produjo cambios.

## 9. Cierre

- [x] implementación del scope;
- [x] tests S02 realmente registrados;
- [x] campaña adversarial integrada;
- [x] regresiones transversales reparadas;
- [x] suite final servidor verde;
- [x] client/integrated/dedicated final verde;
- [x] revisión final sin cambios;
- [x] S02 cerrado.

Evidencia final compartida de G1: implementación/test `3ba014dc2e19b6b700a707bd3177c55ac443328f`; GitHub Actions run `34593762577`, job `103244714934`, **266/266 required GameTests passed**. Client/integrated/dedicated: trigger code-identical `23a1072ef451a14f59f707019415e1f4ac60dbab`, run `34593970131`, job `103245364717`, **success**.
