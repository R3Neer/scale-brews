# S07 — Dispatcher material real y CCD de root/joints

Estado: **IMPLEMENTACIÓN AVANZADA / REABIERTO POR EVIDENCIA ADVERSARIAL DE CONTACTO**. Tercer sprint de G2. S06 ha recuperado una frontera causal verde; S07 ya tiene runtime vivo, hooks root/joint, localidad de batches, límites de staging y solver continuo. El cierre sigue bloqueado por un defecto físico reproducido tanto en la suite ordinaria como en la lane preparada real.

## 1. Tesis y scope

Al terminar S07, **cada intervalo material root/joint que S06 publique debe drenarse una única vez por `MaterialEventDispatcher` y resolverse continuamente contra cuerpos candidatos locales usando la trayectoria certificada del handle, sin inventar endpoints, sin depender del orden de mapas y conservando un contacto material válido cuando la resolución deja al cuerpo apoyado sobre una cara**.

Requisitos primarios: FR-049, FR-050 y FR-051. Requisitos de apoyo: FR-043, FR-048, FR-052, FR-053; NFR-001, NFR-002, NFR-004, NFR-007, NFR-008, NFR-015, NFR-017, NFR-033.

### Bloqueante actual

La frontera causal S06 ya no es el blocker vigente. Los holdouts posteriores cerraron reuse conflictivo de `frameSerial`, `before` forjado, support/dimension/entity-id incorrectos, binding stale y staging ilimitado. El blocker actual está en la **ruta de movimiento propio compartida**:

1. `AnatomyMovement.collide` puede resolver un solape inicial mediante `AnatomySeparation`, colocar correctamente el cuerpo sobre la cara final y conservar el movimiento tangencial;
2. si el `TemporalResponse` posterior queda `CLEAR`, no existe un hit nuevo en `response.contacts()`;
3. el código sólo conserva un contacto previo o crea uno a partir de esos hits;
4. por tanto el cuerpo queda geométricamente bien colocado pero sin `SurfaceContact`/`onGround` material.

Este defecto está probado por dos rutas independientes y no puede declararse cerrado mediante un test que invoque reflectivamente sólo `MaterialPhysicsRuntime.Backend.resolve`.

### Exclusiones explícitas

S07 no cierra todavía:

- FR-056..060 material anchors persistentes, obstruction carry y cadenas de soportes más allá del contacto mínimo necesario para representar correctamente el resultado S07;
- derived carry real entre soportes, aunque el dispatcher conserve su API/ancestry preparada;
- sneak/jump, vehículos/items/falling blocks como cobertura funcional completa;
- prediction/reconciliation/network receipts;
- la reconciliación con `main@39824dd…`, obligatoria antes del cierre global de G2 y antes de considerar definitiva la revalidación de G1/S04 respecto a la autoridad transversal de gravedad;
- G7/Clinging.

## 2. Estado actual investigado

- S06 posee `MaterialIntervalTracker` + `MaterialIntervalRuntime` y sus holdouts adversariales de identidad están reparados.
- `Pending` transporta `Source.ROOT` / `Source.JOINT` y valida la identidad física del support.
- `AnatomyRuntime.interval(entity, handle)` valida que el handle coincida con la identidad activa completa del binding, no sólo dimensión/UUID/entity id.
- El staging previo al dispatcher está acotado por `MaterialIntervalRuntime.MAX_PENDING_INTERVALS=512`; la saturación es sticky/observable, consume el serial sin inventar historia, corta continuidad e invalida sólo el soporte afectado.
- `MaterialEventDispatcher` mantiene queue caps, reentrancy gate, joint batches, candidate limits, ancestry/cycle/depth limits y separación entre captura y resolución.
- `MaterialPhysicsRuntime` está tipado sobre `Entity`, de modo que support viviente no obliga artificialmente a que todo body candidato sea `LivingEntity`.
- Root mutations se drenan síncronamente inmediatamente después de `commitRoot` en `PlatformEntityMixin.move`.
- Joint intervals se drenan después de `AnatomyRuntime.publish(level)` en `Platforms.tick`, conservando la cadencia simultánea.
- Los joint batches espacialmente lejanos se particionan/resuelven localmente en vez de convertir una unión >64 bloques en fallo colectivo.
- La resolución joint fue endurecida para calcular/aplicar clusters de forma conjunta y hay holdouts metamórficos de permutación/localidad.
- El runtime conserva contactos del solver y dispone de lógica de establecimiento de contacto tras CCD material; esa ruta sintética ya tiene cobertura verde.
- **La ruta distinta `AnatomyMovement.collide` sigue perdiendo el contacto cuando una separación inicial válida es seguida por un tramo tangencial sin hit.** Ese es el blocker actual.

