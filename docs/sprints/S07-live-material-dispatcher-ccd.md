# S07 — Dispatcher material real y CCD de root/joints

Estado: **PLANIFICADO / PRE-IMPLEMENTACIÓN / BLOQUEADO por S06 reabierto**. Tercer sprint de G2. El plan puede seguir revisándose, pero no debe comenzar implementación mientras los holdouts adversariales de S06 en `466c666f…` sigan rojos.

## 1. Tesis y scope

Al terminar S07, **cada intervalo material root/joint que S06 publique será drenado una única vez por `MaterialEventDispatcher` y resuelto de forma continua contra cuerpos candidatos locales usando la trayectoria certificada del handle, sin inventar endpoints ni depender del orden de mapas**.

Requisitos primarios: FR-049, FR-050 y FR-051. Requisitos de apoyo: FR-043, FR-048, FR-052, FR-053; NFR-001, NFR-002, NFR-004, NFR-007, NFR-008, NFR-015, NFR-017, NFR-033.

### Dependencia bloqueante actual

S07 consume directamente `MotionIntervalHandle`/`Pending` producidos por S06. La campaña adversarial posterior al cierre provisional de S06 demostró tres contradicciones de identidad: payload distinto reutilizando `frameSerial`, certificación de un handle con otro soporte y `Pending` con soporte distinto al de la `GeometryIdentity`. Hasta que esos casos estén reparados y toda la suite vuelva a verde, S07 no puede considerar confiable su input causal.

### Exclusiones explícitas

S07 no cierra todavía:

- FR-056..060 material anchors, obstruction carry y cadenas de soportes;
- derived carry real entre soportes, aunque el dispatcher conserve su API/ancestry preparada;
- sneak/jump, vehículos/items/falling blocks;
- prediction/reconciliation/network receipts;
- la reconciliación con `main@39824dd…`, obligatoria antes del cierre global de G2;
- G7/Clinging.

## 2. Estado actual investigado

- S06 implementó `MaterialIntervalTracker` + `MaterialIntervalRuntime`, pero su cierre provisional quedó reabierto por la campaña `466c666f…`; por tanto root/joint handles aún no son una frontera confiable para producción S07.
- `AnatomyRuntime.pollIntervals(ServerLevel)` existe, pero ningún caller de producción la drena todavía.
- `AnatomyRuntime.interval(entity, handle)` certifica `MotionSnapshot` desde los dos endpoints capturados mediante el provider activo, pero S06 debe reforzar primero la correspondencia entre `entity` y `handle.identity()`.
- `MaterialEventDispatcher` ya implementa queue caps, reentrancy gate, joint batches, candidate limits, ancestry/cycle/depth limits y separación entre captura y resolución.
- Sus tests actuales son de backend falso; todavía no demuestran world query ni solver real.
- `Platforms.tick` ejecuta `prepare → AnatomyMovement.tick → AnatomyRuntime.publish`; por tanto el primer punto en que el batch S06 está completo es inmediatamente después de `publish`.
- `AnatomyMovement.sweep`/`collide` contienen kernels históricos útiles, pero la ruta de movimiento propio consume principalmente endpoint actual. S07 no debe rededucir history desde ahí: el `MotionIntervalHandle` de S06 será la autoridad temporal cuando S06 vuelva a estar verde.
- El broadphase de S05 indexa soportes materiales, no cuerpos. Para un evento material entrante, el backend debe usar una consulta espacial local/bounded del mundo sobre el envelope certificado, nunca `level.getAllEntities()` por evento.

## 3. Estado objetivo

- Un coordinador runtime de G2 drena `pollIntervals(level)` una vez por END_LEVEL_TICK, después de `publish`.
- Root intervals se ingieren individualmente en orden causal ya fijado por S06.
- Joint intervals de la misma cadence/tick se agrupan como `ingestJointBatch`, conservando simultaneidad en vez de convertir UUID order en causalidad ficticia.
- Cada handle se convierte en `MaterialEventDispatcher.MaterialInterval` sólo si el provider certifica `MotionSnapshot` y se puede construir un envelope temporal finito/acotado.
- El backend captura cuerpos locales dentro de ese envelope con cap explícito y orden determinista.
- El resolver hace CCD cuerpo-estático frente a pieza-material-en-movimiento usando la trayectoria certificada y aplica sólo un prefijo seguro.
- Replay no llega dos veces al resolver porque S06 lo fencea; S07 añade holdout de integración que cuenta resoluciones reales.
- Fallo de certificación, envelope, candidate budget, solver budget o lifecycle produce outcome conservador/local, no fallback endpoint ni scan mundial.

