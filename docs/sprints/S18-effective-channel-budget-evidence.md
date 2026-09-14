# S18 — evidencia adversarial del presupuesto efectivo de channels

Estado: **ROJO causal confirmado**.

Fecha: 2026-09-14

## Resumen

`CollisionBinding.Pose.channels` y `PoseEngine.Inputs.channels` comparten un límite máximo de 64 channels. Sin embargo, `MojangKeyframePoseEngine.bind(...)` añade al conjunto requerido los channels usados por los selectores `clock` y `amplitude` sin validar de nuevo el tamaño efectivo.

Esto permite que un binding aparentemente válido produzca un `PoseEngine.Bound` que exige 65 channels, aunque ningún `PoseEngine.Inputs` válido puede transportar 65. El endpoint queda así permanentemente `UNAVAILABLE` pese a haber superado la fase de binding/aceptación.

## Holdout

Commit del test: `2039d2deb5a49ca61e72b0c7c1bc1b46aa6483c3` (`test(s18): expose effective required-channel overflow`).

Lane aislada: `149a2bc995a9c91a8b291b501b9fa9e3896cda3d` (`ci(s18): isolate effective channel budget holdout`).

La prueba construye:

- exactamente 64 channels declarados `gate0..gate63`;
- un `PoseEngine.Inputs` con esos 64, que se acepta como precondición;
- un payload equivalente con un channel 65 `clock`, que `PoseEngine.Inputs` rechaza como precondición;
- un binding `mojang_keyframes` cuyo `clock = channel:clock` añade precisamente ese channel 65 al conjunto requerido.

El contrato exigido es que el bind falle cerrado si sus selectors convierten el conjunto efectivo en algo que el DTO live canónico no puede representar.

## Evidencia CI

Workflow: `s18-effective-channel-budget-proof`, run `34849872623`.

Job `common-effective-channel-budget` `103994802132`: **FAILURE causal**.

Artifact: `10349626902` (`S18-effective-channel-budget-proof`).

SHA-256: `b6c274deafab0abca1cd7cc6b079524bc867db9aebf6fd3d5c1470829c4fcc96`.

JUnit:

`S18EffectiveChannelBudgetTests.selectorChannelsCannotCreateAnUnrepresentableRequiredSet`

falla con:

`Binding must fail closed when clock/amplitude selectors expand the effective required-channel set beyond the 64-channel Inputs budget; accepting such a Bound creates an endpoint no valid live input can ever satisfy`

La precondición anterior del mismo test confirma que el constructor `PoseEngine.Inputs` acepta 64 y rechaza 65.

## Causa

`MojangKeyframePoseEngine.bind(...)` parte de:

```java
var required = new LinkedHashSet<>(requiredChannels);
```

y los parsers de `clock` / `amplitude` añaden sus channels mediante `required.add(channel)`. Después sólo se valida el formato del nombre, no `required.size()`.

Así, el límite se valida sobre la declaración inicial pero no sobre el **contrato efectivo** que el propio engine produce.

## Clasificación TM

**Bug productivo de validación de binding / presupuesto de inputs.**

No es un requisito nuevo: S18 define `CollisionBinding.Pose.channels` como contrato de channels requeridos, exige que todo channel usado por clock/amplitude pase a ser requerido y exige validación antes del swap. El límite live de 64 ya existe en `PoseEngine.Inputs` y en `CollisionBinding.Pose`; el conjunto efectivo debe respetar la misma frontera.

La corrección causal debe impedir publicar/producir un `Bound` imposible de alimentar con el DTO live canónico.
