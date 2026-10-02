# S28 — G4.4 adversarial model: reconciliation without double-apply or drift

Rol activo: **ADVERSARY**.

Estado: **CERRADO ADVERSARIALMENTE / G4.4 RECERTIFICADO SIN CAMBIO PRODUCTIVO**.

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

## 6. Cierre adversarial final

S28/G4.4 se cierra sin modificar producción.

Workflow final `s28-adversarial-reconciliation`, run **`36987926148`** sobre `95c1c5064b5f5fab82276f5f568e92046bd6a362`:

- `reconciliation-baseline`, job **`110777096130`** — success;
- `baseline-owner`, job **`110777096206`** — success;
- `reference-double-mutant-must-die`, job **`110778159564`** — el mutante compiló y murió;
- `baseline-double-mutant-must-die`, job **`110778159616`** — el mutante compiló y murió.

El owner-level `S28BaselineOwnershipTests` mide directamente los cuatro baselines vanilla:

- player `firstGood`;
- player `lastGood`;
- vehicle `vehicleFirstGood`;
- vehicle `vehicleLastGood`.

Un carry confirmado debe adelantar cada uno exactamente una vez por el mismo `Vec3`. El mutante que duplica `d` compila y el oracle exacto lo mata.

El mutante de reference altera `AnatomyMovementReference.resolve(...)` para sumar de nuevo `TransportLedger.current(body).appliedDelta()` encima de `TransportLedger.since(receiptSequence).appliedDelta()`. El dedicated real player + controlled boat 0/100/200 ms lo mata, demostrando que double-apply produce una desviación observable y que la prueba de reconciliación no es decorativa.

Artifacts:

- `S28-reconciliation-baseline` id **`11218208110`**, SHA-256 **`ef4715b7512b6d8e5e9d264bc399e2582e335bad62852c20ad2b37ea65b1d8f8`**;
- `S28-baseline-owner` id **`11218297367`**, SHA-256 **`773e12f1b30b8df92bce6dc2941b8415c1f150f50e77463d9b0839bca24e4c8f`**.

Ordinary del mismo snapshot, run **`36987926156`**, job **`110777096416`** — success; artifact **`11217304592`**, SHA-256 **`a35fc8cb56598745b5e18f724cb867313d6d52314cff3be09f15152381c2458e`**.

Zero-change: compare `94a0a9b3834ae4a65c4685729e08c9bf0501fa29 → 95c1c5064b5f5fab82276f5f568e92046bd6a362` no contiene cambios bajo `src/main` ni `src/client`; sólo tests, CI y evidencia. La reapertura localizada S27 posterior tampoco cambió este owner de reconciliación y ya quedó recertificada independientemente.

**Conclusión ADVERSARY:** G4.4 queda cerrado. El sistema demuestra dependencia causal de ambos owners sin double-apply ni drift: baseline vanilla se desplaza exactamente una vez al aplicar carry server-side y el movement packet retrasado sólo incorpora transporte server-side posterior al receipt consumido. El siguiente gate es **G4.5: presentación CURRENT_ENDPOINT → intervalo certificado donde Q2 lo requiera**.

