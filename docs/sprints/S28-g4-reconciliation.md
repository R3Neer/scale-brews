# S28 — G4.4 adversarial model: reconciliation without double-apply or drift

Rol activo: **ADVERSARY**.

Estado: **ABIERTO / G4.4 EN REVISIÓN**.

## 1. Scope

G4.4 cierra FR-083: la prediction local debe reconciliarse contra autoridad sin aplicar dos veces transportes confirmados, sin drift creciente y sin correction loop.

No redefine G4.1 references ni G4.2 ownership. Consume sus owners ya cerrados:

- `AnatomyTransportReceipts` / `AnatomyMovementReference` correlacionan un movement packet con transportes server-issued;
- `PlatformConnectionMixin.scalebrews$transportBaseline(...)` adelanta los baselines vanilla cuando el servidor aplica carry;
- `AnatomyMovementReference.resolve(...)` sólo agrega transporte server-side posterior al receipt consumido.

## 2. Evidencia positiva heredada

S25 dedicated final ya recorre player local + controlled boat a 0/100/200 ms con `allow-flight=false`, references v2 ordenadas, receipts reales consumidos y cero correcciones vanilla dentro de la ventana medida.

Eso es baseline de reconciliación, pero no mutation adequacy específica de G4.4.

## 3. Hipótesis adversarial

El sistema sólo es correcto si **ambas** piezas son necesarias y no duplican ownership:

1. carry server-side desplaza una vez los baselines vanilla;
2. un movement packet retrasado sólo recibe el delta server-side ocurrido **después** del receipt que el cliente ya incorporó.

Corromper cualquiera debe ser observable como corrección vanilla, divergencia client/server o movimiento rebasado incorrectamente.

## 4. Mutation adequacy mínima

### A. baseline-double

Mutar `scalebrews$transportBaseline(body,d)` para avanzar `firstGood/lastGood` o `vehicleFirstGood/vehicleLastGood` dos veces por el mismo `d`.

El dedicated 0/100/200 debe fallar. Si sobrevive, la prueba no detecta drift/baseline corruption.

### B. reference-double

Mutar `AnatomyMovementReference.resolve(...)` para sumar, además de `TransportLedger.since(receiptSequence).appliedDelta()`, el `appliedDelta` del transporte actual/receipt ya incorporado.

El dedicated debe fallar por double-apply.

### C. baseline-missing (posterior si hace falta)

Omitir el ajuste vanilla del carry debe producir correcciones bajo alguna fase RTT. Sólo se añade si A/B no demuestran suficientemente la dependencia causal.

## 5. Gate de salida

G4.4 sólo puede cerrarse si:

- baseline real player+controlled boat 0/100/200 permanece verde;
- no hay correcciones, drift creciente ni trace incompleta;
- mutantes baseline-double y reference-double compilan y mueren;
- ordinary permanece verde;
- zero-change confirma que el ADVERSARY no reparó producción.

Hasta entonces task 4 permanece abierta.
