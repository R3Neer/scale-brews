# S07 — Dispatcher material real y CCD de root/joints

Estado: **IMPLEMENTACIÓN AVANZADA / REABIERTO POR DOS DEFECTOS FÍSICOS ADVERSARIALES**. Tercer sprint de G2. El runtime material, hooks root/joint, localidad de batches, bounds y solver continuo existen y una parte sustancial de los holdouts está verde. El cierre sigue bloqueado por dos fallos S07 reproducidos en la suite ordinaria y por el blocker causal S06 de membresía espacial documentado aparte.

## 1. Tesis y scope

Al terminar S07, **cada intervalo material root/joint que S06 publique debe drenarse una única vez por `MaterialEventDispatcher` y resolverse continuamente contra cuerpos candidatos locales usando la trayectoria certificada del handle, sin inventar endpoints, sin depender del orden de mapas y conservando tanto la identidad del contacto real como la localidad del fallo**.

Requisitos primarios: FR-049, FR-050 y FR-051. Requisitos de apoyo: FR-043, FR-048, FR-052, FR-053; NFR-001, NFR-002, NFR-004, NFR-007, NFR-008, NFR-015, NFR-017, NFR-033.

S07 no cierra todavía FR-056..060 de carry/anchors persistentes/chains, prediction-reconciliation/receipts ni G7/Clinging.

## 2. Estado actual investigado

- `MaterialIntervalTracker` + `MaterialIntervalRuntime` poseen serial/replay/provenance ROOT/JOINT; los defectos anteriores de support identity, conflicting frame serial, stale binding y staging ilimitado están reparados.
- `MaterialIntervalRuntime.MAX_PENDING_INTERVALS=512` acota staging y expone saturación explícita/fail-closed.
- `MaterialEventDispatcher` conserva queue caps, reentrancy gate, candidate limits, joint batches, ancestry/cycle/depth limits y separación capture/resolve.
- `MaterialPhysicsRuntime` está parametrizado sobre `Entity`; un soporte viviente no obliga a que todos los bodies sean `LivingEntity`.
- Root mutations se drenan síncronamente tras `commitRoot`; joint intervals se drenan post-`AnatomyRuntime.publish(level)`.
- Joint batches lejanos se particionan en clusters espaciales deterministas en vez de construir una unión global >64 bloques.
- La resolución joint calcula planes para el batch antes de aplicar y tiene holdouts de permutación/localidad.
- El backend revalida contacto final mediante identidad, pieza, gap, eligibility y gravity frame.
- La pérdida histórica de contacto tras **initial separation + tangent clear** fue reparada: los holdouts ordinarios de suelo, pared lateral y gravedad lateral pasan en snapshots posteriores.
- La lane preparada actual alcanza escenarios de barco ocupado simple y de cabeza exportada del player; no existe evidencia vigente para mantener “boat/head” como blocker separado.

### Prerrequisito S06 externo

`S06SpatialMembershipCausalityTests` demuestra que un soporte puede pasar `UNAVAILABLE → AVAILABLE` dentro del mismo authority tick y seguir omitido del broadphase por reutilización de un índice incompleto. Ese defecto está documentado en `S06-material-interval-identity.md` y **bloquea el cierre de S07**, aunque no sea una reparación de `MaterialPhysicsRuntime`.

## 3. Blockers S07 vigentes

### B1 — contacto CCD real en t=1 con desplazamiento neto cero

Commit `619bf038f9f8326f2948c8e42260a9f5ba48b55f` evitó una magnetización espuria haciendo que `MaterialPhysicsRuntime.apply` sólo intentase crear un contacto nuevo cuando `Plan.displacement != 0`. Esa condición confunde dos estados físicamente distintos:

1. el material nunca alcanzó al body y el desplazamiento fue cero;
2. el material alcanzó exactamente al body en `t=1`, por lo que existe un contacto CCD real pero no hace falta desplazar el body.

`f775980c6d3415ac8a932379ee2e579ccf53da34` añadió el holdout y `9588981b80db9bb35baaf14511d06962ab256ce6` corrigió su alineación geométrica para que la pieza llegue exactamente al endpoint según el SAT real, eliminando el falso rojo de fixture.

