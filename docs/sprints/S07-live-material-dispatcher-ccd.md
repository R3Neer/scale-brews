# S07 — Dispatcher material real y CCD de root/joints

Estado: **IMPLEMENTACIÓN AVANZADA / REABIERTO POR VALIDEZ DE CONTACTO TRAS BATCH RECHAZADO**. Tercer sprint de G2.

Los defectos históricos de contacto, provenance, starting-overlap locality, budget exhaustion y plan-conflict locality están reparados. El blocker adversarial vigente es posterior: un batch conjunto puede rechazarse correctamente por un conflicto causado por un support A y, aun así, conservar como autoritativo un contacto previo con support B aunque el propio intervalo material de B certifique que su cara final se ha alejado del body.

S06 ya no es un prerequisite abierto. El problema live de coste espacial quedó tratado en S05; la evidencia ordinaria y preparada reciente de S05/S06 permanece verde. Esta reapertura es exclusivamente S07/NFR-004.

## 1. Tesis y scope

Cada intervalo material ROOT/JOINT publicado por S06 debe:

- drenarse una sola vez y en el punto causal correcto;
- resolverse continuamente contra candidatos locales usando la trayectoria certificada del handle;
- conservar provenance suficiente para distinguir contacto real de mera proximidad;
- fallar cerrado y **localmente** cuando geometría, budgets o derivación no pueden certificarse;
- no convertir una incertidumbre de una pareja en discontinuidad del soporte entero;
- tras un batch rechazado, no conservar como autoritativo ningún contacto que el `after` certificado del propio evento haya dejado materialmente inválido.

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
- `suspendUncertainPairs(...)` localiza solver/budget uncertainty por body/support.
- `29d4595f38f7ff3c44f9af7ebb98512cdd16f93d` localiza conflictos body-body mediante `BodyConflict` y, para un contacto retenido, replantea el conflicto sin los eventos de ese support para decidir si **ese support causó el conflicto**.
- Esa causal attribution es correcta para preservar un support B estable cuando support A causa el rechazo, pero no basta para comprobar si la cara retenida de B sigue siendo válida en el `after` de su propio evento.

## 3. Historia adversarial cerrada

### A1 — tipo del body

El runtime inicial usó `MaterialEventDispatcher<LivingEntity>` aunque la world query devolvía `Entity`. Se generalizó a `Entity`. **Cerrado.**

### A2 — localidad de joint batch

Una versión unía todos los envelopes JOINT del tick. Los holdouts de soportes lejanos forzaron clusters espaciales deterministas. **Cerrado.**

### A3 — staging ilimitado

La cola previa al dispatcher pasó a bound 512 con saturación sticky/observable. **Cerrado.**

### A4 — simultaneidad física

La resolución joint secuencial fue sustituida por planificación conjunta del cluster y holdouts de permutación. **Cerrado.**

### A5 — stale binding

La lane preparada demostró que UUID/entity-id no bastaban; la certificación pasó a identidad activa completa. **Cerrado.**

### A6 — hook real

Los holdouts forzaron root drain síncrono y joint drain post-publish. **Cerrado.**

### A7 — initial separation + tangent perdía contacto

`AnatomyMovement.collide` podía dejar el AABB en la posición correcta sin `SurfaceContact`. La reparación revalida la cara final y controles lateral/gravity evitan crear suelo ficticio. **Cerrado.**

### A8 — contacto real `t=1` con desplazamiento neto cero

`47b134b472e348757f19b9d25b7afc813bf76b1b` hizo que `Plan` conserve evidencia de piezas realmente tocadas. `S07ContactProvenanceTests` añadió un support realmente tocado y otro sólo próximo para impedir inference por cercanía. **Cerrado.**

### A9 — starting overlap irresoluble invalidaba terceros

`S07FailureLocalizationTests` usa un convex mayor que `MAX_SEPARATION`, exige suspensión de la pareja atrapada, conservación del bystander y reacquisition same-binding. **Cerrado.**

