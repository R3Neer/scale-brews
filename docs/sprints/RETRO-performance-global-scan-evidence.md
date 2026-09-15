# Evidencia adversarial de rendimiento — scans globales en tick estable

Estado: **FAIL confirmado / requiere IMPLEMENTER**.

Rol que produjo la evidencia: **ADVERSARY**. Este documento no autoriza una solución productiva concreta.

Hallazgo relacionado: `PERF-001` de `RETRO-S00-S19-adversarial-performance-audit.md`.

## Snapshot y lane

- commit probado: `76041d6f698e3edd0a3d619db50a2204b043d407`
- workflow: `retro-performance-locality-proof`
- run: `35007136618`
- job: `104509617532` (`steady-tick-locality`)
- resultado: **FAIL causal**
- artifact: `10412401018` (`retro-performance-locality-proof`)
- SHA-256 del artifact: `faa0b8f18726162246b73f11a453e978cdacd5e550d3249cb2a780a85f7be48a`

## Instrumentación

La lane añade sólo tooling/GameTest:

- `CollisionPerformanceProbe`;
- `TestCollisionEntityScanMixin`, mixin únicamente del source set de GameTest;
- `RetroPerformanceLocalityTests`.

El mixin envuelve `ServerLevel#getAllEntities()` **sólo cuando**:

1. el probe está activo; y
2. el stack contiene `io.github.r3neer.scalebrews.platform.Platforms` o `io.github.r3neer.scalebrews.collision.internal.AnatomyRuntime`.

Por tanto no cuenta enumeraciones de Minecraft/GameTest ajenas a la orquestación de Scale. Registra:

- número de enumeraciones globales iniciadas;
- número de elementos realmente consumidos de cada iterable.

No se modifica producción para obtener la evidencia.

## Resultado 1 — runtime anatómico inactivo

Fixture:

1. `AnatomyRuntime.stop(server)`;
2. empezar probe;
3. ejecutar un único `Platforms.tick(level)` estable;
4. terminar probe.

Resultado exacto:

```text
Inactive collision runtime performed full-level entity enumeration during one stable tick: calls=2 visited=0
```

El test exigía `globalEnumerations == 0` y falló.

Interpretación: incluso sin sesión anatómica activa, la orquestación ejecuta dos `level.getAllEntities()` por tick desde las rutas legacy de `Platforms.tick`.

## Resultado 2 — runtime anatómico activo pero catálogo/bindings vacíos

Fixture:

1. `AnatomyRuntime.stop(server)`;
2. `AnatomyRuntime.startPrepared(server, Map.of(), Map.of())` fuera de la ventana medida;
3. empezar probe;
4. ejecutar un único `Platforms.tick(level)` estable;
5. terminar probe.

Resultado exacto:

```text
Active empty anatomy runtime performed full-level entity enumeration during one stable tick: calls=4 visited=0
```

El test exigía `globalEnumerations == 0` y falló.

Interpretación: además de los dos scans legacy de `Platforms.tick`, `AnatomyRuntime.prepare(level)` y `AnatomyRuntime.publish(level)` añaden una enumeración global cada uno aunque la sesión no tenga ningún binding ni entidad anatómica que procesar.

## Clasificación

**Defecto adversarial de rendimiento confirmado.**

No se clasifica como fallo literal de NFR-008, porque NFR-008 prohíbe el scan global por *movimiento/query* y la evidencia aquí pertenece a `HOT_TICK`. Sí es una violación de la localidad de trabajo esperable del motor y un riesgo directo para NFR-014: el coste basal de Scale depende del total de entidades del nivel incluso cuando el conjunto de participantes relevantes es vacío.

La lectura estática y la evidencia dinámica son coherentes:

- runtime inactivo → 2 scans/tick;
- runtime activo → 4 scans/tick.

`visited=0` en esta primera lane significa que el nivel de prueba no contenía entidades consumidas durante la ventana manual medida. Confirma la estructura de scans, pero todavía no cuantifica la pendiente respecto al número de entidades. Una segunda lane debe poblar el nivel con entidades irrelevantes y registrar el crecimiento de `entitiesVisited`.

## Contrato adversarial, no solución prescrita

El adversario **no exige** una técnica concreta como requisito de implementación. Son soluciones potenciales, entre otras:

- registries/sets de participantes por nivel;
- hooks de add/remove/tracking;
- dirty sets;
- índices existentes;
- fan-out desde estados de contacto conocidos;
- eliminación de rutas legacy cuando su gate de migración lo permita.

Lo que sí exige la evidencia es la propiedad observable:

> En régimen estable, añadir entidades completamente ajenas al subsistema no debe aumentar linealmente el trabajo de orquestación de Entity Collisions.

La solución productiva pertenece al **IMPLEMENTER**. El adversario conservará la lane y la ampliará con scaling por entidades irrelevantes antes de aceptar el cierre del retro de rendimiento.
