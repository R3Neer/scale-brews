# S05 — Broadphase material acotado

Estado: **REABIERTO POR EVIDENCIA ADVERSARIAL DE LOCALITY LIVE**. Primer sprint de G2.

## 1. Tesis y scope

S05 tiene una sola tesis: **una query material viva debe tener coste espacial local y presupuesto explícito; una query o soporte patológico no puede degradar el índice a un recorrido global de todos los soportes**.

Requisitos primarios: FR-042; NFR-004, NFR-007, NFR-008, NFR-012. Requisitos de apoyo: NFR-001, NFR-002 y NFR-033.

S05 no afirma todavía FR-049..051 ni la resolución material completa de S07.

### Blocker adversarial vigente

El kernel `MaterialBroadphase` es local y `9b34ec8f909f278082a75931d014867c596f0fef` ya eliminó el sondeo global del **steady state**: una segunda query local dentro del mismo tick reutiliza `SPATIAL` sin re-samplear todos los providers.

El blocker vigente es ahora más preciso: **una invalidación legítima local sigue difiriendo un rebuild global a la siguiente query física**. `S05LiveBroadphaseLocalityTests.firstLocalQueryAfterLegitimateRebindMustNotResampleFarWorld` materializa un índice con 64 soportes lejanos, hace un `register(...)`/rebind explícito de un único soporte local, resetea el contador *después* del hook y ejecuta la primera query local posterior. Esa query provoca **128 samples lejanos** porque `rebuildSpatial` llama tanto a `currentSnapshot` como a `currentFrameStamp` sobre los 64 providers legacy.

El test permite que el hook de rebind haga mantenimiento: el contador se resetea después del rebind. Lo que NFR-008 prohíbe es trasladar un trabajo O(todos los soportes) a la siguiente movement/query local.

El holdout S06 de un provider que mutaba internamente `UNAVAILABLE → AVAILABLE` entre dos queries **sin `tick`, rebind, packet-binding ni callback causal** se retiró como no contractual. Se sustituyó por `S06SupportedMembershipCausalityTests`, donde el cambio ocurre dentro de `GeometryProvider.tick`; ese escenario productivo, incluida una pieza material seis bloques fuera del AABB vanilla, pasa en el run actual.

## 2. Estado inicial

`AnatomyMovement` contenía el índice espacial Q1. `indexedSupports` calculaba celdas de una query, pero si superaba `MAX_INDEX_CELLS` hacía `seen.addAll(index.bounds().keySet())`: una query grande se convertía en scan de todos los soportes indexados. Además, todo soporte cuyo AABB ocupaba demasiadas celdas se guardaba en `overflow`, y cada query añadía la lista completa `overflow`.

Ese comportamiento contradecía NFR-007/008 y hacía que el coste dependiera del mundo, no de la región consultada.

## 3. Invariantes

- Identidad de entidad por instancia, no por `Entity.equals`/network id accidental.
- Orden físico final determinista.
- Un soporte que no puede indexarse dentro del presupuesto no entra como candidato global; se rechaza/cuarentena de forma explícita.
- Una query que agota presupuesto produce outcome explícito y nunca itera todas las entradas.
- `spaceClear` y movimiento fallan cerrado ante agotamiento.
- Entidades fuera del subsistema conservan física vanilla.
- El kernel acepta bounds materiales desacoplados del provider.
- **La reutilización live estable no puede convertir una query local en polling de todos los providers registrados.**
- **Una invalidación local productiva tampoco puede diferir un rebuild global repetible a la primera query de cada body.**
- Los cambios productivos de geometría/membresía llegan por `GeometryProvider.tick`, register/rebind, endpoint acceptance o lifecycle hooks; esos puntos pueden actualizar/incrementar el índice sin sondeo global por query.
- Un test no puede exigir discovery de una mutación interna clandestina de un provider y simultáneamente usar eso para justificar polling global.

## 4. Plan de implementación

- [x] I1 Extraer un índice espacial reusable de `AnatomyMovement` a `collision.physics`.
- [x] I2 Budgets explícitos de celdas y outcomes `COMPLETE` / `BUDGET_EXHAUSTED` / `INVALID_QUERY`.
- [x] I3 Deduplicación por identidad y bounds materiales por entrada.
- [x] I4 Eliminar fallback de query a todas las entradas y `overflow` global.
- [x] I5 Rechazar/cuarentenar entradas cuyo envelope exceda el presupuesto.
- [x] I6 Propagar agotamiento conservador.
- [x] I7 Instrumentación `requestedCells` / `cellsVisited` / `candidatesVisited`.
- [x] I8 GameTests S05 registrados.
- [x] I9 Suite histórica y revisión estructural del kernel.
- [x] I10 Hacer local la reutilización steady-state del índice live: una query local repetida no re-samplea soportes lejanos.
- [ ] I11 Hacer local también la invalidación/update bajo **hooks productivos legítimos**; el rebind de un soporte no puede convertir la siguiente query local en rebuild global.
- [ ] I12 Suite completa + revisión final cero-cambios antes de cerrar.

