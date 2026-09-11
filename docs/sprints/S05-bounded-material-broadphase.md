# S05 — Broadphase material acotado

Estado: **CERRADO**. Primer sprint de G2.

## 1. Tesis y scope

S05 tiene una sola tesis: **una query material viva debe tener coste espacial local y presupuesto explícito; una query o soporte patológico no puede degradar el índice a un recorrido global de todos los soportes**.

Requisitos primarios: FR-042; NFR-004, NFR-007, NFR-008, NFR-012. Requisitos de apoyo: NFR-001, NFR-002 y NFR-033.

S05 no afirma todavía FR-049..051: no integra `MaterialEventDispatcher`, no consume intervalos root/joint exactamente una vez y no convierte el solver endpoint actual en Q2 completo. Tampoco resuelve sliding/multicontacto/recovery ni lifecycle G3.

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

## 5. Modelo adversarial previo y resultado

1. **query budget exacto / +1**: el límite exacto pasa; +1 devuelve `BUDGET_EXHAUSTED` con cero candidatos publicados.
2. **entry budget +1**: el soporte patológico se rechaza con `ENTRY_BUDGET_EXHAUSTED`, no entra en una lista global.
3. **far-world amplification**: 256 soportes lejanos no cambian `cellsVisited=1` ni `candidatesVisited=1` para la query local.
4. **identity collision**: dos keys distintas que colisionan bajo `equals/hashCode` siguen siendo dos candidatos.
5. **insertion-order permutation**: orden de build invertido produce el mismo orden determinista de candidatos.
6. **fail-closed movement**: query viva agotada devuelve `Vec3.ZERO`, no el desplazamiento pedido.
7. **fail-closed clearance**: query viva agotada devuelve `false`.
8. **locality structural**: `AnatomyMovement` no contiene rama de hot query sobre `all bounds`, `overflow` o `level.getAllEntities()`.
9. **boundary exacta**: sólo `> budget` agota.
10. **rebind recovery**: soporte runtime rechazado queda en cuarentena local y un `register` explícito con bounds válidos recupera participación.
11. **candidate saturation**: superar el cap de candidatos devuelve `BUDGET_EXHAUSTED` sin lista parcial engañosa.

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

## 7. Evidencia ejecutada

El kernel aislado pasó GitHub Actions run `34602417017` sobre `dbcf315a9ac00d77b43cf442bb2596030b1518c3`.

La integración viva pasó GitHub Actions run **`34602837677`**, job **`103274063124`**, sobre `541a333140543fb1df55e61b4aa78bb2f54f84d7`:

- **277 tests registrados y ejecutados**;
- **277/277 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 13s`;
- artifact **`10265820434`**;
- SHA-256 **`1573c97d70c3b254dcf6957b57c695c9004b0a37cd8acec1301b7e84cbec8f28`**.

El incremento 268 → 277 corresponde a los nueve holdouts S05 realmente registrados; no es evidencia de compilación sin ejecución.

## 8. Revisión final

La pasada posterior al run verde verificó sobre `541a333…`:

- cero referencias a `overflow` en `AnatomyMovement`;
- cero `getAllEntities` en el hot path;
- ninguna rama de query que recorra `bounds().keySet()` o todas las entradas del índice;
- `MaterialBroadphase.query` sólo recorre el rango de celdas acotado y corta por `maxCandidates`;
- el rebuild por tick puede recorrer los providers registrados para construir el índice, pero NFR-008 prohíbe el scan mundial **por movimiento/query** y el hot query ya es local; optimización incremental del build queda para el budget/performance posterior si la evidencia NFR-014 lo exige;
- los tests previos de movimiento temporal, spatial rebuild y demás regresiones permanecieron verdes dentro de 277/277;
- no se integró todavía dispatcher/interval ownership, por lo que no se sobredeclara FR-049..051.

No apareció otro cambio de implementación dentro del scope. **S05 converge y queda cerrado.**

## 9. Cierre

- [x] fallback global eliminado;
- [x] `overflow` global eliminado;
- [x] presupuesto de entry/query/candidatos explícito;
- [x] outcomes de agotamiento distinguibles;
- [x] fail-closed runtime propagado;
- [x] cuarentena/rebind probados;
- [x] locality instrumentada y probada;
- [x] 277/277 server GameTests;
- [x] pasada estructural final sin cambios;
- [x] S05 cerrado.

El siguiente sprint G2 debe atacar causalidad material continua y exactly-once, no reabrir el broadphase salvo evidencia adversarial nueva.