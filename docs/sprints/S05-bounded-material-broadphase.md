# S05 — Broadphase material acotado

Estado: **EN EJECUCIÓN**. Primer sprint de G2.

## 1. Tesis y scope

S05 tiene una sola tesis: **una query material viva debe tener coste espacial local y presupuesto explícito; una query o soporte patológico no puede degradar el índice a un recorrido global de todos los soportes**.

Requisitos primarios: FR-042; NFR-004, NFR-007, NFR-008, NFR-012. Requisitos de apoyo: NFR-001, NFR-002 y NFR-033.

S05 no afirma todavía FR-049..051: no integra `MaterialEventDispatcher`, no consume intervalos root/joint exactamente una vez y no convierte el solver endpoint actual en Q2 completo. Tampoco resuelve sliding/multicontacto/recovery ni lifecycle G3.

## 2. Estado inicial

`AnatomyMovement` contiene el índice espacial Q1. `indexedSupports` calcula celdas de una query, pero si supera `MAX_INDEX_CELLS` hace `seen.addAll(index.bounds().keySet())`: una query grande se convierte en scan de todos los soportes indexados. Además, todo soporte cuyo AABB ocupa demasiadas celdas se guarda en `overflow`, y **cada query ordinaria añade la lista completa `overflow`**.

Ese comportamiento contradice NFR-007/008 y hace que el coste dependa del mundo, no de la región consultada. El índice además está incrustado en `AnatomyMovement`, dificultando sustituir bounds endpoint por envelopes temporales en sprints posteriores.

## 3. Invariantes

- Identidad de entidad por instancia, no por `Entity.equals`/network id accidental.
- Orden físico final determinista sigue en el solver, no en el orden interno de buckets.
- Un soporte que no puede indexarse dentro del presupuesto no inventa geometría ni entra como candidato global; se cuarentena/localiza y se liberan contactos afectados.
- Una query que agota presupuesto produce outcome explícito y nunca itera todas las entradas.
- `spaceClear` falla cerrado ante agotamiento.
- movimiento físico no puede interpretar agotamiento como “sin collider”.
- entidades fuera del subsistema conservan física vanilla.
- el índice debe aceptar `AABB` materiales desacoplados del provider para que un sprint posterior pueda pasar envelopes de intervalos sin reescribir el índice.

## 4. Plan de implementación

- [ ] I1 Extraer un índice espacial reusable de `AnatomyMovement` a una frontera física/runtime sin ownership de mundo ni providers.
- [ ] I2 Dar a build/query budgets explícitos de celdas y outcomes `COMPLETE` / `BUDGET_EXHAUSTED`.
- [ ] I3 Mantener deduplicación por identidad y bounds materiales por entrada.
- [ ] I4 Eliminar el fallback de query a `all bounds` y eliminar el `overflow` global por-query.
- [ ] I5 Hacer que soportes cuyo envelope excede el presupuesto de entrada sean rechazados por build y cuarentenados por `AnatomyMovement`.
- [ ] I6 Propagar agotamiento conservador: `spaceClear=false`; `collide` bloquea el movimiento solicitado; consultas auxiliares no afirman un hit inexistente.
- [ ] I7 Añadir instrumentación mínima de `cellsVisited`/`candidateCount`/rechazos para demostrar localidad sin benchmark global prematuro.
- [ ] I8 Añadir GameTests S05 y registrarlos explícitamente en `fabric.mod.json`.
- [ ] I9 Ejecutar suite completa y revisar regresiones de los tests espaciales/temporales existentes.

## 5. Modelo adversarial previo

Los holdouts de S05 deben intentar romper, como mínimo:

1. **query budget +1**: una AABB que ocupa una celda más que el máximo no puede activar scan global;
2. **entry budget +1**: un soporte patológico no puede acabar en una lista revisada por todas las queries;
3. **far-world amplification**: añadir muchos soportes lejanos no cambia `cellsVisited` ni candidatos de una query local;
4. **identity collision**: dos entidades distintas con el mismo network id siguen siendo entradas distintas;
5. **insertion-order permutation**: mismos bounds producen el mismo conjunto material independientemente del orden de build;
6. **fail-closed movement**: una query agotada no devuelve el desplazamiento solicitado como si el espacio estuviera libre;
7. **fail-closed clearance**: una query agotada no devuelve `true`;
8. **locality structural**: el hot query no tiene rama que itere `all entries`, `level.getAllEntities()` ni equivalente;
9. **boundary exacta**: presupuesto exacto es válido; sólo `> budget` agota;
10. **rebind recovery**: un soporte rechazado puede volver a participar tras un rebind con bounds válidos, sin cuarentena eterna accidental.

## 6. Revisión del plan

P1 separa dos fallos distintos: query sobredimensionada y soporte sobredimensionado. Ambos necesitan resultados conservadores diferentes, no un mismo `overflow` mágico.

P2 mantiene el índice desacoplado de `GeometryProvider`: recibe `key → AABB`, por lo que el futuro Q2 puede indexar envelopes temporales. No se adelanta su semántica.

P3 no convierte S05 en el sprint de dispatcher. Eso evita mezclar corrección de complejidad con causalidad exactly-once.

P4 exige instrumentación observable en tests, porque “no parece global” no demuestra NFR-008. No aparece ningún cambio adicional de scope respecto de P3; el plan converge.

## 7. Criterio de cierre

S05 sólo se cierra cuando:

- el fallback global y `overflow` desaparecen del hot query;
- los holdouts de presupuesto/localidad/fail-closed pasan;
- los tests existentes de spatial rebuild/intermediate limb permanecen verdes;
- la suite completa pasa con los nuevos S05 GameTests realmente registrados;
- una revisión final no encuentra cambios adicionales dentro del scope.