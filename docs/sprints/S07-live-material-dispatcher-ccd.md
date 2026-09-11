# S07 — Dispatcher material real y CCD de root/joints

Estado: **IMPLEMENTACIÓN AVANZADA / REABIERTO POR BUDGET UNCERTAINTY LOCAL**. Tercer sprint de G2. El runtime material, hooks root/joint, localidad de batches, bounds y solver continuo existen. Los defectos previos de contacto `t=1` y de starting-overlap pair-local han sido reparados y sus holdouts vuelven a verde. El blocker S07 vigente es distinto: un `BACKEND_EXHAUSTED` por agotamiento numérico **sin overlap inicial** deja un contacto viejo todavía autoritativo.

S07 tampoco puede cerrarse mientras siga abierto el prerequisite S06 de membresía espacial same-tick.

## 1. Tesis y scope

Cada intervalo material ROOT/JOINT publicado por S06 debe:

- drenarse una sola vez y en el punto causal correcto;
- resolverse continuamente contra candidatos locales mediante la trayectoria certificada del handle;
- conservar provenance suficiente para distinguir contacto real de mera proximidad;
- fallar cerrado y **localmente** cuando geometría, budgets o derivación no pueden certificarse;
- no convertir una incertidumbre de una pareja en discontinuidad del soporte entero.

Requisitos primarios: FR-049, FR-050 y FR-051. Requisitos de apoyo: FR-043, FR-048, FR-052, FR-053; NFR-001, NFR-002, NFR-004, NFR-007, NFR-008, NFR-015, NFR-017 y NFR-033.

S07 no cierra todavía FR-056..060, prediction/reconciliation/receipts ni G7/Clinging.

## 2. Estado actual

- `MaterialIntervalRuntime` conserva provenance ROOT/JOINT y staging acotado (`MAX_PENDING_INTERVALS=512`).
- `MaterialEventDispatcher` tiene queue/candidate/depth limits, reentrancy gate, joint batches y ancestry/cycle fences.
- `MaterialPhysicsRuntime` opera sobre bodies `Entity`, no sólo `LivingEntity`.
- ROOT se drena síncronamente tras `commitRoot`; JOINT tras `AnatomyRuntime.publish(level)`.
- Joint batches lejanos se particionan en clusters espaciales deterministas.
- El backend resuelve CCD continuo con `TemporalResponse`, `ConservativeSweep` y `AnatomySeparation` bajo budgets explícitos.
- `Plan` transporta `contactPieces` scoped por `support UUID + materialSerial + piece`, permitiendo revalidar qué pieza produjo realmente el contacto.
- `establish(...)` revalida binding activo, pieza, gap, eligibility y gravity frame antes de confirmar `SurfaceContact`.
- `BACKEND_EXHAUSTED` ya no invalida automáticamente todo el support.
- Starting overlap irresoluble puede suspender localmente las parejas cuyo material solapa en `t=0`.

## 3. Historia adversarial cerrada

### A1 — tipo del body

El primer runtime parametrizaba el dispatcher como `LivingEntity` aunque la world query devolvía `Entity`. Se generalizó correctamente a `Entity`. **Cerrado.**

### A2 — localidad de joint batch

Una versión unía todos los envelopes JOINT del tick y podía fallar por soportes lejanos. Se introdujeron clusters deterministas. **Cerrado.**

### A3 — staging ilimitado

La cola S06 pasó a bound explícito 512 con saturación sticky/observable. **Cerrado.**

### A4 — simultaneidad física

La primera resolución joint mutaba secuencialmente con bounds capturados inicialmente. Holdouts de permutación forzaron planificación conjunta. **Cerrado por la suite actual, sujeto a revisión final.**

### A5 — stale binding

La lane preparada demostró que UUID/entity-id no bastaban. La certificación pasó a identidad activa completa. **Cerrado.**

### A6 — hook real

El runtime material nació sin caller productivo. Holdouts forzaron root drain síncrono y joint drain post-publish. **Cerrado.**

### A7 — initial separation + tangent perdía contacto

`AnatomyMovement.collide` podía separar correctamente un overlap inicial, terminar sobre una cara válida y perder el `SurfaceContact` si el tramo restante era clear. Se reparó con revalidación final y controles lateral/gravity/anti-magnetización. **Cerrado.**

### A8 — contacto real `t=1` con desplazamiento neto cero

