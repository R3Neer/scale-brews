# Plan del sistema de colisiones entre entidades

Estado: plan canónico de implementación de `chatgpt-editing`. **No define requisitos**: cada tarea referencia [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md). La arquitectura está en [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md) y la evidencia ejecutada en [VALIDATION](VALIDATION.md).

## 1. Criterio de planificación

El trabajo se ordena para eliminar primero ambigüedad y doble ownership, después cerrar la física causal, luego ampliar engines/cobertura y sólo entonces migrar consumidores y targets de compatibilidad.

Un gate no se cierra porque exista una clase o un test: se cierra cuando la evidencia requerida por sus FR/NFR está registrada para el commit exacto.

## 2. Estado inicial de esta revisión

Antes de reestructurar código se observó:

- arquitectura legacy de `platform` coexistiendo con `platform.anatomy`;
- `AnatomyMovement` con demasiadas responsabilidades;
- Q1 de endpoint causal bastante avanzado;
- Q2 (`MaterialEventDispatcher` + motion interval contracts) parcialmente construido pero no integrado como pipeline completo;
- networking/contact/history/provenance avanzados pero reconciliación aún preparatoria;
- catálogo anatómico acoplado a `PlatformDefinition` y `entity_platform`;
- providers de pose que mezclan buena generalización por familias con un caso específico `GrizzlyPose`;
- código de lifecycle/ordering experimental que debe demostrar uso real o eliminarse;
- CI del snapshot previo bloqueada en `compileGametestJava` por dos llamadas desactualizadas al constructor de `MaterialEventDispatcher`.

## 3. Gates

### G0 — baseline compilable y limpieza estructural

**Requisitos:** NFR-025, NFR-031, NFR-035, NFR-037..039 y requisitos afectados por cualquier código eliminado.

Tareas:

1. consolidar documentación en las cuatro fuentes canónicas;
2. eliminar diarios/planes/análisis duplicados una vez transferida su información vigente;
3. corregir los fixtures que ya no compilan, sin rebajar el contrato del dispatcher;
4. inventariar todas las clases/hook/resources del motor legacy y del anatómico;
5. eliminar helpers muertos o experimentales sin consumidores;
6. mover el código sobreviviente a paquetes por responsabilidad;
7. ejecutar build/CI del commit resultante y corregir sólo roturas producidas por la reestructuración.

**Salida:** un árbol comprensible y compilable. El feature puede seguir desactivado; G0 no afirma corrección física.

### G1 — contrato público y data model desacoplados del legacy

**Requisitos:** FR-001..006, FR-009..013, FR-015..034, FR-072..076; NFR-019..025, NFR-034..036.

Tareas:

1. convertir `AnatomyApi` en la frontera pública mínima del nuevo paquete `collision.api`, retirando aliases ambiguos en cuanto los consumidores internos estén migrados;
2. crear interfaces/registries explícitos `GeometryEngine`, `PoseEngine`, `RootTransformProvider` y adapters físicos necesarios;
3. crear binding/policy canónicos sin depender de `PlatformDefinition.Surface`;
4. separar legacy-plane migration como decoder de datos, no motor;
5. versionar codecs y capabilities;
6. migrar policy de ratio/categorías/fricción y adapters de bodies al nuevo integration layer;
7. demostrar un mod fixture que registra comportamiento por API y selecciona ese comportamiento por JSON.

**Salida:** el core ya puede describir una entidad sin conocer su especie en Java y sin depender del motor superior antiguo.

### G2 — pipeline material continuo Q2

**Requisitos:** FR-042..063; NFR-001..004, NFR-007..008, NFR-014..018, NFR-033.

Tareas:

1. dividir `AnatomyMovement` en estado/índice/query/contact/transport y quitar ownership redundante;
2. integrar `MaterialEventDispatcher` con hooks reales de root/joint/carry;
3. usar `MotionIntervalHandle`/trayectoria certificada para traslación, yaw, scale y joints, no sólo endpoint actual;
4. procesar varias contribuciones del mismo tick una vez cada una y mantener ancestry;
5. cerrar tangential retention, multicontacto, sliding y separation recovery;
6. asegurar que un broadphase oversized no degrada a scan mundial en hot path: debe usar fallback acotado/cuarentena;
7. probar cadenas, obstrucción, wall squeeze, huecos y contacto que sólo existe en mitad del intervalo;
8. fijar/instrumentar budgets de sweep/eventos.

**Salida:** física material correcta en server single-player/dedicated sin depender todavía de predicción bajo latencia.

### G3 — catálogo, engines generales y lifecycle

**Requisitos:** FR-014..041, FR-080..082, FR-089..092; NFR-003..013, NFR-015..018, NFR-026..029, NFR-032, NFR-036.

Tareas:

