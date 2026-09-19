# S25 — receipt frame identity mismatch RED

Rol activo: **ADVERSARY**.

Estado: **RED PRODUCTIVO / G4.1 PERMANECE ABIERTO**.

## 1. Qué ya está cerrado

La primera reapertura de integración cliente, donde el `TransportLedger` avanzaba pero nunca se capturaba un cursor, fue reparada por el IMPLEMENTER:

- `d7a5ba9f700f3b3947967b731943474bd5ebeec6` — publica el cursor en el carry causal;
- `866a8ec33faae3c68cb02c5ee7ce4718a4cb0149` — conecta ese callback al staging cliente;
- `0bff2cf523e90cec792361382c5933831e61393a` — elimina tracing temporal del outgoing hook.

Tras esos cambios, el dedicated proof sí observa `anatomy_move_reference_v1` inmediatamente antes de los movement packets vanilla.

El owner/kernel también permanece verde con baseline + **10 mutantes compilables muertos** en run `35447584710`: exactly-once, tracking generation, lifecycle, receipt TTL, saturation, non-controller, double-apply, fabricated-hit, rate budget y pending-reference TTL.

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

## 7. Handoff

**ADVERSARY → IMPLEMENTER.**

G4.1 sigue abierto. La captura y el wire order están reparados; el blocker único conocido es ahora la identidad exacta usada para correlacionar el reference cliente con el receipt server-side.
