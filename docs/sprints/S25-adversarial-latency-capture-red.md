# S25 — dedicated latency reference capture RED

Rol activo: **ADVERSARY**.

Estado: **RED PRODUCTIVO / G4.1 PERMANECE ABIERTO**.

## 1. Contexto

La capa owner/kernel de G4.1 ya está adversarialmente fuerte:

- payload C2S metadata-only;
- receipt server-issued como única autoridad;
- exactly-once;
- tracking/lifecycle/TTL/saturation;
- controlled vehicle;
- fake hit;
- no double-apply;
- rate budget.

Run `35446958412` deja verde el baseline y mata **9 mutantes compilables**.

La deuda restante se trasladó deliberadamente a integración real dedicated/latency: player y controlled boat con `allow-flight=false`, RTT 0/100/200 ms, referencia C2S real y movement packet vanilla real.

## 2. Harness corregido antes de clasificar producto

El primer workflow de latency no entregaba al segundo `runClientGameTest` el catálogo original exportado y moría antes de arrancar la prueba con:

```text
N2 needs the immutable isolated original anatomy export
```

Eso era CI/harness, no producto.

`d8ffe899fbbf4d3ee12f71190be8c36de440eb4c` corrigió únicamente el workflow para pasar:

```text
-PscalebrewsAnatomyCatalog=<anatomy-export>
```

Después de ese cambio la prueba llega a servidor dedicado, contacto material, input real y tráfico Netty medido.

## 3. RED causal en integración real

Run **`35447097292`**, job **`105907798139`**, sobre la línea con instrumentación temporal de diagnóstico:

- el outgoing hook para movement vanilla **sí se alcanza**;
- `AnatomyClientNetworking.ready(body) == true`;
- `body.isLocalInstanceAuthoritative() == true`;
- `ClientPlayNetworking.canSend(AnatomyMoveReferencePayload.TYPE) == true`;
- el cliente mantiene contacto/support real;
- el `TransportLedger` cliente avanza continuamente;
- pero `predictedMovementReferences` no contiene nunca el body;
- por tanto no sale ningún `anatomy_move_reference_v1`.

Diagnóstico inicial del sender:

```text
S25 outgoing anatomy movement hook reached body=entity.minecraft.player packet=...ServerboundMovePlayerPacket$PosRot
S25 client reference diagnostic reject-no-predicted-reference transport=null
```

Muestras posteriores dentro de la fase player RTT=0:

```text
step=25  transportSeq=26  refStaged=false  ready=true  localAuth=true  canSendRef=true
step=75  transportSeq=76  refStaged=false  ready=true  localAuth=true  canSendRef=true
step=125 transportSeq=126 refStaged=false  ready=true  localAuth=true  canSendRef=true
step=175 transportSeq=176 refStaged=false  ready=true  localAuth=true  canSendRef=true
```

La fase completa produjo aproximadamente 201 movement packets y cero correcciones vanilla, pero terminó en:

```text
S25 player RTT 0 emitted no anatomy_move_reference_v1 packet despite real local carry
```

No se alcanzó todavía RTT 100/200 ni la fase boat porque el gate debe fallar en el primer caso causal.

## 4. Localización productiva

El cursor se captura hoy en `AnatomyClientNetworking.END_CLIENT_TICK` sólo si el transport cambia **durante esa llamada concreta** a `AnatomyMovement.carry(entity)`:

```java
var before=AnatomyMovement.transport(entity);
AnatomyMovement.carry(entity);
var after=AnatomyMovement.transport(entity);
if(after!=null && (before==null || after.sequence()!=before.sequence()))
    captureMovementReference(entity,after.sequence());
```

Pero el body cliente ya recibe carry antes, por la ruta de movimiento real:

```text
Entity.move
  -> PlatformEntityMixin
  -> PlatformPhysics.carry(self)
  -> AnatomyMovement.carry(self)
  -> TransportLedger.record(...)
```

Cuando llega `END_CLIENT_TICK`, `before` ya contiene la secuencia nueva. El carry repetido es idempotente, `after.sequence == before.sequence`, y `captureMovementReference(...)` no se llama.

Esto concuerda exactamente con la evidencia:

- transport sequence local sí crece;
- no hay reference staged;
- los guards de envío sí están verdes;
- el outgoing hook sí se ejecuta.

## 5. Clasificación

**PRODUCTIVO / integración cliente.**

No es:

- ausencia de capability;
- error del receiver server;
- `canSend`;
- autoridad local;
- READY;
- ordering Netty;
- catálogo de fixture;
- kernel `claim/resolve`.

El defecto está en la relación temporal **carry cliente real → captura del cursor C2S**.

## 6. Restricciones de la reparación

El ADVERSARY no prescribe el parche. La reparación debe demostrar:

1. el cursor se captura causalmente cuando un carry local realmente incorpora un endpoint server-issued;
2. no se ejecuta un segundo carry para fabricar el cursor;
3. una misma contribución local no produce referencias duplicadas;
4. la referencia sigue saliendo inmediatamente antes del movement packet vanilla correspondiente;
5. player y controlled vehicle funcionan;
6. observer/no-controller no emite autoridad;
7. los 9 mutation-kills owner-level siguen muertos;
8. dedicated `allow-flight=false` pasa RTT 0/100/200 ms para player y boat;
9. zero-change final no conserva tracing temporal de diagnóstico.

## 7. Instrumentación temporal

Los commits:

- `413f408d860b3b1eb2acd3f225ecf7dc6db1e4c2` — trace de guards de emisión;
- `c03f80b9f66ac143065288ca5a1a87d676208f07` — trace del outgoing hook;
- `23dbfcdaf6a2d7cc708affcb22d583ff5e1ac8fd` — trace de captura/carry;

son **diagnóstico temporal**, no parte deseada del diseño final. Deben retirarse al cerrar el RED.

## 8. Handoff

**ADVERSARY → IMPLEMENTER.**

G4.1 permanece abierto. El kernel behavior/reference está mutation-adequate; el blocker único conocido es que la ruta real cliente no captura el cursor en el mismo evento causal que avanza el transport ledger.
