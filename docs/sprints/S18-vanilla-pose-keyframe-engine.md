# S18 — Vanilla PoseEngine + Mojang keyframe program

Estado: **PLAN CERRADO / IMPLEMENTACIÓN PENDIENTE**.

## Baseline adversarial rojo — 2026-09-13

El baseline final pre-implementación queda fijado en **`0258c6c619d2815afe91bcb96343d6e67998f3b0`**. Todos los commits desde la apertura de S18 hasta ese snapshot son documentación, tests o CI; no hay cambios productivos S18.

- Ordinary run **`34756921522`**, job **`103722768191`**: **407/407 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **`10317093981`**, SHA-256 **`e1849077cade02d41604b8c59b312c150951869e4cd0ee84edc5dd1373ccf16b`**. Esto demuestra que los holdouts S18 están aislados de la suite ordinaria y que todo el source set compila.
- Focal S18 run **`34756921526`**, common job **`103722768203`**: 13 tests totales = 12 holdouts S18 + `minecraft:always_pass`; **11 holdouts S18 rojos**, 1 S18 verde y el sentinel verde. El único contrato S18 ya satisfecho antes de implementar es que `CollisionEngines` no expone almacenamiento global de pose programs. Los rojos restantes corresponden exactamente a las deudas declaradas: engines vanilla todavía legacy, DTO live duplicado, registry `PoseProviders`, runtime dependiente de `PoseProvider`, ausencia de `mojang_keyframes`, snapshot sin programs y protocolo/bundle aún v4. Artifact **`10317187330`**, SHA-256 **`450048830cc2d5d850a0db972cdbadf514a4ba708de84e314da79989a62adc1b`**.
- Mismo focal run, client job **`103722768285`**: main/client/GameTest compilan y Minecraft 26.2 arranca realmente bajo Xvfb; el job cae **únicamente** en `S18AnimationDefinitionCompilerClientTests` porque todavía no existe una ruta `client.collision.preparation` que lea `AnimationDefinition` y emita un programa neutral. Artifact **`10317897787`**, SHA-256 **`58552d93b1a122a815539043d4453c773969a729d92e492d4492933472767cf9`**. El ruido headless de narrator/flite, ALSA/OpenAL y servicios de cuenta es ambiental y no es la causa contractual del rojo.

Este baseline es intencionadamente red-before-green. No autoriza a rebajar ningún oracle: la implementación debe volver verdes esas fronteras preservando el ordinary 407/407 o superior.

## Tesis

S18 cierra G3 tarea 4: convertir las fórmulas vanilla que hoy viven detrás de `PoseProvider/PoseProviders` en **`PoseEngine` canónicos**, con una sola autoridad de comportamiento, y añadir un engine general `scalebrews:mojang_keyframes` capaz de ejecutar en common/dedicated un programa neutral exportado desde `AnimationDefinition`.

El renderer cliente puede ser una fuente de tooling para compilar/probar programas; nunca es autoridad de pose física. Servidor y cliente de presentación consumen los mismos inputs autoritativos y el mismo programa aceptado por la revisión del catálogo.

## Scope

### Gate

- G3 tarea 4: consolidar engines de pose vanilla y engine general de `AnimationDefinition`.

### Requisitos incluidos

- **FR-025**: `PoseEngine` reusable por familia a partir de datos server-safe.
- **FR-026**: comportamiento en engines; selección, channels, parámetros y estados en datos.
- **FR-027**: vocabulario de channels autoritativos extensible por engine.
- **FR-028**: keyframes evaluados desde autoridad/reproducción server-safe, nunca desde renderer cliente.
- **FR-029**: pose/channel/primitiva no soportada publica `UNAVAILABLE`, no congela la última geometría.
- **FR-033/FR-034**: bindings seleccionan engines registrados y programas/referencias válidos.
- **FR-035/FR-036/FR-037**: programa y binding pertenecen a una misma revisión atómica; candidato inválido conserva exactamente la revisión previa.
- **FR-089/FR-090**: estados no verificados siguen sin collider y recuperan al volver a un estado soportado.
- **NFR-001/NFR-002**: evaluación determinista e independiente de orden accidental.
- **NFR-003/NFR-004**: validación estricta y fail-closed localizado.
- **NFR-005**: common/dedicated no referencia `AnimationDefinition`, renderer ni otras clases cliente.
- **NFR-010/NFR-012/NFR-013**: bundle preparado/reutilizado, límites e integridad continúan aplicando a la revisión ampliada.
- **NFR-028/NFR-029/NFR-031**: programas reproducibles y equivalencia contra fuentes originales mediante lanes reales.

