# S24 — adversarial tracking-generation retention bound

Rol activo: **ADVERSARY**.

Estado: **BOUND LEVANTADO ADVERSARIALMENTE / INTEGRATION-REVIVAL SEAM AÚN ABIERTO**.

## 1. Hallazgo original

S24 introdujo inicialmente un ledger server-side por receptor equivalente a:

```text
ServerPlayer -> (support UUID -> trackingGeneration)
```

con crecimiento monotónico durante toda la conexión. `STOP_TRACKING` conservaba una entrada por cada UUID retirada y sólo reload/disconnect vaciaban el mapa. Eso preservaba replay fencing a costa de violar NFR-011 bajo sesiones largas.

## 2. Reparación implementer

Producción sustituyó ese mapa interior por `TrackingGenerationLedger`:

- `MAX_ACTIVE = 4096` ventanas simultáneamente activas por receptor;
- `UNAVAILABLE = 0` como outcome fail-closed;
- mapa activo `UUID -> generation` acotado;
- contador escalar `nextGeneration` monotónico por ledger/conexión;
- `release(UUID)` elimina la UUID retirada del mapa activo sin rebobinar el contador;
- al saturarse el mapa o agotarse el contador, `acquire` devuelve `UNAVAILABLE` y no amplía memoria.

El ledger quedó conectado a `AnatomyRuntime`: START adquiere una ventana; STOP la libera; pose/contact/receipts no deben emitirse sin generación válida.

## 3. Campaña adversarial

Holdout: `S24AdversarialTrackingGenerationLedgerTests`, commit `49b02b9ad301757b507b494be3d0745dc7a76224`.

Workflow adversarial, inicialmente creado en `e5dd89b76cb6cfd4a3f6406d3d46ad64c5047238` y reejecutado tras el cableado runtime en `3d06e663aeeaf8c2730933bbcc035a36b79a52fb`.

Se atacaron dos propiedades distintas:

1. **generation reuse mutant**: `release(...)` reinicia indebidamente el contador a `1`, permitiendo reutilizar una generación retirada;
2. **cap off-by-one mutant**: cambia `>= MAX_ACTIVE` por `> MAX_ACTIVE`, permitiendo una ventana activa adicional.

Ambos mutantes compilaron antes de ejecutarse contra el holdout.

### Evidencia

Run adversarial **`35121129121`**:

- `bounded-ledger-holdout`, job **`104878892384`**: **success**;
- `generation-reuse-mutation-kill`, job **`104878892076`**: **success**, mutante compiló y murió;
- `cap-off-by-one-mutation-kill`, job **`104878892457`**: **success**, mutante compiló y murió.

Build general del mismo snapshot: run **`35121128960`**: **success**.

## 4. Propiedades demostradas

Queda demostrado adversarialmente que:

- churn de UUID retiradas no obliga a retener una entrada por UUID para siempre;
- una UUID retirada puede desaparecer del mapa activo sin que su número de generación vuelva a reutilizarse dentro del ledger;
- el número de entradas activas está acotado exactamente por `MAX_ACTIVE`;
- saturación falla cerrada sin ampliar el mapa;
- liberar una ventana recupera capacidad sin reciclar generaciones anteriores.

Esto levanta el blocker original de **memoria estable vs replay fencing** de NFR-011 a nivel del kernel de tracking-generation y su cableado básico en runtime.

## 5. Costura que sigue abierta

Este cierre **no** certifica todavía todo el lifecycle S24. En particular, la API pública/interna `AnatomyRuntime.trackingGeneration(...)` debe auditarse porque actualmente puede adquirir una ventana al ser consultada, no sólo observar la autoridad ya existente.

Eso importa después de `STOP_TRACKING`: si un consumer tardío pudiera llamar a una lectura con efectos laterales y recrear una ventana ya retirada, el sistema conservaría memoria acotada pero violaría FR-080/FR-082/NFR-017 por revival de autoridad. La reparación correcta puede ser separar adquisición de consulta o cualquier diseño equivalente; el adversario no prescribe la arquitectura.

Además permanecen abiertos reconnect, replay explícito A→B→A y reload/barriers completos.