Un parche anti-magnetización infería “contacto” a partir de `Plan.displacement != 0`. El holdout `exactEndpointCcdContactMustAnchorEvenWithZeroBodyDisplacement` demostró que un soporte puede llegar exactamente al body en `t=1` sin desplazarlo.

`47b134b472e348757f19b9d25b7afc813bf76b1b` cambió `Plan` para conservar evidencia causal de las piezas realmente tocadas. La suite posterior deja verde el caso `t=1`.

Además, `S07ContactProvenanceTests` atacó la reparación con dos supports simultáneos: uno toca realmente y otro termina a 0,001 bloques sin tocar, colocado primero en el batch. Tras corregir un desajuste de fixture en el ID de pieza, el commit `69f26d5b182bff388081cae54aac212efb28e278` quedó verde en GitHub Actions run **`34623887683`**, job **`103344184321`**. La provenance scoped conserva el support/piece correcto. **Cerrado.**

### A9 — starting overlap irresoluble invalidaba terceros

`S07FailureLocalizationTests` usa una pieza 10×10×10 y `MAX_SEPARATION=4`, por lo que el fallo es geométricamente determinista. Exige:

- `BACKEND_EXHAUSTED`;
- suspensión de la pareja atrapada;
- conservación del contacto de un bystander sobre otra pieza del mismo support;
- auto-liberación de la suspensión cuando el overlap desaparece dentro del mismo binding, sin rebind.

`47b134b…` dejó de invalidar el support entero para `BACKEND_EXHAUSTED` y suspendió los starting-overlap pairs. En run **`34624485241`**, job **`103346141995`**, este holdout pasa completo; los únicos rojos son el prerequisite S06 y el blocker de budget descrito abajo. **Starting-overlap locality cerrado.**

## 4. Blocker vigente — budget exhaustion sin overlap inicial

### Escenario

`S07BudgetFailureLocalizationTests.solverBudgetExhaustionMustReleaseOnlyUncertainPair` construye:

- un support con dos piezas independientes;
- body `uncertain` a gap exacto **0,01**, dentro de la tolerancia de retención `.025`, con contacto material válido previo;
- body `safe` con otro contacto válido en otra pieza del mismo support;
- el intervalo material relevante es geométricamente estático, pero declara un bound de point-speed `10`, que es **válido aunque holgado**;
- el envelope resultante permanece por debajo del cap live de 64 bloques;
- no existe starting overlap.

La holgura del bound obliga a `ConservativeSweep`/`TemporalResponse` a agotar `QUERY_BUDGET=256` antes de certificar el intervalo.

`ceeacbd2a8bd3495723deb366ef497252de51c1e` alineó el gap a `0.01` usando el SAT real para eliminar ruido float/JOML.

### Evidencia

GitHub Actions run **`34624485241`**, job **`103346141995`**, sobre `82a519d5…`:

- 306 GameTests ejecutados;
- 304 pasan;
- los únicos fallos son este blocker S07 y el blocker S06 de membresía espacial;
- el test supera las precondiciones de gap, contactos iniciales, envelope y eligibility;
- supera la aserción de outcome `QUARANTINED / BACKEND_EXHAUSTED`;
- supera la aserción de que el body **no se mueve** con un prefijo no certificado;
- falla exactamente en:
  `NFR-004 requires budget uncertainty to release or locally quarantine the unresolved body/support relation; an old contact cannot remain authoritative`.

### Causa estructural

Cuando `plan(...)` devuelve `null` por `TemporalResponse.Status.ITERATION_LIMIT`:

1. `resolveAll` / `resolveJointBatch` llaman a `suspendInitialOverlapPairs(...)`;
2. ese helper sólo suspende pairs cuyo material **solapa en `t=0`**;
3. en este escenario no hay overlap inicial, sólo un contacto previo a gap `.01`;
4. `fail(..., BACKEND_EXHAUSTED)` ya no invalida el support entero, correctamente;
5. pero tampoco libera/suspende la relación incierta;
6. el contacto viejo permanece autoritativo aunque el intervalo actual no pudo certificarse.

Esto viola NFR-004 y deja la física en un estado “optimista por incertidumbre”, justo lo que el contrato prohíbe.

### Criterio de reparación aceptable

La implementación necesita saber **qué body/support relations quedaron sin certificar**, no sólo cuáles empezaban solapadas.

Una reparación válida debe:

