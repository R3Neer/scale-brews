# S18 — Evidencia adversarial de paridad procedural equine

Estado: **ORACLE DE STANDING RETRACTADO / ORACLE ORDINARY EN REEVALUACIÓN**.

## Corrección adversarial de alcance

La primera versión de esta evidencia usó `standAnimation=1` con `ordinary=true`. Esa fixture era inválida para el runtime actual: `AnatomyPoseEligibility` rechaza explícitamente equines cuando `eat`, `stand` o `mouth` son distintos de cero, o cuando el caballo está en agua. Esos estados deben llegar como `ordinary=false` y fallar cerrados, no ser reproducidos por el `PoseEngine` canónico.

Por tanto los rojos de los runs `34865530067` y `34865788558` **no se consideran evidencia normativa de un bug de producción**. Se conservan abajo únicamente como historial de cómo se detectó y después se invalidó el oracle. El ADVERSARY no exige soporte para standing/rearing mientras esa frontera de elegibilidad siga vigente.

El oracle cliente se corrigió en `0378ffa8081ec613caa9d070e446389ef8bac77f` para ejercer sólo el dominio realmente elegible:

- caballo adulto;
- `ordinary=true`;
- `eat=0`, `stand=0`, `mouth=0`, `water=0`;
- walking no trivial (`walkAnimationPos=2.3`, `walkAnimationSpeed=0.45`);
- cabeza no trivial (`xRot=12`, `yRot=15`);
- cola animada (`tail=1`);
- `ageScale=1` para el modelo adulto.

Este oracle sigue comparando directamente contra `HorseModel` / `AbstractEquineModel` 26.2 originales y exige paridad sólo para piezas volumétricas del estado soportado.

## Historial del oracle retractado

### Rojo inicial, no normativo tras la corrección de alcance

Snapshot `b7f429a4309ccacd7117454ed0093ffbaae7c8e2`, run `34865530067`, job `104048291973`:

- `root/body`, `matrix[5]`: Mojang `0.7071067`, neutral `1.0`;
- artifact `10356269340`;
- SHA-256 `1c41e4ed541eb4e8c7dd653f1826a76487296bf9b29f8069e5563fd3c9ffa872`.

Producción respondió con `20036150fd68939b19c973bc35b22063b760ccaf`, añadiendo semántica de `eat/stand/mouth/water`.

### Segundo rojo del mismo oracle, también no normativo tras la corrección

Run `34865788558`, job `104049181157`:

- `root/right_front_leg`, `matrix[5]`: Mojang `0.9962148`, neutral `-0.42282853`;
- artifact `10357181405`;
- SHA-256 `ab64cb900bf799526e4ccdf02f061f291115e8b62f06fb3c15cd7f577b48ae0b`.

La inspección source mostró además que el fix parcial intercambiaba la asignación de las expresiones izquierda/derecha de las patas delanteras. Esa observación sólo será un defecto vigente si el oracle **ordinary** corregido la reproduce dentro del dominio elegible.

## Frontera adicional pendiente, no clasificada todavía

Minecraft copia `state.ageScale` desde `entity.getAgeScale()` y `AbstractEquineModel` lo usa en los offsets Y/Z de la cola. Un `LivingEntity` bebé usa por defecto `ageScale=0.5`, y Minecraft selecciona además un `BabyHorseModel` con constantes de pose distintas.

El engine actual consulta `in.channel("age_scale", 1f)`, pero `AuthorityPoseTracker` no publica `age_scale`. No se clasifica aún como rojo: primero debe demostrarse que una geometría baby ligada a `scalebrews:equine` pertenece al dominio soportado del catálogo/runtime.

## Regla TM vigente

La evidencia válida para cerrar o reabrir S18 debe proceder del oracle ordinary corregido o de una nueva sonda que respete `AnatomyPoseEligibility`. Los runs de standing anteriores no bloquean por sí solos G3.4.
