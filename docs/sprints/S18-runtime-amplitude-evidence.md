# S18 — evidencia adversarial de amplitud runtime

Estado: **ROJO causal confirmado**.

Fecha: 2026-09-14

## Resumen

La segunda ronda adversarial posterior a la preservación exacta de `SourcePose` encontró una frontera de validación incorrecta en `scalebrews:mojang_keyframes`.

`PoseProgram.Vector` impone el bound de dato preparado `abs(component) <= 65536`. Ese bound es válido para el contenido serializado del programa, pero `PoseProgramEvaluator` reutiliza el mismo constructor después de aplicar la amplitud runtime. Como consecuencia, una combinación de **programa válido + selector de amplitud válido + resultado físico finito** puede lanzar `IllegalArgumentException` en vez de devolver una pose o `UNAVAILABLE`.

## Holdout

Commit adversarial: `17ebb75ecea44fa56e6d9e9a7cc0c05c67e18c9b` (`test(s18): expose runtime amplitude vector-bound leak`).

Se añadió exclusivamente un GameTest a `S18PoseProgramExecutionTests`; no se modificó producción.

Fixture:

- track `TRANSLATION` sobre `root`;
- keyframe final: `40000` píxeles en X, dentro del bound almacenado `65536`;
- `amplitude = constant:2`, aceptada por `MojangKeyframePoseEngine`;
- evaluación en `t=1`;
- resultado esperado: `80000` píxeles = `5000` bloques de traslación local, finita y afín.

El contrato que se prueba no amplía el formato de `PoseProgram`: el dato preparado sigue limitado a 65536. Lo que se exige es que ese límite de **serialización/preparación** no se vuelva a aplicar implícitamente al valor intermedio runtime.

## Evidencia CI

Workflow focal: `s18-pose-engine-proof`, run `34847972192`.

- `common-authority`, job `103988449997`: **FAILURE**.
- resto de lanes S18 cliente observadas en el mismo run: verdes, por lo que el rojo queda localizado en common/runtime y no en compiler, Euler representative ni signed-scale representative.
- artifact common: `10348702669`, SHA-256 `a86878f8eb5cf6a111ec8e1d850822274c8488b25248751a789d4ea9c42677e6`.

JUnit del artifact:

`S18PoseProgramExecutionTests.validRuntimeAmplitudeMayExceedStoredVectorBound`

falla con:

`Invalid pose-program vector`

Los demás holdouts common del run pasan antes del nuevo caso.

## Causa

Ruta productiva actual:

1. `MojangKeyframePoseEngine` valida la constante de amplitud y acepta magnitudes de hasta `1_000_000`.
2. `PoseProgramEvaluator` samplea un `PoseProgram.Vector` válido.
3. `scale(...)` multiplica sus componentes por la amplitud.
4. `scale(...)` construye un **nuevo `PoseProgram.Vector`**.
5. El constructor vuelve a aplicar el bound `65536`, aunque el objeto ya no representa material serializado sino un valor temporal de ejecución.
6. La excepción escapa antes de `geometry.transforms(...)` y antes de la ruta fail-closed del evaluator.

Por tanto se han mezclado dos dominios distintos:

- **bound del DTO preparado/wire**, para limitar datos externos;
- **valor matemático runtime**, que debe validarse por finitud y por la validez final de la transformación, no por el bound del archivo fuente.

## Clasificación TM

**Bug productivo de frontera DTO → evaluator.** No es un nuevo requisito ni una ampliación del freeze S18. El sprint ya exige clock/amplitude explícitos, evaluación determinista y salida validada mediante `ModelGeometry.transforms(...)`; una entrada aceptada no debe terminar en una excepción no controlada por reutilizar accidentalmente el constructor bounded del DTO.

S18/G3.4 continúa abierto hasta que este rojo y los demás oracles de paridad permanezcan verdes bajo una corrección causal.
