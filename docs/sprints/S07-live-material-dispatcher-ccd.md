# S07 — Dispatcher material real y CCD de root/joints

Estado: **IMPLEMENTACIÓN PARCIAL / BLOQUEADO por S06 reabierto y build rojo**. Tercer sprint de G2. Existe ya un primer `MaterialPhysicsRuntime`, pero no puede considerarse evidencia de implementación válida mientras S06 siga rojo y el árbol actual no compile.

## 1. Tesis y scope

Al terminar S07, **cada intervalo material root/joint que S06 publique será drenado una única vez por `MaterialEventDispatcher` y resuelto de forma continua contra cuerpos candidatos locales usando la trayectoria certificada del handle, sin inventar endpoints ni depender del orden de mapas**.

Requisitos primarios: FR-049, FR-050 y FR-051. Requisitos de apoyo: FR-043, FR-048, FR-052, FR-053; NFR-001, NFR-002, NFR-004, NFR-007, NFR-008, NFR-015, NFR-017, NFR-033.

### Dependencia bloqueante actual

S07 consume directamente `MotionIntervalHandle`/`Pending` producidos por S06. La campaña adversarial posterior al cierre provisional de S06 demostró contradicciones de identidad causal. Los tres primeros holdouts quedaron probados rojos en `466c666f…`; `c9b78f27…` profundizó la campaña con exactitud del `before` aceptado y ejes independientes de identidad soporte/dimensión/id. Ese snapshot no llegó a ejecutar GameTests porque el primer runtime S07 introdujo un fallo de compilación ajeno a los holdouts.

### Exclusiones explícitas

S07 no cierra todavía:

- FR-056..060 material anchors, obstruction carry y cadenas de soportes;
- derived carry real entre soportes, aunque el dispatcher conserve su API/ancestry preparada;
- sneak/jump, vehículos/items/falling blocks;
- prediction/reconciliation/network receipts;
- la reconciliación con `main@39824dd…`, obligatoria antes del cierre global de G2 y antes de volver a considerar definitiva la validación de G1/S04 respecto a la nueva autoridad de gravedad;
- G7/Clinging.

## 2. Estado actual investigado

- S06 implementó `MaterialIntervalTracker` + `MaterialIntervalRuntime`, pero su cierre provisional quedó reabierto por evidencia adversarial; root/joint handles aún no son una frontera confiable para cerrar S07.
- `MaterialIntervalRuntime.Pending` ya transporta `Source.ROOT` / `Source.JOINT`, corrigiendo la carencia inicial de provenance, pero todavía debe demostrar que support e identidad del handle no pueden contradecirse.
- `AnatomyRuntime.interval(entity, handle)` certifica `MotionSnapshot` desde los endpoints capturados mediante el provider activo, pero S06 debe reforzar primero la correspondencia completa entre `entity` y `handle.identity()`.
- `MaterialEventDispatcher` ya implementa queue caps, reentrancy gate, joint batches, candidate limits, ancestry/cycle/depth limits y separación entre captura y resolución.
- `6228523103b237424c2d0289cf90d2e8fcd13be4` añadió un primer `MaterialPhysicsRuntime` de producción con preparación de intervalos, envelope temporal, world query y resolución CCD.
- Ese runtime **no está todavía conectado** en `Platforms.tick` ni inmediatamente después de `MaterialIntervalRuntime.commitRoot`; el código actual no tiene un caller de `MaterialPhysicsRuntime.drain` en las rutas inspeccionadas.
- El snapshot adversarial `c9b78f27c1973434edb71da68028d51db0786816` falla `compileJava`: `MaterialPhysicsRuntime` declara `MaterialEventDispatcher<LivingEntity>` pero captura `Entity`, por lo que no puede construir `Candidate<LivingEntity>` con un `Entity`. GitHub Actions run `34613892535`, job `103310894474` no llegó a GameTests.
- El broadphase de S05 indexa soportes materiales, no cuerpos. Para un evento material entrante, el backend S07 usa una query espacial del mundo, pero todavía debe demostrar que el **número de entidades visitadas/materializadas** también queda acotado antes del cap y no sólo comprobado después.

## 3. Estado objetivo

