# S13 — Spatial index ownership

Estado: **PLAN CERRADO / IMPLEMENTACIÓN PENDIENTE**

## Objetivo

Continuar G2 tareas 1/9 extrayendo de `AnatomyMovement` el ownership persistente del índice espacial Q2, sin mover providers, causal endpoint state, root history ni decisiones físicas.

La frontera es deliberadamente estrecha: `AnatomyMovement` seguirá decidiendo qué snapshot/root es causalmente válido y calculará el envelope conservador. Un nuevo owner espacial almacenará exclusivamente la membresía ya validada, sus budgets y las operaciones bounded de build/upsert/remove/query/lifecycle.

## Hallazgo que justifica el sprint

Tras S12 quedan en `AnatomyMovement`:

- providers/registration/descriptors y seriales causales;
- root history;
- índice espacial por nivel;
- métricas de sweep;
- queries/orquestación física.

Root history sigue ligado a `GeometryProvider.CausalEndpoint` y a la futura generalización de G3, por lo que no se extrae en G2. Las métricas aisladas son demasiado pequeñas para justificar una frontera propia.

El índice espacial sí tiene una frontera estable porque ya delega la estructura bounded a `MaterialBroadphase<LivingEntity>` y puede recibir entradas/envelopes ya validados sin conocer providers, root, gravedad, contactos ni el solver.

Además, la auditoría encontró metadata muerta: `FrameStamp` se almacena en `SpatialIndex.frames`, se actualiza/elimina junto a la membresía, pero nunca se consulta para decidir una query. La revalidación causal real ocurre después mediante `currentSnapshot/queryFrame`. S13 elimina esa metadata en vez de trasladarla a un nuevo owner.

## Frontera arquitectónica

Se introduce `collision.internal.AnatomySpatialIndex` como owner único, package-private mientras G2 termina de estabilizar la frontera runtime.

Puede poseer:

1. índice por `Level` y tick;
2. `MaterialBroadphase<LivingEntity>`;
3. `CELL_SIZE`, budgets de celdas/candidatos y orden estable de supports;
4. build/rebuild a partir de `MaterialBroadphase.Entry<LivingEntity>` ya validadas;
5. upsert/remove/query/deactivate;
6. resultado explícito de rechazo para que el caller aplique la política causal correspondiente.

No puede:

- consultar `GeometryProvider`;
- llamar `AnatomyMovement`;
- construir `RootFrame` ni leer root history;
- decidir `Platforms.eligible`, suspensión o contacto;
- cuarentenizar endpoints;
- muestrear geometría;
- ejecutar CCD/separation/carry.

Dirección única: `AnatomyMovement -> AnatomySpatialIndex`.

## Semántica preservada

- Mismo tick: upsert/remove local no reconstruye ni remuestrea supports no relacionados.
- Cambio de tick: el caller suministra el batch de entradas causales y el owner reemplaza el índice completo de ese nivel.
- Overflow/rechazo: nunca degrada a scan mundial; el owner devuelve rejected supports y `AnatomyMovement` decide cuarentena/contact cleanup.
- Query: el owner devuelve candidatos bounded sin aplicar elegibilidad/suspensión. Esos filtros siguen en `AnatomyMovement`.
- Lifecycle: `deactivate(level)` borra sólo el índice del nivel objetivo.
- `FrameStamp`/`frames` desaparecen porque no aportan una validación observable.

## Plan de implementación

- [ ] Añadir holdout estructural rojo que exija `AnatomySpatialIndex` y prohíba `SPATIAL`, `SpatialIndex` y `FrameStamp` en `AnatomyMovement`.
- [ ] Registrar el rojo ordinario exacto.
- [ ] Crear `AnatomySpatialIndex` con estado weak por nivel y budgets actuales sin cambios.
- [ ] Mover build/upsert/remove/query/deactivate al nuevo owner.
- [ ] Eliminar `FrameStamp`, `SpatialIndex.frames` y `currentFrameStamp` sin sustituto.
- [ ] Mantener snapshot/root/envelope y política de rejected/quarantine en `AnatomyMovement`.
- [ ] Mantener filtros de elegibilidad/suspensión fuera del owner.
- [ ] Reejecutar holdouts S05 de bounded broadphase/localidad/dirty mutation y suite completa.
- [ ] Ejecutar prepared si los paths modificados la disparan; no relajar triggers ni budgets para obtener verde.
- [ ] Revisión adversarial post-verde: cero callbacks al orquestador, cero metadata causal duplicada, cero resample global en same-tick mutation.
- [ ] Actualizar plan/validation al cerrar S13. G2 seguirá abierto si providers/frame/query conservan fronteras aún no estables.

## Modelo adversarial

### A1 — owner único

Debe existir `AnatomySpatialIndex`; `AnatomyMovement` deja de declarar `SPATIAL` y el record `SpatialIndex`.

### A2 — metadata causal fantasma retirada

`FrameStamp` y cualquier mapa `frames` asociado al índice desaparecen. No se permite recrear la misma metadata con otro nombre si no participa en una validación observable.

### A3 — dependencia unidireccional

`AnatomySpatialIndex` no referencia `AnatomyMovement`, `GeometryProvider`, `RootFrame`, `AnatomyContactState` ni `Platforms`.

### A4 — dirty mutation local

Un upsert/remove same-tick modifica sólo el support afectado. Los contadores/fixtures S05 deben demostrar que no se remuestrea un provider no relacionado.

### A5 — overflow fail-closed

Build/upsert/query que superan límites mantienen outcome bounded/rejected; no aparece fallback a `level.getAllEntities()` ni scan equivalente.

### A6 — lifecycle por nivel

Desactivar un nivel elimina su índice sin borrar otro nivel ni alterar registration/root/contact state.

### A7 — revalidación causal sigue fuera

Un candidato espacial no basta para aceptar geometría: las rutas live siguen pasando por `currentSnapshot/queryFrame` antes de usar piezas físicas.

### A8 — regresión cero

S05-S12 permanecen verdes, incluidos prepared A8/A9 y los budgets exactos de S09.

## Criterio de cierre

S13 se cierra sólo cuando:

1. el índice espacial tiene un único owner dedicado;
2. `FrameStamp`/`frames` muertos se han eliminado;
3. no se ha movido ninguna decisión causal o física al owner espacial;
4. same-tick locality y bounded overflow siguen demostrados;
5. ordinary y prepared requeridos quedan verdes sobre el snapshot final;
6. una revisión posterior no exige cambios de producción.