### Invariantes heredados

- S15: una revisión se serializa/hash/fragmenta una vez antes del swap y se reutiliza por receptor.
- S16: el candidato completo se valida antes de publicación; `BINDING` falla cerrado y un rechazo conserva el snapshot aceptado anterior.
- S17: geometría `ModelPart` preparada y pose son owners distintos; S18 no vuelve a leer renderers/model trees en runtime server.
- El servidor sigue siendo la única autoridad de channels, disponibilidad y catálogo.
- El cliente no envía matrices ni programas de pose por C2S.
- Root TRS/gravedad siguen fuera de `PoseEngine` y pertenecen a G3.6/integración existente.
- Alex/Citadel queda fuera de este sprint y continúa en G3 tarea 5.

### Exclusiones explícitas

- No convertir `AdvancedModelBox`/Citadel/Alex; S19/G3.5.
- No generalizar root transforms; G3.6.
- No implementar scanner/coverage ni lifecycle general; G3.8–11.
- No integrar prediction/reconciliation G4.
- No retirar el bridge precomputado de S16 todavía: puede adaptarse al nuevo owner, pero la ejecución arbitraria de bindings no debe forzarse mediante fallback.
- No convertir JSON de binding en un lenguaje procedural. El binding referencia comportamiento/programas validados.

## Estado inicial reconstruido

1. `collision.api.spi.PoseEngine` ya existe y acepta `ModelGeometry + Inputs + parameters`, pero los built-ins vanilla no están registrados ahí.
2. `PoseProvider.Inputs` y `PoseEngine.Inputs` duplican prácticamente el mismo DTO server-safe.
3. `AuthorityPoseTracker`, `AnatomyPosePayload`, `AnatomyPoseHistory` y `ModelGeometryProvider` siguen hablando `PoseProvider.Inputs`.
4. `PoseProviders` mantiene un segundo registry common con los IDs `player_walking`, `quadruped`, `chicken`, `villager`, `iron_golem`, `ghast`, `feline`, `equine`, `bee` y `static`.
5. `PlayerWalkingPose`, `QuadrupedPose` y `VanillaFamilyPose` implementan `PoseProvider`, no `PoseEngine`.
6. El bridge S16 resuelve `legacy_pose_provider` mediante un parámetro `provider=<id>` y llama a `PoseProviders.find(...)`.
7. `CollisionBinding.Pose` ya contiene `engine`, `parameters` y `channels`; no hace falta otro schema para seleccionar programa ni declarar channels requeridos.
8. `PoseChannels` conoce un conjunto global de channels continuos, pero la física ya interpola transforms de endpoints evaluados; no es necesario convertir el mapa de channels en otra autoridad de animación.
9. No existe hoy ningún compiler, DTO, catálogo ni evaluador de `AnimationDefinition`/keyframes.
10. El bundle v4 transporta `{models, bindings}`; un programa keyframe world-owned nuevo no puede aparecer localmente en cliente sin entrar en la misma revisión autoritativa.

## Estado objetivo

### Autoridad única de poses vanilla

- Los IDs vanilla reutilizables existentes pasan a ser IDs de `CollisionEngines.pose(...)`, no un registry paralelo de algoritmos.
- Las fórmulas tienen **un solo owner** implementado como `PoseEngine`.
- `PoseProviders` puede sobrevivir sólo como adapter explícito del bridge legacy S16; no contiene fórmulas ni un segundo registry de comportamiento.
- El adapter legacy delega por ID al `PoseEngine` canónico y conserva fail-closed. Registrar/seleccionar un engine nuevo no crea por accidente otro provider legacy.
- `PoseEngine.Inputs` se convierte en el DTO autoritativo de channels para tracker/history/payload/evaluación. Si `PoseProvider.Inputs` sobrevive transitoriamente, queda confinado al adapter de compatibilidad y no es el tipo emitido por la autoridad live.

