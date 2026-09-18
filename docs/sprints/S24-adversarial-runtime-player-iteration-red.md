# S24 — adversarial runtime player iteration RED

Rol: **ADVERSARY**.

Estado: **HISTÓRICO RED → CERRADO TRAS REVALIDACIÓN ADVERSARIAL**.

## Hallazgo inicial en ordinary

Al registrar `S24RuntimeReloadTests` en la suite server ordinaria, el build general dejó de ser verde:

- run `35128115871`;
- job `104902093787`;
- los tests live de reload acababan en `java.util.ConcurrentModificationException` desde `AnatomyRuntime.reset(...)`.

El rojo aparecía acompañado de varios mock players que eran rechazados por protocolo anatómico incompatible. Como una suite completa tiene bastante fauna, ese resultado todavía permitía una explicación cómoda basada en contaminación o concurrencia entre tests.

## Holdout mínimo

Para eliminar esa explicación se añadió un único GameTest aislado:

`S24AdversarialRuntimePlayerIterationTests.resetMustTolerateMultipleRecipientsRejectedDuringCatalogBootstrap`

Fixture:

1. crea exactamente dos `ServerPlayer` mock dentro del mismo nivel;
2. verifica que ambos están en `server.getPlayerList().getPlayers()`;
3. ejecuta un único `AnatomyRuntime.startPrepared(server, Map.of(), Map.of())`;
4. no ejecuta ningún otro test S24 ni ningún reload paralelo.

Commit de test: `ba4a82bd0f98904bd669523d4da13e7043c705a2`.
Workflow aislado: `.github/workflows/s24-adversarial-runtime-player-iteration.yml`, commit `f447c5d6834afc0f080173286d299ed0ed7f2296`.

Resultado:

- run `35128505094`;
- job `104903389902`;
- **failure**;
- aserción: `Runtime reset mutated the live player list while iterating recipients during catalog bootstrap`;
- causa encadenada: `java.util.ConcurrentModificationException` en `java.util.ArrayList$Itr.next`, llamada desde `AnatomyRuntime.reset(AnatomyRuntime.java:126)`.

El log muestra que los dos recipients son incompatibles y que `catalog(...)` inicia su desconexión antes de que el iterador vivo de `reset()` termine.

## Causa de producción

`AnatomyRuntime.reset(...)` termina con una iteración directa sobre la lista viva de jugadores:

```java
for (var player : server.getPlayerList().getPlayers()) catalog(state, player);
```

A su vez `catalog(...)` puede ejecutar:

```java
player.connection.disconnect(...);
```

si el recipient no anuncia los payloads anatómicos requeridos.

La desconexión modifica la misma colección de `ServerPlayer` que el enhanced-for está recorriendo. En cuanto el iterador vuelve a avanzar, detecta el cambio de `modCount` y lanza `ConcurrentModificationException`.

El holdout aislado demuestra que esto no depende de los tres tests de reload ejecutándose juntos ni del orden de la suite ordinary.

## Impacto

El defecto afecta a cualquier bootstrap/reset/reload que intente publicar el catálogo a múltiples recipients y donde al menos uno pueda ser expulsado por incompatibilidad durante esa publicación. Además de abortar el reset, el fallo ocurre después de que parte del lifecycle ya haya sido mutado, por lo que no debe tratarse como un mero problema cosmético del runner.

Es especialmente relevante para la robustez de lifecycle y para NFR-031: una lane focal con un solo recipient había ocultado el defecto, mientras que la suite ordinaria lo hizo observable.

## Restricciones de la reparación

No se prescribe una implementación concreta. La corrección debe demostrar simultáneamente que:

- procesar un recipient incompatible puede desconectarlo sin invalidar la enumeración de los demás recipients;
- ningún `ConcurrentModificationException` puede abortar `startPrepared`, `reset` o un reload por este motivo;
- los recipients restantes siguen siendo procesados según sus capabilities;
- no se obtiene el verde simplemente suprimiendo la desconexión requerida para clientes incompatibles;
- el cambio no altera la semántica causal de catálogo/reload ya certificada;
- la suite ordinary con `S24RuntimeReloadTests` vuelve a pasar sin retirar esos tests del gate.

Iterar una snapshot estable de recipients es una posible solución, pero el holdout exige comportamiento, no arquitectura.

## Gate de cierre

El defecto no está cerrado hasta que:

1. `S24AdversarialRuntimePlayerIterationTests` pase sin modificar su expectativa;
2. el build ordinary que incluye `S24RuntimeReloadTests` vuelva a verde;
3. las lanes focales/mutation-kill de reload permanezcan verdes.

## Resolución posterior

Producción dejó de iterar la lista viva de jugadores durante `AnatomyRuntime.reset(...)` y pasó a recorrer una snapshot estable antes de llamar a `catalog(...)`, permitiendo que un recipient incompatible sea desconectado sin invalidar la enumeración.

El holdout aislado original se reejecutó sin modificar su expectativa en workflow `s24-player-iteration-revalidation`, run **`35131477985`**: **success**.

**Clasificación vigente:** el `ConcurrentModificationException` de bootstrap/reset está cerrado. No participa en el blocker S24 actual.

