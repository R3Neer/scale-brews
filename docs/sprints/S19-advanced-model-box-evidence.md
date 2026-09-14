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
6. Existe **field hiding real** en la jerarquía. `AdvancedModelBox` declara `public ObjectList<TabulaModelRenderUtils.ModelBox> cubeList` y `public ObjectList<BasicModelPart> childModels`, mientras `BasicModelPart` declara otros campos distintos con los mismos nombres: `private final ObjectList<BasicModelPart.ModelBox> cubeList` y `private final ObjectList<BasicModelPart> childModels`.

Una inspección de bytecode adicional contra el mismo jar fijado confirmó qué representación es autoritativa en ejecución. Evidencia: run `34832259638`, job `103938053795`, artifact `10342294111`, SHA-256 `4cf1dbe793f05f36a3c8e129cb19e4e1e2612c410653aeec539f66952830fd44`. La workflow temporal fue retirada después de capturar el artifact.

- Los constructores de `AdvancedModelBox` inicializan sus propios `cubeList` y `childModels`.
- Todos los overloads públicos observados de `AdvancedModelBox.addBox(...)` terminan en su `private addBox(...)`, que lee `AdvancedModelBox.cubeList` y añade `TabulaModelRenderUtils.ModelBox` a esa lista.
- `AdvancedModelBox.render(...)` y `doRender(...)` leen ese mismo `cubeList` de la subclase. Para este dialecto, ésa es por tanto la representación geométrica que realmente renderiza.
- `AdvancedModelBox.addChild(...)` primero invoca `BasicModelPart.addChild(...)` y después añade el mismo hijo también a `AdvancedModelBox.childModels`; si el hijo también es `AdvancedModelBox`, además fija su parent. Los dos `childModels` homónimos coexisten y se actualizan en paralelo.

Consecuencias adversariales:

- una implementación que derive identidad/cobertura exclusivamente de `getFields()` o de visibilidad pública puede parecer correcta con Grizzly y fallar con Gazelle;
- un resolver reflectivo genérico que busque `cubeList` o `childModels` por nombre atravesando la jerarquía sin fijar el dialecto/clase declaradora puede enlazar el campo homónimo equivocado; en `cubeList` incluso el tipo de elemento es distinto;
- leer el `cubeList` privado de `BasicModelPart` para una instancia `AdvancedModelBox` no reproduce la geometría que los overloads `addBox(...)` de esa clase construyen ni la que `AdvancedModelBox.render(...)` consume;
- para hijos, mezclar o concatenar ambos `childModels` puede duplicar traversal, porque `addChild(...)` mantiene ambas listas;
- el contrato reflectivo del extractor debe validar la estructura exacta esperada de `AdvancedModelBox` y fallar cerrado ante un dialecto parcial o ambiguo, en lugar de mezclar accidentalmente las dos representaciones.

La genericidad no se demuestra con un solo modelo favorable y la compatibilidad reflectiva no puede reducirse a «existe un field con este nombre».

## Raíces e identidad observadas en los modelos de aceptación

Una inspección adicional del mismo jar fijado separó explícitamente la frontera de render (`parts()`) del catálogo plano (`getAllParts()`) y comprobó cómo se materializan los nombres de pieza. Evidencia: run `34833347197`, job `103941521300`, artifact `10342916772`, SHA-256 `77087e1b6bb99d138f15dd37441da4687ba17429f58b3d2db7039f63429088fe`.

- `ModelGazelle.parts()` devuelve únicamente `body`; `getAllParts()` devuelve las 13 `AdvancedModelBox` del modelo.
- `ModelGrizzlyBear.parts()` devuelve únicamente `root`; `getAllParts()` devuelve las 13 `AdvancedModelBox` del modelo, incluidas piezas no públicas como `hat` y `microphone`.
- En ambos constructores inspeccionados, todas las `AdvancedModelBox` observadas se crean mediante `AdvancedModelBox(model, String)` con nombres explícitos como `body`, `neck`, `head`, `root`, `left_arm`, etc.; esos nombres alimentan `boxName` y no dependen de que el field correspondiente sea público.

Consecuencia: `parts()` representa las raíces desde las que debe reconstruirse la jerarquía renderizada; `getAllParts()` es útil como catálogo/cobertura, pero no puede tratarse como una lista de raíces y volver después a recorrer `childModels`, porque eso reintroduciría nodos ya contenidos en el árbol. Para los dos modelos exactos de aceptación, `boxName` ofrece identidad tecnológica independiente de la visibilidad de fields del modelo concreto. Una implementación genérica no debe volver a depender de `getDeclaredFields()` como autoridad de nombres para compensar que Gazelle los declare privados.

## Semántica exacta de transforms locales y `scaleChildren`

Una tercera inspección de bytecode contra el mismo jar fijado cerró la semántica de transformación que el extractor debe reproducir. Evidencia: run `34832565884`, job `103939039210`, artifact `10341929614`, SHA-256 `2c0b01598130a658d6ae03a42b20d800bbc98f93886c0ee15d02fe55ed96e511`.

`AdvancedModelBox.translateAndRotate(PoseStack)` compone, en este orden:

1. `translate(rotationPointX / 16, rotationPointY / 16, rotationPointZ / 16)`;
2. rotación alrededor de Z si `rotateAngleZ != 0`;
3. rotación alrededor de Y si `rotateAngleY != 0`;
4. rotación alrededor de X si `rotateAngleX != 0`;
5. `scale(scaleX, scaleY, scaleZ)`.

