# S26 — G4.2 adversarial model: local prediction authority

Rol activo: **ADVERSARY**.

Estado: **CERRADO / G4.2 RECERTIFICADO**.

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

Ese gate queda satisfecho por la recertificación final de §8.

## 8. Cierre adversarial y fixes productivos

El handoff de control reveló que `isLocalInstanceAuthoritative()` por sí solo era demasiado amplio para expresar **prediction física completa**: un `LocalPlayer` montado sigue siendo una réplica local autoritativa, pero el actor físico único debe ser su root vehicle controlado.

El IMPLEMENTADOR aterrizó tres cambios productivos coherentes:

- `df3d9875842085169e7c879a958b1188414713aa` — `AnatomyMovement.predictsBody(...)` restringe prediction completa a un único root actor; `carry(...)` y `edge(...)` usan esa autoridad.
- `2f73966390c020f77c904e2601cb5a8dfba06dbf` — `AnatomyClientNetworking` mantiene un único `predictionOwner` derivado del control vivo y, en mount/dismount/replacement, retira references y receipt tokens de ambos lados del handoff.
- `ad97efb3927b2289ea60fcf5bb2037e5ee1fc7f5` — el bridge `PlatformPhysics` deja de consultar la autoridad amplia `simulates(...)` y usa `predictsBody(...)` en suppress/collide/afterMove/carry.

El ADVERSARY endureció después el harness y los mutantes. La recertificación final es workflow `s26-adversarial-prediction-authority`, run **`36400355085`**:

- `control-handoff-baseline` job **`108856467859`** — success;
- `remote-observer-baseline` job **`108856468387`** — success;
- `simulate-all-mutant-must-die` **`108856986314`** — success;
- `tick-loop-guard-mutant-must-die` **`108856986340`** — success;
- `reference-local-authority-mutant-must-die` **`108856986378`** — success;
- `bridge-root-owner-mutant-must-die` **`108856986431`** — success;
- `stale-metadata-handoff-mutant-must-die` **`108857082946`** — success;
- `duplicate-root-actor-mutant-must-die` **`108857082947`** — success;
- `stale-vehicle-authority-mutant-must-die` **`108857082996`** — success.

Artifacts: `S26-control-handoff` **`10960180780`** y `S26-prediction-authority` **`10959951365`**.

Build del mismo snapshot: run **`36400355089`** — success. La evidencia positiva dedicada player + controlled boat se volvió a ejecutar después de los cambios de bridge en run **`36400806938`**, también success.

Desde el último cambio productivo (`ad97efb...`) hasta la recertificación final sólo hubo tests/CI, por lo que la pasada zero-change requerida queda satisfecha.

### Resultado

G4.2 queda **CERRADO**: sólo existe un actor de prediction física completa en cliente, derivado del root/control actual; observers y soportes remotos siguen read-only; el handoff retira metadata stale; y los guards relevantes son mutation-sensitive.

El siguiente gate es **G4.3: observer path sin carry local**. Ese gate trata presentación/estado observado, no vuelve a abrir ownership físico salvo evidencia nueva.

## 8. Cierre adversarial final

La campaña S26 terminó separando dos conceptos que inicialmente podían solaparse: una réplica cliente puede ser localmente autoritativa para networking/rendering y, aun así, **sólo un root actor actual** puede poseer full prediction física.

Reparaciones productivas revisadas:

- `df3d9875842085169e7c879a958b1188414713aa` — `fix(s26): fence prediction to the current root actor`;
- `2f73966390c020f77c904e2601cb5a8dfba06dbf` — `fix(s26): retire prediction metadata on control handoff`;
- `ad97efb3927b2289ea60fcf5bb2037e5ee1fc7f5` — `fix(s26): use root prediction authority across bridge physics`.

### 8.1 Real-client authority boundaries

Final workflow `s26-adversarial-prediction-authority`, run **`36400355085`**:

