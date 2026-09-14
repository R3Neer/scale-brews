# S18 — evidencia adversarial de cuantización temporal en `applyWalk`

Estado: **ROJO causal confirmado**.

Fecha: 2026-09-14

## Resumen

Tras aislar por separado el `scaleFactor` de amplitud de `KeyframeAnimation.applyWalk(...)`, una segunda sonda demuestra otra divergencia independiente: Minecraft 26.2 convierte el reloj de locomoción a **milisegundos enteros (`long`) antes de muestrear la animación**, mientras el binding neutral actual transforma `walk_phase` directamente a segundos `float` y conserva la fracción sub-milisegundo.

Con curvas suficientemente sensibles, ambos caminos producen poses observables distintas aun con amplitud exactamente igual a `1`.

## Oracle fuente Minecraft 26.2

Fuente inspeccionada: `Renekovski/26.2-mcp`, commit `51f3128ba7265299dce20b9abd2b37d7dd27d096`, `src/net/minecraft/client/animation/KeyframeAnimation.java`.

`applyWalk(...)` calcula:

```java
long time = (long)(animationPos * 50.0F * speedFactor);
float scale = Math.min(animationSpeed * scaleFactor, 1.0F);
this.apply(time, scale);
```

La conversión a `long` ocurre **antes** de `apply(...)`. Después `getElapsedSeconds(...)` divide esos milisegundos ya truncados entre `1000.0F`.

Por tanto, para locomoción Mojang no evalúa una curva continua en `animationPos * 0.05 * speedFactor`; evalúa esa magnitud cuantizada hacia cero a resolución de 1 ms.

## Divergencia productiva

El selector `walk_phase` de `MojangKeyframePoseEngine` devuelve actualmente:

`inputs.walkPhase() * clockScale`

Eso conserva toda la precisión `float`. No existe en el camino de evaluación una cuantización equivalente a la conversión a milisegundos enteros de `applyWalk`.

Este defecto es independiente del gap de `scaleFactor` de amplitud. El holdout usa amplitud `one` y llama al oracle nativo con una combinación que también produce amplitud nativa exactamente `1`.

## Holdout

Commit adversarial: `13d5ec3737d65744242c1d2c8e7bf76020ffb885` (`test(s18): probe applyWalk clock quantization`).

Se añadieron únicamente:

- `S18ApplyWalkClockQuantizationClientTests`;
- workflow aislado `s18-apply-walk-clock-quantization-proof`.

No se modificó producción.

Fixture:

- definición no-looping de `2s`;
- POSITION X de `0` a `1600` píxeles entre `t=0` y `t=2`; la pendiente alta hace observable la diferencia sub-milisegundo sin recurrir a ningún valor inválido;
- `walkPhase=5.019`;
- `speedFactor=2`;
- amplitud nativa `1 * 1 = 1`;
- binding neutral `clock=walk_phase`, `clock_scale=0.1`, `amplitude=one`.

Minecraft calcula primero aproximadamente `501.9ms` y lo convierte a `501ms`. El neutral evalúa aproximadamente `501.9ms`.

## Evidencia CI

Workflow: `s18-apply-walk-clock-quantization-proof`, run `34855590656`.

Job `client-apply-walk-clock` (`104014189956`): **FAILURE** únicamente en el holdout semántico; setup y captura del artifact completaron correctamente.

Contraejemplo:

`applyWalk clock quantization parity phase=5.019 matrix[12] expected=25.05 actual=25.095001`

La diferencia es `0.045001` bloques en la traslación local X del fixture, muy por encima del epsilon de comparación `3e-5`.

Artifact: `S18-apply-walk-clock-quantization-proof`, id `10352554957`, SHA-256 `7b7bb72c2f9a004faeaad73f04743f6eca72da8830979589feec81f6ef3bd29b`.

## Clasificación TM

**Bug productivo de paridad del reloj locomotor.**

No amplía el freeze de S18: el sprint exige conversión de unidades una sola vez y documentada, casos walking reproducibles y equivalencia contra la fuente original. Si `walk_phase` pretende representar la aplicación locomotora Mojang, su transformación temporal debe conservar también la cuantización observable que introduce `applyWalk`.

La solución no queda prescrita. Puede formar parte de un selector locomotor neutral más rico, de la preparación del binding o de una transformación server-safe equivalente. No debe introducir reloj de render ni dependencia cliente en runtime.

Este rojo es independiente de:

1. la normalización incorrecta de tiempos negativos en loops;
2. la imposibilidad actual de expresar `min(walkAmount * scaleFactor, 1)`.

S18/G3.4 continúa abierto hasta resolver los tres oracles y completar después una nueva segunda lectura sin cambios productivos necesarios.
