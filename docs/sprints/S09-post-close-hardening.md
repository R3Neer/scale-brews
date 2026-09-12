# S09 — Hardening posterior al cierre

Estado: **COMPLETADO**. Este addendum amplía la evidencia histórica de `S09-multicontact-sliding-recovery.md` después de revisiones adversariales posteriores durante S12.

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

## Evidencia final

Snapshot final de esta reapertura: `e1fc4893f069eb2b60fc1fdd352107dc0205bddb`.

- Suite ordinaria: run `34693722959`, **374/374 GameTests verdes**. Incluye el holdout de clearance ULP, simultaneidad dentro de `TIME_EPS`, budgets y regresiones S05–S12.
- Lane preparada: run `34693722977`, job `103553460165`, **verde**.
  - exportación cliente original correcta (`minecraft:cow` 240 vertices / 10 pieces, más player wide/slim);
  - servidor preparado aislado: **2/2 GameTests verdes**;
  - A9 completa de nuevo bajo el budget compartido de 256 con geometría real exportada.

## Revisión post-verde

La pasada final no exige cambios de producción adicionales:

- `QUERY_BUDGET` permanece en 256;
- `SEPARATION_BUDGET` permanece en 128;
- exhaustion live sigue devolviendo desplazamiento/contactos nulos;
- el screen usa la misma banda numérica de contacto que CCD;
- el scheduler sólo cambia orden de evaluación, nunca omite piezas ni altera el criterio temporal de simultaneidad;
- ordinary y prepared son verdes sobre el mismo snapshot final.

**S09 continúa COMPLETADO.**
