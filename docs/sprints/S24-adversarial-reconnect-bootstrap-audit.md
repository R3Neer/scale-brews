# S24 — adversarial reconnect bootstrap audit

Rol activo: **ADVERSARY**.

Estado: **RECONNECT SIN EVIDENCIA / PROOF FOCAL ROJO ANTES DE LA PRIMERA DESCONEXIÓN**.

## 1. Candidato implementer

`S24ReconnectLifecycleClientProof` intenta demostrar una desconexión/reconexión real contra el mismo dedicated server:

1. arrancar runtime preparado con una vaca canónica;
2. conectar cliente y alcanzar `READY` + pose + `presentationFrame`;
3. cerrar la primera conexión;
4. comprobar que catálogo/pose de la conexión vieja desaparecen;
5. reconectar al mismo servidor y exigir mismo epoch/revision/binding, nueva identidad `ServerPlayer` y `trackingGeneration=1` para el nuevo receptor.

El diseño del proof es adecuado para FR-080/FR-082/NFR-017 si consigue ejecutar ambas vidas de conexión.

## 2. Resultado ejecutado

Workflow `s24-client-reconnect-lifecycle`, run **`35120074913`**, job **`104875319948`**: **failure**.

El fallo terminal es:

```text
java.lang.AssertionError: Timed out waiting for predicate
  at S24ReconnectLifecycleClientProof.awaitReady(...:96)
  at S24ReconnectLifecycleClientProof.runTest(...:41)
```

La línea 41 es el **primer** `awaitReady(...)`, dentro de la primera conexión. La primera desconexión del escenario todavía no ha ocurrido. El build general del mismo commit (`35120074766`) sí fue verde, por lo que esta evidencia focal no puede sustituirse por el build ordinario.

Los errores headless de narrator/OpenAL presentes en el log no son el fallo terminal: el cliente y dedicated server arrancan, `Player0` entra y permanece conectado hasta que el GameTest aborta por timeout.

## 3. Clasificación adversarial

Este resultado **no demuestra una regresión de reconnect**, porque reconnect no llegó a ejecutarse. Tampoco permite declarar el proof verde por intención.

El bloqueo real es bootstrap del fixture dedicado: la sesión inicial no alcanza conjuntamente vaca cliente visible + catálogo/pose/presentation requerida por `awaitReady` dentro de 200 ticks.

Antes de modificar producción debe aislarse cuál etapa falta:

- entidad visible en el cliente;
- catálogo `READY`;
- `AnatomyFrameHistory` recibido;
- provider/presentation materializable;
- server tracking window publicada al receptor.

## 4. Factura de aceptación

Reconnect permanece abierto hasta que:

1. el proof dedicado alcance la primera sesión READY de forma reproducible;
2. ejecute realmente disconnect + segunda conexión;
3. la segunda conexión no herede catálogo/frames/contactos/receipts de la primera;
4. el mismo server/binding pueda conservar epoch/revision/binding cuando corresponda sin conservar autoridad del receptor muerto;
5. una campaña mutation-kill demuestre sensibilidad al menos a omitir el reset cliente de conexión y a conservar indebidamente autoridad server-side del receptor antiguo.

Hasta entonces el run rojo es diagnóstico útil, no evidencia de cumplimiento ni evidencia suficiente de fallo de producción.