### Procedural vanilla

- `player_walking`, `quadruped`, `chicken`, `villager`, `iron_golem`, `ghast`, `feline`, `equine`, `bee` y `static` mantienen semántica/IDs compatibles donde sean families reales, pero pasan por el registry público `CollisionEngines`.
- Una familia reconoce explícitamente los modelos/versiones/parts que soporta; no transforma por coincidencia accidental de cinco nombres.
- `ordinary=false` y estados declarados incompatibles devuelven vacío.
- `CollisionBinding.Pose.channels` define los channels requeridos por ese binding. Un required channel ausente produce `UNAVAILABLE`; un channel opcional sólo puede usar un default documentado/neutral.
- Un mob nuevo cubierto por una familia existente se añade mediante binding/data, no una clase Java nueva.

### Programa keyframe neutral

Se introduce un DTO common/server-safe, denominado aquí **`PoseProgram`** (el nombre concreto puede variar), que representa sólo primitivas necesarias para ejecutar un `AnimationDefinition` exportado:

- schema/version y fuente/version metadata;
- duración finita y semántica de loop explícita;
- tracks identificados de forma determinista;
- target soportado explícito (`translation`, `rotation`, `scale` o equivalente canónico);
- keyframes ordenados con tiempo y vector finitos;
- interpolación soportada declarada (`linear`, `catmull_rom` si la equivalencia real la valida);
- ninguna referencia a `ModelPart`, `AnimationDefinition`, lambdas cliente o renderer.

Los límites de programs/tracks/keyframes/tamaño son constantes explícitas y tienen tests N/N+1. No se fijan aquí números arbitrarios: la implementación debe escoger límites razonables antes de aceptar datos y registrarlos en código/tests/documentación.

Un compiler **client/tooling-only** convierte una `AnimationDefinition` original a `PoseProgram`; el compiler valida/normaliza unidades y semántica de target. Una primitiva/interpolación no soportada falla la exportación: nunca se omite silenciosamente.

### Storage y autoridad de programas

El programa exportado es **dato de catálogo**, no recurso local del renderer. `WorldAnatomyCatalog` acepta una colección versionada de `PoseProgram` junto con modelos y bindings; `CollisionBinding.Pose.parameters` referencia el programa mediante un ID canónico (`program=<namespace:id>`).

El candidato valida antes del swap:

1. engine registrado;
2. program requerido existente;
3. schema/bounds/finitud/orden de keyframes;
4. targets/interpolaciones soportados;
5. parts/bones resolubles de forma inequívoca para el modelo seleccionado;
6. channels requeridos dentro de límites y semántica declarada.

El bundle pasa de `{models, bindings}` a **`{models, pose_programs, bindings}`**. Es un cambio incompatible del contrato wire: S18 debe subir `AnatomyApi.PROTOCOL_VERSION` a **5** y no fingir compatibilidad v4. `DATA_SCHEMA_VERSION` sólo cambia si cambia el schema público de binding/policy; añadir un objeto de catálogo/wire no obliga por sí solo a incrementarlo.

La extensión conserva la propiedad S15: models + programs + bindings se serializan/hash/fragmentan como una sola revisión antes del swap. El cliente nunca observa programa de una revisión y binding de otra.

### `scalebrews:mojang_keyframes`

El engine common `scalebrews:mojang_keyframes`:

- selecciona el program ID desde parámetros validados;
- evalúa exclusivamente `PoseEngine.Inputs`/channels autoritativos;
- no consulta reloj/render state cliente;
- resuelve loop/clamp y fronteras de keyframe de forma determinista;
- compone únicamente transforms locales; root/world TRS no entra aquí;
- devuelve vacío si program/model/channel/target no es compatible;
- produce matrices finitas y completas que `ModelGeometry.transforms(...)` valida antes de cache/publicación.

## Plan de implementación convergido