- Un coordinador runtime de G2 drena `pollIntervals(level)` una vez por END_LEVEL_TICK, después de `publish`, y root intervals en el punto causal que corresponda sin dejarlos acumular hasta reinterpretarlos más tarde.
- Root intervals se ingieren individualmente en orden causal ya fijado por S06.
- Joint intervals de la misma cadence/tick se agrupan como `ingestJointBatch`, conservando simultaneidad sin convertir UUID order en causalidad ficticia y sin convertir distancia espacial entre soportes en un fallo colectivo.
- Cada handle se convierte en `MaterialEventDispatcher.MaterialInterval` sólo si el provider certifica `MotionSnapshot` y se puede construir un envelope temporal finito/acotado.
- El backend captura cuerpos locales dentro del envelope con cap explícito y orden determinista, sin materializar primero una colección no acotada.
- El resolver hace CCD cuerpo-estático frente a pieza-material-en-movimiento usando la trayectoria certificada y aplica sólo un prefijo seguro.
- Replay no llega dos veces al resolver porque S06 lo fencea; S07 añade holdout de integración que cuenta resoluciones reales.
- Fallo de certificación, envelope, candidate budget, solver budget o lifecycle produce outcome conservador/local, no fallback endpoint ni scan mundial.

## 4. Invariantes

- S06 sigue siendo owner de identidad/replay de intervalos. S07 no crea un segundo serial ni una segunda deduplicación.
- `MaterialEventDispatcher.EventId` es identidad de scheduling, no reemplazo de `materialSerial`.
- El backend captura candidatos antes de mutar y no puede lanzar después de una mutación parcial no representada por `Outcome`.
- Un cuerpo que entra en el envelope después de drenar un intervalo no recibe replay retrospectivo.
- Un contacto que existe sólo en mitad de la trayectoria debe detectarse aunque ambos endpoints estén libres.
- La resolución de dos soportes/cuerpos no depende del hash order ni del orden arbitrario elegido para miembros simultáneos.
- Un budget exhaust no significa mundo vacío y un soporte patológico no cuarentena soportes sanos espacialmente independientes.
- S07 no genera `DerivedCarry` de producción todavía. Si un backend intenta hacerlo, se mantiene fuera del scope o se rechaza explícitamente hasta el sprint de chains.

## 5. Plan de implementación

- [ ] I1 Completar un coordinador runtime pequeño (`MaterialPhysicsRuntime` o equivalente) conectado a los hooks reales y que drene la cola S06 exactamente una vez. La clase existe, pero aún no hay hook real y el árbol no compila.
- [x] I2 Transportar provenance ROOT/JOINT desde S06 sin rededucirla desde entity state; preservar `materialSerial` y handle original. Pendiente revalidación tras reparar identidad S06.
- [ ] I3 Validar conversión `MotionIntervalHandle -> MotionSnapshot -> MaterialInterval` fail-closed y envelope conservador con límites sin crear un límite espacial colectivo para soportes independientes de un mismo joint batch.
- [ ] I4 Implementar backend real con captura espacial local de cuerpos, cap de candidatos y orden determinista; la decisión de tipos support/body debe ser coherente (`LivingEntity` support no implica necesariamente `LivingEntity` body) y la query debe cortar trabajo al llegar al límite.
- [ ] I5 Añadir/validar solver de evento material que pruebe el AABB del cuerpo frente a `ConservativeSweep.Motion` por pieza y determine prefijo seguro/contacto intermedio con budgets explícitos.
- [ ] I6 Aplicar el prefijo seguro por ruta autorizada, evitando recursión/reingest y preservando block/entity clipping aplicable.
- [ ] I7 Actualizar contacto/suspensión de forma localizada: el primer runtime actual descarta `TemporalResponse.contacts()` y mueve cuerpos, pero no demuestra todavía establecimiento/retención del `SurfaceContact` material correspondiente.
- [ ] I8 Conectar root events como `ROOT_MUTATION` e intervalos simultáneos de joints como `JOINT_BATCH`; no emitir `DERIVED_CARRY` de producción en S07.
- [ ] I9 Instrumentar outcomes mínimos por tick: eventos admitidos, candidatos realmente visitados, evaluaciones, exhaustion/quarantine, para demostrar NFR-007 y preparar NFR-027/G9.
- [ ] I10 Añadir holdouts S07 registrados sobre backend real y hook real, incluidos localidad espacial de batch y permutación simultánea.
- [ ] I11 Ejecutar suite completa, revisar incremento real del total y hacer pasada estructural cero-cambios.

## 6. Modelo adversarial

