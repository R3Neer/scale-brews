# S09 — Multicontacto, sliding y recovery material

Estado: **IMPLEMENTACIÓN COMPLETA / PASADA FINAL EN CURSO**. Quinto sprint de G2.

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
6. `MaterialPhysicsRuntime.plan(...)` ejecuta `TemporalResponse` con movimiento propio cero contra motions materiales; por ello puede detectar contacto generado exclusivamente por el intervalo del soporte.
7. `MaterialPhysicsRuntime.apply(...)` sólo intenta `establish(...)` con pieces presentes en `contactPieces`, que proceden de hits CCD o overlap inicial real; no usa proximidad arbitraria.
8. `MaterialPhysicsRuntime.Metrics` expone admitted/candidates/evaluations/quarantined/exhausted; `AnatomyMovement.SweepMetrics` expone queries/pieces/evaluations/exhausted.

### Estado adversarial final

- **A1/A2 verdes:** la retención tangencial sin hit nuevo sobrevive mientras el endpoint siga materialmente válido y se libera al abandonar el footprint.
- **A3/A4 verdes:** floor+wall y esquina de tres contactos activan las constraints simultáneas de forma estable; inversión de orden, signed zero y contactos dentro de `TIME_EPS` conservan el conjunto simultáneo.
- **A5 verde:** el sliding equivalente con gravedad lateral bloquea las normales físicas correctas y conserva el tangente permitido.
- **A6/A7 verdes:** recovery resoluble queda dentro del límite; los casos irresolubles fallan cerrados y localizan suspensión/conflicto sin destruir relaciones sanas no causales.
- **A8 verde:** `S09PreparedIntervalContactProof` demuestra en la lane preparada real que un body estacionario puede adquirir contacto por un ROOT publicado sin own-move auxiliar.
- **A9 verde:** `S09PreparedIntermediateContactProof` demuestra con catálogo real de cow que una pieza puede tocar sólo en `0<t<1`, desplazar al body, terminar con ambos endpoints libres, no inventar contacto final y dejar `admitted=1`, `quarantined=0`, `exhausted=0` bajo el budget existente.
- **A10 verde:** endpoint cercano sin contacto temporal no magnetiza relación.
- **A11 verde:** exhaustion conserva el prefijo seguro sólo como diagnóstico del kernel; la frontera live devuelve cero desplazamiento y cero contactos/deuda lógica, manteniendo exhaustion observable en métricas.
- **A12 verde:** la frontera exacta de `AnatomySeparation` quedó estabilizada canonicalizando/deduplicando candidatos antes de encolarlos; candidate 128 y `+1` se distinguen sin crecimiento artificial de duplicados.
- **Holdout de screening verde:** un motion CLEAR con cota deformante floja que consume cientos de evaluaciones en CCD directo completa bajo 256 mediante screening certificado; los CLEAR baratos usan primero un probe CCD acotado.
- **Holdout de tolerancia simultánea verde:** dos contactos distintos separados por menos de `TIME_EPS` siguen recogidos ambos.
- **Cota jerárquica fijada:** los nodos rígidos ya no multiplican artificialmente la velocidad por usar norma de Frobenius; `HierarchyMotion` usa una cota espectral conservadora para el bloque lineal y conserva soporte para afines/shear.

Rojos retirados/no probatorios durante la revisión:

- un typo de fixture `before.jointSampleTick()` fue fallo de compilación, no evidencia física;
- una primera versión A9 con cow scale 4 superaba deliberadamente el cap runtime de envelope 64 y fue cuarentenada antes de broadphase;
- un squeeze batch inicial no ejercía correctamente la localidad pretendida y se retiró en vez de parchear producción;
- varias calibraciones A12 asumían una cardinalidad incorrecta de candidatos o dejaban entrar al bystander en la corrección; se corrigieron los fixtures hasta aislar el bug real de deduplicación;
- un primer screening por ventanas con cap local rompía contactos reales y se descartó;
- un recorte del horizonte al primer contacto alteraba empates dentro de tolerancia y se descartó;
- el diagnóstico temporal detallado de A9 (`segments`, SAT uniforme y lower bounds) se eliminó una vez resuelto el bloqueo.

## 3. Plan de implementación

### Checklist

- [x] I1 Construir fixtures live mínimos para own-move y material-interval usando sólo hooks productivos; no añadir seam de test que replique el solver.
- [x] I2 Verificar/ajustar la ruta de own-move para que un contacto previo tangencial final-válido sobreviva aunque `TemporalResponse` no produzca un hit nuevo.
- [x] I3 Verificar multicontacto live inicial: floor+wall simultáneos limitan ambas normales y la respuesta no depende del orden de registro.
- [x] I4 Verificar/ajustar sliding live con floor+wall y al menos una gravedad no vanilla: componente normal bloqueada, tangentes preservadas y block clip respetado.
- [x] I5 Verificar/ajustar initial separation y final recovery live: salida segura acotada o suspensión exclusiva de la pareja irresoluble, sin teleport parcial.
- [x] I6 Verificar establecimiento/reacquisition por intervalo material publicado con body estacionario; sólo un hit/overlap temporal real puede crear contacto.
- [x] I7 Reparar/verificar A9: contacto material estrictamente intermedio con endpoints libres influye en la respuesta física sin dejar contacto final inventado.
- [x] I8 Reparar/verificar A11 y las demás fronteras afectadas: exhaustion observable, sin displacement parcial ni deuda lógica; budgets normativos permanecen en 32/256 y 128.
- [x] I9 Auditar hot paths de S09: no hay `level.getAllEntities()` por query/move ni resample global de providers; la captura live sigue acotada por envelope y `maximumBodies`.
- [x] I10 Revisión completa contra FR-043/044/047-053 y NFR-001/002/004/007/008; se eliminaron los diagnósticos temporales de A9 y no se creó segundo solver/owner.
- [ ] I11 Ejecutar suite ordinaria + lane preparada sobre la versión limpia y hacer pasada final completa sin cambios de producción.

