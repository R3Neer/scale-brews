# S24 — adversarial contact dimension replay RED

Rol: **ADVERSARY**.

Estado: **RED reproducible / segunda barrera de dimensión incompleta**.

## Contrato atacado

Este holdout es independiente del RED de `AnatomyPosePayload` y ataca el canal de contacto confirmado:

- **FR-080**: contacto/frame de red debe validarse por dimensión, epoch, revisión, UUID, network id y tracking generation pertinente; un replay cruzado no puede materializar estado efectivo;
- **FR-082**: dimension change debe invalidar estado temporal y sus fences de forma definida;
- **NFR-017**: el cambio de dimensión es barrera causal; eventos anteriores a la barrera deben rechazarse.

## Por qué no basta el RED de pose

Una posible reparación del primer RED sería preservar únicamente `TrackingReplayFence` al cambiar de `ClientLevel`. Eso cercaría `AnatomyPosePayload`, pero `AnatomyContactInbox` mantiene un watermark separado por cuerpo (`trackingGeneration + sequence`) y `clearPoses()` también ejecuta `contacts.clear()` durante cada cambio de nivel.

Por tanto era necesario demostrar si el contacto podía resucitar incluso cuando la geometría del soporte ya pertenecía inequívocamente a una vida post-barrera.

## Holdout integrado

Test: `S24ContactDimensionReplayClientProof`.

Commit del test: `f28cb910df62a71728f5c6c997bc81bf728bdfe2`.
Workflow: `.github/workflows/s24-adversarial-contact-dimension-replay.yml`.
Commit workflow: `10d0e563e683e9100f827f74ca4bd6a78d851c91`.

Run: `35129175319`.
Job: `104905592860`.
Resultado: **failure** en la aserción causal prevista.

Error exacto:

`Pre-dimension-barrier contact replay became authoritative after fresh support reacquisition`

## Escenario

1. Se arranca un runtime preparado con vaca anatómica estática en Overworld y se espera a que el cliente tenga catálogo y presentation frame válidos.
2. En servidor se crea un cerdo pequeño (`scale=.2`) sobre el lomo real de la vaca usando el solver material de producción; `AnatomyMovement.surface(pig)` y `supported(pig)` confirman un contacto físico real.
3. Se construye y envía un `AnatomyContactPayload` con la identidad real de conexión/dimensión/cuerpo/soporte y el `trackingGeneration` server-side del cuerpo. Se espera hasta que ese mismo contacto sea visible en `presentationContact(...)` cliente.
4. Se detiene únicamente `AnatomyRuntime` para impedir publicaciones anatómicas automáticas posteriores, manteniendo viva la conexión/catálogo vanilla.
5. El jugador hace `Overworld -> Nether -> Overworld`.
6. Al volver, se espera a que vaca y cerdo vanilla existan y se verifica que el contacto anterior no haya sobrevivido materialmente.
7. Antes de reinyectar el contacto se envía explícitamente la pose capturada de la vaca con una **tracking generation de soporte estrictamente posterior** (`oldSupportGeneration + 1`). Se espera hasta que `presentationFrame(cow)` vuelva a estar presente.
8. Se comprueba de nuevo que todavía no existe contacto.
9. Se reenvía byte-semánticamente el contacto pre-barrera original, con su tracking generation/sequence antiguos.
10. Producción actual lo acepta y `bindPhysics()` vuelve a confirmarlo contra la geometría fresca del soporte.

El test además verifica antes de la inyección que el `tick` del paquete sigue dentro de la ventana de 100 ticks del receiver, evitando un falso verde por expiración temporal.

## Causa observada

`AnatomyContactInbox` conserva normalmente un `watermarks` map incluso después de consumir un contacto, precisamente para impedir que un paquete retrasado restaure estado ya confirmado/limpiado.

Sin embargo, `AnatomyClientNetworking.useLevel(...)` llama a `clearPoses()` cuando cambia la identidad del `ClientLevel`, y `clearPoses()` ejecuta:

```java
contacts.clear();
presentationContacts.clear();
```

`contacts.clear()` elimina tanto material pendiente como el watermark causal del cuerpo. Tras `A -> B -> A`, un paquete viejo de A vuelve a cumplir epoch/revision/dimension y ya no encuentra ningún `trackingGeneration/sequence` anterior con el que compararse. Si el soporte ha recuperado geometría válida, el paquete puede volver a materializar contacto.

## Restricciones de la reparación

La solución de la barrera de dimensión debe tratar **pose y contacto**, no sólo `TrackingReplayFence`.

Debe demostrar simultáneamente:

- material temporal del nivel anterior (`pending contacts`, `presentationContacts`, anchors/providers/poses) desaparece inmediatamente al cambiar de nivel;
- permanece información causal compacta suficiente para rechazar un contacto pre-barrera si se vuelve a la misma dimensión antes de recibir una publicación de contacto fresca;
- un contacto genuinamente posterior, con tracking generation/sequence autorizados, puede materializarse normalmente;
- disconnect/host replacement y una nueva revisión aceptada siguen pudiendo limpiar los fences de la conexión/revisión anterior;
- la retención permanece acotada y fail-closed; no vale conservar contactos/material completo indefinidamente;
- el arreglo del canal de contacto no depende de que el soporte conserve una pose vieja: este holdout instala deliberadamente una pose post-barrera fresca antes del replay.

Una implementación posible sería separar en `AnatomyContactInbox` la limpieza de `pending/material` de la limpieza de `watermarks`, igual que el lifecycle cliente necesita distinguir level reset de connection/catalog reset. El holdout no exige esa arquitectura concreta.

## Gate de cierre

La barrera de dimensión de S24 no está cerrada hasta que pasen ambos holdouts integrados:

- `S24DimensionReplayClientProof` para pose;
- `S24ContactDimensionReplayClientProof` para contacto;

y sigan verdes los happy paths de dimensión, reconnect y los mutation-kills ya existentes.
