# S18 — Evidencia adversarial de paridad procedural equine

Estado: **ROJO / FIX PARCIAL INSUFICIENTE**.

## Alcance

Esta evidencia responde a producción S18 real. No amplía el contrato pre-implementación: `AuthorityPoseTracker` ya publica estado equine autoritativo (`eat`, `stand`, `mouth`, `tail`, `water`) y el criterio S18 exige que los `PoseEngine` vanilla canónicos reproduzcan la pose material de Minecraft 26.2 para inputs soportados.

Oracle original-source: `S18EquineProceduralSemanticClientTests`, usando `HorseModel` / `AbstractEquineModel` 26.2 reales y geometría extraída por `GeometryExtractor`.

Fixture principal:

- caballo adulto;
- `ordinary=true`;
- `walkAnimationPos=0`, `walkAnimationSpeed=0`;
- `xRot=0`, `yRot=0`;
- `eatAnimation=0`;
- `standAnimation=1`;
- `feedingAnimation=0`;
- `animateTail=false`;
- `isInWater=false`;
- los cinco channels autoritativos se entregan al engine.

Las precondiciones verifican antes de comparar que Minecraft mueve `root/body` y `root/right_front_leg` fuera de rest pose. Son piezas volumétricas ordinarias, no decoración degenerada ni una excepción de filtro.

## Rojo inicial

Snapshot adversarial: `b7f429a4309ccacd7117454ed0093ffbaae7c8e2`.

Workflow `s18-equine-procedural-parity-proof`, run `34865530067`, job `104048291973`: setup, compilación, aislamiento y captura de evidencia verdes; falla únicamente el oracle semántico.

Primer contraejemplo:

- parte: `root/body`;
- `matrix[5]` Mojang: `0.7071067`;
- `matrix[5]` neutral: `1.0`.

Minecraft aplica `body.xRot = -pi/4` cuando `standAnimation=1`; el engine anterior ignoraba `stand` y dejaba el cuerpo en reposo.

Artifact `10356269340`.
SHA-256 `1c41e4ed541eb4e8c7dd653f1826a76487296bf9b29f8069e5563fd3c9ffa872`.

## Respuesta del implementador y segunda lectura

Producción respondió en `20036150fd68939b19c973bc35b22063b760ccaf` (`fix(s18): honor authoritative equine standing pose`). El fix pasa a requerir/consumir `eat`, `stand`, `mouth`, `tail` y `water`, y corrige el cuerpo, cabeza y colocación de patas.

La segunda ejecución del mismo oracle demuestra que el fix es todavía incompleto. Run `34865788558`, job `104049181157`: de nuevo sólo falla el oracle semántico.

Nuevo primer contraejemplo, después de que `root/body` ya coincida:

- parte: `root/right_front_leg`;
- `matrix[5]` Mojang: `0.9962148`;
- `matrix[5]` neutral: `-0.42282853`.

Causa aislada por inspección original-source:

- Minecraft calcula `rlegRot = (offset + bob) * standing + walk * (1-standing)` y lo asigna a **`leftFrontLeg`**;
- calcula `llegRot = (offset - bob) * standing - walk * (1-standing)` y lo asigna a **`rightFrontLeg`**;
- el fix parcial calcula ambas ramas pero las aplica al lado homónimo, intercambiando la semántica izquierda/derecha respecto a Mojang.

Artifact `10357181405`.
SHA-256 `ab64cb900bf799526e4ccdf02f061f291115e8b62f06fb3c15cd7f577b48ae0b`.

## Frontera adicional observada, aún no clasificada como rojo independiente

Minecraft obtiene `state.ageScale` desde `entity.getAgeScale()` y `AbstractEquineModel` lo usa para los offsets Y/Z de la cola. Para un `LivingEntity` bebé el valor por defecto es `0.5`, no `1.0`.

El fix parcial consulta `in.channel("age_scale", 1f)`, pero `AuthorityPoseTracker` no publica actualmente `age_scale`. Esto merece una sonda separada sólo después de cerrar el rojo de standing, para no mezclar causas.

## Clasificación TM

**Defecto de paridad procedural en el owner canónico `scalebrews:equine`, con autoridad ya disponible.** No es un gap de wire ni de ownership y no requiere introducir estado cliente. El arreglo debe reproducir la semántica material de `AbstractEquineModel` para los channels autoritativos aceptados, o fallar `UNAVAILABLE` si un estado no puede representarse; no debe congelar ni aproximar silenciosamente una pose distinta.

S18/G3.4 permanece abierto hasta que este oracle quede verde y pase una segunda lectura de los estados equine restantes.