Evidencia actual: GitHub Actions run **`34622750287`**, job **`103340432608`** sobre la línea que incluye `2d4a73ae…`:

- el evento termina con outcome aplicado;
- la precondición geométrica exacta pasa;
- el body tiene desplazamiento neto cero como corresponde;
- falla exactamente `A real CCD contact at t=1 must establish the anchor even when the body's net displacement is zero`.

Causa arquitectónica visible: `Plan` conserva `body`, `displacement` y `evaluations`, pero no conserva la provenance de `TemporalResponse.contacts()`. `apply` intenta reconstruir “hubo contacto” a partir de si el body se movió, una variable que no codifica esa información.

**Criterio de reparación**: el plan físico debe transportar suficiente provenance para distinguir contacto CCD real de mera proximidad final. No basta con un booleano global `hadContact` si un joint batch puede contener varios soportes/piezas: la identidad del soporte/pieza relevante debe poder revalidarse sin anclar al vecino equivocado.

### B2 — FR-052: fallo de una pareja invalida el soporte entero

`MaterialPhysicsRuntime.fail(...)` y `quarantine(...)` llaman actualmente a `AnatomyMovement.invalidateSupport(support)`. Si un body queda en un solape material irresoluble, esa operación borra también contactos válidos de otros bodies sobre el mismo soporte.

`S07FailureLocalizationTests` construye:

- un body atrapado dentro de una pieza convexa 10×10×10; `MAX_SEPARATION=4`, por lo que ninguna salida está dentro del bound y `BACKEND_EXHAUSTED` es geométricamente determinista;
- un segundo body con contacto válido sobre otra pieza del mismo soporte.

La primera versión del holdout usaba una jaula de bloques y fue descartada como fixture insuficiente. La versión actual elimina por completo esa dependencia.

Evidencia actual en run **`34622750287`**, job **`103340432608`**:

- la precondición `QUARANTINED / BACKEND_EXHAUSTED` pasa;
- el test falla en `FR-052 requires the unresolvable body/support pair itself to be suspended instead of leaving an immediately retryable bad pair`;
- el mismo holdout conserva después la aserción de que el bystander sano mantenga su contacto, para impedir una reparación que sólo añada suspensión pero siga invalidando todo el soporte.

**Criterio de reparación**: un fallo derivado de resolver una pareja candidata debe suspender/liberar **esa pareja**, no convertir una incapacidad local del body A en discontinuidad causal del soporte para B, C y D. Una discontinuidad real del soporte sigue pudiendo invalidar el soporte entero; un `BACKEND_EXHAUSTED` pair-local no.

## 4. Estado objetivo

- Root intervals se resuelven en su punto causal y nunca esperan a un tick posterior.
- Joint intervals de la misma cadence se tratan como simultáneos y se particionan por localidad sin causalidad ficticia por UUID/distancia.
- Cada handle se convierte en `MotionSnapshot`/`MaterialInterval` sólo si el provider certifica exactamente esa identidad causal.
- Envelope, captura, solver y separación tienen bounds explícitos y outcomes distinguibles.
- Contactos mid-interval y contactos exactos en endpoint se representan aunque la traslación neta del body sea cero.
- Proximidad final sin CCD hit no puede magnetizar un contacto.
- Starting overlap separable/irresoluble no puede saltarse bloques ni exceder la distancia de recuperación.
- Un fallo local suspende sólo la relación problemática; terceros válidos permanecen anclados.
- Replay, gap, lifecycle y saturación no ejecutan trabajo físico dos veces ni fabrican historia.

## 5. Invariantes

- S06 es owner de identidad/replay. S07 no inventa seriales paralelos.
- `MaterialEventDispatcher.EventId` es scheduling identity, no sustituto de `materialSerial`.
- El backend captura antes de mutar y no deja una mutación parcial sin `Outcome` representable.
- Un body que entra después de drenar un intervalo no recibe replay retrospectivo.
- Contacto real y desplazamiento corporal son variables independientes.
- Contact provenance debe sobrevivir hasta la fase que establece/revalida `SurfaceContact`.
- La selección final de contacto revalida geometría, support identity, eligibility y gravity; nunca “primer elemento de un map/list”.
- Un budget exhaust no significa mundo vacío.
- Un fallo pair-local no equivale a lifecycle/support discontinuity.
- La resolución no depende de hash order ni accidental object equality.
- S07 no emite `DerivedCarry` de producción todavía.

