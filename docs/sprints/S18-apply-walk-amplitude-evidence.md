# S18 — evidencia adversarial de `applyWalk` y escala de amplitud

Estado: **ROJO causal confirmado**.

Fecha: 2026-09-14

## Resumen

La segunda lectura adversarial de S18 encontró una divergencia independiente en la reproducción de `KeyframeAnimation.applyWalk(...)` de Minecraft 26.2.

El binding neutral actual puede expresar el `speedFactor` temporal mediante `clock=walk_phase` + `clock_scale`, pero `amplitude=walk_amount` usa el valor crudo de `PoseEngine.Inputs.walkAmount()` y no dispone de una forma equivalente de expresar la semántica Mojang:

`min(animationSpeed * scaleFactor, 1.0F)`

Esto no es un caso hipotético: modelos vanilla 26.2 usan `scaleFactor` distintos de 1 de forma habitual.

## Oracle fuente Minecraft 26.2

Fuente inspeccionada: `Renekovski/26.2-mcp`, commit `51f3128ba7265299dce20b9abd2b37d7dd27d096`, `src/net/minecraft/client/animation/KeyframeAnimation.java`.

`applyWalk(...)` hace exactamente:

```java
long time = (long)(animationPos * 50.0F * speedFactor);
float scale = Math.min(animationSpeed * scaleFactor, 1.0F);
this.apply(time, scale);
```

Por tanto la semántica locomotora original posee dos transformaciones independientes:

1. `speedFactor` escala el reloj;
2. `scaleFactor` escala la amplitud y después la satura a `1.0F`.

Usos vanilla 26.2 observados incluyen, entre otros:

- Camel: `applyWalk(..., 2.0F, 2.5F)`;
- Frog: `applyWalk(..., 1.0F/1.5F, 2.5F)`;
- Sniffer: `applyWalk(..., 9.0F, 100.0F)`;
- Armadillo: `applyWalk(..., 16.5F, 2.5F)`;
- Baby Fox: `scaleFactor=2.5F`;
- Nautilus: `scaleFactor=3.0F`;
- Baby Axolotl: `scaleFactor=30.0F`.

No es correcto resolverlo hardcodeando un multiplicador concreto ni alterando globalmente el significado de `walk_amount`.

## Gap productivo

`MojangKeyframePoseEngine` acepta actualmente:

- clocks `static`, `age`, `walk_phase`, `channel:*`;
- amplitudes `one`, `walk_amount`, `constant:*`, `channel:*`.

`walk_phase` + `clock_scale` puede reproducir `animationPos * 50ms * speedFactor` en segundos de programa.

Sin embargo, `walk_amount` devuelve directamente `Inputs::walkAmount`. No existe en el binding actual un operador neutral equivalente a `min(walkAmount * scaleFactor, 1)` con `scaleFactor` específico de cada aplicación de la animación.

El runtime tampoco introduce esa transformación después: `ModelGeometryProvider` entrega el `PoseEngine.Inputs` autoritativo al evaluator ligado. Por tanto la información/semántica debe estar representada en el binding/preparación neutral o en una fuente autoritativa equivalente.

## Holdout

Commit adversarial: `d039725dfac501a690ea20eb6361f1b9dbadea0b` (`test(s18): expose applyWalk amplitude-scale gap`).

Se añadieron únicamente:

- `S18ApplyWalkSemanticClientTests`;
- workflow aislado `s18-apply-walk-proof`.

No se modificó producción.

Fixture:

- `AnimationDefinition` no-looping de `2s`;
- POSITION de `head`: X de `0` a `16` píxeles entre `t=0` y `t=2`;
- oracle nativo: `applyWalk(walkPhase, walkAmount, 2.0F, 2.5F)`;
- binding neutral más cercano actualmente representable:
  - `clock=walk_phase`;
  - `clock_scale=0.1` porque `50ms * 2 / 1000 = 0.1s` por unidad de phase;
  - `amplitude=walk_amount`.

El primer caso usa `walkPhase=5` y `walkAmount=0.2`, por lo que el tiempo es exactamente `500ms`. Así se elimina la cuantización temporal como explicación alternativa.

## Evidencia CI

Workflow: `s18-apply-walk-proof`, run `34854997653`.

Job `client-apply-walk` (`104012135231`): **FAILURE** únicamente en el holdout semántico; setup y captura de artifact completaron correctamente.

Contraejemplo:

`applyWalk parity phase=5.0 amount=0.2 matrix[12] expected=0.125 actual=0.05`

Descomposición:

- a `t=0.5s`, la curva sin amplitud vale `4` píxeles = `0.25` bloques;
- Minecraft calcula amplitud `min(0.2 * 2.5, 1) = 0.5` y produce `0.125` bloques;
- el engine neutral usa amplitud cruda `0.2` y produce `0.05` bloques.

Artifact: `S18-apply-walk-semantic-proof`, id `10351869438`, SHA-256 `6d837b1f5834a4616ca6927895ab4f676db18163adaa2ee07ec42aebb3b24eab`.

## Clasificación TM

**Gap productivo de expresividad/paridad de binding locomotor.**

No amplía arbitrariamente el freeze de S18. El sprint ya exige:

- clocks/amplitudes explícitos y autoritativos;
- reproducción de casos walking/static;
- equivalencia de programas Mojang con su fuente original;
- conversión de unidades una sola vez y documentada.

La solución concreta no queda prescrita por el adversario. Puede ser un multiplicador/clamp de amplitud validado, una estructura neutral equivalente, un channel derivado autoritativo por binding u otra representación server-safe y revision-bound. Lo normativo es poder expresar el `scaleFactor` y su clamp sin hardcodes por entidad ni estado global.

S18/G3.4 continúa abierto. Para cerrarlo deben volver a verde tanto este oracle como el de tiempo negativo y después completarse una nueva segunda lectura sin cambios productivos necesarios.
