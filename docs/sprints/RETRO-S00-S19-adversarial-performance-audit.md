# Auditoría adversarial retroactiva de rendimiento — S00–S19

Estado: **OPEN / segunda lectura en curso**.

Rol: **ADVERSARY**. Este documento registra hallazgos, evidencia y huecos de rendimiento. No autoriza cambios productivos desde el rol adversario.

Protocolo: `docs/ENTITY_COLLISIONS_ADVERSARIAL_PERFORMANCE.md`.

Requisitos normativos principales: **NFR-007..014, NFR-027**, además de los NFR de lifecycle/compatibilidad que condicionan cualquier optimización.

Snapshot inicial auditado: `f9099f745ea68ad20a2aa9abc075869e5d88c1f1`.

---

## 1. Objetivo

Revisar retroactivamente los hot paths construidos antes de que el rendimiento se convirtiera en una obligación adversarial transversal. La auditoría distingue cinco estados, que no deben confundirse:

- **BOUNDED PASS**: existe una cota explícita y observable de trabajo/memoria;
- **LOCALITY PASS**: el trabajo hot depende de la región/candidatos relevantes y no del mundo completo;
- **REUSE PASS**: trabajo caro ya calculado se comparte según la identidad causal requerida;
- **MEASURED PASS/FAIL**: existe medición temporal/allocation/memoria reproducible;
- **UNMEASURED / PERF-RISK**: el código es funcionalmente correcto o acotado, pero la evidencia de coste es insuficiente.

Un `BOUNDED PASS` no implica `MEASURED PASS`.

---

## 2. Hallazgos de la primera lectura

### PERF-001 — múltiples scans globales por nivel y tick

**Severidad inicial:** alta.  
**Estado:** `PERF-RISK / UNMEASURED`.  
**Superficie:** `HOT_TICK`.

`Platforms.tick(ServerLevel)` ejecuta en cada `END_LEVEL_TICK`:

1. `AnatomyRuntime.prepare(level)`;
2. `AnatomyMovement.tick(level)`;
3. `AnatomyRuntime.publish(level)`;
4. `MaterialPhysicsRuntime.drain(level)`;
5. un `level.getAllEntities()` para `noteSupport`;
6. otro `level.getAllEntities()` para carry/networking legacy.

A su vez:

- `AnatomyRuntime.prepare(level)` recorre `level.getAllEntities()` para descubrir bindings nuevos;
- `AnatomyRuntime.publish(level)` recorre `level.getAllEntities()` para publicar contacto por body.

Por tanto el estado actual realiza **al menos cuatro recorridos globales del nivel por tick** antes de contar cualquier traversal interno adicional.

Esto no es todavía un FAIL formal de NFR-008, porque NFR-008 prohíbe específicamente el scan global *por movimiento/query* y estas rutas son orchestration/tick. Sí es un riesgo directo para NFR-014 y una pérdida clara de locality global que debe medirse y, si es material, volver al IMPLEMENTER.

**Evidencia requerida:** workload con número creciente de entidades irrelevantes manteniendo constantes soportes/contactos activos; medir tiempo del subsistema y/o contador de entidades visitadas. La pendiente no puede inferirse únicamente de wall-clock total del servidor.

---

### PERF-002 — snapshot global de providers por cada nivel/tick

**Severidad inicial:** media-alta.  
**Estado:** `PERF-RISK / UNMEASURED`.  
**Superficie:** `HOT_TICK`.

`AnatomyMovement.tick(level)` obtiene `AnatomyBindingState.providersSnapshot()`. Esa operación crea un `IdentityHashMap` nuevo y copia **todos** los providers registrados globalmente; después `tick(level)` filtra por `entry.getKey().level()==level`.

Con varias dimensiones, el trabajo/copia tiende a `niveles × providers globales`, aunque cada nivel sólo necesite su subconjunto.

**Evidencia requerida:** contador de entries copiadas por level/tick frente a supports realmente pertenecientes a ese level; escenario multidimensional. Revisar también `tickGeometry` y rebuilds de índice que vuelvan a pedir el snapshot.

---

### PERF-003 — locking global en el índice espacial

**Severidad inicial:** media.  
**Estado:** `PERF-RISK / UNMEASURED`.  
**Superficie:** `HOT_MOVE_QUERY`, `HOT_TICK`.

`AnatomySpatialIndex` usa un `Collections.synchronizedMap(new WeakHashMap<>())` y, además, métodos `static synchronized` para `current`, `rebuild`, `upsertIfCurrent`, `removeIfCurrent`, `queryIfCurrent` y `deactivate`.

