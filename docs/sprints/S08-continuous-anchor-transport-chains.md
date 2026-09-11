# S08 — Transporte anclado continuo y cadenas derivadas

Estado: **PLAN CONVERGIDO / IMPLEMENTACIÓN PENDIENTE**. Cuarto sprint de G2.

## 1. Tesis del sprint

Al terminar S08, un body que conserve un `SurfaceContact` podrá ser transportado por la **trayectoria material certificada** del punto local de su cara, no por la cuerda entre dos endpoints; cada desplazamiento de carry se comprobará continuamente frente al mundo y, cuando el body transportado sea a su vez soporte material, la contribución derivada conservará ancestry y se procesará base→dependientes exactamente una vez, sin ciclos ni deuda de transporte.

Esto debe demostrarse sin asumir que el endpoint describe la trayectoria, que un tick contiene una sola contribución material o que las cadenas tienen profundidad uno.

Gate: **G2 — pipeline material continuo Q2**.

## 2. Scope normativo

### Incluido

Requisitos principales:

- FR-049: trayectoria material continua certificada;
- FR-050: cada movimiento material se consume exactamente una vez;
- FR-051: varias contribuciones del mismo tick se conservan separadas;
- FR-055: provenance del contacto/ancla;
- FR-056: ancla ligada a punto local material;
- FR-057: carry por trayectoria certificada y collision-check continuo;
- FR-058: obstrucción libera/suspende localmente sin teleport, deuda ni reaplicación;
- FR-059: cadenas base→dependientes con ancestry y ciclos acotados;
- FR-060: raíz con pasajeros vanilla se desplaza una vez; pasajeros no reciben carry anatómico independiente;
- FR-061: transporte pasivo no fabrica movimiento voluntario/fall-distance.

Requisitos de apoyo que deben permanecer verdes: FR-043, FR-047, FR-048, FR-052, FR-053, FR-062 y FR-063.

NFR: NFR-001, NFR-002, NFR-004, NFR-007, NFR-008, NFR-015, NFR-017, NFR-025 y NFR-033.

### Excluido

- prediction/reconciliation y observer presentation de G4;
- semánticas específicas de boat/minecart/item/falling-block de G5;
- lifecycle/catalog de G3 salvo las barreras ya existentes que el sprint no puede romper;
- nuevos Geometry/Pose engines;
- compatibilidad Clinging/VanillaPlus;
- multicontacto/sliding general no necesario para transportar un contacto retenido válido. Ese frente sigue abierto en G2.

## 3. Investigación / estado actual

### Estado actual

1. `MaterialEventDispatcher` ya soporta `DERIVED_CARRY`, ancestry por identidad, cycle fence, depth limit y event/queue budgets, pero el backend live no devuelve `DerivedCarry`.
2. `BodyPath` ya modela el centro upright del body a partir de un `LocalAnchor` sobre una cara material y un `ConservativeSweep.Motion` certificado. También produce la trayectoria relativa necesaria para comprobar obstáculos continuos.
3. El kernel ya tiene un holdout donde el endpoint chord está despejado pero un arco anclado choca a mitad del intervalo. Ese caso pasa a nivel kernel.
4. `SurfaceContact.localPoint` ya es coordenada local normalizada de la pieza y está validada sobre su cara declarada; no hace falta un segundo formato de anchor.
5. `MaterialPhysicsRuntime` consume ROOT/JOINT de S06 con CCD continuo, pero el carry retenido continúa fuera del dispatcher.
6. `AnatomyMovement.carry(...)` sigue siendo un motor paralelo de transporte: resuelve recursivamente el soporte actual, calcula `piece.point(anchor.local) - anchor.previous`, hace un único `Entity.collideBoundingBox` sobre esa cuerda y actualiza receipts/anchors por endpoint.
7. Esa ruta puede perder un obstáculo atravesado por una trayectoria curva aunque inicio y final sean seguros. También expresa cadenas mediante recursión de estado actual, no mediante ancestry causal del evento material.
8. `positionPassengers(...)` ya mantiene el comportamiento vanilla de pasajeros cuando se desplaza una raíz; debe conservarse sin ejecutar carry anatómico independiente sobre cada pasajero.

### Estado objetivo

