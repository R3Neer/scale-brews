# S20 — Modelo adversarial de rendimiento para Citadel pose

Estado: **OPEN**.  
Rol: **ADVERSARY**.  
Protocolo transversal: `docs/ENTITY_COLLISIONS_ADVERSARIAL_PERFORMANCE.md`.

Este documento no redefine la semántica de S20. Registra el modelo de coste y la evidencia actual del hot path `CitadelPoseProgramEvaluator`.

## 1. Clasificación de rutas

- `CitadelPoseEngine.bind(...)`: **PREPARATION**. Puede validar programa, resolver resources, comprobar huesos/channels y compilar representación runtime.
- `PoseEngine.Bound.evaluate(...)` y `CitadelPoseProgramEvaluator.evaluate(...)`: **HOT_TICK**.
- extracción/reflexión sobre Citadel/Alex: tooling/preparation; no puede reaparecer en HOT_TICK.

NFR-009 sigue siendo obligatorio: una pose física del soporte se evalúa como máximo una vez por authority sample/joint tick y el resultado se reutiliza.

## 2. Límites admitidos por schema

- `C <= 128` clips;
- `F <= 4096` keyframes;
- `D <= 8192` deltas;
- `M <= 2048` operaciones;
- hasta `512` deltas por keyframe;
- hasta `64` huesos en `FACE_TARGET`;
- condiciones de hasta `32` términos por nodo y profundidad `<= 16`;
- hasta `64` channels requeridos.

**Gap adversarial descubierto el 2026-09-16:** los límites de condición son locales por nodo/profundidad, pero no existe un presupuesto global de nodos de condición por programa ni por operación. Por tanto el coste HOT_TICK sigue siendo finito en sentido matemático, pero no está acotado por un límite práctico explícito del schema. S20 no debe cerrar con PERF-004 como `BOUNDED` hasta que exista y se pruebe ese presupuesto global.

## 3. Frontera preparation/runtime

Segunda lectura actual:

- lookup de `CitadelPoseProgram`: sólo en `bind`;
- validación source/version/bones/channels: `bind`;
- evaluator common sin clases Citadel/Alex/render;
- clips compilados una vez a `CompiledClip`/`CompiledFrame`;
- selección temporal mediante offsets precompilados y búsqueda binaria;
- no hay parsing/reflection/resource lookup dentro de `evaluate`.

**Estado:** `PASS estructural`, pendiente únicamente de mantenerlo en la pasada final.

## 4. Riesgos hot y evidencia

### S20-PERF-001 — asignaciones por evaluación

El evaluator sigue materializando por sample:

- `LinkedHashMap<String, MutablePose>`;
- `LinkedHashMap<String, Matrix4f>` de salida;
- un `MutablePose` por hueso tocado;
- `SourcePose`/`Matrix4f` al emitir matrices.

Probe adversarial aislado: workflow `s20-adversarial-allocation-probe`, HotSpot/Java 25 con warm-up y `ThreadMXBean`.

Fixture sintético, run **35014967562**:

- 1 hueso tocado: **936.00 B/evaluación**;
- 16 huesos tocados: **6849.44 B/evaluación**;
- coste marginal observado: **394.23 B por hueso adicional**.

Programas reales fijados en el mismo run:

- Gazelle, 22 operaciones / 3 clips / 10 huesos referenciados: **3898.91 B/evaluación** ordinaria y **4184.00 B/evaluación** para el clip de mayor unión de huesos medido;
- Grizzly Bear, 57 operaciones / 4 clips / 8 huesos referenciados: **5172.53 B/evaluación** ordinaria y **5448.00 B/evaluación** para el clip medido.

La pendiente observada es aproximadamente lineal, no una explosión superlineal. Varios KiB/sample siguen siendo deuda material de asignación, pero el workload real medido queda en el mismo orden que el fixture sintético y NFR-009 evita multiplicarlo por consumidores/root rebuilds del mismo endpoint causal.

**Estado:** `MEASURED / MATERIAL / ACCEPTED AS S20 BASELINE`; vigilar como deuda de rendimiento, sin señal actual de crecimiento superlineal.

### S20-PERF-002 — recompilación de keyframes en HOT_TICK

El riesgo inicial quedó eliminado: `bind` compila los frames a estructuras inmutables y `evaluate` no reconstruye mapas completos de deltas.

**Estado:** `PASS estructural`.

### S20-PERF-003 — escalado CPU con operaciones

El antiguo `poses.values().stream().allMatch(...)` por operación desapareció. La validez se comprueba sobre la pose afectada y el probe de allocations no muestra señal cuadrática respecto a huesos tocados.

Probe aislado `s20-adversarial-cpu-scaling-probe`, run **35059646199**, Java 25, CPU del hilo tras warm-up. Todas las operaciones tocan un único hueso para aislar el término `M`:

| Operaciones | ns/evaluación | ns/operación |
|---:|---:|---:|
| 32 | 1151.84 | 35.9948 |
| 64 | 1561.52 | 24.3988 |
| 128 | 2686.13 | 20.9854 |
| 256 | 6442.14 | 25.1646 |
| 512 | 11633.27 | 22.7212 |
| 1024 | 23164.34 | 22.6214 |
| 2048 | 45989.92 | 22.4560 |

El workload aumenta ×64 entre 32 y 2048 operaciones mientras el tiempo medido aumenta ×39.928. A partir de 128 operaciones el coste normalizado permanece aproximadamente en 21–25 ns/operación, sin señal de una regresión superlineal accidental.

**Estado:** `PASS / MEASURED`; scaling aproximadamente lineal con `M`.

### S20-PERF-004 — condiciones compuestas

`ALL`/`ANY` se evalúan recursivamente en HOT_TICK. Cada nodo admite hasta 32 términos y la profundidad está limitada a 16, pero actualmente **no hay límite total de nodos**. El constructor valida profundidad y cardinalidad local; `requiredChannels()` y el evaluator vuelven a recorrer recursivamente el árbol completo.

El mismo run **35059646199** midió árboles válidos que fuerzan recorrido completo (`ALL` con leaves true):

| Profundidad del fixture | Nodos | ns/evaluación | ns/nodo |
|---:|---:|---:|---:|
| 0 | 1 | 289.66 | 289.6612 |
| 1 | 33 | 538.25 | 16.3105 |
| 2 | 1057 | 9232.50 | 8.7346 |
| 3 | 33825 | 295741.31 | 8.7433 |

El tramo grande también escala de forma aproximadamente lineal, pero eso precisamente demuestra el problema: un único árbol de **33825 nodos**, perfectamente válido con el schema actual y muy lejos de la profundidad máxima 16, cuesta ya alrededor de **0.296 ms por evaluación** sólo en condiciones. El probe confirmó además `explicit_global_node_limit=false`.

**Estado:** `BLOCKER / MEASURED / GLOBAL BOUND MISSING`. La solución debe introducir un presupuesto total explícito y verificable de nodos de condición, no sólo confiar en profundidad/cardinalidad local ni en el tamaño accidental del payload.

### S20-PERF-005 — búsqueda del frame activo

El riesgo inicial de scan lineal quedó eliminado. `CompiledClip.frameAt(...)` usa offsets precompilados y búsqueda binaria del frame activo.

**Estado:** `PASS estructural`.

### S20-PERF-006 — reutilización de joints / NFR-009

La evidencia existente de `ModelGeometryProvider` ya cubre el contrato de reutilización en la capa que posee el cache, independientemente del engine concreto:

- un cambio sólo de root reconstruye convexos pero mantiene una sola evaluación articular;
- `motionBetween(...)` reutiliza los mismos joints inmutables del endpoint;
- un input autoritativo distinto incrementa exactamente una vez `jointEvaluations`.

No se duplica el mismo test sustituyendo el engine por Citadel porque no añadiría una frontera causal nueva: el cache está por debajo de la selección concreta de pose engine.

**Estado:** `PASS`.

## 5. Evidencia todavía necesaria antes del cierre

1. **condition tree global bound**: añadir un presupuesto global de nodos verificable y un holdout que rechace `limit + 1` atómicamente;
2. pasada final de frontera sin parsing, reflection ni resource lookup hot;
3. revalidar los workflows funcionales S20 sobre la misma línea de commits después de la reparación de I9 y del bound de condiciones.

Evidencia ya satisfecha:

- allocations reales Grizzly/Gazelle: run `35014967562`;
- operations CPU scaling: run `35059646199`;
- condition-tree cost baseline: run `35059646199`;
- consumer reuse / NFR-009: cobertura existente de cache causal de joints;
- selección temporal de keyframes: implementación binaria precompilada.

Los wall-clock absolutos de GitHub Actions no son gate estable. Se prefieren contadores de CPU del hilo, allocations y contadores estructurales con warm-up.

## 6. Mutantes que debe matar la evidencia

- reconstruir una tabla de deltas completa por sample;
- validar todos los huesos después de cada operación;
- resolver resource/program id en cada `evaluate`;
- evaluar una vez por observador en lugar de una vez por authority sample;
- degradar selección temporal a scan lineal de clips/frames;
- introducir caches por sample sin bound con crecimiento de memoria;
- aceptar un árbol de condiciones por encima del presupuesto global definido.

## 7. Gate adversarial de rendimiento S20

- [x] frontera PREPARATION/HOT_TICK estructuralmente limpia;
- [x] sin reflexión ni clases client/external en el evaluator hot;
- [x] NFR-009 revalidado en la capa propietaria del cache causal;
- [x] scaling CPU de operaciones clasificado;
- [x] búsqueda temporal de keyframes clasificada y no lineal;
- [x] allocations reales Grizzly/Gazelle medidas y clasificadas;
- [ ] presupuesto global de nodos de condición impuesto y probado en `limit + 1`;
- [ ] ninguna regresión observada aplazada sin clasificación;
- [ ] pasada final adversarial sin cambios productivos ni gaps S20 pendientes.