- conservar terceros sanos sobre el mismo support;
- liberar o suspender sólo las relaciones afectadas por la resolución incierta;
- no aplicar prefijo físico no certificado;
- permitir reacquisition cuando la geometría vuelve a ser demostrablemente válida;
- tratar también otros `BACKEND_EXHAUSTED` locales que puedan surgir sin `INITIAL_OVERLAP`, no sólo este fixture concreto;
- no volver a `invalidateSupport` global como atajo.

La aserción posterior del holdout exige además que el bystander permanezca soportado una vez se repare la primera condición.

## 5. Prerrequisito S06 todavía abierto

`S06SpatialMembershipCausalityTests` endurecido en `82a519d5…` demuestra que el hotfix `debc74bf…` es parcial: invalidar `SPATIAL` cuando un endpoint **ya fue aceptado** no basta si `spaceClear` es el primer consumidor después de `UNAVAILABLE → AVAILABLE` y el support estaba ausente de `current.frames()`.

Ese fallo se documenta en `S06-material-interval-identity.md` y bloquea el cierre de S07.

## 6. Plan actualizado

- [x] I1 Wiring root/joint real y one-shot drain.
- [x] I2 Provenance ROOT/JOINT + material serial.
- [x] I3 Handle → certified motion/envelope fail-closed.
- [x] I4 Candidate capture local, `Entity`, determinista.
- [x] I5 CCD continuo y budgets explícitos.
- [x] I6 Mutación autorizada sin reingest.
- [x] I7 Initial separation + tangent conserva contacto final.
- [x] I8 Contact provenance real, incluido `t=1` con displacement cero y batch con distractor próximo.
- [x] I9 ROOT_MUTATION + JOINT_BATCH, sin `DERIVED_CARRY` productivo.
- [x] I10 Métricas/outcomes mínimos.
- [x] I11 Starting-overlap failure localizado por pareja y reacquisition same-binding.
- [ ] I12 Generalizar locality/fail-closed para `BACKEND_EXHAUSTED` sin overlap inicial y otros exhaustions numéricos.
- [ ] I13 Revalidar S06 same-tick membership.
- [ ] I14 Suite ordinaria + prepared lane + revisión estructural cero-cambios antes de cerrar.

## 7. Modelo adversarial vigente

1. mid-interval-only contact;
2. exact endpoint / zero displacement;
3. near endpoint / no hit;
4. contact provenance con support distractor;
5. same-tick two roots;
6. root + joint cadence;
7. joint permutation;
8. late body sin replay;
9. candidate cap exact/+1;
10. envelope cap exact/+1;
11. solver budget exhaustion;
12. starting overlap separable;
13. starting overlap irresoluble pair-local;
14. budget exhaustion sin starting overlap con contacto previo;
15. block obstruction;
16. reentrancy;
17. lifecycle durante capture;
18. missing certification;
19. replay fence integration;
20. hash/order metamorphic;
21. far-world amplification;
22. NaN/extreme trajectory;
23. derived-carry trap;
24. third-party regression;
25. pre-dispatch saturation;
26. far-apart simultaneous joints;
27. pre-existing body overlap locality;
28. stale active binding;
29. same-tick availability membership;
30. pair recovery same-binding;
31. **planned next attack:** `worsensBodyOverlap`/otro `BACKEND_EXHAUSTED` posterior a planes válidos debe liberar sólo relaciones inciertas, no dejar contactos viejos.

## 8. Lane preparada

La lane cliente+servidor preparada está verde tras los fixes causales recientes:

- run **`34624096154`**;
- job **`103344876628`**;
- export cliente: success;
- prepared server proof: success.

Una lane preparada anterior también quedó verde con el fixture determinista de pair suspension (`34623128548`, job `103341673751`). El antiguo rojo de una jaula de bloques se considera fixture inválido y no evidencia de producción.

## 9. Reconciliación con `main`

Antes del cierre global de G2 debe reconciliarse `chatgpt-editing` con `main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284`.

`io.github.r3neer.scalebrews.integration.gravity.GravityFrames` debe quedar como única autoridad transversal de Scale. `collision.internal.GravityFrames` no puede seguir siendo una segunda autoridad. Debe preservarse la separación entre `RootTransformProvider` y body gravity, el fallback vanilla DOWN, la carga sin Gravity Changer y la ausencia de API Tiny-Mount-específica. G7/Clinging no se adelanta.

**S07 permanece REABIERTO por budget uncertainty local y por el prerequisite S06 de membresía espacial.**
