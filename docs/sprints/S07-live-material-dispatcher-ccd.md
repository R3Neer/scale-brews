# S07 — Dispatcher material real y CCD de root/joints

Estado: **IMPLEMENTACIÓN AVANZADA / REVALIDACIÓN ADVERSARIAL EN CURSO**. Tercer sprint de G2. Los defectos históricos de contacto, provenance y starting-overlap locality están reparados. El último blocker demostrado fue `BACKEND_EXHAUSTED` por agotamiento numérico sin overlap inicial, que dejaba una relación material vieja todavía autoritativa. El commit de producción `dceddae0e359aa743e8e59d9c706fcc77e661c6c` contiene una reparación candidata; el sprint no se cierra hasta revalidarla y atacar sus bordes.

S06 ya no es un prerequisite abierto: el holdout de mutación oculta del provider fue retirado como no contractual. El problema live de coste espacial está clasificado en S05/NFR-008.

## 1. Tesis y scope

Cada intervalo material ROOT/JOINT publicado por S06 debe:

- drenarse una sola vez y en el punto causal correcto;
- resolverse continuamente contra candidatos locales usando la trayectoria certificada del handle;
- conservar provenance suficiente para distinguir contacto real de mera proximidad;
- fallar cerrado y **localmente** cuando geometría, budgets o derivación no pueden certificarse;
- no convertir una incertidumbre de una pareja en discontinuidad del soporte entero.

Requisitos primarios: FR-049, FR-050 y FR-051. Requisitos de apoyo: FR-043, FR-048, FR-052, FR-053; NFR-001, NFR-002, NFR-004, NFR-007, NFR-008, NFR-015, NFR-017 y NFR-033.

S07 no cierra todavía FR-056..060, prediction/reconciliation/receipts ni G7/Clinging.

## 2. Estado actual

- `MaterialIntervalRuntime` conserva provenance ROOT/JOINT y staging acotado (`MAX_PENDING_INTERVALS=512`).
- `MaterialEventDispatcher` tiene queue/candidate/depth limits, reentrancy gate, joint batches y ancestry/cycle fences.
- `MaterialPhysicsRuntime` opera sobre bodies `Entity`.
- ROOT se drena síncronamente tras `commitRoot`; JOINT tras `AnatomyRuntime.publish(level)`.
- Joint batches lejanos se particionan en clusters espaciales deterministas.
- El backend resuelve CCD continuo con `TemporalResponse`, `ConservativeSweep` y `AnatomySeparation` bajo budgets explícitos.
- `Plan` transporta `contactPieces` scoped por `support UUID + materialSerial + piece`.
- `establish(...)` revalida binding activo, pieza, gap, eligibility y gravity frame antes de confirmar `SurfaceContact`.
- `BACKEND_EXHAUSTED` no invalida automáticamente todo el support.
- `dceddae0…` sustituye la lógica limitada a overlap inicial por `suspendUncertainPairs(...)`: una relación material ya retenida al support se vuelve localmente incierta si el solver no puede certificar el intervalo, aunque no exista overlap en `t=0`; los starting overlaps siguen usando la prueba geométrica local.

## 3. Historia adversarial cerrada

### A1 — tipo del body

El runtime inicial usó `MaterialEventDispatcher<LivingEntity>` aunque la world query devolvía `Entity`. Se generalizó a `Entity`. **Cerrado.**

### A2 — localidad de joint batch

Una versión unía todos los envelopes JOINT del tick. Los holdouts de soportes lejanos forzaron clusters espaciales deterministas. **Cerrado.**

### A3 — staging ilimitado

La cola previa al dispatcher pasó a bound 512 con saturación sticky/observable. **Cerrado.**

### A4 — simultaneidad física

La resolución joint secuencial fue sustituida por planificación conjunta del cluster tras holdouts de permutación. **Cerrado por la suite actual, sujeto a revisión final.**

### A5 — stale binding

La lane preparada demostró que UUID/entity-id no bastaban; la certificación pasó a identidad activa completa. **Cerrado.**

### A6 — hook real

Los holdouts forzaron root drain síncrono y joint drain post-publish. **Cerrado.**

### A7 — initial separation + tangent perdía contacto

`AnatomyMovement.collide` podía dejar el AABB en la posición correcta sin `SurfaceContact`. La reparación revalida la cara final y los controles lateral/gravity evitan crear suelo ficticio. **Cerrado.**

### A8 — contacto real `t=1` con desplazamiento neto cero

El runtime confundía “body no se movió” con “no hubo contacto”. `47b134b472e348757f19b9d25b7afc813bf76b1b` hizo que `Plan` conserve evidencia de piezas realmente tocadas.

`S07ContactProvenanceTests` atacó esa reparación con un support realmente tocado y otro sólo próximo, colocado primero en el batch. Tras corregir un desajuste de ID del fixture, run `34623887683`, job `103344184321`, quedó verde. **Cerrado.**

### A9 — starting overlap irresoluble invalidaba terceros

`S07FailureLocalizationTests` usa un convex 10×10×10 con `MAX_SEPARATION=4`, exige suspensión de la pareja atrapada, conservación del bystander y reacquisition same-binding. El holdout queda verde tras `47b134b…`. **Cerrado.**

