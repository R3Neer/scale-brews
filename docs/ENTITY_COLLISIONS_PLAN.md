# Plan del sistema de colisiones entre entidades

Estado: plan canónico de implementación de `chatgpt-editing`. **No define requisitos**: cada tarea referencia [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md). La arquitectura está en [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md) y la evidencia ejecutada en [VALIDATION](VALIDATION.md).

## 1. Criterio de planificación

El trabajo elimina primero ambigüedad y doble ownership, cierra después la física causal, amplía engines/cobertura y sólo entonces migra consumidores y targets de compatibilidad.

Un gate no se cierra porque exista una clase o un test. Se cierra cuando la evidencia requerida por sus FR/NFR está registrada para el commit exacto.

El estado de tareas vive **sólo aquí**. La arquitectura no mantiene un segundo snapshot de implementación y VALIDATION no mantiene una segunda lista de trabajo.

## 2. Resultado de la reestructuración inicial

La auditoría iterativa del árbol legacy + anatómico produjo estas decisiones estables:

- `AnatomyMovement` contiene una base Q1 avanzada, pero sigue mezclando registro de providers, histories, broadphase, solver, contacto, root tracking y carry. Dividirlo sin cerrar las fronteras causales de Q2 sólo movería acoplamiento entre paquetes; su partición real pertenece a G2.
- `MaterialEventDispatcher`, `BodyPath`, `ConservativeSweep`, `TemporalResponse`, `HierarchyMotion`, receipts, histories y trackers **se conservan** porque representan capacidades requeridas por G2-G4, aunque algunas no estén todavía integradas en el pipeline vivo.
- `RootEventDispatcher` se eliminó: era scaffolding de frontera sin consumidor real y duplicaba la dirección del dispatcher material.
- `AnatomyStreamLifecycle` y su test autorreferencial se eliminaron: declaraban explícitamente no estar integrados en payload/runtime/catalog y duplicaban lifecycle que G3/G4 debe implementar sobre streams reales.
- `GrizzlyPose` no se considera arquitectura válida por especie, pero se conserva temporalmente porque hoy sigue siendo parte del vertical slice H1 contra el modelo original. G3 lo sustituye por engine/pose-program Citadel reusable y deja cualquier fórmula específica únicamente como fixture/data.
- El motor `platform` legacy **no se borra aún**: todavía es la única implementación ejecutable de varias semánticas que deben preservarse durante migración. Se elimina en G5, después de demostrar equivalencia en el core nuevo. Conservarlo hasta entonces no autoriza fallback durante `BINDING` ni convierte su arquitectura en objetivo.
- La implementación real del extractor cliente se movió a `client.collision.preparation`; el path anterior contiene sólo un shim de compatibilidad para proofs históricos.
- La implementación real de la API pública se movió a `collision.api`; el antiguo `platform.anatomy.AnatomyApi` contiene sólo un shim de compatibilidad. Ambos shims se eliminan cuando sus callers históricos estén migrados, no se desarrollan como APIs paralelas.
- Los paquetes objetivo completos son los definidos exclusivamente en [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md#9-paquetes-objetivo). El resto del gran paquete interno se divide cuando G1/G2 creen fronteras reales, no mediante renombres cosméticos de clases todavía package-private entre sí.

## 3. Gates

### G0 — baseline compilable y limpieza estructural

**Requisitos:** NFR-025, NFR-031, NFR-035, NFR-037..039 y requisitos afectados por cualquier código eliminado.

Estado de tareas:

- [x] Consolidar documentación en requisitos, arquitectura, plan y VALIDATION como fuentes canónicas no solapadas.
- [x] Retirar planes/diarios/análisis anatómicos duplicados después de absorber información vigente y evidencia útil.
- [x] Corregir los fixtures desactualizados de `MaterialEventDispatcher` sin rebajar el contrato de event budget.
- [x] Inventariar código, mixins, tests y recursos legacy/anatómicos y clasificarlos por requisito/consumidor.
- [x] Eliminar scaffolding sin consumidor real (`RootEventDispatcher`, `AnatomyStreamLifecycle` y su test dedicado).
- [x] Sacar las fronteras ya desacoplables de los paquetes históricos: API pública a `collision.api` y extracción cliente a `client.collision.preparation`, dejando shims sin lógica sólo mientras compilan callers históricos.
- [x] Revisar expresamente el motor legacy y decidir su supervivencia temporal por requisitos, no por nostalgia; retirada fijada en G5.
- [ ] Registrar en VALIDATION un build/CI verde del commit final de esta reestructuración y comprobar que `main` no se movió.

**Salida:** árbol comprensible y compilable, sin scaffolding conocido que sólo se pruebe a sí mismo. El feature puede seguir desactivado; G0 no afirma corrección física.

### G1 — contrato público y data model desacoplados del legacy

**Requisitos:** FR-001..006, FR-009..013, FR-015..034, FR-072..076; NFR-019..025, NFR-034..036.

Tareas:

1. completar la frontera pública de `collision.api`, mover allí sus DTOs públicos mínimos y retirar los aliases/shim de `platform.anatomy` cuando no queden callers;
2. crear interfaces/registries explícitos `GeometryEngine`, `PoseEngine`, `RootTransformProvider` y adapters físicos necesarios;
3. crear binding/policy canónicos sin depender de `PlatformDefinition.Surface`;
4. separar legacy-plane migration como decoder de datos, no motor;
5. versionar codecs y capabilities;
6. migrar policy de ratio/categorías/fricción y adapters de bodies al nuevo integration layer;
7. demostrar un mod fixture que registra comportamiento por API y selecciona ese comportamiento por JSON.

**Salida:** el core describe una entidad sin conocer su especie en Java y sin depender del motor superior antiguo.

### G2 — pipeline material continuo Q2

**Requisitos:** FR-042..063; NFR-001..004, NFR-007..008, NFR-014..018, NFR-033.

Tareas:

1. dividir `AnatomyMovement` en estado/índice/query/contact/transport dentro de los paquetes `collision.physics`/`runtime`/`integration`, quitando ownership redundante;
2. integrar `MaterialEventDispatcher` con hooks reales de root/joint/carry;
3. usar `MotionIntervalHandle`/trayectoria certificada para traslación, yaw, scale y joints, no sólo endpoint actual;
4. procesar varias contribuciones del mismo tick exactamente una vez cada una y mantener ancestry;
5. cerrar tangential retention, multicontacto, sliding y separation recovery;
6. impedir que broadphase oversized degrade a scan mundial en hot path: fallback acotado o cuarentena;
7. probar cadenas, obstrucción, wall squeeze, huecos y contacto que sólo existe en mitad del intervalo;
8. fijar e instrumentar budgets de sweep/eventos.

**Salida:** física material correcta en server single-player/dedicated sin depender todavía de predicción bajo latencia.

### G3 — catálogo, engines generales y lifecycle

**Requisitos:** FR-014..041, FR-080..082, FR-089..092; NFR-003..013, NFR-015..018, NFR-026..029, NFR-032, NFR-036.

Tareas:

1. sustituir `WorldAnatomyCatalog` acoplado a perfiles legacy por catálogo/binding canónico;
2. preparar/serializar una vez por revisión y reutilizar bundle por receptor;
3. consolidar `ModelPart` GeometryEngine;
4. consolidar engines de pose vanilla y añadir engine general de `AnimationDefinition` antes de providers por especie;
5. convertir Citadel/Alex de vertical slice de grizzly a engine/pose-program reusable; retirar `GrizzlyPose` de producción y conservar sólo fixture/data de aceptación;
6. añadir `RootTransformProvider` genérico y fixture de orientación externa;
7. mantener `DisplayRig` como SPI y sólo implementarlo cuando un target real lo exija;
8. implementar scanner/coverage report y estados FULL/SAFE_PARTIAL/EXCLUDED/UNRESOLVED;
9. cerrar reload válido/inválido, tracking tardío, unload, rebind, dimension, reconnect y reutilización de identidad;
10. hacer que unsupported states publiquen unavailable y recuperen sin geometry freeze;
11. implementar lifecycle/order sobre runtime y packets reales, sin reintroducir el `AnatomyStreamLifecycle` descartado como segundo ledger abstracto.

**Salida:** catálogo general reproducible, extensible y con lifecycle transaccional.

### G4 — red, prediction, reconciliación y presentación causal

**Requisitos:** FR-077..088; NFR-001..018, NFR-026..031.

Tareas:

1. completar ledger/receipts/references sobre el lifecycle real para correlacionar movement packets con transportes aplicados;
2. prediction sólo para player/controlled vehicle local;
3. observer path sin carry local;
4. reconciliación sin double-apply ni drift;
5. convertir presentación de `CURRENT_ENDPOINT` a intervalo certificado donde Q2 lo requiera;
6. consolidar residual visual/camera en una única capa;
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
8. retirar shims de paquetes históricos que hayan quedado sin callers.

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
- codifica una especie donde existe ya el engine reusable que debe sustituirla;
- pertenece al motor legacy y su requisito ya tiene sustituto funcional verificado.

No se borra una implementación legacy si todavía es el **único** código que conserva un requisito vigente durante la migración. Se mantiene aislada y con retirada explícita en G5. Git conserva el historial; el árbol no necesita conservar scaffolding muerto.

Un shim de paquete sólo es admisible si no contiene lógica y evita una rotura masiva mientras se migran callers. Debe tener criterio de retirada en este plan y no puede recibir nuevas capacidades.

## 6. Método iterativo y convergencia

### Revisión del plan

**Pasada P1:** antiguos H0-H4 → dependencias explícitas contra FR/NFR; se detectó mezcla de pruebas verticales, migración y compat VP26.

**P2:** separación core general / VanillaPlus y sustitución de expansión “por mobs” por GeometryEngine/PoseEngine + coverage scanner.

**P3:** contraste con código: G0 se adelantó y Q2 se colocó antes de declarar familias FULL.

**P4:** lifecycle/red se dividió entre Q2 físico, catálogo/lifecycle y reconciliación/presentación. No cambió la dependencia principal.

**P5:** auditoría destructiva del legacy mostró que borrarlo antes de portar categorías/placement/network semantics violaría requisitos todavía sin sustituto; la eliminación total quedó fijada en G5. La organización de paquetes se vinculó a fronteras reales de G1/G2, no a renombres cosméticos.

**P6:** después de eliminar scaffolding y mover las dos fronteras ya desacoplables, se volvió a recorrer requisito → dependencia → código → aceptación. No fue necesario crear, eliminar ni reordenar gates. **Plan convergido en esta revisión.**

### Revisión del código

**Pasada C1 — inventario:** cada clase/hook/test/recurso del subsistema se clasificó como runtime vigente, sustituto nuevo, regression oracle, scaffolding o tooling. Se señalaron `RootEventDispatcher` y `AnatomyStreamLifecycle` como candidatos de eliminación.

**C2 — revisión adversarial de borrado:** se intentó justificar la conservación de cada candidato y, a la inversa, la eliminación del legacy. Los dos scaffolds carecían de consumidor real; el legacy sí mantiene requisitos aún no portados. Resultado: se borran los scaffolds, se retiene temporalmente el legacy.

**C3 — revisión tras cambios:** `MaterialEventDispatcher`, `BodyPath`, receipts/histories y el vertical slice grizzly siguen teniendo requisito futuro o aceptación concreta, por lo que no son código muerto. Se detectaron dos fronteras de paquete falsas y se movieron la API pública y el extractor cliente a sus paquetes objetivo. Los paths antiguos quedaron como shims sin lógica por compatibilidad de callers históricos.

**C4 — segunda revisión tras la nueva estructura:** no aparece otra clase que sea simultáneamente (a) sin consumidor/requisito y (b) eliminable sin retirar una capacidad aún no portada. Las siguientes eliminaciones dependen de implementar G1-G5, no de otra limpieza previa. **Revisión de código convergida para G0.**

Cualquier cambio de requisitos o implementación vuelve a ejecutar al menos una pasada completa; si esa pasada cambia el plan o la clasificación del código, se repite hasta obtener una pasada sin cambios.