- El evento material que mueve una cara retenida es la autoridad del carry.
- El body sigue el `BodyPath` certificado de su `SurfaceContact.localPoint`.
- Cualquier obstáculo estático/material relevante se evalúa en el frame relativo de ese path con budgets explícitos.
- Sólo se aplica el endpoint del path si todo el intervalo está certificado libre; una obstrucción no aplica prefijo especulativo ni genera deuda.
- El carry aplicado a una entidad que también posee geometría material produce una contribución `DERIVED_CARRY` hija del evento causante.
- Dispatcher, no recursión oportunista del tick, ordena base→dependientes y corta ciclos/profundidad/event budget.
- El antiguo `AnatomyMovement.carry` deja de ser un segundo motor de carry para intervalos que ya pasaron por el dispatcher.

### Deuda que permanece fuera

- selección multicontacto y sliding general;
- transporte/prediction bajo RTT;
- retiro total del motor Living Platforms;
- partición completa de `AnatomyMovement`.

## 4. Plan de implementación convergido

- [ ] I1 Añadir un adapter package-private que construya un `BodyPath` sólo cuando el contacto retenido coincide con support/revision/piece/face del `before` certificado y la cara tiene un certificado de normal válido durante todo el intervalo.
- [ ] I2 Añadir un resolvedor de transporte continuo acotado que use `BodyPath.relative(...)` + `ConservativeSweep` para detectar obstrucción a lo largo del intervalo y que falle cerrado ante geometría/budget no certificable.
- [ ] I3 Integrar ese transporte en la planificación de `MaterialPhysicsRuntime` antes de aplicar un batch: el body transportado recibe el desplazamiento exacto `path.displacement(1)`, no el chord endpoint calculado por `AnatomyMovement.carry`.
- [ ] I4 Mantener atomicidad del batch: ninguna ruta de carry aplica desplazamiento si otra parte necesaria del mismo plan queda sin certificar.
- [ ] I5 Tras aplicar un carry, actualizar contacto/ancla/receipt mediante una única ruta y preservar baseline vanilla de server-player y passenger positioning sin contabilizar movimiento voluntario.
- [ ] I6 Suprimir el carry endpoint paralelo para intervalos ya consumidos por el dispatcher; conservar fallback legacy sólo donde todavía no existe intervalo material certificado y documentar exactamente esa frontera temporal.
- [ ] I7 Si el body movido es soporte material activo, certificar su before→after como intervalo derivado y devolver `DerivedCarry` con el `EventId` padre; no reinyectarlo como ROOT/JOINT huérfano.
- [ ] I8 Hacer que `DERIVED_CARRY` use el mismo capture/resolve live y que ancestry/depth/cycle/event budgets del dispatcher gobiernen cadenas A→B→C.
- [ ] I9 Garantizar que pasajeros vanilla de una raíz transportada no entren como candidatos de carry anatómico independiente para la misma contribución.
- [ ] I10 Añadir GameTests de arco, obstrucción, no-debt, cadena, ciclo, orden/permutación, same-tick y passenger-once; registrar métricas/evidencia.
- [ ] I11 Revisar el diff completo contra FR-056..061 y eliminar helpers o estado endpoint que hayan quedado sin consumidor real.
- [ ] I12 Ejecutar suite ordinaria y lane preparada que resulte necesaria; después realizar una pasada completa sin cambios de producción antes de cierre.

### Revisiones del plan

- P1 requisitos/aceptación: no requiere ampliar API pública ni G4; mantener el trabajo en `physics/internal` es compatible con NFR-025.
- P2 ownership: `BodyPath` sigue siendo kernel puro; scheduling/ancestry permanece en dispatcher; live world mutation en runtime/integration.
- P3 código real: reutiliza `SurfaceContact.localPoint`, `MotionIntervalHandle` y `DerivedCarry`; no introduce un segundo formato de trayectoria/ancla.
- P4 fail-closed: una curva no certificable, obstacle overflow, cycle/depth/event overflow o derivación inválida no degrada a chord carry.
- P5 regresiones: no sustituye todavía semánticas especiales de G5 ni prediction G4.
- P6 simplicidad/verificabilidad: cada paso tiene observable físico o causal; no se añade scheduler alternativo.

**Convergencia de plan:** una segunda pasada completa P1-P6 no produjo cambios.

