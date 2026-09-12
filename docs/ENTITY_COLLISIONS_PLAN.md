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

Estado: **cerrado**. El prerreisito definido en `ENTITY_COLLISIONS_FOUNDATION_AUDIT.md` completó modelo adversarial clean-room, inventario/clasificación, reparaciones bloqueantes, holdouts, campaña de mutaciones y revisión final cero-cambios. Registro: `docs/sprints/S00-foundation-audit.md`; evidencia: `VALIDATION.md`.

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

**Estado:** **ABIERTO / en ejecución**. S05, S06, S07, S08 y S09 están cerrados. Broadphase, identidad causal de intervalos, dispatcher live ROOT/JOINT, transporte anclado/DERIVED_CARRY, tangential retention, multicontacto, sliding, recovery, wall squeeze, contacto sólo intermedio y fronteras de budget ya tienen evidencia. La física material Q2 del gate está cerrada; G2 permanece abierto exclusivamente por la partición/ownership arquitectónicos de las tareas 1 y 9. El frente activo es S10, que extrae primero la frontera estable del passive transport ledger/cursor sin cambiar semántica física.

**Requisitos:** FR-042..063; NFR-001..004, NFR-007..008, NFR-014..018, NFR-033.

1. [ ] dividir `AnatomyMovement` en estado/índice/query/contact/transport dentro de fronteras reales, quitando ownership redundante;
2. [x] integrar `MaterialEventDispatcher` con hooks reales de root/joint/carry;
3. [x] usar `MotionIntervalHandle`/trayectoria certificada para traslación, yaw, scale y joints, no sólo endpoint actual;
4. [x] procesar varias contribuciones del mismo tick exactamente una vez cada una y mantener ancestry;
5. [x] cerrar tangential retention, multicontacto, sliding y separation recovery;
6. [x] impedir que broadphase oversized degrade a scan mundial en hot path: kernel `MaterialBroadphase` acotado, fail-closed y cuarentena local, cerrado en S05;
7. [x] probar cadenas, obstrucción, wall squeeze, huecos y contacto que sólo existe en mitad del intervalo;
8. [x] fijar e instrumentar budgets de sweep/eventos;
9. [ ] mover fuera de `collision.internal` tipos físicos/orquestadores sólo cuando queden realmente desacoplados al cerrar Q2.

**S05 cerrado:** `MaterialBroadphase<K>` quedó extraído a `collision.physics`; desaparecieron el fallback `all bounds` y `overflow` del hot query; entry/query/candidate budgets tienen outcomes explícitos; runtime propaga agotamiento de forma conservadora. Run `34602837677`, job `103274063124`: **277/277 required GameTests passed**.

**S06 cerrado:** `MotionIntervalHandle` quedó ligado a identidad causal completa y publicación exactly-once, con fences de epoch/revision/binding/local generation y staging acotado. La membresía espacial se mantiene por hooks causales conocidos, no por polling global.

**S07 cerrado:** el dispatcher consume intervalos live ROOT/JOINT, agrupa batches simultáneos, usa CCD temporal, localiza fallos por relación y revalida contactos retenidos tras batches rechazados. La campaña adversarial cerró overflow, provenance, contacto exacto, initial separation y conflictos multi-support.

**S08 cerrado tras reapertura adversarial tardía:** carry retenido usa trayectoria anclada certificada, obstrucción continua, receipts/passengers y cadenas `DERIVED_CARRY` base→dependientes. Después del cierre provisional sobre `0a6914ba...`, holdouts posteriores demostraron dos huecos FR-057: una entidad vanilla sólo en mitad de un arco y un soporte anatómico estacionario fuera del batch cuya AABB vanilla estaba suprimida. El cierre vigente es `4abe57cd30441761e667f8a56fa233f2a9d709e8`: ordinary run `34651958772` verde, artefacto `10284575580` SHA-256 `9439f0806924e8a35c85c6fa6f3d4b7d46152f10baeb37422bde779ff9a14818`; prepared run `34651958909`, job `103435928454`, export original verde y servidor **2/2**. La revisión posterior no produjo cambios de producción.

