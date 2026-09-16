# S24 — adversarial reconnect bootstrap audit

Rol activo: **ADVERSARY**.

Estado: **PROOF IMPLEMENTER INVALIDADO COMO HARNESS / RECONNECT REAL EN RECERTIFICACIÓN ADVERSARIAL**.

## 1. Candidato implementer

`S24ReconnectLifecycleClientProof` intenta demostrar una desconexión/reconexión real contra el mismo dedicated server:

1. arrancar runtime preparado con una vaca canónica;
2. conectar cliente y alcanzar `READY` + pose + `presentationFrame`;
3. cerrar la primera conexión;
4. comprobar que catálogo/pose de la conexión vieja desaparecen;
5. reconectar al mismo servidor y exigir mismo epoch/revision/binding y autoridad de tracking independiente para el nuevo receptor.

El diseño causal es adecuado para FR-080/FR-082/NFR-017, pero el fixture original introduce una dependencia ajena a reconnect: crea el soporte antes de que exista ningún jugador/conexión en el dedicated test world.

## 2. Rojos originales

Primer workflow implementer `s24-client-reconnect-lifecycle`, run **`35120074913`**, job **`104875319948`**: **failure** en el primer `awaitReady`, antes de la primera desconexión.

Tras retirar la asunción excesiva de que una conexión nueva deba recibir exactamente `trackingGeneration=1`, el rerun **`35120983007`**, job **`104878391797`**, volvió a fallar en el mismo primer `awaitReady`. Por tanto el ajuste de generation scope era correcto, pero no explicaba el rojo.

Los builds generales correspondientes fueron verdes; eso no convierte una lane focal roja en evidencia de reconnect.

## 3. Diagnóstico por etapas

El adversario añadió `S24AdversarialReconnectBootstrapDiagnostics` y el workflow `s24-adversarial-reconnect-bootstrap-diagnostic` para separar:

- conexión cliente;
- existencia del `ServerPlayer` y del soporte;
- frame autoritativo server;
- negociación de canales catálogo/pose/contacto;
- `PlayerLookup.tracking`;
- catálogo announced/READY;
- entidad visible en cliente;
- pose history;
- presentation frame.

Run **`35121576945`**, job **`104880398209`**: **failure** con diagnóstico terminal exacto:

```text
BOOTSTRAP_STAGE server-cow: canonical cow missing before publication
```

La conexión cliente ya existía y `Player0` había entrado, pero la vaca que el fixture creó **antes** de conectar ya no estaba en el `ServerLevel`. El test abortó antes de comprobar canales, tracking, catálogo, pose o presentación.

Los warnings/errores headless de narrator/OpenAL/servicios externos del log no son el fallo terminal.

## 4. Clasificación adversarial

Los rojos implementer anteriores **no prueban un bug de producción en reconnect**. Prueban que el laboratorio mezclaba reconnect anatómico con lifecycle de entidad/chunk en un dedicated server todavía sin jugadores.

Modificar `AnatomyRuntime`, networking o causal fencing para hacer verde ese escenario habría sido una reparación imaginaria.

## 5. Recertificación limpia

Se creó `S24AdversarialReconnectLifecycleProof` con una separación explícita de responsabilidades:

1. arrancar dedicated server;
2. conectar primero el cliente;
3. sólo entonces crear el soporte cerca del `ServerPlayer`;
4. marcarlo persistente y mantener su chunk forzado durante el hueco entre conexiones;
5. arrancar `AnatomyRuntime.startPrepared(...)` con el receptor ya presente;
6. demostrar primera sesión READY;
7. desconectar y exigir borrado de catálogo/pose client-side;
8. comprobar que el soporte server-side sigue siendo exactamente la misma entidad;
9. reconectar y exigir mismo epoch/revision/binding, nueva vida de `ServerPlayer` y un `AnatomyFrameHistory` cliente nuevo.

Este proof mide reconnect en lugar de la supervivencia accidental del soporte antes de la primera conexión. Su evidencia ejecutada se añadirá sólo cuando CI finalice.

## 6. Factura de aceptación

Reconnect permanece abierto hasta que:

1. el proof dedicado limpio alcance la primera sesión READY de forma reproducible;
2. ejecute realmente disconnect + segunda conexión;
3. la segunda conexión no herede catálogo/frames/contactos/receipts de la primera;
4. el mismo server/binding pueda conservar epoch/revision/binding cuando corresponda sin conservar autoridad del receptor muerto;
5. una campaña mutation-kill demuestre sensibilidad al menos a conservar indebidamente catálogo y pose/history cliente al desconectar.

El cleanup server-side por receptor se evaluará con mutantes sólo si afectan autoridad de la segunda conexión; no se añadirán mutantes decorativos que sobrevivan o mueran por GC de weak keys sin cambiar semántica observable.
