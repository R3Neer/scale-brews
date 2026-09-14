# S18 — evidencia retroactiva de semántica de targets ModelPart

Estado: **evidencia adversarial de reapertura**. Este archivo no redefine requisitos ni arquitectura; documenta la semántica exacta de Minecraft 26.2 usada para clasificar los defectos productivos descubiertos al reabrir S18/G3.4.

## Fuente exacta

La inspección se ejecutó contra el `minecraft-clientOnly` de Minecraft 26.2 resuelto por el proyecto:

- commit de la sonda temporal: `891caff2d1ca2a482cb4b65030bf8e0859f0a61e`;
- run `34838496335`;
- job `103957744729`;
- artifact `10344957741` (`S18-modelpart-target-semantics-probe`);
- SHA-256 del artifact: `38d6d772b25bdcf2044aba403c4d034aa28a1e6886cc1210956f2afe77eafe34`;
- SHA-256 del jar cliente inspeccionado: `7f64691f870cbf79fdfe754fd20091a285ceea65e28788836b8041fb025771b8`.

La workflow era exclusivamente diagnóstica y fue retirada tras conservar esta evidencia.

## `ModelPart` 26.2

El bytecode de `ModelPart` muestra:

- `offsetPos(Vector3f)`: suma por componente sobre `x/y/z`;
- `offsetRotation(Vector3f)`: suma por componente sobre `xRot/yRot/zRot`;
- `offsetScale(Vector3f)`: suma por componente sobre `xScale/yScale/zScale`;
- `translateAndRotate(PoseStack)`: primero `translate(x/16, y/16, z/16)`, después construye **un único** `Quaternionf.rotationZYX(zRot, yRot, xRot)`, y finalmente aplica `scale(xScale, yScale, zScale)`.

Por tanto los tres targets Mojang son offsets aditivos sobre los fields del part antes de reconstruir su transform local.

## Helpers de keyframes

El bytecode de `KeyframeAnimations` confirma:

- `posVec(x,y,z)` conserva `x`, invierte el signo de `y` y conserva `z`, según la convención de animación Mojang;
- `degreeVec(x,y,z)` convierte grados a radianes;
- `scaleVec(x,y,z)` devuelve `(x-1, y-1, z-1)`.

En consecuencia, el vector de SCALE que llega al target es un **delta aditivo alrededor de 1**, no un multiplicador porcentual.

## Primera clasificación de `PoseProgramEvaluator`

La implementación productiva previa a la reparación `7733dfceee00bc4601316ae66240a7ba1ad97645` extraía translation, scale y quaternion del transform de reposo de `ModelGeometry.Part` y aplicaba los samples así:

- TRANSLATION: `position += sampled / 16`;
- ROTATION: `restQuaternion *= Quaternion(sampledEuler)`;
- SCALE: `baseScale *= (1 + sampledDelta)`.

La comparación con Minecraft aisló tres casos:

1. **TRANSLATION es semánticamente compatible** con `offsetPos`: `(basePixels + deltaPixels) / 16 == baseBlocks + deltaPixels / 16`.
2. **ROTATION divergía** para reposos no identidad. Minecraft hace `rotationZYX(restEuler + deltaEuler)`, no `Q(rest) * Q(delta)`.
3. **SCALE divergía** cuando la escala de reposo no es 1. Minecraft hace `baseScale + delta`, mientras la implementación hacía `baseScale * (1 + delta)`.

Ejemplo algebraico mínimo de SCALE: con `baseScale=1.5` y `scaleVec(1.2,...)`, el sample es `+0.2`; Minecraft produce `1.7` y la multiplicación previa producía `1.8`.

## Evidencia ejecutada del defecto SCALE

El holdout cliente `S18NonUnitRestScaleSemanticClientTests` parte de un `CowModel` real, fija la escala de reposo de `root/body` a `(1.5, 0.75, 1.25)`, compila un `AnimationDefinition` real con target SCALE y compara la aplicación nativa de Mojang con `PoseProgramEvaluator`.