**S09 cerrado:** la física live de multicontacto/sliding/recovery quedó cubierta por holdouts de tangential retention, floor+wall y triple contacto, separación inicial acotada, wall squeeze/localidad, contacto estrictamente intermedio y metamorfismos de traslación/gravedad. La campaña adversarial reparó dos defectos reales: aliases numéricos del frontier de recovery y agotamiento del screening temporal con geometría preparada real. El candidate limpio `05e8ad9c90bc4c9f09a5d47d49e929c8a080b628` pasó ordinary run `34659541784` y prepared run `34659541774`, job `103458981680`, con export original verde y servidor **2/2**. `80fc63386dc0ae5faf10ce28f4658aa2d24c5fec` fijó el borde exacto A11 N-1/N en 256 evaluaciones; la revisión posterior sólo renombró un holdout para alinear nombre y oracle (`2aca10ef37c8fdbde85a26e9a4a5c0067189eb0e`, run `34659926225` verde), sin cambios de producción.

**S10 activo:** `docs/sprints/S10-passive-transport-ledger.md` define la primera extracción arquitectónica posterior a la estabilización física: ownership único del ledger/cursor de transporte pasivo en `collision.runtime`, con `AuthorityPoseTracker` como consumidor real. S10 sólo cubre una subresponsabilidad de las tareas 1/9 y no cierra G2 por sí solo.

**Siguiente frente:** ejecutar S10 y continuar la partición de `AnatomyMovement` únicamente por fronteras ya demostradas, eliminando ownership redundante y moviendo tipos/orquestadores fuera de `collision.internal` sólo cuando tengan consumidores reales y dependencias acíclicas.

**Salida:** física material correcta en server single-player/dedicated y ownership Q2 particionado, sin depender todavía de predicción bajo latencia.

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

S05 abrió G2 con una tesis aislada: eliminar el fallback global del broadphase sin adelantar causalidad continua. El kernel y su integración viva cerraron con 277/277 y una pasada estructural sin cambios.

S06 fijó identidad/continuidad material y exactly-once; S07 conectó esos intervalos al dispatcher live y cerró CCD/fallo local; S08 añadió carry anclado continuo y cadenas derivadas. La reapertura tardía de S08 añadió dos restricciones importantes sin cambiar requisitos: las entidades vanilla deben barrerse a lo largo del `BodyPath`, y los soportes anatómicos estacionarios fuera del batch siguen siendo obstáculos mediante el broadphase material aunque su AABB vanilla esté suprimida. El cierre vigente de S08 es `4abe57cd...`.

S09 cerró las tareas físicas 5, 7 y 8: tangential retention, multicontacto/sliding, recovery, wall squeeze/localidad, contacto sólo intermedio y fronteras exactas de budget. La revisión adversarial encontró y reparó explosión de aliases de coma flotante en recovery y agotamiento del screening temporal bajo geometría preparada real sin aumentar budgets. Después de S09, G2 sólo mantiene abiertas 1 y 9.

S10 toma la primera subresponsabilidad arquitectónica ya desacoplable: ledger/cursor de transporte pasivo. Su plan limita expresamente el scope a ownership/history/generation y deja la aplicación física del carry fuera, para continuar la partición por fronteras reales en lugar de por estética.

G2 continúa con el mismo patrón: sprint pequeño, modelo adversarial previo, implementación, suite, revisión y una pasada final sin cambios antes de marcar cada scope como cerrado.

### Revisión del código

El inventario y revisiones destructivas de G0 fijaron qué conservar/eliminar. G1 dejó fronteras públicas/data estables. S05 extrajo `MaterialBroadphase`; S06-S09 establecieron fronteras causales y físicas reales alrededor de `MaterialIntervalRuntime`, `MaterialPhysicsRuntime`, `AnchoredTransportPlanner`, `TemporalResponse`, `AnatomySeparation` y el dispatcher, sin fingir que `AnatomyMovement` ya está completamente particionado.

El siguiente corte es S10 y las extracciones posteriores de G2: sólo se moverá ownership cuando exista una frontera verificable con consumidores reales; la extracción por estética sigue prohibida.

Cualquier cambio de requisitos o implementación vuelve a ejecutar una pasada completa; si esa pasada cambia plan o clasificación, se repite hasta obtener una pasada sin cambios.