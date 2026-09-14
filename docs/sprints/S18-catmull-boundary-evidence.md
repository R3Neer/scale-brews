# S18 — evidencia adversarial de vecinos CATMULL_ROM en los bordes

Estado: **ROJO causal confirmado en ambos extremos**.

Fecha: 2026-09-14

## Resumen

La comparación diferencial contra `AnimationDefinition` de Minecraft 26.2 demuestra que `PoseProgramEvaluator` no reproduce la selección de puntos de control usada por Mojang en los tramos de borde de `CATMULL_ROM` cuando los keyframes extremos tienen `preTarget != postTarget`.

No es el mismo fallo que la frontera inicial LINEAR: las fixtures CATMULL usan **tres keyframes** y fallan dentro de tramos interpolados, lejos de cualquier clamp temporal exterior.

Además, dos holdouts independientes aíslan ya los extremos izquierdo y derecho. Una reparación parcial de un solo punto fantasma no basta.

## Holdout izquierdo / general

Commit del test: `4bfeeb7724ecea3b011af66ecc4b1f3ed4536da2` (`test(s18): probe CATMULL endpoint neighbor parity`).

Lane aislada: `777837bbb6b6165f21fffcf87cb99b72a50b0680` (`ci(s18): isolate CATMULL endpoint neighbor probe`).

Fixture sobre `CowModel`, bone `head`, target `POSITION`, duración 2 s, no loop, con tres keyframes CATMULL_ROM:

- `t=0`: `pre=-20`, `post=0` px en X;
- `t=1`: `pre=8`, `post=12` px en X;
- `t=2`: `pre=20`, `post=40` px en X.

Workflow `s18-catmull-boundary-proof`, run `34849734168`.

Job `client-catmull-boundary` `103994335197`: **FAILURE causal**.

Artifact `10349527118` (`S18-catmull-boundary-semantic-proof`), SHA-256 `13b93898f54bc3f4d1cbecda1824d5c180b8b8ab12b4e383c5bcda361e80a90d`.

Primer fallo exacto:

`CATMULL boundary parity seconds=0.1 matrix[12] expected=0.040125 actual=0.07925`

La divergencia ocurre dentro del primer tramo `[0,1]`.

## Holdout derecho aislado

Commit del test: `c4541ee2e1a91859ab37a395f511e6626779ced3` (`test(s18): isolate CATMULL right endpoint parity`).

Lane aislada: `583dd621ec920d68ea7d8c6d31ad10bcda336290` (`ci(s18): isolate CATMULL right endpoint probe`).

La fixture mantiene `pre==post==0` en el primer keyframe para neutralizar la ambigüedad del borde izquierdo y sólo muestrea el segundo tramo, con el último keyframe discontinuo `pre=20`, `post=40` px.

Workflow `s18-catmull-right-boundary-proof`, run `34850311503`.

Job `client-catmull-right-boundary` `103996286453`: **FAILURE causal**.

Artifact `10350601462` (`S18-catmull-right-boundary-proof`), SHA-256 `c5e8facfb974707387c53bc8e3a7bb5aaac9bb2f8d7c8413d896947217ea3888`.

Primer fallo exacto:

`CATMULL right-boundary parity seconds=1.1 matrix[12] expected=0.892375 actual=0.80675`

La divergencia aparece a `t=1.1`, dentro del segundo tramo `[1,2]`, por lo que prueba de forma independiente que la regla del vecino fantasma derecho también es incorrecta.

## Causa

La implementación neutral actual construye los cuatro puntos CATMULL así:

```java
var p0 = previous > 0 ? frames.get(previous - 1).postTarget() : a.preTarget();
var p3 = next + 1 < frames.size() ? frames.get(next + 1).preTarget() : b.postTarget();
```

En otras palabras, cuando no existe vecino exterior inventa el punto fantasma izquierdo a partir de `first.preTarget` y el derecho a partir de `last.postTarget`.

La aplicación original de Mojang no produce esas curvas. Los diferenciales reales obligan a reproducir la misma selección/clamp de keyframes que emplea `KeyframeAnimations`, en vez de derivar una convención propia para puntos fantasma.

## Clasificación TM

**Bug productivo de interpolación CATMULL_ROM en `PoseProgramEvaluator.sample(...)`, confirmado en ambos bordes.**

No se añade una nueva capacidad: S18 ya exige preservar las interpolaciones Mojang y compararlas contra la fuente original. El oracle histórico sí ejercitaba CATMULL, pero sus keyframes extremos tenían `preTarget == postTarget`, por lo que las reglas erróneas de borde coincidían accidentalmente con la fuente y ocultaban el defecto.

Los dos rojos deben permanecer separados durante la reparación para impedir un fix asimétrico que corrija sólo `p0` o sólo `p3`.
