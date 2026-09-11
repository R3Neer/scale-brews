# Plan del sistema de colisiones entre entidades

Estado: plan canónico de implementación de `chatgpt-editing`. **No define requisitos**: cada tarea referencia [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md). La arquitectura está en [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md) y la evidencia ejecutada en [VALIDATION](VALIDATION.md).

## 1. Criterio de planificación

El trabajo elimina primero ambigüedad y doble ownership, cierra después la física causal, amplía engines/cobertura y sólo entonces migra consumidores y targets de compatibilidad.

Un gate no se cierra porque exista una clase o un test. Se cierra cuando la evidencia requerida por sus FR/NFR está registrada para el commit exacto.

El estado de tareas vive **sólo aquí**. La arquitectura no mantiene un segundo snapshot de implementación y VALIDATION no mantiene una segunda lista de trabajo.

## 2. Resultado de la reestructuración inicial

La auditoría iterativa del árbol legacy + anatómico produjo estas decisiones estables:

- `AnatomyMovement`, ahora dentro de `collision.internal`, contiene una base Q1 avanzada, pero sigue mezclando registro de providers, histories, broadphase, solver, contacto, root tracking y carry. Dividirlo antes de cerrar las fronteras causales de Q2 sólo trasladaría el mismo acoplamiento entre paquetes; su partición real pertenece a G2.
- `MaterialEventDispatcher`, `BodyPath`, `ConservativeSweep`, `TemporalResponse`, `HierarchyMotion`, receipts, histories y trackers **se conservan** porque representan capacidades requeridas por G2-G4, aunque algunas no estén todavía integradas en el pipeline vivo.
- `RootEventDispatcher` se eliminó: era scaffolding de frontera sin consumidor real y duplicaba la dirección del dispatcher material.
- `AnatomyStreamLifecycle` y su test autorreferencial se eliminaron: declaraban explícitamente no estar integrados en payload/runtime/catalog y duplicaban lifecycle que G3/G4 debe implementar sobre streams reales.
- La fórmula/guard específicos de Alex's Mobs Continued 2.1.9 para grizzly dejaron de ser producción. `GrizzlyPose` existe sólo en GameTest como fixture H1; el runtime general ya no registra ni hardcodea un provider/guard de grizzly.
- El motor `platform` legacy **no se borra aún**: todavía es la única implementación ejecutable de varias semánticas que deben preservarse durante migración. Se elimina en G5, después de demostrar equivalencia en el core nuevo. Conservarlo hasta entonces no autoriza fallback durante `BINDING` ni convierte su arquitectura en objetivo.
- La API pública vive en `collision.api`; la extracción de modelos cliente vive en `client.collision.preparation`; la recepción/cache cliente vive en `client.collision.network`. Los shims de los paths `platform.anatomy` y `client.platform.anatomy` ya fueron retirados al migrar todos los callers del repositorio en una operación atómica.
- Los tipos puros ya están separados en `collision.geometry`, `collision.pose` y `collision.physics`. El resto de orquestación se concentra deliberadamente en `collision.internal` hasta que G1/G2 definan fronteras reales para `catalog`, `network`, `runtime` e `integration`.
- No queda el paquete `platform.anatomy`. El paquete `platform` que sobrevive pertenece exclusivamente al motor legacy pendiente de sustitución funcional en G5.

## 3. Gates

### G0 — baseline compilable y limpieza estructural

**Requisitos:** NFR-025, NFR-031, NFR-035, NFR-037..039 y requisitos afectados por cualquier código eliminado.

Estado de tareas:

- [x] Consolidar documentación en requisitos, arquitectura, plan y VALIDATION como fuentes canónicas no solapadas.
- [x] Retirar planes/diarios/análisis anatómicos duplicados después de absorber información vigente y evidencia útil.
- [x] Corregir los fixtures desactualizados de `MaterialEventDispatcher` sin rebajar el contrato de event budget.
- [x] Inventariar código, mixins, tests y recursos legacy/anatómicos y clasificarlos por requisito/consumidor.
- [x] Eliminar scaffolding sin consumidor real (`RootEventDispatcher`, `AnatomyStreamLifecycle` y su test dedicado).
- [x] Retirar producción específica por especie que contradecía la estrategia general cuando ya podía conservarse como evidencia de test (`GrizzlyPose`).
- [x] Reorganizar físicamente el subsistema nuevo en `collision.api`, `geometry`, `pose`, `physics`, `internal` y los paquetes cliente `collision.preparation`/`network`, sin conservar shims con lógica duplicada.
- [x] Revisar expresamente el motor legacy y decidir su supervivencia temporal por requisitos, no por nostalgia; retirada fijada en G5.
- [x] Ejecutar una segunda compilación completa después del traslado de paquetes y registrar evidencia exacta en VALIDATION.
- [x] Comprobar que `main` no se modifica durante toda la reestructuración.

**Salida:** árbol comprensible y compilable, sin scaffolding conocido que sólo se pruebe a sí mismo y con el subsistema nuevo fuera de `platform.anatomy`. El feature puede seguir desactivado; G0 no afirma corrección física ni cumplimiento de G1-G9.

### S00 — Foundation Audit previa a G1

Estado: **cerrado**. El prerrequisito definido en `ENTITY_COLLISIONS_FOUNDATION_AUDIT.md` completó el modelo adversarial clean-room, inventario/clasificación de cimientos, reparaciones bloqueantes, holdouts, campaña de mutaciones y revisión final cero-cambios. El registro de ejecución está en `docs/sprints/S00-foundation-audit.md` y la evidencia realmente ejecutada en `VALIDATION.md`.

Los componentes que siguen `REWORK` o `REPLACE` tienen owner explícito en G1-G5 y no se consideran implementados por cerrar S00. **G1 está en curso; S01-S04 tienen implementación candidata y evidencia no adversarial, pero G1 no se declara cerrado hasta integrar la campaña adversarial paralela y completar una pasada final cero-cambios. G2 no se ha iniciado.**

### G1 — contrato público y data model desacoplados del legacy

**Estado:** **EN CURSO**. Las nueve tareas de implementación están completadas en el candidato actual y existen pruebas reales de servidor y cliente; quedan la campaña adversarial paralela, la repetición de CI sobre cualquier cambio que produzca y la pasada final completa sin cambios antes del cierre formal.

**Requisitos:** FR-001..006, FR-009..013, FR-015..034, FR-072..076; NFR-019..025, NFR-034..036.

Tareas:

1. [x] completar la frontera pública de `collision.api` y estabilizar sus DTOs públicos mínimos;
2. [x] romper el ciclo façade `api ↔ internal` mediante un backend/SPI interno explícito, sin filtrar implementación al API;
3. [x] crear interfaces/registries explícitos `GeometryEngine`, `PoseEngine`, `RootTransformProvider` y adapters físicos necesarios;
4. [x] crear binding/policy canónicos sin depender de `PlatformDefinition.Surface`;
5. [x] separar legacy-plane migration como decoder de datos, no motor;
6. [x] versionar codecs y capabilities;
7. [x] migrar policy de ratio/categorías/fricción y adapters de bodies al nuevo integration layer;
8. [x] demostrar un mod fixture que registra comportamiento por API y selecciona ese comportamiento por JSON;
9. [x] partir desde `collision.internal` las responsabilidades de catálogo/runtime/integration que ya tengan una frontera estable después de los pasos anteriores.

**Salida:** el core describe una entidad sin conocer su especie en Java y sin depender del motor superior antiguo; `internal` deja de ser el cajón de integración de G0 para las responsabilidades ya estabilizadas.

### G2 — pipeline material continuo Q2

**Requisitos:** FR-042..063; NFR-001..004, NFR-007..008, NFR-014..018, NFR-033.

Tareas:

1. dividir `AnatomyMovement` en estado/índice/query/contact/transport dentro de `collision.physics`/`runtime`/`integration`, quitando ownership redundante;
2. integrar `MaterialEventDispatcher` con hooks reales de root/joint/carry;
3. usar `MotionIntervalHandle`/trayectoria certificada para traslación, yaw, scale y joints, no sólo endpoint actual;
4. procesar varias contribuciones del mismo tick exactamente una vez cada una y mantener ancestry;
5. cerrar tangential retention, multicontacto, sliding y separation recovery;
6. impedir que broadphase oversized degrade a scan mundial en hot path: fallback acotado o cuarentena;
7. probar cadenas, obstrucción, wall squeeze, huecos y contacto que sólo existe en mitad del intervalo;
8. fijar e instrumentar budgets de sweep/eventos;
9. mover fuera de `collision.internal` los tipos físicos/orquestadores que queden realmente desacoplados al cerrar Q2.

