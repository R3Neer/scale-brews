# S18 — Vanilla PoseEngine + Mojang keyframe program

Estado: **PLAN CERRADO / IMPLEMENTACIÓN PENDIENTE**.

## Tesis

S18 cierra G3 tarea 4: las fórmulas vanilla pasan a ser `PoseEngine` canónicos con una sola autoridad de comportamiento y se añade `scalebrews:mojang_keyframes`, capaz de ejecutar en common/dedicated un programa neutral compilado desde `AnimationDefinition`.

El renderer/`AnimationDefinition` cliente sólo es fuente de tooling. La autoridad física es servidor + revisión de catálogo + inputs autoritativos.

## Requisitos y fronteras

Incluye FR-025..029, FR-033..037, FR-089..090 y NFR-001..005, NFR-010, NFR-012..013, NFR-028..029, NFR-031.

Se preservan:

- S15: bundle preparado una vez por revisión y reutilizado por receptor;
- S16: candidato completo validado antes del swap; rechazo conserva exactamente snapshot/bundle previo;
- S17: geometry y pose siguen separados; no se reconstruyen modelos/renderers en runtime server;
- server authority de channels/disponibilidad/catálogo;
- cero matrices/programas C2S;
- root TRS/gravedad fuera de `PoseEngine`;
- Alex/Citadel fuera de scope hasta G3.5.

## Baseline adversarial rojo — 2026-09-13

Snapshot pre-implementación: **`0258c6c619d2815afe91bcb96343d6e67998f3b0`**. Hasta él sólo hay documentación/tests/CI S18.

- Ordinary `34756921522`, job `103722768191`: **407/407**, `BUILD SUCCESSFUL`; artifact `10317093981`, SHA-256 `e1849077cade02d41604b8c59b312c150951869e4cd0ee84edc5dd1373ccf16b`.
- Focal `34756921526`, common job `103722768203`: 12 holdouts S18 + `minecraft:always_pass`; **11 S18 rojos**, 1 S18 verde. Lo único ya correcto es que `CollisionEngines` no expone almacenamiento global de pose programs. Artifact `10317187330`, SHA-256 `450048830cc2d5d850a0db972cdbadf514a4ba708de84e314da79989a62adc1b`.
- Mismo run, client job `103722768285`: cliente real arranca bajo Xvfb y falla únicamente porque no existe compiler `AnimationDefinition -> programa neutral`; artifact `10317897787`, SHA-256 `58552d93b1a122a815539043d4453c773969a729d92e492d4492933472767cf9`.

El baseline es red-before-green deliberado. No se rebaja ningún oracle.

## Estado inicial

1. `PoseEngine` público existe y es SAM de `geometry + inputs + parameters`.
2. Los built-ins siguen detrás de `PoseProvider/PoseProviders`.
3. `PoseProvider.Inputs` duplica `PoseEngine.Inputs` y sigue en tracker/history/payload/runtime.
4. `PoseProviders` mantiene registry y fórmulas paralelos.
5. S16 usa `legacy_pose_provider` como bridge precomputado.
6. `CollisionBinding.Pose` ya contiene `engine + parameters + channels`.
7. No existe `PoseProgram`, compiler `AnimationDefinition`, evaluator keyframe ni tabla de programs en catálogo.
8. Wire v4 sincroniza `{models, bindings}`.

## Estado objetivo

### 1. Una sola autoridad procedural

Los IDs `player_walking`, `quadruped`, `chicken`, `villager`, `iron_golem`, `ghast`, `feline`, `equine`, `bee` y `static` pasan a `CollisionEngines.pose(...)`.

Las fórmulas pertenecen al `PoseEngine`. `PoseProviders` puede sobrevivir únicamente como adapter read-only **legacy -> engine** para S16: sin `register`, sin `Map` propio y sin fórmulas duplicadas. El engine canónico no puede depender de `PoseProvider`.

`PoseEngine.Inputs` pasa a ser el DTO live único en `AuthorityPoseTracker`, `AnatomyPosePayload`, `AnatomyPoseHistory` y `ModelGeometryProvider`.

`CollisionBinding.Pose.channels` es el contrato de channels requeridos. Ausencia y valor `0` son estados distintos. `ordinary=false` y estados no soportados devuelven vacío y recuperan después sin pose congelada.

