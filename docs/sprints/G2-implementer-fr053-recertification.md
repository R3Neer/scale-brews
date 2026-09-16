# G2 / FR-053 — recertificación del IMPLEMENTER

Rol activo: **IMPLEMENTER**.

Estado: **LISTO PARA REVISIÓN ADVERSARIAL DE RECierre**. Este documento no cierra G2, no marca la tarea 10 del plan y no sustituye la revisión independiente requerida por `ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`.

## 1. Rojo que abrió la deuda

La reapertura adversarial de FR-053 aisló una única conducta productiva: una pareja moving-platform anatómica y elegible conservaba correctamente un único owner geométrico, pero perdía el impulso vanilla de `Entity.push` después de adquirir contacto material. La evidencia causal vigente del rojo está en `G2-vanilla-push-reopen-evidence.md`, incluida la reproducción bidireccional sobre `42ff3180cb42ad72310b98ebcc1b0487d1e197c5`.

## 2. Reparación productiva localizada

Commit productivo: `166f7cad069c787615b3fdd3f757b50ff74f7767` — `fix(g2): preserve vanilla pairwise push`.

La modificación fue deliberadamente mínima: `PlatformEntityMixin.push(Entity)` dejó de cancelar la llamada por `AnatomyApi.ready(self) && AnatomyMovement.suppressesPush(self, other)`. Se conservó la cancelación del soporte legacy basada en `PlatformState.support`, y no se modificó `PlatformPhysics.suppressPair(...)`, que sigue siendo el owner de la sustitución del blocker geométrico vanilla para una pareja anatómica elegible.

Por tanto la reparación separa las dos responsabilidades que FR-053 exige mantener distintas:

- anatomía sustituye el blocker geométrico de la pareja elegible;
- Minecraft conserva `Entity.push` vanilla exactamente una vez.

El HEAD posterior conserva esta semántica: el hook `push` ya no contiene ningún guard de cancelación anatómica.

## 3. Evidencia sobre la reparación

En el propio commit `166f7cad...` quedaron verdes:

- build: run `35004801864`, job `104501777125`;
- `vanilla-push-preservation`: run `35004801553`, job `104501776021`;
- `single-blocking-owner`: run `35004801545`, job `104501776059`;
- `pair-suppression-mutant-must-die`: run `35004801545`, job `104501776365`.

La lane preparada de ese snapshot falló por una deuda distinta de preparación/S08 y no constituye un rojo FR-053.

## 4. Recertificación conjunta posterior

Snapshot: `6aa8a4a56157fbb0fd10da0ff62715ce9f2daae2`.

Este descendiente ejecutó en un único SHA las cuatro fronteras focales de FR-053 más build, todas con resultado **success**:

| Frontera | Run | Job | Resultado |
| --- | --- | --- | --- |
| `vanilla-push-preservation` | `35079377805` | `104739406890` | success |
| `ineligible-no-wall` | `35079377736` | `104739406861` | success |
| `single-blocking-owner` | `35079377739` | `104739407106` | success |
| `pair-suppression-mutant-must-die` | `35079377739` | `104739407316` | success |
| `build` | `35079377669` | `104739406131` | success |

Esto cubre en el mismo candidato:

1. `support.push(body)` y `body.push(support)` preservan la respuesta vanilla tras adquirir la relación material;
2. una pareja fuera de elegibilidad no adquiere pared anatómica;
3. una pareja elegible conserva un único owner geométrico;
4. reactivar el blocker vanilla mediante la mutación de `suppressPair` sigue matando el GameTest;
5. el árbol compila y construye correctamente.

La `prepared-proof` de ese SHA (`35079377701`, job `104739406193`) fue roja por el caso del jugador exportado cuya segunda capa `hat` seguía siendo física bajo `AnatomyFilter.DEFAULT`. Esa incidencia fue aislada posteriormente como **TEST / EVIDENCIA**, no como regresión de FR-053: `docs/sprints/S08-implementer-player-cosmetic-fixture-classification.md` registra dos diagnósticos emparejados que demuestran que el mismo runtime contacta `root/head/hat/cube_0` con `DEFAULT` y `root/head/cube_0` cuando el fixture declara el filtro cosmético exigido por FR-020.

## 5. Revisión implementadora final

No se identifica una segunda corrección productiva necesaria para FR-053:

- el fix `166f7cad...` ya está integrado en la rama;
- los cambios posteriores no han reintroducido el guard anatómico en `PlatformEntityMixin.push`;
- los tres holdouts físicos focales y el mutante geométrico quedaron verdes juntos en `6aa8a4a...`;
- el rojo prepared coexistente pertenece a una frontera de evidencia S08 ya clasificada, no a push, elegibilidad ni ownership geométrico.

`AnatomyMovement.suppressesPush(...)` permanece por ahora como helper interno consumido por el holdout adversarial para identificar exactamente la relación material gestionada. No se retira antes del recierre para no alterar el oráculo durante su revisión independiente.

## 6. Handoff

**Clasificación IMPLEMENTER:** FR-053 está productivamente reparado y recertificado; G2 queda **listo para una revisión adversarial posterior sin cambios de producción**.

El ADVERSARY debe decidir el recierre formal, reconciliar `G2-vanilla-push-reopen-evidence.md` y `ENTITY_COLLISIONS_PLAN.md`, y sólo entonces marcar la tarea 10/G2 como cerrada. Si esa revisión abre un rojo causal nuevo, vuelve al IMPLEMENTER según la clasificación normal del workflow.