## 3. Estado objetivo

- Root intervals se resuelven en su punto causal y nunca esperan a un tick posterior.
- Joint intervals de la misma cadence se tratan como simultáneos y se particionan por localidad sin introducir causalidad ficticia por UUID ni distancia.
- Cada handle se convierte en `MotionSnapshot`/`MaterialInterval` sólo si el provider certifica exactamente esa identidad causal.
- El envelope temporal es finito, conservador y acotado.
- La captura de bodies es local, determinista y presupuestada.
- El solver detecta contacto sólo intermedio, starting overlap y obstrucción sin caer a endpoint-only.
- Un body que termina soportado debe salir con un `SurfaceContact` final válido aunque el contacto relevante proceda de la recuperación inicial y no aparezca en el hit-list del tramo tangencial posterior.
- Replay, gap, lifecycle y saturación no pueden ejecutar trabajo físico dos veces ni fabricar historia.

## 4. Invariantes

- S06 sigue siendo owner de identidad/replay de intervalos. S07 no crea otro serial ni otra deduplicación.
- `MaterialEventDispatcher.EventId` es identidad de scheduling, no sustituto de `materialSerial`.
- El backend captura antes de mutar y no puede lanzar después de una mutación parcial no representada por `Outcome`.
- Un body que entra después de drenar un intervalo no recibe replay retrospectivo.
- Un contacto existente sólo a mitad de trayectoria debe detectarse aunque ambos endpoints estén libres.
- Una recuperación de solape que termina sobre una cara soportante no puede perder su identidad sólo porque el tramo restante sea tangencial/clear.
- La selección final de contacto debe revalidarse contra geometría final, eligibility y gravedad; no se puede anclar simplemente al primer soporte de una colección.
- La resolución de soportes/cuerpos simultáneos no depende de hash order.
- Un budget exhaust no significa mundo vacío y un soporte patológico no cuarentena soportes sanos independientes.
- S07 no emite `DerivedCarry` de producción todavía.

## 5. Plan de implementación

- [x] I1 Conectar `MaterialPhysicsRuntime` a hooks reales y drenar S06 exactamente una vez en root/joint.
- [x] I2 Transportar provenance ROOT/JOINT desde S06 preservando `materialSerial` y handle original.
- [x] I3 Certificar `MotionIntervalHandle -> MotionSnapshot -> MaterialInterval` fail-closed y construir envelope temporal acotado.
- [x] I4 Mantener backend real sobre bodies `Entity`, captura espacial local y orden determinista.
- [x] I5 Resolver eventos materiales con CCD continuo y budgets explícitos, incluido contacto mid-interval.
- [x] I6 Aplicar desplazamientos por ruta autorizada evitando reingest/recursión del mismo evento.
- [ ] I7 Cerrar establecimiento/retención de contacto en **todas** las rutas compartidas: además del backend S07, `AnatomyMovement.collide` debe convertir una separación inicial final-válida en contacto material aunque el tramo tangencial posterior no genere hit.
- [x] I8 Conectar ROOT_MUTATION y JOINT_BATCH sin emitir `DERIVED_CARRY` de producción.
- [x] I9 Mantener métricas/outcomes mínimos de eventos, candidatos, evaluaciones y exhaustion/quarantine.
- [x] I10 Registrar holdouts S07 sobre hooks/backend reales, localidad, simultaneidad, stale work, boundaries y contacto.
- [ ] I11 Repetir suite ordinaria + lane preparada, hacer revisión estructural cero-cambios y sólo entonces cerrar S07.

## 6. Modelo adversarial

