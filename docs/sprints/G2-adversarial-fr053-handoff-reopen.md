# G2 / FR-053 — revisión adversarial post-fix: handoff legacy → READY

Rol activo: **ADVERSARY**.

Estado: **ROJO PRODUCTIVO / G2.10 PERMANECE ABIERTO**.

Esta revisión no modifica producción. Revisa el candidato posterior a la reparación implementadora que retiró la cancelación anatómica directa de `Entity.push`, y abre un contraejemplo distinto en la frontera temporal entre ownership legacy y ownership anatómico READY.

## 1. Snapshot y holdout

Producción revisada: `4782935a049b90b1827c232223003e2d4b4c16bd`.

Holdout adversarial: `8e7c946d7d5da853569dc5df7b9d97596e672acd` — `test(g2): adversarial stale legacy push handoff`.

El cambio adversarial sólo endurece `G2VanillaPushReadyTests`; no toca `src/main`.

La lane aislada `g2-vanilla-push-proof` compila correctamente el árbol y ejecuta el GameTest. El caso limpio existente continúa pasando; el nuevo caso de transición es el único required test que falla.

## 2. Contrato atacado

FR-053 exige que una pareja moving-platform anatómica elegible conserve `Entity.push` vanilla exactamente una vez. Esa propiedad debe cumplirse desde el instante en que la anatomía adquiere ownership compartido; no puede depender de que llegue un `END_LEVEL_TICK` posterior para purgar estado legacy residual.

Por tanto el handoff correcto debe cumplir simultáneamente:

1. una relación legacy previamente válida puede existir antes de activar la sesión anatómica;
2. al pasar a READY, la anatomía puede sustituir el blocker geométrico según el contrato ya certificado;
3. cualquier metadata legacy residual deja de poder cancelar `Entity.push` desde ese mismo handoff;
4. la ruta legacy ordinaria conserva su semántica cuando no existe ownership anatómico.

## 3. Reproducción causal

El holdout usa estado alcanzable por producción, no escritura manual de `PlatformState`:

1. crea un ghast soporte y un jugador pequeño;
2. mide `support.push(body)` y `body.push(support)` sin relación gestionada;
3. hace aterrizar al jugador mediante `Entity.move`, atravesando la ruta legacy real;
4. verifica que `Platforms.state(body).support == support` ha sido adquirido por producción;
5. activa `AnatomyRuntime.startPrepared(...)` sin conceder un tick de limpieza;
6. verifica que ambos participantes están en READY y que la relación legacy adquirida sigue presente;
7. establece un contacto material anatómico válido con el mismo par;
8. ejecuta `Entity.push` en ambas direcciones.

El test exige deliberadamente el primer instante del handoff. Esperar a `Platforms.tick` convertiría el oracle en un test del saneamiento tardío, no del cambio de ownership.

## 4. Resultado

Workflow run `35146286790`, job `104963104520`: **failure causal**.

Artifact `10467532328`, SHA-256:

```text
31a10f197a7b6916d34ef3165e014e0951091a5444fb91eaad672a2b4b2df042
```

La compilación y el arranque del GameTest son verdes. El fallo ocurre en la aserción de preservación de `push` del handoff, en tick `0`.

Control vanilla:

```text
support.push(body)
  body    = ( 0.022360680118610494, 0.0, 0.0)
  support = (-0.022360680118610494, 0.0, 0.0)

body.push(support)
  body    = ( 0.022360680118610494, 0.0, 0.0)
  support = (-0.022360680118610494, 0.0, 0.0)
```

Tras legacy → READY + contacto material anatómico:

```text
support.push(body)
  body    = (0.0, 0.0, 0.0)
  support = (0.0, 0.0, 0.0)

body.push(support)
  body    = (0.0, 0.0, 0.0)
  support = (0.0, 0.0, 0.0)
```

Mensaje causal del GameTest:

```text
READY handoff must invalidate stale legacy push suppression immediately, without waiting for Platforms.tick
```

## 5. Localización productiva

La reparación implementadora anterior eliminó correctamente el guard anatómico explícito de `PlatformEntityMixin.push(Entity)`. El hook actual conserva sólo el guard legacy:

```java
if(Platforms.state(self).support==other || Platforms.state(other).support==self) ci.cancel();
```

Ese guard no comprueba quién posee actualmente la física compartida. Por ello una relación legacy legítimamente adquirida antes del cambio de ownership sigue cancelando `push` durante la ventana legacy → READY.

`Platforms.tick(ServerLevel)` acaba limpiando `PlatformState` para participantes legacy que ya están bajo `AnatomyApi.ownsSharedPhysics(e)`, pero esa limpieza sucede después. `AnatomyRuntime.startPrepared/reset` no purga el `PlatformState.support` previamente adquirido. El resultado es una ventana causal observable en la que READY ya posee la relación pero el hook legacy sigue actuando como owner de `push`.

## 6. Clasificación

**PRODUCTIVO.** No es un fallo de fixture:

- el estado legacy se obtiene por `Entity.move` real;
- READY se verifica explícitamente;
- el contacto anatómico se verifica explícitamente;
- el control vanilla es medible en ambas direcciones;
- el fallo reproduce exactamente la firma `±0.022360680118610494 → 0`;
- ocurre antes de cualquier tick de cleanup, que es precisamente la frontera atacada.

El fix previo resolvió el rojo original de contacto anatómico limpio, pero FR-053 todavía no está recertificado para transiciones desde estado legacy vivo.

## 7. Handoff al IMPLEMENTER

G2.10 no puede cerrarse todavía. La reparación siguiente debe hacer que el estado legacy residual deje de cancelar `Entity.push` en cuanto la anatomía posea la relación, sin romper:

1. la supresión legacy de `push` cuando el ownership sigue siendo legacy;
2. el single geometric owner certificado por `PlatformPhysics.suppressPair`;
3. el holdout de pareja no elegible;
4. el caso READY limpio ya verde;
5. la ausencia de doble push o doble carry.

El ADVERSARY no propone ni aplica el cambio productivo. El nuevo holdout queda integrado para matar una reparación que sólo funcione después del siguiente `Platforms.tick`.
