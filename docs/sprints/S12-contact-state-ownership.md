# S12 — Contact state ownership

Estado: **CERRADO**

## Objetivo

Continuar la tarea 1 de G2 con una partición real de `AnatomyMovement`: extraer el ownership persistente del estado de contacto corporal a un componente dedicado, sin mover el solver, las queries espaciales ni los tipos cuya frontera sigue ligada a Q2/G3.

S12 no abre G3 y no cambia deliberadamente la física aceptada en S05-S11. La meta es que `AnatomyMovement` orqueste validación y aplicación física, pero deje de poseer directamente los mapas de contacto, secuencia, anchor, surface y suspensión.

## Baseline observado

Tras S10/S11, `AnatomyMovement` poseía directamente cinco almacenes coherentes como una sola responsabilidad:

- `CONTACTS`;
- `CONTACT_SEQUENCES`;
- `ANCHORS`;
- `SURFACES`;
- `SUSPENDED`.

Ese estado participaba en confirmación, retained contact, carry, suspension/local quarantine, lifecycle y enumeración de cuerpos contactados. La física que decide **si** un contacto es válido depende de providers/query/gravity y debía permanecer en `AnatomyMovement`; el almacenamiento que recuerda **qué** contacto/anchor/suspensión está vigente no necesita poseer esas dependencias.

La alternativa de extraer root history se descartó para este sprint: `RootFrame` sigue formando parte de la causalidad de `GeometryProvider.CausalEndpoint` y G3 generalizará `RootTransformProvider`. Moverlo ahora fijaría una frontera todavía inestable. El estado de contacto, en cambio, lleva estable desde S07-S09 y ya tenía lifecycle y semántica de secuencia probados.

## Frontera arquitectónica cerrada

`collision.internal.AnatomyContactState` es el owner único del estado retenido de contacto, todavía package-private/internal mientras la frontera física termina de estabilizarse.

Posee exclusivamente:

1. contacto material actual por body;
2. watermark/sequence de contacto por body;
3. anchor físico retenido por body;
4. `SurfaceContact` confirmado por body;
5. generaciones de suspensión body/support;
6. enumeración y cleanup local/de nivel de esos datos.

No consulta `GeometryProvider`, no ejecuta broadphase/raycast/sweep/separation/carry, no decide elegibilidad o gravedad, no escribe `TransportLedger` ni receipts, no invalida endpoints y no depende de `AnatomyMovement`.

La dirección queda: **`AnatomyMovement` -> `AnatomyContactState`**, nunca al revés.

`AnatomyMovement.Contact` se conserva como façade/DTO transitorio porque tiene consumidores reales, entre ellos receipts. `AnatomyContactState` almacena `ContactEntry` propio y el orquestador traduce en el borde. `Anchor`, que era privado y puramente retenido, pasó al owner de estado.

## Plan de implementación

- [x] Añadir holdout estructural rojo registrado que exija `AnatomyContactState` y prohíba `CONTACTS`, `CONTACT_SEQUENCES`, `ANCHORS`, `SURFACES` y `SUSPENDED` dentro de `AnatomyMovement`.
- [x] Registrar evidencia roja exacta en CI ordinario.
- [x] Crear `AnatomyContactState` con weak identity keys y sin dependencia hacia `AnatomyMovement`.
- [x] Mover al state el ownership de contacto y secuencia, preservando idempotencia de sequence para el mismo support/revision/piece.
- [x] Mover anchor y surface actuales al state.
- [x] Mover suspension generations al state, manteniendo la decisión física de stale/overlap en `AnatomyMovement`.
- [x] Sustituir acceso directo en `confirm`, `clear`, `tick`, `deactivate`, `afterMove`, `carry`, `suspend/suspended` y cleanup por operaciones del state.
- [x] Mantener `AnatomyTransportReceipts.invalidate(...)` en el orquestador: borrar contact state y borrar receipt son owners distintos aunque ocurran en la misma operación pública.
- [x] Añadir holdouts de comportamiento para sequence estable/incremental, clear coherente, identity-local suspension, stale suspension tras rebind, liberación al acabar overlap y cleanup de nivel/dimensión.
- [x] Reejecutar la suite ordinaria completa y los holdouts S07-S11.
- [x] Incluir `AnatomyContactState` en el trigger prepared y ejecutar geometría real + servidor prepared sobre el estado final combinado.
- [x] Revisar la extracción adversarialmente: cero mapas duplicados, cero callback `AnatomyContactState -> AnatomyMovement`, cero ownership físico nuevo.
- [x] Reabrir S12 al descubrir cleanup cross-dimension incompleto y repararlo de forma local al owner de state.
- [x] Registrar cierre; G2 sigue abierto por las fronteras restantes de las tareas 1 y 9.

