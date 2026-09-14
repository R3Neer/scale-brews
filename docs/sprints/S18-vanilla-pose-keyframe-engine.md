# S18 — Vanilla PoseEngine + Mojang keyframe program

Estado: **REABIERTO / G3.4 PENDIENTE POR PARIDAD MOJANG**.

## Tesis

El objetivo original de S18 es cerrar G3 tarea 4: las fórmulas vanilla pasan a ser `PoseEngine` canónicos con una sola autoridad de comportamiento y se añade `scalebrews:mojang_keyframes`, capaz de ejecutar en common/dedicated un programa neutral compilado desde `AnimationDefinition`.

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

La implementación se sometió a una segunda ronda adversarial antes del cierre. Los rojos que aparecieron se resolvieron por causa, sin relajar los contratos productivos.

Hallazgos causales cerrados:

- **Ownership semántico:** el oracle provisional que contaba una tercera `Map` en `Snapshot` se sustituyó en **`f05c742467612f813222599e87df1a33c39b466b`** por una prueba de dos revisiones que demuestra contenido `posePrograms` distinto, persistencia del snapshot anterior e inmutabilidad. Ya no prescribe la forma del record.
- **Duplicados de timestamp:** el mismo holdout expuso que `PoseProgram.Track` aceptaba dos keyframes con timestamp idéntico porque comparaba `< previous`. Producción se corrigió en **`1c2e4d503bc65e2c225a530fceeab520ea0253e7`** con validación estrictamente creciente (`<= previous` rechaza).
- **Adapter legacy:** el primer run common post-implementación mostró un fallo del propio oracle al reflejar una clase lambda sintética bajo Java 25. Se corrigió sólo el test en **`960ddb32630e58f0acf804ba98caef62928f1f8c`** para invocar por la interfaz pública `PoseProvider`; no fue un defecto productivo.
- **Semántica `preTarget/postTarget`:** la comparación contra `AnimationDefinition` original detectó en t=1.0 un resultado `0.175` donde Mojang da `0.0875`: el evaluator neutral devolvía `postTarget` exactamente en el keyframe en vez de `preTarget`. Producción se corrigió en **`49d828a12e9fec7d75c6b1d5ee1760ce6b7fbb7e`** distinguiendo instante exacto, tramo posterior y clamp final.
- **IDs canónicos de programs:** `1fbf2d71341e69bce662bc989dca32eefc490894` canoniza IDs en snapshot y rechaza alias duplicados; `9ff92bb6f6bf93844516643e1b2fcac8ca3405a6` hace lo mismo antes de serializar/hash v5; `cace9620b9b7332dcc628e73c9ff423cb0afbeb2` añade el holdout de alias/atomicidad.
- **Bounds congelados:** `f0361263bcff9f96d6b9b75f20b92a5a0fa76e7a` añade N/N+1 para tracks, keyframes, número de programs y bytes v5. Son límites ya exigidos por el freeze, no requisitos nuevos.
- **Fixture ordinaria histórica:** la suite completa detectó que `AnatomyGeometryTests.resourceReloadAndNetworkBundleRemainAtomic` fabricaba accidentalmente un JSON legacy dentro del nuevo namespace `scalebrews/pose_programs`. Producción lo rechazaba correctamente. La fixture se corrigió en **`01d46f348041bf511e61db00c287a146679da92b`** para devolver vacío en `pose_programs`; el parser productivo no se relajó.
- **Oracle de protocolo histórico:** el test S04 que fijaba literalmente protocolo v4 se migró a v5 en **`aa159393704887a47d68dfb20e039ba49f3c066e`**.

## Cierre histórico — 2026-09-14

Este cierre ocurrió y la evidencia fue válida para la cobertura disponible entonces, pero quedó **posteriormente invalidado como cierre de G3.4** por la auditoría retroactiva descrita más abajo.

- **Focal S18 final histórico:** run **`34819384389`**. Common job **`103897173738`**: **21/21 required S18 GameTests passed**, incluidos ownership revision-local, IDs canónicos, duplicados de timestamp y límites N/N+1; artifact **`10337721645`**, SHA-256 **`4275b29568f179353825ade70191893325118330f6b076bfbbabae0cb703b09f`**. Client job **`103897173936`**: verde con `S18_ANIMATION_COMPILER_BOUNDARY PASS` y `S18_ANIMATION_DEFINITION PASS`, comparando contra `AnimationDefinition` original de Minecraft 26.2; artifact **`10337647129`**, SHA-256 **`50df0d9d0050009b08afc1f4e85a65da6a811e6a89bdea8971e534b2d7ec8cce`**.
- **Ordinary completo histórico:** run **`34819573289`**, job **`103897783223`**: **407/407 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **`10337702118`**, SHA-256 **`d12a251fd777d0e5bfd41c63585f99ffbdf9d4eca3a8eef7ba55da947444a741`**.
- **Build normal histórico:** run **`34819573066`**, job **`103897782597`**: `./gradlew build` verde, vuelve a ejecutar **407/407 required GameTests**, `BUILD SUCCESSFUL`; artifact **`10336964165`**, SHA-256 **`4f4699a8e336703bfe29b54dde8b3c3954d9c9cf44626184cef26d86b89015c8`**.
- **Segunda lectura / I12 histórica:** desde el último cambio productivo S18 **`9ff92bb6f6bf93844516643e1b2fcac8ca3405a6`** hasta el checkpoint de cierre **`22b1803784b7792163b881ae49254efaa2dd8c2c`** sólo cambiaron tests, documentación y CI; no cambió ningún archivo bajo `src/main` ni `src/client`. La revisión final produjo **cero cambios de producción** bajo los oracles entonces existentes.
- La lane extraordinaria usada para demostrar la suite completa se eliminó tras capturar la evidencia en **`bbd7dc3b4dd150621e567237539022dd9737511f`**.

