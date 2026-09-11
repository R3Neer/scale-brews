# S05 — Broadphase material acotado

Estado: **REABIERTO POR EVIDENCIA ADVERSARIAL DE LOCALITY LIVE**. Primer sprint de G2.

## 1. Tesis y scope

S05 tiene una sola tesis: **una query material viva debe tener coste espacial local y presupuesto explícito; una query o soporte patológico no puede degradar el índice a un recorrido global de todos los soportes**.

Requisitos primarios: FR-042; NFR-004, NFR-007, NFR-008, NFR-012. Requisitos de apoyo: NFR-001, NFR-002 y NFR-033.

S05 no afirma todavía FR-049..051: no integra `MaterialEventDispatcher`, no consume intervalos root/joint exactamente una vez y no convierte el solver endpoint actual en Q2 completo. Tampoco resuelve sliding/multicontacto/recovery ni lifecycle G3.

### Blocker adversarial vigente

El kernel `MaterialBroadphase` sigue siendo local, pero la **wrapper viva** que decide si puede reutilizar un `SpatialIndex` no lo es. En una segunda query local dentro del mismo tick, `AnatomyMovement.spatial(...)` recorre todos los `current.frames()` y ejecuta `currentFrameStamp(...)`; para bindings legacy eso vuelve a llamar `provider.sample(...)` sobre cada soporte indexado, incluso si está a miles de bloques de la query.

`S05LiveBroadphaseLocalityTests.localQueryMustNotResampleEveryFarWorldSupport` materializa primero un índice con 64 soportes lejanos, resetea un contador de samples y repite la misma query local. La segunda query provoca **64 samples lejanos**. Por tanto, la tesis S05 no está satisfecha en la integración live aunque el kernel espacial aislado sí lo esté.

Este blocker además restringe la reparación S06 de membership same-tick: no es aceptable solucionar freshness ampliando el polling global de providers por query. Freshness causal y locality deben coexistir.

## 2. Estado inicial

`AnatomyMovement` contenía el índice espacial Q1. `indexedSupports` calculaba celdas de una query, pero si superaba `MAX_INDEX_CELLS` hacía `seen.addAll(index.bounds().keySet())`: una query grande se convertía en scan de todos los soportes indexados. Además, todo soporte cuyo AABB ocupaba demasiadas celdas se guardaba en `overflow`, y **cada query ordinaria añadía la lista completa `overflow`**.

Ese comportamiento contradecía NFR-007/008 y hacía que el coste dependiera del mundo, no de la región consultada. El índice además estaba incrustado en `AnatomyMovement`, dificultando sustituir bounds endpoint por envelopes temporales en sprints posteriores.

## 3. Invariantes

- Identidad de entidad por instancia, no por `Entity.equals`/network id accidental.
- Orden físico final determinista sigue en el solver, no en el orden interno de buckets.
- Un soporte que no puede indexarse dentro del presupuesto no inventa geometría ni entra como candidato global; se cuarentena/localiza y se liberan contactos afectados.
- Una query que agota presupuesto produce outcome explícito y nunca itera todas las entradas.
- `spaceClear` falla cerrado ante agotamiento.
- movimiento físico no puede interpretar agotamiento como “sin collider”.
- entidades fuera del subsistema conservan física vanilla.
- el índice acepta `AABB` materiales desacoplados del provider para que un sprint posterior pueda pasar envelopes de intervalos sin reescribir el índice.
- **la validación/reutilización live del índice tampoco puede convertir una query local en polling de todos los providers registrados**.

## 4. Plan de implementación

- [x] I1 Extraer un índice espacial reusable de `AnatomyMovement` a `collision.physics` sin ownership de mundo ni providers.
- [x] I2 Dar a build/query budgets explícitos de celdas y outcomes `COMPLETE` / `BUDGET_EXHAUSTED` / `INVALID_QUERY`.
- [x] I3 Mantener deduplicación por identidad y bounds materiales por entrada.
- [x] I4 Eliminar el fallback de query a `all bounds` y eliminar el `overflow` global por-query.
- [x] I5 Hacer que soportes cuyo envelope excede el presupuesto de entrada sean rechazados por build y cuarentenados por `AnatomyMovement`.
- [x] I6 Propagar agotamiento conservador: `spaceClear=false`; `collide` bloquea el movimiento solicitado; consultas auxiliares no afirman un hit inexistente.
- [x] I7 Añadir instrumentación mínima de `requestedCells`/`cellsVisited`/`candidatesVisited` y rechazos.
- [x] I8 Añadir GameTests S05 y registrarlos explícitamente en `fabric.mod.json`.
- [x] I9 Ejecutar suite completa y revisar regresiones de tests espaciales/temporales existentes.
- [ ] I10 Hacer local también la reutilización/validación del índice live: una query local repetida no puede re-samplear todos los soportes lejanos.
- [ ] I11 Revalidar S05 junto con el holdout S06 first-consumer para impedir una reparación que intercambie locality por freshness.

## 5. Modelo adversarial previo y resultado

