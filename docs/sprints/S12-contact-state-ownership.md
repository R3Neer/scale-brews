# S12 — Contact state ownership

Estado: **PLAN CERRADO / IMPLEMENTACIÓN PENDIENTE**

## Objetivo

Continuar la tarea 1 de G2 con una partición real de `AnatomyMovement`: extraer el ownership persistente del estado de contacto corporal a un componente dedicado, sin mover todavía el solver, las queries espaciales ni los tipos cuya frontera sigue ligada a Q2/G3.

S12 no abre G3 y no cambia la física aceptada en S05-S11. La meta es que `AnatomyMovement` orqueste validación y aplicación física, pero deje de poseer directamente los mapas de contacto, secuencia, anchor, surface y suspensión.

## Baseline observado

Tras S10/S11, `AnatomyMovement` sigue poseyendo directamente cinco almacenes coherentes como una sola responsabilidad:

- `CONTACTS`;
- `CONTACT_SEQUENCES`;
- `ANCHORS`;
- `SURFACES`;
- `SUSPENDED`.

Ese estado participa en confirmación, retained contact, carry, suspension/local quarantine, lifecycle y enumeración de cuerpos contactados. La física que decide **si** un contacto es válido depende de providers/query/gravity y debe permanecer en `AnatomyMovement`; el almacenamiento que recuerda **qué** contacto/anchor/suspensión está vigente no necesita poseer esas dependencias.

La alternativa de extraer root history se descarta para este sprint: `RootFrame` sigue formando parte de la causalidad de `GeometryProvider.CausalEndpoint` y G3 generalizará `RootTransformProvider`. Moverlo ahora obligaría a fijar una frontera todavía inestable. El estado de contacto, en cambio, lleva estable desde S07-S09 y ya tiene lifecycle y semántica de secuencia probados.

## Frontera arquitectónica cerrada

Se introduce `collision.internal.AnatomyContactState` como owner único, todavía package-private/internal mientras la frontera pública y los tipos físicos terminan de estabilizarse.

`AnatomyContactState` puede poseer exclusivamente:

1. contacto material actual por body;
2. watermark/sequence de contacto por body;
3. anchor físico retenido por body;
4. `SurfaceContact` confirmado por body;
5. generaciones de suspensión body/support;
6. operaciones de snapshot/enumeración y cleanup local/de nivel de esos datos.

No puede:

- consultar `GeometryProvider`;
- ejecutar broadphase, raycast, sweep, separation o carry;
- decidir elegibilidad o gravedad;
- escribir `TransportLedger` o receipts;
- invalidar providers/endpoints;
- llamar de vuelta a `AnatomyMovement`.

La regla de dirección es: **`AnatomyMovement` -> `AnatomyContactState`**, nunca al revés.

## Compatibilidad de tipos

`AnatomyMovement.Contact` se conserva durante S12 como façade/DTO transitorio porque ya tiene consumidores internos y de receipts. El nuevo state puede almacenar una representación interna equivalente y convertir al DTO en el borde. No se permite que `AnatomyContactState` dependa del tipo anidado del orquestador, porque eso convertiría la extracción en un ciclo disfrazado.

`Anchor`, hoy privado dentro de `AnatomyMovement`, sí se mueve al nuevo owner porque no constituye API y sólo describe estado retenido. Su validación geométrica sigue ocurriendo antes de almacenarlo.

## Plan de implementación

- [ ] Añadir holdout estructural rojo registrado que exija `AnatomyContactState` y prohíba `CONTACTS`, `CONTACT_SEQUENCES`, `ANCHORS`, `SURFACES` y `SUSPENDED` dentro de `AnatomyMovement`.
- [ ] Registrar evidencia roja exacta en CI ordinario.
- [ ] Crear `AnatomyContactState` con weak identity keys y sin dependencia hacia `AnatomyMovement`.
- [ ] Mover al state el ownership de contacto y secuencia, preservando idempotencia de sequence para el mismo support/revision/piece.
- [ ] Mover anchor y surface actuales al state.
- [ ] Mover suspension generations al state, manteniendo la decisión física de stale/overlap en `AnatomyMovement`.
- [ ] Sustituir acceso directo en `confirm`, `clear`, `tick`, `deactivate`, `afterMove`, `carry`, `suspend/suspended` y cleanup por operaciones del state.
- [ ] Mantener `AnatomyTransportReceipts.invalidate(...)` en el orquestador: borrar contact state y borrar receipt son owners distintos aunque ocurran en la misma operación pública.
- [ ] Añadir holdouts de comportamiento para sequence estable/incremental, clear coherente, stale suspension tras rebind y cleanup de nivel.
- [ ] Reejecutar la suite ordinaria completa y los holdouts S07-S11.
- [ ] Ejecutar prepared si el diff toca paths cubiertos por esa lane; no ampliar artificialmente el trigger si el cambio es puramente state/storage y la suite ordinaria ya ejerce la integración real relevante.
- [ ] Revisar la extracción adversarialmente: cero mapas duplicados, cero callback `AnatomyContactState -> AnatomyMovement`, cero ownership físico nuevo.
- [ ] Actualizar plan/validation al cerrar S12; G2 seguirá abierto hasta completar las fronteras restantes de las tareas 1 y 9.

## Holdouts adversariales

### A1 — owner dedicado existe

Debe existir `io.github.r3neer.scalebrews.collision.internal.AnatomyContactState`.

### A2 — AnatomyMovement ya no posee contact state

Los campos `CONTACTS`, `CONTACT_SEQUENCES`, `ANCHORS`, `SURFACES` y `SUSPENDED` deben desaparecer de `AnatomyMovement`.

### A3 — sequence idempotente

Reconfirmar la misma identidad material support/revision/piece no avanza `contactSequence`; cambiar esa identidad sí la avanza exactamente una vez.

### A4 — clear coherente

`AnatomyMovement.clear(body)` elimina contacto, surface y anchor retenidos y mantiene la invalidación de receipts en el owner correspondiente.

### A5 — suspensión causal

Una suspensión conserva su binding generation; si el support se rebindea, la generación vieja deja de suspender la nueva identidad. Si el solapamiento que justificaba la suspensión desaparece, se libera localmente.

### A6 — lifecycle por nivel

`deactivate(level)` elimina contact/anchor/surface/suspensions del nivel objetivo sin borrar estado de otro nivel ni rebobinar registration generations.

### A7 — dirección de dependencia

El nuevo state no referencia `AnatomyMovement` como tipo de campo/owner ni ejecuta callbacks al orquestador. El estado puede exponer snapshots/operaciones package-private; la física siempre fluye desde el orquestador hacia el state.

### A8 — regresión física cero

Los holdouts de retained contact, rejected batches, multicontacto, gravity-independence, carry y transport ledger de S07-S11 continúan verdes.

## Criterio de cierre

S12 se cierra sólo cuando:

1. los cinco almacenes dejan de pertenecer a `AnatomyMovement`;
2. existe un único owner de contact state sin duplicación temporal;
3. sequence, clear, suspension y lifecycle conservan su semántica;
4. `AnatomyContactState` no llama al orquestador ni adquiere responsabilidades físicas;
5. la suite ordinaria completa permanece verde sobre el snapshot final;
6. la revisión post-verde no exige cambios adicionales de producción.

S12 reduce la tarea 1 de G2, pero **no autoriza G3** y no declara cerrada la tarea 9 mientras sigan otros tipos/orquestadores realmente acoplados en `collision.internal`.
