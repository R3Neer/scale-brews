# S20 — Modelo adversarial de rendimiento para Citadel pose

Estado: **OPEN**.  
Rol: **ADVERSARY**.  
Protocolo transversal: `docs/ENTITY_COLLISIONS_ADVERSARIAL_PERFORMANCE.md`.

Este documento no redefine la semántica de S20. Registra el modelo de coste que el adversario usará para impedir que un `PoseEngine` correcto semánticamente se convierta en un hot path desproporcionado.

## 1. Clasificación de rutas

- `CitadelPoseEngine.bind(...)`: **PREPARATION**. Puede validar programa, resolver resources, comprobar huesos/channels y construir una representación runtime.
- el `PoseEngine.Bound` retornado por `bind`, `CitadelPoseProgramEvaluator.evaluate(...)` y todo lo que ejecuten por authority sample: **HOT_TICK**.
- la extracción/reflexión sobre clases Citadel/Alex sigue perteneciendo a tooling/preparation S19 y **NO PUEDE** reaparecer aquí.

NFR-009 sigue siendo obligatorio: una pose física del soporte se evalúa como máximo una vez por authority sample/joint tick y se reutiliza. Ese reuse reduce el multiplicador de consumidores, pero **no convierte una evaluación cara en aceptable**.

## 2. Dimensiones de escalado aceptadas por el schema

El programa actualmente permite, como máximos globales:

- `C <= 128` clips;
- `F <= 4096` keyframes;
- `D <= 8192` deltas de keyframe;
- `M <= 2048` operaciones procedurales;
- hasta `512` deltas por keyframe;
- hasta `64` huesos en una operación `FACE_TARGET`;
- condiciones compuestas de hasta `32` términos por nodo y profundidad `<= 16`;
- hasta `64` channels requeridos.

Por tanto el adversario debe probar coste con programas grandes **válidos**, no sólo con los fixtures mínimos del implementador.

## 3. Lo que ya está bien en la frontera

En la primera lectura del snapshot `4af19d96c2c8f299e8a1db7a645ca51eb1b1059d`:

- el lookup de `CitadelPoseProgram` ocurre en `bind`, no en `evaluate`;
- la validación de source/version y huesos ocurre en `bind`;
- el evaluator common no importa clases Citadel/Alex/render;
- el runtime recibe un programa neutral y server-safe;
- el mapa clip-id → clip se materializa una vez en `bind`.

Estos puntos son **PASS provisional de frontera preparación/runtime**. Deben mantenerse durante el sprint.

## 4. Riesgos hot detectados

### S20-PERF-001 — colecciones y matrices por evaluación

Cada `evaluate` crea actualmente:

- un `LinkedHashMap<String, MutablePose>`;
- un `LinkedHashMap<String, Matrix4f>` de resultado;
- un `Map.copyOf(result)`;
- un `MutablePose` por hueso tocado;
- una `SourcePose` + `Matrix4f` por hueso emitido.

**Estado:** `allocation UNMEASURED`.

El adversario medirá bytes/allocations por evaluación típica y de alta cardinalidad. No se presupone que el JIT elimine estas asignaciones.

### S20-PERF-002 — reconstrucción de deltas de keyframe dentro de HOT_TICK

`applyClip()` llama `deltas(frame)` mientras recorre los keyframes. `deltas(frame)` crea un `LinkedHashMap`, lo rellena y ejecuta `Map.copyOf`.

Consecuencia: una evaluación en un tick tardío de un clip puede reconstruir mapas de **todos los frames anteriores recorridos**, aunque el programa sea inmutable y esté ya bound a una revisión.

**Estado:** `PERF-RISK alto`.

Mutante/contrato adversarial: un programa bound no debe comportarse como si sus estructuras de keyframe todavía necesitaran compilación/materialización por sample. La solución concreta pertenece al IMPLEMENTER; el adversario no prescribe una clase/cache específica.

### S20-PERF-003 — validación potencialmente cuadrática M × B

`applyOperation()` termina actualmente con:

`poses.values().stream().allMatch(MutablePose::valid)`

Esto vuelve a validar todos los huesos ya tocados **después de cada operación ejecutada**. Con `M` operaciones que introducen progresivamente `B` huesos, el número de validaciones puede aproximarse a `1 + 2 + ... + B`, además del stream por operación.

Dado que `MAX_OPERATIONS=2048`, el peor caso aceptado por schema no puede descartarse como irrelevante.

**Estado:** `PERF-RISK alto / scaling sin medir`.

Gate adversarial: aumentar linealmente el número de operaciones/huesos no debe producir crecimiento cuadrático del trabajo interno salvo necesidad semántica documentada.

### S20-PERF-004 — condiciones compuestas por streams recursivos

`ALL`/`ANY` usan streams recursivos en HOT_TICK. La profundidad y cardinalidad están acotadas, por lo que no es un riesgo de no terminación, pero falta evidencia de allocations/coste para programas con condiciones grandes válidas.

**Estado:** `BOUNDED PASS + allocation UNMEASURED`.

### S20-PERF-005 — búsqueda lineal del frame activo

Cada sample recorre `clip.keyframes()` desde el inicio acumulando duraciones hasta localizar el frame del `animation_tick`.

Con hasta 1024 frames por clip y 4096 globales, un clip largo aceptado puede pagar O(F) por sample. Esto puede ser perfectamente suficiente para los programas reales, pero debe medirse antes de decidir si hace falta una representación bound con offsets/índice temporal.

**Estado:** `PERF-RISK medio / UNMEASURED`.

## 5. Casos adversariales mínimos de rendimiento

Antes de cierre S20 deben existir resultados reproducibles para:

1. **operations scaling**: programas equivalentes con 32/64/128/256/... operaciones y huesos tocados; registrar trabajo/CPU y detectar tendencia superlineal;
2. **late-frame scaling**: mismo delta activo situado tras 1/16/64/256/1024 keyframes previos; comprobar coste y allocations;
3. **consumer reuse**: N consultas/observadores sobre el mismo authority sample no incrementan `jointEvaluations` del provider;
4. **allocation evidence**: bytes/allocations por `Bound.evaluate` en programa pequeño, típico y adversarial válido;
5. **condition tree**: condición compuesta cercana a los límites sin explosión desproporcionada;
6. **no preparation leakage**: prueba/inspección que confirme cero resource lookup, reflexión o construcción de programa desde el hot evaluator.

Los wall-clock absolutos de GitHub Actions no serán por sí solos un gate estable. Se prefieren contadores estructurales, CPU del hilo y allocation counters; el tiempo se usa comparativamente y con warm-up.

## 6. Mutantes de rendimiento que debe matar la evidencia

- reconstruir explícitamente una tabla de deltas completa por sample;
- validar todos los huesos después de cada operación aunque sólo cambie uno;
- resolver el resource/program id de nuevo en cada `evaluate`;
- ejecutar evaluación una vez por observador en lugar de una vez por authority sample;
- convertir la selección del clip/frame en scan de todos los clips + todos los frames;
- conservar resultados/caches por sample sin bound y producir crecimiento de memoria.

Si los tests/benchmarks no distinguen producción de estos mutantes, la evidencia de rendimiento de S20 se considera insuficiente.

## 7. Criterio adversarial de cierre de rendimiento S20

- [ ] frontera PREPARATION/HOT_TICK sigue limpia;
- [ ] no hay reflexión ni clases client/external en ejecución hot;
- [ ] NFR-009 demostrado explícitamente para el engine bound;
- [ ] scaling de operaciones/huesos clasificado y no cuadrático accidental;
- [ ] scaling de keyframes/tick tardío clasificado;
- [ ] allocations por evaluación medidas y aceptadas o corregidas;
- [ ] condiciones grandes válidas permanecen bounded con coste conocido;
- [ ] ninguna regresión observada queda etiquetada sólo como “ya se verá en G9”;
- [ ] pasada final adversarial a cero cambios productivos y cero gaps de rendimiento S20 pendientes.
