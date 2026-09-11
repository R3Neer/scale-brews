# S08 — Transporte anclado continuo y cadenas derivadas

Estado: **CERRADO TRAS REAPERTURA ADVERSARIAL TARDÍA**. Cuarto sprint de G2. G2 permanece abierto para multicontacto/sliding/recovery e instrumentación pendiente.

## 1. Tesis del sprint

S08 convierte el carry de un `SurfaceContact` retenido en transporte por la **trayectoria material certificada** de su punto local, no por la cuerda entre endpoints. Cada desplazamiento se comprueba continuamente frente al mundo y, cuando el body transportado es a su vez soporte material activo, su desplazamiento produce una contribución `DERIVED_CARRY` hija que el dispatcher procesa base→dependientes exactamente una vez, con ancestry, cycle/depth/event fences y sin deuda de transporte.

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

Requisitos de apoyo preservados: FR-043, FR-047, FR-048, FR-052, FR-053, FR-062 y FR-063.

NFR: NFR-001, NFR-002, NFR-004, NFR-007, NFR-008, NFR-015, NFR-017, NFR-025 y NFR-033.

### Excluido

- prediction/reconciliation y observer presentation de G4;
- semánticas específicas de boat/minecart/item/falling-block de G5;
- lifecycle/catalog de G3 salvo las barreras ya existentes;
- nuevos Geometry/Pose engines;
- compatibilidad Clinging/VanillaPlus;
- multicontacto/sliding/recovery general que no sea necesario para el carry retenido. Ese frente pasa a S09.

## 3. Arquitectura resultante

1. `BodyPath` modela el centro upright del body a partir de `SurfaceContact.localPoint` sobre una cara material y un `ConservativeSweep.Motion` certificado.
2. `AnchoredTransportPlanner` valida el carry por la trayectoria real, incluida obstrucción material, bloques, entidades vanilla y soportes anatómicos estacionarios que no formen parte del batch actual, con budgets explícitos y fallo cerrado.
3. `MaterialPhysicsRuntime` integra ese plan dentro de ROOT/JOINT/DERIVED, aplica el endpoint sólo después de certificar el intervalo y actualiza una única ruta de contacto/ancla/receipt.
4. `MaterialIntervalRuntime.deriveRoot(...)` captura el before→after de un soporte transportado y entrega el intervalo derivado al mismo dispatcher causal, sin reinyectarlo como ROOT/JOINT huérfano.
5. `MaterialEventDispatcher` conserva ancestry, orden base→dependientes, exactly-once y límites de ciclo/profundidad/eventos.
6. `AnatomyMovement.recordCertifiedTransport(...)` mantiene pasajeros vanilla, baseline de player y accounting pasivo.
7. El carry legacy queda como fallback para supports manuales/locales que no están bajo ownership material del runtime; no actúa como segundo motor sobre bindings poseídos por la cadencia material.
8. Un candidato activo incluido sólo por broadphase y con desplazamiento planificado cero **no** se trata como derivación inválida. Un candidato activo con desplazamiento no nulo y sin `transport` certificado sigue fallando cerrado como `INVALID_DERIVATION`.
9. Los obstáculos anatómicos estacionarios reutilizan el `MaterialBroadphase` S05 y los convexos actuales de `AnatomyMovement.candidates(...)`; no se introdujo un segundo índice ni un scan mundial. Los supports que ya tienen evento material en el batch se excluyen de esta ruta para evitar doble cómputo.

## 4. Plan de implementación cerrado

- [x] I1 Construir `BodyPath` sólo desde contacto/revision/piece/face certificados.
- [x] I2 Resolver transporte continuo acotado con trayectoria relativa + `ConservativeSweep` y fallo cerrado.
- [x] I3 Integrar el transporte anclado en `MaterialPhysicsRuntime` antes de aplicar un batch.
- [x] I4 Mantener atomicidad: ningún carry parcial si el plan necesario no puede certificarse.
- [x] I5 Actualizar contacto/ancla/receipt por una sola ruta y preservar pasajeros/baseline/accounting vanilla.
- [x] I6 Evitar carry endpoint paralelo bajo ownership material y conservar fallback para supports manuales/locales.
- [x] I7 Derivar before→after cuando el body transportado es soporte material activo.
- [x] I8 Procesar `DERIVED_CARRY` por el mismo dispatcher con ancestry/depth/cycle/event budgets.
- [x] I9 Excluir pasajeros vanilla de carry anatómico independiente para la misma contribución.
- [x] I10 Demostrar orden/permutación live con bystander activo no causal dentro del envelope conservador.
- [x] I11 Revisar consumidores/ownership/estado S08: planner, receipts y `deriveRoot` están conectados; no quedó helper de producción S08 sin consumidor identificado.
- [x] I12 Ejecutar suite ordinaria y lane preparada final y realizar una pasada final sin nuevos cambios de producción.
- [x] I13 Barrer continuamente entidades vanilla que sólo intersectan el interior de una trayectoria curva, sin reducir el chequeo a `collideBoundingBox` del endpoint.
- [x] I14 Conservar como obstáculo un soporte anatómico estacionario fuera del batch actual aunque su AABB vanilla esté suprimido por ownership anatómico.

