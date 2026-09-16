# S08 — Clasificación implementadora del fixture cosmético del jugador

Rol activo: **IMPLEMENTER**.

## Motivo

La lane preparada S08 volvió a exponer un rojo en `AnatomyGeometryTests.exportedPlayerHeadCarriesBoatPrepared`: una barca ocupada debía aterrizar sobre `root/head/cube_0`, pero el contacto observado no coincidía con esa pieza.

Este documento clasifica ese rojo sin modificar el oráculo adversarial ni introducir una excepción por especie en el runtime.

## Contrato relevante

- FR-020 exige filtros declarativos para equipo, cosméticos y segunda capa de skin.
- S17 fija que las exclusiones físicas específicas de una especie pertenecen al `AnatomyFilter` del binding; el `ModelPartGeometryEngine` no debe hardcodear nombres de partes por especie.
- `AnatomyFilter.DEFAULT` no excluye ninguna de las partes cosméticas del jugador.
- El fixture preparado construía tanto el perfil del jugador como el `ModelGeometryProvider` manual con `AnatomyFilter.DEFAULT`, aunque después esperaba que la segunda capa no participase en colisión.

## Evidencia A — configuración DEFAULT

Commit: `d615a5f80ec1f3b7627d736ae6c4e0e221d8bb1e`.

Workflow: `boat-contact-diagnostic`, run `35081094150`, job `104745016597` — **success**.

El diagnóstico ejecutó la misma geometría exportada y la misma ruta `Entity.move`. El snapshot contenía, entre otras, `root/head/cube_0` y `root/head/hat/cube_0`. La barca no quedó detenida por el AABB vanilla: el contacto anatómico fue `root/head/hat/cube_0`, cuya cara superior está por encima de la cabeza base. Por tanto el runtime estaba obedeciendo la geometría física configurada; la segunda capa seguía siendo collider porque el fixture no la excluía.

Build del mismo snapshot: run `35081094083`, job `104745016366` — **success**.

## Evidencia B — mismo runtime con filtro FR-020 explícito

Commit: `22a8658ce0fabd4ddb9a7be8033671ee7ef6b7fa`.

Se mantuvieron los umbrales de `AnatomyFilter.DEFAULT` y se excluyeron declarativamente las seis partes exteriores del jugador:

- `root/head/hat`
- `root/body/jacket`
- `root/left_arm/left_sleeve`
- `root/right_arm/right_sleeve`
- `root/left_leg/left_pants`
- `root/right_leg/right_pants`

Workflow: `boat-contact-diagnostic`, run `35085817748`, job `104760253595` — **success**. El servidor pasó **2/2 required GameTests**.

Marcadores observados:

- piezas físicas resultantes: `root/right_arm/cube_0`, `root/left_leg/cube_0`, `root/left_arm/cube_0`, `root/head/cube_0`, `root/right_leg/cube_0`, `root/body/cube_0`;
- contacto final: `piece=root/head/cube_0`;
- `boatY=-31.37218713760376` y `headMaxY=-31.37218713760376`.

Build del mismo snapshot: run `35085817757`, job `104760253915` — **success**.

## Clasificación

**TEST / EVIDENCIA → ADVERSARY.**

No hay evidencia de un bug de producción en supresión de AABB, selección anatómica o respuesta de la barca. El rojo surge porque el fixture instala explícitamente `AnatomyFilter.DEFAULT` y simultáneamente exige una exclusión cosmética que ese filtro no declara.

La corrección adecuada conserva el oráculo físico (`root/head/cube_0`) y corrige el setup del holdout para suministrar el filtro declarativo que exige FR-020. El IMPLEMENTER no modifica el holdout adversarial para hacerlo pasar y no introduce hardcodes de `hat`/`jacket`/mangas/pantalones dentro del engine genérico.

## Cleanup

El GameTest y workflow `S08ImplementerBoatContactDiagnostic` eran tooling temporal de clasificación. Tras registrar las dos ejecuciones anteriores se eliminan del árbol; la evidencia reproducible queda en los runs citados y en este documento.