## 5. Modelo adversarial previo

### A1 — cuerda segura, arco bloqueado

Una cara rota alrededor de un eje. El anchor termina en un endpoint cuyo chord no toca un bloque, pero la trayectoria real sí. La implementación cómoda `delta=end-start` debe fallar el test.

### A2 — endpoints coincidentes / recorrido no nulo

El punto material vuelve muy cerca del origen tras una rotación. Delta neto ≈ 0 no autoriza a ignorar el intervalo recorrido ni una obstrucción intermedia.

### A3 — justo en la frontera de soporte

La normal de la cara queda justo en `cos(45°)`, apenas por encima y apenas por debajo. Sólo los certificados físicamente soportantes pueden transportar.

### A4 — obstrucción y deuda

Un carry bloqueado debe liberar/suspender la relación local sin aplicar teleport parcial. Al retirar el obstáculo en el tick siguiente no puede reaplicar el delta rechazado del tick anterior.

### A5 — dos contribuciones legítimas en el mismo tick

ROOT y JOINT, o dos intervalos materiales serializados, mueven el mismo anchor. Cada contribución se consume una vez y en orden causal; agregarlas por tick o deduplicarlas por posición es incorrecto.

### A6 — cadena A→B→C

A mueve B y B soporta C. B se mueve una vez por A; el intervalo derivado de B mueve C después. C no puede observar el endpoint nuevo de B antes de la contribución que lo causa.

### A7 — ciclo A↔B

Dos entidades se retienen mutuamente. La cadena se corta por ancestry/cycle fence dentro de budgets; no recursión infinita, stack overflow ni invalidación mundial.

### A8 — depth/event boundary

Cadena exactamente en el máximo permitido y una unidad por encima. La primera permanece determinista; la segunda falla localmente con razón observable.

### A9 — passenger root

Una raíz transportada lleva passenger vanilla. El passenger sigue el movimiento vanilla de la raíz una vez y no recibe además un carry anatómico por ser candidato espacial.

### A10 — permutación

Cambiar registration/candidate order no cambia posiciones finales, relaciones retenidas ni ancestry aceptada.

### A11 — stale/rebind durante derivación

Si el support derivado cambia generation/revision antes de certificarse, no se fabrica un `DERIVED_CARRY` sobre una identidad vieja.

### A12 — obstacle/candidate overflow

Demasiados obstáculos o envelope no acotable deben producir fail-closed local. Nunca se cambia a scan mundial, muestreo endpoint-only ni aplicación parcial.

### Holdouts reservados

El implementador conoce las propiedades, no los fixtures exactos, de:

- recorrido de retorno con endpoint casi idéntico;
- permutación de una cadena con un bystander no causal dentro del mismo envelope;
- obstrucción que afecta a un dependiente pero no a la base;
- cycle/depth en combinación con una segunda contribución same-tick.

## 6. Matriz de pruebas prevista

| Ataque/propiedad | Requisito | Nivel |
| --- | --- | --- |
| chord libre / arco bloqueado | FR-056..058 | GameTest + kernel diferencial |
| normal 45° | FR-045/056 | kernel/GameTest |
| blocked carry sin deuda | FR-057/058 | GameTest |
| same-tick exactamente una vez | FR-050/051 | GameTest |
| A→B→C ancestry | FR-059 | GameTest |
| ciclo/depth/event budget | FR-059, NFR-007 | GameTest/kernel |
| passenger once | FR-060/061 | GameTest |
| permutación/bystander | NFR-001/002/004 | GameTest |
| stale identity | NFR-017 | GameTest |
| overflow local | NFR-004/007/008 | GameTest |

## 7. Criterio de cierre

S08 sólo puede cerrarse si:

1. no queda un camino productivo que sustituya una trayectoria certificada disponible por el delta entre endpoints;
2. el arco bloqueado se detecta en runtime live;
3. obstrucción no genera teleport parcial ni deuda/reapply;
4. una cadena material derivada conserva ancestry, orden y exactly-once;
5. ciclos/profundidad/event budget fallan localmente;
6. passenger transport ocurre una vez;
7. suites afectadas quedan verdes y la segunda pasada adversarial no fuerza cambios de requisitos;
8. una pasada completa posterior no produce cambios de producción.
