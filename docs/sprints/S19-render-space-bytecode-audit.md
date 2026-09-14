# S19 renderer-space bytecode audit

Estado: evidencia adversarial de la familia AdvancedModelBox fijada para S19. Este documento no cambia producción ni requisitos; aclara el oracle de espacio de render de Grizzly/Gazelle.

## Input fijado

La inspección se realizó sobre el artefacto exacto de la lane `external-input-contract`:

- `alexsmobs-2.1.9-fabric+26.2.jar`
- mod id `alexsmobs`
- SHA-256 `b810e1b7ac925f1f1ba6a2a87e742e30b33fafad4d164ff64192f21f46f6154c`
- Minecraft `26.2`

El jar se obtuvo del artifact producido por el workflow S19, no de una dependencia ordinaria del mod.

## Orden real del transform de `LivingEntityRenderer`

`javap -c -p` sobre `com.github.alexthe666.alexsmobs.client.render.compat.LivingEntityRenderer` muestra, dentro de `render(...)`, este orden observable sobre el mismo `PoseStack` antes de `model.renderToBuffer(...)`:

1. aplica el `AMRenderState.scale` general del entity;
2. ejecuta `setupRotations(...)`;
3. aplica `PoseStack.scale(-1, -1, 1)`;
4. llama al hook virtual `scale(entity, poseStack, partialTick)`;
5. aplica `PoseStack.translate(0, -1.501, 0)`;
6. ejecuta `model.setupAnim(...)` y después `model.renderToBuffer(...)`.

Por tanto, para el estado adulto ordinario usado por el proof estructural, la porción model-space común relevante es `flip -> renderer-specific scale -> base translation`. La traslación `-1.501` ocurre **después** del hook específico, no antes.

## `RenderGazelle`

`javap -c -p` sobre `com.github.alexthe666.alexsmobs.client.render.RenderGazelle` demuestra que su override `scale(EntityGazelle, PoseStack, float)` sólo ejecuta:

- `PoseStack.scale(0.8, 0.8, 0.8)`.

Por ello el transform adulto de fuente para Gazelle debe representar, en el mismo orden que el renderer:

`scale(-1,-1,1) -> scale(0.8,0.8,0.8) -> translate(0,-1.501,0)`.

Grizzly no introduce ese `0.8` en su hook y conserva la ruta común `scale(-1,-1,1) -> translate(0,-1.501,0)`.

## Estado de edad oculto en `ModelGazelle`

La auditoría encontró además un defecto del oracle anterior, no de producción:

- `com.github.alexthe666.alexsmobs.client.render.compat.EntityModel` inicializa `young = true` en su constructor;
- `ModelGazelle.renderToBuffer(...)` comprueba `young`;
- en la rama `young=true`, escala temporalmente la cabeza/cuerno, activa scale-children en la cabeza y aplica al `PoseStack` un `scale(0.5,0.5,0.5)` seguido de `translate(0,1.5,0.125)` antes de renderizar;
- en la rama adulta no existe ese transform externo de cría.

El proof real anterior forzaba `young=false` para Grizzly pero no para Gazelle. Por tanto comparaba la extracción estructural neutral/adulta contra un render de Gazelle bebé. La ejecución roja de `030bfef828eb5f50f12eefa777b6fd0df7bf8b54` se clasifica como **oracle inválido por estado de edad y composición de transform**, no como defecto de producción.

`6bb261e630668881f6160c856927d8ce3d3cebc7` normaliza ambos modelos a estado adulto antes de extraer/renderizar para separar esa variable.

## Consecuencia para el oracle S19

Un acceptance proof válido debe mantener tres autoridades separadas:

1. **árbol/cubos original:** reflexión independiente sobre el modelo real;
2. **render original:** vértices emitidos por `renderToBuffer` con estado de edad explícito;
3. **transform de fuente:** matriz fijada desde el renderer concreto y comprobada independientemente del DTO exportado.

La comparación espacial debe aplicar a renderer y collider el mismo transform de fuente correcto. El `modelTransform` serializado se comprueba por igualdad contra esa autoridad externa; nunca se usa como fuente del valor esperado.

Así, una omisión del `0.8`, un orden incorrecto `translate -> scale`, un estado bebé accidental o dos errores compensatorios dejan de poder producir un falso verde.