**Salida:** física material correcta en server single-player/dedicated sin depender todavía de predicción bajo latencia.

### G3 — catálogo, engines generales y lifecycle

**Requisitos:** FR-014..041, FR-080..082, FR-089..092; NFR-003..013, NFR-015..018, NFR-026..029, NFR-032, NFR-036.

Tareas:

1. sustituir `WorldAnatomyCatalog` acoplado a perfiles legacy por catálogo/binding canónico;
2. preparar/serializar una vez por revisión y reutilizar bundle por receptor;
3. consolidar `ModelPart` GeometryEngine;
4. consolidar engines de pose vanilla y añadir engine general de `AnimationDefinition` antes de providers por especie;
5. convertir Citadel/Alex desde el vertical slice de test del grizzly a engine/pose-program reusable; el fixture grizzly no vuelve a producción;
6. añadir `RootTransformProvider` genérico y fixture de orientación externa;
7. mantener `DisplayRig` como SPI y sólo implementarlo cuando un target real lo exija;
8. implementar scanner/coverage report y estados FULL/SAFE_PARTIAL/EXCLUDED/UNRESOLVED;
9. cerrar reload válido/inválido, tracking tardío, unload, rebind, dimension, reconnect y reutilización de identidad;
10. hacer que unsupported states publiquen unavailable y recuperen sin geometry freeze;
11. implementar lifecycle/order sobre runtime y packets reales, sin reintroducir `AnatomyStreamLifecycle` como segundo ledger abstracto;
12. terminar la separación de `collision.internal` en `catalog`, `network` y `runtime` cuando esas fronteras sean estables.

**Salida:** catálogo general reproducible, extensible y con lifecycle transaccional.

### G4 — red, prediction, reconciliación y presentación causal

**Requisitos:** FR-077..088; NFR-001..018, NFR-026..031.

Tareas:

1. completar ledger/receipts/references sobre el lifecycle real para correlacionar movement packets con transportes aplicados;
2. prediction sólo para player/controlled vehicle local;
3. observer path sin carry local;
4. reconciliación sin double-apply ni drift;
5. convertir presentación de `CURRENT_ENDPOINT` a intervalo certificado donde Q2 lo requiera;
6. consolidar residual visual/camera en una única capa bajo `client.collision.presentation`;
7. ejecutar dedicated `allow-flight=false` con 0/100/200 ms y late tracking/reconnect.

**Salida:** multiplayer autoritativo y prediction estable.

### G5 — categorías especiales, placement y retirada del motor legacy

**Requisitos:** FR-007..013, FR-053..071, FR-089..092; NFR-033..035.

Tareas:

1. portar boat/raft, off-rail minecart, item y falling-block semantics al nuevo core;
2. verificar agua, rail, despawn, hardening, placement y anvil una vez;
3. portar sneak edge y jump release;
4. portar raycast/placement con permisos/inventario/footprint;
5. demostrar no regresión de fall/exhaustion/stats/Growth landing;
6. eliminar `PlatformPhysics`, `PlatformGeometry`, `PlatformState`, networking/camera/visual carry legacy, `automatic_top` y los recursos/runtime que sólo los alimentan cuando sus equivalentes estén verdes;
7. conservar únicamente el decoder de legacy surface si FR-023 sigue justificándolo;
8. comprobar que ningún mixin o helper sigue bifurcando entre dos motores físicos.

**Salida:** un solo motor físico. A partir de aquí no existe ruta legacy de gameplay.

### G6 — cobertura completa de Minecraft general

**Requisitos:** FR-016, FR-024..032, FR-038..040, FR-089..092; NFR-019..024, NFR-028..032.

Tareas:

1. ejecutar scanner sobre todos los `LivingEntity` Minecraft 26.2;
2. agrupar por GeometryEngine/PoseEngine/variant, nunca por lista manual como estrategia principal;
3. completar humanoids, quadrupeds, equines, felines, flying/aquatic y keyframed families necesarias;
4. mantener excepciones técnicas explícitas y tests negativos;
5. cerrar `UNRESOLVED=0` y llevar todos los ordinarios no excluidos a FULL;
6. generar catálogo reproducible con hashes/versions.

**Salida:** Scale Brews general cubre Minecraft sin perfiles físicos manuales por mob.

### G7 — migración de Clinging Reoriented

**Requisitos:** FR-001..004, FR-044..046, FR-072..085; NFR-021..024, NFR-030, NFR-033..035.

Tareas:

1. compilar consumer contra API exacta de G1+;
2. migrar preflight/raycast/contact/gravity frame;
3. preservar Space/charge/Reorientation/Elytra/efectos/persistencia/camera contractual;
4. borrar del consumer AABB selection, moving surfaces, carry, references y reconciliation duplicados después de tests verdes;
5. ejecutar transiciones cardinales y casos de bloqueo;
6. repetir network/latency con el consumer real.

**Salida:** Clinging consume Scale como único motor compartido.

### G8 — VanillaPlus compat como proyecto separado

**Requisitos:** FR-017..018, FR-031, FR-038..041; NFR-019..024, NFR-028..032, NFR-034, NFR-036.

Este gate vive principalmente en el repositorio privado de compatibilidad, no en el core.

Tareas:

1. fijar manifest de mods/resource packs y hashes;
2. proporcionar bindings/overrides/generated catalog para Alex's Mobs, Wilder Wild, Friends & Foes, Stormie's, composites de datapack y el stack visual correspondiente;
3. upstream al core cualquier engine reusable descubierto;
4. añadir preparación opcional CEM/EMF para el stack administrado, sin geometría C2S;
5. scanner obligatorio de todos los namespaces target y composites conocidos;
6. `UNRESOLVED=0`; SAFE_PARTIAL sólo con estado y razón explícitos;
7. QA visual con Fresh Animations/EMF además de física canónica.

**Salida:** compatibilidad VP26 versionada sin contaminar Scale Brews general.

### G9 — rendimiento y aceptación de entrega

**Requisitos:** todos; especialmente NFR-014, NFR-026..032.

Tareas:

1. ejecutar benchmark normativo y revisar hot paths/caches;
2. ejecutar unit/GameTest/client/dedicated/consumer/latency/soak;
3. registrar evidencia y hashes en VALIDATION;
4. QA humana de superficies animadas, cámaras, cadenas, vehículos y combinaciones del pack;
5. generar artefactos sólo después de todos los gates obligatorios;
6. no inferir tag/release/publicación/instalación fuera de la instrucción explícita correspondiente.

**Salida:** candidato completo para el scope declarado.

## 4. Dependencias

```text
G0
 ↓
G1
 ↓
G2 ─────┐
 ↓      │
G3      │
 ↓      │
G4      │
 ↓      │
G5      │
 ↓      │
G6      │
 ├──> G7
 └──> G8
       │
       └──┐
G7 ───────┴─> G9
```

G3 puede preparar tooling mientras G2 avanza, pero no se declara una familia FULL hasta que el solver que consume su movimiento material esté cerrado. G8 puede empezar bindings de prueba antes de G6, pero la compat específica no es prerequisito del core.

## 5. Regla de borrado

Una pieza se elimina si cumple cualquiera:

- no tiene consumidor real ni requisito futuro;
- duplica estado/ownership que pertenece a otra capa;
- implementa un fallback prohibido;
- sólo demuestra un experimento ya absorbido por otro componente;
- codifica una especie donde el comportamiento debe pertenecer a un engine reusable y puede conservarse como fixture/data;
- pertenece al motor legacy y su requisito ya tiene sustituto funcional verificado.

No se borra una implementación legacy si todavía es el **único** código que conserva un requisito vigente durante la migración. Se mantiene aislada y con retirada explícita en G5. Git conserva el historial; el árbol no necesita conservar scaffolding muerto.

