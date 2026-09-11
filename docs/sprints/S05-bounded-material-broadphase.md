# S05 — Broadphase material acotado

Estado: **REABIERTO POR EVIDENCIA ADVERSARIAL DE LOCALITY LIVE**. Primer sprint de G2.

## 1. Tesis y scope

S05 tiene una sola tesis: **una query material viva debe tener coste espacial local y presupuesto explícito; una query o soporte patológico no puede degradar el índice a un recorrido global de todos los soportes**.

Requisitos primarios: FR-042; NFR-004, NFR-007, NFR-008, NFR-012. Requisitos de apoyo: NFR-001, NFR-002 y NFR-033.

S05 no afirma todavía FR-049..051 ni la resolución material completa de S07.

### Blocker adversarial vigente

El kernel `MaterialBroadphase` sigue siendo local, pero la wrapper viva histórica que decidía si podía reutilizar un `SpatialIndex` no lo era. En una segunda query local dentro del mismo tick, `AnatomyMovement.spatial(...)` recorría todos los `current.frames()` y ejecutaba `currentFrameStamp(...)`; para bindings legacy eso volvía a llamar `provider.sample(...)` sobre cada soporte indexado, aunque estuviera a miles de bloques.

`S05LiveBroadphaseLocalityTests.localQueryMustNotResampleEveryFarWorldSupport` materializa primero un índice con 64 soportes lejanos, resetea contadores y repite la misma query local. La segunda query histórica provocó **64 samples lejanos**. La tesis S05 no estaba satisfecha de extremo a extremo aunque el kernel aislado sí lo estuviera.

El holdout S06 de un provider que mutaba internamente `UNAVAILABLE → AVAILABLE` entre dos queries **sin `tick`, rebind, packet-binding ni callback causal** se retiró como no contractual. No se usa para condicionar la reparación S05: los providers productivos cambian por hooks conocidos del runtime.

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
- **La reutilización live del índice tampoco puede convertir una query local estable en polling de todos los providers registrados.**
- Los cambios productivos de geometría/membresía llegan por `GeometryProvider.tick`, register/rebind, endpoint acceptance o lifecycle hooks; esos puntos pueden invalidar/actualizar el índice sin sondeo global por query.
- Un test no puede exigir discovery de una mutación interna clandestina de un provider internal y simultáneamente usar eso para justificar polling global.

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
- [ ] I10 Hacer local la reutilización/validación del índice live: una query local repetida no puede re-samplear soportes lejanos.
- [ ] I11 Revalidar invalidación/update bajo **hooks productivos legítimos** sin reintroducir polling global por query.
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
12. **live steady-state locality**: segunda query local no re-samplea 64 supports lejanos;
13. **siguiente ataque tras el steady-state**: un cambio legítimo por hook causal no debe convertir la primera query posterior en un rebuild/poll global repetido por cada body; la estrategia de actualización debe tener coste coherente con NFR-008 y con la cadencia de autoridad.

## 6. Implementación histórica

### `MaterialBroadphase<K>`

Añadido por `dbcf315a9ac00d77b43cf442bb2596030b1518c3`.

- `Entry<K>(key, AABB)` sin ownership de mundo;
- identidad por `IdentityHashMap` y comparator total del caller;
- límites de celdas por entrada/query y candidatos;
- `boundedCellCount` corta en `budget+1`;
- query incompleta nunca publica candidatos parciales;
- métricas suficientes para locality.

### Integración viva histórica

`541a333140543fb1df55e61b4aa78bb2f54f84d7` sustituyó el índice embebido por `MaterialBroadphase<LivingEntity>` y eliminó `overflow`/fallback global del kernel. La reapertura posterior demostró que la validación live de frames previa al kernel seguía siendo global.

### Candidato actual `dceddae0…`

`dceddae0e359aa743e8e59d9c706fcc77e661c6c` elimina el `allMatch(current.frames())` de cada query y reutiliza el índice durante el mismo tick, confiando en invalidaciones causales explícitas. También prepara envelopes de discovery para bindings sin geometría disponible.

La dirección arquitectónica es compatible con el holdout de steady-state locality, pero **el primer run del candidato no compila** por una llamada a `union(AABB,AABB)` inexistente en `AnatomyMovement`. Hasta que producción compile y el holdout pase, I10 sigue abierto.

## 7. Evidencia ejecutada

### Cierre histórico

Run `34602837677`, job `103274063124`, sobre `541a333…`:

- 277/277 required GameTests;
- build successful;
- artifact `10265820434`;
- SHA-256 `1573c97d70c3b254dcf6957b57c695c9004b0a37cd8acec1301b7e84cbec8f28`.

### Reapertura live

Run **`34624970288`**, job **`103347750374`**, sobre `311aee5…`:

- 307 GameTests;
- 304 pasaron / 3 fallaron;
- fallo S05 exacto: steady local query vuelve a ejecutar **64** samples de providers lejanos;
- los otros rojos pertenecían a S07 budget uncertainty y a un holdout S06 que posteriormente fue reclassificado y retirado por no representar una transición soportada de provider.

La prueba S05 llega a la aserción de locality; no es un fallo de fixture.

### Primer run del candidato live

Run **`34625585117`**, job **`103349765804`**, contiene `dceddae0…` y la retirada del holdout S06, pero falla en `compileJava` antes de GameTests:

`AnatomyMovement.java:572: cannot find symbol union(AABB,AABB)`.

Por tanto, ese run **no aporta evidencia verde** para I10 ni para S07; sólo demuestra un defecto de compilación del candidato.

## 8. Estado de cierre

- [x] fallback global del kernel eliminado;
- [x] `overflow` global eliminado;
- [x] budgets explícitos;
- [x] fail-closed runtime;
- [x] cuarentena/rebind probados;
- [x] locality del kernel probada;
- [ ] locality de la wrapper live probada tras un fix compilable;
- [ ] comportamiento tras invalidaciones productivas revisado;
- [ ] suite completa verde;
- [ ] pasada estructural final sin cambios;
- [ ] S05 cerrado.

S05 queda **reabierto** por evidencia adversarial contractual de NFR-008; el antiguo holdout S06 clandestino no forma parte de su aceptación.
