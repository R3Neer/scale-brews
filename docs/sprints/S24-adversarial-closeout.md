# S24 — adversarial closeout G3.9–G3.12

Rol activo: **ADVERSARY**.

Estado: **CERRADO / G3.9–G3.12 RECERTIFICADOS**.

Este documento cierra la revisión independiente del candidato S24. El último cambio productivo propio de este tramo es `99969ccb4e44b86c28f6898b62c4752781679949`. Los cambios posteriores de la campaña final son GameTests, mixins de test, workflows, gates estructurales y documentación; no modifican `src/main` ni `src/client`.

## 1. G3.9 — lifecycle y tracking authority

La reapertura adversarial real fue `AnatomyRuntime.trackingGeneration(recipient, body)`: una façade descrita como observación podía crear una ventana mediante `TrackingGenerationLedger.acquire(...)` y además fabricaba generación `1` cuando no había runtime.

Reparaciones productivas:

- `256ab44b434af4d7cfae2da37bda7dc873444e58` — separa observación de adquisición;
- `99969ccb4e44b86c28f6898b62c4752781679949` — reconstruye explícitamente tracking ya existente al arrancar/resetear runtime, sin volver a esconder adquisición en el getter.

Mutation adequacy del rojo: run **`35333686648`**:

- baseline `tracking-read-purity`, job **`105563538619`** — success;
- `read-acquires-mutant-must-die`, job **`105563913730`** — mutante compiló y murió;
- `no-runtime-default-mutant-must-die`, job **`105563913814`** — mutante compiló y murió.

Artifact baseline **`10541553565`**, SHA-256 **`68706200ef77786cb2ee99ebb3c2064c8fb4295f1cbab62d46be3aa1a8671062`**.

El resto del lifecycle permanece mutation-protected:

- dimensión: run **`35329062829`**, baseline + tracking-generation mutant + catalog-scope mutant verdes;
- reconnect: run **`35329603429`**, baseline + stale-history mutant + stale-catalog mutant verdes; artifact `10540477049`, SHA-256 `3931790fc7ba671eb22cbc5887f7e7b3ea4442d4326a2a6b85ad6137e3c57289`;
- replay de pose/contacto y rebind de soporte permanecen verdes en la línea integrada de `99969cc`;
- tracking ledger conserva bound y no reutilización de generaciones;
- reload transaccional y player-list bootstrap permanecen verdes.

Los receipt GameTests que fallaron inmediatamente tras hacer pura la lectura fueron clasificados como **TEST/EVIDENCIA**: habían usado accidentalmente el getter impuro como fixture. Se migraron a un seam exclusivo de GameTest, opt-in y action-scoped; producción no recuperó autoridad sintética.

## 2. G3.10 — UNAVAILABLE real y recuperación

El proof cliente fue endurecido para exigir un **endpoint de red explícito** `available=false`, con `frameSerial` mayor que el AVAILABLE previo y dentro de una ventana menor que el TTL. La desaparición de presentation/geometry ya no puede obtener verde esperando a que expire un frame viejo.

Run final **`35335621883`**:

- baseline `unavailable-recovery`, job **`105569661575`** — success;
- `recovery-freeze-mutant-must-die`, job **`105570042027`** — mutante compiló y murió;
- `unavailable-transition-mutant-must-die`, job **`105570042132`** — mutante compiló y murió.

Artifact baseline **`10543506605`**, SHA-256 **`68a628ad45fa44a0fc94b248e69a2472b691d5e45a564e70cd735a7d7943a5db`**.

Queda demostrado `AVAILABLE → UNAVAILABLE → AVAILABLE` con epoch/revision/binding/tracking estables y seriales causales estrictamente progresivos.

## 3. G3.11 — late tracking y orden de packets

El client proof integrado demuestra que un observador que empieza a trackear tarde recibe directamente el endpoint vigente, no replay histórico.

La primera mutación de `AnatomyFrameHistory.accept(...)` sobrevivió al proof integrado porque ese proof no era un oracle owner-level adecuado para los tres clocks. La campaña final separó responsabilidades:

- client proof real para START_TRACKING/receiver/presentación;
- `S24AdversarialFrameOrderTests` como oracle exacto del owner de orden.

Run final **`35336440655`**:

- `same-window-ordering-owner`, job **`105572251990`** — success;
- `late-tracking-order`, job **`105572252178`** — success;
- `stale-order-mutant-must-die`, job **`105572742720`** — mutante compiló y murió al eliminar simultáneamente los fences de frame serial, authority tick y joint sample tick.

Artifacts:

- client real **`10543522907`**, SHA-256 **`6602a57e3985d4d8c252f932716704da3819a56507a1a5695618959664c787c0`**;
- owner baseline **`10543032562`**, SHA-256 **`6eb6acd6352d49f6caf71c1e0f89e9c63d01e3280ac6791936a35b3ff11ad2f5`**;
- owner mutant **`10542979062`**, SHA-256 **`3d9e53d9f64bc4b0c218ceb009b3e78c47bddba521cdfbb3933199acc250b8de`**.

El owner holdout queda además registrado permanentemente en la suite ordinaria.

## 4. G3.12 — ownership root/endpoint

La extracción estructural queda protegida por `tools/s24_ownership_gate.py`: `AnatomyMovement` puede consumir `RootFrame`/endpoints, pero no reintroducir mapas persistentes propietarios de root history o endpoint serials.

Run **`35333447075`**:

- `ownership-boundary`, job **`105562783643`** — success;
- duplicate endpoint owner mutant, job **`105562809519`** — mutante compiló y murió;
- duplicate root owner mutant, job **`105562809667`** — mutante compiló y murió.

`RootFrameLedger` conserva el ownership de provenance root y `AnatomyEndpointLedger` el endpoint state. No queda segundo owner dentro del orquestador.

## 5. Ordinary y zero-change

Ordinary final sobre el árbol con el owner-order holdout registrado:

- run **`35336440635`**, job **`105572251864`**;
- **442/442 required GameTests passed**;
- `BUILD SUCCESSFUL`;
- artifact **`10543418166`**, SHA-256 **`36ab2120935b3339753130f43a80e3771d785f4526b0a732e934c0d65eea5579`**.

Comparación `99969cc... → 8dcbb615...`: treinta commits posteriores y **cero cambios en `src/main` o `src/client`**. La segunda lectura adversarial no requirió reparación productiva posterior.

## 6. Veredicto

- G3.9: **cerrado**;
- G3.10: **cerrado**;
- G3.11: **cerrado**;
- G3.12: **cerrado**.

Con las tareas 1–8 ya cerradas previamente, **G3 queda cerrado por completo**.

El siguiente gate canónico es **G4 — red, prediction, reconciliación y presentación causal**. La primera deuda visible es G4.1: existen receipts server-side, pero todavía no existe el metadata-only C2S anatomy reference/validator que los consuma.
