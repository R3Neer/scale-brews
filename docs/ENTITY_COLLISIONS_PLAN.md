# Plan del sistema de colisiones entre entidades

Estado: plan canónico de implementación de `chatgpt-editing`. **No define requisitos**: cada tarea referencia [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md). La arquitectura está en [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md) y la evidencia ejecutada en [VALIDATION](VALIDATION.md).

## 1. Criterio de planificación

El trabajo elimina primero ambigüedad y doble ownership, cierra después la física causal, amplía engines/cobertura y sólo entonces migra consumidores y targets de compatibilidad.

Un gate no se cierra porque exista una clase o un test. Se cierra cuando la evidencia requerida por sus FR/NFR está registrada para el commit exacto.

El estado de tareas vive **sólo aquí**. La arquitectura no mantiene un segundo snapshot de implementación y VALIDATION no mantiene una segunda lista de trabajo.

## 2. Resultado de la reestructuración inicial

La auditoría iterativa del árbol legacy + anatómico produjo estas decisiones estables:

- `AnatomyMovement`, dentro de `collision.internal`, contiene una base Q1 avanzada, pero sigue mezclando registro de providers, histories, broadphase, solver, contacto, root tracking y carry. Su partición real pertenece a G2.
- `MaterialEventDispatcher`, `BodyPath`, `ConservativeSweep`, `TemporalResponse`, `HierarchyMotion`, receipts, histories y trackers se conservan porque representan capacidades requeridas por G2-G4.
- `RootEventDispatcher` y `AnatomyStreamLifecycle` se eliminaron como scaffolding sin consumidor real.
- La fórmula/guard específicos de Alex's Mobs Continued 2.1.9 para grizzly dejaron de ser producción; `GrizzlyPose` existe sólo como fixture H1 de GameTest.
- El motor `platform` legacy no se borra aún: sigue siendo la única implementación de varias semánticas que deben preservarse durante migración. Se elimina en G5.
- La API pública vive en `collision.api`; modelos cliente en `client.collision.preparation`; recepción/cache en `client.collision.network`.
- Los tipos puros ya están separados en `collision.geometry`, `collision.pose` y `collision.physics`. La orquestación acoplada permanece en `collision.internal` hasta que G2/G3 definan fronteras reales.
- No queda el paquete `platform.anatomy`; el `platform` superviviente pertenece exclusivamente al motor legacy pendiente de G5.

## 3. Gates

### G0 — baseline compilable y limpieza estructural

**Requisitos:** NFR-025, NFR-031, NFR-035, NFR-037..039 y requisitos afectados por código eliminado.

Estado: **cerrado**.

- [x] consolidar documentación canónica;
- [x] retirar planes/diarios duplicados;
- [x] corregir fixtures desactualizados de `MaterialEventDispatcher` sin rebajar budgets;
- [x] inventariar legacy/anatomy por requisito/consumidor;
- [x] eliminar scaffolding sin consumidor real;
- [x] retirar producción específica por especie sustituible por engines/fixtures;
- [x] reorganizar el subsistema nuevo en fronteras reales;
- [x] decidir supervivencia temporal del legacy y retirada en G5;
- [x] compilar y registrar evidencia del árbol reordenado;
- [x] comprobar que `main` no se modifica.

**Salida:** árbol comprensible y compilable, sin scaffolding conocido que sólo se pruebe a sí mismo.

### S00 — Foundation Audit previa a G1

Estado: **cerrado**. El prerrequisito definido en `ENTITY_COLLISIONS_FOUNDATION_AUDIT.md` completó modelo adversarial clean-room, inventario/clasificación, reparaciones bloqueantes, holdouts, campaña de mutaciones y revisión final cero-cambios. Registro: `docs/sprints/S00-foundation-audit.md`; evidencia: `VALIDATION.md`.

Los componentes que siguen `REWORK` o `REPLACE` tienen owner explícito en G1-G5. La reapertura adversarial posterior de G1 fue reparada y revalidada; **G1 está cerrado de nuevo y G2 es el primer gate abierto**.

### G1 — contrato público y data model desacoplados del legacy

**Estado:** **CERRADO**. Las nueve tareas están implementadas y las dos campañas adversariales quedaron integradas. La reapertura final de NFR-025 se resolvió en `a09c881a6a526bb735fe9e5f9f4a26d74325e615` haciendo `collision.runtime.AnatomyBackend` independiente de `collision.api`; la façade traduce DTOs en el borde. La suite servidor pasó **268/268** y la prueba real cliente/integrated/dedicated volvió a quedar verde antes de retirar el workflow temporal.

**Requisitos:** FR-001..006, FR-009..013, FR-015..034, FR-072..076; NFR-019..025, NFR-034..036.

