# S26 — G4.2 adversarial model: local prediction authority

Rol activo: **ADVERSARY**.

Estado: **ABIERTO / G4.2 EN REVISIÓN**.

## 1. Scope

G4.2 cierra FR-079: sólo el player local y un vehículo realmente controlado pueden ejecutar prediction física completa en cliente.

No cierra todavía:

- G4.3 observer presentation completa;
- reconciliación/drift G4.4;
- residual visual/cámara G4.6.

El problema de G4.2 es **ownership de simulación**, no estética ni recepción de contacto.

## 2. Fronteras productivas actuales

Producción ya expresa tres barreras distintas:

1. `AnatomyMovement.simulates(body)`:
   `!body.level().isClientSide() || body.isLocalInstanceAuthoritative()`;
2. el loop `END_CLIENT_TICK` sólo llama `AnatomyMovement.carry(entity)` para entidades `isLocalInstanceAuthoritative()` con contacto;
3. `AnatomyClientNetworking.sendMovementReference(body)` exige autoridad local y, para vehículo, que el body sea el root vehicle controlado por el player.

Estas barreras deben permanecer coherentes. Ninguna puede convertirse en una segunda definición más amplia de authority.

## 3. Evidencia positiva ya heredada de S25

El dedicated final S25 `36318320536` demuestra prediction real para los dos actores permitidos:

- player local a RTT 0/100/200 ms;
- controlled boat a RTT 0/100/200 ms.

En las seis fases hay transporte cliente real, references v2 correlacionadas con receipts server-side y cero correcciones dentro de la ventana medida. Esta evidencia cuenta como **positivo de actor autorizado**, no como negativo de observers.

## 4. Negativos obligatorios

### A. Body remoto directo

Un body remoto con contacto material válido puede conservar estado/presentación recibido, pero:

- `AnatomyMovement.simulates(remote)==false`;
- llamar `carry(remote)` no cambia su posición;
- no crea/avanza `TransportLedger` cliente para ese body.

### B. Soporte remoto dentro de carry local

Un player/vehicle local apoyado sobre un body remoto puede predecir su propio carry, pero la recursión no puede mover físicamente el soporte remoto.

### C. Loop cliente

Aunque un body remoto tenga contacto, el loop cliente ordinario no puede invocar carry físico para él. La frontera explícita `isLocalInstanceAuthoritative()` debe permanecer delante de `AnatomyMovement.carry`.

### D. Transición de control

Mount/dismount o replacement no puede dejar dos actores con prediction física simultánea por estado stale. El body autorizado debe derivarse del ownership local actual, no de un cursor/contacto retenido.

## 5. Mutation adequacy mínima

Antes de cerrar G4.2 deben morir mutantes compilables equivalentes a:

1. `AnatomyMovement.simulates(...) -> true` en cliente;
2. eliminar el guard `isLocalInstanceAuthoritative()` del loop de carry cliente;
3. permitir que un reference client-side se emita para una entidad no local/no controlada, si existe un caller capaz de alcanzarla;
4. conservar prediction en el actor anterior tras una transición explícita de control, si el lifecycle permite reproducirlo.

## 6. Relación con el RED observer descubierto en S25

Durante el cierre S25, el assert agregado `remote observer body was not passive client-side` falló después de que las seis fases G4.1 ya hubieran pasado.

Ese assert mezclaba:

- existencia del observer;
- presentation-contact;
- ausencia de transport/carry.

No es evidencia productiva válida por sí sola. S26 debe separar esas precondiciones. Un fallo de presentación pertenece a G4.3; sólo movimiento/transport local de un body remoto reabre G4.2.

## 7. Gate de salida

G4.2 se cierra cuando:

- los positivos local player + controlled vehicle permanecen reales;
- remote direct y recursive carry son read-only;
- el loop cliente conserva ownership local;
- mutation adequacy demuestra que los guards son necesarios;
- control handoff no deja prediction stale, o existe evidencia equivalente de invalidación inmediata.

Hasta entonces, **G4.2 permanece abierto**.
