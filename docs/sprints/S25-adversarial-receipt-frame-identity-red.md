# S25 — receipt frame identity mismatch RED

Rol activo: **ADVERSARY**.

Estado: **RED PRODUCTIVO / G4.1 PERMANECE ABIERTO**.

## 1. Qué ya está cerrado

La primera reapertura de integración cliente, donde el `TransportLedger` avanzaba pero nunca se capturaba un cursor, fue reparada por el IMPLEMENTER:

- `d7a5ba9f700f3b3947967b731943474bd5ebeec6` — publica el cursor en el carry causal;
- `866a8ec33faae3c68cb02c5ee7ce4718a4cb0149` — conecta ese callback al staging cliente;
- `0bff2cf523e90cec792361382c5933831e61393a` — elimina tracing temporal del outgoing hook.

Tras esos cambios, el dedicated proof sí observa `anatomy_move_reference_v1` inmediatamente antes de los movement packets vanilla.

El owner/kernel queda ahora verde con baseline + **15 mutantes compilables muertos** en run `35452742190`: exactly-once, tracking generation, lifecycle combinado, receipt TTL, saturation, non-controller, double-apply, fabricated-hit, rate budget, pending-reference TTL, epoch fence, dimension fence, network-id fence, adjacent-frame matching y ambiguous-exact matching. Baseline job `105922624414`; el mutante de identidad ambigua es `105922827839`. Ordinary del mismo snapshot, run `35452742176`, job `105922622233`: **446/446 required GameTests**, artifact `10586933280`, SHA-256 `869ade328e5d491fd393bbfe484ee5b813b4afb944b12a392549af5b4392e53d`.

## 2. Nuevo RED real

El siguiente hardening exige que el dedicated proof no se limite a ver el payload en Netty: cada fase debe demostrar que el servidor consumió al menos un receipt real mediante `AnatomyTransportReceipts.claim(...)`.

Run **`35448151599`**, job **`105910578723`**, artifact **`10585761418`**, SHA-256:

```text
89ff7254b5883e205e9006c78b686eb0b086e084a333b8f41ae22e3c7ad1c379
```

Player RTT=0 alcanza una fase material estable:

- 201 movement packets player;
- 199 anatomy movement references observadas en la traza acotada;
- cada reference está inmediatamente antes de su movement packet;
- cero correcciones vanilla;
- support/contacto siguen confirmados;
- cliente y servidor terminan en la misma posición;
- **consumedReferences = 0**.

Por tanto el C2S llega al receiver pero `claim()` rechaza todas las referencias.

## 3. Correlación exacta wire ↔ receipt

Se endureció únicamente el harness para trazar los campos metadata del payload y el mapa privado server-side
`transportSequence -> supportFrameSerial`.

Las referencias cliente de la fase son:

```text
frame = 8, 10, 12, 14, ..., 400, 402, 404
```

Hay 199 referencias y **todos los supportFrameSerial enviados son pares**.

Los receipts vivos al final de la misma fase contienen:

```text
167 -> 335
168 -> 337
169 -> 339
...
199 -> 399
200 -> 401
201 -> 403
```

Todos los `supportFrameSerial` retenidos por el servidor son **impares**.

La intersección entre los seriales nombrados por el cliente y los seriales de receipts server-issued es vacía. El support UUID sí coincide.

Esto explica `consumedReferences=0` sin invocar tracking, revision, body identity o control authority: el lookup exacto por `supportFrameSerial` no puede encontrar una entrada.

## 4. Causa arquitectónica localizada

El servidor registra un receipt cuando el dispatcher aplica un transporte certificado:

```text
MaterialPhysicsRuntime
  -> AnatomyMovement.recordCertifiedTransport(...)
     supportFrameSerial = TransportEvidence.supportFrameSerial
```

Ese serial pertenece al endpoint causal del **intervalo material que produjo el carry**.

El cliente, en cambio, publica su cursor desde el `QueryFrame` vigente cuando ejecuta el carry local:

```text
AnatomyMovement.carry(...)
  causalFrame = queryFrame(support)
  ...
  publishClientTransport(body, support, causalFrame.endpoint().frameSerial(), ...)
```

