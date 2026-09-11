# S04 — Integración estable y cierre funcional de G1

Estado: **implementación y CI real verificadas; pendiente de campaña adversarial paralela y revisión final del gate G1**.

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
- [x] demostrar un mod fixture que registra geometry/pose/root/body **durante inicialización** por API y selecciona geometry/pose/root desde JSON;
- [x] eliminar el codec de filter duplicado de `collision.internal` delegándolo al codec canónico;
- [x] registrar S01-S04 como entrypoints GameTest para que la evidencia ejecute realmente la suite nueva;
- [x] fijar la frontera de ratio exactamente en `<`, `==` y `>` sin epsilon permisivo;
- [x] emitir warning deduplicado al migrar surfaces legacy a planos unilaterales.

P1 rechazó reemplazar `WorldAnatomyCatalog`: el plan asigna literalmente esa sustitución a G3. P2 mantuvo `automatic_top` sólo en el motor legacy que G5 retirará, pero lo excluyó de `CollisionPolicy` y de toda descripción canónica. P3 detectó que traducir `enabled=true` legacy a un profile override reactivaría un support deshabilitado globalmente; la migración lo representa como inherit y sólo conserva `false` explícito. P4 reservó selección ambigua de variantes como fail-closed. P5 detectó que el índice canónico no verificaba IDs de engine/provider y añadió rechazo explícito. P6 detectó que S01-S04 compilaban pero no figuraban en `fabric-gametest`; se corrigió el manifest y el fixture externo pasó a entrypoint `main`. P7 detectó que el `+1e-7` histórico contradecía la frontera literal FR-009 y lo retiró; también añadió el warning requerido por FR-023. La pasada siguiente no cambió el scope de G1.

## 4. Modelo adversarial

La campaña adversarial completa está delegada al agente paralelo indicado por el propietario; cualquier test nuevo que aterrice en `chatgpt-editing` se integra en el sprint y un fallo obliga a reabrir la fase correspondiente.

Invariantes ya cubiertos por la implementación/test base: adapter duplicado/categoría inválida, binding JSON con engine id externo, engine/provider ausente, selector variante ambiguo, peer protocol v2, ratio inmediatamente debajo/exacto/inmediatamente encima, support/category disable legacy y exclusión de `automatic_surfaces` del modelo canónico.

## 5. Implementación

- [x] `collision.api.spi.BodyAdapter` + `CollisionAdapters`;
- [x] `collision.integration.BodyClassification`;
- [x] `collision.integration.CollisionRules`;
- [x] `collision.catalog.CollisionBindingCatalog`;
- [x] bridge de policy legacy en `LegacyCollisionData`;
- [x] `Platforms` delega adapters/category/ordinary/ratio/friction a las fronteras nuevas;
- [x] API protocol v3/data schema v1 y capabilities G1;
- [x] test-mod fixture de inicialización + datapack JSON;
- [x] codec filter interno deduplicado con error de decode fail-closed;
- [x] referencias de geometry/pose/root validadas contra registries públicos;
- [x] manifest del test mod incluye S01, S02, S03 y S04;
- [x] ratio canónico exacto y warning de planos legacy.

## 6. Test matrix

| Propiedad | Nivel | Resultado ejecutado |
| --- | --- | --- |
| S01 backend/fachada | GameTest/reflection | PASS |
| S02 registries/DTO bounds | GameTest | PASS |
| S03 schema/policy/legacy migration | GameTest | PASS |
| fixture de mod API + JSON | init + GameTest/resource | PASS |
| engine/pose/root inexistentes rechazados | GameTest | PASS |
| variant ambiguity fail-closed | GameTest | PASS |
| protocol/capabilities | GameTest | PASS |
| ratio `nextDown / exact / nextUp` para profile y default 0.85 | GameTest | PASS |
| filter inválido devuelve error de codec, no excepción fuera del parse | regresión GameTest | PASS |
| warning de plano legacy | runtime log | OBSERVADO |
| suite histórica | `./gradlew build` | PASS |

## 7. Fallos/bucles

El primer candidato `ac848a41edb2044e4f2a188623262642f6bd7909` ejecutó GitHub Actions run `34583969083`. Producción, cliente y GameTests compilaron, pero `runGameTest` terminó rojo: 242 tests ejecutados y exactamente uno falló, `anatomy_geometry_tests_explicit_piece_filters_retain_only_valid_anatomy`. La causa fue un **bug de implementación** en la deduplicación del codec de `AnatomyFilter`: el constructor lanzaba `IllegalArgumentException` durante decode en vez de convertir el dato inválido en `DataResult.error`. Se volvió a implementación y `fbd9e31ef0c78c4666f840816d167f24b5e9fb2b` restauró la semántica fail-closed mediante un DTO intermedio + `comapFlatMap`.

La misma revisión detectó que `CollisionBindingCatalog` aceptaba IDs de geometry/pose/root no registrados. Se clasificó como bug de implementación de FR-033 y `8f4a682232fe39e385f6279352430f326f982711` añadió validación y tests de las tres referencias.

Una auditoría de evidencia reveló que los runs S01-S03 y el primer S04 mantenían el baseline de **242**: las clases nuevas no estaban registradas en `fabric.mod.json`. Compilar tests no equivale a ejecutarlos. `144470d769869426d8ee4be57912b9468a25e79e` registró S01-S04 y convirtió el fixture externo en un verdadero initializer del mod de tests.

El primer intento del run `34584717556` falló durante configuración de Loom por descarga de Minecraft, antes de compilación. Se clasificó como **entorno/evidencia** y se repitió exactamente el mismo snapshot. El intento 2/job `103216390687` ejecutó **256/256 required GameTests** y quedó verde.

La revisión de gate posterior detectó dos requisitos demasiado laxos: FR-023 exigía warning al migrar un plano legacy y FR-009 exige frontera `< / == / >` exacta. `1a19ec31cdaeb8c0d98cc86b327adb1661e09632` añadió warning deduplicado y eliminó el epsilon de ratio. GitHub Actions run `34585112975`, job `103217403352`, volvió a ejecutar **256/256 required GameTests**; el log muestra expresamente el warning de migración para `minecraft:cow`.

## 8. Revisión final

La revisión no adversarial del gate ha recorrido las nueve tareas de G1, separación API/data/catalog/integration/migration, ownership, versión/capabilities, ratio/policy, adapters, fixture init-time y fronteras G2/G3/G5. No se ha iniciado G2. Queda ejecutar/integrar la campaña adversarial paralela y, si modifica cualquier cosa, repetir CI y esta revisión desde el principio.

## 9. Cierre

- [x] implementación de las nueve tareas G1;
- [x] CI G1 realmente registrada verde: 256/256;
- [x] fallos encontrados clasificados y reparados;
- [x] revisión no adversarial completa sin cambios posteriores al último fix;
- [ ] campaña adversarial paralela integrada o sin fallos pendientes;
- [ ] pasada final completa posterior a esa campaña;
- [ ] fuentes canónicas `ENTITY_COLLISIONS_PLAN.md` y `VALIDATION.md` cerradas para G1;
- [ ] G1 cerrado formalmente.

Evidencia vigente de implementación: `1a19ec31cdaeb8c0d98cc86b327adb1661e09632`, GitHub Actions run `34585112975`, job `103217403352`, **256/256 required GameTests passed**. Artifact `10193275290`, SHA-256 `bf62d346858f722fd1a639779ff9c36a9897a676dcd9cd27e166195d4a8f0494`.
