# S04 — Integración estable y cierre funcional de G1

Estado: **CIERRE DE G1 SUPERADO POR EVIDENCIA POSTERIOR**. La implementación de S04 permanece realizada, pero G1 está reabierto por un bloqueo S01/NFR-025; el estado global vive en `ENTITY_COLLISIONS_PLAN.md`. **G2 no se ha iniciado**.

## 1. Scope

Tesis única: conectar las fronteras estabilizadas por S01-S03 sin iniciar el solver Q2 ni sustituir el lifecycle/catálogo preparado que G3 posee explícitamente.

Incluye el resto de tareas G1: capabilities/versionado, body adapters/categorías/policy en integration, fixture externo API+JSON, selección canónica de bindings fuera de `collision.internal`, validación de referencias y cierre adversarial del gate.

Excluye: división de `AnatomyMovement`, Q2, prepared geometry catalog/lifecycle, ModelPart/Mojang engines completos, prediction/reconciliation y retirada del motor legacy.

## 2. Estado inicial

Tras S03, `collision.api` ya no dependía directamente de `internal`, existían SPIs públicos y binding/policy v1, pero `Platforms` seguía poseyendo body adapters/categorías y evaluaba ratio/friction mediante tipos legacy. Tampoco existía un índice canónico de bindings fuera de `WorldAnatomyCatalog`, cuya sustitución completa pertenece a G3.

## 3. Plan y convergencia

- [x] añadir `BodyAdapter` público y registry `CollisionAdapters` sin ownership físico;
- [x] mover clasificación/unsupported ordinary states a `collision.integration.BodyClassification`;
- [x] mover evaluación ratio/enabled/friction a `collision.integration.CollisionRules`;
- [x] convertir policy legacy a policy canónica sólo en el decoder de migración, sin importar `automatic_top`;
- [x] hacer que el motor legacy consuma temporalmente clasificación/rules nuevas para evitar dos políticas divergentes;
- [x] crear `collision.catalog.CollisionBindingCatalog` como índice/loader canónico sin adelantar lifecycle G3;
- [x] validar geometry/pose/root registrados, sin fallback por id desconocido;
- [x] versionar protocol/capabilities de G1;
- [x] demostrar fixture externo init-time que registra geometry/pose/root/body por API y los selecciona desde JSON;
- [x] deduplicar codec de filter interno hacia el codec canónico;
- [x] registrar S01-S04 como entrypoints GameTest;
- [x] fijar ratio exactamente en `< / == / >` sin epsilon permisivo;
- [x] warning deduplicado al migrar surfaces legacy;
- [x] validar support policy antes de ordenar;
- [x] identidad estructural para variant selectors;
- [x] orden canónico de parámetros, snapshots, engines y adapters;
- [x] body adapters fail-closed ante input/output inválidos;
- [x] `PlatformEligibility` como bridge deprecated al mismo `CollisionRules` canónico;
- [x] integrar campaña adversarial paralela y reparar todos sus fallos reales;
- [x] repetir server + client/integrated/dedicated sobre el árbol reparado;
- [x] pasada final completa sin cambios dentro del scope G1.

Las revisiones P1-P10 evitaron invadir G2/G3/G5, corrigieron policy y ratio, endurecieron determinismo/fail-closed y eliminaron semántica duplicada. La campaña adversarial final reabrió S01 y S03, no el plan de G1: ambos fallos eran bugs de implementación y se repararon sin cambiar requisitos ni orden de gates.

## 4. Modelo adversarial

La campaña base y paralela cubrió, entre otros: backend público/ausente/duplicado, API leakage, registries duplicados/desconocidos, DTO bounds, engine refs ausentes, variant ambiguity/collisions, ordering, schema y unknown fields, legacy xor, filtros inválidos, ratio nextDown/exact/nextUp, support/category precedence, adapters inválidos y fixture externo init-time.

El snapshot adversarial `19626950569110963cd47611b245234c650f2ca0` dejó exactamente dos fallos requeridos:

1. S01: el contrato `AnatomyBackend` seguía siendo API pública de consumer;
2. S03: codecs canónicos aceptaban campos legacy/desconocidos por permisividad de `RecordCodecBuilder`.

