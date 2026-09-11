# S10 — Passive transport ledger ownership

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN PENDIENTE**. Sexto sprint de G2.

## 1. Scope

### Tesis

Al terminar S10, la identidad temporal del transporte pasivo de un body —secuencia actual, history acotada, generation de lifecycle y ventana contigua consumible por pose— tendrá un único owner en `collision.runtime`, mientras `AnatomyMovement` conservará únicamente la aplicación física/orquestación del carry y una façade transitoria para callers existentes. La extracción no cambiará displacement, receipts, accounting, contacto ni semántica de lifecycle.

Gate: **G2 — pipeline material continuo Q2**.

### Incluido

- FR-058 como invariante: un carry parcial/rechazado no puede reaparecer como deuda de transporte.
- FR-059 como invariante de secuencias exactly-once en contribuciones derivadas.
- FR-061: transporte pasivo no cuenta como movimiento voluntario/fall/exhaustion/stats.
- NFR-001/002: mismo ledger y orden causal producen la misma ventana.
- NFR-004: hueco, rewind o lifecycle discontinuity fallan cerrado para el consumidor de pose.
- NFR-007: history acotada y outcome `contiguous=false` cuando ya no puede demostrarse continuidad.
- NFR-011: TTL y cap constantes de history de transporte.
- NFR-015/017: mutación física/lifecycle conserva barreras y generation.
- NFR-025: reducir ownership monolítico de `AnatomyMovement` y mover una frontera ya desacoplable a `collision.runtime` sin ciclo hacia `collision.internal`.
- G2 tareas 1 y 9, sólo en la subresponsabilidad **transport ledger/cursor**.

### Excluido

- Aplicación física de carry, block/entity clip, passengers y baselines.
- `AnatomyTransportReceipts`: sigue siendo el ledger causal de receipts, no se fusiona con el cursor local de transporte.
- Contact/surface/anchor/suspension state.
- Provider registration, endpoint/frame publication, root history y spatial index.
- Reorganización completa de `AnatomyMovement`.
- G3 lifecycle/catalog y G4 prediction/reconciliation.

## 2. Estado actual

`AnatomyMovement` posee actualmente:

- `TRANSPORT`: último `SupportTransport` aplicado por body;
- `TRANSPORT_HISTORY`: history necesaria para que `AuthorityPoseTracker` reste todas las contribuciones pasivas ocurridas entre dos samples;
- `TRANSPORT_GENERATIONS`: fence explícito de lifecycle;
- TTL `40` ticks y cap `64` entradas;
- `transport(...)`, `transportGeneration(...)`, `transportSince(...)`, `rememberTransport(...)`, `pruneTransport(...)`, `forgetTransport(...)`;
- invalidación desde `invalidateBody(...)` y limpieza por `deactivate(level)`.

La aplicación del carry está en dos callers reales:

1. `recordCertifiedTransport(...)`, usado por el dispatcher material S08+;
2. `carry(...)`, fallback/client endpoint carry que todavía aplica posición, block clip, passengers, baselines y receipts.

`AuthorityPoseTracker` consume únicamente `transportGeneration` + `transportSince`; no necesita provider, geometry, contact, spatial index ni solver. Por tanto el ledger ya tiene una frontera real separable.

## 3. Estado objetivo

Crear `collision.runtime.TransportLedger` como único owner de:

- current transport por body;
- bounded history;
- lifecycle generation;
- cálculo de ventana contigua desde un cursor consumido.

`TransportLedger` sólo puede depender de JDK/Minecraft + `collision.physics.SupportTransport`; **no puede importar `collision.internal`**.

`AnatomyMovement`:

- delegará temporalmente `transport(...)`, `transportGeneration(...)` y `transportSince(...)` para no obligar a migrar todos los callers en el mismo cambio;
- usará el ledger para registrar contribuciones e invalidar lifecycle;
- seguirá siendo owner de la aplicación física del carry en este sprint.

`AuthorityPoseTracker` debe migrar al ledger directamente: es el consumidor que demuestra que la frontera es real y no un helper que sólo se prueba a sí mismo.

## 4. Plan de implementación

