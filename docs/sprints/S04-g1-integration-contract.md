# S04 — Integración estable y cierre funcional de G1

Estado: **candidato de implementación; pendiente de CI reparada, campaña adversarial paralela y revisión de gate**.

## 1. Scope

Tesis única: conectar las fronteras ya estabilizadas por S01-S03 sin iniciar el solver Q2 ni sustituir el lifecycle/catálogo preparado que G3 posee explícitamente.

Incluye el resto de tareas G1: capabilities/versionado, body adapters/categorías/policy en integration, fixture externo API+JSON y extracción de la selección canónica de bindings fuera de `collision.internal`.

Excluye: división de `AnatomyMovement`, Q2, prepared geometry catalog/lifecycle, ModelPart/Mojang engines completos, prediction/reconciliation y retirada del motor legacy.

## 2. Estado inicial

Tras S03, `collision.api` ya no dependía de `internal`, existían SPIs públicos y binding/policy v1, pero `Platforms` seguía poseyendo body adapters/categorías y evaluaba ratio/friction mediante tipos legacy. Tampoco existía un índice canónico de bindings fuera de `WorldAnatomyCatalog`, cuya sustitución completa pertenece a G3.

## 3. Plan y convergencia

- [x] añadir `BodyAdapter` público y registry `CollisionAdapters` sin ownership físico;
- [x] mover clasificación/unsupported ordinary states a `collision.integration.BodyClassification`;
- [x] mover evaluación ratio/enabled/friction a `collision.integration.CollisionRules`;
- [x] convertir policy legacy a policy canónica sólo en el decoder de migración, sin importar `automatic_top`;
- [x] hacer que el motor legacy consuma temporalmente clasificación/rules nuevas para evitar dos políticas divergentes durante la migración;
- [x] crear `collision.catalog.CollisionBindingCatalog` como índice/loader de bindings canónicos; dejar prepared geometry/lifecycle para G3;
- [x] validar que geometry/pose/root referenciados por un binding canónico estén registrados, sin fallback por id desconocido;
- [x] versionar protocol/capabilities de la frontera G1;
- [x] demostrar fixture externo que registra geometry/pose/root/body por API y selecciona geometry/pose/root desde JSON;
- [x] eliminar el codec de filter duplicado de `collision.internal` delegándolo al codec canónico.

P1 rechazó reemplazar `WorldAnatomyCatalog`: el plan asigna literalmente esa sustitución a G3. P2 mantuvo `automatic_top` sólo en el motor legacy que G5 retirará, pero lo excluyó de `CollisionPolicy` y de toda descripción canónica. P3 detectó que traducir `enabled=true` legacy a un profile override reactivaría un support deshabilitado globalmente; la migración lo representa como inherit y sólo conserva `false` explícito. P4 reservó selección ambigua de variantes como fail-closed. P5, durante la revisión posterior al primer candidato, detectó que el índice canónico no verificaba los IDs de engine/provider; se añadió rechazo explícito para cerrar FR-033. La pasada siguiente no cambió el scope de G1.

## 4. Modelo adversarial

La campaña adversarial completa está delegada al agente paralelo indicado por el propietario; cualquier test nuevo que aterrice en `chatgpt-editing` se integra en el sprint y un fallo obliga a reabrir la fase correspondiente.

Invariantes ya cubiertos por la implementación/test base: adapter duplicado/categoría inválida, binding JSON con engine id externo, engine/provider ausente, selector variante ambiguo, peer protocol v2, ratio exacto y por encima, support/category disable legacy y exclusión de `automatic_surfaces` del modelo canónico.

## 5. Implementación

- [x] `collision.api.spi.BodyAdapter` + `CollisionAdapters`;
- [x] `collision.integration.BodyClassification`;
- [x] `collision.integration.CollisionRules`;
- [x] `collision.catalog.CollisionBindingCatalog`;
- [x] bridge de policy legacy en `LegacyCollisionData`;
- [x] `Platforms` delega adapters/category/ordinary/ratio/friction a las fronteras nuevas;
- [x] API protocol v3/data schema v1 y capabilities G1;
- [x] test-mod fixture + datapack JSON;
- [x] codec filter interno deduplicado con error de decode fail-closed;
- [x] referencias de geometry/pose/root validadas contra registries públicos.

## 6. Test matrix

| Propiedad | Nivel | Resultado |
| --- | --- | --- |
| fixture API + JSON | GameTest/resource | pendiente CI reparada |
| engine/pose/root inexistentes rechazados | GameTest | pendiente CI reparada |
| variant ambiguity fail-closed | GameTest | pendiente CI reparada |
| protocol/capabilities | GameTest | pendiente CI reparada |
| ratio/friction canonical | GameTest | pendiente CI reparada |
| filter inválido devuelve error de codec, no excepción fuera del parse | regresión GameTest | pendiente CI reparada |
| suite histórica | `./gradlew build` | pendiente CI reparada |

## 7. Fallos/bucles

El primer candidato `ac848a41edb2044e4f2a188623262642f6bd7909` ejecutó GitHub Actions run `34583969083`. Producción, cliente y GameTests compilaron, pero `runGameTest` terminó rojo: 242 tests ejecutados y exactamente uno falló, `anatomy_geometry_tests_explicit_piece_filters_retain_only_valid_anatomy`. La causa fue un **bug de implementación** en la deduplicación del codec de `AnatomyFilter`: el constructor lanzaba `IllegalArgumentException` durante decode en vez de convertir el dato inválido en `DataResult.error`. Se volvió a implementación y `fbd9e31ef0c78c4666f840816d167f24b5e9fb2b` restauró la semántica fail-closed mediante un DTO intermedio + `comapFlatMap`.

En la revisión del mismo ciclo se detectó además un hueco independiente: `CollisionBindingCatalog` aceptaba IDs de geometry/pose/root no registrados. Se clasificó como bug de implementación respecto de FR-033 y se añadió validación explícita más test de los tres tipos de referencia.

## 8. Revisión final

Pendiente de CI reparada y de la campaña adversarial paralela. Después se revisará G1 completo contra sus nueve tareas y se actualizarán plan/VALIDATION; no se empezará G2.

## 9. Cierre

- [ ] CI reparada verde;
- [ ] campaña adversarial paralela integrada o sin fallos pendientes;
- [ ] revisión completa G1 sin cambios;
- [ ] fuentes canónicas actualizadas;
- [ ] G1 cerrado.