Ambos se clasificaron como bugs de implementación. Ningún requisito se relajó y ningún test adversarial se eliminó.

Una pasada adversarial posterior al cierre añadió un holdout estructural de NFR-025 y detectó que la primera reparación de S01 había movido `AnatomyBackend` a `collision.runtime` pero había dejado una dependencia mutua `collision.api ↔ collision.runtime`.

## 5. Implementación final de G1

- [x] façade pública mínima `AnatomyApi` con backend runtime Scale-owned no público de consumer;
- [x] `GeometryEngine`, `PoseEngine`, `RootTransformProvider`, `BodyAdapter` y registries públicos;
- [x] `CollisionBinding`, `CollisionPolicy`, `CollisionCodecs` v1;
- [x] `CollisionBindingCatalog` canónico y determinista;
- [x] `BodyClassification` y `CollisionRules` en integration;
- [x] `LegacyCollisionData` como decoder unidireccional, no motor;
- [x] fixture externo registrado por API y seleccionado por datapack JSON;
- [x] protocol v3 / data schema v1 / capabilities explícitas;
- [x] validación estricta de unknown fields y de referencias geometry/pose/root;
- [x] legacy `platform` conservado sólo como motor temporal hasta G5, sin convertirlo en modelo del core nuevo.

La primera reparación adversarial fue `3ba014dc2e19b6b700a707bd3177c55ac443328f`: movió el contrato de backend a `collision.runtime`, actualizó ServiceLoader e introdujo decode estricto de objetos canónicos y anidados. Esa reparación cerró los dos fallos observados entonces, pero el holdout posterior demostró que la frontera de paquetes todavía no había convergido respecto de NFR-025.

## 6. Test matrix final

| Propiedad | Nivel | Resultado vigente |
| --- | --- | --- |
| S01 façade/backend/fail-closed | adversarial GameTest/reflection | PASS salvo ciclo de capas |
| backend no público de consumer | adversarial GameTest/reflection | PASS |
| `collision.api ↔ collision.runtime` sin ciclo | adversarial reflection/GameTest | **FAIL vigente** |
| S02 registries/DTO bounds/order | adversarial GameTest | PASS |
| engine/pose/root inexistentes | GameTest | PASS, fail-closed |
| S03 schema/policy/legacy migration | adversarial GameTest | PASS |
| unknown/legacy fields canónicos | adversarial GameTest | PASS, reject |
| constructor→codec round-trip en límites máximos | adversarial GameTest | PASS |
| filter inválido devuelve codec error | regression GameTest | PASS |
| fixture mod API + JSON | init + GameTest/resource | PASS |
| variant ambiguity / structural identity | adversarial GameTest | PASS |
| ratio nextDown/exact/nextUp | GameTest | PASS |
| body adapter invalid input/output | adversarial GameTest | PASS |
| legacy policy bridge | regression GameTest | PASS |
| suite servidor actual | `./gradlew build` | **267/268 PASS; 1 FAIL NFR-025** |
| client real + integrated + dedicated | `xvfb-run -a ./gradlew runClientGameTest` | PASS histórico anterior al nuevo bloqueo; debe repetirse tras reparación common/API |

## 7. Fallos encontrados y bucles

El primer candidato S04 `ac848a41edb2044e4f2a188623262642f6bd7909` falló run `34583969083` por el codec de `AnatomyFilter`. `fbd9e31ef0c78c4666f840816d167f24b5e9fb2b` lo reparó. La revisión detectó además referencias a engines no registrados y `8f4a682232fe39e385f6279352430f326f982711` añadió rechazo explícito.

`144470d769869426d8ee4be57912b9468a25e79e` corrigió un defecto de evidencia: S01-S04 compilaban pero no estaban registrados como GameTests. Desde entonces la suite nueva se ejecuta realmente. Revisiones posteriores endurecieron warning legacy, ratio exacto, policy null, selector identity, ordering, adapters fail-closed y el bridge de `PlatformEligibility`. El candidato no adversarial llegó a **262/262** y client proof verde.

