# Evidencia adversarial de rendimiento — scans globales en tick estable

Estado: **FAIL confirmado / requiere IMPLEMENTER**.

Rol que produjo la evidencia: **ADVERSARY**. Este documento no autoriza una solución productiva concreta.

Hallazgo relacionado: `PERF-001` de `RETRO-S00-S19-adversarial-performance-audit.md`.

## 1. Instrumentación

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

## 2. Primera confirmación: existencia de scans en tick estable

Snapshot:

- commit probado: `76041d6f698e3edd0a3d619db50a2204b043d407`;
- workflow: `retro-performance-locality-proof`;
- run: `35007136618`;
- job: `104509617532` (`steady-tick-locality`);
- resultado: **FAIL causal**;
- artifact: `10412401018` (`retro-performance-locality-proof`);
- SHA-256 del artifact: `faa0b8f18726162246b73f11a453e978cdacd5e550d3249cb2a780a85f7be48a`.

Resultado exacto con nivel de fixture sin entidades consumidas dentro de la ventana medida:

```text
runtime inactivo: calls=2 visited=0
runtime anatómico activo con catálogo/bindings vacíos: calls=4 visited=0
```

Interpretación:

- incluso sin sesión anatómica activa, la orquestación ejecuta dos `level.getAllEntities()` por tick desde las rutas legacy de `Platforms.tick`;
- con sesión anatómica activa, `AnatomyRuntime.prepare(level)` y `AnatomyRuntime.publish(level)` añaden una enumeración global cada uno aunque no exista ningún binding/participante anatómico que procesar.

Esta primera ejecución confirmó la estructura de scans, pero no medía todavía la pendiente respecto al número de entidades.

## 3. Confirmación limpia de scaling lineal

Se amplió el fixture para añadir 64 `Pig` sin IA ni gravedad, sin bindings anatómicos y completamente irrelevantes para una sesión vacía. Un intento intermedio con cuatro GameTests separados mostró contaminación entre fixtures porque Fabric los ejecutaba en el mismo nivel/batch; esa ejecución **no se usa como medición cuantitativa**.

El fixture final consolida las cuatro observaciones secuencialmente dentro de **un único GameTest**, sobre el mismo nivel y sin concurrencia entre fixtures.

Snapshot limpio:

- commit probado: `0d02a9c93ef1fa408dac2c27db3724821075326e`;
- workflow: `retro-performance-locality-proof`;
- run: `35008268681`;
- job: `104513434615` (`steady-tick-locality`);
- resultado: **FAIL causal**;
- artifact: `10412556547` (`retro-performance-locality-proof`);
- SHA-256 del artifact: `3447ca20dff15193c903ac116ce869324bd77cc39c89f9084feb7949a51d01b8`.

Mensaje exacto del holdout:

```text
Steady collision orchestration is not local to relevant participants.
empty[inactive=calls=2,visited=0, active=calls=4,visited=0];
64 irrelevant living entities[
  inactive=calls=2,visited=128 fullScanFloor=128,
  active=calls=4,visited=256 fullScanFloor=256
]
```

Por tanto, con exactamente 64 entidades vivas irrelevantes:

| Estado | enumeraciones globales/tick | entidades visitadas/tick |
|---|---:|---:|
| runtime anatómico inactivo | 2 | 128 = 2 × 64 |
| runtime anatómico activo vacío | 4 | 256 = 4 × 64 |

La pendiente es lineal y coincide exactamente con el número de scans observado. No es un efecto de timings de CI, GC, JIT ni ruido del runner: es trabajo estructural contado directamente.

## 4. Clasificación

**Defecto adversarial de rendimiento confirmado.**

No se clasifica como fallo literal de NFR-008, porque NFR-008 prohíbe el scan global por *movimiento/query* y la evidencia aquí pertenece a `HOT_TICK`. Sí es una violación de la localidad de trabajo esperable del motor y un riesgo directo para NFR-014: el coste basal de Scale crece con el total de entidades del nivel incluso cuando el conjunto de participantes relevantes es vacío.

La evidencia dinámica confirma la lectura estática:

- runtime inactivo → 2 scans globales/tick;
- runtime activo → 4 scans globales/tick;
- población irrelevante `N` → al menos `2N` visitas/tick inactivo y `4N` visitas/tick activo en estas rutas.

Con el fixture de `N=64`, el contador obtiene exactamente 128 y 256 visitas respectivamente.

## 5. Contrato adversarial, no solución prescrita

El adversario **no exige** una técnica concreta como requisito de implementación. Son soluciones potenciales, entre otras:

- registries/sets de participantes por nivel;
- hooks de add/remove/tracking;
- dirty sets;
- índices existentes;
- fan-out desde estados de contacto conocidos;
- eliminación de rutas legacy cuando su gate de migración lo permita.

Lo que sí exige la evidencia es la propiedad observable:

> En régimen estable, añadir entidades completamente ajenas al subsistema no debe aumentar linealmente el trabajo de orquestación de Entity Collisions.

La solución productiva pertenece al **IMPLEMENTER**. La lane `retro-performance-locality-proof` debe conservarse roja hasta que la producción recupere esa propiedad, y después convertirse en regresión permanente verde.