## Reapertura retroactiva — 2026-09-14

La auditoría adversarial posterior encontró que el oracle cliente original era favorable: la pieza `head` no ejercitaba una composición de reposo suficientemente exigente. Al ampliar la comparación a `body`, S18 dejó de reproducir la semántica real de `ModelPart`.

Hallazgos de la reapertura:

1. **ROTATION inicial:** `PoseProgramEvaluator` componía `Q(rest) * Q(delta)`, mientras Minecraft 26.2 ejecuta `offsetRotation` sobre `xRot/yRot/zRot` y después construye un único `rotationZYX(restEuler + deltaEuler)`. El nuevo oracle original-source hizo rojo G3.4.
2. **SCALE no unitario:** la inspección exacta de `ModelPart.offsetScale` y `KeyframeAnimations.scaleVec` mostró que SCALE es aditivo alrededor de 1. El holdout `S18NonUnitRestScaleSemanticClientTests` produjo `expected=1.6999999 actual=1.8` sobre un `CowModel` real con rest scale no unitario. Evidencia detallada en `S18-target-semantics-evidence.md`.
3. **Fix parcial del IMPLEMENTADOR:** `7733dfceee00bc4601316ae66240a7ba1ad97645` corrigió SCALE y sustituyó la multiplicación quaternion por una descomposición Euler del quaternion de reposo. Run `34839362564`: common, client semantic y SCALE quedaron verdes.
4. **Holdout post-fix de identidad Euler:** `S18EulerRepresentativeSemanticClientTests` demuestra que dos triples Euler distintos pueden colapsar a la misma matriz de reposo y, sin embargo, separarse después bajo el mismo `offsetRotation` aditivo de Mojang. La representación neutral actual sólo conserva la matriz y no puede distinguir ambos estados fuente.
5. **Resultado aislado:** run `34839609532`. `common-authority`, `client-compiler-boundary` y `client-non-unit-rest-scale` verdes; `client-euler-representative` job `103961255753` rojo con `expected=0.510475 actual=0.69862896`. Artifact `10345492485`, SHA-256 `bbc9d485aa66de37a698858b4ba64a95c4a0386e720435a97fd2b6b379473eaa`.

Clasificación TM actual: **gap de arquitectura/modelo de datos en la frontera geometry↔pose de `mojang_keyframes`**. El material neutral server-safe debe conservar información suficiente para reproducir la semántica aditiva de los fields `ModelPart` originales, sin clases cliente ni estado global. Este documento no prescribe si la solución concreta pertenece a geometry, program o material preparado; esa decisión vuelve a arquitectura/IMPLEMENTADOR.

Por tanto **G3.4 permanece abierto**. El cierre histórico no autoriza avanzar el estado global como si esta paridad estuviera resuelta, y S19/G3.5 no puede declararse cerrado por delante de esta dependencia.

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

## Plan convergido histórico

- [x] **I1** Migrar runtime live a `PoseEngine.Inputs`.
- [x] **I2** Mover built-ins vanilla a `CollisionEngines.pose(...)` como owners únicos.
- [x] **I3** Eliminar o reducir `PoseProviders` a adapter legacy read-only.
- [x] **I4** Required channels/unsupported state fail-closed + recovery.
- [x] **I5** `PoseProgram` neutral bounded con `preTarget/postTarget`.
- [x] **I6** Seam de preparación/binding por revisión preservando el SAM público.
- [x] **I7** Clock/amplitude selectors explícitos y server-authoritative.
- [x] **I8** Catálogo/bundle `{models, pose_programs, bindings}`, validación pre-swap y protocolo v5.
- [x] **I9** Compiler client/tooling `AnimationDefinition -> PoseProgram`.
- [x] **I10** `mojang_keyframes` sobre evaluator ligado/server-safe y proof original a múltiples tiempos.
- [x] **I11** Holdouts malformed/N+1/channels/freeze/order/repeat/v4-v5/atomicidad y lanes ordinary/common/dedicated/client.
- [x] **I12** Segunda lectura completa hasta cero cambios productivos bajo la representación/oracles entonces disponibles.

La reapertura no convierte estos pasos históricos en “no implementados”; demuestra que **I10/I12 no bastaron para probar paridad general**. El trabajo correctivo vuelve a arquitectura/IMPLEMENTADOR y requerirá una nueva pasada de cierre antes de volver a marcar G3.4 completado.

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
- programa no escribe root/world ni bones ajenos;
- una representación neutral no puede tratar dos rest states de `ModelPart` como equivalentes si la semántica aditiva del target puede distinguirlos después.

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
9. procedural vanilla + una definición original quedan equivalentes **también para rest transforms cuya semántica no puede reconstruirse ambiguamente desde una matriz**;
10. ordinary + common/dedicated + client compiler/original-source + holdouts verdes;
11. segunda lectura final produce cero cambios de producción.
