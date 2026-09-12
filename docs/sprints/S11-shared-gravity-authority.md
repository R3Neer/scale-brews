# S11 — Autoridad compartida de gravedad

Estado: **CERRADO**

## Objetivo

Cerrar la reconciliación de autoridad de gravedad pendiente de G2: Scale Brews debe tener una sola fuente de verdad para la dirección de gravedad de una entidad y Entity Collisions debe consumirla, no poseer una autoridad paralela.

Este sprint no abre G3 ni porta cambios funcionales de Tiny Mounts. El alcance es estrictamente de autoridad, integración y seams de prueba necesarios para preservar FR-050 y los invariantes físicos ya cerrados en S08/S09.

## Baseline observado

Al abrir S11, `chatgpt-editing` tenía dos capas capaces de decidir la gravedad efectiva dentro de Entity Collisions:

1. `collision.internal.GravityFrames`, con un adaptador global instalable por owner.
2. `AnatomyMovement.GRAVITY`, un mapa por entidad con precedencia sobre ese adaptador.

Además, `ScaleAnatomyBackend.gravity(...)` delegaba en `AnatomyMovement.gravity(...)`, por lo que la API pública podía observar la autoridad local de collisions en vez de una autoridad compartida de Scale Brews.

`main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` ya contenía la dirección arquitectónica que debía reconciliarse: `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como autoridad única de gravedad corporal. S11 adopta ese principio sin duplicar el `GravityFrame` físico de la API de Entity Collisions.

## Arquitectura cerrada

- La autoridad global vive en `io.github.r3neer.scalebrews.integration.gravity.GravityFrames`.
- `collision.api.GravityFrame` sigue siendo la representación física canónica del pipeline de Entity Collisions. No existe un segundo DTO de frame con semántica solapada.
- La autoridad compartida expone dirección cardinal y frame, con fallback vanilla `DOWN` y compat opcional con Gravity Changer.
- Solo puede existir un proveedor global externo. La instalación se identifica por `owner`; reinstalar el mismo owner es un no-op completo y un owner distinto falla explícitamente.
- `AnatomyMovement` ya no conserva un mapa `GRAVITY` ni decide precedencia entre fuentes de gravedad.
- `ScaleAnatomyBackend.gravity(...)` y `AnatomyMovement.gravity(...)` observan la misma autoridad compartida.
- `collision.internal.GravityFrames` se eliminó de producción.
- La gravedad sintética por entidad usada por GameTest se conserva como seam explícito dentro de la propia autoridad compartida y está bloqueada fuera de `isDevelopmentEnvironment()`; no constituye una segunda autoridad de producción.
- Un override de test puede representar cualquiera de las seis direcciones, incluido `DOWN`; limpiar un override es una operación explícita distinta y nunca resetea el proveedor global.
- `deactivate(Level)` solo limpia los overrides de test asociados al nivel y nunca resetea ni sustituye el proveedor global.
- El fixture legado de `AnatomyGeometryTests` usa un puente **solo en `src/gametest`** que no posee estado y delega al servicio compartido.

## Requisitos e invariantes cubiertos

- FR-050: compatibilidad con gravedad personalizada/cardinal.
- FR-049 / FR-051: transporte y soporte continúan interpretándose en el frame de gravedad corporal correcto.
- NFR-002: resolución determinista de autoridad.
- NFR-004: fallback seguro a gravedad vanilla cuando no hay proveedor.
- NFR-015 / NFR-017: lifecycle y rebind no dejan estado fantasma de gravedad.
- NFR-025: una sola autoridad/owner para el dato físico de gravedad.

## Plan de implementación

- [x] Añadir el holdout rojo de S11 que demuestre que aún faltaba la autoridad compartida y que `AnatomyMovement`/`collision.internal.GravityFrames` seguían siendo owners paralelos.
- [x] Registrar evidencia roja en CI sin reinterpretar fallos ajenos como fallo de S11.
- [x] Introducir `integration.gravity.GravityFrames` con fallback vanilla, compat Gravity Changer y ownership explícito del proveedor.
- [x] Migrar `ScaleAnatomyBackend.installGravityAdapter(...)` y `gravity(...)` a la autoridad compartida.
- [x] Migrar `AnatomyMovement.gravity(...)` a la autoridad compartida y eliminar `GRAVITY`.
- [x] Retirar `collision.internal.GravityFrames` de producción.
- [x] Mover la inyección de gravedad sintética de GameTest a un seam explícito de desarrollo sin crear una segunda autoridad de producción.
- [x] Añadir holdouts verdes para vanilla, seis direcciones cardinales, ownership de proveedor y coincidencia API/pipeline.
- [x] Reejecutar los holdouts de S08/S09 que ejercitan gravedad lateral y transporte continuo dentro de la suite ordinaria completa.
- [x] Ejecutar CI ordinario y prepared adversarial sobre el estado final de la autoridad compartida.
- [x] Revisar adversarialmente la semántica de reinstalación del mismo owner: el resolver original queda inmutable.
- [x] Separar explícitamente `overrideForTests(..., DOWN)` de `clearOverrideForTests(...)`, de modo que DOWN pueda probarse incluso si el proveedor global resuelve otra dirección.
- [x] Hacer que cambios futuros en `integration.gravity.GravityFrames` activen automáticamente la lane prepared real-geometry.
- [x] Registrar evidencia final y dejar S11 listo para reflejarse en `ENTITY_COLLISIONS_PLAN.md` y `VALIDATION.md`; G2 sigue abierto por las tareas arquitectónicas 1 y 9.

## Holdouts adversariales

### A1 — autoridad compartida presente — VERDE

Existe `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como servicio compartido de Scale Brews.

