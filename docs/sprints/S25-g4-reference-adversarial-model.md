# S25 — G4.1 adversarial model: transport references from confirmed receipts

Rol activo: **ADVERSARY**.

Estado: **CANDIDATO IMPLEMENTER PRESENTE / PENDIENTE CIERRE ADVERSARY**.

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

### E2. Aceptación no reaplica transporte

Incluso una referencia **válida** sólo nombra/correlaciona transporte que el servidor ya aplicó y cuyo baseline ya actualizó al crear el receipt. Inmediatamente antes/después de aceptar la referencia, y antes de consumir el siguiente movement packet vanilla, deben permanecer idénticos:

- `body.position()` y AABB;
- `AnatomyMovement.contactSequence(body)`/support;
- `TransportLedger.current(body).sequence()` y `appliedDelta`;
- número/contenido de receipts server-issued;
- tracking/binding/root endpoint authority.

Sólo puede cambiar estado metadata de consumo/rebase. Si el diseño aplica el reference conjuntamente con el movement packet, el oracle debe demostrar igualmente que `receipt.appliedDelta` no se suma una segunda vez.

Este será el mutation-kill de **double-apply**: introducir una segunda aplicación del delta confirmado debe compilar y producir desplazamiento/sequence/receipt extra observable.

### F. Controlled vehicle

El mismo mecanismo debe distinguir player local de vehículo realmente controlado sin permitir referencias a una tercera entidad observada.

El server debe derivar el body permitido desde `context.player()` y control authority, no confiar en un arbitrary body id enviado por cliente.

**Holdout adversarial recomendado:** boat con dos `ServerPlayer` pasajeros. El store de receipts emite provenance a ambos indirect passengers, de modo que los dos pueden poseer un receipt server-issued del mismo body; sólo `getControllingPassenger()` tiene autoridad de movimiento. La referencia del segundo pasajero debe rechazarse sin consumir ni invalidar el receipt del controlador. Esto evita el falso diseño “receipt existe ⇒ cliente autorizado”.

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

El baseline de apertura permaneció RED mientras la capacidad estuvo ausente. El candidato IMPLEMENTER de §11 elimina ese RED de presencia, pero **G4.1 sigue ABIERTO** hasta superar la campaña conductual y mutation adequacy independientes.

## 8. Frontera estructural previa al implementer

La ausencia de capacidad no deja el diseño sin protección. Antes de que exista el primer candidate C2S se añadió un gate independiente para impedir dos atajos inválidos:

1. **borrow legacy**: ningún código `collision/**` puede depender de `PlatformMovePayload` o `PlatformMovementReference`; además la ruta legacy debe seguir saliendo/limpiando bajo `AnatomyApi.ownsSharedPhysics(...)`;
2. **rich client authority**: cualquier payload registrado en `PayloadTypeRegistry.serverboundPlay()` bajo `collision/**` se sigue hasta su clase de schema y no puede contener `AnatomyContactPayload`, `AnatomyPosePayload`, `SurfaceContact`, geometry/snapshots/endpoints, pose inputs, root frames/transforms, matrices/quaternions ni campos de contacto material como `localPoint`/normal.

El gate no exige nombre de clase, forma exacta de la clave ni algoritmo de lookup. Sólo fija que la futura referencia sea metadata y que el receipt server-side siga siendo la autoridad.

Workflow final `s25-adversarial-reference-authority`, run **`35384438639`**:

- baseline `reference-authority-boundary`, job **`105727903384`** — success;
- `legacy-borrow-mutant-must-die`, job **`105727968468`** — el mutante common/server compiló y fue rechazado;
- `client-legacy-borrow-mutant-must-die`, job **`105727968484`** — el mutante client-side compiló y fue rechazado;
- `rich-authority-mutant-must-die`, job **`105727968513`** — registra como serverbound el payload de contacto autoritativo, compila y el gate lo mata.

Ordinary sobre el mismo snapshot de hardening, run **`35384438584`**, job **`105727903091`** — success; artifact **`10563561102`**, SHA-256 **`89cfb89533561e1dcdba5af812f2e1d2a4b540912eff88deee7b4c9cf435fe47`**.

### Observación sobre identidad de receipt