## 4. Invariantes

- S06 sigue siendo owner de identidad/replay de intervalos. S07 no crea un segundo serial ni una segunda deduplicación.
- `MaterialEventDispatcher.EventId` es identidad de scheduling, no reemplazo de `materialSerial`.
- El backend captura candidatos antes de mutar y no puede lanzar después de una mutación parcial no representada por `Outcome`.
- Un cuerpo que entra en el envelope después de drenar un intervalo no recibe replay retrospectivo.
- Un contacto que existe sólo en mitad de la trayectoria debe detectarse aunque ambos endpoints estén libres.
- La resolución de dos soportes/cuerpos no depende del hash order.
- Un budget exhaust no significa mundo vacío.
- S07 no genera `DerivedCarry` de producción todavía. Si un backend intenta hacerlo, se mantiene fuera del scope o se rechaza explícitamente hasta el sprint de chains.

## 5. Plan de implementación

- [ ] I1 Introducir un coordinador runtime pequeño (`MaterialPhysicsRuntime` o nombre equivalente) que se invoque una vez después de `AnatomyRuntime.publish(level)` y drene la cola S06 exactamente una vez.
- [ ] I2 Clasificar el batch drenado en root vs joint/cadence sin rededucir causalidad desde entity state; preservar `materialSerial` y handle original.
- [ ] I3 Añadir conversión `MotionIntervalHandle -> MotionSnapshot -> MaterialInterval` fail-closed y construir envelope conservador desde la trayectoria certificada, con límite explícito.
- [ ] I4 Implementar backend real de `MaterialEventDispatcher<LivingEntity>` con captura espacial local de cuerpos, cap de candidatos y orden determinista; no usar `level.getAllEntities()` en el hot event path.
- [ ] I5 Añadir un solver de evento material que pruebe el AABB del cuerpo frente a `ConservativeSweep.Motion` por pieza durante el intervalo y determine prefijo seguro/contacto intermedio con budgets explícitos.
- [ ] I6 Aplicar el prefijo seguro al cuerpo por la ruta de movimiento autorizada, evitando recursión/reingest y preservando block/entity clipping aplicable.
- [ ] I7 Actualizar contacto/suspensión de forma localizada: hit válido puede producir/retener contacto; starting overlap irresoluble usa recovery acotado y después suspende sólo la pareja problemática.
- [ ] I8 Conectar root events como `ROOT_MUTATION` e intervalos simultáneos de joints como `JOINT_BATCH`; no emitir `DERIVED_CARRY` de producción en S07.
- [ ] I9 Instrumentar outcomes mínimos por tick: eventos admitidos, candidatos, evaluaciones, exhaustion/quarantine, para poder demostrar NFR-007 y preparar NFR-027/G9.
- [ ] I10 Añadir holdouts S07 registrados sobre el backend real y el hook real.
- [ ] I11 Ejecutar suite completa, revisar que los nuevos tests realmente incrementan el total y hacer pasada estructural cero-cambios.

## 6. Modelo adversarial previo

