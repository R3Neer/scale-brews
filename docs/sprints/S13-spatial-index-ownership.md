# S13 — Spatial index ownership

Estado: **CERRADO**. G2 continúa abierto por el ownership restante de las tareas 1/9.

## Objetivo

Continuar G2 tareas 1/9 extrayendo de `AnatomyMovement` el ownership persistente del índice espacial Q2, sin mover providers, causal endpoint state, root history ni decisiones físicas.

La frontera es deliberadamente estrecha: `AnatomyMovement` sigue decidiendo qué snapshot/root es causalmente válido y calcula el envelope conservador. `AnatomySpatialIndex` almacena exclusivamente la membresía ya validada, sus budgets y las operaciones bounded de build/upsert/remove/query/lifecycle.

## Hallazgo que justificó el sprint

Tras S12 quedaban en `AnatomyMovement` providers/registration/descriptors y seriales causales, root history, índice espacial por nivel, métricas de sweep y queries/orquestación física.

Root history sigue ligado a `GeometryProvider.CausalEndpoint` y a la futura generalización de G3, por lo que no se extrajo en S13. Las métricas aisladas tampoco justifican una frontera propia.

El índice espacial sí tenía una frontera estable porque ya delegaba la estructura bounded a `MaterialBroadphase<LivingEntity>` y podía recibir entries/envelopes ya validados sin conocer providers, root, gravedad, contactos ni solver.

La auditoría detectó además metadata muerta: `FrameStamp` se almacenaba en `SpatialIndex.frames` y se mantenía junto a la membresía, pero nunca se consultaba para aceptar una query. La revalidación causal real ocurría después mediante `currentSnapshot/queryFrame`. S13 eliminó esa metadata en vez de trasladarla.

## Frontera arquitectónica final

`collision.internal.AnatomySpatialIndex` es el owner único, package-private mientras G2 termina de estabilizar la frontera runtime.

Posee:

1. índice por `Level` y tick;
2. `MaterialBroadphase<LivingEntity>`;
3. `CELL_SIZE`, budgets de celdas/candidatos y orden estable de supports;
4. build/rebuild a partir de `MaterialBroadphase.Entry<LivingEntity>` ya validadas;
5. upsert/remove/query/deactivate;
6. rechazo explícito para que el caller aplique la política causal.

No consulta `GeometryProvider`, no llama `AnatomyMovement`, no construye `RootFrame`, no conoce `AnatomyContactState` ni `Platforms`, no cuarenteniza endpoints, no muestrea geometría y no ejecuta CCD/separation/carry.

Dirección única demostrada: `AnatomyMovement -> AnatomySpatialIndex`.

## Semántica preservada

- Mismo tick: upsert/remove local modifica sólo el support afectado y no remuestrea providers no relacionados.
- Cambio de tick: `AnatomyMovement` suministra un batch de entries causalmente válidas y el owner reemplaza el índice del nivel.
- Overflow/rechazo: nunca degrada a scan mundial; el owner devuelve rejected supports y `AnatomyMovement` conserva cuarentena/contact cleanup.
- Query: el owner devuelve candidatos bounded; elegibilidad y suspensión siguen fuera.
- Lifecycle: `deactivate(level)` borra sólo el índice del nivel objetivo.
- `FrameStamp`, `SpatialIndex.frames` y `currentFrameStamp` desaparecieron sin sustituto.
- Un rebuild legacy muestrea cada provider una sola vez.

## Implementación

- [x] Añadir holdout estructural rojo que exija `AnatomySpatialIndex` y prohíba `SPATIAL`, `SpatialIndex` y `FrameStamp` en `AnatomyMovement`.
- [x] Registrar el rojo ordinario exacto.
- [x] Crear `AnatomySpatialIndex` con estado weak por nivel y budgets actuales sin cambios.
- [x] Mover build/upsert/remove/query/deactivate al nuevo owner.
- [x] Eliminar `FrameStamp`, `SpatialIndex.frames` y `currentFrameStamp` sin sustituto.
- [x] Mantener snapshot/root/envelope y política de rejected/quarantine en `AnatomyMovement`.
- [x] Mantener filtros de elegibilidad/suspensión fuera del owner.
- [x] Reejecutar holdouts S05 de bounded broadphase/localidad/dirty mutation y suite completa.
- [x] Añadir `AnatomySpatialIndex.java` a los triggers de la lane prepared.
- [x] Ejecutar ordinary + prepared sobre el snapshot productivo final.
- [x] Revisión adversarial post-verde: cero callbacks al orquestador, cero metadata causal duplicada, cero resample global same-tick y single-sample rebuild.
- [x] Actualizar documentación de cierre; G2 no se da por cerrado por S13.

## Modelo adversarial y resultado

### A1 — owner único

Verde: existe `AnatomySpatialIndex`; `AnatomyMovement` ya no declara `SPATIAL` ni `SpatialIndex`.