`SupportTransport.sequence` es un cursor body-local; una discontinuidad que puede reiniciarlo pasa por `AnatomyMovement.invalidateBody(...)`, que primero ejecuta `clear(body)` y ésta invalida los receipts. Por tanto no se ha encontrado un alias histórico que obligue a transmitir geometry/contacto en la referencia. La implementación sigue siendo libre de elegir la clave metadata adecuada, pero debe validar el receipt exacto y los ejes lifecycle exigidos por este modelo.

**Estado histórico previo al handoff implementer:** la superficie C2S/validator aún no existía y las fronteras de seguridad ya estaban mutation-sensitive. El estado vigente está en §11.

## 9. Receipt authority store — precondición adversarial cerrada

Antes de implementar el C2S se cerró una propiedad que el futuro validator no debe volver a discutir: el store de receipts es autoridad **recipient-local + body-local**, con teardown independiente y saturation scoped.

`S25ReceiptAuthorityIsolationTests` usa dos `ServerPlayer` como pasajeros del mismo boat:

- ambos reciben un receipt server-issued para el mismo body;
- el primer recipient cambia a un segundo boat y acumula history independiente por body UUID;
- se satura sólo `(first, secondBody, tick)`;
- el segundo recipient no adquiere receipt ni saturation del segundo body;
- `disconnect(first)` elimina todos los bodies de first sin tocar el receipt de second.

Esto además fija el futuro holdout de controlled vehicle: el segundo pasajero puede poseer un receipt válido del boat sin ser necesariamente su controlador. **Receipt existence no concede control authority.**

Workflow `s25-adversarial-receipt-isolation`, run **`35384233437`**:

- baseline `receipt-isolation`, job **`105727247783`** — success;
- recipient-key mutant, job **`105727617910`** — compiló y murió;
- global-disconnect mutant, job **`105727617921`** — compiló y murió;
- body-key mutant, job **`105727617960`** — compiló y murió.

Artifact baseline **`10563370910`**, SHA-256 **`dc523680cb24cd8036fc102ee56a5c6a4a68cd73f37586edc0f9d72410eced0e`**.

El store sigue siendo histórico, no autorización viva: STOP_TRACKING libera la ventana server-side pero no borra necesariamente el receipt antes de TTL. Por tanto el futuro validator debe comparar `receipt.trackingGeneration` contra la ventana actual o aplicar un fence causal equivalente; no puede aceptar por mera presencia en `history()`.

**Conclusión de la precondición adversarial:** recipient/body/saturation/teardown queda cerrada. La ausencia de reference C2S descrita aquí fue posteriormente cubierta por el candidato IMPLEMENTER de §11; el gate global sigue abierto por validación conductual pendiente.

## 10. Handoff al IMPLEMENTER

Este fue el handoff original al IMPLEMENTER cuando la capacidad aún no existía. El candidato G4.1 debía aportar una superficie C2S real que hiciera pasar la presence gate y permitiera ejecutar los holdouts conductuales; §11 registra la respuesta implementer ya aterrizada.

Propiedades mínimas observables del handoff, sin prescribir nombres/clases:

1. el cliente puede emitir una referencia **metadata-only** asociada al player o vehículo realmente controlado;
2. el servidor deriva el body autorizado desde el contexto de conexión y no confía en un body arbitrario del payload;
3. el lookup parte del store recipient/body ya certificado y localiza un receipt server-issued exacto;
4. receipt histórico de tracking generation retirada se rechaza aunque siga dentro de TTL;
5. saturated tick, receipt expirado o transport sequence inexistente fallan cerrado;
6. la primera aceptación consume la referencia/receipt según el diseño y el replay idéntico falla;
7. una referencia inventada no cambia contacto, support, position/AABB, root/endpoint, tracking ledger ni transport ledger;
8. una referencia válida tampoco reaplica `appliedDelta`; sólo habilita/correlaciona la baseline del movement packet vanilla;
9. un segundo pasajero con receipt válido del mismo boat pero sin control authority no puede usarlo;
10. el schema permanece bounded y sin geometry/pose/contact authority.

Al aterrizar ese candidato, la presence gate debe pasar. Eso **no cerrará G4.1**: habilitará la campaña A–G y los ocho mutation-kills mínimos definidos arriba.