### Convergencia y revisiones

- P1 requisitos/aceptación: no se amplió API pública ni G4.
- P2 ownership: kernel puro en `physics`, scheduling/ancestry en dispatcher, world mutation en runtime/integration.
- P3 datos: se reutilizan `SurfaceContact.localPoint`, `MotionIntervalHandle`, `DerivedCarry` y el índice material S05; no existe formato paralelo de ancla/trayectoria/obstáculo anatómico.
- P4 fail-closed: curva no certificable, overflow, broadphase material incompleto, cycle/depth/event limit o derivación móvil sin provenance no degradan a chord carry.
- P5 regresiones: G5 y G4 siguen fuera.
- P6 simplicidad: no se añadió scheduler ni broadphase alternativo.

La primera convergencia abrió después defectos reales encontrados por revisión adversarial: ownership legacy demasiado amplio, rechazo de bystanders activos estacionarios y, tras un cierre documental prematuro, dos huecos adicionales de FR-057 en los obstáculos de la trayectoria. Todos se repararon sin cambiar requisitos.

## 5. Campaña adversarial

### A1 — cuerda segura, arco bloqueado

Una cara rota alrededor de un eje; el chord no toca el obstáculo pero la trayectoria del anchor sí. Verde tras usar el recorrido material real.

### A2 — endpoints casi coincidentes / recorrido no nulo

Delta neto ≈ 0 no autoriza a ignorar recorrido intermedio ni una obstrucción. Cubierto por los holdouts de trayectoria.

### A3 — frontera de soporte

Normal de cara en torno a `cos(45°)`: sólo el certificado físicamente soportante puede transportar. Verde.

### A4 — obstrucción y deuda

Carry bloqueado libera/suspende localmente, no aplica teleport parcial y no reaplica el delta rechazado al desaparecer el obstáculo. Verde.

### A5 — varias contribuciones legítimas en el mismo tick

ROOT/JOINT/DERIVED conservan serial causal y exactly-once. Verde.

### A6 — cadena A→B→C

A mueve B; B soporta C. B se aplica una vez y genera un intervalo derivado que después mueve C. Verde en scheduler sintético y en runtime preparado con geometría exportada real.

### A7/A8 — ciclo, depth y event budget

Ciclos y cadenas por encima del límite se cortan localmente y de forma observable. Verde.

### A9 — passenger root

La raíz mueve passenger vanilla una vez; el passenger no entra además como carry anatómico independiente. Verde.

### A10 — permutación + bystander activo no causal

El holdout `S08PreparedPermutationProof` ejecuta dos escenarios: bystander-first y chain-first. El bystander es un binding activo dentro del envelope material conservador de A, pero empieza fuera del material, fuera del envelope de construcción de B→C y A se mueve alejándose de él. Debe permanecer estacionario mientras B y C reciben exactamente `(+0.2, 0, 0)` y se conservan sus contactos causales.

**Rojo válido:** prepared run `34647526137`, snapshot `8d28f3f8d26ec9abcceb921295e29f5090387294`, alcanzó el holdout con todas las precondiciones y falló únicamente al mover A: `B actual=(0,0,0)` frente a `expected=(0.2,0,0)`. La causa era que `MaterialPhysicsRuntime` clasificaba como `INVALID_DERIVATION` a cualquier candidato `LivingEntity` activo sin `plan.transport()`, incluso con desplazamiento cero.

**Reparación:** `0a6914ba355021b0e0e8c29a32eee0a32cb0adbc` exige desplazamiento no nulo para esa derivación inválida. Un bystander activo estacionario no fabrica una contribución ROOT; un soporte activo que sí vaya a moverse sin provenance certificado continúa siendo rechazado.

**Verde inicial:** prepared run `34647905593` sobre `0a6914ba...` pasó exportación cliente original y el servidor preparado completo, incluido A10 y las dos permutaciones. Este verde no fue cierre definitivo: holdouts posteriores reabrieron FR-057.