La lane se aisló de los demás oracles S18 para que el rojo de ROTATION no ocultase la causa SCALE. Evidencia inicial:

- commit de la lane aislada: `5701ba3cb6f11a9ecbffcfc5c9a4e235fe75bc86`;
- run `34839087126`;
- job SCALE `103959597946`: **failure intencional/causal**;
- mensaje exacto: `Mojang SCALE target diverges for non-unit rest xScale: expected=1.6999999 actual=1.8`;
- artifact `10345108684` (`S18-non-unit-rest-scale-proof`);
- SHA-256 del artifact: `45c38c7180ef83cfbd5e8d0e5d621331c68d445057b4233ffc8c326449d42ac0`.

En ese mismo run, `common-authority` quedó verde y el cliente semántico siguió rojo por ROTATION. `AnimationDefinitionCompiler` no era la causa: copia literalmente los vectores `preTarget`/`postTarget` de Mojang al `PoseProgram`.

Clasificación TM de SCALE: **bug de implementación productiva**.

## Reparación parcial `7733dfce`

El IMPLEMENTADOR corrigió ambos defectos conocidos en `7733dfceee00bc4601316ae66240a7ba1ad97645`:

- SCALE pasó a `baseScale.add(sampledDelta)`, reproduciendo `ModelPart.offsetScale`;
- ROTATION dejó de multiplicar quaternions y pasó a descomponer el quaternion de reposo mediante `getEulerAnglesZYX(...)`, sumar el delta por componente y reconstruir `rotationZYX(...)`.

Run focal del fix: `34839362564`.

- `common-authority` job `103960475436`: **success**;
- `client-compiler-boundary` job `103960475221`: **success**;
- `client-non-unit-rest-scale` job `103960475418`: **success**.

Por tanto la corrección SCALE queda confirmada para escala de reposo positiva no unitaria y el caso ROTATION multi-bone que reabrió originalmente S18 también deja de fallar. El fix no fue un simple ajuste para ocultar el rojo conocido.

## Holdout post-fix: pérdida de representante Euler

La estrategia ROTATION del fix recupera un representante Euler desde la matriz de reposo. Ese paso pierde información que Minecraft sí conserva en `ModelPart.xRot/yRot/zRot`.

Para ZYX, los triples:

- `(x, y, z)` y
- `(x + π, π - y, z + π)`

pueden representar la **misma orientación inicial**. Al aplicar después el mismo delta por componentes, como hace `offsetRotation`, ya no tienen por qué seguir representando la misma orientación.

El holdout `S18EulerRepresentativeSemanticClientTests` usa un `CowModel` real y demuestra exactamente esa pérdida:

1. construye dos `body` con los dos representantes Euler equivalentes;
2. verifica que `GeometryExtractor.vanilla(...)` los colapsa a la misma matriz local de reposo;
3. aplica el mismo `AnimationDefinition` ROTATION nativo a ambos y verifica que los resultados Mojang se separan;
4. evalúa el mismo `PoseProgram` neutral sobre ambas geometrías;
5. exige paridad individual con cada fuente nativa.

Commits adversariales:

- holdout: `f5899e44d91a0a64a5ffe027908e670ceb5e84c7`;
- lane aislada: `e76f6448423638d517fecc8b9cd93a085243b317`.

Evidencia ejecutada: run `34839609532`.

- `common-authority` job `103961255802`: **success**;
- `client-compiler-boundary` job `103961255617`: **success**;
- `client-non-unit-rest-scale` job `103961255970`: **success**;
- `client-euler-representative` job `103961255753`: **failure causal**.

Mensaje exacto del rojo:

`alternate equivalent Euler representative differs at matrix[0]: expected=0.510475 actual=0.69862896`

