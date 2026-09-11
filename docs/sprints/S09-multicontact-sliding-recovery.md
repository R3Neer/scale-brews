# S09 — Multicontacto, sliding y recovery material

Estado: **PLAN + MODELO ADVERSARIAL CONVERGIDOS / IMPLEMENTACIÓN EN REVISIÓN**. Quinto sprint de G2.

## 1. Scope

### Tesis

Al terminar S09, el pipeline live debe resolver movimiento propio y movimiento material con contactos múltiples de forma determinista, conservar un contacto tangencial mientras siga siendo materialmente válido, deslizar sólo en el subespacio permitido por las normales activas y recuperar/suspender solapamientos de forma acotada y localizada; además, un intervalo material publicado debe poder establecer o reacquirir contacto aunque el body no ejecute movimiento propio, sin magnetizar por mera proximidad de endpoint.

Gate: **G2 — pipeline material continuo Q2**.

### Incluido

- FR-043: contacto estable cara/arista/esquina/tangencia.
- FR-044: movimiento propio en frame de gravedad con sliding tangencial.
- FR-047: retención tangencial previa sin nuevo hit si el contacto final sigue válido.
- FR-048: múltiples candidatos/contactos, resultado estable e independiente de orden accidental.
- FR-049: contacto estrictamente intermedio del intervalo material.
- FR-052: separation/recovery acotado y fallo localizado por pareja.
- FR-053 como invariante de localidad de pareja.
- NFR-001/002: determinismo y orden canónico.
- NFR-004: fallo cerrado localizado.
- NFR-007/008: budgets explícitos y hot path local.
- NFR-015/017: world-thread y barreras causales existentes.
- NFR-025: no introducir un segundo solver/owner.
- NFR-033: terceros no participantes permanecen vanilla.

### Excluido

- G3 lifecycle/catalog/reload más allá de conservar sus fences existentes.
- G4 prediction/reconciliation/observer presentation.
- G5 sneak/jump y cuerpos especiales.
- benchmark normativo completo de G9.
- extracción estética de `AnatomyMovement`; sólo se extrae una frontera si el trabajo físico la vuelve real.

## 2. Estado inicial e investigación

### Kernel ya existente

1. `TemporalResponse` ordena piece ids con `TreeSet`, mantiene constraints activas en `TreeMap` y, si varios hits comparten la primera fracción dentro de `TIME_EPS`, activa **todos** esos contactos.
2. `proposal(...)` proyecta el desplazamiento contra todas las restricciones activas; por construcción bloquea sólo la componente normal necesaria y conserva el movimiento tangencial compatible.
3. `TemporalResponse` tiene budgets explícitos de eventos y evaluaciones y devuelve `ITERATION_LIMIT` al agotarse.
4. `AnatomySeparation` usa una priority queue ordenada por distancia/x/y/z, limita distancia y candidatos y valida cada escape con block clip y sweeps contra piezas inicialmente libres.
5. `S00TemporalTests.holdoutH05EqualContactsIgnoreMapOrderAndSignedZero` ya demuestra en kernel dos contactos simultáneos, permutaciones y signed zero.
6. `S00TemporalTests.holdoutH01RootReturnAndCounterJointCannotHideInteriorCollision` ya demuestra contacto estrictamente intermedio con endpoints libres en kernel.

### Integración live existente

1. `AnatomyMovement.collide` usa índice material local, `AnatomySeparation(4,128)`, `TemporalResponse(32,256)` y block clip real.
2. Si la separación inicial no puede resolverse, suspende sólo los supports que realmente solapan al body y elimina únicamente esas motions del intento actual.
3. Tras el sweep, ejecuta recovery final acotado y suspende sólo las relaciones que siguen solapando.
4. La retención tangencial se intenta primero con `finalSupportNormal(...)`; por tanto un contacto previo no necesita aparecer otra vez en `response.contacts()` si sigue materialmente válido.
5. Un contacto nuevo se selecciona desde hits CCD reales; sólo una corrección inicial no nula permite considerar un candidato final válido. La mera proximidad de endpoint separado no magnetiza contacto.
6. `MaterialPhysicsRuntime.plan(...)` ejecuta `TemporalResponse` con movimiento propio cero contra motions materiales; por ello puede, en principio, detectar contacto generado exclusivamente por el intervalo del soporte.
7. `MaterialPhysicsRuntime.apply(...)` sólo intenta `establish(...)` con pieces presentes en `contactPieces`, que proceden de hits CCD o overlap inicial real; no usa proximidad arbitraria.
8. `MaterialPhysicsRuntime.Metrics` expone admitted/candidates/evaluations/quarantined/exhausted; `AnatomyMovement.SweepMetrics` expone queries/pieces/evaluations/exhausted. Hay instrumentación útil, pero S09 debe demostrar fronteras live en lugar de inferirlas por existencia de contadores.

