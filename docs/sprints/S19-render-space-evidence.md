# S19 — AdvancedModelBox render-space evidence

Estado: **evidencia auxiliar preimplementación**. Este archivo no añade requisitos ni concreta holdouts. Fija la conversión de unidades y la composición espacial observadas en el jar exacto de aceptación para evitar que el oracle y el extractor compartan una suposición equivocada.

## Fuente exacta

La inspección parte del mismo Alex's Mobs Continued `2.1.9` / Fabric `26.2` fijado por `tools/s19-external-inputs.lock.json`, SHA-256 `b810e1b7ac925f1f1ba6a2a87e742e30b33fafad4d164ff64192f21f46f6154c`.

El bytecode completo de `AdvancedModelBox` está preservado en:

- run `34832565884`;
- job `103939039210`;
- artifact `10341929614` (`S19-transform-semantics-probe`);
- SHA-256 del artifact `2c0b01598130a658d6ae03a42b20d800bbc98f93886c0ee15d02fe55ed96e511`.

## Conversión de unidades dentro del renderer

`AdvancedModelBox.doRender(...)` recorre el `cubeList` propio de `AdvancedModelBox`, después `ModelBox.quads` y finalmente `TexturedQuad.vertexPositions`.

Para cada `PositionTextureVertex.position`, el bytecode hace explícitamente:

- `x / 16.0f`;
- `y / 16.0f`;
- `z / 16.0f`;

Sólo **después** construye el `Vector4f(x, y, z, 1)` y lo multiplica por la `Matrix4f` del `PoseStack`.

La misma convención aparece en el transform del part: `translateAndRotate(PoseStack)` aplica `rotationPointX/Y/Z / 16.0f` antes de las rotaciones y la escala local.

Por tanto los extremos/vertices almacenados por `TabulaModelRenderUtils.ModelBox` y los `rotationPoint*` están expresados en unidades de modelo/píxel, mientras `ModelGeometry` y el renderer trabajan espacialmente en bloques después de esa conversión.

## Consecuencias para extracción y oracle

1. Los bounds locales físicos derivados de los quads post-inflation deben convertirse de píxeles a bloques (`/16`) **antes** de aplicar el transform de part.
2. La traslación de `rotationPoint` se convierte también una sola vez mediante `/16`; no debe volver a dividirse al serializar/componer la matriz.
3. No es equivalente dividir una posición world-space o la matriz final por `16`: las traslaciones del part, del renderer y del `modelTransform` ya están en el frame de render correspondiente.
4. Una implementación que use los vertices post-inflation pero olvide `/16` puede conservar jerarquía, IDs e incluso proporciones internas y aun así producir colliders dieciséis veces mayores en cada eje local.
5. Una implementación que divida tanto vertices como una segunda vez el transform local producirá el error inverso. El oracle debe comprobar local-space y world-space por separado para que dos errores compensatorios no se cancelen.

## Normal y UV

El renderer transforma la normal del `TexturedQuad` mediante la `Matrix3f` normal del `PoseStack`; UVs y winding pertenecen al render visual, no añaden volumen físico al DTO actual. Para S19 la autoridad geométrica de un cubo son sus posiciones espaciales materializadas y su intención dimensional fuente, no sus UVs.

`mirror` puede alterar winding/orden de vertices, pero no autoriza un AABB físico distinto cuando el conjunto espacial de posiciones es el mismo.

## Frame externo adulto

La inspección del bytecode completo de `ModelGrizzlyBear` y `ModelGazelle` del artifact `10342916772` mostró que, con `young=false`, ambos `renderToBuffer(...)` hacen únicamente `pushPose -> parts().render -> popPose`; las escalas/traslaciones globales adicionales pertenecen a la rama juvenil.

Por eso el proof S19 de geometría canónica debe comparar estado adulto y mantener separadas tres capas:

1. geometría local de cubo en bloques (`quad vertices / 16`);
2. jerarquía/transforms locales `AdvancedModelBox`;
3. `modelTransform` externo derivado del renderer real (`scale(-1,-1,1)`, hook `renderer.scale(...)`, traslación base del proof).

Gazelle añade en su renderer real `scale(0.8, 0.8, 0.8)`; Grizzly no declara ese override. Esa diferencia sigue perteneciendo a la fuente concreta, no a la familia global.
