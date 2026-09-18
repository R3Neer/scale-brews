# S24 — tracking-generation read revival RED

Rol activo: **ADVERSARY**.

Estado: **RED PRODUCTIVO / BLOQUEA G3.9**.

## 1. Contrato atacado

S24 separó la vida de tracking server-side en `TrackingGenerationLedger`:

- `acquire(UUID)` crea una ventana autoritativa nueva;
- `current(UUID)` observa únicamente una ventana ya activa;
- `release(UUID)` retira la ventana sin rebobinar el contador;
- `UNAVAILABLE = 0` significa ausencia de autoridad activa o saturación.

La façade pública `AnatomyRuntime.trackingGeneration(ServerPlayer, Entity)` está documentada como:

> Current recipient/body tracking generation for a server receipt; never client authority. Zero means saturated/no active authority.

Por tanto una consulta a una pareja que nunca recibió `START_TRACKING` no puede crear autoridad.

## 2. Holdout adversarial

Test:

`S24AdversarialTrackingReadPurityTests.observingUntrackedPairCannotMintTrackingAuthority`

Commit del holdout:

- **`d63deca5c4d16445d9349cb5613abbe4b53d3954`** — `test(s24): hold out tracking-generation read revival`.

Workflow aislado:

- **`4ec3eb69fc70fee760fba34716bbd261a4ede6af`** — `ci(s24): exercise tracking read-purity holdout`.

El fixture evita deliberadamente cualquier explicación basada en el tracker vanilla:

1. arranca `AnatomyRuntime.startPrepared(...)`;
2. crea un `ServerPlayer` mediante `GameTestHelper.makeMockServerPlayer(GameType.SURVIVAL)`;
3. Minecraft 26.2 deja ese mock fuera de `PlayerList`, condición comprobada por el propio test;
4. crea un body;
5. no existe ningún `START_TRACKING` posible para esa pareja receptor/body;
6. llama una sola vez a `AnatomyRuntime.trackingGeneration(recipient, body)`;
7. exige `TrackingGenerationLedger.UNAVAILABLE == 0`.

## 3. Resultado

Run **`35329517023`**, job **`105550355468`**: **failure causal**.

Aserción exacta:

```text
A read of an untracked recipient/body pair must return UNAVAILABLE and must not acquire tracking authority; observed=1 on tick 0
```

Artifact **`10541105179`**, SHA-256:

```text
ccd06b1e6f39d3e66670f7315f1599b313531abfce3c2f52e495d6f20fc1bd99
```

Ordinary build del mismo snapshot: run **`35329516917`** — **success**.

El rojo no procede de compilación, setup, network capabilities ni un evento de tracking retrasado. La precondición del receptor fuera de `PlayerList` pasa y el fallo aparece únicamente al observar la generación.

## 4. Causa productiva

En `AnatomyRuntime`:

```java
private static long generation(State state,ServerPlayer recipient,UUID body) {
    return state.trackingGenerations
        .computeIfAbsent(recipient,ignored->new TrackingGenerationLedger())
        .acquire(body);
}

public static long trackingGeneration(ServerPlayer recipient,Entity body) {
    if(recipient==null || body==null)return 1;
    var state=STATES.get(recipient.level().getServer());
    return state==null?1:generation(state,recipient,body.getUUID());
}
```

La façade de lectura llama al helper de **adquisición**. Consultar una pareja ausente crea un ledger si hace falta y ejecuta `acquire(body)`, mintiendo una generación nueva.

Esto confirma la costura que ya estaba marcada como pendiente en `S24-adversarial-tracking-generation-bound.md`: memoria acotada no basta si un consumer tardío puede revivir autoridad después de que una ventana haya sido retirada.

## 5. Consumers productivos afectados

Hay al menos dos rutas productivas que usan la façade con side effect:

- `AnatomyTransportReceipts.record(...)` obtiene `AnatomyRuntime.trackingGeneration(recipient, body)` antes de registrar un receipt;
- el overload `AnatomyNetworking.sendPose(ServerPlayer, PublishedFrame)` obtiene la misma façade antes de enviar una pose.

Por ello la reparación no debe limitarse a cambiar el retorno del método sin revisar quién posee la adquisición legítima. Una lectura pura puede hacer visibles callers que antes dependían implícitamente de que leer significase crear.

## 6. Restricciones de la reparación

No se prescribe arquitectura concreta. El candidato debe demostrar simultáneamente:

1. observar una pareja sin ventana activa devuelve `UNAVAILABLE` y no modifica el ledger;
2. tras un `STOP_TRACKING/release`, una lectura tardía no recrea la ventana retirada;
3. la adquisición ocurre sólo en una transición causal autorizada, por ejemplo START_TRACKING o un seam explícito equivalente;
4. pose/contact/receipt que necesiten una generación no pueden fabricarla desde un consumer tardío;
5. self/player/passenger cases que requieran autoridad explícita siguen funcionando mediante una ruta justificada, no reintroduciendo adquisición escondida en el getter;
6. saturación y bounds del ledger permanecen fail-closed;
7. los holdouts de dimensión, reconnect, replay y tracking-ledger mutation-kill siguen verdes.

## 7. Handoff

**Clasificación ADVERSARY: PRODUCTIVO.**

G3.9/S24 no puede cerrarse. El IMPLEMENTER debe separar observación de adquisición y revalidar los consumers que hoy dependen de la façade impura.

El adversario no modifica producción.
