# S13 — Spatial index ownership

Estado: **CERRADO**. G2 continúa abierto por el ownership restante de las tareas 1/9.

## Objetivo

Continuar G2 tareas 1/9 extrayendo de `AnatomyMovement` el ownership persistente del índice espacial Q2, sin mover providers, causal endpoint state, root history ni decisiones físicas.

La frontera es deliberadamente estrecha: `AnatomyMovement` sigue decidiendo qué snapshot/root es causalmente válido y calcula el envelope conservador. `AnatomySpatialIndex` almacena exclusivamente la membresía ya validada, sus budgets y las operaciones bounded de build/upsert/remove/query/lifecycle.

## Hallazgo que justificó el sprint

Tras S12 quedaban en `AnatomyMovement` providers/registration/descriptors y seriales causales, root history, índice espacial por nivel, métricas de sweep y queries/orquestación física.

Root history sigue ligado a `GeometryProvider.CausalEndpoint` y a la futura generalización de G3, por lo que no se extrajo en S13. Las métricas aisladas tampoco justifican una frontera propia.

El índice espacial sí tenía una frontera estable porque ya delegaba la estructura bounded a `MaterialBroadphase<LivingEntity>` y podía recibir entries/envelopes ya validados sin conocer providers, root, gravedad, contactos ni solver.

La auditoría detectó además metadata muerta: `FrameStamp` se almacenaba en `SpatialIndex.frames` y se mantenía junto a la membresía, pero nunca se consultaba para aceptar una query. La revalidación causal real ocurría después mediante `currentSnapshot/queryFrame`. S13 eliminó esa metadata en vez de trasladarla. Al hacerlo también desapareció un segundo sample legacy que existía únicamente para construir ese stamp muerto.

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

## Semántica final

- Mismo tick: upsert/remove local modifica sólo el support afectado y no remuestrea providers no relacionados.
- Cambio de tick: `AnatomyMovement` suministra un batch de entries causalmente válidas y el owner reemplaza el índice del nivel.
- Índice instalado más viejo que el caller: `queryIfCurrent` devuelve `null` para exigir rebuild causal.
- Caller más viejo que un índice ya instalado: devuelve outcome incompleto con candidatos vacíos; nunca consume membresía futura ni convierte incertidumbre de lifecycle en excepción.
- Overflow/rechazo: nunca degrada a scan mundial; el owner devuelve rejected/exhaustion y `AnatomyMovement` conserva cuarentena/contact cleanup.
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
- [x] Fijar overflow, lifecycle por nivel y fences stale/current con outcomes fail-closed.
- [x] Fijar single-sample rebuild legacy.
- [x] Añadir `AnatomySpatialIndex.java` a los triggers de la lane prepared.
- [x] Ejecutar ordinary + prepared sobre la cadena final integrada.
- [x] Revisión adversarial post-verde sin callbacks al orquestador, metadata causal duplicada ni resample global same-tick.

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

### A6 — lifecycle por nivel y tick fence

Verde: desactivar un nivel elimina únicamente su índice; otro nivel permanece intacto. Un índice viejo solicita rebuild y un caller viejo frente a un índice más nuevo falla cerrado con candidatos vacíos.

### A7 — revalidación causal sigue fuera

Verde: `AnatomyMovement` construye entries desde `queryFrame/currentSnapshot`, calcula `spatialEnvelope` y conserva la política de quarantine/rejection. El owner no puede aceptar geometría por sí mismo.

### A8 — regresión cero

Verde: S05-S12 y la lane prepared real-geometry permanecen verdes en la cadena final. Una ejecución prepared intermedia reabrió A9 de S09, pero el fallo estaba en `TemporalResponse.resolve(...)` directo y no atravesaba `AnatomySpatialIndex`; se reparó en S09 antes del cierre integrado.

### A9 — single-sample rebuild

Verde: un rebuild legacy activado por query muestrea cada provider una sola vez. La eliminación de `currentFrameStamp` no dejó un segundo sample oculto.

## Historia red-before-green

- `2369b45daad9e0a0578343c62fe094e0cc0e8b3d`: holdouts estructurales. Run `34694155736`: **377 tests, 3 fallos previstos**.
- `263b818daf1a9298a72df5ec0987a65698809166`: owner puro inicial.
- `097237fcc35633b6bb8cbdc68d0a226b884d8fce`: holdouts de overflow/lifecycle; la primera versión contenía un typo de enum de fixture.
- `6e252e8821a9b3fec3e4234ecb1f7a838ff14748`: corrige sólo el fixture. Run `34694707067`, job `103556082914`: **379 tests, 378 verdes / 1 rojo**, exclusivamente ownership viejo aún residente.
- `5f489dff9a535bf9d25d2f4e63dca17f9675a7f8`: añade `AnatomySpatialIndex.java` al trigger prepared.
- `2d5a1b75251decdfc1645f2fdc2394792f356960`: migra el storage y elimina metadata muerta.
- `36f15ccd0bf92bc2317cff57680dfbfdf42e8342`: holdout post-verde de single-sample rebuild.
- `3cd0209b1dd1d01cd2ce949b9a17b0e229411274`: endurece stale/current tick semantics para fail-closed en vez de crash.
- `efb5e736612f1565a83c4ae860bba2da5df3bd5d`: fija la barrera stale-caller en GameTest.

## Evidencia final

### Evidencia estructural S13

`2d5a1b75251decdfc1645f2fdc2394792f356960`:

- ordinary run `34695088619`, job `103557088207`: **379/379**;
- prepared run `34695088607`, job `103557088037`: export original verde y servidor **2/2**.

`36f15ccd0bf92bc2317cff57680dfbfdf42e8342`:

- ordinary run `34695273523`, job `103557571998`: **380/380**;
- fija single-sample rebuild.

### Hardening lifecycle e integración final

`3cd0209b1dd1d01cd2ce949b9a17b0e229411274` es el último cambio productivo propio de S13: distingue índice viejo de caller viejo y falla cerrado ante lifecycle stale.

La lane prepared disparada por ese cambio encontró una reapertura independiente de A9/S09 en `TemporalResponse`; no atravesaba el owner espacial. Ese defecto se cerró en `de44c78c75bcc54ef423af783d35761014b49356` sin cambiar S13 y quedó endurecido por la matriz de traslación prepared de `a1f1a568726d95664c6e8b4a144659c36be0e5cc`.

Snapshot integrado final `a1f1a568726d95664c6e8b4a144659c36be0e5cc`:

- ordinary run `34696004871`, job `103559482658`: **380/380 required GameTests passed**;
- artifact `10298651622`, SHA-256 `2d9d4275bccba84139b0a9d95425197c019a36d6fb28a1d18e6ef0f298137131`;
- prepared run `34696004864`, job `103559482598`: export original verde y servidor aislado **2/2**;
- el prepared incluye además A9 del cow real en varias traslaciones mundiales deterministas, hasta magnitudes cercanas al borde práctico.

## Criterio de cierre

S13 cierra porque:

1. el índice espacial tiene un único owner dedicado;
2. `FrameStamp/frames` muertos se eliminaron;
3. causalidad y física no se trasladaron al owner espacial;
4. locality, overflow, lifecycle, stale-caller y single-sample rebuild están fijados por holdouts;
5. ordinary y prepared son verdes sobre la cadena integrada final;
6. la revisión posterior converge sin nuevos cambios de producción S13.

**S13 CERRADO. G2 sigue abierto hasta resolver la frontera arquitectónica restante de las tareas 1/9.**
