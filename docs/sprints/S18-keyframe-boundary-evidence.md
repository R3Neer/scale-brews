# S18 — evidencia adversarial de fronteras de keyframes

Estado: **ROJO causal confirmado**.

Fecha: 2026-09-14

## Resumen

La revisión adversarial de `PoseProgramEvaluator` encontró una divergencia semántica con `AnimationDefinition` de Minecraft 26.2 para un canal válido que contiene **un único keyframe con `preTarget != postTarget`**.

La implementación neutral posee un special-case:

```java
if (frames.size() == 1) {
    var only = frames.getFirst();
    return time <= only.timestamp() ? only.preTarget() : only.postTarget();
}
```

El comportamiento original de Mojang no coincide con esa regla.

## Holdout diferencial

Commit del test: `1486a7a02ee61c88cd33b12c8a05e9ad398b9174` (`test(s18): probe single-keyframe discontinuity parity`).

Lane aislada: `f9d1c52a30ccf39bd457e04dab3bf1fefc7d143f` (`ci(s18): isolate single-keyframe semantic probe`).

La fixture usa un `CowModel` real y un `AnimationDefinition` 26.2 con:

- bone `head`;
- target `POSITION`;
- duración 2 s, no loop;
- un único keyframe en `t=1 s`;
- `preTarget = posVec(4,0,0)`;
- `postTarget = posVec(12,0,0)`;
- interpolación LINEAR.

El test compila la definición con `AnimationDefinitionCompiler`, liga el `PoseProgram` contra la geometría extraída del mismo `CowModel` y compara matrices locales contra la aplicación nativa de Mojang en `t = 0`, `0.999`, `1`, `1.001` y `2`.

## Evidencia CI

Workflow: `s18-single-keyframe-proof`, run `34849478744`.

Job `client-single-keyframe` `103993478599`: **FAILURE causal**.

Artifact: `10350420390` (`S18-single-keyframe-semantic-proof`).

SHA-256: `10e2a75f4d89f41b29fa501f21efbd098716d3408809292eff681e6a2df41539`.

Primer fallo exacto del cliente real:

`single-keyframe parity seconds=0.0 matrix[12] expected=0.75 actual=0.25`

Como la posición base de la cabeza es la misma en ambos caminos, la diferencia corresponde exactamente a los offsets X del keyframe:

- Mojang aplica `12 px / 16 = 0.75`;
- el evaluator neutral aplica `4 px / 16 = 0.25`.

Por tanto el rojo no procede de extracción, rest pose, Euler, escala firmada ni del compiler: la diferencia está en la selección `preTarget/postTarget` durante la evaluación del canal de un único keyframe.

## Clasificación TM

**Bug productivo de semántica de keyframe en `PoseProgramEvaluator.sample(...)`.**

No amplía el contrato S18. El sprint ya exige preservar `preTarget/postTarget` y comparar LINEAR/CATMULL contra la fuente original. La fixture simplemente cubre una forma válida del mismo `AnimationDefinition` que el oracle histórico no ejercitaba.

La reparación debe reproducir la selección original de Mojang para la frontera de un único keyframe, no introducir una convención propia basada en «antes = pre / después = post».

## Siguiente holdout

Se ha preparado por separado un diferencial CATMULL_ROM con `pre/post` distintos en los keyframes de borde. Su objetivo es comprobar si la divergencia se limita al special-case de un único frame o si la selección de vecinos fantasma de `PoseProgramEvaluator.catmull(...)` también difiere de Mojang.