## 4. Último blocker demostrado — budget exhaustion sin overlap inicial

`S07BudgetFailureLocalizationTests.solverBudgetExhaustionMustReleaseOnlyUncertainPair` prepara:

- body `uncertain` con contacto válido previo a gap exacto 0,01, sin initial overlap;
- body `safe` con contacto independiente en otra pieza del mismo support;
- trayectoria material estática pero con bound de point-speed `10`, válido aunque holgado;
- envelope <64 bloques;
- `QUERY_BUDGET=256` agotado antes de certificar el intervalo.

Evidencia roja: run `34624485241`, job `103346141995`:

- `QUARANTINED / BACKEND_EXHAUSTED` pasa;
- el body no recibe movimiento parcial no certificado;
- falla exactamente porque la relación vieja `uncertain ↔ support` sigue reportándose soportada;
- el bystander permanece como control de localidad.

La causa era que la implementación sólo suspendía pairs que solapaban en `t=0`.

### Reparación candidata `dceddae0…`

`dceddae0e359aa743e8e59d9c706fcc77e661c6c` introduce `suspendUncertainPairs(...)`:

- si el body ya retiene contacto con un support cuyo evento no pudo resolverse, la relación se suspende localmente aunque no exista initial overlap;
- si no hay contacto previo, conserva la detección de overlap inicial;
- un error al inspeccionar la motion vuelve la relación conservadoramente incierta;
- `fail(BACKEND_EXHAUSTED)` sigue sin invalidar el support entero.

Esta forma encaja con NFR-004 mucho mejor que volver a `invalidateSupport`, pero **la aceptación depende de CI y de holdouts adicionales**.

## 5. Siguiente superficie adversarial tras poner verde el fixture de budget

Aunque `suspendUncertainPairs` cierre el caso `plan()==null`, hay otras rutas que producen `BACKEND_EXHAUSTED` y deben conservar la misma localidad:

1. `worsensBodyOverlap(plans,candidates)` puede rechazar un conjunto de planes ya calculados;
2. candidate/batch exhaustion puede producir uncertainty sin `INITIAL_OVERLAP`;
3. un batch con múltiples supports debe suspender sólo las relaciones realmente inciertas, no todas las elegibles cercanas;
4. la suspensión debe auto-liberarse cuando la geometría actual vuelve a ser demostrablemente válida, sin requerir rebind global.

El siguiente holdout adversarial debe priorizar `worsensBodyOverlap`: dos bodies con contactos previos independientes cuyo batch genera planes mutuamente incompatibles. Si el outcome es `BACKEND_EXHAUSTED`, ambos pairs inciertos deben quedar fail-closed, pero terceros no participantes deben sobrevivir.

## 6. Plan actualizado

- [x] I1 Wiring root/joint real y one-shot drain.
- [x] I2 Provenance ROOT/JOINT + material serial.
- [x] I3 Handle → certified motion/envelope fail-closed.
- [x] I4 Candidate capture local, `Entity`, determinista.
- [x] I5 CCD continuo y budgets explícitos.
- [x] I6 Mutación autorizada sin reingest.
- [x] I7 Initial separation + tangent conserva contacto final.
- [x] I8 Contact provenance real, incluido `t=1`/zero displacement y distractor support.
- [x] I9 ROOT_MUTATION + JOINT_BATCH, sin `DERIVED_CARRY` productivo.
- [x] I10 Métricas/outcomes mínimos.
- [x] I11 Starting-overlap failure localizado y reacquisition same-binding.
- [ ] I12 Revalidar `suspendUncertainPairs` contra budget exhaustion sin initial overlap.
- [ ] I13 Atacar otras rutas de `BACKEND_EXHAUSTED`, especialmente `worsensBodyOverlap`.
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
29. pair recovery same-binding;
30. conflictivo `worsensBodyOverlap` con relaciones previas.

## 8. Lane preparada

La lane cliente+servidor preparada está verde en evidencias recientes, incluido run `34624096154`, job `103344876628`, y el pair-suspension determinista en run `34623128548`, job `103341673751`.

No hay blocker preparado activo en este momento. La suite ordinaria sigue siendo la capa que debe validar el hotfix `dceddae0…`.

## 9. Relación con S05

S05 está reabierto por una violación separada de NFR-008: la validación del índice live podía re-muestrear providers lejanos en una segunda query local. `S05LiveBroadphaseLocalityTests` es la evidencia correcta de ese problema.

No se reutiliza el holdout S06 retirado de mutación clandestina del provider: resolver NFR-008 no requiere que el broadphase detecte cambios internos que ningún provider productivo puede realizar fuera de sus hooks causales.

## 10. Reconciliación con `main`

Antes del cierre global de G2 debe reconciliarse `chatgpt-editing` con `main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` y dejar `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como única autoridad transversal de body gravity.

`collision.api.GravityFrame` conserva su contrato; `RootTransformProvider` sigue separado de body gravity; Scale debe cargar sin Gravity Changer y no se crea API Tiny-Mount-específica.

**S07 está en revalidación del hotfix `dceddae0…`; no se declara cerrado hasta CI verde y el ataque a rutas alternativas de `BACKEND_EXHAUSTED`.**