1. **mid-interval-only contact**: una pieza cruza el cuerpo y termina libre; endpoint-only debe fallar, CCD debe detectar.
2. **same-tick two roots**: dos handles serial 1 y 2 se resuelven una vez cada uno y en orden.
3. **root + joint cadence**: root y joint legítimos del mismo tick no colapsan ni se duplican.
4. **joint permutation**: cambiar orden de registro/mapa no altera resultado físico del batch simultáneo.
5. **late body**: cuerpo que entra después de drenar E1 no recibe E1 retrospectivamente.
6. **candidate cap exact/+1**: exactamente N candidatos es válido; N+1 produce outcome explícito sin partial set y sin recorrer/materializar arbitrariamente muchos antes de detectar el límite.
7. **envelope cap exact/+1**: envelope individual justo en budget funciona; superior falla cerrado antes de world query.
8. **solver iteration exact/+1**: agotamiento explícito no aplica movimiento no certificado.
9. **starting overlap separable**: recovery acotado encuentra separación válida.
10. **starting overlap irresoluble**: suspende sólo support/body afectado; terceros siguen normales.
11. **block obstruction**: un prefijo que atravesaría bloque se recorta; no existe teleport por material motion.
12. **reentrancy**: `setPos/move` provocado por backend no reingiere el mismo evento como root mutation oculta.
13. **lifecycle during capture**: teleport/rebind entre capture y resolve invalida el evento, no aplica snapshot stale.
14. **certification missing**: provider sin `interval(...)` no cae a `motion()` ni endpoint chord.
15. **replay fence integration**: presentar dos veces el mismo handle a la seam S06 sólo causa una llamada real a resolve.
16. **hash/order metamorphic**: permutar supports/candidates con mismo estado produce mismo resultado/hash físico.
17. **far-world amplification**: añadir entidades lejanas no aumenta candidatos/evaluaciones de una consulta local.
18. **NaN/extreme trajectory**: certificación/envelope inválidos se rechazan antes de mutar mundo.
19. **derived carry trap**: S07 no debe comenzar accidentalmente FR-057..060; ningún outcome root/joint genera chain work de producción.
20. **third-party regression**: cuerpos/supports no elegibles mantienen física vanilla y no entran al dispatcher material.
21. **pre-dispatch queue saturation**: el staging S06→S07 debe tener un bound/outcome explícito; el límite interno del dispatcher no sirve si la cola previa puede crecer sin cap.
22. **source/batch provenance**: ROOT/JOINT y simultaneidad deben viajar desde S06; ya existe `Pending.Source`, pero falta prueba de integración completa.
23. **far-apart simultaneous joints**: dos soportes con envelopes individuales válidos y separados >64 bloques no pueden convertir la unión espacial del batch en `CANDIDATE_LIMIT`/quarantine colectiva. La simultaneidad temporal no implica proximidad espacial.
24. **joint batch mutation permutation**: dos eventos simultáneos que afectan al mismo cuerpo deben producir el mismo estado final al permutar el orden canónico de los miembros. Compartir captura inicial pero mutar secuencialmente no es por sí solo prueba de simultaneidad física.
25. **pre-existing body overlap locality**: dos candidatos que ya solapan entre sí no pueden provocar automáticamente cuarentena de todo el soporte si el material event no creó ese conflicto; failure localization debe identificar la pareja/evento relevante.
26. **contact establishment**: un CCD que desplaza un cuerpo hasta una cara de soporte debe conservar/crear el contacto material necesario; mover con `setPos` y descartar los contactos del solver no basta para declarar resuelta la interacción.

## 7. Revisión adversarial del primer runtime

### A1 — compilación y tipos

`6228523103b237424c2d0289cf90d2e8fcd13be4` introdujo `MaterialPhysicsRuntime`, pero el run `34613892535` falla en `compileJava`: el dispatcher está parametrizado con `LivingEntity` y el world query produce `Entity`. Esto no es sólo una errata que deba taparse con un cast: la reparación debe decidir conscientemente el contrato entre **support viviente** y **body físico**, para no excluir accidentalmente categorías que el core deba conservar ni permitir supports inválidos.

### A2 — locality de joint batch

`captureJointBatch` une todos los envelopes de eventos JOINT del mismo authority tick y aplica `MAX_ENVELOPE_SPAN=64` a la unión. Dos soportes individualmente válidos a más de 64 bloques hacen que el batch completo aparezca saturado. La cadencia simultánea es una relación temporal, no una región espacial. Este diseño viola la expectativa de fallo localizado y debe cambiar o quedar demostrado con un esquema de partición local equivalente.

### A3 — candidate budget real

La captura usa `level.getEntitiesOfClass(..., envelope, predicate)` y sólo después comprueba `entities.size()>maximumBodies`. Eso limita el resultado publicado, pero no demuestra que la **búsqueda/lista previa** esté acotada por `maximumBodies`. NFR-007 exige límites explícitos del trabajo, no sólo rechazar después de haber materializado una multitud local patológica.

