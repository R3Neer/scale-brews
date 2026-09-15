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

Probe adversarial aislado: workflow `s20-adversarial-allocation-probe`, run **35014198225**, HotSpot/Java 25 con warm-up y `ThreadMXBean`.

Medición reproducible del fixture sintético:

- 1 hueso tocado: **936.00 B/evaluación**;
- 16 huesos tocados: **6853.97 B/evaluación**;
- coste marginal observado: **394.53 B por hueso adicional**.

La pendiente observada es aproximadamente lineal, no una explosión superlineal. Sin embargo varios KiB/sample son materialmente relevantes si muchas entidades se evalúan cada tick.

**Estado:** `MEASURED / MATERIAL`; falta cruzar con Grizzly/Gazelle reales y decidir aceptación o reducción.

### S20-PERF-002 — recompilación de keyframes en HOT_TICK

El riesgo inicial quedó eliminado: `bind` compila los frames a estructuras inmutables y `evaluate` no reconstruye mapas completos de deltas.

**Estado:** `PASS estructural`.

### S20-PERF-003 — validación cuadrática operaciones × huesos

El antiguo `poses.values().stream().allMatch(...)` por operación desapareció. La validez se comprueba sobre la pose afectada y el probe de 1→16 huesos no muestra una señal de allocations cuadrática.

Esto no sustituye un probe CPU de 32/64/128/256 operaciones, pero elimina el mutante estructural original.

**Estado:** `PASS estructural + scaling CPU pendiente`.

### S20-PERF-004 — condiciones compuestas

`ALL`/`ANY` siguen evaluándose recursivamente y están acotadas por cardinalidad/profundidad del schema.

**Estado:** `BOUNDED`, coste/allocations de árbol cercano al límite aún sin medir.

### S20-PERF-005 — búsqueda del frame activo

El riesgo inicial de scan lineal quedó eliminado. `CompiledClip.frameAt(...)` usa offsets precompilados y búsqueda binaria del frame activo.

**Estado:** `PASS estructural`.

## 5. Evidencia todavía necesaria antes del cierre

1. **operations CPU scaling**: 32/64/128/256/... operaciones para excluir regresión superlineal accidental;
2. **real-program allocation**: Grizzly Bear y Gazelle 2.1.9, incluyendo al menos un estado ordinario y un estado/clip de alta cardinalidad;
3. **consumer reuse / NFR-009**: N consumidores sobre el mismo authority sample no incrementan `jointEvaluations`;
4. **condition tree**: condición válida cercana a límites con coste conocido;
5. pasada final de frontera sin parsing, reflection ni resource lookup hot.

Los wall-clock absolutos de GitHub Actions no son gate estable. Se prefieren contadores de CPU del hilo, allocations y contadores estructurales con warm-up.

## 6. Mutantes que debe matar la evidencia

- reconstruir una tabla de deltas completa por sample;
- validar todos los huesos después de cada operación;
- resolver resource/program id en cada `evaluate`;
- evaluar una vez por observador en lugar de una vez por authority sample;
- degradar selección temporal a scan lineal de clips/frames;
- introducir caches por sample sin bound con crecimiento de memoria.

## 7. Gate adversarial de rendimiento S20

- [x] frontera PREPARATION/HOT_TICK estructuralmente limpia;
- [x] sin reflexión ni clases client/external en el evaluator hot;
- [ ] NFR-009 revalidado explícitamente para el engine bound final;
- [ ] scaling CPU de operaciones/huesos clasificado;
- [x] búsqueda temporal de keyframes clasificada y no lineal;
- [ ] allocations reales Grizzly/Gazelle medidas y aceptadas o reducidas;
- [ ] condiciones grandes válidas con coste conocido;
- [ ] ninguna regresión observada aplazada sin clasificación;
- [ ] pasada final adversarial sin cambios productivos ni gaps S20 pendientes.