1. **mid-interval-only contact**: una pieza cruza el body y termina libre; endpoint-only debe fallar, CCD debe detectar.
2. **same-tick two roots**: dos handles serial 1 y 2 se resuelven una vez cada uno y en orden.
3. **root + joint cadence**: contribuciones legítimas del mismo tick no colapsan ni se duplican.
4. **joint permutation**: permutar orden canónico no altera estado físico final.
5. **late body**: un body que entra tras drenar E1 no recibe E1 retrospectivamente.
6. **candidate cap exact/+1**: N es válido; N+1 produce outcome explícito sin partial set.
7. **envelope cap exact/+1**: el límite exacto funciona; superior falla cerrado antes de resolver.
8. **solver budget**: agotamiento no aplica movimiento no certificado.
9. **starting overlap separable**: recovery acotado encuentra separación válida.
10. **starting overlap irresoluble**: suspende/localiza sólo la pareja problemática.
11. **initial separation + tangent**: la recuperación deja al body sobre una cara soportante y el tramo posterior es tangencial/clear; el contacto final debe existir.
12. **block obstruction**: no existe teleport a través de bloque por movimiento material.
13. **reentrancy**: movimiento provocado por backend no reingiere el mismo evento ocultamente.
14. **lifecycle during capture**: teleport/rebind invalida trabajo stale.
15. **certification missing**: provider sin `interval(...)` no cae a `motion()` ni chord endpoint.
16. **replay fence integration**: mismo handle no produce dos resolves.
17. **hash/order metamorphic**: permutar soportes/candidatos equivalentes conserva resultado.
18. **far-world amplification**: entidades lejanas no aumentan trabajo local relevante.
19. **NaN/extreme trajectory**: se rechaza antes de mutar mundo.
20. **derived carry trap**: S07 no empieza FR-057..060 por accidente.
21. **third-party regression**: entidades no elegibles mantienen física vanilla.
22. **pre-dispatch queue saturation**: staging tiene bound y señal explícita.
23. **source/batch provenance**: ROOT/JOINT viaja desde S06.
24. **far-apart simultaneous joints**: simultaneidad temporal no implica una unión espacial gigante.
25. **joint batch mutation permutation**: compartir captura inicial no basta; el resultado debe ser conmutativo bajo permutación equivalente.
26. **pre-existing body overlap locality**: un solape de terceros no cancela un evento sano globalmente.
27. **contact establishment**: un CCD que lleva al body a una cara debe representar el contacto final.
28. **stale active binding**: misma entidad física con otra `bindingGeneration`/registro no puede certificar un handle antiguo.

## 7. Historia adversarial relevante

### A1 — contrato de tipos del body

El primer runtime parametrizaba el dispatcher como `LivingEntity` mientras la world query devolvía `Entity`, provocando fallo de compilación. La reparación correcta fue generalizar el dispatcher runtime a `Entity`, no recortar artificialmente la query a cuerpos vivos. **Cerrado.**

### A2 — localidad de joint batch

La primera versión unía todos los envelopes JOINT del mismo tick y aplicaba el cap de 64 bloques a la unión. Holdouts de soportes lejanos demostraron que simultaneidad temporal no implica proximidad espacial; el runtime pasó a clusters/localidad. **Cerrado por tests actuales.**

### A3 — staging y candidate bounds

El staging S06 era potencialmente ilimitado. `814f10ca…` introdujo `MAX_PENDING_INTERVALS=512`, saturación sticky, corte de continuidad e invalidación localizada. Los límites del dispatcher permanecen separados del staging. **Staging cerrado; candidate-budget final se mantiene como punto de revisión de rendimiento G2/G9.**

### A4 — simultaneidad física

La primera `resolveJointBatch` mutaba secuencialmente mientras conservaba bounds capturados iniciales. Los holdouts de permutación condujeron a resolución conjunta por clusters. **Cerrado por la suite actual, sujeto a revisión final cero-cambios.**

### A5 — identidad stale

La validación inicial de intervalos sólo comparaba entidad física y revisión. La lane preparada adversarial demostró que un handle de otra generación del mismo entity podía certificarse. `2b3eb3ad…` endureció la identidad activa completa y el rerun preparado dejó de fallar por stale binding. **Cerrado.**

### A6 — hook real

El primer `MaterialPhysicsRuntime` no tenía caller productivo. Los holdouts provocaron wiring root síncrono (`6cc482c5…`) y joint post-publish (`ff9a4edc…`). **Cerrado.**