### A10 — budget exhaustion sin overlap inicial

`S07BudgetFailureLocalizationTests` fuerza `QUERY_BUDGET=256` manteniendo un contacto previo separado de la superficie por un gap válido. `dceddae0e359aa743e8e59d9c706fcc77e661c6c` introdujo `suspendUncertainPairs(...)` y el holdout queda verde. **Cerrado.**

### A11 — planes individualmente válidos pero mutuamente incompatibles

`S07PlanConflictLocalizationTests` construye dos planes individuales `COMPLETE` que, combinados, harían solapar los bodies. Las dos permutaciones de candidatos deben rechazar el batch, no aplicar desplazamiento parcial y liberar sólo las relaciones implicadas. Un tercer body `safe` es candidato real y debe sobrevivir.

El rojo válido inicial fue run `34627841257`, job `103357151536`. `29d4595f…` sustituyó el booleano global por conflictos body-body explícitos y localización por support causante.

`S07MultiSupportConflictLocalizationTests` añadió dos supports en el mismo batch: A causa el conflicto y B mantiene un contacto estable. Tras `29d4595f…`, B sobrevive. **Cerrado.**

Evidencia posterior:

- run `34633241588`, job `103374905847`: **319/319 GameTests verdes**;
- run `34633398671`, job `103375427902`: **320/320 GameTests verdes** tras el holdout cross-tick S05;
- prepared proof run `34633591227`, job `103376062867`: cliente exportado + runtime preparado verdes.

Estas evidencias cierran el plan-conflict causal anterior, pero no prueban la nueva superficie A12.

## 4. Blocker vigente — contacto retenido materialmente inválido tras batch rechazado

`S07RejectedBatchContactValidityTests.rejectedConflictMustStillReleaseRetainedFaceThatMovedAway` separa dos preguntas que el runtime actual trata como una sola:

1. **qué support causó el conflicto que obliga a rechazar el batch**;
2. **qué contactos retenidos siguen siendo materialmente válidos en el `after` certificado de sus propios eventos**.

El escenario contiene:

- support A con dos piezas que mueven dos bodies hacia un overlap final incompatible;
- body derecho con relación retenida a A;
- body izquierdo con relación retenida a support B;
- B participa genuinamente en el mismo batch y su floor se traslada `0.4` hacia abajo;
- antes del evento, el floor B queda a gap `0.01`: dentro de la tolerancia de retención `0.025`, pero muy fuera del skin CCD `1e-6`;
- la traslación de B se certifica como rígida (`deformationSpeed=0`, `linearTranslation=(0,-0.4,0)`), evitando hits artificiales en `t=0`;
- los planes individuales permanecen `COMPLETE` y A sigue siendo la causa del body-body conflict;
- el batch se rechaza como `QUARANTINED / BACKEND_EXHAUSTED` y no aplica desplazamiento parcial;
- el `after` de B deja su floor a más de `0.2` del body.

### Evidencia roja válida

GitHub Actions run **`34634310744`**, job **`103378374367`**, sobre `a48f4e6e960e5b1adf4bd937e3275e838cdb7ad3`:

- **321 GameTests ejecutados**;
- **320 pasaron / 1 falló**;
- todas las precondiciones del fixture pasaron;
- el único fallo fue exactamente:
  `NFR-004 forbids preserving a retained B face that the same rejected batch certifies has moved away`;
- no hubo otro rojo de S05, S06, S07 histórico ni regresión general.

El run anterior del mismo test no cuenta como evidencia de producción porque el fixture exact-tangent entraba en `ITERATION_LIMIT`; `a48f4e6e…` lo saneó antes de obtener el rojo anterior.

### Causa estructural

La reparación `29d4595f…` hace correctamente causal attribution del conflicto:

- si eliminar los eventos del support retenido elimina el body-body conflict, esa relación se suspende;
- si el conflicto persiste sin ese support, la relación se conserva.