- [ ] I1 Crear `collision.runtime.TransportLedger` con weak identity keys, current transport, history, generation y `Window` inmutable.
- [ ] I2 Mover al ledger TTL/cap, `current`, `generation`, `since`, `record`, `invalidate` y cleanup por level sin cambiar algoritmos ni valores.
- [ ] I3 Migrar `AuthorityPoseTracker` para consumir `TransportLedger` directamente.
- [ ] I4 Sustituir en `AnatomyMovement` los mapas/helpers de ledger por delegación; `recordCertifiedTransport` y `carry` registran por el ledger.
- [ ] I5 Hacer que `invalidateBody` y `deactivate(level)` deleguen lifecycle al ledger; liberar contacto ordinario no borra history ya aplicada.
- [ ] I6 Mantener façade transitoria de `AnatomyMovement.transport(...)` sólo si sigue teniendo callers reales; eliminar cualquier wrapper que quede sin consumidor.
- [ ] I7 Añadir tests adversariales directos de continuidad, gap/rewind, lifecycle generation, TTL/cap y weak-identity/network-id reuse donde el harness lo permita.
- [ ] I8 Ejecutar regresiones S08/S09 de carry, pose accounting, receipts, chains y ordinary suite; revisar imports/ciclos y diff completo.
- [ ] I9 Pasada final sin cambios de producción y registrar evidencia.

### Revisiones del plan

- P1 requisitos: la extracción preserva FR-058/059/061 y ataca NFR-011/025; no cambia comportamiento físico.
- P2 ownership: ledger posee sólo identidad/history/cursor; `AnatomyMovement` conserva aplicación física. No se crea segundo carry engine.
- P3 consumidores: `AuthorityPoseTracker` es consumidor directo real; carry/dispatcher producen entradas mediante `AnatomyMovement`.
- P4 lifecycle: `clear(contact)` no equivale a lifecycle; `invalidateBody(..., discardTransport)` sí incrementa generation y puede descartar current/history.
- P5 boundedness: TTL=40 y cap=64 se conservan exactamente; history insuficiente devuelve ventana no contigua.
- P6 identidad: weak object identity se conserva; network/entity id igual no puede aliasar ledger.
- P7 dependencias: `collision.runtime.TransportLedger` no importa `collision.internal`; runtime→physics es válido y no forma ciclo de capa.
- P8 simplicidad: no se extrae receipts, contacto, provider ni carry application porque introducirían callbacks/ciclos sin necesidad.

**Convergencia del plan:** una pasada completa P1-P8 no requiere ampliar scope. La primera frontera estable es el ledger/cursor, no el movimiento físico completo.

## 5. Modelo adversarial previo

### A1 — contacto liberado antes del siguiente pose sample

Se aplica carry, después se libera el contacto y sólo entonces corre `AuthorityPoseTracker`. El delta pasivo ya aplicado debe seguir en history para no convertirse en locomoción voluntaria.

### A2 — múltiples contribuciones entre samples

Dos o más `SupportTransport` contiguos deben sumarse exactamente una vez y avanzar el cursor al último sequence.

### A3 — sequence gap

Si el cursor necesita una secuencia que ya no está en history o existe un hueco, `since(...)` debe devolver `contiguous=false` y delta cero; nunca aproximar la suma restante.

### A4 — cursor futuro / rewind

`consumedSequence > latest` debe fallar cerrado; registrar una secuencia no creciente no puede fabricar continuidad vieja.

### A5 — lifecycle generation

Teleport/removal/discontinuity incrementa generation. Aunque posición y sequence parezcan compatibles, `AuthorityPoseTracker` debe resetear locomotion al observar otra generation.

### A6 — release ordinario frente a discard

`clear(contact)` no borra transport history ya aplicada; invalidación con `discardTransport=true` sí elimina current/history además de avanzar generation.

### A7 — TTL y cap

Tras superar 40 ticks o 64 entradas, memoria se estabiliza. Un cursor que necesite una entrada podada obtiene `contiguous=false`.

### A8 — identity reuse

Dos objetos Entity distintos con mismo network id no comparten current/history/generation.

### A9 — passive accounting

La extracción no puede alterar velocity, fallDistance, exhaustion ni stats; `S08PassiveTransportStateTests` permanece verde.

### A10 — receipts y exactly-once

Mover ownership del ledger no duplica ni pierde `AnatomyTransportReceipts`, transport sequences ni ancestry de DERIVED_CARRY.

### A11 — layer mutation

Sustituir accidentalmente weak identity por equality/UUID, quitar generation o hacer que `TransportLedger` importe `collision.internal` debe quedar detectado por tests/inspección estructural.

## 6. Holdouts reservados

Para la segunda pasada adversarial se reservan escenarios concretos de:

- cursor justo en el borde TTL/cap y `+1`;
- release entre dos contribuciones contiguas;
- dos bodies con id de red reutilizado;
- deactivation de un level sin borrar ledger de otro level.

No se implementará para fixtures concretos antes de revelar estos holdouts.