1. [x] completar frontera pública de `collision.api` y DTOs mínimos;
2. [x] backend/SPI explícito sin filtrar implementación al consumer ni formar ciclos de capas;
3. [x] `GeometryEngine`, `PoseEngine`, `RootTransformProvider` y adapters necesarios;
4. [x] binding/policy canónicos sin `PlatformDefinition.Surface`;
5. [x] legacy-plane migration como decoder, no motor;
6. [x] codecs/capabilities versionados;
7. [x] policy ratio/categorías/fricción y body adapters en integration;
8. [x] fixture mod por API seleccionado por JSON;
9. [x] extraer de `collision.internal` responsabilidades con frontera estable.

**Evidencia de cierre vigente:** server run `34600301662`, job `103265686046`, 268/268; client proof run `34600577409`, job `103266578934`, incluido dedicated 0/100/200 ms RTT. Detalle en `VALIDATION.md` y S01/S04.

**Salida:** el core describe una entidad sin conocer especie en Java y sin depender del motor superior antiguo.

### G2 — pipeline material continuo Q2

**Estado:** **ABIERTO / en ejecución desde el cierre de G1**. Se implementará por sprints pequeños según `ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`; no se declarará cerrado por una refactorización parcial.

**Requisitos:** FR-042..063; NFR-001..004, NFR-007..008, NFR-014..018, NFR-033.

1. [ ] dividir `AnatomyMovement` en estado/índice/query/contact/transport dentro de fronteras reales, quitando ownership redundante;
2. [ ] integrar `MaterialEventDispatcher` con hooks reales de root/joint/carry;
3. [ ] usar `MotionIntervalHandle`/trayectoria certificada para traslación, yaw, scale y joints, no sólo endpoint actual;
4. [ ] procesar varias contribuciones del mismo tick exactamente una vez cada una y mantener ancestry;
5. [ ] cerrar tangential retention, multicontacto, sliding y separation recovery;
6. [ ] impedir que broadphase oversized degrade a scan mundial en hot path: fallback acotado o cuarentena;
7. [ ] probar cadenas, obstrucción, wall squeeze, huecos y contacto que sólo existe en mitad del intervalo;
8. [ ] fijar e instrumentar budgets de sweep/eventos;
9. [ ] mover fuera de `collision.internal` tipos físicos/orquestadores sólo cuando queden realmente desacoplados al cerrar Q2.

**Primer frente:** aislar y acotar el broadphase material, eliminando el fallback de scan global y estableciendo la costura para envelopes temporales certificados antes de conectar el dispatcher vivo. El sprint concreto y su adversarial model viven en `docs/sprints/`.

**Salida:** física material correcta en server single-player/dedicated sin depender todavía de predicción bajo latencia.

### G3 — catálogo, engines generales y lifecycle

**Requisitos:** FR-014..041, FR-080..082, FR-089..092; NFR-003..013, NFR-015..018, NFR-026..029, NFR-032, NFR-036.

1. [ ] sustituir `WorldAnatomyCatalog` acoplado a perfiles legacy por catálogo/binding canónico;
2. [ ] preparar/serializar una vez por revisión y reutilizar bundle por receptor;
3. [ ] consolidar `ModelPart` GeometryEngine;
4. [ ] consolidar engines de pose vanilla y engine general de `AnimationDefinition`;
5. [ ] convertir Citadel/Alex a engine/pose-program reusable;
6. [ ] añadir `RootTransformProvider` genérico y fixture externo;
7. [ ] mantener `DisplayRig` como SPI hasta target real;
8. [ ] scanner/coverage FULL/SAFE_PARTIAL/EXCLUDED/UNRESOLVED;
9. [ ] reload/tracking/unload/rebind/dimension/reconnect/reutilización de identidad;
10. [ ] unsupported states publican unavailable y recuperan sin freeze;
11. [ ] lifecycle/order sobre runtime y packets reales;
12. [ ] terminar separación de `collision.internal` cuando fronteras sean estables.

**Salida:** catálogo general reproducible, extensible y con lifecycle transaccional.

### G4 — red, prediction, reconciliación y presentación causal

**Requisitos:** FR-077..088; NFR-001..018, NFR-026..031.

1. [ ] ledger/receipts/references sobre lifecycle real;
2. [ ] prediction sólo para player/controlled vehicle local;
3. [ ] observer path sin carry local;
4. [ ] reconciliación sin double-apply ni drift;
5. [ ] presentación `CURRENT_ENDPOINT` → intervalo certificado donde Q2 lo requiera;
6. [ ] residual visual/camera en única capa `client.collision.presentation`;
7. [ ] dedicated `allow-flight=false` 0/100/200 ms y late tracking/reconnect.

**Salida:** multiplayer autoritativo y prediction estable.

### G5 — categorías especiales, placement y retirada del motor legacy

**Requisitos:** FR-007..013, FR-053..071, FR-089..092; NFR-033..035.