La campaña adversarial paralela amplió la suite a **266 tests**. Run `34592683287`, job `103241351742`, sobre `19626950569110963cd47611b245234c650f2ca0` quedó rojo exactamente por los dos defectos S01/S03 descritos arriba. Se volvió a implementación, no se tocaron tests ni requisitos.

`3ba014dc2e19b6b700a707bd3177c55ac443328f` reparó ambos. GitHub Actions run `34593762577`, job `103244714934`, ejecutó la batería completa y terminó **266/266 required GameTests passed**, `BUILD SUCCESSFUL`. Artifact `10260227920`, SHA-256 `67622ee3c4af2ddffb902854f6d6843371aa9b6768b4cd79c300c6e44c238049`.

Para repetir la capa cliente sobre el mismo código, el trigger `23a1072ef451a14f59f707019415e1f4ac60dbab` modificó únicamente el workflow temporal. El workflow ordinario run `34593970120` volvió a quedar verde. `g1-client-proof` run `34593970131`, job `103245364717`, ejecutó `xvfb-run -a ./gradlew runClientGameTest` y terminó **success / BUILD SUCCESSFUL**. El log confirmó export original de cow/player wide/slim, 80 comparaciones por cada una, 640 comparaciones vanilla-family adicionales, `S00_CLIENT_RECEIPT_AUTHORITY PASS`, `S00_OBSERVER_AUTHORITY PASS` y dedicated `allow-flight=false` a **0/100/200 ms RTT, 120 ticks cada uno**. El workflow temporal se eliminó después; esa limpieza no cambia producción ni tests.

La pasada adversarial posterior añadió `S01PublicApiBoundaryTests.publicApiAndRuntimeDoNotFormALayerCycle`. `94f2db96277f02c9e9d4903bdb02046b7878f4c2`, run `34595220211`, job `103249293688`, ejecutó 267 tests: **266 PASS y un único FAIL**, el ciclo `collision.api ↔ collision.runtime`. `be41003bbceaecd27a36dc1d13e4bef0f8bde114`, run `34595505502`, job `103250215902`, añadió el holdout S03 de round-trip máximo y ejecutó 268 tests: el nuevo holdout pasó y quedó exactamente el mismo único fallo S01.

## 8. Revisión final

La antigua pasada final recorrió las nueve tareas de G1, API/data/catalog/integration/migration, ownership, versionado/capabilities, policy/ratio, adapters, fixture init-time, engine refs, strict codecs, identidad estructural, determinismo y fronteras de gates.

Conserva valor para los puntos que sí verificó:

- no queda `AnatomyBackend` como tipo público en `collision.api`;
- `CollisionBindingCatalog` valida geometry/pose/root registrados;
- codecs canónicos rechazan unknown/legacy fields, incluidos objetos anidados;
- legacy surfaces sólo migran a planos explícitos y no generan anatomía;
- `PlatformEligibility` no conserva semántica paralela;
- no se ha dividido ni modificado `AnatomyMovement` para Q2;
- no se ha iniciado el pipeline material de G2;
- responsabilidades que pertenecen a G3/G5 permanecen explícitamente diferidas.

La revisión posterior invalida únicamente la afirmación de convergencia de paquetes: mover el backend a `collision.runtime` dejó un ciclo de capas de alto nivel prohibido por NFR-025. **G1 no está actualmente cerrado.**

## 9. Cierre

- [x] nueve tareas G1 implementadas;
- [x] campaña adversarial inicial integrada;
- [x] fallos adversariales iniciales clasificados y reparados;
- [x] strict codecs y holdout de límites máximos verdes;
- [ ] ciclo `collision.api ↔ collision.runtime` eliminado;
- [ ] suite servidor completa verde con los holdouts actuales;
- [ ] client/integrated/dedicated repetido tras reparar la frontera common/API;
- [ ] `VALIDATION.md` actualizado con la futura evidencia verde;
- [ ] pasada final completa sin cambios;
- [ ] G1 cerrado formalmente de nuevo;
- [x] G2 permanece sin iniciar.

El estado global vigente está en `ENTITY_COLLISIONS_PLAN.md`; la evidencia roja vigente está en `VALIDATION.md`.
