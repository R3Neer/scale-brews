# S18 — evidencia adversarial de tiempo negativo en loops

Estado: **ROJO causal confirmado**.

Fecha: 2026-09-14

## Resumen

La segunda lectura adversarial posterior a la corrección general de selección de keyframes encontró una divergencia adicional entre `PoseProgramEvaluator` y Minecraft 26.2 para programas `looping=true` evaluados con tiempo negativo.

El caso es alcanzable por contrato: `MojangKeyframePoseEngine` acepta `clock_scale` finito y distinto de cero, incluidas escalas negativas, y también clocks por channel cuyo valor puede ser negativo.

## Oracle fuente Minecraft 26.2

Fuente inspeccionada: `Renekovski/26.2-mcp`, commit `51f3128ba7265299dce20b9abd2b37d7dd27d096`, `src/net/minecraft/client/animation/KeyframeAnimation.java`.

`KeyframeAnimation#getElapsedSeconds(long)` calcula el tiempo looping como resto Java directo:

`secondsSinceStart % definition.lengthInSeconds()`

No normaliza un resto negativo al intervalo positivo `[0,duration)`.

Después, `KeyframeAnimation.Entry#apply(...)` obtiene `prev/next` mediante `Mth.binarySearch` y clampa el factor de interpolación. Para tiempo anterior al primer keyframe, la interpolación queda anclada en el primer tramo con `alpha=0`.

La fuente 26.2 de `AnimationChannel` confirma además que `LINEAR` interpola `postTarget(prev)` -> `preTarget(next)` y que `CATMULLROM` usa los cuatro `postTarget` con índices clampados.

## Divergencia productiva

`PoseProgramEvaluator.normalize(...)` hace actualmente:

1. `result = time % duration`;
2. si `result < 0`, devuelve `result + duration`.

Eso convierte, por ejemplo, `t=-0.25` en un programa de duración `2s` en `t=1.75`, mientras Minecraft 26.2 conserva `-0.25` y termina muestreando el comienzo de la animación.

No es una diferencia numérica menor: cambia de extremo del ciclo y puede producir una pose totalmente distinta.

## Holdout

Commit adversarial: `e5679af922c3714a8ec1fff0f746e58b9ef27db8` (`test(s18): probe negative looping time semantics`).

Se añadieron únicamente:

- `S18NegativeLoopSemanticClientTests`, oracle diferencial contra `AnimationDefinition` original de Minecraft 26.2;
- workflow aislado `s18-negative-loop-proof`.

No se modificó producción.

Fixture:

- duración `2s`, `looping=true`;
- track POSITION de `head` con X = `0`, `8`, `16` píxeles en `t=0`, `1`, `2`;
- tiempos de prueba `-0.25`, `-1`, `-2.25`, `-4.25`;
- amplitud `1`.

## Evidencia CI

Workflow: `s18-negative-loop-proof`, run `34853792937`.

Job `client-negative-loop` (`104008039243`): **FAILURE** en el holdout semántico.

Primer contraejemplo:

`negative loop parity seconds=-0.25 matrix[12] expected=0.0 actual=0.875`

Es decir:

- Minecraft original permanece en la posición inicial X = `0` bloques;
- el evaluator neutral envuelve `-0.25` a `1.75` y produce X = `14/16 = 0.875` bloques.

Artifact: `S18-negative-loop-semantic-proof`, id `10351524499`, SHA-256 `e9901228826c67f8489625448ab546d44c41fe5f6609010aa33c8984e67fbda6`.

## Clasificación TM

**Bug productivo de paridad temporal.** No amplía el freeze de S18: el sprint ya exige equivalencia con la fuente original, loop determinista y clocks explícitos. Una entrada temporal aceptada por el engine debe conservar la semántica de Minecraft 26.2.

La corrección causal esperable está en la normalización looping del evaluator, no en rechazar clocks negativos para ocultar la divergencia, salvo que PLANNER cambie explícitamente el contrato.

S18/G3.4 continúa abierto hasta que el oracle vuelva a verde y una nueva segunda lectura no encuentre cambios productivos necesarios.
