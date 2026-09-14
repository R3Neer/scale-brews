# S19 — AdvancedModelBox supporting evidence

Estado: **evidencia auxiliar preimplementación**. Este archivo no redefine requisitos, arquitectura, scope ni estado global. El contrato normativo sigue en `ENTITY_COLLISIONS_REQUIREMENTS.md`, la arquitectura en `ENTITY_COLLISIONS.md`, el plan global en `ENTITY_COLLISIONS_PLAN.md` y el plan de sprint en `S19-advanced-model-box-geometry-engine.md`. La evidencia final de aceptación se consolidará en `VALIDATION.md` al cerrar S19.

## Baseline preimplementación

Snapshot de plan: `b5ce86a54f8954afcb2d2f5eddff587f7a874720`.
Freeze common: `9c8e93f2a0dfb7e91e1b348339de3c22bc5a2f8d`.

La lane focal common preimplementación ejecuta 6 GameTests. En el último checkpoint sin implementación (`290fe5badd2391c68c1ed425e9efe2ace4c92843`, run `34828067552`, job `103924691094`) el resultado es **3 verdes / 3 rojos intencionales**. Los tres fallos son exactamente la ausencia de `scalebrews:advanced_model_box`: registro common, aceptación del binding canónico y ownership/no-id-theft. Los guards de signatures/constant-pool y los casos que no necesitan el engine ya pasan. Artifact common `10341725136`, SHA-256 `772381517140bf0c07de8b1052ea91f2bc708a7f550fb0706fceced9370f3395`.

El build ordinary del mismo checkpoint, run `34828067528`, concluyó **success**. Por tanto S19 parte de ordinary verde y un focal rojo únicamente por funcionalidad todavía inexistente.

## Inputs externos exactos de aceptación

El lock persistente es `tools/s19-external-inputs.lock.json`. La lane `external-input-contract` de run `34828067552`, job `103924690762`, concluyó verde y verificó resolución, identidad de mod y SHA-256 de exactamente tres jars, sin extras transitorios. Artifact `10341311087`, SHA-256 `3cd9313811e1a44c9a4c4ae357644329fecc1299651eb780971fdd1ee5a44fc1`.

Inputs fijados:

- Alex's Mobs Continued `2.1.9`, CurseMaven `curse.maven:alexs-mobs-continued-1635121:8772801`, fichero de origen `alexsmobs-2.1.9-fabric+26.2.jar`, SHA-256 `b810e1b7ac925f1f1ba6a2a87e742e30b33fafad4d164ff64192f21f46f6154c`.
- CodxLib `1.5.1`, CurseMaven `curse.maven:codxlib-1633207:8723545`, fichero de origen `codxlib-1.5.1-fabric+26.2.jar`, SHA-256 `2133472c9e504923c592709901d77092a09bcf645884968839479c8ee02c7acb`.
- Cloth Config `26.2.155`, CurseMaven `curse.maven:cloth-config-348521:8269699`, fichero de origen `cloth-config-26.2.155.jar`, SHA-256 `def4be7639cd66704f7e304d658ea0f6bf490fb4a6eaa2dbf18ec2c3999d6349`.

No se usa `latest`, una página mutable ni el source tree público como sustituto del jar fijado.

## Dialecto exacto observado en Alex 2.1.9 / Fabric 26.2

El probe temporal fue ejecutado sólo contra el jar Alex cuyo SHA coincide con el lock y después fue retirado del branch. Evidencia: run `34827014334`, job `103921389362`, artifact `10340895068`, SHA-256 `d8d11a3641ec6538b0597d2d921695f557f28e7c1811dd5e3356d72b562c0413`.

Hallazgos relevantes:

1. La tecnología está **relocada dentro del propio jar de Alex** bajo `com.github.alexthe666.alexsmobs.citadel...`. No coincide con el paquete `com.github.alexthe666.citadel...` de otras ramas/ports públicos.
2. `AdvancedModelBox` extiende `BasicModelPart` y expone, entre otros, `scaleX`, `scaleY`, `scaleZ`, `scaleChildren`, `cubeList`, `childModels`, `boxName`, `translateAndRotate(PoseStack)`, `setScale(...)` y `setShouldScaleChildren(...)`.
3. `BasicModelPart` posee `rotationPointX/Y/Z`, `rotateAngleX/Y/Z`, `showModel`, su propio `cubeList`/`childModels` y `translateRotate(PoseStack)`.
4. `AdvancedEntityModel` expone `getAllParts()` como método abstracto de la familia, además de las primitivas procedurales Citadel. S19 sólo usa la parte geométrica.
5. `ModelGrizzlyBear` expone muchas piezas como fields públicos, pero `ModelGazelle` declara sus piezas como fields privados. Ambos exponen `getAllParts()`.