Consecuencia arquitectónica: todos los niveles comparten el monitor de clase aunque sus índices sean independientes. En el servidor actual gran parte del trabajo físico ocurre en world thread, por lo que no se clasifica aún como defecto productivo, pero debe comprobarse que client/presentation o futuras rutas concurrentes no introduzcan contention innecesaria.

**Evidencia requerida:** profiling/lock evidence si existen llamadas concurrentes; si todo el acceso sigue single-thread, documentar el coste real y dejarlo como mantenibilidad/riesgo futuro en vez de forzar una optimización sin señal.

---

### PERF-004 — allocation churn por celda en broadphase

**Severidad inicial:** media-alta.  
**Estado:** `BOUNDED PASS + PERF-RISK / allocation UNMEASURED`.  
**Superficie:** `HOT_MOVE_QUERY`.

`MaterialBroadphase.query()` está localmente acotado por `maxQueryCells` y `maxCandidates`, pero por query crea estructuras temporales (`IdentityHashMap`/set, `ArrayList`, pequeños arrays de contadores) y `forEachCell()` construye un record `Cell` por cada celda visitada para consultar el `HashMap`.

El runtime actual permite hasta **4096 celdas por query**, por lo que una query válida de borde puede generar miles de objetos `Cell` efímeros además de las colecciones auxiliares. La JIT puede eliminar parte de ese churn, pero no se asume sin evidencia.

**Evidencia requerida:** allocation profile o benchmark que compare queries típicas y de presupuesto alto; registrar `requestedCells`, `cellsVisited`, `candidatesVisited`, bytes/allocations si la herramienta lo permite y GC bajo workload prolongado.

**Estado semántico:** locality/bounds del broadphase están bien diseñados; este hallazgo no propone sustituir el índice por scan global.

---

### PERF-005 — pose/geometría: reuse fuerte, object churn pendiente de medir

**Severidad inicial:** media.  
**Estado:** `REUSE PASS provisional + allocation UNMEASURED`.  
**Superficie:** `HOT_TICK`, `HOT_PRESENTATION`.

`ModelGeometryProvider` mantiene:

- snapshot cacheado por `Key` causal;
- dos endpoints de joints por entidad;
- trayectoria interpolada por segmento;
- contadores `evaluations` y `jointEvaluations`;
- constructor runtime con `PoseEngine.Bound`, evitando lookup de resources en hot path.

`tick()` precalienta joints y `motionBetween()` vuelve a consultar esos endpoints, por lo que la arquitectura parece compatible con NFR-009: el mismo joint sample puede reutilizarse por consumidores/queries.

Pendiente:

- demostrar con N observadores/queries que `jointEvaluations` no escala con N;
- medir churn de `Sample`, `Key`, `Matrix4f`, `LinkedHashMap`, `Snapshot` y convexes trasladados cuando sí cambia el frame;
- comprobar que el cache key no invalida de más por valores equivalentes.

---

### PERF-006 — interval queues y receipts están explícitamente acotados

**Severidad:** informativa.  
**Estado:** `BOUNDED PASS`.

- `MaterialIntervalRuntime.MAX_PENDING_INTERVALS = 512`, con `SATURATED` sticky hasta consumo y fail-closed localizado;
- `AnatomyTransportReceipts.HISTORY_TICKS = 40`;
- `AnatomyTransportReceipts.MAX_RECEIPTS_PER_TICK = 16` por body/recipient history;
- histories se podan por TTL y disconnect/clear tienen cleanup explícito.

Pendiente de NFR-011: soak/memory evidence que demuestre estabilización real y ausencia de retención accidental por mapas/listeners.

---

### PERF-007 — catálogo preconstruido/reutilizado por revisión

**Severidad:** informativa.  
**Estado:** `REUSE PASS provisional`.

`WorldAnatomyCatalog` construye `AnatomyCatalogTransfer.PreparedBundle` al aceptar/reemplazar snapshot. `AnatomyRuntime.reset()` fuerza la materialización del packet list antes del primer jugador y `AnatomyNetworking.sendCatalog()` envía la lista ya preparada.

La ruta observada coincide con NFR-010: añadir receptores no debería rehacer serialización/hash/fragmentación del catálogo.

Pendiente: contador explícito de bundle builds vs recipients y bytes/packets, tal como exige NFR-027.

---

### PERF-008 — CCD está acotado; separation tiene peor caso muy amplio

**Severidad inicial:** media-alta.  
**Estado:** `BOUNDED PASS + worst-case UNMEASURED`.

`ConservativeSweep.query()` exige `1 <= maxIterations <= 4096` y publica `ITERATION_LIMIT` al agotarse.