Un endpoint posterior puede conservar el mismo root/estado de transporte incorporado y, sin embargo, avanzar `frameSerial` por otra publicación causal, por ejemplo un nuevo joint/pose sample. El serial exacto del endpoint es por tanto demasiado fino para identificar necesariamente el transporte server-side que el cliente ya incorporó.

El patrón par/impar observado demuestra precisamente dos publicaciones sucesivas por ciclo: el receipt queda ligado a una y el cliente nombra la siguiente.

## 5. Clasificación

**PRODUCTIVO / identidad de referencia incorrecta.**

No es:

- orden Netty;
- captura cliente;
- capability `canSend`;
- READY/local authority;
- controlled-body selection;
- tracking generation;
- receipt TTL/saturation;
- exactamente-once;
- fixture de catálogo;
- corrección vanilla.

El blocker es que el payload/claim usa como clave autoritativa un `supportFrameSerial` que no es estable entre el carry server-side y el endpoint posterior que el cliente incorpora.

## 6. Restricciones de reparación

El ADVERSARY no prescribe el nuevo schema. La reparación debe proporcionar una identidad metadata-only que correlacione **el transporte incorporado** con el receipt server-issued incluso cuando existan publicaciones posteriores del soporte que no representen otro carry.

### Prohibición de reparación por proximidad de serial

El patrón par/impar observado **no** autoriza una corrección `frameSerial±1`, matching por paridad, “último receipt del support” ni búsqueda del serial más cercano. `frameSerial` ordena publicaciones causales del endpoint; no identifica por sí mismo el transporte incorporado. Dos publicaciones consecutivas pueden pertenecer a la misma vida de transporte, y bajo RTT/coalescing la distancia entre el endpoint que produjo el carry server-side y el endpoint vigente en cliente no tiene por qué ser constante.

El nuevo lookup debe ser exacto sobre una identidad causal estable del transporte incorporado. Ante más de un receipt compatible, debe fallar cerrado en vez de elegir por cercanía temporal/serial.

### `rootFrameSequence` por sí solo tampoco basta

El extremo contrario también está descartado por evidencia existente. `AnatomyAnimatedSqueezingTests.twoLocalJointContributionsKeepDistinctTransportProvenance` produce dos contribuciones materiales reales de joints con:

- `transportSequence` consecutivos y distintos;
- `appliedDelta` distintos por contribución;
- **el mismo `rootFrameSequence`**, porque el root permanece estacionario.

Por tanto una clave `support + rootFrameSequence` puede ser ambigua para dos receipts válidos distintos. La reparación debe identificar el transporte/material interval incorporado, no simplemente sustituir un reloj de endpoint demasiado fino por un reloj root demasiado grueso.

Debe conservar:

1. ningún DTO de geometry/contact/pose como autoridad C2S;
2. exactamente-once;
3. recipient/body/control authority;
4. tracking/lifecycle/TTL/saturation;
5. no double-apply;
6. rate budget y pending TTL;
7. orden reference → movement packet;
8. player + controlled boat en dedicated 0/100/200 ms;
9. al menos un receipt realmente consumido por fase;
10. todos los mutation-kills owner-level existentes.

Una posible familia de identidades causales más estable puede derivarse del transporte/root incorporado, pero la elección concreta pertenece al IMPLEMENTER y debe ser validada adversarialmente.

### Versionado wire si cambia la semántica de identidad

El TYPE vigente es `scalebrews:anatomy_move_reference_v1` y su tercer campo significa **`supportFrameSerial`**. Si la reparación reemplaza esa identidad por un token/material-interval/transport identity distinto, no puede reinterpretar silenciosamente el mismo `long` bajo el mismo protocolo v1.

La transición debe ser explícitamente versionada o mantener compatibilidad inequívoca. Cliente y servidor no pueden considerar compatible un mismo payload si cada extremo interpreta ese campo como una identidad causal distinta.

## 7. Handoff

**ADVERSARY → IMPLEMENTER.**

G4.1 sigue abierto. La captura y el wire order están reparados; el blocker único conocido es ahora la identidad exacta usada para correlacionar el reference cliente con el receipt server-side.

## 8. Hardening posterior del owner — sin cambio productivo

La primera separación de los fences lifecycle reveló tres **supervivientes de oracle**, no defects de producción: al retirar individualmente epoch, dimension o body network id de `claim(...)`, el holdout anterior seguía verde porque sólo inducía una discontinuidad de catalog revision.