### 2. `PoseProgram` neutral

El DTO common/server-safe representa un `AnimationDefinition` sin tipos cliente:

- schema + metadata de fuente/version;
- duración finita y loop explícito;
- tracks/bones canónicos y deterministas;
- target soportado (`translation`, `rotation`, `scale`);
- keyframes ordenados;
- **cada keyframe conserva `timestamp`, `preTarget` y `postTarget`**, aunque ambos sean iguales en el constructor abreviado de Mojang;
- interpolación soportada identificable y validada;
- límites explícitos de programs/tracks/keyframes/bytes con N/N+1;
- ninguna referencia a `ModelPart`, `AnimationDefinition`, renderer, lambdas cliente ni clocks de render.

La separación `preTarget/postTarget` es normativa: el formato moderno puede representar discontinuidad a ambos lados de un mismo timestamp; aplanarlo a un vector perdería semántica original.

### 3. Compiler client/tooling

Una clase bajo `client.collision.preparation` lee `AnimationDefinition` original y emite `PoseProgram`. Primitiva, target o interpolación no soportada => exportación rechazada, nunca track omitido.

La equivalencia se compara contra una definición original en varios tiempos, incluyendo bordes de keyframe, loop y al menos un caso donde `preTarget != postTarget` si la fuente/API lo permite.

### 4. Programs son datos de revisión, no registry global

`WorldAnatomyCatalog.Snapshot` posee `posePrograms` junto con models/catalog/bindings. El bundle v5 contiene **`{models, pose_programs, bindings}`**. `AnatomyApi.PROTOCOL_VERSION` pasa a **5** y v4 es incompatible. `DATA_SCHEMA_VERSION` no cambia salvo cambio real de binding/policy.

Un program no se registra en `CollisionEngines`, ni en otro singleton equivalente. El registry global posee comportamiento; el snapshot world-owned posee datos.

Antes del swap se validan engine, referencia de program, schema/bounds/finitud, targets/interpolaciones, bones/parts y channels requeridos. Cualquier fallo conserva exactamente el `Accepted` anterior, incluido `PreparedBundle`.

### 5. Seam de binding/preparación del SPI

**Gap detectado en la revisión adversarial previa a implementación:** el `PoseEngine.evaluate(...)` actual sólo recibe `geometry + inputs + Map<String,String> parameters`. Un `program=<id>` no puede resolverse desde ahí si la tabla es world-owned, salvo usando estado global prohibido.

S18 debe resolverlo con una fase neutral de **preparación/binding por revisión** o mecanismo equivalente:

- el `PoseEngine` procedural público conserva compatibilidad de fuente como SAM; no se obliga a mods externos a reescribir sus lambdas por añadir programs;
- se puede añadir un método `default`/seam neutral que, durante aceptación de catálogo, reciba recursos de revisión (`PoseProgram` resolver), geometry, parameters y required channels y produzca un evaluator ligado/inmutable;
- engines procedurales pueden usar por defecto su `evaluate(...)` actual;
- `mojang_keyframes` resuelve el program **una vez al ligar el binding**, no mediante lookup global en hot path;
- runtime almacena/consume ese evaluator ligado a la revisión; un cambio de revisión produce otra identidad/evaluator;
- el seam no introduce dependencia `catalog -> singleton mutable -> engine` ni thread-local context.

El nombre exacto del tipo (`PreparedPose`, `Evaluator`, `BoundPoseEngine`, etc.) no es normativo; sí lo son ownership, inmutabilidad, fail-closed y compatibilidad del SAM público.

### 6. `scalebrews:mojang_keyframes`

El engine common/dedicated evalúa únicamente el programa ya ligado + `PoseEngine.Inputs` autoritativos. Resuelve loop/clamp y keyframes determinísticamente, aplica sólo transforms locales y valida la salida contra `ModelGeometry.transforms(...)` antes de cache/publicación.

No consulta resources locales, renderer, `AnimationDefinition`, `ModelPart` ni reloj cliente.

## Plan convergido

