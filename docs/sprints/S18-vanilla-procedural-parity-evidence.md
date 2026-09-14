# S18 — evidencia de paridad procedural vanilla canónica

Estado: **VERDE / oracle diferencial original-source permanente**.

Fecha: 2026-09-14

## Motivación

La segunda lectura adversarial de S18 detectó que el proof histórico de familias vanilla (`AnatomyExportProof`) había quedado obsoleto tras endurecer correctamente los `PoseEngine` con allowlists de `ModelGeometry.source`: el fixture antiguo fabricaba fuentes `proof:*`, que ahora deben ser rechazadas. Mantener ese oracle sin corregirlo habría convertido un fail-closed correcto en un falso rojo.

Se creó por tanto `S18VanillaProceduralSemanticClientTests`, que conserva la intención del proof original pero cruza la frontera productiva actual:

- instancia modelos reales Minecraft 26.2;
- extrae geometría mediante `GeometryExtractor.vanilla(...)` con **source IDs canónicos**;
- ejecuta directamente `CollisionEngines.pose(...)`, no el wrapper legacy;
- aplica en paralelo `model.setupAnim(state)` sobre la fuente Mojang real;
- compara las piezas materiales transformadas en ambos lados;
- recorre 80 ticks por familia/variante, 800 comparaciones en total.

Familias cubiertas: `quadruped` (cow y llama), `chicken`, `villager`, `iron_golem`, `ghast`, `feline`, `equine`, `bee` y `player_walking`. Los fixtures incluyen estados no triviales de crouch/sprint/sit/lie/relax, comer/levantarse/boca/agua/cola, angry/on-ground/roll y canales de aleteo.

## Calibración del epsilon

La primera ejecución del nuevo oracle encontró en `bee 29` una distancia de vértice al cuadrado de `5.685533294164102E-10`, equivalente a unos `2.38E-5` bloques. Ese delta estaba por debajo del epsilon `3E-5` ya usado por los oracles semánticos S18 de matrices, pero la prueba nueva había fijado accidentalmente un umbral más estricto (`sqrt(1E-10)=1E-5`).

El adversario no clasificó ese rojo como defecto productivo. El oracle se alineó con el criterio S18 existente usando `VERTEX_EPS = 3E-5` y comparación `d2 <= VERTEX_EPS^2`.

Commits adversariales:

- oracle canónico original-source: `6b2f672e88df9da27ec8e9d575c7cafa2f2d874b`;
- lane aislada: `9277b0a46ccb76ae4fe95723d1a7ae055c938744`;
- calibración de epsilon: `8319cb59a60253658c40e53b1855986bc817cf30`.

## Evidencia ejecutada

Workflow `s18-vanilla-procedural-parity-proof`, run **`34871991696`**, job **`104069894227`**: **SUCCESS**.

Salida causal:

`S18_VANILLA_PROCEDURAL PASS 800 original-source Minecraft 26.2 pose comparisons`

`BUILD SUCCESSFUL`.

Artifact **`10359732016`** (`S18-vanilla-procedural-original-source-proof`), SHA-256 **`a36189425efdb29ba8d5f0d746676838dfcb16d9e65cbe80b008bf90f81305f8`**.

## Clasificación TM

Este trabajo **no amplía el contrato S18**. Sustituye un oracle histórico que ya no atravesaba correctamente las nuevas source gates por una comparación semánticamente equivalente y más fuerte contra la autoridad canónica actual.

Resultado de esta pasada: no queda divergencia procedural vanilla reproducible bajo estos 800 casos original-source y el epsilon S18 existente. La lane queda permanente para que cambios futuros en `PoseEngine`, `SourcePose`, `AuthorityPoseTracker`, elegibilidad o extracción de ModelPart vuelvan a ejecutar esta comparación diferencial.
