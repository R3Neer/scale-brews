# S04 — Integración estable y cierre funcional de G1

Estado: **implementación, servidor y cliente real verificados; pendiente de campaña adversarial paralela y revisión final cero-cambios del gate G1**.

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
- [x] emitir warning deduplicado al migrar surfaces legacy a planos unilaterales;
- [x] rechazar keys nulas de support policy antes de canonical ordering;
- [x] usar identidad estructural para variant selectors, sin `Map.toString()` como key accidental;
- [x] conservar orden canónico en parámetros, snapshots de bindings, registries de engines y adapters;
- [x] hacer fail-closed la frontera de body adapters ante body/request/output inválidos;
- [x] convertir `PlatformEligibility` en fachada legacy sin semántica propia, delegando al mismo `CollisionRules` canónico.

P1 rechazó reemplazar `WorldAnatomyCatalog`: el plan asigna literalmente esa sustitución a G3. P2 mantuvo `automatic_top` sólo en el motor legacy que G5 retirará, pero lo excluyó de `CollisionPolicy` y de toda descripción canónica. P3 detectó que traducir `enabled=true` legacy a un profile override reactivaría un support deshabilitado globalmente; la migración lo representa como inherit y sólo conserva `false` explícito. P4 reservó selección ambigua de variantes como fail-closed. P5 detectó que el índice canónico no verificaba IDs de engine/provider y añadió rechazo explícito. P6 detectó que S01-S04 compilaban pero no figuraban en `fabric-gametest`; se corrigió el manifest y el fixture externo pasó a entrypoint `main`. P7 detectó que el `+1e-7` histórico contradecía la frontera literal FR-009 y lo retiró; también añadió el warning requerido por FR-023. P8 encontró validación de support policy posterior al sort, colisión de identidad de selectors basada en representación textual y pérdida de orden canónico al exponer snapshots. P9 endureció los outputs de body adapters para que datos ausentes/no finitos no fabriquen transporte. P10 detectó que `PlatformEligibility`, aunque ya no era ruta viva, conservaba una segunda implementación legacy del ratio y la convirtió en bridge puro al policy canónico para preservar sus regression callers sin mantener doble semántica. La pasada siguiente no amplió el scope de G1.

## 4. Modelo adversarial

La campaña adversarial completa está delegada al agente paralelo indicado por el propietario; cualquier test nuevo que aterrice en `chatgpt-editing` se integra en el sprint y un fallo obliga a reabrir la fase correspondiente.

Invariantes ya cubiertos por la implementación/test base: adapter duplicado/categoría inválida, binding JSON con engine id externo, engine/provider ausente, selector variante ambiguo, selector estructural con valores que contienen delimitadores, peer protocol v2, ratio inmediatamente debajo/exacto/inmediatamente encima, support/category disable legacy, ausencia de `automatic_surfaces` en el modelo canónico, canonical ordering y fail-closed en entradas/outputs de adapters. El owner de gravedad no se volvió a duplicar en S04: `GravityFrames` ya había sido clasificado TRUSTED en S00 con ownership/idempotencia y seis frames cubiertos.

## 5. Implementación

- [x] `collision.api.spi.BodyAdapter` + `CollisionAdapters`;
- [x] `collision.integration.BodyClassification`;
- [x] `collision.integration.CollisionRules`;
- [x] `collision.catalog.CollisionBindingCatalog`;
- [x] bridge de policy legacy en `LegacyCollisionData`;
- [x] `Platforms` delega adapters/category/ordinary/ratio/friction a las fronteras nuevas;
- [x] `PlatformEligibility` queda sólo como fachada deprecated hacia `CollisionRules`, sin un segundo criterio de elegibilidad;
- [x] API protocol v3/data schema v1 y capabilities G1;
- [x] test-mod fixture de inicialización + datapack JSON;
- [x] codec filter interno deduplicado con error de decode fail-closed;
- [x] referencias de geometry/pose/root validadas contra registries públicos;
- [x] manifest del test mod incluye S01, S02, S03 y S04;
- [x] ratio canónico exacto y warning de planos legacy;
- [x] validación/canonicalización determinista de policy, selectors, engine requests y snapshots;
- [x] integración de body adapters acotada y fail-closed.