1. **mid-interval-only contact**: una pieza cruza el cuerpo y termina libre; endpoint-only debe fallar, CCD debe detectar.
2. **same-tick two roots**: dos handles serial 1 y 2 se resuelven una vez cada uno y en orden.
3. **root + joint cadence**: root y joint legítimos del mismo tick no colapsan ni se duplican.
4. **joint permutation**: cambiar orden de registro/mapa no altera resultado físico del batch simultáneo.
5. **late body**: cuerpo que entra después de drenar E1 no recibe E1 retrospectivamente.
6. **candidate cap exact/+1**: exactamente N candidatos es válido; N+1 cuarentena todo el evento, sin partial set.
7. **envelope cap exact/+1**: envelope justo en budget funciona; superior falla cerrado antes de world query.
8. **solver iteration exact/+1**: agotamiento explícito no aplica un movimiento no certificado.
9. **starting overlap separable**: recovery acotado encuentra separación válida.
10. **starting overlap irresoluble**: suspende sólo support/body afectado; terceros siguen normales.
11. **block obstruction**: un prefijo que atravesaría bloque se recorta; no existe teleport por material motion.
12. **reentrancy**: `setPos/move` provocado por backend no reingiere el mismo evento como root mutation oculta.
13. **lifecycle during capture**: teleport/rebind entre capture y resolve invalida el evento, no aplica snapshot stale.
14. **certification missing**: provider sin `interval(...)` no cae a `motion()` ni endpoint chord.
15. **replay fence integration**: presentar dos veces el mismo handle a la seam S06 sólo causa una llamada real a resolve.
16. **hash/order metamorphic**: permutar supports/candidates con mismo estado produce mismo resultado/hash físico.
17. **far-world amplification**: añadir 1000 entidades lejanas no aumenta candidatos/evaluaciones de una consulta local salvo coste del índice vanilla/local query subyacente.
18. **NaN/extreme trajectory**: certificación/envelope inválidos se rechazan antes de mutar mundo.
19. **derived carry trap**: S07 no debe comenzar accidentalmente FR-057..060; ningún outcome root/joint genera chain work de producción.
20. **third-party regression**: cuerpos/supports no elegibles mantienen física vanilla y no entran al dispatcher material.
21. **pre-dispatch queue saturation**: el staging S06→S07 debe tener un bound/outcome explícito; el límite interno del dispatcher no sirve si la cola previa puede crecer sin cap.
22. **source/batch provenance**: un `List<Pending>` ordenado no basta para distinguir causalmente root de joint batch; la fuente/simultaneidad debe viajar desde S06 o quedar determinada por metadata causal, nunca por estado vivo posterior.

## 7. Revisión iterativa del plan

### P1 — requisitos y criterios

FR-049 exige trayectoria continua, no endpoint chord; I3/I5 y el ataque mid-interval lo cubren. FR-050 queda dividido correctamente: S06 posee replay identity y S07 demuestra consumo físico exactly-once. FR-051 exige varias contribuciones same-tick; root individual + joint batch conserva ambas semánticas.

No es necesario incorporar FR-057..060: derived carry/chains pueden permanecer fuera sin impedir demostrar CCD root/joint. **Sin cambios.**

### P2 — arquitectura y ownership

El dispatcher conserva scheduling; S06 conserva serial/fence; provider conserva certificación de geometría temporal; el nuevo runtime sólo orquesta drain/backend. No se duplica broadphase de soportes ni se mueve solver a API pública. **Sin cambios.**

### P3 — código real y orden temporal

`Platforms.tick` sitúa `publish` después de `AnatomyMovement.tick`, así que el drain puede ocurrir inmediatamente después sin adivinar un tick futuro. La consulta de cuerpos debe ser espacial/local y acotada, no `getAllEntities()`. La revisión adversarial posterior añade dos condiciones que el plan original no explicitaba: la staging queue previa al dispatcher también debe estar acotada y la clasificación root/joint no puede inferirse desde estado vivo porque `Pending` actual no transporta source/batch. **Plan actualizado; esta pasada debe repetirse tras reparar S06.**

### P4 — errores y fail-closed

Faltas de certificación/envelope/candidatos/budget/lifecycle tienen outcomes explícitos y no fallback. El backend no puede lanzar post-mutation como mecanismo de control. **Sin cambios.**

### P5 — simplicidad y scope

No hace falta integrar receipts, red, prediction ni chains para demostrar la tesis. `DerivedCarry` se conserva en dispatcher como capacidad futura pero producción S07 no la emite. **Sin cambios.**

El plan ya no se considera listo para implementación mientras S06 esté rojo. Tras la reparación S06 debe repetirse P1–P5 sobre la frontera causal final y obtener una pasada completa sin cambios antes de empezar I1.

## 8. Criterio de cierre

S07 se cierra sólo cuando:

- S06 está nuevamente cerrado y verde con sus holdouts adversariales;
- la cola S06 se drena una sola vez desde el hook real y está acotada de forma explícita antes del dispatcher;
- root/joint events reales llegan al `MaterialEventDispatcher` con provenance suficiente para no rededucir source/simultaneidad desde estado vivo;
- un contacto sólo intermedio es detectado con trayectoria certificada;
- replay/same-tick/batches/budgets/lifecycle tienen holdouts registrados y verdes;
- no existe fallback a endpoint-only ni scan mundial en el hot event path;
- una revisión final no descubre un segundo owner de replay, causal order o material trajectory;
- el total de GameTests aumenta de forma esperada y toda la suite queda verde.

La reconciliación de gravedad con `main@39824dd…` sigue siendo requisito de **cierre de G2**, no de cierre de S07.