### Reparaciones aplicadas

**A9 / budget multipieza**

- No se aumentó `QUERY_BUDGET`; permanece en 256.
- `HierarchyMotion` sustituyó la norma de Frobenius usada como factor lineal por una cota superior conservadora de la norma espectral basada en `AᵀA`, evitando inflar identidades/rotaciones rígidas por `√3` a cada nivel jerárquico.
- `TemporalResponse` mantiene el orden canónico de piece ids.
- Para piezas deformantes inactivas, un probe CCD de 8 evaluaciones resuelve inmediatamente los casos baratos; `ITERATION_LIMIT` del probe significa sólo “no barato”, no failure global.
- Los casos caros pasan por broadphase temporal y cobertura certificada de intervalos libres mediante planos SAT y cotas de velocidad; cada muestra consume el mismo budget global.
- Si el screening alcanza una zona que no puede certificar como libre, el CCD exacto recibe el budget restante; los contactos activos ya no vuelven a pagar screening.
- La pieza causal aislada y el manifold real completo del cow preparado completan bajo 256; la ruta runtime física queda verde sin contacto final inventado.

**A11 / exhaustion live**

- El overload puro de `TemporalResponse` puede conservar un prefijo seguro para diagnóstico.
- El overload live con `clip` convierte `ITERATION_LIMIT` en displacement cero y contacts vacíos antes de recovery/selección persistente.
- Exhaustion sigue contabilizado en métricas y el holdout calibrado no deja contacto ni suspensión derivados de un movimiento que no se aplica.

**A12 / recovery acotado**

- `AnatomySeparation` canonicaliza candidatos antes de encolarlos para que offsets numéricamente equivalentes no consuman el budget como estados independientes.
- El límite de 128 candidatos permanece intacto; el caso `+1` falla cerrado y localmente.

### Revisiones del plan

- P1 requisitos: el scope cierra tareas G2 5 y la parte pendiente de 7/8 que depende de contacto/respuesta; no adelanta G3-G5.
- P2 ownership: `TemporalResponse` sigue siendo kernel, `AnatomyMovement.collide` own-move live y `MaterialPhysicsRuntime` interval-move live. No se crea otro motor.
- P3 consumidores: las reparaciones quedaron en fronteras físicas reutilizadas por las rutas live (`TemporalResponse`, `HierarchyMotion`, `AnatomySeparation`) y en la política live de exhaustion.
- P4 fail-closed: budget/recovery ambiguo libera/suspende localmente; nunca aproxima con endpoint-only ni scan global.
- P5 determinismo: orden canónico, contactos simultáneos dentro de `TIME_EPS` y signed zero quedan fijados por tests; se descartó el recorte de horizonte que alteraba empates.
- P6 simplicidad: se retiraron intentos que sólo desplazaban el problema (cap local, screening repetido de constraints activas, horizonte truncado) en vez de conservarlos como deuda.
- P7 verificabilidad: I2-I8 tienen observables físicos o métricos independientes y A9 se prueba además contra catálogo preparado real.

**Convergencia del plan:** el ownership inicial permanece intacto. La revisión convergió en corregir la calidad de las cotas jerárquicas, hacer eficiente el consumo bounded del kernel y endurecer la frontera live de exhaustion, sin aumentar budgets ni añadir un segundo solver.

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

### Holdouts concretados durante la revisión

- contacto tangencial retenido + nuevo segundo contacto;
- pieza que toca sólo en subintervalo y se retira, incluida la prueba preparada A9;
- endpoint casi tocando pero sin contacto temporal;
- frontera exacta de own-move y de separation con bystander no causal;
- sliding con gravedad lateral;
- tres contactos simultáneos;
- screening con cota deformante floja pero trayectoria CLEAR;
- dos contactos distintos dentro de `TIME_EPS`;
- cota rígida jerárquica para no multiplicar yaw speed por dimensión.

## 5. Clasificación de fallos aplicada

- Un holdout live rojo con precondiciones físicas válidas se trató como **bug de implementación** salvo prueba contraria.
- Fixtures que no alcanzaban la rama declarada, excedían deliberadamente caps o fabricaban causalidad accidental se corrigieron/retiraron sin tocar producción.
- Fallos de kernel normativos se repararon en el kernel reutilizable, no en wrappers ad hoc.
- No apareció doble ownership ni fue necesario cambiar arquitectura.
- Fallos previos al oracle físico se separaron de regresiones físicas antes de modificar código.

## 6. Criterio de cierre

S09 sólo cierra cuando:

1. retención tangencial y release al abandonar footprint están demostrados live;
2. multicontacto simultáneo/sliding son deterministas y respetan todas las constraints relevantes;
3. recovery resoluble e irresoluble tienen outcomes acotados y pair-local;
4. un intervalo runtime puede establecer/reacquirir contacto sin own-move;
5. contacto sólo intermedio se detecta sin inventar contacto final;
6. fronteras de budget afectadas tienen outcome observable y no mutación parcial ni deuda lógica;
7. no se introduce scan mundial ni segundo solver;
8. suite final requerida está verde y la pasada completa posterior no produce cambios de producción.

Los puntos 1-7 están demostrados. El punto 8 queda pendiente únicamente de la ejecución final sobre la versión ya limpiada de diagnósticos; no queda ningún blocker funcional conocido.