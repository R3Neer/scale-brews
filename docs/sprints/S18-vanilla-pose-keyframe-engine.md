# S18 — Vanilla PoseEngine + Mojang keyframe program

Estado: **PLAN CERRADO / IMPLEMENTACIÓN PENDIENTE**.

## Tesis

S18 cierra G3 tarea 4: las fórmulas vanilla pasan a ser `PoseEngine` canónicos con una sola autoridad de comportamiento y se añade `scalebrews:mojang_keyframes`, capaz de ejecutar en common/dedicated un programa neutral compilado desde `AnimationDefinition`.

El renderer/`AnimationDefinition` cliente sólo es fuente de tooling. La autoridad física es servidor + revisión de catálogo + inputs autoritativos.

## Requisitos y fronteras

Incluye FR-025..029, FR-033..037, FR-089..090 y NFR-001..005, NFR-010, NFR-012..013, NFR-028..029, NFR-031.

Se preservan S15 (bundle preparado/reutilizado), S16 (swap atómico/fail-closed), S17 (geometry y pose separados), server authority de channels/catálogo, cero transforms/programs C2S y separación root/joints/gravity. Alex/Citadel queda para G3.5.

## Baseline adversarial rojo — 2026-09-13

Snapshot pre-implementación: **`0258c6c619d2815afe91bcb96343d6e67998f3b0`**. Hasta él sólo hay documentación/tests/CI S18.

- Ordinary `34756921522`, job `103722768191`: **407/407**, `BUILD SUCCESSFUL`; artifact `10317093981`, SHA-256 `e1849077cade02d41604b8c59b312c150951869e4cd0ee84edc5dd1373ccf16b`.
- Focal `34756921526`, common job `103722768203`: 12 holdouts S18 + sentinel; **11 rojos**, 1 S18 verde. Lo único ya correcto es que `CollisionEngines` no expone program storage global. Artifact `10317187330`, SHA-256 `450048830cc2d5d850a0db972cdbadf514a4ba708de84e314da79989a62adc1b`.
- Mismo run, client job `103722768285`: cliente real arranca bajo Xvfb y falla únicamente porque no existe compiler `AnimationDefinition -> programa neutral`; artifact `10317897787`, SHA-256 `58552d93b1a122a815539043d4453c773969a729d92e492d4492933472767cf9`.

El baseline es red-before-green deliberado. No se rebaja ningún oracle.

## Freeze adversarial pre-implementación — 2026-09-13

Snapshot congelado: **`b39d6a1aab61eb4852ce302072ef9717b910a8dc`**. Entre el baseline anterior y este punto sólo se añadieron documentación, holdouts y la lane focal S18; **no hay cambio de producción S18**.

- Ordinary run **`34765139924`**, job **`103744660904`**: **407/407 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **`10319929687`**, SHA-256 **`42599d8cf021fe10017559db58b2292be1e705cd163442817501ee08fdd6063b`**.
- Focal run **`34765139902`**, common job **`103744660860`**: **15 tests totales**, con **11 required S18 rojos y 4 verdes**. Los rojos siguen aislando únicamente la implementación pendiente: autoridad canonical de built-ins, `PoseEngine.Inputs` live, retirada del registry legacy, paridad adapter↔engine, `mojang_keyframes`, protocolo v5/programs, runtime sin `PoseProvider` y ownership revision-local de programs. Artifact **`10320126961`**, SHA-256 **`bc722710f417a0e4e3845ca8703c51592c621569d30fd28a70cf6b9a25108f8b`**.
- Los verdes incluyen el control de que no existe almacenamiento global de programs y los dos nuevos holdouts de compatibilidad SPI: `PoseEngine` sigue teniendo **un único método abstracto (`evaluate`)** y una implementación externa mediante lambda sigue compilando/evaluando. Por tanto el seam de preparación S18 debe añadirse sin romper el SAM público.
- En el mismo focal, client job **`103744660959`** arranca el cliente real bajo Xvfb y falla exclusivamente en el oracle que exige un compiler/tooling que referencie la API de animación de Mojang antes de emitir el programa neutral. Artifact **`10320250799`**, SHA-256 **`0761925a20e4ff0738b50a169de1e534f6f8cbec55967d7e8f2bbbcca076027d`**. Los avisos headless de flite/ALSA/OpenAL/Realms no son la causa del fallo.

**La diana adversarial queda congelada aquí.** No se añadirán nuevos requisitos pre-implementación por mera expansión de tests. Los siguientes cambios adversariales deben responder a producción S18 real o sustituir un oracle provisional por otro semánticamente más preciso sin endurecer el contrato. En particular, el holdout estructural que detecta una tercera tabla revision-local en `Snapshot` es provisional: cuando exista la API `PoseProgram`, deberá convertirse en una prueba semántica de ownership de programs, sin imponer la forma concreta del record.