1. [ ] portar boat/raft, off-rail minecart, item y falling-block semantics;
2. [ ] verificar agua, rail, despawn, hardening, placement y anvil una vez;
3. [ ] portar sneak edge y jump release;
4. [ ] portar raycast/placement con permisos/inventario/footprint;
5. [ ] demostrar no regresión de fall/exhaustion/stats/Growth landing;
6. [ ] eliminar `PlatformPhysics`, `PlatformGeometry`, `PlatformState`, networking/camera/visual carry legacy, `automatic_top` y recursos/runtime sólo cuando sus equivalentes estén verdes;
7. [ ] conservar únicamente decoder legacy surface si FR-023 sigue justificándolo;
8. [ ] comprobar que ningún mixin/helper bifurca entre dos motores físicos.

**Salida:** un solo motor físico.

### G6 — cobertura completa de Minecraft general

**Requisitos:** FR-016, FR-024..032, FR-038..040, FR-089..092; NFR-019..024, NFR-028..032.

1. [ ] scanner sobre todos los `LivingEntity` Minecraft 26.2;
2. [ ] agrupar por engines/variant, no lista manual;
3. [ ] completar familias necesarias;
4. [ ] mantener excepciones técnicas explícitas y tests negativos;
5. [ ] `UNRESOLVED=0` y ordinarios no excluidos FULL;
6. [ ] catálogo reproducible con hashes/versions.

### G7 — migración de Clinging Reoriented

**Requisitos:** FR-001..004, FR-044..046, FR-072..085; NFR-021..024, NFR-030, NFR-033..035.

1. [ ] compilar consumer contra API final;
2. [ ] migrar preflight/raycast/contact/gravity frame;
3. [ ] preservar Space/charge/Reorientation/Elytra/efectos/persistencia/camera;
4. [ ] borrar AABB selection/moving surfaces/carry/references/reconciliation duplicados del consumer;
5. [ ] transiciones cardinales y bloqueo;
6. [ ] network/latency con consumer real.

### G8 — VanillaPlus compat como proyecto separado

**Requisitos:** FR-017..018, FR-031, FR-038..041; NFR-019..024, NFR-028..032, NFR-034, NFR-036.

1. [ ] manifest de mods/resource packs y hashes;
2. [ ] bindings/overrides/generated catalog para targets VP26;
3. [ ] upstream de engines reusables al core;
4. [ ] preparación opcional CEM/EMF sin geometría C2S;
5. [ ] scanner de namespaces target/composites;
6. [ ] `UNRESOLVED=0`; SAFE_PARTIAL sólo explícito;
7. [ ] QA visual Fresh Animations/EMF.

### G9 — rendimiento y aceptación de entrega

**Requisitos:** todos; especialmente NFR-014, NFR-026..032.

1. [ ] benchmark normativo y hot paths/caches;
2. [ ] unit/GameTest/client/dedicated/consumer/latency/soak;
3. [ ] evidencia/hashes en VALIDATION;
4. [ ] QA humana de superficies animadas, cámaras, cadenas, vehículos y pack;
5. [ ] artefactos sólo tras gates obligatorios;
6. [ ] sin tag/release/publicación/instalación salvo instrucción explícita.

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

Una pieza se elimina si: no tiene consumidor/requisito; duplica ownership; implementa fallback prohibido; sólo demuestra experimento absorbido; codifica especie donde corresponde engine reusable; o pertenece al legacy y su requisito ya tiene sustituto verificado.

No se elimina una implementación legacy si todavía es el único código que conserva un requisito vigente durante migración. Se mantiene aislada y con retirada explícita en G5.

## 6. Método iterativo y convergencia

### Revisión del plan

P1-P9 consolidaron dependencias FR/NFR, separaron core/VP26, colocaron Q2 antes de familias FULL, separaron lifecycle/red, retuvieron temporalmente el legacy, ejecutaron reordenamiento real, eliminaron hardcodes grizzly y convergieron G0.

G1 se ejecutó en S01-S04. Dos campañas adversariales reabrieron implementación, no requisitos: primero backend público/codec permisivo; después ciclo `api ↔ runtime`. Ambas fueron reparadas sin cambiar orden de gates. La evidencia final 268/268 + client/dedicated cerró G1 de nuevo.

G2 se ejecutará con el mismo patrón: sprint pequeño, modelo adversarial previo, implementación, suite, revisión y una pasada final sin cambios antes de marcar cada scope como cerrado.

### Revisión del código

El inventario y revisiones destructivas de G0 fijaron qué conservar/eliminar. G1 dejó fronteras públicas/data estables y confirmó que las responsabilidades Q2 permanecían sin adelantar dentro de `AnatomyMovement`/dispatcher. La primera revisión de G2 parte de ese árbol y debe extraer ownership sólo cuando reduzca acoplamiento real, no para producir paquetes decorativos.

Cualquier cambio de requisitos o implementación vuelve a ejecutar una pasada completa; si esa pasada cambia plan o clasificación, se repite hasta obtener una pasada sin cambios.