### Deuda demostrable

- no hay prueba live equivalente al holdout kernel de contacto simultáneo/permutación;
- no hay prueba live fuerte de retención tangencial sin hit nuevo bajo movimiento propio;
- la prueba de squeeze existente usa un provider local/manual y documenta expresamente que falta demostrar reacquisition por **intervalo runtime publicado**;
- wall squeeze/recovery live debe demostrar que sólo se suspende la pareja mala y que un bystander seguro sigue autoritativo;
- falta una prueba live donde el contacto exista sólo en mitad del intervalo material y ambos endpoints estén libres;
- las fronteras 32/256 y 128 deben quedar vinculadas a outcomes/metrics live, no sólo a kernels aislados.

## 3. Plan de implementación

### Checklist

- [ ] I1 Construir fixtures live mínimos para own-move y material-interval usando sólo hooks productivos; no añadir seam de test que replique el solver.
- [ ] I2 Verificar/ajustar la ruta de own-move para que un contacto previo tangencial final-válido sobreviva aunque `TemporalResponse` no produzca un hit nuevo.
- [ ] I3 Verificar/ajustar multicontacto live: hits simultáneos deben limitar el movimiento con todas sus normales y la selección de contacto persistente debe ser canónica y estable bajo permutación.
- [ ] I4 Verificar/ajustar sliding live con floor+wall y al menos una gravedad no vanilla: componente normal bloqueada, tangentes preservadas y block clip respetado.
- [ ] I5 Verificar/ajustar initial separation y final recovery live: salida segura acotada o suspensión exclusiva de la pareja irresoluble, sin teleport parcial.
- [ ] I6 Verificar/ajustar establecimiento/reacquisition por intervalo material publicado con body estacionario; sólo un hit/overlap temporal real puede crear contacto.
- [ ] I7 Verificar contacto material estrictamente intermedio con endpoints libres: debe influir en la respuesta física sin dejar un contacto final inventado si la pieza se retira.
- [ ] I8 Instrumentar sólo la dimensión de budget que resulte no observable durante I2-I7; reutilizar métricas existentes cuando ya distingan el agotamiento exigido.
- [ ] I9 Auditar hot paths de S09: ningún arreglo puede introducir `level.getAllEntities()` por query/move ni resample global de providers.
- [ ] I10 Revisión completa contra FR-043/044/047-053 y NFR-001/002/004/007/008; eliminar sólo helpers nuevos sin consumidor.
- [ ] I11 Ejecutar suite ordinaria + cualquier lane preparada necesaria y hacer pasada final completa sin cambios de producción.

### Revisiones del plan

- P1 requisitos: el scope cierra tareas G2 5 y la parte pendiente de 7/8 que depende de contacto/respuesta; no adelanta G3-G5.
- P2 ownership: `TemporalResponse` sigue siendo kernel, `AnatomyMovement.collide` own-move live y `MaterialPhysicsRuntime` interval-move live. No se crea otro motor.
- P3 consumidores: los únicos cambios productivos admisibles deben caer en esas rutas o en una frontera física reutilizable consumida por ambas.
- P4 fail-closed: budget/recovery ambiguo libera/suspende localmente; nunca aproxima con endpoint-only ni scan global.
- P5 determinismo: cualquier selección persistente debe derivar de ids/material provenance ordenados, no del orden de `HashMap`/registro.
- P6 simplicidad: no se modifica producción si el holdout demuestra que la implementación actual ya cumple.
- P7 verificabilidad: cada I2-I8 tiene observable físico o métrico independiente.

**Convergencia del plan:** una segunda pasada P1-P7 no produjo cambios. La investigación eliminó una propuesta inicial de reescribir el manifold: el kernel ya mantiene todas las constraints simultáneas; la deuda es integración live y sólo los holdouts decidirán si hace falta producción nueva.

## 4. Modelo adversarial previo

### A1 — tangente sin hit nuevo