## Revisión adversarial post-implementación — 2026-09-14

La implementación ya existe, pero **S18 permanece abierto** hasta obtener ordinary + focal + regresión verdes sobre el estado de cierre y completar la segunda lectura sin cambios productivos.

Hallazgos causales cerrados hasta ahora:

- **Ownership semántico:** el oracle provisional que contaba una tercera `Map` en `Snapshot` se sustituyó en **`f05c742467612f813222599e87df1a33c39b466b`** por una prueba de dos revisiones que demuestra contenido `posePrograms` distinto, persistencia del snapshot anterior e inmutabilidad. Ya no prescribe la forma del record.
- **Duplicados de timestamp:** el mismo holdout expuso que `PoseProgram.Track` aceptaba dos keyframes con timestamp idéntico porque comparaba `< previous`. Producción se corrigió en **`1c2e4d503bc65e2c225a530fceeab520ea0253e7`** con validación estrictamente creciente (`<= previous` rechaza), sin más cambio productivo.
- **Adapter legacy:** el primer run common post-implementación mostró un fallo del propio oracle al reflejar una clase lambda sintética bajo Java 25. Se corrigió sólo el test en **`960ddb32630e58f0acf804ba98caef62928f1f8c`** para invocar por la interfaz pública `PoseProvider`; no fue un defecto productivo.
- **Semántica `preTarget/postTarget`:** la comparación contra `AnimationDefinition` original detectó en t=1.0 un resultado `0.175` donde Mojang da `0.0875`: el evaluator neutral devolvía `postTarget` exactamente en el keyframe en vez de `preTarget`. Producción se corrigió en **`49d828a12e9fec7d75c6b1d5ee1760ce6b7fbb7e`** distinguiendo instante exacto, tramo posterior y clamp final. En el focal **`34818424359`**, common job **`103894162131`** quedó **18/18 verde** y client job **`103894162350`** quedó verde contra la definición original, incluidos bordes, loop, Catmull-Rom, translation/rotation/scale, canales aditivos y fail-closed. Artifacts: common **`10337163876`**, SHA-256 **`098771b4944e68c1aacf6f6699df5589febae98dd58be3b97bfc4a625cb42cbe`**; client **`10336974137`**, SHA-256 **`ef501f209230699385fa36772a085080dfcc8e0c41f453fd6fb99c7ceb31dac1`**.
- **IDs canónicos de programs:** `1fbf2d71341e69bce662bc989dca32eefc490894` canoniza IDs en snapshot y rechaza alias duplicados; `9ff92bb6f6bf93844516643e1b2fcac8ca3405a6` hace lo mismo antes de serializar/hash v5; `cace9620b9b7332dcc628e73c9ff423cb0afbeb2` añade el holdout de alias/atomicidad. La ruta de recepción sigue publicando `acceptedRevision` sólo después de `replaceAtRevision`, por lo que un candidato inválido no sustituye el accepted anterior.
- **Bounds congelados:** `f0361263bcff9f96d6b9b75f20b92a5a0fa76e7a` añade N/N+1 para tracks, keyframes, número de programs y bytes v5. Son límites ya exigidos por el freeze, no requisitos nuevos.

Deuda de cierre actualmente aislada:

- El ordinary completo de **`ac50b5003dc3256cdef8ef89668ee5c0fb33b631`**, run **`34819068786`**, ejecuta **407 tests** en `Fabric Env=SERVER` y sólo falla `AnatomyGeometryTests.resourceReloadAndNetworkBundleRemainAtomic`: su `ResourceManager` de prueba devuelve el JSON legacy de perfil también para `scalebrews/pose_programs`, fabricando `test:scalebrews/pose_programs/body.json`. Producción lo rechaza correctamente como `PoseProgram` inválido. La fixture debe devolver vacío para `pose_programs`; **no** debe relajarse el parser productivo. Artifact del rojo: **`10338120008`**, SHA-256 **`05b135587fbcb451105664adf8a1a8bbe52c33c14347965b39d38edae6e1a8df`**.
- El oracle legacy de S04 que fijaba protocolo v4 ya se migró a v5 en **`aa159393704887a47d68dfb20e039ba49f3c066e`**. En el ordinary anterior era el segundo rojo; en `34819068786` ya no falla.
- Los nuevos holdouts de bounds aún requieren ejecución focal de cierre. Este cambio documental fuerza la lane S18 sin alterar producción.

No se cierra I11/I12 ni la tarea G3.4 hasta eliminar la fixture obsoleta, obtener evidence verde actualizada y realizar la segunda lectura final.

## Estado inicial

