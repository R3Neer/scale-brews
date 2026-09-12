# S14 — Binding state ownership

Estado operativo: **EN EJECUCIÓN**. Contribuye a G2 tareas 1/9; no cambia por sí solo el estado canónico de G2 hasta disponer de evidencia ejecutada y revisión final.

## 1. Tesis

Al terminar S14, el registro vivo de bindings anatómicos tendrá un único owner dedicado y acíclico: provider actual, descriptor causal opcional, generación local monotónica, quarantine de la generación actual y guard de captura reentrante dejarán de residir en `AnatomyMovement`.

La extracción se demostrará estructuralmente y mediante lifecycle/rebind reales, sin mover `FRAME_SERIALS`, endpoint serialisation, root history, spatial membership, contacto, transporte ni solver.

Gate: **G2 — pipeline material continuo Q2**, tareas arquitectónicas 1/9.

## 2. Scope

### Incluido

- ownership único del binding vivo por instancia de entidad;
- provider actual y descriptor `GeometryIdentityDescriptor` opcional;
- `localRegistrationGeneration` monotónico por instancia;
- quarantine ligada exclusivamente a la generación local actual;
- guard reentrante de captura por entidad;
- snapshot estable del conjunto de providers para tick/rebuild;
- deactivate por `Level` que retire el binding activo sin rebobinar la generación;
- rebind causal atómico: provider + descriptor + nueva generación aparecen en una sola transición;
- migración del consumidor real de generación (`AnatomyRuntime.acceptsIntervalIdentity`) cuando no requiera una façade artificial;
- preservación de world-thread/fail-closed/lifecycle fences existentes.

### Excluido explícitamente

- `EndpointStamp`, `EndpointSerial`, `FRAME_SERIALS`, `serverEndpoint`, `acceptEndpoint` y la semántica de frame serial;
- `RootFrame`, `RootHistory`, `ROOTS`, `observeRoot` e invalidación de root;
- `AnatomySpatialIndex`, `MaterialBroadphase` y envelopes;
- `AnatomyContactState`, receipts, carry/`TransportLedger`;
- `TemporalResponse`, CCD, separation y cualquier cambio de budgets físicos;
- extracción de `queryFrame/currentSnapshot` si exige callback inverso al orquestador;
- generalización de `RootTransformProvider`, lifecycle de catálogo o trabajo propio de G3.

## 3. Estado actual

Después de S13, `AnatomyMovement` ya no posee gravedad compartida, contacto persistente, transporte pasivo ni índice espacial. Sin embargo aún declara cinco piezas de estado que forman una única responsabilidad de binding vivo:

- `PROVIDERS`;
- `REGISTRATIONS`;
- `DESCRIPTORS`;
- `QUARANTINED_REGISTRATIONS`;
- `CAPTURING`.

El resto del estado causal no es equivalente: `FRAME_SERIALS` y `ROOTS` describen material history/endpoints y permanecen fuera de S14.

### Consumidores reales

- `AnatomyMovement` necesita provider/descriptor/generation/quarantine para publicación, current geometry, tick y revalidación.
- `AnatomyRuntime.acceptsIntervalIdentity` consume la generación local para rechazar handles de bindings anteriores.
- Los tests Q1/S06 dependen de que un rebind mantenga la `bindingGeneration` autoritativa pero incremente `localRegistrationGeneration`.

No existe un consumidor externo que necesite mutar esos mapas individualmente.

### Hallazgo adicional

El overload causal actual `register(support, provider, descriptor)` llama primero a `register(support, provider)`. Si existe un índice espacial current, esa primera fase puede tratar temporalmente al support como binding fixture/legacy sin descriptor y muestrear el provider antes de instalar la identidad causal; después `queryFrame` vuelve a muestrearlo como binding causal.

S14 debe eliminar esa ventana intermedia: un rebind causal instala provider, descriptor y generación de forma atómica y sólo entonces invalida/reconstruye consumidores derivados.

## 4. Frontera objetivo

Nuevo owner package-private: `collision.internal.AnatomyBindingState`.

Puede conocer:

- `LivingEntity` / `Level`;
- `GeometryProvider` y `GeometryProvider.GeometryIdentityDescriptor`;
- weak identity storage y contadores locales.

No puede conocer ni llamar:

- `AnatomyMovement`;
- `AnatomyContactState`;
- `AnatomySpatialIndex`;
- `TransportLedger` / receipts;
- `Platforms`;
- solver/physics.

Dirección prevista: `AnatomyMovement -> AnatomyBindingState`; `AnatomyRuntime` puede leer la generación directamente del owner si esto elimina una façade sin crear ciclo.

### Semántica del slot

Cada entidad conserva un slot weak-identity con:

- provider activo o ausencia;
- descriptor activo o ausencia;
- generación monotónica, incluso cuando el binding activo se desactiva;
- quarantine sólo de la generación actualmente activa;
- flag de captura reentrante.

Rebind incrementa generación exactamente una vez y limpia quarantine. `deactivate(level)` elimina provider/descriptor/quarantine del binding activo, pero no rebobina la generación de la misma instancia. El guard de captura no puede abrir una captura anidada aunque el provider rebindee durante la captura exterior.

## 5. Plan de implementación