### A11 — stale/rebind durante derivación

`S08DerivedRebindTests` rechaza capture viejo, exige cero deuda y recuperación desde la identidad nueva. Verde.

### A12 — obstacle/candidate overflow

Frontera estática `256 -> COMPLETE/evaluations=256`, `257 -> EXHAUSTED/evaluations=256`, sin mover el body. Sigue verde tras integrar entidades vanilla y candidatos anatómicos en el mismo presupuesto de obstáculos.

### A13 — ownership de fallback bajo sesión preparada

Una sesión preparada no puede silenciar el fallback de un support registrado manualmente sólo por compartir nivel/servidor.

**Rojo:** run `34645201815` falló en `anatomicalRootTransportOncePrepared` porque el fence confundía sesión activa con ownership del soporte.

**Reparación:** `fc4ef518bbb8b357d8f3ee062d15caf4493e4f7c` añadió ownership estrecho por binding activo para `LivingEntity`, manteniendo la semántica pública de sesión.

**Verde:** prepared run `34645613492` pasó la regresión manual y la cadena preparada A→B→C.

**Oráculo retirado:** una prueba posterior exigía fallback inmediato tras `setPos` directo de un binding activo antes de publicar su cadencia material. Se retiró porque podía exigir doble consumo y contradecir FR-050/051; no forma parte de la evidencia de cierre.

### A14 — entidad vanilla sólida sólo en mitad del arco

Tras el primer cierre documental, `c8445e82092f7f249af829d63d7d65b022dc6ecd` añadió `solidEntityInMiddleOfArcMustNotBeReducedToEndpointChord`. El cuerpo empezaba y terminaba libre, pero una entidad vanilla sólida ocupaba únicamente el interior de la trayectoria curva.

**Rojo:** el planner sólo comprobaba entidades mediante el clip del desplazamiento endpoint y podía devolver `COMPLETE` aunque el arco atravesase la entidad.

**Reparación:** `c562025985dbeffd378171ada06bb55776b1eee1` consulta las collision shapes vanilla dentro del envelope bajo `PlatformPhysics.enter(body)` y las barre continuamente con `BodyPath.relative(...) + ConservativeSweep`. Los AABB world se convierten a convexos directamente en double para no introducir redondeo `Matrix4f` en coordenadas grandes.

**Verde intermedio:** snapshot `7c44a104bbf7a6471ec4b961bf138b4ac3a88e76`, ordinary run `34650752411` y prepared run `34650752345` verdes. La campaña siguió abierta porque apareció A15.

### A15 — soporte anatómico estacionario fuera del batch

`S08PreparedStaticAnatomicalObstacleProof` coloca un binding anatómico B estacionario atravesando sólo el interior del carry de A. B no tiene evento material en el batch; precisamente por ser una replacement pair, su AABB vanilla queda suprimido y no puede depender de `getEntityCollisions`.

Una primera versión del fixture falló antes del planner por el handshake del mock player preparado; ese fallo se clasificó como test incorrecto y **no** cuenta como rojo de producción. `d819a6e09b45ca9ccc7490e9d37333ac8777525e` sustituyó el body por un living fixture normal sin cambiar el oráculo físico.

**Rojo válido:** prepared run `34651404651`, job `103434171335`, snapshot `d819a6e09b45ca9ccc7490e9d37333ac8777525e`. Todas las precondiciones pasaron y el fallo final fue exactamente: `FR-057 requires a stationary anatomical support outside A's batch to obstruct the certified carry path: COMPLETE`.

**Reparación:** `d0fbcf61ca10c10ea32a329271f607d2b77629d4` expone package-private la consulta de candidatos materiales ya existente en `AnatomyMovement`; `4abe57cd30441761e667f8a56fa233f2a9d709e8` hace que `AnchoredTransportPlanner` consulte esa vista dentro del envelope, barra sus convexos actuales como obstáculos estáticos y excluya los supports ya representados por eventos. No se añade índice alternativo ni scan mundial.

**Verde final:** ordinary run `34651958772` y prepared run `34651958909` sobre `4abe57cd...`; la lane preparada exportó cow/player/familias originales y el servidor pasó **2/2 required GameTests**.

## 6. Matriz final