Artifact `10345492485` (`S18-euler-representative-proof`), SHA-256 `bbc9d485aa66de37a698858b4ba64a95c4a0386e720435a97fd2b6b379473eaa`.

El punto del fallo importa: el test alcanza la última comparación de la representación alternativa. Por tanto ya habían pasado las precondiciones de que ambas poses de reposo colapsan a la misma matriz, de que Mojang las separa tras el mismo delta y de que la representación canónica sí coincide con el evaluator reparado.

## Holdout post-fix: escala de reposo firmada

`ModelPart.translateAndRotate(...)` aplica directamente `xScale/yScale/zScale`; `ModelGeometry` acepta transforms afines no degenerados con determinante negativo. Una reflexión en reposo es por tanto una entrada representable del pipeline, no un dato prohibido.

El fix `7733dfce` obtiene la escala mediante `Matrix4f.getScale(...)`, que devuelve magnitudes y no conserva por sí sola qué componente fuente era negativa. `S18SignedRestScaleSemanticClientTests` usa de nuevo un `CowModel` real:

- fija `root/body` a `xScale=-1.5`, `yScale=0.75`, `zScale=1.25`;
- exige como precondición que la extracción conserve determinante negativo;
- aplica un target SCALE Mojang aditivo `+0.2` sobre X;
- exige que Mojang conserve la reflexión tras el target;
- compara esa matriz nativa con el evaluator neutral.

Commits adversariales:

- holdout: `e5e06db5f0875c4d4d77bdd4f9cc67f2c780e096`;
- lane aislada: `80a1a432a72aea62ffd474c2d38546e63894131f`.

Evidencia ejecutada: run `34840312369`, job `client-signed-rest-scale` `103963505205`: **failure causal**.

Mensaje exacto:

`signed rest SCALE differs at matrix[0]: expected=-1.2999998 actual=-1.6999995 expectedDet=-1.2187496 actualDet=1.5937489`

Artifact `10345981147` (`S18-signed-rest-scale-proof`), SHA-256 `53e9b46a547e9aa7827ef1596dcbcc5504ce6f8c96f91f7d6fdeb66bbd001dfe`.

En el mismo run:

- `common-authority` `103963504791`: **success**;
- `client-compiler-boundary` `103963505045`: **success**;
- `client-non-unit-rest-scale` `103963505181`: **success**;
- `client-euler-representative` `103963505119`: sigue **failure**;
- `client-signed-rest-scale` `103963505205`: **failure**.

La precondición del holdout firmada pasó: la reflexión llegó a `ModelGeometry` y Mojang la conservó tras el target. El neutral no sólo obtuvo una magnitud X incorrecta, sino que cambió el signo del determinante y convirtió un transform reflejado en uno no reflejado.

## Clasificación actual

Los dos holdouts post-fix muestran el mismo problema general desde ángulos distintos: la matriz local de reposo conserva el transform final, pero **no conserva necesariamente la semántica de los fields TRS fuente sobre los que `AnimationDefinition` aplica offsets aditivos**.

- El holdout Euler demuestra formalmente pérdida de identidad: dos estados fuente distintos colapsan a la misma matriz y requieren salidas distintas para el mismo delta.
- El holdout de escala firmada demuestra además que una descomposición ordinaria por magnitudes puede destruir una reflexión válida incluso sin necesitar una pareja equivalente más sofisticada.

Clasificación TM: **gap de arquitectura/modelo de datos en la frontera geometry↔pose de `mojang_keyframes`**. El requisito no cambia: el material neutral server-safe debe conservar información suficiente del estado de reposo fuente para reproducir `offsetPos`, `offsetRotation` y `offsetScale` exactamente sin clases cliente ni estado global. La forma concreta de esa información y dónde vive pertenecen al IMPLEMENTADOR/arquitectura, no al adversario.

S18/G3.4 permanece abierto. S19 puede seguir investigándose, pero no debe declararse cerrado por delante de esta dependencia.