## 6. Test matrix

| Propiedad | Nivel | Resultado ejecutado |
| --- | --- | --- |
| S01 backend/fachada | GameTest/reflection | PASS |
| S02 registries/DTO bounds/order | GameTest | PASS |
| S03 schema/policy/legacy migration | GameTest | PASS |
| fixture de mod API + JSON | init + GameTest/resource | PASS |
| engine/pose/root inexistentes rechazados | GameTest | PASS |
| variant ambiguity fail-closed | GameTest | PASS |
| selectors estructuralmente distintos aunque `toString()` colisione | GameTest | PASS |
| protocol/capabilities | GameTest | PASS |
| ratio `nextDown / exact / nextUp` para profile y default 0.85 | GameTest | PASS |
| filter inválido devuelve error de codec, no excepción fuera del parse | regresión GameTest | PASS |
| support policy null rechazada antes de ordenar | GameTest | PASS |
| orden canónico de requests/catalog/registries | GameTest | PASS |
| adapter/body inválido no fabrica movimiento | GameTest | PASS |
| legacy `PlatformEligibility` usa el mismo policy canónico | regresión GameTest vía callers históricos | PASS |
| warning de plano legacy | runtime log | OBSERVADO |
| suite histórica servidor | `./gradlew build` | PASS, 262/262 |
| cliente real + integrated + dedicated | `xvfb-run -a ./gradlew runClientGameTest` | PASS |

## 7. Fallos/bucles

El primer candidato `ac848a41edb2044e4f2a188623262642f6bd7909` ejecutó GitHub Actions run `34583969083`. Producción, cliente y GameTests compilaron, pero `runGameTest` terminó rojo: 242 tests ejecutados y exactamente uno falló, `anatomy_geometry_tests_explicit_piece_filters_retain_only_valid_anatomy`. La causa fue un **bug de implementación** en la deduplicación del codec de `AnatomyFilter`: el constructor lanzaba `IllegalArgumentException` durante decode en vez de convertir el dato inválido en `DataResult.error`. Se volvió a implementación y `fbd9e31ef0c78c4666f840816d167f24b5e9fb2b` restauró la semántica fail-closed mediante un DTO intermedio + `comapFlatMap`.

La misma revisión detectó que `CollisionBindingCatalog` aceptaba IDs de geometry/pose/root no registrados. Se clasificó como bug de implementación de FR-033 y `8f4a682232fe39e385f6279352430f326f982711` añadió validación y tests de las tres referencias.

Una auditoría de evidencia reveló que los runs S01-S03 y el primer S04 mantenían el baseline de **242**: las clases nuevas no estaban registradas en `fabric.mod.json`. Compilar tests no equivale a ejecutarlos. `144470d769869426d8ee4be57912b9468a25e79e` registró S01-S04 y convirtió el fixture externo en un verdadero initializer del mod de tests.

El primer intento del run `34584717556` falló durante configuración de Loom por descarga de Minecraft, antes de compilación. Se clasificó como **entorno/evidencia** y se repitió exactamente el mismo snapshot. El intento 2/job `103216390687` ejecutó **256/256 required GameTests** y quedó verde.

La revisión de gate posterior detectó dos requisitos demasiado laxos: FR-023 exigía warning al migrar un plano legacy y FR-009 exige frontera `< / == / >` exacta. `1a19ec31cdaeb8c0d98cc86b327adb1661e09632` añadió warning deduplicado y eliminó el epsilon de ratio. GitHub Actions run `34585112975`, job `103217403352`, volvió a ejecutar **256/256 required GameTests**.

`f51580ff13f9f932cd6651955099fc6d5d0ad8c5` hizo explícito el rechazo de support keys nulas antes del ordenado canónico; run `34585899279`, job `103219909691`, pasó **257/257**. `86d8ec139fb96ceaad19aa3a0937637c899224b2` sustituyó la identidad textual de variant selectors por identidad estructural. `8728e7a9375e5ace0a6996576517bc1f6792fec2` conservó orden canónico en request/catalog snapshots.

