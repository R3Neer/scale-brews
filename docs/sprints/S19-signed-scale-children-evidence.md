# S19 — Evidencia de `scaleChildren=false` con escala negativa

Estado: **ORACLE CORREGIDO / PRODUCCIÓN COINCIDE CON EL BYTECODE FIJADO**.

Este documento conserva tanto el contraejemplo adversarial como su clasificación correcta. El primer holdout asumió que `scaleChildren=false` debía compensar una escala negativa mediante el recíproco firmado. Esa hipótesis era razonable como geometría abstracta, pero no era la semántica del dialecto Citadel que S19 ha fijado como autoridad. El adversario retiró esa expectativa antes de solicitar cualquier cambio productivo.

## Fuente exacta

Los inputs siguen siendo los fijados por `tools/s19-external-inputs.lock.json`:

- Minecraft `26.2`;
- Alex's Mobs Continued `2.1.9`;
- CodxLib `1.5.1`;
- Cloth Config fijado por el lock;
- hashes SHA-256 comprobados antes de ejecutar la prueba.

La evidencia de bytecode exacta ya capturada por S19 es:

- run `34832565884`;
- job asociado al probe de transform semantics;
- artifact `10341929614` (`S19-transform-semantics-probe`);
- SHA-256 `2c0b01598130a658d6ae03a42b20d800bbc98f93886c0ee15d02fe55ed96e511`.

En `AdvancedModelBox.render(...)`, el `javap` del jar fijado muestra, después de `translateAndRotate(...)` y `doRender(...)`, que cuando `scaleChildren == false` el renderer ejecuta para cada eje:

```text
fconst_1
getfield scaleX/scaleY/scaleZ
ldc 1.0E-4f
invokestatic java/lang/Math.max:(FF)F
fdiv
...
PoseStack.scale(FFF)
```

Es decir, la regla de la fuente es literalmente:

```java
1F / Math.max(scale, 0.0001F)
```

No es `1F / scale` ni preserva necesariamente el signo.

## Caso adversarial

La fixture parte del `ModelGrizzlyBear` real y muta únicamente la escala de `body` a `(-1.5, 0.75, 1.25)`. Se prueban dos variantes sobre material fresco:

1. `scaleChildren=true`, para comprobar que la escala fuente firmada se conserva end-to-end;
2. `scaleChildren=false`, para comprobar la compensación exacta del dialecto y comparar después los colliders contra los vertices del renderer real.

Para la variante `scaleChildren=false`, el helper correcto según el bytecode fijado es:

- X: `1 / max(-1.5, 0.0001) = 10000`;
- Y: `1 / max(0.75, 0.0001) = 1.3333334`;
- Z: `1 / max(1.25, 0.0001) = 0.8`.

Aunque `10000` sea una semántica poco intuitiva para una reflexión negativa, S19 exige fidelidad al renderer de aceptación, no sustituir su comportamiento por una regla geométricamente más elegante.

## Rojo inicial y reclasificación

La primera versión del holdout esperaba erróneamente `(-2/3, 4/3, 0.8)`.

Workflow: `s19-signed-scale-children-proof`

- run `34893813120`;
- job `104142688968`;
- artifact `10367213456`;
- SHA-256 `957f40f1f2c1da5b9ab3f09c0ed64e80e092c191f63108ddae01334b389381b7`.

El control `scaleChildren=true` pasó y la variante `false` produjo:

```text
expected=[-0.6666667, 1.3333334, 0.8]
actual=[10000.0, 1.3333334, 0.8]
```

Ese rojo **no demuestra un defecto productivo**. Demuestra que el oracle había impuesto una semántica distinta de la fuente. La inspección posterior del bytecode exacto confirmó que el valor `10000` es el resultado que produce Citadel 2.1.9 para esa entrada.

## Oracle corregido

`S19SignedScaleChildrenClientProof` queda redefinido para:

- conservar y comprobar la escala fuente negativa en `SourcePose`;
- exigir exactamente `1 / Math.max(scale, 1e-4)` en el helper `unscaled_children`;
- mantener una entrada negativa para que el test ejerza específicamente la rama clamp a `10000`;
- comparar los colliders resultantes con los vertices capturados del renderer real.

La lane `s19-signed-scale-children-proof` sigue siendo útil: ahora protege la semántica peculiar del dialecto en lugar de intentar corregirla.

## Regla de cierre adversarial

Antes de cerrar esta superficie de S19 deben cumplirse las dos condiciones siguientes:

1. el holdout corregido debe pasar sobre los jars pinned;
2. un mutante que sustituya la regla `1 / Math.max(scale, 1e-4)` por el recíproco firmado `1 / scale` debe morir causalmente en esta lane.

No hay corrección productiva solicitada por esta evidencia. La revisión adversarial sí deja una lección de contrato: en extractores versionados, una conducta fuente extraña sigue siendo autoridad mientras el sprint prometa reproducir exactamente ese dialecto.
