# S24 — lifecycle generation fencing

Rol activo: **IMPLEMENTER**.

Estado: **IMPLEMENTACIÓN DE G3.9–G3.12 VERDE / PENDIENTE DE CIERRE ADVERSARIAL**. Este documento registra el candidato implementer y su evidencia. No modifica por sí solo los checkboxes canónicos de `ENTITY_COLLISIONS_PLAN.md` ni sustituye la revisión adversarial independiente.

## 1. Alcance consolidado

S24 empezó aislando la identidad temporal de pose y terminó cubriendo los cuatro trabajos G3 que dependen de lifecycle estable:

- **G3.9** — reload/tracking/unload/rebind/dimension/reconnect/reutilización de identidad;
- **G3.10** — estados no soportados publican `UNAVAILABLE` y recuperan sin freeze;
- **G3.11** — lifecycle/order sobre runtime y packets reales;
- **G3.12** — separación final de ownership root/endpoint que G2 dejó deliberadamente para G3.

La regla común es que ninguna vida temporal puede legitimarse usando sólo UUID, network id, revision o una generación del eje equivocado.

## 2. Dos ejes lifecycle: binding y tracking

El wire de pose `anatomy_pose_v6` transporta dos generaciones independientes:

- `bindingGeneration`: vida causal del binding físico server-side;
- `trackingGeneration`: ventana de visibilidad específica de receptor + UUID.

`AnatomyFrameHistory.transition(...)` clasifica continuidad/restart/reject sin convertir `accept(...)` en una segunda state machine. Un `RESTART` obliga al receiver a crear un historial nuevo; `accept(...)` conserva el contrato S00 de identidad exacta.

La autoridad server-side de tracking usa `TrackingGenerationLedger`:

- scalar monotónico que no rebobina al liberar UUIDs;
- mapa sólo de ventanas activas;
- `MAX_ACTIVE = 4096`;
- saturación fail-closed sin eviction de otra ventana autoritativa;
- `STOP_TRACKING` libera la entrada sin conservar tombstones server-side;
- disconnect/reset limpian los ledgers del receptor.

La prueba de estrés recorre 50 000 ventanas secuenciales y demuestra memoria constante y no reutilización de generation.

## 3. Replay fences cliente y lifecycle por dimensión

El cliente conserva dos niveles distintos de estado:

- **material del `ClientLevel`**: poses, geometry providers, evaluators, presentation frames y pending contacts;
- **causalidad de la conexión**: replay fences y watermarks que deben sobrevivir a un cambio de level, pero no a disconnect/revision replacement.

`TrackingReplayFence` retira por `(UUID, dimension)` en lugar de un único tombstone global. Esto permite A→B→A sin aceptar un paquete antiguo de la primera vida en A y sin bloquear el estado legítimo de B.

Los tombstones retienen la identidad causal necesaria para impedir resurrection tardía: epoch/revision/entity id/UUID/model/providers/binding generation/tracking generation/frame serial y authority/sample ticks. La capacidad es acotada y la saturación queda sticky/fail-closed hasta reset de conexión.

Para contactos, `AnatomyContactInbox.clearPending()` elimina sólo material pendiente al cruzar nivel; los watermarks de la conexión permanecen. `clear()` sigue reservado a replacement de conexión/catálogo.

## 4. Contactos y rebind del soporte

Se encontró un replay real adicional: un contacto viejo podía conservar el mismo support UUID/entity id/piece y reinterpretarse después de un rebind del soporte.

La solución fue identidad de protocolo, no heurística local:

- `AnatomyContactPayload` pasó a `anatomy_contact_v6`;
- un contacto presente transporta `supportBindingGeneration >= 1`;
- producción obtiene esa generation del descriptor vivo del soporte;
- el cliente sólo materializa/presenta el contacto si coincide con el binding generation del frame vivo del soporte;
- el watermark se conserva aunque el contacto material viejo se consuma y descarte.

La lane adversarial de mutación `35130851057` pasó tanto el baseline como el mutant que elimina exclusivamente esos fences: el holdout mata el mutant. La recuperación con contacto fresco tras rebind también permanece verde.

## 5. Dimensión, reconnect, reload y reutilización

La suite integrada cubre ahora:

- **dimensión**: material del level anterior desaparece, pero la causalidad de conexión impide replay A→B→A;
- **reconnect**: catálogo/pose/contact/replay state de la conexión anterior no legitima la nueva sesión;
- **unload/retrack**: una tracking generation retirada no revive aunque binding avance;
- **network-id reuse**: la sustitución observada retira la generación antigua; un timeout ordinario no se interpreta como lifecycle authority;
- **reload válido**: rebind transaccional a la nueva revisión/identidad;
- **reload inválido**: el snapshot/revisión aceptados continúan intactos;
- **bootstrap/reset con múltiples receptores incompatibles**: `AnatomyRuntime.reset` itera un snapshot del player list, no la lista viva que `catalog(...)` puede modificar al desconectar receptores.

La corrección del player-list quedó revalidada sobre el holdout adversarial sin modificarlo en run `35131477985`. Esa lane es evidencia implementer adicional, no una declaración de cierre adversarial.

