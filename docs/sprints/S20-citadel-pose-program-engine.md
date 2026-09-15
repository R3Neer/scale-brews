# S20 — Citadel/Alex reusable pose-program engine

Estado: **OPEN / IMPLEMENTATION ACTIVE**.

Rol principal de esta rama de trabajo: **IMPLEMENTER**. El adversario conserva ownership independiente de holdouts, mutantes, segunda lectura y gate de rendimiento.

Este sprint ejecuta la tarea G3.5 que quedó abierta después de S19. S19 cerró la mitad de geometría `AdvancedModelBox`; S20 cubre la mitad pose/animación sin volver a introducir Java nominal por especie.

## 1. Requisitos ya congelados

S20 no crea requisitos funcionales nuevos. Deriva de los contratos normativos existentes:

- FR-025: un `PoseEngine` representa una familia reusable y dos modelos de la misma tecnología comparten evaluador;
- FR-026: Java/API implementa engines, mientras datos seleccionan engine, programa, canales, parámetros y estados;
- FR-027: vocabulario de canales autoritativos explícito y reproducible;
- FR-028: relojes/estado de pose proceden de autoridad común/servidor, nunca de verdad del renderer;
- FR-029: canal indispensable, pose, primitive o dialecto desconocidos producen `UNAVAILABLE`, nunca collider congelado;
- FR-033: binding canónico selecciona geometry + pose + root + policy declarativamente;
- NFR-003/004/005: validación estricta, fail-closed y common/dedicated sin linking client-only;
- NFR-009: evaluar joints como máximo una vez por muestra causal y reutilizar el resultado;
- NFR-012: programas patológicos explícitamente acotados;
- NFR-019: una especie ya cubierta por engines no exige clase Java nueva;
- NFR-024: core sin dependencia obligatoria de Alex's Mobs/Citadel;
- NFR-028/029: inputs externos fijados y aceptación contra modelos/renderers originales, no fórmulas sintéticas equivalentes.

El protocolo adversarial transversal de rendimiento clasifica el compilado/bind de programas como **PREPARATION** y la evaluación de un `PoseEngine.Bound` como **HOT_TICK**. No se permite reflexión, resolución de resources o parsing en HOT_TICK.

## 2. Inputs externos fijados

La evidencia de familia usa exactamente los inputs fijados por S19 en `tools/s19-external-inputs.lock.json`:

- Alex's Mobs Continued Fabric 26.2, `alexsmobs` **2.1.9**;
- CodxLib **1.5.1**;
- mismos artefactos y SHA-256 que S19.

Los modelos representativos mínimos de aceptación son **Grizzly Bear** y **Gazelle** porque ejercitan dos programas distintos sobre el mismo lenguaje Citadel (`AdvancedEntityModel` + `ModelAnimator`). El mismo engine debe evaluar ambos.

## 3. Contrato neutral S20

### 3.1 Programa

`CitadelPoseProgram` es DTO common/server-safe, revision-local y acotado. Puede expresar únicamente las primitivas necesarias del dialecto fijado:

- deltas de rotación/posición;
- `walk`, `swing`, `flap`, `bob`, `faceTarget`;
- `progressRotationPrev` / `progressPositionPrev`;
- escala explícita cuando el modelo original la modifica;
- clips `ModelAnimator`: keyframe, static keyframe y reset keyframe con interpolación original;
- condiciones declarativas sobre canales autoritativos.

Primitivas no soportadas fallan bind/evaluación; no se aproximan.

### 3.2 Rest pose

La autoridad neutral son los `ModelGeometry.Part.sourcePose` extraídos por S19. El engine no descompone matrices para reconstruir Euler/signos de escala. Un bone objetivo sin `SourcePose` exacta hace fallar el bind.

### 3.3 Ownership de resources

Los programas Citadel pertenecen al snapshot/revisión aceptado igual que los `PoseProgram` Mojang. No existe registry global mutable de programas. `PoseEngine.Resources` sólo expone una vista inmutable del snapshot al hacer bind.

### 3.4 Runtime

El resultado del bind es un `PoseEngine.Bound` sin referencias a Citadel/Alex, sin reflexión, sin JSON y sin resource lookup. `ModelGeometryProvider` conserva el ownership de cache/joint reuse establecido en S18.

## 4. Fases del implementador

- [x] I1 — DTO neutral/acotado `CitadelPoseProgram`.
- [x] I2 — evaluador common de helpers Citadel y semántica `ModelAnimator`.
- [x] I3 — `scalebrews:citadel_program` como `PoseEngine` reusable y bind revision-local.
- [x] I4 — regresiones de implementador para helper, tween, unknown animation y `SourcePose` indispensable.
- [ ] I5 — incorporar `CitadelPoseProgram` al snapshot atómico y bundle de catálogo; evolucionar protocolo explícitamente.
- [ ] I6 — frontera reusable de canales autoritativos externos, sin `instanceof`/Java por mob dentro del core hot path.
- [ ] I7 — programas declarativos representativos para Grizzly y Gazelle 2.1.9.
- [ ] I8 — prueba client/tooling contra los modelos originales fijados, con muestreo ordinario + clips/condiciones cubiertas por los programas.
- [ ] I9 — integrar los bindings representativos sin dependencia dura de Alex/Citadel.
- [ ] I10 — documentación de runtime, límites, ownership y datos necesarios para añadir otra especie cubierta por la misma tecnología.

## 5. Invariantes que S20 no puede romper

- S15/S16: snapshot de catálogo atómico y authority única por revisión.
- S17/S19: geometry source/version y `SourcePose` siguen siendo autoridad de preparación.
- S18: programas de pose data-backed son revision-local; no registry global; unknown/missing input falla cerrado.
- G2: pairwise vanilla push, blocker y carry no cambian por este sprint.
- dedicated/common no enlaza clases `net.minecraft.client`, Citadel o Alex's Mobs.

## 6. Gate implementador y adversarial

El implementador no declara S20 cerrado sólo por compilar. Antes de entrega al adversario deben existir:

1. build verde y regresiones S17–S19 relevantes verdes;
2. dos modelos originales distintos usando **el mismo** `scalebrews:citadel_program`;
3. comparación contra fuente/render original fijado, no contra una copia de la fórmula;
4. fail-closed observable para programa/canal/bone/primitive desconocidos;
5. bounds de clips/keyframes/deltas/operaciones y ausencia de parsing/reflection/resource lookup en HOT_TICK;
6. evidencia de que `ModelGeometryProvider.jointEvaluations` conserva reuse causal de NFR-009.

Después el **ADVERSARY** debe añadir holdouts/mutantes propios, auditoría de performance del nuevo hot path y una segunda lectura sin cambios productivos antes de cierre.
