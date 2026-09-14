# Auditoría retroactiva S00–S18 — observabilidad adversarial

Estado: **AUDITORÍA CERRADA / S17 REVALIDADO / S18 REABIERTO**.

Esta auditoría reevalúa los sprints anteriores a S19 después de autorizar explícitamente al rol **ADVERSARIO** a crear tooling de test complejo, programas paralelos, mutation/fault harnesses y snapshots visuales deterministas. No redefine requisitos ni arquitectura. Su objetivo es distinguir entre «una herramienta nueva habría ayudado a entender mejor» y «la evidencia de cierre podía dejar pasar un defecto contractual real».

## Clasificación

- **N0 — no aporta evidencia material nueva.** No se recomienda acción retroactiva.
- **N1 — útil para diagnóstico/ergonomía, cierre actual suficiente.** Puede usarse si el sprint vuelve a tocarse, pero no se reabre.
- **N2 — backfill de evidencia recomendado.** El cierre actual sigue siendo suficiente, pero snapshots/tooling mejorarían de forma material la observabilidad de fenómenos espaciales o temporales.
- **N3 — laguna material demostrada.** Una mutación contractual o comparación independiente demuestra que la aceptación anterior podía quedar verde con comportamiento incorrecto. Se reabre evidencia y, si aparece divergencia productiva real, también producción/plan.

## Resultado S00–S18

| Sprint | Nivel | Conclusión retroactiva |
| --- | --- | --- |
| S00 Foundation Audit | N1 | Ya utilizó metodología equivalente a tooling avanzado: modelo clean-room, verifier estático, fault/mutation campaign y lanes client/dedicated. Snapshots no mejoran el núcleo causal/geométrico auditado lo suficiente para reabrir. |
| S01 Public API/backend | N0 | Contrato de capas/API. La evidencia relevante es estructural/compilación/consumer, no visual. |
| S02 Engine SPI/registries | N0 | Ownership, duplicate IDs, SPI y classloading. Snapshots no observan la propiedad contractual. |
| S03 Canonical data policy | N1 | Inspectores/codec fuzzers podrían ayudar, pero la propiedad es de datos canónicos y fail-closed; no se encontró laguna material. |
| S04 G1 integration contract | N1 | Harness consumer/network ya cubre la frontera real. Snapshots serían ornamentales frente a las propiedades verificadas. |
| S05 bounded material broadphase | N0 | Localidad, budgets y fail-closed son propiedades cuantitativas. Counters/fixtures son un oracle mejor que una captura. |
| S06 material interval identity | N0 | Exactly-once, identidad causal, revision/epoch/generation y staging; no visual. |
| S07 live material dispatcher/CCD | N1 | Un visualizador de sweeps/contactos sería útil para diagnóstico, pero los holdouts existentes ya atacan contacto temporal, provenance, batch conflicts, separation y budgets. |
| S08 continuous anchor transport/chains | **N2** | Muy buen candidato a backfill visual: BodyPath curvo vs chord, obstáculo sólo en mitad de arco, soporte anatómico estacionario fuera del batch y cadena A→B→C. Los tests actuales ya detectaron/reabrieron estos casos, por lo que no se reabre cierre. |
| S09 multicontact/sliding/recovery | **N2** | Recomendada herramienta de timeline/manifold + snapshots: floor+wall/triple contact, wall squeeze, initial separation, contacto estrictamente intermedio, lateral gravity y large-world translation. Los oracles actuales ya son contractuales y detectaron varias reaperturas reales. |
| S10 passive transport ledger | N0 | Ownership/generation/cursor. Snapshot no añade poder de detección. |
| S11 shared gravity authority | N1 | Un visualizador de frames podría ayudar a depurar orientación, pero la reapertura DOWN-vs-cleanup y los seis frames se prueban semánticamente. |
| S12 contact state ownership | N0 | Lifecycle/sequence/cross-dimension state; no visual. |
| S13 spatial index ownership | N0 | Stale/current, locality, rebuild y budgets; no visual. |
| S14 binding state ownership | N0 | Atomic rebind, generation, quarantine y reentrancy; no visual. |
| S15 prepared bundle reuse | N0 | Reuse/bytes/identity/transacción. Un screenshot no puede demostrar que un bundle se serializa una sola vez. |
| S16 canonical catalog authority | N0 | Canonical selectors, wire rejection y rollback atómico. Los oracles de datos son el nivel correcto. |
| S17 ModelPart GeometryEngine | **N3** | La aceptación anterior permitía omitir un cubo volumétrico original. Se reabrió evidencia, se añadió oracle original-tree bidireccional independiente y snapshots SVG, y la mutación quedó muerta. Producción no necesitó cambio. |
| S18 vanilla pose/Mojang keyframes | **N3** | La aceptación anterior sólo ejercitaba `head`: una mutación que eliminaba todos los demás bones quedó verde. El nuevo oracle multi-bone encontró además una divergencia productiva real en composición de rotación sobre bones con rotación de reposo no identidad. S18/G3.4 quedan reabiertos. |