Un shim de paquete sólo habría sido admisible temporalmente si no contenía lógica y evitaba una rotura masiva durante una migración atómica. Después del reordenamiento G0 ya no queda ninguno en el subsistema nuevo.

## 6. Método iterativo y convergencia

### Revisión del plan

**P1:** antiguos H0-H4 → dependencias explícitas contra FR/NFR; se detectó mezcla de pruebas verticales, migración y compat VP26.

**P2:** separación core general / VanillaPlus y sustitución de expansión “por mobs” por GeometryEngine/PoseEngine + coverage scanner.

**P3:** contraste con código: G0 se adelantó y Q2 se colocó antes de declarar familias FULL.

**P4:** lifecycle/red se dividió entre Q2 físico, catálogo/lifecycle y reconciliación/presentación. No cambió la dependencia principal.

**P5:** auditoría destructiva del legacy mostró que borrarlo antes de portar categorías/placement/network semantics violaría requisitos todavía sin sustituto; la eliminación total quedó fijada en G5.

**P6:** la primera limpieza eliminó scaffolding y sacó API/extractor a fronteras propias. La revisión siguiente no cambió el orden de gates.

**P7:** el requisito literal de reordenar el código sobreviviente obligó a ejecutar un traslado real, no sólo documentar paquetes objetivo. El ensayo compilado mostró que una división completa en ocho paquetes todavía introduciría acoplamiento nominal porque Q1/Q2 comparten contratos internos. Se adoptó `api/geometry/pose/physics/internal` como frontera transitoria y se dejó la partición fina para G1/G2.

**P8:** la revisión adversarial del árbol reordenado detectó que conservar `GrizzlyPose`/guard en producción contradecía NFR-019/020. Se movió a fixture H1 de GameTest y se retiró su registro/hardcode del runtime. No fue necesario crear, eliminar ni reordenar gates.

**P9:** tras actualizar arquitectura, plan y evidencia contra el árbol efectivo se repitió requisito → dependencia → código → aceptación. No apareció ningún cambio adicional de ownership, orden o gate. **Plan convergido para G0.**

### Revisión del código

**C1 — inventario:** cada clase/hook/test/recurso del subsistema se clasificó como runtime vigente, sustituto nuevo, regression oracle, scaffolding o tooling. Se señalaron `RootEventDispatcher` y `AnatomyStreamLifecycle` como candidatos de eliminación.

**C2 — revisión adversarial de borrado:** se intentó justificar la conservación de cada candidato y, a la inversa, la eliminación del legacy. Los dos scaffolds carecían de consumidor real; el legacy sí mantiene requisitos aún no portados. Resultado: se borran los scaffolds, se retiene temporalmente el legacy.

**C3 — primera revisión estructural:** `MaterialEventDispatcher`, `BodyPath`, receipts/histories y los kernels físicos conservan requisito futuro o aceptación concreta. API y extractor cliente se separaron del package histórico.

**C4 — revisión del grafo:** mover cada clase inmediatamente a `catalog/network/runtime/integration` habría creado fronteras falsas alrededor de dependencias package-private aún reales. Se trasladaron sólo las capas puras y se agrupó la orquestación acoplada en `collision.internal`.

**C5 — compilación del árbol reordenado:** el traslado completo compiló y la suite requerida alcanzó 137/137. Al revisar el diff se detectó el último hardcode específico del grizzly en producción; se eliminó y el fixture se hizo test-only.

**C6 — recompilación adversarial:** el árbol con grizzly sólo de test volvió a compilar y a pasar los 137 tests requeridos. Se comprobó además que no quedan `platform.anatomy`, `client.platform.anatomy`, shims de esos paths ni el script/workflow temporal usado para efectuar la migración.

**C7 — pasada de cierre:** se revisaron de nuevo clases supervivientes contra requisitos y plan. No aparece otra pieza simultáneamente (a) sin consumidor/requisito y (b) eliminable o reubicable sin implementar una frontera futura de G1-G5. **Revisión de código convergida para G0.**

Cualquier cambio de requisitos o implementación vuelve a ejecutar al menos una pasada completa; si esa pasada cambia el plan o la clasificación del código, se repite hasta obtener una pasada sin cambios.