Body ya apoyado sobre una pieza plana. Ejecuta un movimiento estrictamente paralelo a la cara y termina todavía dentro del footprint. Un solver que reconstruya contacto sólo desde hits del movimiento lo perderá/parpadeará.

### A2 — salida real del footprint

Mismo caso que A1, pero el desplazamiento tangencial termina fuera de la pieza. La retención previa no puede convertirse en sticky contact.

### A3 — floor + wall simultáneos

El body llega exactamente a suelo y pared en la misma primera fracción. Deben activarse ambas normales; cambiar el orden de registro/ids equivalentes no cambia displacement ni penetración final.

### A4 — esquina de tres contactos

Tres constraints simultáneas bloquean tres componentes compatibles. El resultado no puede depender de cuál hit se visitó primero ni descartar uno por usar un único `best`.

### A5 — sliding con gravedad lateral

Con gravedad WEST/EAST, una superficie de apoyo y una pared forman el equivalente rotado de floor+wall. Bloquear coordenadas world fijas en vez del frame físico correcto debe fallar.

### A6 — recovery resoluble

El body empieza con penetración pequeña en una pieza y existe una salida corta libre. Debe salir dentro de `MAX_SEPARATION`, respetar bloques y no suspender la pareja.

### A7 — wall squeeze irresoluble + bystander

Una pieza anima al body contra un muro/bloques sin salida dentro del budget. Sólo esa body/support relation se suspende; otro body/support seguro permanece soportado y no se mueve.

### A8 — reacquisition sin own move

El body está separado y sin contacto. Un intervalo material runtime real cruza y termina en una cara soportante válida. El body no llama a own-move para “ayudar”: el evento material debe establecer el nuevo contacto.

### A9 — contacto sólo intermedio

La pieza comienza y termina separada, pero atraviesa la trayectoria del body en `0<t<1`. Debe detectarse y afectar la respuesta continua; al final no puede quedar contacto retenido si la pieza vuelve a estar separada.

### A10 — endpoint cercano sin contacto temporal

La pieza termina a una distancia menor que tolerancias visuales pero nunca toca durante el intervalo. No se permite magnetización por endpoint.

### A11 — frontera de query budget

Caso construido para consumir exactamente el límite de evaluaciones y otro `+1`. El primero completa; el segundo produce `ITERATION_LIMIT`/exhaustion observable sin mutación parcial.

### A12 — frontera de separation budget

Recovery que encuentra salida en la última candidatura permitida frente a uno que necesitaría una más. El segundo falla cerrado y localmente.

### A13 — permutación y metamorfismo

Repetir A3/A7 con orden de registro invertido y tras una traslación común grande del fixture conserva resultado relativo, contactos y razones de fallo.

### Holdouts reservados

Después de implementar/revisar se concretarán sin avisar al código de antemano:

- una combinación de contacto tangencial retenido + nuevo segundo contacto;
- una pieza que toca sólo en un subintervalo muy corto y se retira;
- un squeeze irresoluble donde el support culpable comparte batch con otro support válido;
- una frontera exacta de budget que además contiene un bystander no causal.

## 5. Clasificación previa de fallos

- Un holdout live rojo con precondiciones físicas válidas es **bug de implementación** salvo prueba contraria.
- Si el fixture no alcanza la rama declarada o fabrica contacto/overlap accidental, es **test incorrecto** y se corrige sin tocar producción.
- Si el kernel puro falla una propiedad ya normativa, se vuelve a implementación del kernel; no se parchea en el wrapper.
- Si el fallo revela doble ownership/fallback, se vuelve a arquitectura/plan antes de código.
- Si CI falla antes de ejecutar el oracle físico, se clasifica como entorno/evidencia o compilación de fixture antes de inferir una regresión física.

## 6. Criterio de cierre

S09 sólo cierra cuando:

1. retención tangencial y release al abandonar footprint están demostrados live;
2. multicontacto simultáneo/sliding son deterministas y respetan todas las constraints relevantes;
3. recovery resoluble e irresoluble tienen outcomes acotados y pair-local;
4. un intervalo runtime puede establecer/reacquirir contacto sin own-move;
5. contacto sólo intermedio se detecta sin inventar contacto final;
6. fronteras de budget afectadas tienen outcome observable y no mutación parcial;
7. no se introduce scan mundial ni segundo solver;
8. suite final requerida está verde y la pasada completa posterior no produce cambios de producción.