## 6. G3.10 — `UNAVAILABLE` real y recuperación

`S24UnavailableRecoveryClientProof` usa una vaca real con el perfil `scalebrews:quadruped` y la transición runtime:

`STANDING → SLEEPING → STANDING`.

La fase no soportada demuestra simultáneamente:

- la sesión/catálogo siguen READY y con el mismo epoch/revision;
- el soporte conserva UUID/entity id/binding/tracking identity;
- `presentationFrame` y geometry desaparecen fail-closed;
- no se exige retener una pose material stale: el receiver puede descartar material mientras conserva causalidad.

Al volver a `STANDING`, llega un endpoint `AVAILABLE` fresco con `frameSerial` mayor y reaparecen presentation + geometry sin rebind/retrack artificial.

Evidencia implementer corregida: run `35131733393`, **success**; build del mismo candidato `35131733233`, **success**. Tras la extracción de endpoint/root ownership, el mismo proof volvió a pasar en `35135583002`.

## 7. G3.11 — late tracking y orden real de packets

`S24LateTrackingOrderClientProof` cubre un observador que empieza a trackear tarde una entidad que ya lleva tiempo viva y tickeando:

1. el primer paquete aceptado materializa directamente estado actual (`frameSerial > 1`), no una reproducción desde el nacimiento de la entidad;
2. dentro de esa misma tracking window se captura un frame real aceptado;
3. llega un frame más nuevo;
4. se reinyecta el frame anterior a través del receiver real;
5. serial/authority/presentation no retroceden.

Primer verde implementer: `35133536245`. Tras extraer los ledgers de task 12, el proof volvió a pasar en `35135606727`.

## 8. G3.12 — ownership root/endpoint fuera del orquestador

G2 había dejado explícitamente root history + endpoint serials dentro de `AnatomyMovement` porque `GeometryProvider.CausalEndpoint` dependía de `AnatomyMovement.RootFrame`. Tras estabilizar root/lifecycle en S21/S24 esa dependencia dejó de ser necesaria.

El corte final se hizo en dos pasos, cada uno compilado y revalidado antes del siguiente:

### 8.1 Root provenance

- `collision.runtime.RootFrame` es el DTO neutral;
- `collision.runtime.RootFrameLedger` posee sequence/history/retención/dedupe/discontinuity;
- conserva semántica previa: secuencia inicial 0, máximo 64 frames, ventana 20 ticks, discontinuidad por salto >4 bloques o cambio de gravedad;
- el ledger **no** limpia contactos ni conoce física; devuelve `discontinuity` y `AnatomyMovement` decide la reacción.

`S24RootFrameLedgerTests` fija dedupe, monotonicidad, stale-before, discontinuidad y boundedness de forma directa.

### 8.2 Endpoint serial ownership

- `collision.internal.AnatomyEndpointLedger` posee stamp/endpoint/snapshot/invalidated y el mapa weak-key por entidad;
- `AnatomyMovement` conserva las decisiones de aceptación, quarantine, unavailable, contactos y spatial refresh;
- no queda `FRAME_SERIALS`, `EndpointStamp` ni `EndpointSerial` como ownership anidado del orquestador.

Se mantuvo `AnatomyEndpointLedger` en `collision.internal` de forma deliberada: sus entradas dependen de `GeometryProvider`/`AnatomyPoseHistory`; moverlo a `collision.runtime` habría creado una dependencia inversa `runtime → internal` y una falsa separación.

La migración automática del segundo corte compiló `main + client + gametest` antes del push y el commit resultante `4511bdc403856185bb14eed963eadca62eef3c03` modificó únicamente `AnatomyMovement.java`.

## 9. Evidencia de regresión relevante

Verdes posteriores a los fences de dimensión/contact-v6:

- lifecycle generation `35130391549`;
- TTL/replay fence `35130391552`;
- reconnect `35130391598`;
- dimension lifecycle `35130391647`;
- ordinary build `35130391596`;
- pose dimension replay `35130391518`;
- contact dimension replay `35130391543`;
- contact support-rebind replay `35130391795`.

Verdes posteriores a la separación estructural G3.12:

- unavailable recovery `35135583002`;
- late tracking/order `35135606727`;
- lifecycle generation + root ledger contract `35135695700`;
- ordinary build `35135695698`.

Todos ellos son evidencia sobre la rama integrada. Los verdes implementer no sustituyen mutation/holdout/zero-change adversarial cuando el gate lo exija.

## 10. Handoff adversarial

No hay un bug productivo S24 conocido pendiente en tasks 9-12 al final de este tramo. El handoff debe atacar, sin cambiar expectativas para obtener verde:

1. mutaciones que eliminen fences de generation/dimension/rebind/unavailable;
2. replay/out-of-order sobre runtime y packets reales;
3. unload/reconnect/entity-id reuse tras largas sesiones y saturación bounded;
4. ownership estructural: reintroducir root/endpoint history en `AnatomyMovement` debe ser detectable;
5. final zero-change read.

Hasta esa revisión, **G3.9–G3.12 permanecen formalmente abiertos en el plan canónico aunque el candidato implementer esté verde**.