Se añadieron tres casos owner-level que reescriben únicamente el receipt histórico en un eje y exigen que la referencia no se consuma ni stagee. Red-before-green del harness:

- run `35451440249`: epoch/dimension/network-id mutants compilaron y sobrevivieron;
- run `35451920466`: los mismos tres mutantes compilaron y murieron con los nuevos oracles.

Además se añadió un caso exacto contra fuzzy matching: un receipt con serial `N` debe rechazar `N+1` y seguir siendo reclamable por `N`. El mutante `adjacent-frame-match`, que acepta `|frame-reference|<=1`, compila y muere en job `105920630894`; artifact `10587346709`, SHA-256 `544e0626aad36be89dbe64ba8fd2168ce416239cec256641830f4d78d394c0ed`.

Esto cierra mutation adequacy del owner conocido, pero **no toca el RED productivo** de este documento: el dedicated client sigue nombrando un endpoint distinto del receipt server-side y por tanto no consume ninguno.

**Estado tras hardening:** owner/kernel cerrado; reference↔receipt transport identity continúa RED y bloquea G4.1.

## 9. Matriz negativa de identidades demasiado gruesas

El hardening posterior añade un oracle explícito para el extremo contrario al fuzzy frame matching.

`S25AdversarialReferenceBehaviorTests.oneTickContactAndRootCanContainMultipleDistinctTransportReceipts` crea dos receipts server-issued que comparten simultáneamente:

- support UUID;
- game tick;
- `contactSequence`;
- `rootFrameSequence`;

pero tienen `transportSequence` distintos y deben seguir siendo reclamables de forma independiente.

Workflow `s25-adversarial-reference-behavior`, run **`35452355407`**: baseline y toda la matriz de 14 mutantes permanecen **success**. Ordinary run **`35452355386`**, job **`105921596373`**: **446/446 required GameTests**, `BUILD SUCCESSFUL`; artifact **`10586817824`**, SHA-256 **`8fd565192421040101aa8b49c92ca6d4f17bd218e6d0dbf3ddc7a79c33676e52`**.

La prueba demuestra que tampoco son identidades suficientes, por sí solas:

- support + tick;
- support + contact sequence;
- support + root-frame sequence;
- cualquier combinación de esos campos que no distinga las dos contribuciones materiales del mismo tick/contact/root.

Esto complementa la prohibición de `frameSerial` aproximado. La reparación debe nombrar **el transporte/material interval incorporado**, no elegir un reloj causal que sea demasiado fino o demasiado grueso.

### Evidencia independiente de que `frameSerial` no es un transport id

El mismatch dedicated no depende de una casualidad de paridad. La suite cliente existente `AnatomyPresentationFrameTests` ejerce explícitamente **root-only endpoints**: crea/publica un endpoint con `frameSerial = prior + 1` manteniendo el mismo `jointSampleTick`, y el cliente debe aceptar ese root nuevo sin re-evaluar joints. La arquitectura y `AnatomyPosePayload` documentan además que root-only updates pueden repetir el joint clock mientras `frameSerial` avanza.

Por tanto `frameSerial` es un reloj de publicación de endpoint. Puede avanzar sin que exista una nueva contribución de transporte para el body y no debe reinterpretarse como transport identity.

## 10. Identidad exacta también debe ser única

El hardening añade `S25AdversarialReferenceBehaviorTests.ambiguousExactReceiptIdentityFailsClosed`: dos receipts distintos comparten deliberadamente el mismo support + reference identity exacto. `claim(...)` debe devolver null sin consumir ninguno.

Run **`35452742190`**:

- baseline job **`105922624414`** — success;
- `ambiguous-exact-match`, job **`105922827839`** — el mutante compila, elimina únicamente el rechazo `if(match!=null)return null` y muere.

Artifact del mutante **`10587292882`**, SHA-256 **`b668ee8891b6a818c53c4962953b29a19d878e60cd733ab2176bafd24ed6ba76`**.

Por tanto la reparación de la identidad dedicada debe satisfacer dos propiedades simultáneas:

1. matching exacto, sin aproximación;
2. unicidad, con fail-closed si más de un receipt comparte el token.

Esto no modifica producción ni resuelve el dedicated RED; impide que una nueva identidad server-issued se cierre eligiendo arbitrariamente entre receipts colisionados.