- [ ] **I1** Elegir `PoseEngine.Inputs` como DTO live único y migrar tracker, payload, history y evaluación interna sin cambiar los bits wire salvo el bump de protocolo exigido por programs.
- [ ] **I2** Registrar los built-ins vanilla en `CollisionEngines.pose(...)` con IDs reusable-families y mover las fórmulas a `PoseEngine` sin duplicarlas.
- [ ] **I3** Reducir `PoseProviders` a adapter legacy read-only hacia `CollisionEngines`; retirar su registry/fórmulas como autoridad independiente y preservar el bridge S16.
- [ ] **I4** Fijar required-channel semantics desde `CollisionBinding.Pose.channels` y fail-closed para channel indispensable ausente/estado no ordinario, con recuperación posterior sin pose frozen.
- [ ] **I5** Añadir `PoseProgram` common con codec/validación estrictos, límites explícitos, orden canónico e igualdad/reproducibilidad.
- [ ] **I6** Extender `WorldAnatomyCatalog`/bundle a `{models, pose_programs, bindings}`, validar referencias completas antes del swap y subir protocol a v5 conservando atomicidad/reuse S15/S16.
- [ ] **I7** Añadir compiler client/tooling de `AnimationDefinition` a `PoseProgram`, sin referencias cliente en DTO/evaluator/common; soportar sólo targets/interpolaciones demostrados.
- [ ] **I8** Implementar `scalebrews:mojang_keyframes` en common/dedicated y probar loop, keyframe boundaries, TRS targets, orden de tracks y failure semantics.
- [ ] **I9** Migrar proof vanilla a `PoseEngine` y añadir al menos un `AnimationDefinition` real/fixture original comparado a múltiples tiempos contra el evaluador server-safe compilado.
- [ ] **I10** Añadir holdouts de classloading, required channels, unsupported primitive/interpolation, ambiguous/missing bone, malformed program, stale-pose non-reuse, recovery, order permutation, exact repeat y v4/v5 incompatibility.
- [ ] **I11** Ejecutar ordinary + lane focal common/dedicated + client original-definition compiler/equivalence y revisar S15/S16 regressions sobre v5.
- [ ] **I12** Segunda lectura completa; S18 sólo cierra con una pasada final que no requiera cambios productivos.

## Revisión iterativa del plan

### Pasada 1 — eliminar doble owner, no envolverlo

Se descarta registrar `PoseEngine` que simplemente llame a un `PoseProvider` con fórmulas propias que sigan registradas aparte. Eso conservaría dos registries/identidades y haría imposible saber qué comportamiento valida un binding. El engine canónico posee la fórmula; el provider legacy, si existe, es adapter unidireccional hacia él.

### Pasada 2 — un solo DTO de inputs live

`PoseProvider.Inputs` y `PoseEngine.Inputs` duplicados no son una frontera estable. La autoridad server, payload/history y evaluator deben converger en `PoseEngine.Inputs`; un tipo legacy puede sobrevivir sólo dentro del adapter y sin convertirse otra vez en contrato wire/runtime.

### Pasada 3 — `AnimationDefinition` no cruza common

Importar o retener `AnimationDefinition` en common violaría NFR-005 y FR-028 aunque ocurriera sólo durante init. La fuente original pertenece a tooling cliente; el resultado compilado es un DTO neutral que dedicated puede evaluar sin classloading cliente.

### Pasada 4 — el programa es autoridad de catálogo

Conservar el program sólo en recursos locales del cliente permitiría que server y client usaran definiciones distintas bajo el mismo binding/revision. El program se integra en el snapshot/bundle server-owned. Como v4 no exige/entiende ese objeto, se sube el protocolo a v5 en vez de aceptar peers que no pueden reproducir la revisión.

### Pasada 5 — channels no son defaults universales

Los `channel(name, 0)` existentes son válidos sólo cuando cero es un default neutral **y el binding no declara el channel obligatorio**. FR-029 exige distinguir ausencia de un channel indispensable de un valor cero auténtico. El binding ya posee el set de channels y se usa como contrato requerido.

### Pasada 6 — physical interpolation sigue entre endpoints evaluados

S18 no vuelve a interpolar arbitrariamente inputs para física. `HierarchyMotion` conserva interpolación continua entre transforms de dos endpoints autoritativos. El keyframe engine evalúa cada endpoint a su tiempo autoritativo; `PoseChannels.interpolate(...)` no se convierte en otro reloj físico.

### Pasada completa final del plan

No se encontró una alternativa que mantenga simultáneamente world/server authority, equivalencia cliente, dedicated classloading y atomicidad sin sincronizar el programa neutral. El scope queda limitado a pose/program/catalog estático; lifecycle general y ejecución arbitraria live continúan en las tareas G3 posteriores.