1. `PoseEngine` público existe y es SAM de `geometry + inputs + parameters`.
2. Built-ins siguen detrás de `PoseProvider/PoseProviders`.
3. `PoseProvider.Inputs` duplica `PoseEngine.Inputs` y sigue en tracker/history/payload/runtime.
4. `PoseProviders` mantiene registry y fórmulas paralelos.
5. S16 usa `legacy_pose_provider` como bridge precomputado.
6. `CollisionBinding.Pose` ya contiene `engine + parameters + channels`.
7. No existe `PoseProgram`, compiler, evaluator keyframe ni tabla de programs en catálogo.
8. Wire v4 sincroniza `{models, bindings}`.

## Estado objetivo

### 1. Una sola autoridad procedural

Los IDs `player_walking`, `quadruped`, `chicken`, `villager`, `iron_golem`, `ghast`, `feline`, `equine`, `bee` y `static` pasan a `CollisionEngines.pose(...)`.

Las fórmulas pertenecen al `PoseEngine`. `PoseProviders` puede desaparecer o sobrevivir sólo como adapter read-only **legacy -> engine** para S16: sin `register`, `Map` propio ni fórmulas duplicadas. El engine canónico no depende de `PoseProvider`.

`PoseEngine.Inputs` pasa a DTO live único en tracker, payload, history y runtime. `CollisionBinding.Pose.channels` es el contrato de channels requeridos: ausencia y cero son distintos. Estado no soportado devuelve vacío y recupera después sin freeze.

### 2. `PoseProgram` neutral

El DTO common/server-safe representa un `AnimationDefinition` sin tipos cliente:

- schema + metadata fuente/version;
- duración finita y loop explícito;
- tracks/bones canónicos;
- target (`translation`, `rotation`, `scale`);
- keyframes ordenados;
- **cada keyframe conserva `timestamp`, `preTarget` y `postTarget`** aunque el constructor abreviado use el mismo vector;
- interpolación soportada identificable;
- límites explícitos de programs/tracks/keyframes/bytes con N/N+1;
- ninguna referencia a `ModelPart`, `AnimationDefinition`, renderer, lambdas cliente ni clocks de render.

`preTarget/postTarget` es normativo: aplanarlos pierde discontinuidades válidas del formato moderno.

### 3. Compiler client/tooling

Código bajo `client.collision.preparation` lee `AnimationDefinition` original y emite `PoseProgram`. Primitiva/target/interpolación no soportada => exportación rechazada, nunca track omitido.

La equivalencia usa una definición original en varios tiempos, bordes de keyframe, loop y al menos un caso `preTarget != postTarget` si la API/fuente lo permite.

### 4. Programs son datos de revisión

`WorldAnatomyCatalog.Snapshot` posee programs junto con models/catalog/bindings. Bundle v5 = **`{models, pose_programs, bindings}`**. `AnatomyApi.PROTOCOL_VERSION = 5`; v4 es incompatible. `DATA_SCHEMA_VERSION` sólo cambia si cambia realmente binding/policy.

Programs no viven en `CollisionEngines` ni en otro singleton. Antes del swap se validan engine, program, schema/bounds, targets/interpolaciones, bones y channels. Cualquier fallo conserva exactamente el `Accepted` anterior, incluido `PreparedBundle`.

### 5. Seam de binding/preparación del SPI

Gap adversarial detectado: `PoseEngine.evaluate(...)` sólo recibe `geometry + inputs + parameters`; un `program=<id>` no puede resolverse desde ahí si la tabla es world-owned sin recurrir a estado global prohibido.

S18 añade una fase neutral de **preparación/binding por revisión** o equivalente:

- el `PoseEngine` procedural conserva compatibilidad como SAM público;
- un método `default`/seam puede recibir geometry, parameters, required channels y resolver resources de revisión para producir un evaluator ligado/inmutable;
- procedural usa por defecto `evaluate(...)` existente;
- `mojang_keyframes` resuelve program una vez durante aceptación, no mediante lookup global en hot path;
- runtime consume evaluator ligado a revisión;
- nada de thread-local/global mutable context.

El nombre concreto del evaluator no es normativo; ownership, inmutabilidad, fail-closed y compatibilidad del SAM sí.

### 6. Reloj y amplitud autoritativos del keyframe

`AnimationDefinition` describe curvas, pero **no decide qué reloj físico/autoritativo debe alimentarlas en Scale**. Mojang puede aplicar una definición como animación de estado, locomoción o pose estática. Por tanto `mojang_keyframes` no puede asumir implícitamente `age` ni ningún reloj cliente.

El binding/preparación debe fijar explícitamente, mediante parameters validados o estructura neutral equivalente:

- **fuente de tiempo**: por ejemplo age/tick autoritativo, walk phase, un channel nombrado o static; el naming concreto no es normativo;
- conversión de unidades una sola vez y documentada (ticks/phase/channel -> segundos de programa);
- cuando la semántica original lo requiera, **amplitud/scale de animación** desde `walkAmount`, constante o channel autoritativo;
- todo channel usado como clock/amplitude pasa a ser requerido por ese binding;
- selector ausente, desconocido o channel requerido ausente => `UNAVAILABLE`, nunca fallback a age/render clock;
- mismo program con clocks distintos son bindings distintos pero reutilizan el mismo engine/program data.

Esto permite reproducir tanto aplicación temporal normal como casos tipo walking/static sin convertir JSON en lenguaje procedural general.

### 7. `scalebrews:mojang_keyframes`

El engine common/dedicated evalúa sólo el programa ligado + `PoseEngine.Inputs` y clock/amplitude ya validados. Resuelve loop/clamp/keyframes determinísticamente, aplica transforms locales y valida salida con `ModelGeometry.transforms(...)`.

No consulta resources locales, renderer, `AnimationDefinition`, `ModelPart` ni reloj cliente.

## Plan convergido

- [ ] **I1** Migrar runtime live a `PoseEngine.Inputs`.
- [ ] **I2** Mover built-ins vanilla a `CollisionEngines.pose(...)` como owners únicos.
- [ ] **I3** Eliminar o reducir `PoseProviders` a adapter legacy read-only.
- [ ] **I4** Required channels/unsupported state fail-closed + recovery.
- [ ] **I5** `PoseProgram` neutral bounded con `preTarget/postTarget`.
- [ ] **I6** Seam de preparación/binding por revisión preservando el SAM público.
- [ ] **I7** Clock/amplitude selectors explícitos y server-authoritative.
- [ ] **I8** Catálogo/bundle `{models, pose_programs, bindings}`, validación pre-swap y protocolo v5.
- [ ] **I9** Compiler client/tooling `AnimationDefinition -> PoseProgram`.
- [ ] **I10** `mojang_keyframes` sobre evaluator ligado/server-safe y proof original a múltiples tiempos.
- [ ] **I11** Holdouts malformed/N+1/channels/freeze/order/repeat/v4-v5/atomicidad y lanes ordinary/common/dedicated/client.
- [ ] **I12** Segunda lectura completa hasta cero cambios productivos.

## Modelo adversarial

### Ownership / SPI

- engines vanilla no dependen de owner legacy;
- adapter legacy, si existe, no tiene registry propio;
- ningún singleton almacena programs;
- keyframe program se resuelve por binding de revisión;
- añadir programs no rompe lambdas/engines externos procedurales de S02.

### Inputs / procedural

- maps defensivamente copiados;
- required ausente != cero;
- extra channel irrelevante no altera resultado;
- parameters desconocidos fail-closed;
- `ordinary=false` vacío + recovery;
- repeat exacto;
- family/model/version no se reconoce por coincidencia accidental de nombres.

### Program / clock validation

- NaN/∞, duración inválida, timestamp fuera de rango, track vacío, duplicados, orden inválido o N+1 => rechazo;
- `preTarget/postTarget` ambos preservados/validados;
- bone inexistente/ambiguo y target/interpolación no soportado => rechazo;
- clock/amplitude selector desconocido o channel requerido ausente => `UNAVAILABLE`;
- no existe default implícito a `age` cuando el binding no declara clock;
- maps/resources equivalentes producen bytes/hash/evaluación canónicos iguales.

### Evaluation

- t=0, t=duración, antes/después de keyframe, discontinuidad pre/post, loop wrap;
- translation/rotation/scale preservan unidades/rest pose;
- clocks age/walk/channel/static tienen oráculos separados;
- linear y splines soportadas se comparan contra fuente original;
- programa no escribe root/world ni bones ajenos.

### Wire / lifecycle estático

- v4/v5 incompatibles;
- fragments mezclados no publican;
- program inválido conserva **mismo objeto** snapshot + bundle;
- replacement válido nueva revisión invalida material anterior;
- cliente nunca usa program local fallback.

### Classloading

- common sin referencias de tipos client animation;
- compiler sólo client preparation;
- dedicated evalúa fixture sin cargar cliente.

## Criterio de cierre

S18 sólo cierra si:

1. built-ins vanilla son `PoseEngine` reales con autoridad única;
2. input live único = `PoseEngine.Inputs`;
3. program conserva semántica relevante, incluido pre/post target;
4. program world-owned se liga por revisión sin global state y sin romper el SAM S02;
5. clock/amplitude son explícitos y autoritativos;
6. `mojang_keyframes` funciona en dedicated sin clases cliente;
7. v5 conserva atomicidad/reuse/rejection S15/S16;
8. unsupported/missing produce `UNAVAILABLE` y recovery sin freeze;
9. procedural vanilla + una definición original quedan equivalentes;
10. ordinary + common/dedicated + client compiler/original-source + holdouts verdes;
11. segunda lectura final produce cero cambios de producción.