1. **query budget exacto / +1**: el límite exacto pasa; +1 devuelve `BUDGET_EXHAUSTED` con cero candidatos publicados.
2. **entry budget +1**: el soporte patológico se rechaza con `ENTRY_BUDGET_EXHAUSTED`, no entra en una lista global.
3. **far-world amplification**: 256 soportes lejanos no cambian `cellsVisited=1` ni `candidatesVisited=1` para la query local del kernel.
4. **identity collision**: dos keys distintas que colisionan bajo `equals/hashCode` siguen siendo dos candidatos.
5. **insertion-order permutation**: orden de build invertido produce el mismo orden determinista de candidatos.
6. **fail-closed movement**: query viva agotada devuelve `Vec3.ZERO`, no el desplazamiento pedido.
7. **fail-closed clearance**: query viva agotada devuelve `false`.
8. **locality structural**: `AnatomyMovement` no contiene rama de hot query sobre `all bounds`, `overflow` o `level.getAllEntities()`.
9. **boundary exacta**: sólo `> budget` agota.
10. **rebind recovery**: soporte runtime rechazado queda en cuarentena local y un `register` explícito con bounds válidos recupera participación.
11. **candidate saturation**: superar el cap de candidatos devuelve `BUDGET_EXHAUSTED` sin lista parcial engañosa.
12. **live cache locality — ROJO**: con 64 soportes lejanos ya indexados, una segunda query local del mismo tick vuelve a ejecutar 64 samples de provider antes de consultar el kernel.

## 6. Implementación

### `MaterialBroadphase<K>`

Añadido en `collision.physics` por `dbcf315a9ac00d77b43cf442bb2596030b1518c3`.

- recibe `Entry<K>(key, AABB)` y no conoce `Level`, `Entity` ni `GeometryProvider`;
- usa `IdentityHashMap` para identidad y comparator total del caller para orden estable;
- limita celdas por entrada, celdas por query y candidatos visitados;
- `boundedCellCount` corta en `budget+1` y evita overflow aritmético;
- una query incompleta nunca publica candidatos parciales;
- expone métricas suficientes para demostrar locality sin adelantar el benchmark G9.

### Integración viva

`541a333140543fb1df55e61b4aa78bb2f54f84d7` sustituyó el índice embebido de `AnatomyMovement` por `MaterialBroadphase<LivingEntity>`.

- desaparecen `Cell`, el mapa `cells` local, `overflow` y el fallback `all bounds` de `indexedSupports`;
- `rebuildSpatial` materializa bounds de piezas y cuarentena entradas rechazadas;
- `indexedSupports` sólo filtra los candidatos locales devueltos por el kernel;
- `spaceClear`, `raycast`, `sweep` y `collide` distinguen query incompleta y fallan de forma conservadora;
- la validación de frame dentro del mismo tick se conserva antes de reutilizar el índice;
- `snapshotBounds` sigue alimentando endpoints instantáneos en S05, pero la frontera del kernel ya admite AABB de envelopes certificados para S06+.

La reapertura demuestra que la frase anterior “la validación de frame se conserva” ocultaba un coste global en la capa live. El kernel sigue siendo correcto; el owner de mundo necesita una estrategia de invalidación/freshness que no vuelva a consultar todos los providers por query.

## 7. Evidencia ejecutada

El kernel aislado pasó GitHub Actions run `34602417017` sobre `dbcf315a9ac00d77b43cf442bb2596030b1518c3`.

La integración viva histórica pasó GitHub Actions run **`34602837677`**, job **`103274063124`**, sobre `541a333140543fb1df55e61b4aa78bb2f54f84d7`:

- **277 tests registrados y ejecutados**;
- **277/277 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 13s`;
- artifact **`10265820434`**;
- SHA-256 **`1573c97d70c3b254dcf6957b57c695c9004b0a37cd8acec1301b7e84cbec8f28`.

El incremento 268 → 277 corresponde a los nueve holdouts S05 realmente registrados; no es evidencia de compilación sin ejecución.

### Evidencia de reapertura

GitHub Actions run **`34624970288`**, job **`103347750374`**, sobre `311aee5c4dbe039cc971a7f16d3ea27f8180af7b`:

- **307 tests registrados y ejecutados**;
- **304 pasaron / 3 fallaron**;
- fallo S05 exacto: `S05LiveBroadphaseLocalityTests.localQueryMustNotResampleEveryFarWorldSupport`;
- diagnóstico: `far provider samples=64` en la segunda query local del mismo authority tick;
- los otros dos rojos pertenecen a blockers ya separados de S06 first-consumer membership y S07 budget uncertainty.

La prueba S05 compila y llega a la aserción de locality; no es un fallo de fixture ni de presupuesto del kernel.

## 8. Revisión histórica y reapertura

La pasada posterior al run verde histórico verificó sobre `541a333…`:

- cero referencias a `overflow` en `AnatomyMovement`;
- cero `getAllEntities` en el hot path;
- ninguna rama de query que recorra `bounds().keySet()` o todas las entradas del índice;
- `MaterialBroadphase.query` sólo recorre el rango de celdas acotado y corta por `maxCandidates`;
- los tests previos permanecían verdes dentro de 277/277.

Esa revisión fue correcta pero incompleta: comprobó que el **kernel query** no hacía un scan global, pero no instrumentó el trabajo previo de `spatial()` para decidir si reutilizaba el índice. La evidencia nueva demuestra que ese preámbulo sí amplifica con soportes lejanos.

Por tanto, el cierre histórico queda supersedido. S05 no puede volver a cerrarse hasta que la query live sea local de extremo a extremo y siga detectando cambios causales same-tick sin polling global.

## 9. Estado de cierre

- [x] fallback global del kernel eliminado;
- [x] `overflow` global eliminado;
- [x] presupuesto de entry/query/candidatos explícito;
- [x] outcomes de agotamiento distinguibles;
- [x] fail-closed runtime propagado;
- [x] cuarentena/rebind probados;
- [x] locality del kernel instrumentada y probada;
- [ ] locality de la wrapper live probada;
- [ ] compatibilidad simultánea con freshness causal S06 probada;
- [ ] suite completa verde tras la reparación;
- [ ] pasada estructural final sin cambios;
- [ ] S05 cerrado.

S05 queda **reabierto** por evidencia adversarial nueva, exactamente la condición que su cierre original reservaba para reabrirlo.