### A4 — simultaneidad física

`resolveJointBatch` comparte una captura inicial, pero recorre los eventos y llama `resolveAll` secuencialmente; cada `resolveAll` puede mutar posiciones con `setPos`, mientras los `Candidate.bounds` siguen siendo los del snapshot inicial. El resultado debe superar un holdout metamórfico de permutación antes de considerar que `JOINT_BATCH` conserva simultaneidad y no que simplemente ha serializado eventos concurrentes con mejor marketing.

### A5 — contacto y localización de fallo

El primer runtime reduce `TemporalResponse.Result` a `Plan(body, displacement, evaluations)`, descartando los contactos temporales antes de mover. Además, `finalOverlap` puede rechazar el evento completo por solape final entre dos candidatos. Antes del cierre hacen falta pruebas que distingan contacto válido, solape previo ajeno al evento y conflicto creado por el propio evento; un fallo local no debe cancelar un soporte sano entero sin identificar la pareja problemática.

### A6 — hook real

La clase documenta que root callers y END_LEVEL_TICK invocarán `drain`, pero en el snapshot inspeccionado `PlatformEntityMixin` termina en `MaterialIntervalRuntime.commitRoot` y `Platforms.tick` termina la secuencia `prepare → AnatomyMovement.tick → AnatomyRuntime.publish` sin llamar a `MaterialPhysicsRuntime.drain`. Hasta que exista ese wiring, el runtime es código no consumido y no constituye evidencia funcional S07.

## 8. Revisión iterativa del plan

### P1 — requisitos y criterios

FR-049 exige trayectoria continua, no endpoint chord; I3/I5 y el ataque mid-interval lo cubren. FR-050 queda dividido correctamente: S06 posee replay identity y S07 demuestra consumo físico exactly-once. FR-051 exige varias contribuciones same-tick; root individual + joint batch debe conservar ambas semánticas sin imponer proximidad espacial ni orden físico artificial. **Actualizado por A2/A4.**

### P2 — arquitectura y ownership

El dispatcher conserva scheduling; S06 conserva serial/fence; provider conserva certificación de geometría temporal; el runtime sólo orquesta drain/backend. La implementación debe separar conceptualmente tipo de support y tipo de body aunque la parametrización concreta use una superclase común. **Actualizado por A1.**

### P3 — código real y orden temporal

La consulta de cuerpos es espacial/local, pero falta demostrar límite de trabajo antes de materializar candidatos. El wiring real de drain tampoco existe todavía. **Actualizado por A3/A6.**

### P4 — errores y fail-closed

Faltas de certificación/envelope/candidatos/budget/lifecycle tienen outcomes explícitos y no fallback. Pero quarantine debe ser localizada y no derivarse de la extensión espacial del batch temporal ni de un solape entre terceros no causado por el evento. **Actualizado por A2/A5.**

### P5 — simplicidad y scope

No hace falta integrar receipts, red, prediction ni chains para demostrar la tesis. `DerivedCarry` se conserva en dispatcher como capacidad futura pero producción S07 no la emite. **Sin cambios.**

La implementación no converge todavía: existe código nuevo, pero el build está rojo y A1–A6 dejan condiciones adversariales abiertas.

## 9. Criterio de cierre

S07 se cierra sólo cuando:

- S06 está nuevamente cerrado y verde con sus holdouts adversariales;
- el árbol compila y la suite servidor realmente ejecuta los nuevos tests;
- la cola S06 se drena una sola vez desde hooks reales y está acotada de forma explícita antes del dispatcher;
- root/joint events reales llegan al dispatcher con provenance suficiente;
- batches simultáneos espacialmente separados no se cuarentenan por una unión artificial de sus envelopes;
- un contacto sólo intermedio es detectado con trayectoria certificada y el contacto material final queda representado correctamente;
- replay/same-tick/batches/budgets/lifecycle tienen holdouts registrados y verdes;
- el candidate cap limita trabajo antes de materializar una colección local arbitrariamente grande;
- no existe fallback a endpoint-only ni scan mundial en el hot event path;
- una revisión final no descubre un segundo owner de replay, causal order o material trajectory;
- el total de GameTests aumenta de forma esperada y toda la suite queda verde.

La reconciliación de gravedad con `main@39824dd…` sigue siendo requisito previo al cierre de G2 y a la revalidación definitiva de G1/S04 respecto a la nueva fuente transversal de gravedad.