`AdvancedModelBox.render(...)` hace `pushPose()`, aplica ese transform local y renderiza su propio `cubeList`. La herencia hacia hijos depende después de `scaleChildren`:

- si `scaleChildren == true`, los hijos reciben la escala del padre normalmente;
- si `scaleChildren == false`, antes de renderizar los hijos el renderer aplica `scale(1/max(scaleX, 1e-4), 1/max(scaleY, 1e-4), 1/max(scaleZ, 1e-4))`, cancelando la escala local del padre para la rama descendiente mientras conserva la traslación y rotaciones ya compuestas;
- el `1e-4` observado aquí es un guard del renderer para la **operación inversa de escala**. No debe reinterpretarse como permiso para inventar espesor físico a primitivas planas ni como reparación genérica de geometría corrupta; el contrato estricto de S19 sobre primitivas/validez permanece intacto.

El mismo bytecode muestra que `render(...)` retorna antes de recorrer cubos o hijos cuando `showModel == false`. En este dialecto la visibilidad es por tanto estructural y oculta el subárbol renderizado. Es una semántica distinta de un `AnatomyFilter` declarativo: excluir una part/piece por política no autoriza a propagar esa exclusión a descendientes físicos que no coincidan con el filtro.

Consecuencia: un traversal que acumule siempre la matriz local completa del padre hacia los hijos no reproduce el renderer cuando `scaleChildren == false`. La extracción debe distinguir el transform usado para la geometría propia del nodo del transform que se propaga a descendientes, y debe mantener separadas visibilidad estructural y selección declarativa.

## Semántica exacta de `ModelBox`

La representación renderizada del cubo se inspeccionó también contra el mismo jar exacto. Evidencia: run `34832761258`, job `103939670854`, artifact `10343230830`, SHA-256 `b189bb48fd5e06168c2feedd1006bbcb7069f1794981f1b820949f0f6aedf242`. La workflow temporal fue retirada tras conservar el artifact.

`TabulaModelRenderUtils.ModelBox` conserva dos niveles distintos de información:

- `posX1/Y1/Z1` guardan la posición fuente recibida por el constructor;
- `posX2/Y2/Z2` se calculan como posición fuente + tamaño fuente, **antes de aplicar inflation**;
- los ocho `PositionTextureVertex` usados para construir los seis `TexturedQuad` se calculan después de restar/sumar los tres inflates por eje;
- cuando `mirror` es verdadero se intercambian los extremos X usados para los vértices, pero ello no convierte el tamaño fuente almacenado en otro valor.

Consecuencia adversarial: el extractor no debe decidir si una dimensión fuente era positiva, cero o negativa a partir de un `min/max` posterior que ya haya normalizado orientación e inflation. Para paridad de forma, los vértices/quads materializados son la autoridad geométrica; para validar la intención dimensional del dialecto, los `pos1/pos2` conservan la dimensión fuente previa a inflation. Una dimensión fuente negativa no debe transformarse accidentalmente en una `Piece` positiva sólo porque `min/max` produzca una AABB ordenada. A la inversa, una dimensión fuente exactamente cero **no implica por sí sola** una primitiva plana después de inflation: con delta positivo los vertices pueden delimitar volumen. Sólo un caso cuyo volumen renderizado siga siendo plano puede clasificarse render-only; Gazelle lo demuestra con profundidad e inflation ambas `0.0f`.

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

La autoridad legacy está actualmente en `src/client/java/io/github/r3neer/scalebrews/client/collision/preparation/GeometryExtractor.java`. La búsqueda de consumidores muestra que `GeometryExtractor.alex(...)` sólo está consumido por el proof `AnatomyExportProof`; los demás usos de `GeometryExtractor` pertenecen a la ruta vanilla/ModelPart y ya son adapters del engine S17.

El algoritmo Alex legacy confirma la deuda que S19 sustituye: deriva nombres de `getDeclaredFields()` del modelo concreto, propaga la exclusión/visibilidad de un parent a todos sus descendientes, descarta silenciosamente primitivas planas/degeneradas y usa la compensación `1/max(scale, 1e-4)` dentro de su representación de herencia. Esta última constante coincide con el renderer 2.1.9 sólo en la operación concreta de compensación de hijos descrita arriba; no puede generalizarse a validación o reparación física.

Por tanto S19 puede migrar el proof Alex al engine de familia y reducir/eliminar `GeometryExtractor.alex(...)` sin una migración runtime masiva. El criterio sigue siendo uno: **no deben sobrevivir dos algoritmos independientes de extracción AdvancedModelBox**.

## Qué no demuestra esta evidencia

- No demuestra todavía que exista `scalebrews:advanced_model_box` en producción.
- No demuestra extracción real de Grizzly + Gazelle por el nuevo engine.
- No demuestra filtrado parent/child, clasificación render-only en el nuevo extractor, límites N/N+1 ni fail-closed de datos corruptos.
- No demuestra pose Citadel/`ModelAnimator`; esa mitad de G3 tarea 5 sigue fuera de S19.

Esos puntos deben pasar a pruebas adversariales únicamente cuando exista la superficie productiva correspondiente, sin ampliar el contrato congelado.