1. sustituir `WorldAnatomyCatalog` acoplado a perfiles legacy por catálogo/binding canónico;
2. preparar/serializar una vez por revisión y reutilizar bundle por receptor;
3. consolidar `ModelPart` GeometryEngine;
4. consolidar engines de pose vanilla y añadir engine general de `AnimationDefinition` antes de providers por especie;
5. convertir Citadel/Alex de vertical slice de grizzly a engine/pose-program reusable; cualquier código de grizzly queda como fixture/data, no producción específica;
6. añadir `RootTransformProvider` genérico y fixture de orientación externa;
7. mantener `DisplayRig` como SPI y sólo implementarlo cuando un target real lo exija;
8. implementar scanner/coverage report y estados FULL/SAFE_PARTIAL/EXCLUDED/UNRESOLVED;
9. cerrar reload válido/inválido, tracking tardío, unload, rebind, dimension, reconnect y misma identidad reutilizada;
10. hacer que unsupported states publiquen unavailable y recuperen sin geometry freeze.

**Salida:** catálogo general reproducible, extensible y con lifecycle transaccional.

### G4 — red, prediction, reconciliación y presentación causal

**Requisitos:** FR-077..088; NFR-001..018, NFR-026..031.

Tareas:

1. unificar lifecycle/order real y eliminar helpers paralelos no consumidos;
2. completar ledger/receipts/references para correlacionar movement packets con transportes aplicados;
3. prediction sólo para player/controlled vehicle local;
4. observer path sin carry local;
5. reconciliación sin double-apply ni drift;
6. convertir presentación de `CURRENT_ENDPOINT` a intervalo certificado donde Q2 lo requiera;
7. consolidar residual visual/camera en una única capa;
8. ejecutar dedicated `allow-flight=false` con 0/100/200 ms y late tracking/reconnect.

**Salida:** multiplayer autoritativo y prediction estable.

### G5 — categorías especiales, placement y equivalencia del feature antiguo

**Requisitos:** FR-007..013, FR-053..071, FR-089..092; NFR-033..035.

Tareas:

1. portar boat/raft, off-rail minecart, item y falling-block semantics al nuevo core;
2. verificar agua, rail, despawn, hardening, placement y anvil una vez;
3. portar sneak edge y jump release;
4. portar raycast/placement con permisos/inventario/footprint;
5. demostrar no regresión de fall/exhaustion/stats/Growth landing;
6. eliminar definitivamente `PlatformPhysics`, `PlatformGeometry`, `PlatformState`, networking/camera/visual carry legacy y recursos `automatic_top` cuando sus equivalentes estén verdes;
7. conservar únicamente el decoder de legacy surface si sigue justificado por FR-023.

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
2. proporcionar bindings/overrides/generated catalog para Alex's Mobs, Wilder Wild, Friends & Foes, Stormie's, composites de datapack y el stack visual que corresponda;
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

**Salida:** candidato que puede calificarse de completo para el scope declarado.

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

G3 puede preparar tooling mientras G2 avanza, pero no se declara una familia FULL hasta que el solver que consumirá su movimiento material esté cerrado. G8 puede empezar bindings de prueba antes de G6, pero no convierte compat específica en prerequisito del core.

## 5. Regla de borrado durante la reestructuración

En esta rama no se conserva código por nostalgia. Una pieza se elimina si cumple cualquiera:

- no tiene consumidor real ni requisito futuro;
- duplica estado/ownership que debe pertenecer a otra capa;
- implementa un fallback prohibido;
- sólo existe para demostrar un experimento ya absorbido por otro componente;
- codifica una especie donde el plan exige un engine reusable y puede quedar como fixture/data;
- pertenece al motor legacy y su requisito ya tiene sustituto funcional verificado.

No se borra todavía una implementación legacy si es el **único** código que mantiene un requisito vigente durante G0-G4; se aísla y se marca para retirada en G5. Git conserva el historial, pero el árbol de trabajo debe seguir siendo compilable entre gates.

## 6. Método iterativo aplicado al plan

**Pasada 1:** se trasladaron los antiguos H0-H4 a dependencias explícitas contra los nuevos FR/NFR. Se detectó que mezclaban pruebas verticales, migración y compat VP26.

**Pasada 2:** se separó core general de target VanillaPlus y se sustituyó expansión “por mobs” por GeometryEngine/PoseEngine + coverage scanner.

**Pasada 3:** al contrastar con el código, se adelantó G0 y se añadió una fase explícita de desacoplamiento del legacy. También se colocó Q2 antes de declarar familias FULL, porque expandir catálogo sobre endpoint-only collision produciría falsa cobertura.

**Pasada 4:** revisión de lifecycle/red separó Q2 físico (G2), catálogo/lifecycle (G3) y reconciliación/presentación (G4). La revisión de dependencias posterior no requirió mover tareas ni crear otro gate.

El plan queda estable cuando una nueva pasada requisito→dependencia→código→aceptación no cambia orden, ownership ni gates. Cualquier cambio futuro vuelve a ejecutar el ciclo completo.