**Handoff ADVERSARY → IMPLEMENTER (histórico):** capacidad C2S/validator/consumption ausente; fronteras previas y store server-side verdes. §11 contiene el handoff de vuelta IMPLEMENTER → ADVERSARY.


## 11. Candidato IMPLEMENTER — referencia causal server-issued

Rol activo de esta fase: **IMPLEMENTER**.

El primer candidato productivo G4.1 ya existe. No cambia el modelo adversarial de las secciones anteriores y **no cierra S25**; convierte el RED de capacidad ausente en una superficie real que el ADVERSARY puede someter ahora a A–G y a los ocho mutation-kills definidos arriba.

### 11.1 Diseño aterrizado

- `AnatomyMoveReferencePayload` es C2S **metadata-only** y versionado por id de payload. Transporta únicamente:
  - `vehicle`: distingue jugador frente a vehículo controlado;
  - `support UUID`;
  - `supportFrameSerial`: endpoint causal que el servidor ya publicó.
- El payload **no contiene body id**, posición, geometría, contacto, pose, root transform, matrices ni delta.
- `AnatomyMovementReference.accept(...)` deriva el body desde `context.player()`:
  - jugador sólo si no está montado;
  - vehículo sólo si es el root vehicle y `getControllingPassenger()==player`.
- `AnatomyTransportReceipts.claim(...)` localiza únicamente un receipt ya emitido para ese recipient/body y exige:
  - support + frame serial exactos y no ambiguos;
  - epoch, revisión y dimensión vigentes;
  - body UUID + network id vigentes;
  - tracking generation vigente;
  - tick no saturado;
  - referencia no consumida previamente.
- Exactly-once se guarda como metadata `consumedTransportSequences`; **el receipt histórico no se borra ni reescribe**.
- El receipt conserva server-side el `transportSequence` asociado al frame. El cliente nunca lo inventa ni lo transmite.
- `resolve(...)` usa `TransportLedger.since(receipt.transportSequence())`; por tanto sólo suma transporte server-side **posterior** al receipt. `receipt.appliedDelta` no se reaplica.
- Una referencia pendiente vive como máximo `PENDING_TICKS=2` y sólo puede haber una pendiente por conexión.
- El presupuesto C2S es `MAX_REFERENCES_PER_TICK=16`.
- El cliente sólo captura un cursor después de que un `SupportTransport` local nuevo haya incorporado un endpoint server-issued y lo emite una vez antes del siguiente movimiento vanilla.

La primera versión implementer intentó identificar el receipt con clocks/secuencias locales del cliente y fue descartada durante revisión: con RTT/coalescing esos valores no identifican necesariamente el material server-side. El diseño vigente usa exclusivamente `support + server frameSerial` como metadata nombrable por el cliente.

### 11.2 Evidencia implementer ejecutada

Candidato productivo final: `b09c490a8405cf5042468e8ea1bd6d4538fad0cd`.

Sobre ese snapshot:

- build `35388973833`, job `105742610038`: **success**;
- presence `35388973889`, job `105742611717`: **success**;
- authority boundary + tres mutantes `35388973830`: **success** completo;
- receipt isolation + recipient/body/disconnect mutants `35388973790`: **success** completo;
- tracking ledger `35388973836`: **success**;
- S16 canonical catalog `35388973771`: **success**;
- S22 coverage residuals `35388973929`: **success**.

Regresión propia del implementer añadida después, sin cambios productivos:

- commit `35224ed5261969854feb58379f2fcd34927d61c9`;
- workflow `s25-implementer-reference-proof`, run `35444126698`, job `105900018851`: **success**;
- artifact `S25-implementer-reference`, id `10585450603`;
- build del mismo snapshot `35444126655`: **success**.

`S25ImplementerReferenceTests` demuestra únicamente la regresión de desarrollo del candidato: support/frame incorrectos no consumen, el frame exacto resuelve el receipt, el receipt histórico permanece valor-equivalente tras claim y el replay exacto falla. No sustituye los holdouts adversariales independientes.

### 11.3 Lectura de coste implementer

Clasificación:

- receiver/claim: **HOT_NETWORK**;
- `resolve(...)` del siguiente packet vanilla: **HOT_MOVE_QUERY**;
- captura cliente tras carry: **HOT_NETWORK/HOT_TICK** acotado a actores localmente autoritativos.

Bounds estructurales actuales:

- máximo 16 intentos C2S por conexión/tick;
- una referencia pendiente;
- pending TTL de 2 ticks;
- receipts: 40 ticks × máximo 16 por tick/body, es decir, como máximo 640 entradas históricas por recipient/body antes de pruning;
- claim hace scan lineal sólo sobre ese history local y por tanto permanece acotado;
- no hay scan global de entidades, reflexión, geometría, matrices ni reconstrucción de catálogo en esta ruta.

No se declara con esto cumplimiento temporal de NFR-014; sólo boundedness/locality de este corte.

### 11.4 Handoff IMPLEMENTER → ADVERSARY

S25/G4.1 permanece **ABIERTO**. El RED de “superficie ausente” ya no aplica, pero el cierre requiere todavía la campaña independiente A–G y los ocho mutation-kills de §5 sobre el candidato vigente.

Hasta ese cierre no se abre trabajo productivo G4.2.

## 12. Primera devolución adversarial — clasificación IMPLEMENTER

Rol activo de esta clasificación: **IMPLEMENTER**. No se modifica el holdout adversarial ni producción.

El adversario añadió `S25AdversarialReferenceBehaviorTests` y el workflow `s25-adversarial-reference-behavior`. La primera ejecución real fue run `35444313146`, job `105900506887`: el holdout compiló, pero falló antes de ejercitar válidamente el happy path.

Fallo observado:

`Valid reference must rebase only transport applied after the claimed receipt: expected=(3.375, 4.0, 2.0) resolved=(3.125, 4.0, 2.0)`.

El log muestra inmediatamente antes que el mock `ServerPlayer` fue desconectado por la frontera productiva de compatibilidad de protocolo:

`Scale Brews: incompatible anatomical protocol; update the client mod.`

Con `AnatomyRuntime` activo, `ServerPlayConnectionEvents.JOIN` ejecuta `catalog(...)`; un mock GameTest sin capacidades registradas no puede enviar/recibir el protocolo anatomy y es expulsado. Después de esa desconexión `AnatomyMovementReference.accept(...)` rechaza correctamente al player muerto, por lo que no existe pending reference y `resolve(...)` devuelve el absoluto original.

**Clasificación IMPLEMENTER:** RED de fixture/harness previo a la propiedad que se pretendía medir, no evidencia de defecto productivo en rebase. Relajar `catalog(...)`, `canSend(...)`, `player.isRemoved()` o la autoridad de conexión para hacer verde el mock violaría la frontera de protocolo y no es una reparación aceptable.

**Acción:** cero cambios productivos. El control vuelve al ADVERSARY para que su harness proporcione una conexión/capability válida o un seam estrictamente test-only que alcance la ruta `accept → claim → resolve` sin debilitar producción. Una nueva ejecución válida decidirá si existe un RED productivo posterior.

**Localización adversarial posterior:** run `35444656309` añadió un assert de precondiciones y reportó `active=true owns=false`. Ese `owns=false` tampoco demuestra pérdida de sesión: el holdout llama `AnatomyRuntime.owns(player)` con tipo estático `ServerPlayer`, por lo que Java selecciona el overload package-private `owns(LivingEntity)`, cuya semántica es ownership del **binding causal de un soporte**. El candidato productivo llama `owns(body)` con `body` tipado como `Entity`, y por tanto usa `owns(Entity)`, ownership de sesión. Con catálogo preparado vacío es correcto que el primer overload estrecho devuelva false para el player.

La instrumentación implementer temporal confirmó además que el overload `owns(Entity)` no era el que estaba fallando y fue retirada completamente en `9a2b8f0913d45c1e8f936f28c5f2392dcd417330` + `fce6d86bf9d63858838593b18d9e872bdeee429b`.

**Clasificación vigente:** el holdout todavía no ha alcanzado una precondición equivalente a la llamada productiva de `accept(...)`; sigue devuelto al ADVERSARY. No hay reparación productiva autorizada por estos REDs.