## Holdouts adversariales

### A1 — owner dedicado — VERDE

Existe `io.github.r3neer.scalebrews.collision.internal.AnatomyContactState` como owner package-private del estado retenido.

### A2 — AnatomyMovement sin contact storage — VERDE

`CONTACTS`, `CONTACT_SEQUENCES`, `ANCHORS`, `SURFACES` y `SUSPENDED` desaparecieron de `AnatomyMovement`.

### A3 — sequence idempotente — VERDE

Reconfirmar la misma identidad material support/revision/piece conserva la sequence; cambiar piece o support la incrementa exactamente una vez. `clear` elimina la relación actual sin rebobinar el watermark.

### A4 — clear coherente — VERDE

El owner elimina contacto, surface y anchor juntos. `AnatomyMovement.clear(body)` conserva aparte `AnatomyTransportReceipts.invalidate(body)`, por lo que state y receipt siguen teniendo owners distintos.

### A5 — suspensión causal — VERDE

Las suspensiones usan weak object identity y binding generation. Dos supports distintos con el mismo network id no aliasan; limpiar uno no borra otro. Un rebind real invalida la generación vieja y una suspensión vigente se libera localmente cuando desaparece el overlap que la justificaba.

### A6 — lifecycle por nivel/dimensión — VERDE tras reapertura

La primera versión del owner limpiaba por `body.level()` y podía dejar una relación stale si el body ya había cambiado de dimensión pero el support retenido seguía perteneciendo al nivel desactivado. El holdout post-verde reprodujo exactamente ese caso.

El cleanup final elimina la relación si **body o support** pertenecen al nivel desactivado. Si el body ya transitó, conserva su sequence watermark; las suspensiones eliminan también supports del nivel viejo y un control de otro nivel permanece intacto.

### A7 — dependencia unidireccional — VERDE

Los campos, métodos y tipos anidados de `AnatomyContactState` no referencian `AnatomyMovement`. El state almacena hechos ya validados y no ejecuta callbacks al orquestador.

### A8 — regresión física cero — VERDE

La suite ordinaria final ejecuta los holdouts de S07-S12. La lane prepared real-geometry también queda verde en el snapshot combinado final.

## Evidencia red-before-green inicial

`6e6b2e4033784d2814b77f521c26bcde6a7d7f91` registró correctamente los holdouts estructurales S12.

GitHub Actions run **`34691755247`**, job **`103548121256`**:

- **367 GameTests** ejecutados;
- **365 passed / 2 failed**;
- fallaron exactamente los dos holdouts S12;
- A1 falló porque no existía `AnatomyContactState`;
- A2 enumeró exactamente `[ANCHORS, CONTACTS, CONTACT_SEQUENCES, SURFACES, SUSPENDED]` dentro de `AnatomyMovement`.

No hubo otro rojo que contaminase la evidencia.

## Extracción y primer verde

La extracción se hizo sin mover solver/root/CCD:

- `e843385cf1b9adf2b929b08e03255f153a853c1e` — introduce `AnatomyContactState`;
- `24177411a13b48ed56a439467713b446efa8dd8c` — elimina incluso la referencia documental inversa al orquestador;
- `dc82d28effda665d14a72220201abdf99a984958` — migra los cinco almacenes y sus consumidores desde `AnatomyMovement`;
- `9317fdccd191a66b2e2fe0ffc90783a63b38589d` — añade holdouts de sequence/clear/identity/dependency;
- `2a9422c24a8a57f501a02d62856fdb98ef763ab1` — corrige una posición de fixture;
- `0d698dcd87c71d395cfe569d2938cf30b3288eb7` — añade el nuevo owner al trigger de la lane prepared.

En `0d698d...`, ordinary run **`34692212141`**, job **`103549359469`**, pasó **371/371**. Ese verde era correcto para los holdouts existentes, pero la revisión de S12 no se dio por cerrada porque A5/A6 todavía no ejercían rebind/overlap y cleanup cross-dimension de forma explícita.

## Reapertura adversarial de S12

`643f436cd067210f86b24e90ef85b31b7e3d74e7` añadió dos holdouts que faltaban:

1. rebind real + expiración de suspensión + liberación al desaparecer overlap;
2. cleanup por nivel con un body ya transitado a otra dimensión pero una relación vieja apuntando a un support del nivel anterior.