- `remote-observer-baseline`, job **`108856468387`** — success;
- `control-handoff-baseline`, job **`108856467859`** — success.

El observer holdout demuestra dos negativos independientes:

1. un body remoto con contacto material válido no se mueve ni crea transporte al invocar carry directamente;
2. un player local puede predecir su propio carry sobre un soporte remoto sin mover físicamente ese soporte remoto por recursión.

El handoff real-client recorre:

```text
player -> boat A -> boat B -> player
```

y exige en cada transición:

- un único root actor con `AnatomyMovement.predictsBody(...)`;
- el actor anterior deja de predecir;
- metadata staged del root anterior desaparece;
- al desmontar, la autoridad vuelve sólo al player.

Artifact handoff **`10960180780`**, SHA-256 **`5d102d280a8076e1bd62aa425f24870193312a8aa9605b99e84f8670ff946e9b`**.

Artifact observer **`10959951365`**, SHA-256 **`099d9c039f0e1d1723d935ee7d9c461503d745fe211374d538dd8c2916fdafff`**.

### 8.2 Mutation adequacy

El mismo run mata **siete mutantes compilables**:

- `simulate-all-mutant-must-die`, job **`108856986314`** — una réplica remota no puede ganar full client physics;
- `tick-loop-guard-mutant-must-die`, job **`108856986340`** — el loop ordinario no puede ejecutar carry para observers;
- `reference-local-authority-mutant-must-die`, job **`108856986378`** — una entidad no-local/no-controlada no puede emitir prediction references;
- `bridge-root-owner-mutant-must-die`, job **`108856986431`** — PlatformPhysics no puede aceptar cualquier réplica local en vez del unique root prediction owner;
- `stale-metadata-handoff-mutant-must-die`, job **`108857082946`** — el root anterior no puede conservar receipt/reference metadata staged;
- `duplicate-root-actor-mutant-must-die`, job **`108857082947`** — player montado y vehículo controlado no pueden poseer full prediction simultáneamente;
- `stale-vehicle-authority-mutant-must-die`, job **`108857082996`** — un boat ya no controlado no puede conservar prediction tras handoff.

Todos compilaron antes de morir.

### 8.3 Positivo integrado autorizado

Sobre HEAD S26 **`c39014a5374f6cb2348ee7c726ebb8c239dc613c`**:

- ordinary `build` run **`36400806928`**, job **`108857942507`** — success; artifact **`10960905150`**, SHA-256 **`c6b1be0d3145d0eaa48bb984fcf53c11f84241bac0f443f1a3780dc369fdd6ae`**;
- dedicated reference latency run **`36400806938`**, job **`108857942892`** — success; artifact **`10960181581`**, SHA-256 **`af6bd1866693b11bb220574794ddc483d365593ca9727e9b8c5c1949f13e5145`**.

Ese dedicated preserva el positivo real heredado de S25: player local y controlled boat a 0/100/200 ms continúan ejecutando prediction/reference real sin perder el contrato G4.1.

### 8.4 Zero-change

Comparando el último fix productivo S26 `ad97efb...` con HEAD `c39014a...` no existe ningún cambio posterior bajo `src/main` o `src/client`. Los 11 commits posteriores modifican únicamente:

- workflow S25/S26;
- `S26ControlHandoffClientProof`;
- `s26_prediction_authority_gate.py`.

### 8.5 Veredicto

G4.2 queda cerrado:

- full prediction cliente pertenece únicamente al player local desmontado o al root vehicle realmente controlado;
- observers/remotos permanecen read-only físicamente;
- el soporte remoto de un actor local no se convierte en segundo actor de prediction;
- control handoff retira owner y metadata antiguos;
- sender, bridge y ordinary tick loop comparten la misma frontera de autoridad;
- los positivos player/controlled vehicle permanecen integrados y verdes.

**S26 queda cerrado adversarialmente. G4 task 2 puede marcarse `[x]`. El siguiente gate canónico es G4.3: observer path sin carry local.**

