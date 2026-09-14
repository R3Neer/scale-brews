# S18 — evidencia adversarial de vecinos CATMULL_ROM en los bordes

Estado: **ROJO causal confirmado**.

Fecha: 2026-09-14

## Resumen

La comparación diferencial contra `AnimationDefinition` de Minecraft 26.2 demuestra que `PoseProgramEvaluator` no reproduce la selección de puntos de control usada por Mojang en los tramos de borde de `CATMULL_ROM` cuando los keyframes extremos tienen `preTarget != postTarget`.

No es el mismo fallo que el special-case de un único keyframe: esta fixture usa **tres keyframes** y falla dentro del primer intervalo, lejos de cualquier clamp temporal exterior.

## Holdout

Commit del test: `4bfeeb7724ecea3b011af66ecc4b1f3ed4536da2` (`test(s18): probe CATMULL endpoint neighbor parity`).

Lane aislada: `777837bbb6b6165f21fffcf87cb99b72a50b0680` (`ci(s18): isolate CATMULL endpoint neighbor probe`).

Fixture sobre `CowModel`, bone `head`, target `POSITION`, duración 2 s, no loop, con tres keyframes CATMULL_ROM:

- `t=0`: `pre=-20`, `post=0` px en X;
- `t=1`: `pre=8`, `post=12` px en X;
- `t=2`: `pre=20`, `post=40` px en X.

Se comparan matrices locales nativas y neutrales en tiempos internos de ambos tramos: `0.1`, `0.25`, `0.75`, `1.25`, `1.75`, `1.999` s.

## Evidencia CI

Workflow: `s18-catmull-boundary-proof`, run `34849734168`.

Job `client-catmull-boundary` `103994335197`: **FAILURE causal**.

Artifact: `10349527118` (`S18-catmull-boundary-semantic-proof`).

SHA-256: `13b93898f54bc3f4d1cbecda1824d5c180b8b8ab12b4e383c5bcda361e80a90d`.

Primer fallo exacto:

`CATMULL boundary parity seconds=0.1 matrix[12] expected=0.040125 actual=0.07925`

La divergencia ocurre a `t=0.1`, dentro del primer tramo `[0,1]`. Por tanto no procede del tratamiento posterior al último keyframe ni del special-case `frames.size()==1`.

## Causa

La implementación neutral actual construye los cuatro puntos CATMULL así:

```java
var p0 = previous > 0 ? frames.get(previous - 1).postTarget() : a.preTarget();
var p3 = next + 1 < frames.size() ? frames.get(next + 1).preTarget() : b.postTarget();
```

En otras palabras, cuando no existe vecino exterior inventa el punto fantasma izquierdo a partir de `first.preTarget` y el derecho a partir de `last.postTarget`.

La aplicación original de Mojang no produce esa curva. El diferencial real obliga a tratar los extremos con la misma selección/clamp de keyframes que emplea `KeyframeAnimations`, en vez de derivar una convención propia para puntos fantasma.

## Clasificación TM

**Bug productivo de interpolación CATMULL_ROM en `PoseProgramEvaluator.sample(...)`.**

No se añade una nueva capacidad: S18 ya exige preservar las interpolaciones Mojang y compararlas contra la fuente original. El oracle histórico sí ejercitaba CATMULL, pero sus keyframes extremos tenían `preTarget == postTarget`, por lo que ambas reglas de borde coincidían accidentalmente y ocultaban el defecto.

Este rojo debe permanecer separado del fallo de keyframe único para impedir una reparación estrecha limitada al `if (frames.size() == 1)`.
