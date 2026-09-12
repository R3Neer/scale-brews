# S10 — Passive transport ledger ownership

Estado: **CERRADO**. Sexto sprint de G2.

## 1. Scope

### Tesis

Al terminar S10, la identidad temporal del transporte pasivo de un body —secuencia actual, history acotada, generation de lifecycle y ventana contigua consumible por pose— tiene un único owner en `collision.runtime`, mientras `AnatomyMovement` conserva la aplicación física/orquestación del carry y sólo las façades que todavía tienen consumidores reales. La extracción no cambia displacement, receipts, accounting, contacto ni semántica de lifecycle.

Gate: **G2 — pipeline material continuo Q2**.

### Incluido

- FR-058 como invariante: un carry parcial/rechazado no reaparece como deuda de transporte.
- FR-059 como invariante de secuencias exactly-once en contribuciones derivadas.
- FR-061: transporte pasivo no cuenta como movimiento voluntario/fall/exhaustion/stats.
- NFR-001/002: mismo ledger y orden causal producen la misma ventana.
- NFR-004: hueco, rewind o lifecycle discontinuity fallan cerrado para el consumidor de pose.
- NFR-007: history acotada y outcome `contiguous=false` cuando ya no puede demostrarse continuidad.
- NFR-011: TTL y cap constantes de history de transporte.
- NFR-015/017: mutación física/lifecycle conserva barreras y generation.
- NFR-025: reducir ownership monolítico de `AnatomyMovement` y mover una frontera desacoplable a `collision.runtime` sin ciclo hacia `collision.internal`.
- G2 tareas 1 y 9, sólo en la subresponsabilidad **transport ledger/cursor**.

### Excluido

- Aplicación física de carry, block/entity clip, passengers y baselines.
- `AnatomyTransportReceipts`: sigue siendo el ledger causal de receipts, no se fusiona con el cursor local de transporte.
- Contact/surface/anchor/suspension state.
- Provider registration, endpoint/frame publication, root history y spatial index.
- Reorganización completa de `AnatomyMovement`.
- G3 lifecycle/catalog y G4 prediction/reconciliation.

## 2. Estado final

`collision.runtime.TransportLedger` es el único owner de:

- `current SupportTransport` por body;
- history acotada necesaria para descontar transporte pasivo entre pose samples;
- generation de lifecycle;
- cálculo de `Window(contiguous, latestSequence, appliedDelta)` desde un cursor consumido;
- TTL de **40 ticks** y cap de **64 entradas**;
- limpieza localizada por body y por `Level`.

La implementación conserva weak object identity mediante `MapMaker().weakKeys()`. Dos objetos `Entity` diferentes que comparan iguales por network id no comparten current/history/generation.

`AuthorityPoseTracker` consume `TransportLedger.generation(...)` y `TransportLedger.since(...)` directamente. Éste es el consumidor real que demuestra que la frontera no es un helper autocontenido.

`AnatomyMovement` ya no posee mapas/history/generation de transporte. Sigue siendo owner de la **aplicación física** y produce `SupportTransport` desde:

1. `recordCertifiedTransport(...)`, ruta del dispatcher material;
2. `carry(...)`, fallback/client endpoint carry todavía vigente.

Ambas rutas registran en `TransportLedger`. `invalidateBody(...)` y `deactivate(level)` delegan el lifecycle al ledger. `clear(contact)` no invalida history de transporte ya aplicado.

Se conserva `AnatomyMovement.transport(...)` como façade transitoria porque existen consumidores reales de integración, prediction y regresión. La generation sigue delegada donde la captura causal de endpoint la usa como fence. La migración del consumidor de pose al ledger no crea un segundo owner.

## 3. Implementación

### Checklist

- [x] I1 Crear `collision.runtime.TransportLedger` con weak identity keys, current transport, history, generation y `Window` inmutable.
- [x] I2 Mover al ledger TTL/cap, `current`, `generation`, `since`, `record`, `invalidate` y cleanup por level sin cambiar algoritmos ni valores.
- [x] I3 Migrar `AuthorityPoseTracker` para consumir `TransportLedger` directamente.
- [x] I4 Sustituir en `AnatomyMovement` los mapas/helpers propietarios del ledger por delegación; `recordCertifiedTransport` y `carry` registran por el ledger.
- [x] I5 Hacer que `invalidateBody` y `deactivate(level)` deleguen lifecycle al ledger; liberar contacto ordinario no borra history ya aplicada.
- [x] I6 Mantener `AnatomyMovement.transport(...)` como façade porque sigue teniendo callers reales; no existe un segundo almacén de current/history/generation.
- [x] I7 Añadir/revelar tests adversariales de continuidad, gap/rewind, lifecycle generation, TTL/cap, release entre contribuciones, weak identity/network-id reuse y aislamiento por level.
- [x] I8 Ejecutar la suite completa con regresiones S08/S09 de carry, passive accounting, receipts y chains; revisar dependencias y diff.
- [x] I9 Pasada final sin cambios de producción y registrar evidencia.