`AnatomySeparation.resolve()` exige `budget <= 4096`, pero su frontier permite `queue.size() < budget * 32`, es decir hasta **131 072** offsets pendientes cuando `budget=4096`. Dentro del loop crea listas filtradas y vectores candidatos. La cota evita trabajo infinito, pero no demuestra que el peor caso cumpla el presupuesto temporal de NFR-014.

**Evidencia requerida:** fixture adversarial que fuerce candidate explosion sin depender de geometría corrupta; registrar `candidates`, queue high-water mark mediante test tooling si es necesario, tiempo y allocations. Distinguir recovery excepcional de ruta habitual, pero no aceptar un freeze largo sólo porque termina eventualmente.

---

### PERF-009 — clustering de soportes simultáneos es cuadrático pero acotado

**Severidad inicial:** baja-media.  
**Estado:** `BOUNDED PASS / scaling UNMEASURED`.

`MaterialPhysicsRuntime.jointClusters()` construye componentes conexas comparando envelopes con un loop interno sobre el input, por lo que el clustering es O(n²) en número de intervalos simultáneos del cadence. La cola total está acotada por 512 y el workload objetivo NFR-014 usa 64 soportes.

No se clasifica como bug sin medición: para n=64 son unas pocas miles de comparaciones AABB. Sí debe quedar incluido en el benchmark integrado y en un scaling probe 8/16/32/64/128 cuando sea posible.

---

## 3. S19 — clasificación específica

`AdvancedModelBoxGeometryEngine` / `AdvancedModelBoxGeometryExtractor` se clasifican como **PREPARATION**.

Observado:

- registry de sources: máximo 4096;
- parts: máximo 512;
- primitives: máximo 4096;
- depth: máximo 64;
- extracción reflectiva estricta del dialecto fijado;
- dos modelos frescos + dos extracciones completas por `prepareDetailed()` para verificar determinismo;
- `PoseStack`, matrices, arrays y reflexión por part/cube durante extracción;
- no se ha observado esa reflexión dentro de `AnatomyMovement`, `MaterialPhysicsRuntime`, `ModelGeometryProvider` ni networking runtime;
- `ScaleBrewsClient` sólo instala el delegate en inicialización; el common dispatcher no enlaza clases client/Alex.

Conclusión provisional: el coste pesado de S19 es aceptable como preparación **siempre que siga fuera del hot loop**. S19 no necesita inventarse un objetivo de microsegundos runtime para una ruta que no corre durante física. Su gate de rendimiento es demostrar frontera de preparación, bounds y ausencia de linking/reflection/re-extraction por tick/query.

Pendiente antes de cerrar S19:

- búsqueda/inspección final de callers de `GeometryEngine.prepare` para demostrar que no se ejecuta desde HOT_TICK/HOT_MOVE_QUERY;
- si existe preparación runtime por reload, medir sólo esa operación como reload/preparation y demostrar que el resultado queda materializado para uso posterior;
- añadir la clasificación PREPARATION y esta evidencia al cierre S19.

---

## 4. Gaps de instrumentación detectados

NFR-027 exige un snapshot más rico del que hoy se ve disperso. Existen métricas parciales de sweeps, material runtime y receipts, pero falta una vista única y, en esta primera lectura, no se ha demostrado todavía que exponga de forma directa:

- supports activos por level;
- pose/joint evaluations por periodo;
- broadphase requested/visited cells y candidates de forma agregada;
- cache hits/misses del `ModelGeometryProvider`;
- bytes/packets de catálogo/pose/contacto;
- queue/high-water marks de intervals/separation;
- allocations/GC o un proxy reproducible de churn.

Esto no implica que todas esas métricas deban permanecer como API de producción. Pueden existir como instrumentation/test seam mientras permitan ejecutar NFR-027 y el benchmark.

---

## 5. Siguiente pasada adversarial

Orden de trabajo:

1. medir/contar los scans globales y demostrar su scaling con entidades irrelevantes;
2. demostrar NFR-009 con N observers/queries y contadores existentes;
3. medir broadphase allocations/locality y validar que entidades lejanas no elevan candidatesVisited;
4. stress de separation/CCD cerca de sus presupuestos;
5. soak de histories/receipts/queues y memory stabilization;
6. fan-out de networking y bundle reuse;
7. benchmark integrado NFR-014 sobre snapshot candidato;
8. repetir segunda lectura hasta que no aparezcan nuevos riesgos ni cambios productivos pendientes.

Mientras `PERF-001`, `PERF-002`, `PERF-004`, `PERF-005` y `PERF-008` sigan sin medir/clasificar, la **auditoría retroactiva de rendimiento permanece OPEN**, aunque los sprints históricos continúen funcionalmente cerrados.