- [ ] **I1** Migrar runtime live a `PoseEngine.Inputs` sin cambiar los bits existentes salvo el bump de protocolo.
- [ ] **I2** Mover built-ins vanilla a `CollisionEngines.pose(...)` como únicos owners de fórmula.
- [ ] **I3** Reducir `PoseProviders` a adapter legacy read-only hacia engines canónicos.
- [ ] **I4** Implementar required-channel/unsupported-state fail-closed y recovery sin freeze.
- [ ] **I5** Añadir `PoseProgram` neutral, canónico, bounded y con `preTarget/postTarget`.
- [ ] **I6** Añadir seam de preparación/binding por revisión preservando el SAM procedural público.
- [ ] **I7** Extender catálogo/bundle a `{models, pose_programs, bindings}`, validar antes del swap y subir a protocolo v5.
- [ ] **I8** Añadir compiler client/tooling de `AnimationDefinition`.
- [ ] **I9** Implementar `scalebrews:mojang_keyframes` sobre evaluator ligado/server-safe.
- [ ] **I10** Migrar proof vanilla y comparar una definición original compilada a múltiples tiempos.
- [ ] **I11** Completar holdouts de malformed programs, N/N+1, required channels, stale non-reuse, recovery, orden, repeat exacto, v4/v5 y atomicidad S15/S16.
- [ ] **I12** Ejecutar ordinary + common/dedicated + client compiler/equivalence y hacer segunda lectura completa hasta cero cambios productivos.

## Modelo adversarial

### Ownership

- ningún engine vanilla canónico depende de `PoseProvider`;
- `PoseProviders` no tiene registry/register propio;
- adapter legacy == engine canónico para mismos inputs;
- ningún singleton common almacena programs fuera del snapshot world-owned;
- engine keyframe recibe/resuelve program por binding de revisión, no lookup global.

### Inputs y procedural

- mapas defensivamente copiados e inmutables;
- required ausente != required presente con cero;
- extra channel irrelevante no altera resultado;
- parameters desconocidos fallan cerrados;
- `ordinary=false` vacío y recovery posterior válido;
- mismo input produce matrices exactamente reproducibles;
- modelo/version/familia no compatible no se reconoce por coincidencia accidental de nombres.

### Program validation

- NaN/∞, duración inválida, timestamp fuera de rango, track vacío, ID duplicado, keyframes desordenados o límite N+1 => rechazo;
- `preTarget/postTarget` se conservan y ambos se validan;
- bone inexistente/ambiguo => rechazo cuando deba resolverse;
- target/interpolación no soportado => rechazo, nunca omisión silenciosa;
- orden equivalente de maps/resources produce bytes/hash/evaluación canónicos iguales.

### Evaluation

- t=0, t=duración, antes/después de keyframe, discontinuidad pre/post y loop wrap;
- translation/rotation/scale conservan unidades y composición de rest pose;
- linear y cualquier spline soportada se comparan contra fuente original con tolerancia documentada;
- programa no puede escribir root/world transform ni bones ajenos.

### Wire / atomicidad

- v4/v5 incompatibles;
- fragments de epoch/revision mezcladas no publican;
- program inválido conserva **el mismo objeto** snapshot + prepared bundle aceptado;
- replacement válido mismo program ID/nueva revisión invalida material anterior;
- cliente jamás usa program local como fallback.

### Classloading

- common collision runtime no contiene referencias a `net.minecraft.client`, `AnimationDefinition` ni compiler;
- compiler vive exclusivamente en client preparation;
- dedicated registra/evalúa program fixture sin cargar clases cliente.

## Criterio de cierre

S18 sólo cierra si:

1. built-ins vanilla son `PoseEngine` reales con una sola autoridad y legacy es sólo adapter;
2. input live único = `PoseEngine.Inputs`;
3. program neutral conserva semántica completa relevante, incluido `preTarget/postTarget`;
4. program world-owned se liga a engine por revisión sin estado global;
5. `mojang_keyframes` funciona en dedicated sin clases cliente;
6. v5 conserva atomicidad/reuse/rejection de S15/S16;
7. missing/unsupported produce `UNAVAILABLE` y recovery sin freeze;
8. procedural vanilla + al menos un `AnimationDefinition` original quedan equivalentes;
9. ordinary + common/dedicated + client compiler/original-source + holdouts quedan verdes;
10. segunda lectura final produce cero cambios de producción.
