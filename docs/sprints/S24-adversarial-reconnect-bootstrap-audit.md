# S24 — adversarial reconnect lifecycle audit

Rol activo: **ADVERSARY**.

Estado: **SUBCONTRATO RECONNECT CERRADO ADVERSARIALMENTE**. Esto no cierra G3.9 completo.

## 1. Por qué los primeros rojos no eran producción

El proof implementer `S24ReconnectLifecycleClientProof` intentaba demostrar dos conexiones sucesivas contra el mismo dedicated server, pero creaba la vaca canónica y arrancaba runtime **antes** de que existiera ningún jugador/conexión.

Runs focales:

- `35120074913`, job `104875319948`: failure en el primer `awaitReady`, antes de la primera desconexión;
- `35120983007`, job `104878391797`: mismo fallo tras retirar la asunción excesiva de que una conexión nueva deba recibir exactamente `trackingGeneration=1`.

El adversario añadió `S24AdversarialReconnectBootstrapDiagnostics`, separando conexión, soporte server, frame autoritativo, canales, tracking vanilla, catálogo, pose y presentación.

Run `35121576945`, job `104880398209`, aisló el fallo exacto:

```text
BOOTSTRAP_STAGE server-cow: canonical cow missing before publication
```

La vaca creada antes de conectar ya no existía cuando `Player0` entró. Por tanto esos rojos medían lifecycle de entidad/chunk en un dedicated world sin jugadores, no reconnect anatómico. Modificar producción para hacerlos verdes habría sido una reparación imaginaria.

## 2. Proof reconnect limpio

`S24AdversarialReconnectLifecycleProof` elimina esa variable ajena:

1. arranca dedicated server;
2. conecta primero el cliente;
3. crea después el soporte cerca del `ServerPlayer`;
4. lo marca persistente y mantiene su chunk forzado durante el hueco entre conexiones;
5. arranca `AnatomyRuntime.startPrepared(...)` con receptor ya presente;
6. demuestra primera sesión READY + pose + `presentationFrame`;
7. cierra la primera conexión;
8. exige desaparición de catálogo y pose/history cliente de la conexión muerta;
9. comprueba que el mismo soporte server-side sobrevive;
10. reconecta y exige mismo epoch/revision/binding cuando corresponde, nuevo `ServerPlayer` y nuevo objeto `AnatomyFrameHistory` cliente.

La primera ejecución compilable y limpia fue run `35122224022`, job `104882563084`: **success**.

## 3. Mutation-kill

La primera campaña incluyó un mutante que eliminaba `clearPoses()` sólo de `reset()`. Ese mutante sobrevivió porque `useLevel(null)` proporciona una segunda ruta legítima de limpieza. Se descartó como **mutante semánticamente equivalente**, no como defecto del proof.

La campaña final reemplazó esa mutación por dos corrupciones observables:

1. **stale catalog mutant**: disconnect deja de ejecutar `session.resetConnection()`;
2. **stale history mutant**: se neutraliza la limpieza real de `poses/frames/staleFrames/receivedAt/evaluators/providers/presentationFrames` en `clearPoses()`, de modo que ni reset ni cambio de nivel pueden retirar el history de la conexión anterior.

Ambos mutantes compilaron antes de ejecutar el proof.

Run final **`35122952451`**:

- `reconnect-lifecycle`, job **`104884976784`**: **success**;
- `stale-catalog-mutation-kill`, job **`104884976614`**: **success**, mutante compiló y murió;
- `stale-history-mutation-kill`, job **`104884976378`**: **success**, mutante compiló y murió.

Build general del mismo snapshot: run **`35122952497`**, job **`104884976215`**: **success**.

## 4. Propiedades demostradas

Queda demostrado adversarialmente para reconnect real que:

- disconnect retira autoridad cliente de la conexión anterior;
- catálogo connection-scoped no sobrevive al cierre del enlace;
- pose/frame histories de la conexión muerta no sobreviven por otra ruta de teardown;
- una segunda conexión al mismo servidor no reutiliza el objeto `ServerPlayer` ni el `AnatomyFrameHistory` del receptor muerto;
- el soporte y binding server-side pueden seguir siendo los mismos sin convertir eso en continuidad causal del receptor;
- el proof es sensible precisamente a las dos corrupciones de cleanup que pretende certificar.

Esto cubre el subcontrato reconnect de FR-080/FR-082/NFR-017. G3.9 sigue abierto por reload/barriers restantes, replay explícito A→B→A y el nuevo RED NFR-011 sobre retención TTL de histories cliente.