### Revisiones del plan

- P1 requisitos: la extracción preserva FR-058/059/061 y ataca NFR-011/025; no cambia comportamiento físico.
- P2 ownership: ledger posee sólo identidad/history/cursor; `AnatomyMovement` conserva aplicación física. No se crea segundo carry engine.
- P3 consumidores: `AuthorityPoseTracker` es consumidor directo real; carry/dispatcher producen entradas mediante `AnatomyMovement`.
- P4 lifecycle: `clear(contact)` no equivale a lifecycle; `invalidateBody(..., discardTransport)` sí incrementa generation y puede descartar current/history.
- P5 boundedness: TTL=40 y cap=64 se conservan exactamente; history insuficiente devuelve ventana no contigua.
- P6 identidad: weak object identity se conserva; network/entity id igual no aliasa ledger.
- P7 dependencias: `collision.runtime.TransportLedger` no importa `collision.internal`; runtime→physics es la única dependencia del ledger.
- P8 simplicidad: no se extrajeron receipts, contacto, provider ni carry application porque introducirían callbacks/ciclos sin necesidad.

**Convergencia:** la revisión final no requiere ampliar scope. La frontera transport ledger/cursor es estable y queda extraída sin alterar ownership físico.

## 4. Resultado adversarial

- **A1 verde:** un release ordinario entre dos contribuciones contiguas no borra el delta pasivo ya aplicado; la ventana posterior suma ambas exactamente.
- **A2 verde:** varias contribuciones entre samples se suman exactamente una vez y el cursor avanza al último sequence.
- **A3 verde:** un gap devuelve `contiguous=false` y delta cero.
- **A4 verde:** cursor futuro/rewind falla cerrado; una secuencia no creciente no fabrica continuidad anterior.
- **A5/A6 verdes:** lifecycle generation cambia en invalidación; `discardTransport=false` conserva history ya aplicada y `true` elimina current/history.
- **A7 verde:** el cap `64` conserva la cola demostrable y rechaza el prefijo podado; el TTL conserva exactamente el último tick dentro de ventana y al `+1` mantiene watermark pero rechaza el cursor viejo.
- **A8 verde:** dos objetos con el mismo network id mantienen estado independiente por identidad de objeto.
- **A9 verde:** `S08PassiveTransportStateTests` sigue demostrando que passive carry no altera velocity, fall distance, exhaustion ni movement stats.
- **A10 verde:** la suite S08/S09 sigue cubriendo receipts, carry certificado, chains y exactly-once sin regresión.
- **A11 verde por inspección + tests:** `TransportLedger` no importa `collision.internal`; weak identity, generation y aislamiento por `Level` quedan fijados por holdouts directos.

### Red-before-green de fixtures

Dos fallos durante la segunda pasada adversarial fueron errores de harness y se conservaron como evidencia, no como bugs ficticios de producción:

1. El primer test TTL usaba `runAfterDelay(40, ...)` con el timeout GameTest por defecto de 20 ticks; moría antes de alcanzar el oracle. Se aumentó `maxTicks` sin tocar producción.
2. Al revelar el holdout de deactivation, éste desactivaba el overworld compartido por otros GameTests y borraba correctamente el estado de los TTL retrasados. Se aisló por dimensión: TTL en Nether, deactivation en End y superviviente en overworld.

Tras esas correcciones de fixture no fue necesario modificar producción.

## 5. Evidencia de cierre

Snapshot probado: `8eb01be17bfec05fa0fa4fd9f2abe792b5b7b52c`.

GitHub Actions run `34683859470`, job `103527259218`:

- `./gradlew build` completó correctamente;
- **357/357 required GameTests passed**;
- incluye los holdouts reservados revelados de TTL exacto/`+1`, release entre contribuciones, network-id reuse y deactivation de un level sin borrar otro;
- incluye las regresiones ordinarias S08/S09;
- artifact `10294278917`;
- SHA-256 del artifact: `e180afafe5dd501ccff0db28f2bf33473d82b7948ac4602563e59ec2bdb5bab2`.

La pasada final sólo contiene cambios de tests/aislamiento posteriores a la extracción productiva del ledger. No fue necesario ampliar budgets ni cambiar física para hacer verdes los holdouts.

## 6. Criterio de cierre

S10 queda cerrado porque:

1. current/history/generation/cursor tienen un único owner en `collision.runtime`;
2. `AuthorityPoseTracker` consume esa frontera directamente;
3. aplicación física y receipts no cambiaron de owner;
4. TTL/cap, gaps, rewind, lifecycle, release y weak identity tienen oracles adversariales directos;
5. cleanup por level es local;
6. no existe dependencia `TransportLedger -> collision.internal`;
7. la suite final completa es verde sin cambios de producción posteriores.

S10 reduce las tareas arquitectónicas 1/9 de G2, pero **no las cierra por completo**. El siguiente trabajo debe extraer otra frontera real o resolver una deuda estructural vigente antes de declarar G2 cerrado; no se adelanta G3.