## 13. Cierre adversarial del owner/kernel — G4.1 sigue abierto

La campaña owner-level ya está convergida sobre el candidato productivo actual. No cierra G4.1 porque el dedicated proof sigue sin correlacionar el token cliente con un receipt real, pero elimina los huecos locales conocidos de `accept → claim → resolve`.

Run final del owner **`35452742190`**:

- baseline `reference-behavior`, job **`105922624414`** — success;
- **15 mutantes dirigidos compilaron y murieron**:
  - expired receipt `105922827704`;
  - lifecycle combinado `105922827646`;
  - non-controller `105922827657`;
  - saturated tick `105922827653`;
  - tracking generation `105922827754`;
  - rate budget `105922827739`;
  - fabricated hit `105922827776`;
  - exactly-once `105922827715`;
  - pending TTL `105922827683`;
  - adjacent-frame/fuzzy matching `105922827685`;
  - network-id fence `105922827680`;
  - double-apply `105922827666`;
  - dimension fence `105922827764`;
  - epoch fence `105922827678`;
  - ambiguous exact identity `105922827839`.

Los tres fences lifecycle separados tuvieron un ciclo rojo de **oracle** antes de quedar cubiertos: run `35451440249` demostró que epoch/dimension/network-id mutants compilaban y sobrevivían; `S25AdversarialReferenceLifecycleFenceTests` añadió un caso aislado por eje sin modificar producción, y el run final los mata.

El mutante `adjacent-frame-match` sustituye la igualdad exacta por `|serverSerial-clientSerial| <= 1`. También compila y muere. Por tanto el RED dedicated par/impar **no puede repararse** con `±1`, paridad, nearest-frame ni cualquier fuzzy match equivalente.

Baseline artifact **`10587775660`**, SHA-256 **`3ebc64a0b8cdb6c19550d9638770719fd9bbccea744a392c951696921fa092ba`**. Ambiguous-identity mutant artifact **`10587292882`**, SHA-256 **`b668ee8891b6a818c53c4962953b29a19d878e60cd733ab2176bafd24ed6ba76`**.

Ordinary sobre el mismo snapshot, run **`35452742176`**, job **`105922622233`**:

- **446/446 required GameTests passed**;
- `BUILD SUCCESSFUL`;
- artifact **`10586933280`**, SHA-256 **`869ade328e5d491fd393bbfe484ee5b813b4afb944b12a392549af5b4392e53d`**.

### Único blocker conocido tras este cierre local

El dedicated proof continúa RED porque la metadata wire actual nombra `supportFrameSerial` del endpoint vigente incorporado por el cliente, mientras el receipt server-side está ligado al endpoint del intervalo material que produjo el carry. Esos endpoint serials pueden avanzar sin otro transporte y no son una identidad estable del transporte incorporado.

El adversario ha descartado además dos clases de parche:

- `frameSerial` aproximado/fuzzy: mutation-killed;
- `rootFrameSequence` solo: demasiado grueso, porque dos contribuciones joint reales pueden compartir root sequence y conservar transport sequences distintos.

**Estado vigente:** owner/kernel G4.1 cerrado adversarialmente; integración reference↔receipt transport identity **RED PRODUCTIVO**. G4.1 y G4 global permanecen abiertos y G4.2 no debe comenzar.

### 13.1 Authority schema hardening adicional

La frontera de autoridad rechaza también el intento de elevar un cursor local del cliente a autoridad C2S. El field name `localTransportSequence` está prohibido en cualquier schema collision/anatomy registrado serverbound; un token/sequence **server-issued** no queda prohibido por esta regla.

Run `s25-adversarial-reference-authority` **`35452660678`**:

- baseline `105922405151` — success;
- legacy server borrow `105922422964` — success as mutation harness;
- rich contact-authority schema `105922422974` — success as mutation harness;
- client legacy borrow `105922423031` — success as mutation harness;
- client-local sequence schema `105922423094` — mutant compiló y el gate lo mató.

El propósito es impedir que el RED dedicated se “resuelva” enviando al servidor el `localTransportSequence` que sólo existe para coherencia del carry cliente. Esa secuencia no es receipt authority.