## Modelo adversarial previo

### Ownership / registries

- un ID vanilla no puede resolver a algoritmos independientes en `PoseProviders` y `CollisionEngines`;
- el adapter legacy debe producir exactamente la salida del engine canónico para los mismos inputs;
- registrar un engine duplicado sigue fallando y no cambia el owner previo;
- un binding directo `pose.engine=scalebrews:quadruped` no pasa por `legacy_pose_provider`.

### Inputs / channels

- mapa de channels mutado después de construir inputs no cambia el endpoint;
- required channel ausente => vacío, no default;
- required channel presente con `0` se distingue de ausencia;
- channel desconocido adicional no altera una familia que no lo consume;
- `ordinary=false` => vacío y el siguiente endpoint ordinario válido recupera sin reutilizar matrices stale.

### Formula families

- player wide/slim reconocen sólo versiones/fuentes soportadas;
- quadruped no acepta por accidente un modelo cualquiera que casualmente tenga `head + four legs`;
- chicken/villager/golem/feline/equine/bee conservan sus channels existentes y equivalencia de fuente;
- permutations de part order producen las mismas matrices por ID;
- salida con joint desconocido/no finito se rechaza antes de cache.

### Program validation

- NaN/∞ en duración, tiempo o vector => candidato inválido;
- duración <=0, keyframes fuera de rango, track vacío inválido, ID duplicado o límites N+1 => rechazo antes del swap;
- bone inexistente o ambiguo => program/binding no ejecutable y candidato inválido cuando la referencia deba resolverse;
- target/interpolación no soportado => export/aceptación falla; nunca se omite el track;
- orden de maps/resources/keyframes equivalente produce bytes/hash/evaluación canónicos idénticos.

### Evaluation semantics

- t=0, t=duración, justo antes/después de keyframe y loop wrap tienen oráculos explícitos;
- linear/catmull-rom se comparan con la fuente original dentro de tolerancia documentada;
- translation/rotation/scale preservan unidades y composición de rest pose;
- mismo program + geometry + inputs produce matrices bitwise/estructuralmente iguales en repeats donde JOML lo permita;
- un programa no puede escribir root/world transform ni parts fuera de su modelo.

### Wire / atomicity

- v4 y v5 son incompatibles explícitamente;
- fragmentos v5 mezclados entre revisiones/epochs no publican nada;
- program inválido de revisión nueva conserva **exactamente** snapshot + prepared bundle aceptados;
- reemplazo válido de un program con mismo ID pero nueva revisión invalida material anterior por identidad de revisión;
- client nunca usa un program local ausente/diferente como fallback.

### Classloading

- constant pool de DTO/catalog/evaluator common no contiene `net.minecraft.client`, `AnimationDefinition`, renderer ni compiler client;
- dedicated registra engines y decodifica/evalúa un program fixture sin cargar clases cliente;
- compiler client puede desaparecer del classpath server sin alterar evaluación.

### No freeze / recovery

- missing required channel, unsupported state o invalid program endpoint publica `UNAVAILABLE`;
- el provider/evaluator no devuelve la última pose válida como sustituto;
- al recuperar channels/state/program válido, un endpoint posterior vuelve a materializarse con nueva identidad causal sin rebind fantasma.

## Criterio de cierre

S18 sólo puede cerrarse si:

1. los built-ins vanilla son `PoseEngine` reales con una sola autoridad de fórmula y el bridge legacy es sólo adapter;
2. el input live autoritativo converge en `PoseEngine.Inputs`;
3. `scalebrews:mojang_keyframes` ejecuta un programa neutral en dedicated sin clases cliente;
4. programas forman parte de la revisión server-owned y v5 conserva atomicidad/reuse/rejection de S15/S16;
5. missing/unsupported pose data produce `UNAVAILABLE` y recuperación posterior sin freeze;
6. procedural vanilla y al menos un `AnimationDefinition` real/fixture quedan equivalentes a la fuente original;
7. ordinary + common/dedicated + client compiler/original-source + holdouts adversariales quedan verdes;
8. una segunda lectura completa final produce cero cambios de producción.