## 5. Modelo adversarial

1. query budget exacto / +1;
2. entry budget +1;
3. far-world amplification del kernel;
4. identity collision;
5. insertion-order permutation;
6. fail-closed movement;
7. fail-closed clearance;
8. ausencia estructural de `all bounds`/`overflow`/`getAllEntities` en hot query;
9. boundary exacta;
10. rebind recovery tras cuarentena;
11. candidate saturation sin partial set;
12. **live steady-state locality**: segunda query local no re-samplea 64 supports lejanos — reparado por `9b34ec8f…`;
13. **post-invalidation locality**: rebind local explícito y primera query posterior no pueden re-samplear soportes lejanos — rojo actual, 128 samples;
14. **membership soportada**: `GeometryProvider.tick` publica `UNAVAILABLE → AVAILABLE` y el rebuild canónico indexa una pieza remota fuera del AABB vanilla — verde actual.

## 6. Implementación histórica y candidato actual

### `MaterialBroadphase<K>`

Añadido por `dbcf315a9ac00d77b43cf442bb2596030b1518c3`.

- `Entry<K>(key, AABB)` sin ownership de mundo;
- identidad por `IdentityHashMap` y comparator total del caller;
- límites de celdas por entrada/query y candidatos;
- `boundedCellCount` corta en `budget+1`;
- query incompleta nunca publica candidatos parciales;
- métricas suficientes para locality.

### Integración viva histórica

`541a333140543fb1df55e61b4aa78bb2f54f84d7` sustituyó el índice embebido por `MaterialBroadphase<LivingEntity>` y eliminó `overflow`/fallback global del kernel. La reapertura posterior demostró que la validación live previa al kernel seguía siendo global.

### Reparación steady-state `9b34ec8f…`

`9b34ec8f909f278082a75931d014867c596f0fef` conserva la parte contractual del candidato previo:

- misma `SPATIAL` dentro del mismo tick se reutiliza sin `allMatch(current.frames())` ni provider polling;
- se eliminó el fallback no contractual basado en AABB vanilla para providers `UNAVAILABLE`;
- `rebuildSpatial` vuelve a indexar sólo geometría material disponible;
- desapareció también la llamada `union(...)` que impedía compilar el candidato anterior.

La reparación cierra steady-state, pero no I11: `register/rebind` elimina `SPATIAL` y la primera query posterior reconstruye todos los providers del nivel.

## 7. Evidencia ejecutada

### Cierre histórico

Run `34602837677`, job `103274063124`, sobre `541a333…`:

- 277/277 required GameTests;
- build successful;
- artifact `10265820434`;
- SHA-256 `1573c97d70c3b254dcf6957b57c695c9004b0a37cd8acec1301b7e84cbec8f28`.

### Reapertura live inicial

Run **`34624970288`**, job **`103347750374`**, sobre `311aee5…`:

- 307 GameTests;
- 304 pasaron / 3 fallaron;
- fallo S05: steady local query ejecutaba **64** samples lejanos.

### Candidato steady-state compilable + invalidación adversarial

Run **`34626316654`**, job **`103352177773`**, sobre `f06ec043ff5c5ea4936829d94e7114cf8eb1cd64`, que contiene `9b34ec8f…` más el holdout post-rebind:

- **308 GameTests registrados y ejecutados**;
- **306 pasaron / 2 fallaron**;
- el holdout steady-state S05 ya pasa;
- `S06SupportedMembershipCausalityTests` pasa, incluida anatomía remota publicada por `provider.tick`;
- `S07BudgetFailureLocalizationTests` pasa;
- fallo S05 exacto: `firstLocalQueryAfterLegitimateRebindMustNotResampleFarWorld` con **`far provider samples=128`**;
- el segundo rojo es un test histórico que muta un provider legacy mediante `support.setPos(...)` directo dentro del tick y queda **pendiente de clasificación contractual**; no se usa aquí como evidencia S05.

Por tanto, I10 queda cerrada y I11 permanece abierta con una reproducción contractual que llega a la aserción de locality.

## 8. Estado de cierre

- [x] fallback global del kernel eliminado;
- [x] `overflow` global eliminado;
- [x] budgets explícitos;
- [x] fail-closed runtime;
- [x] cuarentena/rebind probados;
- [x] locality del kernel probada;
- [x] locality steady-state de la wrapper live probada;
- [ ] locality de la primera query tras invalidación productiva probada;
- [x] membership productiva `provider.tick` con anatomía remota probada;
- [ ] suite completa verde;
- [ ] pasada estructural final sin cambios;
- [ ] S05 cerrado.

S05 queda **reabierto** exclusivamente por la locality post-invalidation contractual de NFR-008; el antiguo holdout S06 clandestino no forma parte de su aceptación.
