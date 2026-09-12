# S09 — Hardening posterior al cierre

Estado: **COMPLETADO**. Este addendum amplía la evidencia histórica de `S09-multicontact-sliding-recovery.md` después de revisiones adversariales posteriores durante S12/S13.

## Motivo de reapertura

La revisión post-verde de S12 volvió a ejecutar la lane preparada de geometría real en una colocación mundial distinta y reabrió dos defectos de S09 que no justificaban cambiar ownership ni budgets:

1. `ConservativeSweep` consideraba contacto hasta `SKIN + numericalGapTolerance(...)` en coordenadas mundiales grandes, mientras los certificados de clearance de `TemporalResponse` podían declarar `CLEAR` usando sólo `SKIN`.
2. Tras alinear ambos umbrales, A9 volvió a agotar el budget compartido de 256 por orden de trabajo: piezas CLEAR lexicográficamente anteriores consumían evaluaciones de intervalo completo antes de consultar la pieza causal; tras el primer contacto, piezas no activas podían volver a pagar trabajo antes del recontacto del manifold ya conocido.

No se aumentó `QUERY_BUDGET`, no se relajó `fail-closed` y no se cambió la semántica de contacto simultáneo dentro de `TIME_EPS`.

## Rojo de precisión numérica

`S09TranslationMetamorphismTests.temporalClearanceCannotUndercutCcdNumericContactBand` fija una configuración a coordenadas de borde mundial cuyo gap final queda por encima de `SKIN` pero dentro de la banda numérica de contacto del CCD.

Baseline rojo `8f335aaf623ad663f576a971d47f9fdbdaf74296`:

- gap final: `1.0095536708831787E-6`;
- `ConservativeSweep`: `CONTACT`, 28 evaluaciones;
- `TemporalResponse`: `COMPLETE`, cero contactos, 34 evaluaciones.

La causa era un certificado superior autorizado a probar `CLEAR` con un umbral más estricto que la consulta exacta que sustituía.

## Reparación de clearance

- `b9cd7317fedbb4a260757f28312f6ac764c76b9b` comparte `ConservativeSweep.contactClearance(body, material)` como política única de contacto numérico.
- `2cd5b1da45f6a04e0874899e0374bad5b621e367` aplica esa misma clearance a los certificados temporales y a fast paths que pueden evitar CCD exacto (`certifiedClearWindow`, `certifiedClearTrajectory`, `certifies` y la prevalidación de `q`).

El screen ya no puede demostrar `CLEAR` dentro de una banda que el CCD exacto clasifica como contacto.

## Reparación de budget A9 sin aumentar 256

La clearance correcta hizo visible un segundo problema de scheduling. En una ejecución prepared real, el contacto causal de `root/head/cube_0` se detectaba correctamente alrededor de `t≈0.28423`, pero el manifold completo agotaba 256 porque el solver recorría el trabajo en orden lexicográfico puro.

`e1fc4893f069eb2b60fc1fdd352107dc0205bddb` conserva el conjunto de piezas y la comparación exacta de earliest-contact, pero cambia únicamente el **orden de trabajo** de forma determinista:

- si ya existe un manifold activo, sus piezas se consultan primero para que un recontacto a `t=0` fije inmediatamente el horizonte de las piezas no activas;
- en un manifold nuevo pequeño (4–64 piezas), una muestra central presupuestada por pieza prioriza primero las piezas que realmente solapan la trayectoria en el midpoint; todas las piezas siguen consultándose y el desempate sigue siendo lexicográfico;
- el horizonte posterior conserva `earliest + TIME_EPS`, no `earliest`, para no perder contactos simultáneos dentro de tolerancia;
- todas las muestras auxiliares consumen el mismo budget global; no existe cap local nuevo ni una ruta de éxito fuera del accounting normativo.

Por tanto, la frase histórica de S09 que decía que `TemporalResponse` “mantiene el orden canónico de piece ids” debe leerse como **orden determinista/canónico de desempate y resultados**, no como obligación de evaluar físicamente todas las piezas en lexicografía pura. El scheduler puede priorizar trabajo causal sin alterar qué piezas se consideran ni cómo se selecciona el primer contacto.

## Primera evidencia final de esta reapertura

Snapshot `e1fc4893f069eb2b60fc1fdd352107dc0205bddb`:

- Suite ordinaria: run `34693722959`, **374/374 GameTests verdes**. Incluye el holdout de clearance ULP, simultaneidad dentro de `TIME_EPS`, budgets y regresiones S05–S12.
- Lane preparada: run `34693722977`, job `103553460165`, **verde**.
  - exportación cliente original correcta (`minecraft:cow` 240 vertices / 10 pieces, más player wide/slim);
  - servidor preparado aislado: **2/2 GameTests verdes**;
  - A9 completa de nuevo bajo el budget compartido de 256 con geometría real exportada.

## Reapertura posterior durante S13: corrección `q`

La validación prepared del stale-fence espacial S13, sobre `3cd0209b1dd1d01cd2ce949b9a17b0e229411274`, reabrió A9 de forma independiente de la arquitectura espacial:

- prepared run `34695379182`, job `103557851681`;
- el contacto causal de cow se encontraba, pero el manifold terminaba en `ITERATION_LIMIT` con **256/256 evaluaciones** alrededor de `t≈0.28427`;
- la pieza en la que se agotaba el budget, aislada, completaba en **22** evaluaciones; el sweep directo de referencia quedaba en **16**;
- la extracción cliente era verde, por lo que el defecto estaba en el consumo de budget post-contacto de `TemporalResponse`, no en S13 ni en la geometría preparada.

La causa era `validCorrection(q)`: para cada pieza estática en el instante de corrección pagaba primero una muestra/separación de endpoint y después otra consulta para validar el trayecto `q`. Era trabajo duplicado sobre el mismo convexo.

`8910564e53e9752c0a96646d38aaf284b7724530` eliminó la doble carga, pero una revisión inmediata detectó que usar sólo el fast path estático de `ConservativeSweep` podía perder la banda numérica `SKIN + ULP` en el endpoint.

`de44c78c75bcc54ef423af783d35761014b49356` dejó la reparación final:

- una sola muestra presupuestada del convexo estático por pieza;
- `ConvexBox.sweep(body,q)` valida el trayecto real de la corrección;
- `separation(body.move(q))` más `ConservativeSweep.contactClearance(...)` preservan la misma banda numérica final usada por el CCD temporal;
- no se aumenta `QUERY_BUDGET`, no cambia `SKIN`, no cambia `TIME_EPS` y `fail-closed` permanece intacto.

Evidencia sobre `de44c78...`:

- ordinary run `34695839944`, job `103559056801`: **380/380 required GameTests passed**; artifact `10299110544`, SHA-256 `58c33a23d8a8cbd9256736e56d55ffeee43b93cb7b02dcf5df2e8d78d1b7985b`;
- prepared run `34695839860`, job `103559056605`: export original verde y servidor aislado **2/2** verde.

## Holdout final independiente de la colocación aleatoria

`a1f1a568726d95664c6e8b4a144659c36be0e5cc` endurece `S09PreparedIntermediateContactProof`: la misma geometría real de cow y el mismo contacto estrictamente intermedio se reejecutan trasladados a cuatro offsets mundiales explícitos, incluyendo magnitudes de millones de bloques y signos distintos.

- ordinary run `34696004871`: verde;
- prepared run `34696004864`, job `103559482598`: **verde**;
- export original: cow **240 vertices / 10 pieces**, player wide/slim **144 vertices / 6 pieces**, 80 comparaciones animadas por modelo y **640** comparaciones adicionales vanilla-family;
- servidor preparado aislado: **2/2 required GameTests passed**;
- A9 mantiene el mismo resultado causal bajo las traslaciones explícitas, sin exhaustion y sin contacto final inventado.

## Revisión post-verde vigente

La pasada final converge sin nuevos cambios de producción:

- `QUERY_BUDGET` permanece en 256;
- `SEPARATION_BUDGET` permanece en 128;
- exhaustion live sigue devolviendo desplazamiento/contactos nulos;
- screening y corrección usan la banda numérica de contacto correcta;
- el scheduler cambia sólo el orden determinista de evaluación, nunca omite piezas ni altera la simultaneidad dentro de `TIME_EPS`;
- la validación de `q` no cobra dos veces el mismo convexo;
- ordinary y prepared, incluido el holdout de traslación explícita, están verdes sobre la cadena final.

**S09 continúa COMPLETADO.**
