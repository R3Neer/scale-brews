# S04 — Integración estable y cierre funcional de G1

Estado: **candidato de implementación; pendiente de CI y revisión de gate**.

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
- [x] versionar protocol/capabilities de la frontera G1;
- [x] demostrar fixture externo que registra geometry/pose/root/body por API y selecciona geometry/pose/root desde JSON;
- [x] eliminar el codec de filter duplicado de `collision.internal` delegándolo al codec canónico.

P1 rechazó reemplazar `WorldAnatomyCatalog`: el plan asigna literalmente esa sustitución a G3. P2 mantuvo `automatic_top` sólo en el motor legacy que G5 retirará, pero lo excluyó de `CollisionPolicy` y de toda descripción canónica. P3 detectó que traducir `enabled=true` legacy a un profile override reactivaría un support deshabilitado globalmente; la migración lo representa como inherit y sólo conserva `false` explícito. P4 reservó selección ambigua de variantes como fail-closed. La siguiente pasada no cambió el plan.

## 4. Modelo adversarial

Ataques: adapter duplicado/categoría inválida, binding JSON con engine id externo, engine ausente, selector variante ambiguo, peer protocol v2, ratio justo por debajo/encima, support/category disable legacy y `automatic_surfaces=true` legacy.

Holdouts: el fixture debe registrarse sólo mediante API pública; el JSON debe seleccionar ids exactos; el catálogo canónico no importa `platform.*`; una ambigüedad de variantes no depende del orden de recursos.

## 5. Implementación

- [x] `collision.api.spi.BodyAdapter` + `CollisionAdapters`;
- [x] `collision.integration.BodyClassification`;
- [x] `collision.integration.CollisionRules`;
- [x] `collision.catalog.CollisionBindingCatalog`;
- [x] bridge de policy legacy en `LegacyCollisionData`;
- [x] `Platforms` delega adapters/category/ordinary/ratio/friction a las fronteras nuevas;
- [x] API protocol v3/data schema v1 y capabilities G1;
- [x] test-mod fixture + datapack JSON;
- [x] codec filter interno deduplicado.

## 6. Test matrix

| Propiedad | Nivel | Resultado |
| --- | --- | --- |
| fixture API + JSON | GameTest/resource | pendiente CI |
| variant ambiguity fail-closed | GameTest | pendiente CI |
| protocol/capabilities | GameTest | pendiente CI |
| ratio/friction canonical | GameTest | pendiente CI |
| suite histórica | `./gradlew build` | pendiente CI |

## 7. Fallos/bucles

Pendiente de CI.

## 8. Revisión final

Pendiente tras CI. Después se revisará G1 completo contra sus nueve tareas y se actualizarán plan/VALIDATION; no se empezará G2.

## 9. Cierre

- [ ] CI verde;
- [ ] segunda pasada adversarial;
- [ ] revisión completa G1 sin cambios;
- [ ] fuentes canónicas actualizadas;
- [ ] G1 cerrado.