## S08 — backfill visual recomendado, no bloqueo

Si S08 vuelve a tocarse, el adversario debería reutilizar o crear un visualizador temporal que emita una secuencia determinista t=0, .25, .5, .75, 1 con:

- trayectoria `BodyPath` real y chord de endpoints superpuestos;
- envelope/sweep del body y soporte;
- obstáculos candidatos y primer tiempo de contacto;
- anchor local y posición mundial resultante;
- cadena A→B→C coloreada por dependencia/orden causal;
- caso de obstáculo que existe sólo en el interior del arco;
- caso de soporte anatómico estacionario fuera del batch cuya AABB vanilla no es autoridad.

Un contact sheet o SVG/PNG de esas muestras ayuda a detectar un chord accidental, inversión de frame o uso de AABB equivocada. No sustituye los asserts actuales de bloqueo/clear/propagación ni justifica reabrir S08 por sí solo.

## S09 — backfill visual recomendado, no bloqueo

Si S09 vuelve a tocarse, conviene un harness de contacto que pueda exportar por tiempo:

- piezas activas, normales y manifold simultáneo;
- floor+wall y triple contact;
- initial overlap y vector de recovery;
- wall squeeze y residual permitido;
- intervalo donde el contacto sólo existe entre endpoints;
- gravity frame no-DOWN;
- `SKIN`, ULP/tolerancia y brackets/probes consumidos;
- mismo escenario trasladado a coordenadas mundiales grandes.

La visualización ideal es una secuencia 2D/3D con overlays de cuerpos, normales, candidatos y corrección. S09 ya posee holdouts semánticos suficientes y varias reaperturas históricas que prueban estas propiedades, por lo que esta mejora queda N2.

## S17 — reapertura de evidencia y revalidación

### Mutación que sobrevivía

Se modificó **sólo dentro del runner** `ModelPartGeometryExtractor` para omitir silenciosamente `root/body/cube_0`, un cubo volumétrico real del Cow. La aceptación S17 anterior siguió verde:

- run `34835030941`, job `103946799939`;
- el proof pasó de 10 a **9 pieces** y de 240 a **216 vertices** sin fallar;
- sentinel: `S17_RETRO_MUTATION_SURVIVED missing root/body/cube_0 was not detected by existing S17 acceptance`;
- artifact `10343891913`, SHA-256 `4847b9bddb5d446c2d5aab523b2e980891410a96e94c854ba2c2a8eef661e7cc`.

La causa era doble:

1. `S17ModelPartGeometryEngineClientTests` comparaba engine contra `GeometryExtractor.vanilla(...)`, compartiendo algoritmo;
2. el oracle original `AnatomyExportProof.verifyVanilla(...)` hacía `if (box == null) return`, de modo que no exigía contraparte para cada cubo original.

### Oracle reforzado

`S17OriginalModelStructuralParityClientTests` recorre directamente el árbol `ModelPart` original y no usa `GeometryExtractor` en el lado de referencia. Para Cow, Chicken y Player wide:

- exige igualdad exacta del conjunto de piezas volumétricas;
- compara cardinalidad de vertices y pertenencia bidireccional;
- omite sólo planos renderer-only legítimos;
- produce snapshots SVG deterministas XY/XZ de original vs candidate.

Producción normal con el oracle reforzado quedó verde en run `34835445968`: common, client-preparation y original-model/export terminaron `success`. El artifact cliente `10343789445` contiene los SVG.

La misma mutación quedó después **muerta**:

- run `34835751555`, job `103949057971`;
- fallo esperado: `missing=[root/body/cube_0]`;
- sentinel: `S17_RETRO_MUTATION_KILLED independent original-tree oracle detected missing volumetric source cube`;
- artifact `10344780088`, SHA-256 `7fed43a270d7d72f003dc57578903a3f03549428c63efe714161eba4a5070701`.

Se inspeccionó además `cow.svg`: en las proyecciones XY/XZ los puntos de referencia y cruces preparadas se superponen sin desplazamiento sistemático ni silueta fantasma visible. S17 queda **revalidado sin cambio de producción**.

## S18 — reapertura de evidencia y bug productivo real

### Mutación que sobrevivía

Se modificó **sólo dentro del runner** `AnimationDefinitionCompiler` para ignorar cualquier bone distinto de `head`. Toda la aceptación cliente S18 anterior siguió verde:

- run `34836415599`, job `103951151864`;
- los sentinels originales `S18_ANIMATION_COMPILER_BOUNDARY PASS` y `S18_ANIMATION_DEFINITION PASS` aparecieron normalmente;
- sentinel adicional: `S18_RETRO_MUTATION_SURVIVED compiler ignored all non-head tracks without failing current S18 original-source acceptance`;
- artifact `10344550870`, SHA-256 `d12e7181b7ef36482f04386cabfbb75f2396e7b30f5266e845398521555e3dc8`.

La causa es que el oracle original contra Mojang era profundo en tiempo/interpolación/discontinuidades, pero construía todos sus channels bajo `head`. Por tanto demostraba muy bien un bone favorable, no genericidad multi-bone.

### Holdout multi-bone y divergencia real

Se amplió el oracle original-source con una segunda animación sobre `body`, manteniendo `head` y añadiendo translation + Catmull-Rom rotation sobre el body real de Cow. Se comparan matrices completas contra `AnimationDefinition` de Minecraft 26.2 en varios tiempos y se genera `s18-retro-snapshots/multi_bone.svg`.

Run `34836738334`:

- common S18 siguió verde;
- client falló en `body` a `t=0.35`, amplitud `.7`:
  `matrix[1] expected=-0.027032088 actual=0.111668594`.

La divergencia es productiva, no un cambio de contrato. `PoseProgramEvaluator` reconstruye el quaternion de reposo y compone `restRotation * deltaRotation`. Minecraft 26.2 hace otra semántica para `AnimationDefinition`:

- `ModelPart.offsetRotation(Vector3f)` suma por componente: `xRot += x`, `yRot += y`, `zRot += z`;
- `ModelPart.translateAndRotate(...)` crea después un único `Quaternionf.rotationZYX(zRot, yRot, xRot)` con los Euler ya sumados.

Esto se verificó contra bytecode exacto de Minecraft 26.2 en run `34837085233`, job `103953249382`; artifact `10344916064`, SHA-256 `1938d29de3b6b4dfd92983dc47364b772a96e7ffc640807442d21eba1423be74`.

Para un bone con rotación de reposo no identidad, `Q(rest) * Q(delta)` no equivale en general a `Q(restEuler + deltaEuler)`. `head` ocultaba el defecto por ser un caso favorable; `body` lo revela.

**Clasificación:** bug de implementación S18. El adversario no lo corrige. El holdout rojo se conserva para el implementador. S18 y G3 tarea 4 deben considerarse reabiertos hasta que producción se repare y ordinary + common + original-source multi-bone + mutation kill + snapshot reproducible vuelvan a quedar verdes.

## Conclusión operativa

La nueva capacidad adversarial no obliga a rehacer todos los sprints. Su valor real quedó demostrado precisamente porque la auditoría fue selectiva:

- S00–S07 y S10–S16 no requieren reapertura;
- S08/S09 obtienen recomendaciones de backfill visual N2;
- S17 tenía una laguna de oracle, ya reparada sin cambio productivo;
- S18 ocultaba una laguna de genericidad y, al reforzarla, apareció un bug productivo real.

Hasta que S18 vuelva a verde, el trabajo productivo de G3 debe tratar **G3.4 como primer trabajo abierto anterior a G3.5/S19**. El adversario puede seguir preparando evidencia/test tooling de S19, pero no debe cerrar la implementación de G3.5 saltándose una regresión productiva abierta en G3.4.
