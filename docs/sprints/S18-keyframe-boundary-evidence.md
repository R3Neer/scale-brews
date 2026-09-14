# S18 — evidencia adversarial de fronteras de keyframes

Estado: **ROJO causal confirmado**.

Fecha: 2026-09-14

## Resumen

La revisión adversarial de `PoseProgramEvaluator` encontró una divergencia semántica con `AnimationDefinition` de Minecraft 26.2 para canales válidos con `preTarget != postTarget` en su frontera inicial.

El primer hallazgo apareció en un canal de **un único keyframe**, cuyo special-case neutral era:

```java
if (frames.size() == 1) {
    var only = frames.getFirst();
    return time <= only.timestamp() ? only.preTarget() : only.postTarget();
}
```

Un segundo holdout diferencial demuestra que el defecto **no se limita a ese special-case**: un canal LINEAR ordinario con dos keyframes también diverge antes del primer timestamp por la regla general `time <= first.timestamp() -> first.preTarget()`.

El comportamiento original de Mojang no coincide con ninguna de esas dos reglas.

## Holdout 1 — único keyframe

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

### Evidencia CI

Workflow: `s18-single-keyframe-proof`, run `34849478744`.

Job `client-single-keyframe` `103993478599`: **FAILURE causal**.

Artifact: `10350420390` (`S18-single-keyframe-semantic-proof`).

SHA-256: `10e2a75f4d89f41b29fa501f21efbd098716d3408809292eff681e6a2df41539`.

Primer fallo exacto del cliente real:

`single-keyframe parity seconds=0.0 matrix[12] expected=0.75 actual=0.25`

Como la posición base de la cabeza es la misma en ambos caminos, la diferencia corresponde exactamente a los offsets X del keyframe:

- Mojang aplica `12 px / 16 = 0.75`;
- el evaluator neutral aplica `4 px / 16 = 0.25`.

## Holdout 2 — frontera inicial multikeyframe

Commit del test: `99806559f705159c01bf8b25790ac288cbc1bf98` (`test(s18): probe multi-keyframe first boundary parity`).

Lane aislada: `6324832146f8ef74242a68b0e199f1746eafaacd` (`ci(s18): isolate multi-keyframe first boundary probe`).

Fixture LINEAR sobre `CowModel`, bone `head`:

- primer keyframe en `t=0.5 s`, `pre=4`, `post=12` px;
- segundo keyframe en `t=2 s`, `pre=post=20` px;
- muestras `t=0`, `0.499`, `0.5`, `0.501`, `1`.

Workflow `s18-first-keyframe-boundary-proof`, run `34850265081`.

Job `client-first-keyframe-boundary` `103996129105`: **FAILURE causal**.

Artifact `10350556473` (`S18-first-keyframe-boundary-proof`), SHA-256 `9c5038f6fc061785a76cf64b07556df84c0d274bbb0f4ee01181d05098f8f3b1`.

Primer fallo exacto:

`first-keyframe boundary parity seconds=0.0 matrix[12] expected=0.75 actual=0.25`

De nuevo Mojang aplica el `postTarget` de 12 px antes del primer timestamp, mientras el evaluator neutral devuelve `preTarget` de 4 px. Esto mata explícitamente una reparación estrecha que modificase sólo `frames.size()==1`.

## Clasificación TM

**Bug productivo de semántica de frontera inicial en `PoseProgramEvaluator.sample(...)`.**

No amplía el contrato S18. El sprint ya exige preservar `preTarget/postTarget` y comparar LINEAR/CATMULL contra la fuente original. Las fixtures cubren formas válidas del mismo `AnimationDefinition` que el oracle histórico no ejercitaba.

La reparación debe reproducir la selección original de Mojang para la frontera anterior/al primer keyframe tanto en canales de uno como de múltiples keyframes, no introducir una convención propia basada en «antes = pre / después = post».

CATMULL_ROM mantiene evidencia separada porque su divergencia ocurre además dentro de tramos interpolados y responde a la selección de vecinos de spline, no sólo a esta frontera LINEAR.