### A2 — sin owner local en AnatomyMovement — VERDE

`AnatomyMovement` ya no declara `GRAVITY` ni otro registro por entidad que preceda a la autoridad compartida.

### A3 — sin autoridad interna duplicada — VERDE

`io.github.r3neer.scalebrews.collision.internal.GravityFrames` desapareció del código de producción.

### A4 — API y pipeline coinciden — VERDE

`AnatomyApi.gravity(entity)`, `AnatomyMovement.gravity(entity)` y `integration.gravity.GravityFrames.frame(entity)` se prueban contra el mismo frame efectivo.

### A5 — vanilla seguro — VERDE

Sin override de fixture ni proveedor no-DOWN, la resolución es `Direction.DOWN` / `GravityFrame.VANILLA`.

### A6 — seis direcciones cardinales — VERDE

Los seis `Direction.values()` atraviesan la misma autoridad, incluido un `DOWN` inyectado explícitamente por el seam de test. Conservan `toLocal(toWorld(v)) == v` dentro de tolerancia y mantienen `supports(up())`.

### A7 — ownership exclusivo e inmutable — VERDE

Un owner competidor produce `IllegalStateException`. Reinstalar el mismo owner conserva tanto el owner como el resolver original; el holdout usa un resolver contradictorio y verifica que no lo sustituye.

### A8 — independencia body/support preservada — VERDE

La suite final incluye `S08GravityIndependenceTests`, por lo que una gravedad corporal distinta de la del soporte sigue siendo válida para el carry certificado.

### A9 — sliding lateral preservado — VERDE

La suite final incluye `S09LiveOwnMoveTests` y el resto de S09; los casos de gravedad lateral siguen verdes y no reaparece una suposición global de `DOWN`.

### A10 — lifecycle/test cleanup sin estado fantasma — VERDE

El seam de test limpia un override por entidad de forma explícita y restaura la gravedad del proveedor/fallback sin cambiar `owner()`. `AnatomyMovement.deactivate(Level)` limpia únicamente overrides de test del nivel; el proveedor global no se rebobina ni se reemplaza.

## Evidencia roja

El primer commit de holdouts (`c8ad81e2f81ce0cfd4d16a85c2fac77310507b2d`) compilaba, pero la clase no estaba registrada en `fabric.mod.json`; ese verde inicial **no cuenta** como prueba porque los holdouts no se ejecutaron.

`9007726e6e81dc419671dc177fb436e8a10c1f4b` registró correctamente `S11GravityAuthorityTests`.

GitHub Actions run **`34690210541`**, job **`103544012659`**:

- ejecutó **361 GameTests**;
- fallaron exactamente **3** required tests, todos de S11;
- los fallos demostraron: ausencia de `integration.gravity.GravityFrames`, presencia de `AnatomyMovement.GRAVITY` y existencia de `collision.internal.GravityFrames`;
- no hubo otro fallo ordinario que contaminase el rojo.

Esto constituye la evidencia red-before-green de S11.

## Reparación de producción

La migración se repartió en cambios pequeños para evitar mezclar física con ownership:

- `a3c2879d8017a9abcfdca0e5335b7cbe77d1f5bc` — añade `integration.gravity.GravityFrames` con proveedor único, owner, fallback vanilla, Gravity Changer y seam de desarrollo;
- `5d13c1ea2d60b1a201849c3a79accbb46e1b588b` — inicializa la autoridad compartida antes que los subsistemas consumidores;
- `105ae43876783cb9e19c8708ba72f01083d520b7` — enruta `ScaleAnatomyBackend` y el adaptador público a la autoridad compartida;
- `e1cc8d3d51fc689a7934cbaf3a3ab490856ab899` — elimina el ownership local de `AnatomyMovement`, sin churn de CCD/carry;
- `ee8f6309199936741df0000d7219fef4b31cb798` — elimina `collision.internal.GravityFrames` de producción;
- `1e7cbade4656a41223e4e00aa1af52bbed438cbd` — añade un puente de fixture solo en `src/gametest` para el antiguo `AnatomyGeometryTests`; no posee estado y delega a la autoridad compartida;
- `a4c406c8638e63d4daeee56cb7450b01fdf562c2` — refuerza A7 probando que reinstalar el mismo owner tampoco puede cambiar el resolver;
- `474c678a3c73137ec9719cbad788b6d8dc61950b` — corrige el seam de pruebas: `DOWN` deja de significar implícitamente “borrar override”, añade `clearOverrideForTests(...)` y fija A10;
- `52770bc04d2945f800bf06f78c7e4e33f5bcc6f0` — añade la autoridad compartida de gravedad a los triggers de la lane prepared.

### Reapertura posterior al primer cierre

El cierre provisional sobre `a4c406c...` se reabrió durante la revisión adversarial. Se detectó que `overrideForTests(entity, GravityFrame.VANILLA)` eliminaba el override en lugar de representar explícitamente `DOWN`. Con un proveedor global no-DOWN, el fixture no habría podido probar las seis direcciones de forma independiente del proveedor.

La corrección `474c678...` separa dos operaciones que antes estaban sobrecargadas:

1. `overrideForTests(entity, frame)` fija cualquier frame cardinal, incluido vanilla/DOWN;
2. `clearOverrideForTests(entity)` elimina exclusivamente el override local.

El cambio no altera la semántica física de producción ni el ownership global. La revisión también detectó que la lane prepared no se disparaba ante cambios del nuevo owner; `52770bc...` corrigió el filtro de paths.

## Evidencia verde final

Snapshot probado: **`52770bc04d2945f800bf06f78c7e4e33f5bcc6f0`**.

### Suite ordinaria

GitHub Actions run **`34691237169`**, job **`103546736147`**:

- checkout exacto `52770bc04d2945f800bf06f78c7e4e33f5bcc6f0`;
- **365/365 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 3s`;
- artifact **`10297365579`**;
- SHA-256 **`1dc9b7587cef827a4b0f74ed839b58bea3045f49369e743caaf02fa5691ddeae`**.

La suite incluye los holdouts S11 finales, `S08GravityIndependenceTests`, `S09LiveOwnMoveTests` y el resto de regresiones ordinarias S05-S10.

### Prepared adversarial real-geometry

GitHub Actions run **`34691237142`**, job **`103546736010`**, sobre el mismo snapshot `52770bc...`:

- export cliente original completado correctamente;
- `minecraft:cow`: **240 vertices / 10 pieces**, 80 comparaciones de pose;
- **640** comparaciones adicionales de familias vanilla;
- `minecraft:player_wide`: **144 vertices / 6 pieces**, 80 comparaciones de pose;
- `minecraft:player_slim`: **144 vertices / 6 pieces**, 80 comparaciones de pose;
- el servidor prepared ejecutó **2/2 required GameTests passed**;
- export cliente: `BUILD SUCCESSFUL in 1m 37s`;
- suite prepared servidor: `BUILD SUCCESSFUL in 17s`.

Los avisos de narrator/ALSA/X11/autenticación del runner fueron no fatales y no afectaron la exportación ni los oracles.

## Revisión final

La revisión posterior al verde no encuentra una segunda autoridad de producción:

- el servicio compartido posee la elección efectiva de gravedad;
- `collision.api.GravityFrame` conserva únicamente la representación/transformación física;
- `ScaleAnatomyBackend` no vuelve a crear estado de gravedad;
- `AnatomyMovement` no posee un almacén de gravedad;
- el override por entidad queda restringido al entorno de desarrollo/GameTest dentro de la misma autoridad y tiene cleanup explícito;
- el puente legado vive solo en source-set de test y no almacena estado;
- un owner no puede sustituir a otro ni cambiar silenciosamente su resolver mediante reinstalación;
- lifecycle/cleanup de tests no rebobina el proveedor global;
- cualquier cambio futuro en la autoridad compartida activa la prueba prepared de geometría real.

No se identifica ningún cambio adicional de producción para S11 tras esta revisión.

## Cierre

**S11 está CERRADO.** `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` es la autoridad compartida de gravedad que consumen Entity Collisions y la API pública, y el cierre final está validado sobre `52770bc...` en las lanes ordinaria y prepared.

Este cierre **no cierra G2**. Permanecen las tareas arquitectónicas de partición/ownership de G2, especialmente la división del estado/orquestación todavía concentrado en `AnatomyMovement` y la retirada/migración de tipos físicos u orquestadores que siguen en `collision.internal` cuando sus fronteras estén estabilizadas.