### A7 — contacto tras separación inicial — ABIERTO

La reparación `33748be…` añadió establecimiento de contacto en el backend material S07 y su test sintético reflectivo pasa. Sin embargo no cubre `AnatomyMovement.collide`, que es la ruta que toma un body bajo ownership preparado.

Evidencia preparada real: workflow `s06-prepared-adversarial-proof` run **`34619053244`**, job **`103328141054`** sobre `cf36f9bb…`:

- export cliente original cow/player y pose comparisons: **PASS / BUILD SUCCESSFUL**;
- stale-binding holdout: ya no falla;
- lane server preparada: **2 tests, 1 PASS, 1 FAIL**;
- único fallo: `AnatomyGeometryTests.actualEntityMovesWithAscendingAnatomyPrepared` → `Real moving contact acquires a material face anchor`.

Evidencia mínima ordinaria: `11f7f3e81cb42952b6a51e4532a9c719a221c7e6` añadió `S07InitialSeparationContactTests`; `34121dd8cfac371a2fcbc631566c745818beb296` lo registró. GitHub Actions run **`34619303541`**, job **`103328985914`**:

- **298 tests registrados y ejecutados**;
- **297 pasaron**;
- falló **exactamente** `initialSeparationOntoSupportingFaceMustCreateAnchor`;
- la primera aserción del holdout confirma que posición final y movimiento tangencial son correctos;
- la única propiedad rota es el anclaje/contacto material final.

Clasificación: **bug de implementación S07**, no defecto de fixture ni de la lane preparada. Dos rutas distintas reproducen la misma pérdida de contacto.

## 8. Revisión del plan tras la reapertura

### P1 — requisitos y física

FR-049/FR-051 no se satisfacen sólo moviendo el AABB al lugar correcto: el resultado de la resolución debe conservar la semántica de soporte necesaria para que el siguiente movimiento no trate al body como desconectado. La corrección debe considerar el resultado de separación inicial además de `TemporalResponse.contacts()`. **Actualizado.**

### P2 — ownership

`MaterialPhysicsRuntime` y `AnatomyMovement.collide` son rutas distintas. Arreglar una no constituye evidencia de la otra. No se debe duplicar un segundo sistema de contactos; ambas deben terminar usando la misma validación final de `SurfaceContact`/support normal en la medida que su arquitectura lo permita. **Actualizado.**

### P3 — reparación aceptable

Después de initial separation, la implementación puede revalidar candidatos contra el `finalBox` mediante geometría final, eligibility y `gravity(body).supports(normal)`. No es aceptable seleccionar ciegamente el primer support/piece del mapa ni convertir cualquier overlap inicial en contacto. El resultado debe ser determinista. **Actualizado.**

### P4 — evidencia obligatoria

El nuevo holdout `S07InitialSeparationContactTests` debe permanecer intacto. Tras la reparación se exige verde en la suite ordinaria y en la lane preparada real; una sola de ellas no basta. **Actualizado.**

### P5 — scope

Esto no adelanta chains/derived carry: establecer el contacto mínimo de la interacción recién resuelta es parte de la corrección física S07, no FR-057..060. **Sin expansión de scope.**

## 9. Criterio de cierre

S07 se cierra sólo cuando:

- los holdouts S06/S07 permanecen intactos y verdes;
- la suite ordinaria ejecuta los 298+ tests sin fallos;
- la lane preparada que exporta geometría real y ejecuta `AnatomyPreparedIntegrationProof` queda verde;
- initial separation + tangent establece un contacto final válido sin anclar soportes arbitrarios;
- root/joint hooks consumen cada intervalo una sola vez;
- locality, simultaneidad, stale work, saturation, mid-interval CCD y boundaries siguen verdes;
- no existe fallback endpoint-only ni scan mundial en el hot event path;
- una revisión final cero-cambios no descubre un segundo owner de replay, causal order, material trajectory o contacto;
- el workflow preparado temporal se elimina sólo después de conservar evidencia verde del snapshot reparado.

La reconciliación de gravedad con `main@39824dd…` sigue siendo requisito previo al cierre de G2 y a la revalidación definitiva de G1/S04 respecto a la fuente transversal de gravedad.