## 6. Plan de implementación actualizado

- [x] I1 Conectar `MaterialPhysicsRuntime` a hooks reales y drenar S06 exactamente una vez en root/joint.
- [x] I2 Transportar provenance ROOT/JOINT preservando `materialSerial` y handle original.
- [x] I3 Certificar `MotionIntervalHandle -> MotionSnapshot -> MaterialInterval` fail-closed y construir envelope acotado.
- [x] I4 Backend real sobre bodies `Entity`, captura local y orden determinista.
- [x] I5 CCD continuo y budgets explícitos, incluido contacto mid-interval.
- [x] I6 Aplicar desplazamientos sin reingest/recursión del mismo evento.
- [x] I7 Reparar initial separation + tangent clear en `AnatomyMovement.collide` y mantener controles de pared/gravedad/anti-magnetización.
- [ ] I8 Conservar provenance suficiente para establecer contacto real `t=1` aunque el desplazamiento neto del body sea cero, sin reintroducir magnetización.
- [x] I9 Conectar ROOT_MUTATION y JOINT_BATCH sin `DERIVED_CARRY` de producción.
- [x] I10 Métricas/outcomes de eventos, candidatos, evaluaciones y exhaustion/quarantine.
- [ ] I11 Localizar `BACKEND_EXHAUSTED`/failure al body-support problemático y conservar contactos sanos del mismo soporte.
- [x] I12 Holdouts de hooks/backend reales, locality, simultaneidad, stale work, boundaries y initial-contact.
- [ ] I13 Revalidar el prerequisite S06 de membresía espacial same-tick.
- [ ] I14 Suite ordinaria + lane preparada + revisión estructural cero-cambios antes de cerrar.

## 7. Modelo adversarial vigente

1. **mid-interval-only contact**: endpoints libres, cruce intermedio obligatorio.
2. **exact endpoint / zero displacement**: `t=1` real contact sin mover body debe anclar.
3. **near endpoint / no hit**: proximidad dentro de tolerance sin CCD hit no debe anclar.
4. **same-tick two roots**: dos contributions se consumen una vez y en orden.
5. **root + joint cadence**: no colapsar contribuciones legítimas.
6. **joint permutation**: resultado físico invariante bajo permutación equivalente.
7. **late body**: no replay retrospectivo.
8. **candidate cap exact/+1**: no partial candidate set.
9. **envelope cap exact/+1**: fallo antes de mutar.
10. **solver budget**: exhaustion no aplica movimiento no certificado.
11. **starting overlap separable**: recovery acotado.
12. **starting overlap irresoluble**: suspender sólo la pareja; no soporte entero.
13. **initial separation + tangent**: contacto final válido aunque el tramo posterior sea clear.
14. **block obstruction**: nunca teleport a través de bloque.
15. **reentrancy**: backend movement no reingiere evento.
16. **lifecycle during capture**: work stale descartado.
17. **certification missing**: no fallback a `motion()`/endpoint chord.
18. **replay fence integration**: handle no resuelve dos veces.
19. **hash/order metamorphic**: orden incidental no cambia resultado.
20. **far-world amplification**: entidades lejanas no aumentan trabajo local.
21. **NaN/extreme trajectory**: rechazo previo a mutación.
22. **derived carry trap**: no adelantar FR-057..060.
23. **third-party regression**: no elegibles conservan vanilla.
24. **pre-dispatch saturation**: bound/señal explícita.
25. **far-apart simultaneous joints**: simultaneidad temporal no crea unión espacial gigante.
26. **pre-existing body overlap locality**: terceros no cancelan evento sano.
27. **stale active binding**: misma entidad física, binding distinto, handle rechazado.
28. **same-tick availability membership**: collider disponible debe entrar al broadphase aunque antes del mismo tick estuviera unavailable.
29. **pair recovery**: tras desaparecer el overlap, la suspensión local debe liberarse/reacquirir sin rebind global del mundo.
30. **contact provenance ambiguity**: en un batch con un support realmente tocado y otro sólo próximo, el ancla debe pertenecer al hit real.