### A2 — metadata causal fantasma retirada

Verde: `FrameStamp`, el mapa `frames` y `currentFrameStamp` desaparecieron. No se recreó la misma metadata con otro nombre.

### A3 — dependencia unidireccional

Verde: el bytecode del owner no referencia `AnatomyMovement`, `GeometryProvider`, `AnatomyContactState` ni `Platforms`.

### A4 — dirty mutation local

Verde: los holdouts S05 siguen demostrando que una mutación same-tick no remuestrea el mundo lejano.

### A5 — overflow fail-closed

Verde: membership y query oversized devuelven rechazo/exhaustion bounded, sin candidatos parciales ni fallback global.

### A6 — lifecycle por nivel

Verde: desactivar un nivel elimina únicamente su índice; otro nivel permanece intacto y un tick stale no se sirve como current.

### A7 — revalidación causal sigue fuera

Verde: `AnatomyMovement` construye entries desde `queryFrame/currentSnapshot`, calcula `spatialEnvelope` y conserva la política de quarantine/rejection. El owner no puede aceptar geometría por sí mismo.

### A8 — regresión cero

Verde: S05-S12, budgets S09 y lane prepared real-geometry permanecen verdes.

### A9 — single-sample rebuild

Verde: un rebuild legacy activado por query muestrea cada provider una sola vez. La eliminación de `currentFrameStamp` no dejó un segundo sample oculto.

## Historia red-before-green

- `2369b45daad9e0a0578343c62fe094e0cc0e8b3d` añadió los holdouts estructurales. Run `34694155736`: **377 tests, 3 fallos previstos**: owner ausente, storage viejo presente y dependencia no auditable todavía.
- `263b818daf1a9298a72df5ec0987a65698809166` añadió el owner puro. Run `34694260384`: el rojo se redujo a **1 único fallo**, porque `AnatomyMovement` seguía poseyendo el storage viejo.
- `097237fcc35633b6bb8cbdc68d0a226b884d8fce` endureció overflow/lifecycle, pero la primera versión usó un nombre de enum inexistente. Fue un fallo de fixture/compilación, no evidencia de producción.
- `6e252e8821a9b3fec3e4234ecb1f7a838ff14748` corrigió sólo ese fixture. Run `34694707067`, job `103556082914`: **379 tests, 378 verdes / 1 rojo**, exclusivamente el ownership espacial todavía residente en `AnatomyMovement`.
- `5f489dff9a535bf9d25d2f4e63dca17f9675a7f8` añadió `AnatomySpatialIndex.java` al trigger de prepared CI.
- `2d5a1b75251decdfc1645f2fdc2394792f356960` migró el storage y eliminó la metadata muerta sin cambiar budgets ni solver.
- `36f15ccd0bf92bc2317cff57680dfbfdf42e8342` añadió después del verde el holdout single-sample; no cambió producción.

## Evidencia final

### Snapshot productivo `2d5a1b75251decdfc1645f2fdc2394792f356960`

Ordinary run `34695088619`, job `103557088207`:

- **379/379 required GameTests passed**;
- `BUILD SUCCESSFUL`;
- artifact `10298357943`;
- SHA-256 `ea5f4f3138f105b837dc53d3acb613bb03b53c64ac91bdb79606ff8f36abee76`.

Prepared run `34695088607`, job `103557088037`:

- export cliente original verde;
- cow: **240 vertices / 10 pieces**, 80 comparaciones animadas;
- player wide/slim: **144 vertices / 6 pieces** cada uno, 80 comparaciones cada uno;
- **640 additional vanilla-family comparisons** verdes;
- servidor preparado aislado: **2/2 required GameTests passed**;
- `BUILD SUCCESSFUL`.

### Revisión post-verde `36f15ccd0bf92bc2317cff57680dfbfdf42e8342`

Sólo añade el holdout `rebuildSamplesEachLegacyProviderOnce`; producción permanece idéntica a `2d5a1b7...`.

Ordinary run `34695273523`, job `103557571998`:

- **380/380 required GameTests passed**;
- `BUILD SUCCESSFUL`;
- artifact `10298329724`;
- SHA-256 `662072bf5e11aaa5f8be6d902ecbb4a1f2d03a9f8bd077496555c6b393ff4654`.

No se requirió ningún cambio de producción tras esta revisión.

## Criterio de cierre

S13 cierra porque:

1. el índice espacial tiene un único owner dedicado;
2. `FrameStamp/frames` muertos se eliminaron;
3. causalidad y física no se trasladaron al owner espacial;
4. locality, overflow, lifecycle y single-sample rebuild están fijados por holdouts;
5. ordinary y prepared son verdes sobre el código productivo final;
6. la revisión posterior converge sin cambios de producción.

**S13 CERRADO. G2 sigue abierto hasta resolver la frontera arquitectónica restante de las tareas 1/9.**