| Ataque/propiedad | Requisito | Nivel | Estado |
| --- | --- | --- | --- |
| chord libre / arco bloqueado | FR-056..058 | GameTest + kernel | verde |
| entidad vanilla sólo en mitad del arco | FR-057 | GameTest | verde |
| soporte anatómico estacionario fuera del batch | FR-057, NFR-008 | prepared/live | verde |
| gravedad body independiente | FR-032/056 | GameTest | verde |
| blocked carry sin deuda | FR-057/058 | GameTest | verde |
| derived root exactly-once | FR-050/051 | GameTest | verde |
| A→B→C scheduler | FR-059 | GameTest sintético | verde |
| A→B→C runtime preparado | FR-050/059 | lane preparada | verde |
| ciclo/depth/event budget | FR-059, NFR-007 | GameTest/kernel | verde |
| passenger candidate once | FR-060 | GameTest | verde |
| passive velocity/fall/stats/exhaustion | FR-061 | GameTest | verde |
| permutación + bystander | NFR-001/002/004 | preparado/live | verde |
| stale identity / rebind | NFR-017 | GameTest | verde |
| overflow 256/257 | NFR-004/007/008 | GameTest | verde |
| fallback manual en sesión preparada | FR-050/I6 | lane preparada | verde |

## 7. Evidencia de cierre

La evidencia registrada anteriormente sobre `0a6914ba...` fue un **cierre provisional incorrectamente tratado como definitivo**. Los holdouts A14 y A15 posteriores lo superseden. Se conserva aquí para mantener la historia red-before-green, pero el snapshot de cierre vigente es `4abe57cd30441761e667f8a56fa233f2a9d709e8`.

Red-before-green relevante:

- `34645201815`: lane preparada roja por ownership legacy demasiado amplio; reparado por `fc4ef518...`.
- `34647526137`: lane preparada roja válida de A10; reparado por `0a6914ba...`.
- holdout `c8445e82...`: entidad vanilla en mitad de arco reveló que el clip endpoint no certificaba la trayectoria completa; reparado por `c5620259...`.
- `34651404651`, job `103434171335`, snapshot `d819a6e0...`: rojo válido de A15, planner `COMPLETE` ante soporte anatómico estacionario intermedio; reparado por `d0fbcf61...` + `4abe57cd...`.

Evidencia final sobre **el mismo snapshot `4abe57cd30441761e667f8a56fa233f2a9d709e8`**:

- ordinary build run **`34651958772`**: verde; artefacto **`10284575580`**, SHA-256 **`9439f0806924e8a35c85c6fa6f3d4b7d46152f10baeb37422bde779ff9a14818`**;
- prepared adversarial run **`34651958909`**, job **`103435928454`**: exportación original verde (`cow 240/10`, player wide/slim `144/6`, 80 comparaciones por primario y 640 familiares adicionales) y servidor preparado **2/2 required GameTests**, incluido el soporte anatómico estacionario.

Pasada final posterior al último verde:

- `AnatomyRuntime.owns(LivingEntity)` sigue teniendo consumidor real en el fence de `AnatomyMovement.carry`;
- `MaterialPhysicsRuntime` consume `AnchoredTransportPlanner`; `apply()` consume `recordCertifiedTransport(...)`; supports activos desplazados producen `deriveRoot(...)`/`derivedInterval(...)`;
- obstáculos de evento, anatómicos estacionarios, bloques y entidades vanilla se certifican dentro de la misma ruta continua y budgets explícitos;
- la consulta de obstáculos anatómicos reutiliza `MaterialBroadphase`/`currentSnapshot`, sin `getAllEntities()` nuevo ni segundo índice;
- supports ya representados por eventos se excluyen de la ruta anatómica estática, evitando doble cómputo;
- no se identificó helper/estado S08 sin consumidor ni otra modificación de producción necesaria.

La pasada completa final no produjo cambios de producción.

## 8. Cierre

S08 cumple su criterio de cierre vigente:

1. no sustituye trayectoria certificada disponible por delta endpoint;
2. arco bloqueado se detecta frente a bloques, entidades vanilla y obstáculos anatómicos fuera del batch;
3. obstrucción no genera teleport parcial ni deuda;
4. cadenas conservan ancestry, orden y exactly-once;
5. cycle/depth/event budget fallan localmente;
6. passenger transport ocurre una vez;
7. ownership material y fallback legacy están separados;
8. permutación/bystander live está demostrada;
9. ordinary + prepared final están verdes en `4abe57cd...`;
10. la pasada completa posterior al último verde no exigió otro cambio de producción.

**S08 queda cerrado de nuevo tras la reapertura tardía. G2 no queda cerrado:** multicontacto/sliding/recovery live y la instrumentación restante continúan en S09.