## 8. Historia adversarial relevante cerrada

### A1 — tipo del body

El primer runtime usó `MaterialEventDispatcher<LivingEntity>` aunque la world query devolvía `Entity`. La reparación correcta generalizó a `Entity`. **Cerrado.**

### A2 — localidad de joint batch

Una versión unía todos los envelopes JOINT del tick y podía fallar por separación entre soportes lejanos. Se introdujeron clusters deterministas. **Cerrado por holdouts actuales.**

### A3 — staging ilimitado

`MaterialIntervalRuntime` pasó a `MAX_PENDING_INTERVALS=512` con saturación sticky/observable y corte conservador. **Cerrado.**

### A4 — simultaneidad física

La primera resolución joint mutaba secuencialmente con bounds capturados antes. Holdouts de permutación condujeron a planificación conjunta del cluster. **Cerrado por tests actuales, pendiente sólo de revisión final.**

### A5 — stale binding

La lane preparada demostró que UUID/entity-id no bastaban para certificar un handle de otra generación. La validación pasó a identidad activa completa. **Cerrado en reruns posteriores.**

### A6 — hook real

El runtime material empezó sin caller productivo. Los holdouts forzaron root drain síncrono y joint drain post-publish. **Cerrado.**

### A7 — initial separation pierde contacto

La suite ordinaria y una lane preparada anterior reprodujeron un body geométricamente colocado sobre la cara final pero sin `SurfaceContact`. La reparación posterior revalida contacto final tras una corrección inicial real y los controles lateral/gravity impiden convertir cualquier separación en suelo. **Cerrado en el estado actual; no es el blocker vigente.**

## 9. Lane preparada

La lane `s06-prepared-adversarial-proof` sigue siendo obligatoria porque ejercita catálogo exportado por cliente, `AnatomyPreparedSession`, mixins y `Entity.move` reales.

Un run anterior llegó a fallar en el escenario de barco sobre la cabeza exportada. En snapshots posteriores, la lane alcanzó y superó tanto el barco sobre convex simple como el escenario exportado, por lo que **no se mantiene un blocker de barco separado sin nueva reproducción**.

El run `34622223722`, job `103338727694`, alcanzó al final `trappedPairSuspendsAndReacquiresIndependentlyPrepared`, pero ese helper utilizaba una jaula de bloques que ya se había demostrado no determinista como certificado de irresolubilidad. Esa caída no se clasifica como bug de producción.

Para eliminar ese falso rojo se añadieron:

- `S07PreparedPairSuspensionProof`, con convex 10×10×10 que exige >4 bloques para escapar;
- `AnatomyPreparedIntegrationProof` actualizado para usar ese helper en lugar de la jaula histórica;
- workflow marker `553d0c17…` para rerun aislado.

El resultado de ese rerun debe registrarse antes del cierre.

## 10. Evidencia ordinaria vigente

GitHub Actions run **`34622750287`**, job **`103340432608`** ejecutó **304 GameTests** y falló exactamente tres holdouts:

1. `S06SpatialMembershipCausalityTests.sameTickUnavailableToAvailableSupportMustEnterBroadphaseWithoutRebind` — prerequisite S06;
2. `S07ContactEstablishmentTests.exactEndpointCcdContactMustAnchorEvenWithZeroBodyDisplacement` — blocker S07 B1;
3. `S07FailureLocalizationTests.irresolvableMaterialOverlapMustNotInvalidateOtherBodyOnSameSupport` — blocker S07 B2.

No hay fallo de compilación ni ruido no relacionado en esa evidencia.

## 11. Reconciliación con main antes del cierre global

`main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` introdujo `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como autoridad transversal de Scale. Antes de cerrar G2 debe reconciliarse `chatgpt-editing` y eliminar/delegar la autoridad duplicada `collision.internal.GravityFrames`, conservando fallback vanilla DOWN y la separación `RootTransformProvider` vs body gravity. Scale debe seguir cargando sin Gravity Changer; no se crea API Tiny-Mount-específica ni se adelanta G7.

**S07 permanece abierto por B1 y B2, y no puede cerrarse mientras S06 mantenga el falso negativo de membresía espacial same-tick.**
