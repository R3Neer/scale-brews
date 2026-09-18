# S25 — G4.1 adversarial model: transport references from confirmed receipts

Rol activo: **ADVERSARY**.

Estado: **BASELINE RED / CAPACIDAD G4.1 AUSENTE**.

Este sprint abre G4 después del cierre completo de G3. No prescribe una clase, un nombre de payload ni una estructura de paquetes concreta. Fija las propiedades que cualquier implementación de movement reference/rebase debe demostrar.

## 1. Requisitos congelados

Principalmente:

- **FR-077** — el servidor sigue siendo autoridad de transporte confirmado;
- **FR-078** — C2S no puede subir geometría, matrices de pose ni contactos;
- **FR-080** — toda materialización/referencia debe respetar dimensión, epoch, revision, UUID/network id y generaciones pertinentes;
- **FR-084** — movement references/baselines sólo pueden correlacionarse con transportes realmente aplicados, consumirse una vez y permanecer temporalmente acotados;
- **FR-085** — sólo contacto físicamente confirmado puede justificar support usado por movement/flying checks;
- **NFR-030/031** — la aceptación debe quedar estratificada y reproducible en CI.

## 2. Estado productivo observado al abrir G4

El árbol ya contiene una buena mitad server-side del contrato:

- `AnatomyTransportReceipts` registra únicamente transporte ya aplicado y baselined;
- receipt identity incluye epoch, catalog revision, dimension, body network id/UUID, tracking generation, support/contact identity, root/transport sequence y tick;
- retención está acotada a `HISTORY_TICKS=40`;
- por body/tick hay `MAX_RECEIPTS_PER_TICK=16`;
- saturación queda registrada para que una futura referencia de ese tick falle cerrada;
- disconnect/invalidate/clear retiran histories.

Pero **no existe todavía la otra mitad**.

Evidencia de ausencia actual:

1. `AnatomyTransportReceipts.history(...)` se documenta como snapshot para un **“eventual metadata-only C2S reference validator”**.
2. El C2S de movement que sí existe es legacy: `PlatformMovePayload` registrado por `PlatformNetworking`.
3. Ese receiver sale inmediatamente cuando `AnatomyApi.ownsSharedPhysics(body)`; por diseño no presta autoridad legacy al motor anatómico.
4. `PlatformMovementReference.resolve(...)` también limpia cualquier reference legacy y devuelve el movimiento absoluto cuando anatomy posee shared physics.
5. `AnatomyPredictionBaselineProof` declara expresamente que su medición N2 tiene **“no anatomy C2S reference, rollback or replay”**.

**Clasificación:** no es una regresión; es el RED de entrada esperado de G4.1. G3 está cerrado y esta capacidad aún no se ha implementado.

## 3. Modelo de autoridad

Una referencia C2S válida **no es** una prueba de contacto ni una orden de carry. Es sólo una clave/metadata suficiente para que el servidor localice un receipt que **él mismo ya emitió** para ese recipient/body y decida si ese transporte confirmado puede usarse como baseline/rebase de un input vanilla.

La referencia nunca puede:

- crear un `SurfaceContact`;
- registrar geometry/provider;
- cambiar support;
- aplicar `appliedDelta` de nuevo;
- alterar root/contact sequence;
- crear una tracking window;
- transportar matrices, convexas, local points/normals de contacto ni pose channels como autoridad cliente.

El receipt es la autoridad. El C2S únicamente lo nombra.

## 4. Holdouts obligatorios del primer corte

### A. Happy path exacto

Dado un receipt server-issued no consumido dentro de TTL y perteneciente a la ventana tracking vigente:

- una referencia metadata-only lo localiza;
- se valida contra recipient, body y lifecycle actuales;
- produce exactamente el baseline/rebase permitido por el contrato;
- queda consumida una vez.

El test debe comparar contra el receipt exacto, no contra geometry cliente.

### B. Exactly-once

Reenviar la misma referencia después de su primera aceptación debe fallar cerrado.

No vale que el segundo envío sea idempotente “por casualidad”: debe existir evidencia de que la referencia/receipt ya no es admisible.

### C. TTL y saturación

Deben rechazarse:

- receipt anterior a la ventana `HISTORY_TICKS`;
- referencia a un body/tick marcado como saturated;
- referencia a un transport sequence inexistente dentro de un tick real.

### D. Lifecycle cross-product

El mismo token/referencia debe fallar si cambia cualquiera de los ejes relevantes:

- epoch;
- catalog revision;
- dimension;
- body UUID;
- body network id;
- recipient/connection identity;
- tracking generation;
- binding/contact provenance que forme parte de la identidad elegida por el diseño.

Network-id reuse y reconnect no pueden legitimar un receipt anterior.

### E. Negative authority / cheat

Una referencia plausible pero sin receipt server-issued debe dejar invariantes:

- contacto;
- support;
- position/AABB;
- root/endpoint state;
- tracking ledgers;
- receipt metrics salvo telemetría de rechazo permitida.

El cliente no puede “adivinar” un soporte y conseguir que el servidor lo materialice.

### F. Controlled vehicle

El mismo mecanismo debe distinguir player local de vehículo realmente controlado sin permitir referencias a una tercera entidad observada.

El server debe derivar el body permitido desde `context.player()` y control authority, no confiar en un arbitrary body id enviado por cliente.

### G. Bound y schema

El payload C2S debe ser:

- versionado;
- bounded;
- metadata-only;
- sin geometry/matrices/pose/contact DTO;
- con rate/budget explícito si el diseño permite más de una referencia por tick.

## 5. Mutation adequacy mínima

Una implementación no puede cerrar G4.1 sin matar, como mínimo, mutantes equivalentes a:

1. aceptar la misma referencia dos veces;
2. omitir tracking-generation validation;
3. omitir dimension/epoch/revision fence;
4. admitir receipt expirado;
5. ignorar saturated tick;
6. aceptar body no controlado;
7. aplicar el transport delta una segunda vez;
8. fabricar un receipt/reference hit cuando no existe entrada server-side.

Los mutantes deben compilar antes de ejecutar el holdout.

## 6. Delimitación con G4.2+

Este corte **no** exige todavía prediction visual completa ni reconciliación suave. Primero se cierra la autoridad de references.

G4.2–G4.7 podrán consumir esa primitive; no deben redefinirla ni crear una segunda fuente de verdad.

## 7. Gate de salida

G4.1 sólo puede marcarse cerrado cuando exista un camino C2S real y bounded que:

- correlacione exclusivamente receipts server-issued;
- sea exactly-once + TTL/lifecycle fenced;
- no transporte ni cree autoridad física cliente;
- quede cubierto por happy path + negativos + mutation-kills.

Hasta entonces, **G4.1 permanece RED por capacidad ausente**.