Consecuencia adversarial: una implementación que derive identidad/cobertura exclusivamente de `getFields()` o de visibilidad pública puede parecer correcta con Grizzly y fallar con Gazelle. La genericidad no se demuestra con un solo modelo favorable.

## Primitiva plana exacta de Gazelle

La excepción render-only del plan también fue comprobada contra el jar exacto del lock, no contra otra rama de source. Evidencia: run `34828802003`, job `103926989818`, artifact `10340938164`, SHA-256 `4683707be2d3a3820c39309333223b964ddc9ebe5bd5e4c349b7c6e59385c1f6`.

El constructor real de `ModelGazelle` ejecuta para `tail`:

`addBox(-2.0f, 0.0f, 0.0f, 4.0f, 5.0f, 0.0f, 0.0f, false)`.

La tercera dimensión es exactamente `0.0f`. Es una primitiva visual plana legítima de la versión fijada. Por tanto:

- no puede convertirse en `ModelGeometry.Piece`, porque el DTO físico exige volumen estrictamente positivo;
- no se le puede inventar epsilon/espesor;
- tampoco puede provocar por sí sola el rechazo del resto del modelo Gazelle válido;
- su clasificación/omisión debe ser deliberada y determinista, diferenciada de una primitiva volumétrica corrupta o de datos NaN/∞.

## Transform de renderer por fuente

Una segunda inspección temporal verificó los renderers del mismo jar exacto del lock. Evidencia: run `34828467843`, job `103925942390`, artifact `10341416449`, SHA-256 `27a82acf23dda01b24a38bbd92184575e8eca2000ca46af6ea78793940b26795`.

- `RenderGazelle` declara su propio `scale(EntityGazelle, PoseStack, float)` y ejecuta `PoseStack.scale(0.8f, 0.8f, 0.8f)`.
- `RenderGrizzlyBear` **no** declara un override equivalente; usa el comportamiento heredado de su renderer base.
- Los `0.4f` y `0.8f` pasados por sus constructores a `MobRenderer` son parámetros de renderer (por ejemplo shadow radius), no deben confundirse con el transform geométrico aplicado por `scale(...)`.
- El proof histórico `rendererRoot(...)` invoca precisamente el hook `scale(...)` del renderer real antes de la traslación de modelo, por lo que esta diferencia forma parte del frame de referencia, no de una preferencia estética del test.

Consecuencia de diseño ya prevista por I6: el `modelTransform` debe pertenecer a la **fuente concreta** y no puede ser una constante compartida por la familia `AdvancedModelBox`. La aceptación posterior debe detectar tanto la pérdida del `0.8` de Gazelle como la aplicación accidental de esa escala a Grizzly.

## Legacy y ownership

La búsqueda de consumidores del algoritmo Alex legacy muestra que `GeometryExtractor.alex(...)` sólo está consumido por el proof `AnatomyExportProof`. Los demás usos de `GeometryExtractor` pertenecen a la antigua ruta vanilla/ModelPart y ya son adapters del engine S17.

Por tanto S19 puede migrar el proof Alex al engine de familia y reducir/eliminar `GeometryExtractor.alex(...)` sin una migración runtime masiva. El criterio sigue siendo uno: **no deben sobrevivir dos algoritmos independientes de extracción AdvancedModelBox**.

## Qué no demuestra esta evidencia

- No demuestra todavía que exista `scalebrews:advanced_model_box` en producción.
- No demuestra extracción real de Grizzly + Gazelle por el nuevo engine.
- No demuestra filtrado parent/child, clasificación render-only en el nuevo extractor, límites N/N+1 ni fail-closed de datos corruptos.
- No demuestra pose Citadel/`ModelAnimator`; esa mitad de G3 tarea 5 sigue fuera de S19.

Esos puntos deben pasar a pruebas adversariales únicamente cuando exista la superficie productiva correspondiente, sin ampliar el contrato congelado.