`d6a17d5207c9eefd13b5b31dfb35ede578679f2a` añadió canonical ordering a snapshots de registries y fail-closed a la frontera de body adapters. GitHub Actions run `34586795093`, job `103222758920`, ejecutó **262/262 required GameTests** y terminó `BUILD SUCCESSFUL`.

La pasada final no adversarial detectó después que `PlatformEligibility` seguía compilado como segundo evaluador legacy con su antiguo epsilon, aunque la ruta viva ya usaba `CollisionRules`. `7ddfe8a09a6375d8470b3b20c66e5a1e2a0f2460` lo convirtió en bridge deprecated sin lógica propia. GitHub Actions run `34588025491`, job `103226623446`, mantuvo **262/262 required GameTests**. Artifact `10194451880`, SHA-256 `fd47a68a48a771add9a223d600beae7fc3a2f23e8de7e411245423539ed85ba2`.

Para no aceptar un cambio de frontera compartida sólo con servidor, se creó un workflow temporal autocontenido y se ejecutó sobre el snapshot exacto `b1d7c3b204412119d2c5ee708d3784f231a7db76`. En ese mismo commit, el workflow ordinario run `34588346710`, job `103227629075`, volvió a pasar **262/262 required GameTests**, con artifact `10194580575` y SHA-256 `3f0fa9bb4c344eab80452d7eba9235253acb158677c6a544a9acabf0d8ee99da`. El workflow `g1-client-proof` run `34588346767`, job `103227629118`, ejecutó `xvfb-run -a ./gradlew runClientGameTest` y terminó **success**: arrancó cliente real/integrated server, exportó geometría original de cow y player wide/slim, completó las comparaciones de pose del harness, imprimió las pruebas de autoridad client receipt/observer y completó el dedicated proof con `allow-flight=false` a **0/100/200 ms RTT**. Los avisos de ALSA/narrador/servicios HTTP del runner no fueron causa de fallo. El workflow temporal se retiró después en `3da3f569749ad8f5bbc87e99c03a58d6da2dc9fc`; esa retirada es sólo CI y no cambia producción ni tests.

## 8. Revisión final

La revisión no adversarial del gate ha recorrido las nueve tareas de G1, separación API/data/catalog/integration/migration, ownership, versionado/capabilities, ratio/policy, adapters, fixture init-time, identidad estructural, determinismo de colecciones y fronteras G2/G3/G5. La comparación contra el cierre S00 mantiene cambios exclusivamente en G1 y sus tests/documentos; no se ha modificado `AnatomyMovement` ni se ha iniciado Q2. La lectura literal de requisitos confirmó además que FR-073 no requería nueva mutación global: `GravityFrames` ya estaba aceptado como TRUSTED en S00 con owner único/idempotente y seis frames cubiertos.

Esta revisión todavía **no es la pasada final de cierre**, porque la campaña adversarial paralela encargada por el propietario aún puede introducir tests o cambios. Si modifica cualquier cosa, se repiten CI y revisión completa desde el principio.

## 9. Cierre

- [x] implementación de las nueve tareas G1;
- [x] CI servidor G1 realmente registrada verde: 262/262;
- [x] client/integrated/dedicated real verde sobre el mismo snapshot de código;
- [x] fallos no adversariales encontrados, clasificados y reparados;
- [x] revisión no adversarial completa del candidato actual;
- [ ] campaña adversarial paralela integrada o sin fallos pendientes;
- [ ] pasada final completa posterior a esa campaña con cero cambios;
- [ ] `VALIDATION.md` cerrado para G1;
- [ ] G1 cerrado formalmente.

Evidencia vigente no adversarial: snapshot `b1d7c3b204412119d2c5ee708d3784f231a7db76`; server run `34588346710` / job `103227629075`, **262/262**; client proof run `34588346767` / job `103227629118`, **success**. La cabeza posterior `3da3f569749ad8f5bbc87e99c03a58d6da2dc9fc` sólo elimina el workflow temporal y no altera código de producción ni tests.