- [ ] I1 Añadir holdout estructural rojo: exigir `AnatomyBindingState` y prohibir los cinco owners de binding en `AnatomyMovement`.
- [ ] I2 Añadir holdouts de generación/quarantine/deactivate/reentrancia sobre la frontera real, sin seam que replique publication logic.
- [ ] I3 Añadir holdout rojo de rebind causal atómico: con índice same-tick ya construido, el provider causal se muestrea una sola vez durante `register(..., descriptor)`.
- [ ] I4 Implementar `AnatomyBindingState` con weak identity slot, generación monotónica, quarantine y guard de captura.
- [ ] I5 Migrar lecturas/escrituras de provider/descriptor/generation/quarantine/capture desde `AnatomyMovement`.
- [ ] I6 Hacer fixture rebind y causal rebind dos transiciones explícitas; causal rebind instala provider+descriptor atómicamente antes de invalidar/reconstruir estado derivado.
- [ ] I7 Mantener `FRAME_SERIALS`, endpoint state y root history intactos y hacer que sus invalidaciones consuman el owner nuevo sin callback inverso.
- [ ] I8 Migrar `AnatomyRuntime.acceptsIntervalIdentity` a lectura directa del owner si la façade `AnatomyMovement.registrationGeneration` queda sin consumidor real; eliminar sólo wrappers huérfanos.
- [ ] I9 Deactivate por nivel retira binding activo y quarantine preservando watermark monotónico.
- [ ] I10 Ejecutar suite ordinaria y lane prepared real-geometry sobre el mismo snapshot.
- [ ] I11 Revisión adversarial post-verde: rebind durante capture, stale generation, level deactivate/reactivate, identidad débil server/client y ausencia de callbacks/ciclos.
- [ ] I12 Actualizar evidencia/cierre canónico sólo tras una pasada completa sin cambios de producción.

## 6. Revisiones iterativas del plan

### Revisión P1 — requisitos y arquitectura

Sin cambios: la frontera contribuye a G2 tareas 1/9 y conserva NFR-001/002/004/007/008/015/017. No adelanta G3.

### Revisión P2 — consumers y ciclos

Cambio aplicado: se descartó extraer `queryFrame/currentSnapshot` en S14. Esas queries consumen directamente provider + endpoint/root + eligibility/suspension; extraerlas ahora exigiría callbacks inversos a `AnatomyMovement` y produciría una separación nominal, no de ownership.

### Revisión P3 — lifecycle y error states

Cambio aplicado: la generación local debe sobrevivir a `deactivate(level)` para la misma instancia; quarantine no. El guard de captura pertenece al slot y una captura exterior mantiene bloqueada la reentrancia incluso si ocurre rebind durante el callback.

### Revisión P4 — simplicidad y verificabilidad

Cambio aplicado: el causal rebind debe ser atómico. El overload actual pasa por un estado descriptorless intermedio y puede duplicar sampling same-tick. Se incorpora un holdout de contador de samples.

### Revisión P5 — pasada completa

Sin cambios adicionales. La frontera queda limitada al binding vivo y todos los demás owners permanecen explícitamente fuera.

**Convergencia del plan:** alcanzada antes de modificar producción.

## 7. Modelo adversarial previo

### A1 — rebind monotónico

Dos rebinds consecutivos de la misma instancia incrementan la generación exactamente una vez cada uno. Mantener la misma `bindingGeneration` de catálogo no permite reciclar la generación local.

### A2 — deactivate no rebobina

Binding activo → deactivate(level) → reactivate/rebind de la misma instancia. El nuevo binding recibe una generación mayor, no `1` ni la generación anterior.

### A3 — quarantine de generación

Quarantine bloquea sólo la generación actual. Un rebind posterior limpia la quarantine y no puede ser invalidado por un rechazo perteneciente a la generación vieja.

### A4 — reentrancia durante rebind

Un provider inicia capture y, dentro de su callback, provoca un rebind e intenta una captura anidada. La captura exterior sigue poseyendo el guard hasta su `finally`; la anidada no puede abrirse sobre el binding nuevo.

### A5 — rebind causal atómico

Con un índice espacial same-tick current, `register(provider, descriptor)` no puede instalar primero un binding descriptorless ni muestrear el provider como fixture. El provider causal se evalúa sólo por la publicación causal necesaria.

### A6 — fixture descriptorless

`register(provider)` sigue siendo un seam fixture válido: descriptor ausente, provider accesible, spatial membership derivada correctamente y generación monotónica.

### A7 — deactivate limpia activo

Tras deactivate no quedan provider/descriptor/quarantine activos del nivel, pero el watermark de generación persiste.

### A8 — weak identity

Dos objetos entidad distintos con ids de red iguales en mundos lógicos distintos no comparten slot ni generación.

### A9 — stale capture/rebind

Una captura iniciada sobre provider/descriptor/generation A no puede publicar un endpoint bajo binding B después de rebind. El fence compara el binding snapshot completo.

### A10 — regresión causal

Los holdouts Q1/S06 de immutable endpoint, unavailable→valid, joint/root serial, rebind y replay siguen verdes sin mover endpoint/root ownership.

### A11 — prepared real geometry

Cow/player exportados y los dos proofs prepared siguen verdes. El owner nuevo no entra en geometry/physics y no puede convertirse en un atajo que eluda los fences existentes.

## 8. Clasificación prevista de fallos

- Fallo del holdout estructural antes del owner: rojo esperado de S14.
- Sample count >1 en causal rebind con precondiciones válidas: bug real del flujo de binding, no fixture.
- Rewind de generación tras deactivate o rebind: bug de ownership/lifecycle.
- Captura anidada que entra después de rebind durante callback: bug de reentrancia.
- Rojo de endpoint/root serial tras la migración: regresión S14, porque esos owners están explícitamente fuera y deben permanecer semánticamente idénticos.
- Rojo prepared en solver independiente: clasificar por owner real antes de modificar S14, como ya ocurrió con A9 durante S13.
