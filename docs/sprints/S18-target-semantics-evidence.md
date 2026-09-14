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

## Clasificación de `PoseProgramEvaluator`

La implementación productiva actual extrae translation, scale y quaternion del transform de reposo de `ModelGeometry.Part` y aplica los samples de la siguiente forma:

- TRANSLATION: `position += sampled / 16`;
- ROTATION: `restQuaternion *= Quaternion(sampledEuler)`;
- SCALE: `baseScale *= (1 + sampledDelta)`.

La comparación con Minecraft permite aislar las responsabilidades:

1. **TRANSLATION es semánticamente compatible** con `offsetPos`: `(basePixels + deltaPixels) / 16 == baseBlocks + deltaPixels / 16`.
2. **ROTATION diverge** para reposos no identidad. Minecraft hace `rotationZYX(restEuler + deltaEuler)`, no `Q(rest) * Q(delta)`. El oracle multi-bone ya expone esta divergencia sobre `body`.
3. **SCALE diverge cuando la escala de reposo no es 1**. Minecraft hace `baseScale + delta`, mientras la implementación actual hace `baseScale * (1 + delta)`. Ambas expresiones sólo coinciden de forma general cuando `baseScale == 1` o `delta == 0`.

Ejemplo algebraico mínimo: con `baseScale=1.5` y `scaleVec(1.2,...)`, el sample es `+0.2`; Minecraft produce `1.7` y la multiplicación actual produce `1.8`.

## Evidencia ejecutada del defecto SCALE

El holdout cliente `S18NonUnitRestScaleSemanticClientTests` parte de un `CowModel` real, fija la escala de reposo de `root/body` a `(1.5, 0.75, 1.25)`, compila un `AnimationDefinition` real con target SCALE y compara la aplicación nativa de Mojang con `PoseProgramEvaluator`.

La lane se aisló de los demás oracles S18 para que el rojo de ROTATION no ocultase la causa SCALE. Evidencia:

- commit de la lane aislada: `5701ba3cb6f11a9ecbffcfc5c9a4e235fe75bc86`;
- run `34839087126`;
- job SCALE `103959597946`: **failure intencional/causal**;
- mensaje exacto: `Mojang SCALE target diverges for non-unit rest xScale: expected=1.6999999 actual=1.8`;
- artifact `10345108684` (`S18-non-unit-rest-scale-proof`);
- SHA-256 del artifact: `45c38c7180ef83cfbd5e8d0e5d621331c68d445057b4233ffc8c326449d42ac0`.

En el mismo run, `common-authority` (`103959598058`) quedó **success**, mientras `client-compiler-boundary` (`103959598157`) siguió rojo por el defecto ROTATION ya conocido. Quedan así dos defectos productivos independientes y localizados en la semántica de aplicación del programa neutral, no en el registry/SPI común ni en el entorno de CI.

`AnimationDefinitionCompiler` no introduce la divergencia SCALE: copia literalmente los vectores `preTarget`/`postTarget` de Mojang al `PoseProgram`. La discrepancia aparece al combinar ese sample con el transform de reposo en `PoseProgramEvaluator`.

Clasificación TM: **bug de implementación productiva**. El adversario conserva el rojo y la reproducción; la reparación corresponde al IMPLEMENTADOR.

## Riesgo de representación de la rotación

`ModelGeometry.Part` conserva una matriz local, no los fields Euler de reposo originales. Para reproducir exactamente una semántica basada en `restEuler + deltaEuler`, una reparación no puede asumir sin evidencia que cualquier descomposición quaternion/matriz a Euler elige siempre el mismo representante que los fields fuente, especialmente cerca de ambigüedades Euler/gimbal.

Esto no declara todavía un defecto arquitectónico adicional: es un riesgo que la reparación debe demostrar cerrado mediante el oracle original-source. El adversario no prescribe la representación productiva concreta; exige paridad con la semántica Minecraft congelada.