Eso responde quién **causó** el rechazo, pero no valida la relación conservada contra el `after` del evento del support retenido. En el caso A/B:

- A sigue causando el conflicto aunque B se elimine del replanning;
- por eso B no se suspende;
- como el batch completo fue rechazado, `apply(...)` no ejecuta la revalidación/establishment normal;
- el contacto previo B queda almacenado aunque el intervalo B certifique que la cara final se alejó.

### Criterio de reparación aceptable

Una reparación válida debe mantener simultáneamente:

- **causal locality:** el test estable-B anterior sigue preservando B cuando A causa el conflicto;
- **final material validity:** si el `after` certificado de B ya no valida pieza/cara/gap/gravity/identity del contacto retenido, B se libera aunque no haya causado el conflicto;
- ningún desplazamiento parcial de un batch rechazado;
- ningún `invalidateSupport(...)` global;
- determinismo respecto al orden de candidates/events;
- reacquisition posterior sólo por una relación material nuevamente válida.

No es aceptable simplemente suspender todos los supports del batch: eso volvería rojo `S07MultiSupportConflictLocalizationTests`.

## 5. Superficies adversariales posteriores

Tras reparar A12, sólo se abrirán nuevos blockers con reproducción contractual. Permanecen candidatas a revisión:

1. candidate/batch exhaustion antes de `plan(...)`;
2. reacquisition tras liberación por final-invalid contact;
3. permutación multi-support con la misma causal/final-validity clasificación;
4. lifecycle que invalida un evento durante capture/resolve.

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
- [x] I12 Budget exhaustion sin initial overlap libera/suspende la relación incierta y conserva terceros.
- [x] I13 Plan-conflict body-local determinista.
- [x] I14 Multi-support causal locality: support estable no causante sobrevive.
- [ ] I15 Revalidar contactos retenidos contra el `after` de su propio evento también cuando el batch es rechazado.
- [ ] I16 Revalidar suite ordinaria + prepared lane y realizar revisión estructural cero-cambios antes de cerrar de nuevo.

## 7. Modelo adversarial vigente

Además de la batería histórica:

- exact endpoint / zero displacement;
- near endpoint / no hit;
- contact provenance con distractor;
- same-tick roots + joint cadence;
- candidate/envelope caps;
- solver budget exhaustion;
- starting overlap separable/irresoluble;
- third-party locality;
- stale active binding;
- pair recovery same-binding;
- plan-conflict two orders + safe candidate;
- multi-support stable-B locality;
- **rejected batch + non-causal support B cuyo contact final deja de ser válido — rojo vigente.**

## 8. Lane preparada

La lane preparada más reciente sobre el candidato previo al nuevo holdout está verde:

- run **`34633591227`**, job **`103376062867`**;
- export cliente de geometría original completado;
- runtime preparado servidor completado.

No hay blocker preparado activo. El blocker vigente está demostrado por la suite ordinaria de 321 tests y debe repararse antes de repetir la lane preparada para cierre.

## 9. Relación con S05 y S06

S05/S06 no se reabren por este fallo. Sus holdouts recientes permanecen verdes, incluido locality steady/post-mutation/cross-tick y causal membership soportada. El rojo actual pertenece exclusivamente a S07/NFR-004.

## 10. Reconciliación con `main`

Antes del cierre global de G2 debe reconciliarse `chatgpt-editing` con `main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` y dejar `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como única autoridad transversal de body gravity.

`collision.api.GravityFrame` conserva su contrato; `RootTransformProvider` sigue separado de body gravity; Scale debe cargar sin Gravity Changer y no se crea API Tiny-Mount-específica.

**S07 queda REABIERTO por A12. No se declara cerrado hasta reparar final-contact validity en batches rechazados, mantener verdes los holdouts de causal locality y repetir suite ordinaria + prepared lane sobre el mismo candidato.**