Run **`34692691813`**, job **`103550650061`**:

- ejecutó **373 GameTests**;
- **372 passed / 1 failed**;
- el holdout de rebind/overlap pasó;
- el único rojo fue el cleanup cross-dimension: el state conservaba la relación stale porque sólo inspeccionaba el nivel actual del body.

Ese fallo reabrió legítimamente S12 bajo lifecycle/ownership; no era un defecto de fixture ni de S09.

## Reparación de lifecycle S12

`7142207dba60a64c5f69f8cbe0d56bae5df05a13` reparó únicamente `AnatomyContactState.deactivate(Level)`:

- contacto/anchor/surface se eliminan si body **o support** pertenecen al nivel desactivado;
- el sequence watermark de un body que ya transitó a otro nivel no se rebobina;
- si el body sigue en el nivel desactivado, su state completo sí se retira;
- las suspensiones de un body superviviente eliminan sólo supports del nivel viejo;
- no se añadió ninguna dependencia inversa ni decisión física al owner de estado.

Ordinary run **`34692838485`**, job **`103551041282`**, sobre `7142207...` pasó **373/373**. Artefacto **`10298205581`**, SHA-256 **`52ef5a4d7abc02a8041853c227bcd9d1573c8c3d5d40b804e10706e4ddde93f4`**.

## Rojo prepared independiente de S12

Prepared run **`34692838450`**, job **`103551041264`**, sobre el mismo `7142207...` exportó correctamente la geometría cliente real, pero el servidor prepared encontró un fallo de **S09 A9**: el full cow manifold agotaba `TemporalResponse` exactamente en el budget normativo de 256 evaluaciones.

Ese rojo no implicaba `AnatomyContactState`: el cliente/export estaba verde y el fallo estaba en la respuesta temporal de S09. El agente concurrente reparó exclusivamente `collision.physics.TemporalResponse` en `0a0faece9ac17f5163bbfd644121af4c0bdc97ea`, refinando ventanas temporales ambiguas sin elevar el budget.

## Evidencia verde final combinada

Snapshot final probado: **`0a0faece9ac17f5163bbfd644121af4c0bdc97ea`**. Es descendiente directo de la reparación S12 `7142207...`; el único cambio posterior es el fix independiente S09 en `TemporalResponse`.

### Ordinary

Run **`34692861210`**, job **`103551102716`**:

- checkout exacto `0a0faece9ac17f5163bbfd644121af4c0bdc97ea`;
- **373/373 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 15s`;
- artifact **`10297696857`**, 904292 bytes;
- SHA-256 **`fc3c9783e6527052ef7167e8049a0593be390e1aa8e99833f42fb8c938dacdc0`**.

### Prepared real-geometry

Run **`34692861165`**, job **`103551102638`**:

- export cliente original completado: cow **240 vertices / 10 pieces**, 80 comparaciones de pose;
- **640** comparaciones adicionales de familias vanilla;
- player wide/slim **144 vertices / 6 pieces** cada uno, con 80 comparaciones de pose cada uno;
- export cliente `BUILD SUCCESSFUL in 1m 34s`;
- servidor prepared ejecutó **2/2 required GameTests passed**;
- suite servidor prepared `BUILD SUCCESSFUL in 27s`.

Los avisos de narrator/ALSA/X11/autenticación del runner fueron no fatales y no afectaron los oracles.

## Revisión final cero-cambios

Tras el verde final:

- `AnatomyMovement` no posee ninguno de los cinco almacenes extraídos;
- `AnatomyContactState` es el único owner del estado retenido de contacto;
- weak identity evita alias por network id reutilizable;
- sequence conserva monotonicidad y no se rebobina por clear ni por transición de nivel;
- stale suspension se invalida por binding generation y se libera al acabar overlap;
- cleanup cubre ambos extremos de una relación cross-dimension;
- receipt invalidation permanece en su owner/orquestador;
- no existe dependencia `AnatomyContactState -> AnatomyMovement`;
- no se movió solver, root history, broadphase, CCD ni carry al state.

No se identifica otro cambio de producción propio de S12.

## Cierre

**S12 está CERRADO.** La extracción reduce materialmente la tarea 1 de G2 y elimina otro bloque de ownership persistente de `AnatomyMovement` sin crear una capa nominal ni un ciclo nuevo.

S12 **no cierra G2** y **no autoriza G3**. Siguen abiertas las fronteras restantes de las tareas 1 y 9, que deben auditarse por dependencia real antes de decidir el siguiente sprint.
