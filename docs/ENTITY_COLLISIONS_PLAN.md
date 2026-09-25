# Plan del sistema de colisiones entre entidades

Estado: plan canónico de implementación de `chatgpt-editing`. **No define requisitos**: cada tarea referencia [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md). La arquitectura está en [ENTITY_COLLISIONS](ENTITY_COLLISIONS.md) y la evidencia ejecutada en [VALIDATION](VALIDATION.md).

## 1. Criterio de planificación

El trabajo elimina primero ambigüedad y doble ownership, cierra después la física causal, amplía engines/cobertura y sólo entonces migra consumidores y targets de compatibilidad.

Un gate no se cierra porque exista una clase o un test. Se cierra cuando la evidencia requerida por sus FR/NFR está registrada para el commit exacto.

El estado de tareas vive **sólo aquí**. La arquitectura no mantiene un segundo snapshot de implementación y VALIDATION no mantiene una segunda lista de trabajo.

Este plan **no congela `main`** mientras dura el proyecto. Antes de cada sprint que toque integración, mixins, limpieza, migración legacy, controles, interacción o merge/rebase, se compara contra el `main` vigente y se preservan sus cambios ajenos a entity collisions. Tiny Mounts, equipamiento/menús, alcance, gravedad efectiva, attachments/render, gamefeel y cualquier otra mecánica mainline pueden seguir evolucionando en paralelo. Su ausencia de este roadmap no las convierte en código descartable. La rama de colisiones se integra alrededor de ellas salvo conflicto explícito con un FR/NFR canónico.

## 2. Resultado de la reestructuración inicial

La auditoría iterativa del árbol legacy + anatómico produjo estas decisiones estables:

- `AnatomyMovement`, dentro de `collision.internal`, contenía una base Q1 avanzada, pero mezclaba registro de providers, histories, broadphase, solver, contacto, root tracking y carry. Su partición real pertenecía a G2.
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

Los componentes que siguen `REWORK` o `REPLACE` tienen owner explícito en G1-G5. La reapertura adversarial posterior de G1 fue reparada y revalidada; **G1 está cerrado. G2 cerró históricamente después, pero permanece reabierto de forma localizada por la revisión posterior de FR-053 descrita en su gate.**

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

**Estado:** **CERRADO tras recertificación adversarial FR-053**. S05-S14 permanecen cerrados y la reapertura tardía bloqueo-vs-`Entity.push` también queda resuelta. El fix productivo final `1437795ce18689282b4532fc3644bc4566cd1f58` libera inmediatamente el guard legacy al tomar ownership shared-physics; el holdout endurecido `5b4b516...` demuestra en la misma pareja que legacy puro conserva su supresión antes del handoff y READY recupera exactamente el `push` vanilla sin esperar cleanup tick. Run `35327758929`, job `105544729000`, artifact `10539368207`, SHA-256 `865ff29a05c58c7f4205f11713c005d5e000352abaf5eab4bbfd0417d4095c6b`. La frontera no-wall volvió a verde en `35327844908` y el single geometric owner + mutation-kill permanecieron verdes en `35205403082`. Cierre completo: `docs/sprints/G2-adversarial-fr053-final-closeout.md`.

El cierre histórico de G2 no afirmaba que `AnatomyMovement` hubiera desaparecido ni exigía mover tipos antes de tener una frontera acíclica. Tras S14, `FRAME_SERIALS` + root history quedaron deliberadamente dentro del orquestador porque `GeometryProvider.CausalEndpoint` todavía contenía `AnatomyMovement.RootFrame`. **Ese aplazamiento ya fue resuelto posteriormente en G3.12**: `RootFrame`/`RootFrameLedger` poseen provenance root y `AnatomyEndpointLedger` posee endpoint serial state, mientras `AnatomyMovement` conserva decisiones físicas/query. Esto actualiza el árbol actual sin reescribir el criterio histórico de cierre G2.

**Requisitos:** FR-042..063; NFR-001..004, NFR-007..008, NFR-014..018, NFR-033.

1. [x] dividir `AnatomyMovement` en estado/índice/query/contact/transport dentro de fronteras reales, quitando ownership redundante;
2. [x] integrar `MaterialEventDispatcher` con hooks reales de root/joint/carry;
3. [x] usar `MotionIntervalHandle`/trayectoria certificada para traslación, yaw, scale y joints, no sólo endpoint actual;
4. [x] procesar varias contribuciones materiales genuinas del mismo tick exactamente una vez y mantener ancestry;
5. [x] cerrar tangential retention, multicontacto, sliding y separation recovery;
6. [x] impedir que broadphase oversized degrade a scan mundial en hot path: kernel `MaterialBroadphase` acotado, fail-closed y cuarentena local, cerrado en S05;
7. [x] probar cadenas, obstrucción, wall squeeze, huecos y contacto que sólo existe en mitad del intervalo;
8. [x] fijar e instrumentar budgets de sweep/eventos;
9. [x] mover fuera de `collision.internal` tipos físicos/orquestadores sólo cuando queden realmente desacoplados al cerrar Q2; los tipos aún acoplados a root/endpoint quedan deliberadamente para G3;
10. [x] recerrar FR-053: una pareja fuera de elegibilidad moving-platform no adquiere pared anatómica, y una pareja elegible con contacto material conserva el `Entity.push` vanilla exactamente una vez sin doble bloqueo/carry.

**S05 cerrado:** `MaterialBroadphase<K>` quedó extraído a `collision.physics`; desaparecieron el fallback `all bounds` y `overflow` del hot query; entry/query/candidate budgets tienen outcomes explícitos; runtime propaga agotamiento de forma conservadora. Run `34602837677`, job `103274063124`: **277/277 required GameTests passed**.

**S06 cerrado:** `MotionIntervalHandle` quedó ligado a identidad causal completa y publicación exactly-once, con fences de epoch/revision/binding/local generation y staging acotado. La membresía espacial se mantiene por hooks causales conocidos, no por polling global.

**S07 cerrado:** el dispatcher consume intervalos live ROOT/JOINT, agrupa batches simultáneos, usa CCD temporal, localiza fallos por relación y revalida contactos retenidos tras batches rechazados. La campaña adversarial cerró overflow, provenance, contacto exacto, initial separation y conflictos multi-support.

**S08 cerrado tras reapertura adversarial tardía:** carry retenido usa trayectoria anclada certificada, obstrucción continua, receipts/passengers y cadenas `DERIVED_CARRY` base→dependientes. Después del cierre provisional sobre `0a6914ba...`, holdouts posteriores demostraron dos huecos FR-057: una entidad vanilla sólo en mitad de un arco y un soporte anatómico estacionario fuera del batch cuya AABB vanilla estaba suprimida. El cierre vigente es `4abe57cd30441761e667f8a56fa233f2a9d709e8`: ordinary run `34651958772` verde, artefacto `10284575580` SHA-256 `9439f0806924e8a35c85c6fa6f3d4b7d46152f10baeb37422bde779ff9a14818`; prepared run `34651958909`, job `103435928454`, export cliente original verde y servidor **2/2**. La revisión posterior no produjo cambios de producción.

**S09 cerrado tras reaperturas adversariales tardías:** multicontacto/sliding/recovery quedó cubierto por tangential retention, floor+wall y triple contacto, separación inicial acotada, wall squeeze/localidad, contacto estrictamente intermedio y metamorfismos de traslación/gravedad. La banda numérica de contacto quedó unificada entre CCD y screening; el scheduler prioriza trabajo causal sin omitir piezas; `validCorrection(q)` evita doble carga y preserva `SKIN+ULP`; y el hardening final permite un probe ampliado únicamente para localizar el primer candidato midpoint-positive antes de existir `earliest`, manteniendo el budget global en 256. El último cambio productivo es `d04874085b60ff4c9aaef7246928cf8379240484`. Workflow `34708981488`, job `103594131113`, validó antes del push la suite ordinaria, exportación cliente original y servidor prepared **2/2** sobre la fuente exacta del parche. La ordinary independiente del árbol integrado, run `34708981487`, job `103594130965`, pasó **386/386** y produjo artifact `10302359158`, SHA-256 `c6f506d4cc33cb3d2071d4534eb61057114ab116d53626d4350e4776038ce363`.

**S10 cerrado:** `docs/sprints/S10-passive-transport-ledger.md` extrajo a `collision.runtime.TransportLedger` el ownership único de current/history/generation/cursor de transporte pasivo. `AuthorityPoseTracker` consume el ledger directamente; `AnatomyMovement` conserva únicamente la aplicación física del carry y las façades con consumidores reales. La revisión final retiró la façade de cursor que quedó huérfana tras la migración.

**S11 cerrado:** `docs/sprints/S11-shared-gravity-authority.md` reconcilió una única autoridad de gravedad en `integration.gravity.GravityFrames`, eliminó `AnatomyMovement.GRAVITY` y `collision.internal.GravityFrames`, y mantuvo `collision.api.GravityFrame` como representación física canónica. La revisión post-cierre separó `DOWN` explícito de cleanup en el seam de tests y añadió la nueva autoridad compartida a los triggers de prepared CI. Snapshot final `52770bc04d2945f800bf06f78c7e4e33f5bcc6f0`: ordinary run `34691237169`, job `103546736147`, **365/365**; prepared run `34691237142`, job `103546736010`, export cliente original verde y servidor **2/2**.

**S12 cerrado:** `docs/sprints/S12-contact-state-ownership.md` extrajo a `collision.internal.AnatomyContactState` el ownership único de contacto actual, sequence watermark, anchor, `SurfaceContact` y suspensiones body/support. `AnatomyMovement` conserva la validación física y la invalidación de receipts. La revisión post-verde reabrió S12 al demostrar una relación stale cross-dimension cuando el body ya había transitado pero el support seguía en el nivel desactivado; `7142207dba60a64c5f69f8cbe0d56bae5df05a13` reparó el cleanup por ambos extremos sin rebobinar sequence. S12 permanece cerrado en la cadena integrada final.

**S13 cerrado:** `docs/sprints/S13-spatial-index-ownership.md` extrajo a `collision.internal.AnatomySpatialIndex` el ownership único del índice por `Level`/tick, `MaterialBroadphase`, budgets y membership bounded. `AnatomyMovement` conserva sampling, `queryFrame/currentSnapshot`, construcción del envelope, elegibilidad, suspensión y política de cuarentena. `FrameStamp`, `SpatialIndex.frames` y `currentFrameStamp` muertos se eliminaron; un holdout post-verde fijó single-sample rebuild. `3cd0209b1dd1d01cd2ce949b9a17b0e229411274` añadió el fence stale/current: un índice viejo puede solicitar rebuild, pero un caller viejo frente a un índice más nuevo falla cerrado con candidatos vacíos. `efb5e736612f1565a83c4ae860bba2da5df3bd5d` fijó ese contrato en GameTest. La lane prepared de ese hardening reabrió de forma independiente A9/S09, ya reparado sin cambiar S13.

**S14 cerrado:** `docs/sprints/S14-binding-state-ownership.md` extrajo a `collision.internal.AnatomyBindingState` el ownership único del binding vivo: provider, descriptor causal opcional, generación local monotónica, quarantine de la generación actual y capture guard reentrante. El causal rebind dejó de pasar por un binding descriptorless transitorio y ahora instala provider+descriptor+generation atómicamente. Baseline rojo `5019fd7f7116c0c79291556fe41a8d70f2c95941`, run `34696527544`, job `103560853344`: **386 tests, 383 verdes / 3 rojos**, incluido `samples=3` en causal rebind. Tras `f8693c1d17ac0b73b95aeec74e5b2a57490fe9e1`, la cadena integrada pasa ordinary **386/386** y la fuente final con `d048740...` pasa cliente real + prepared **2/2** en `34708981488`. La revisión final no encontró un corte adicional de producción S14.

**Cierre arquitectónico histórico G2:** transport, gravity, contact state, spatial membership y live binding state tienen owners únicos. `AnatomyMovement` queda como orquestador/query y retiene endpoint/root history porque esa frontera todavía no es acíclica: `GeometryProvider.CausalEndpoint` referencia `AnatomyMovement.RootFrame`. Ese cierre estructural sigue demostrado. **La recertificación adversarial final de FR-053 recierra también el gate global G2.**

**Salida de recierre cumplida:** física material Q2 correcta y acotada en server, con `Entity.push` vanilla preservado exactamente una vez para moving platforms y sin collider anatómico para parejas no elegibles; prediction/reconciliación bajo latencia sigue perteneciendo a G4.

### G3 — catálogo, engines generales y lifecycle

**Estado:** **CERRADO**. Las doce tareas están completadas y S24 cerró adversarialmente lifecycle, UNAVAILABLE/recovery, orden live y ownership root/endpoint.

**Requisitos:** FR-014..041, FR-080..082, FR-089..092; NFR-003..013, NFR-015..018, NFR-026..029, NFR-032, NFR-036.

1. [x] sustituir `WorldAnatomyCatalog` acoplado a perfiles legacy por catálogo/binding canónico;
2. [x] preparar/serializar una vez por revisión y reutilizar bundle por receptor;
3. [x] consolidar `ModelPart` GeometryEngine;
4. [x] consolidar engines de pose vanilla y engine general de `AnimationDefinition`;
5. [x] convertir Citadel/Alex a engine/pose-program reusable;
6. [x] añadir `RootTransformProvider` genérico y fixture externo;
7. [x] mantener `DisplayRig` como SPI hasta target real;
8. [x] scanner/coverage FULL/SAFE_PARTIAL/EXCLUDED/UNRESOLVED;
9. [x] reload/tracking/unload/rebind/dimension/reconnect/reutilización de identidad;
10. [x] unsupported states publican unavailable y recuperan sin freeze;
11. [x] lifecycle/order sobre runtime y packets reales;
12. [x] terminar separación de `collision.internal` cuando fronteras sean estables.

**S15 cerrado:** G3 tarea 2/NFR-010 queda cerrada. `WorldAnatomyCatalog` publica `Snapshot + PreparedBundle` atómicamente y la ruta live reutiliza packets preparados por `epoch + revision`. Evidencia final `e52766a71cf66c4157d31b8884d901b22d4de7a8`, run `34747160506`, job `103697083788`: focal **8/8 S15** (+ sentinel) y ordinary **394/394**; artifact `10313754760`, SHA-256 `5a3a20f13c7f24726cee3ed6af5baa3a1f3f63c4c6b5347c69c5063dfba42f57`. Revisión post-verde sin cambios de producción. G3 sigue abierto para tareas 1 y 3-12.

**S16 cerrado tras reapertura adversarial:** el rojo post-cierre `e3e49ac2...` demostró que un selector variant del bridge eludía validación integral. `5cff913349a4fc64921a81e0d8236e5aae235ac9` valida todos los bindings del candidato antes de resolver el selector runtime y conserva atómicamente snapshot+bundle ante rechazo. La campaña final `eabd6ef31ee8f6ed4a7e4fffa873171339db508f` añadió conflicto canonical↔legacy, round-trip variant y rechazo wire completo con restauración client-style; quedó verde sin otro cambio productivo. Evidencia final: ordinary `34753730671` **402/402** y focal `34753730753` **9/9**. G3 tarea 1 queda cerrada.

**S17 cerrado y revalidado retroactivamente:** G3 tarea 3 sigue cerrada. La auditoría `docs/sprints/RETRO-S00-S18-adversarial-observability-audit.md` demostró que el oracle original permitía omitir un cubo volumétrico real. Se añadió un oracle original-tree independiente y bidireccional más snapshots SVG; producción normal volvió a quedar verde y la misma mutación quedó muerta con `missing=[root/body/cube_0]`. No fue necesario cambiar producción S17.

**S18 cerrado tras reapertura adversarial:** G3 tarea 4 queda completada. La reparación final conserva `SourcePose` exacto para representantes Euler/escalas con signo, reproduce la aplicación secuencial de targets y bordes Mojang, mantiene programs revision-local sin registry global, y expresa clocks/amplitudes autoritativos con las truncaciones de milisegundos de `applyWalk` y `AnimationState`. El último cambio productivo es `3ff54a708e0c0ac93f81dc0f26d4b6be13e44897`. Sobre él, los **17 workflows** de la campaña final terminaron verdes y no existe ningún run fallido; la lane focal `34876662857` pasó common authority, compiler boundary, Euler representative, non-unit rest scale y ambos signed-scale holdouts. El regression test posterior `6a72c9f...` dejó también verde el build `34876732944`. La segunda lectura final no produjo cambios de producción y el compare hasta `4afcc19...` contiene sólo test/documentación/CI posteriores. Detalle completo en `docs/sprints/S18-vanilla-pose-keyframe-engine.md` y `VALIDATION.md`.

**S20 cerrado adversarialmente:** G3 tarea 5 queda completada. `scalebrews:citadel_program` queda aceptado como pose-program reusable y data-backed para Citadel/Alex sin reintroducir Java nominal por especie ni dependencia hard de Alex/Citadel. La campaña independiente cerró atomicidad de pose channels, oracle de modelo real, boundedness/allocation/CPU, catálogo v6, materialización canónica y ejecución runtime con mutation-kill. Evidencia de cierre: `s20-adversarial-canonical-binding` run `35073158624` verde; `s20-adversarial-canonical-runtime` run `35079377725` verde con mutation kill; condition-budget `35070996485` verde; real-model `35014694555` verde; pose-channel atomicity `35012624020` verde; ordinary build `35081094083` verde. Detalle en `docs/sprints/S20-adversarial-closeout.md`.

**S21 cerrado adversarialmente:** G3 tarea 6 queda completada. El root DTO/registry, binding, wire, availability, gravedad independiente, compatibilidad exacta, same-tick, rebind, teleport y teardown quedaron convergidos; el último RED real fue removal inmediato. `c481194bb6156906768c12c499fc6ad8650a6595` añadió el fence `entity.isRemoved()` en `AnatomyRuntime.acceptsIntervalIdentity(...)`. El adversario probó su necesidad en run `35105998877`: baseline job `104827240871` verde y mutation-kill job `104827781422` verde tras eliminar sólo ese fence; ordinary del mismo snapshot `35105998939`, job `104827241696`, también verde. La segunda lectura PREPARATION/HOT_TICK y root/joint cache no produjo cambios de producto. Detalle en `docs/sprints/S21-root-transform-provider.md` y `docs/sprints/S21-adversarial-model.md`.

**S23 cerrado adversarialmente:** G3 tarea 7 queda completada sin introducir un target ficticio en producción. El proof implementer demuestra que una familia sintética atraviesa `GeometryEngine -> ModelGeometry -> ConvexBox`; el cierre independiente añade un holdout metamórfico de opacidad entre familias y dos mutation-kills que impiden tanto branch físico por `ModelGeometry.source()` como hardcode nominal de `DisplayRig`/display-composite en producción. Evidencia final adversarial: run `35104624130` sobre `2ffeb627bc9b397b417393eba6454c49a2a714e9`, jobs `104822510244`, `104823016011` y `104823015908`, todos verdes. Detalle en `docs/sprints/S23-display-rig-spi-proof.md` y `VALIDATION.md`.

**S22 cerrado adversarialmente:** G3 tarea 8 queda completada. La reparación productiva final `50296a6792f523e57fcd8daa71ba6f598254a6e6` mantiene independientes completeness y provenance. La pasada final adversarial añadió un control positivo de artifact canónico y dos mutantes opuestos: eliminar el fence de provenance y eliminar la emisión del token legítimo. Run `35328441530`: baseline job `105546923380` verde; bypass mutant job `105547301841` muerto; issuance mutant job `105547301819` muerto. Build `35328441560` verde. El compare desde `50296a6` no muestra cambios posteriores en `CollisionCoverageDiscovery` ni `CollisionCoverageScanner`. Detalle en `docs/sprints/S22-adversarial-model.md`.

**S24 cerrado adversarialmente:** G3 tareas 9-12 quedan completadas. El RED de tracking-authority revival se reparó en `256ab44...` + `99969cc...` y la campaña final añadió mutation adequacy para lectura/adquisición, dimensión, reconnect, `UNAVAILABLE → recovery`, orden same-window y ownership root/endpoint. Run de tracking-read `35333686648`: baseline + dos mutantes verdes. Run de UNAVAILABLE `35335621883`: baseline + transition/recovery mutants verdes. Run de late tracking/order `35336440655`: client real + owner baseline + ordering mutant verdes. Ownership boundary `35333447075`: baseline + duplicate-root/endpoint mutants verdes. Ordinary final `35336440635`, job `105572251864`: **442/442 required GameTests passed**. Cierre completo: `docs/sprints/S24-adversarial-closeout.md`.

**Prioridad vigente:** **G3 está cerrado**. El siguiente gate canónico es **G4 — red, prediction, reconciliación y presentación causal**.

**Salida:** catálogo general reproducible, extensible y con lifecycle transaccional.

### G4 — red, prediction, reconciliación y presentación causal

**Estado:** **ABIERTO / siguiente gate**.

**Requisitos:** FR-077..088; NFR-001..018, NFR-026..031.

1. [ ] ledger/receipts/references sobre lifecycle real;
2. [ ] prediction sólo para player/controlled vehicle local;
3. [ ] observer path sin carry local;
4. [ ] reconciliación sin double-apply ni drift;
5. [ ] presentación `CURRENT_ENDPOINT` → intervalo certificado donde Q2 lo requiera;
6. [ ] residual visual/camera en única capa `client.collision.presentation`;
7. [ ] dedicated `allow-flight=false` 0/100/200 ms y late tracking/reconnect.

**S25 adversarial baseline RED / G4.1:** receipts server-side ya existen y están bounded, pero todavía no hay una ruta C2S anatómica de movement reference. El único receiver C2S de movimiento es `PlatformMovePayload` legacy y sale inmediatamente bajo `AnatomyApi.ownsSharedPhysics(body)`; `PlatformMovementReference.resolve(...)` tampoco rebasa anatomy. Presence gate `s25-adversarial-reference-presence` run **`35337131202`**, job **`105574448871`**: RED esperado con `no non-legacy anatomy/collision C2S receiver is registered`. Ordinary del mismo snapshot **`35337131192`**, job **`105574448591`**: **442/442 required GameTests**, build verde. Threat model: `docs/sprints/S25-g4-reference-adversarial-model.md`.

**Prioridad G4 vigente:** **G4.1 / S25 sigue ABIERTO, pero el RED productivo original de identidad reference↔receipt ya está reparado con protocolo v2 server-issued.** El servidor publica `AnatomyTransportReceiptPayload` con un `receiptSequence` exacto y el cliente productivo devuelve únicamente `AnatomyMoveReferenceV2Payload(vehicle, receiptSequence)`; `localTransportSequence` queda sólo como guard interno cliente y no cruza wire. El claim servidor exige igualdad exacta de receipt sequence más recipient/body/lifecycle, sin fuzzy/nearest matching. Authority/schema sigue verde en run `36129090393`, incluido el mutante que intenta subir secuencia local cliente; receipt publication sin listener queda verde en `36129522915`. Ordinary sobre el oracle v2, run `36129628428`, job `108053654651`: **449/449 required GameTests**, build verde. Dedicated con v2 consume receipts reales: player RTT 0/100/200 ms registra respectivamente `35/24/22` references consumidas y cero correcciones; controlled boat RTT=0 registra `35` consumidas. El RED dedicated restante ocurre antes de la primera v2 del boat: dos `ClientboundMoveVehiclePacket` pertenecen al handshake de montaje/setup, incluido un packet cliente transitorio `(0,-0.12,0)`; con trace completo no aparecen nuevas correcciones después de iniciarse la secuencia estable `v2 reference → ServerboundMoveVehiclePacket`. Owner-v2 queda recertificado en run `36130245780`: baseline `108055602568` y mutantes tracking `108056028215`, replay `108056028216` y fabricated-sequence `108056028268`, todos verdes; **no se inicia G4.2** hasta cerrar esa barrera de setup y completar player + controlled boat 0/100/200 ms sobre trace limpio.

**Salida:** multiplayer autoritativo y prediction estable.

### G5 — categorías especiales, placement, interacción y retirada del motor legacy

**Requisitos:** FR-007..013, FR-053..071, FR-089..093; NFR-033..035.

1. [ ] portar boat/raft, off-rail minecart, item y falling-block semantics;
2. [ ] verificar agua, rail, despawn, hardening, placement y anvil una vez;
3. [ ] portar sneak edge y jump release;
4. [ ] portar raycast/placement con permisos/inventario/footprint;
5. [ ] demostrar no regresión de fall/exhaustion/stats/Growth landing;
6. [ ] preservar la ruta física/interacción Minecraft/`main` para parejas que no califican como moving platform según FR-053/FR-093; una pareja elegible puede sustituir bloqueo por anatomía, pero conserva el `push` vanilla exactamente una vez;
7. [ ] eliminar `PlatformPhysics`, `PlatformGeometry`, `PlatformState`, networking/camera/visual carry legacy, `automatic_top` y recursos/runtime sólo cuando sus equivalentes estén verdes;
8. [ ] conservar únicamente decoder legacy surface si FR-023 sigue justificándolo;
9. [ ] comprobar que ningún mixin/helper bifurca entre dos motores físicos ni crea un segundo owner de interacción.

**Salida:** un solo motor físico, sin convertir entidades no elegibles en paredes ni borrar el push vanilla de una moving platform.

### G6 — cobertura completa de Minecraft general

**Requisitos:** FR-016, FR-024..032, FR-038..040, FR-089..092; NFR-019..024, NFR-028..032.

1. [ ] scanner sobre todos los `LivingEntity` Minecraft 26.2;
2. [ ] agrupar por engines/variant, no lista manual;
3. [ ] completar familias necesarias;
4. [ ] mantener excepciones técnicas explícitas y tests negativos;
5. [ ] `UNRESOLVED=0` y ordinarios no excluidos FULL;
6. [ ] catálogo reproducible con hashes/versions.

### G7 — migración de Clinging Reoriented

**Requisitos:** FR-001..004, FR-044..046, FR-072..085, FR-094; NFR-021..024, NFR-030, NFR-033..035.

1. [ ] compilar consumer contra API final;
2. [ ] migrar preflight/raycast/contact/gravity frame;
3. [ ] registrar/adaptar las superficies materiales válidas de Scale al SPI público `LandingSurfaceProvider`/`LandingSurfaces` de Clinging, usando identidad/revisión/normal/sweep de Scale y sin declarar superficies para parejas no elegibles;
4. [ ] preservar Space/charge/Reorientation/Elytra/efectos/persistencia/camera y dejar en Clinging la decisión de giro/reposicionamiento al aterrizar;
5. [ ] borrar AABB selection/moving surfaces/carry/references/reconciliation duplicados del consumer;
6. [ ] transiciones cardinales y bloqueo;
7. [ ] network/latency con consumer real.

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

Tampoco se elimina ni revierte código de `main` ajeno a entity collisions por el mero hecho de no aparecer en este plan. Toda limpieza destructiva debe distinguir deuda del subsistema de evolución productiva mainline y volver a contrastar el `main` vigente antes de borrar.

## 6. Método iterativo y convergencia

### Revisión del plan

P1-P9 consolidaron dependencias FR/NFR, separaron core/VP26, colocaron Q2 antes de familias FULL, separaron lifecycle/red, retuvieron temporalmente el legacy, ejecutaron reordenamiento real, eliminaron hardcodes grizzly y convergieron G0.

G1 se ejecutó en S01-S04. Dos campañas adversariales reabrieron implementación, no requisitos: primero backend público/codec permisivo; después ciclo `api ↔ runtime`. Ambas fueron reparadas sin cambiar orden de gates. La evidencia final 268/268 + client/dedicated cerró G1 de nuevo.

S05 abrió G2 con una tesis aislada: eliminar el fallback global del broadphase sin adelantar causalidad continua. El kernel y su integración viva cerraron con 277/277 y una pasada estructural sin cambios.

S06 fijó identidad/continuidad material y exactly-once; S07 conectó esos intervalos al dispatcher live y cerró CCD/fallo local; S08 añadió carry anclado continuo y cadenas derivadas. La reapertura tardía de S08 añadió dos restricciones importantes sin cambiar requisitos: las entidades vanilla deben barrerse a lo largo del `BodyPath`, y los soportes anatómicos estacionarios fuera del batch siguen siendo obstáculos mediante el broadphase material aunque su AABB vanilla esté suprimida. El cierre vigente de S08 es `4abe57cd...`.

S09 cerró las tareas físicas 5, 7 y 8 y soportó varias reaperturas adversariales posteriores: aliases/recovery, screening temporal preparado, tolerancia numérica a coordenadas mundiales grandes, divergencia entre la banda ULP de CCD y los certificados temporales, scheduling causal, doble carga del budget durante `q` y finalmente el probe insuficiente del primer candidato midpoint-positive. Ninguna reapertura elevó el budget global ni cambió requisitos. El último cambio productivo es `d048740...`, validado ordinary + cliente original + prepared antes del push.

S10 extrajo el ledger/cursor de transporte pasivo a `collision.runtime.TransportLedger`, con un consumidor real (`AuthorityPoseTracker`) y sin mover la aplicación física del carry. La revisión final retiró el wrapper de cursor que quedó sin consumidor.

S11 reconcilió la autoridad de gravedad en `integration.gravity.GravityFrames`. Su primera clausura se reabrió al descubrir que el seam de tests no podía representar explícitamente DOWN bajo un proveedor no-DOWN; se separaron override y cleanup y se añadió el nuevo owner a la lane prepared. El cierre vigente es `52770bc...`, ordinary 365/365 + prepared 2/2.

S12 extrajo contacto, sequence, anchor, surface y suspension a `AnatomyContactState`, sin trasladar decisiones físicas. La revisión adversarial posterior al primer verde añadió rebind/overlap y lifecycle cross-dimension; este último reabrió S12 y produjo la reparación localizada `7142207...`. S12 permanece cerrado en la cadena integrada final.

S13 extrajo el índice espacial per-level/tick a `AnatomySpatialIndex`, eliminó `FrameStamp/frames` muertos y fijó locality, overflow, lifecycle, single-sample rebuild y stale-caller fail-closed. `3cd0209...` es su último cambio productivo propio. Su prepared post-hardening reabrió de forma independiente A9/S09; tras las reparaciones S09, la frontera espacial sigue cerrada.

S14 extrajo el binding vivo a `AnatomyBindingState`: provider, descriptor, generación local, quarantine y capture guard tienen un único owner. El rojo inicial probó además que el causal rebind pasaba transitoriamente por un binding descriptorless y muestreaba de más; `f8693c1...` convirtió esa transición en atómica. La revisión final cero-cambios demostró que endpoint/root/query no puede separarse todavía sin callback inverso o sin adelantar G3.

**Reapertura FR-053:** la precisión posterior de requisitos separó bloqueo anatómico y `Entity.push`. El primer oracle verde (`34878390388`) no atravesaba `AnatomyApi.READY`; el segundo rojo (`34884834050`) no poseía binding canónico y falló antes de crear contacto. El tercer oracle añadió ambos precondicionantes y demostró causalmente en `34885201453` que el contacto material cancela por completo el impulso vanilla. G2 no necesita rehacer S05-S14, pero no puede volver a declararse cerrado hasta reparar esa frontera y pasar además el holdout no-wall para parejas fuera de elegibilidad.

**Convergencia estructural G2:** tras S14 no queda otra frontera de ownership Q2 que pueda separarse de forma acíclica. Task 1 se satisface porque state/index/contact/transport tienen owners separados y `AnatomyMovement` queda como query/orquestador; task 9 se satisface precisamente dejando dentro de `collision.internal` los tipos que aún no están desacoplados. `FRAME_SERIALS` y root history quedan trazados a G3, no como deuda oculta de G2. Esta conclusión arquitectónica no sustituye el recierre funcional pendiente de FR-053.

### Revisión del código

El inventario y revisiones destructivas de G0 fijaron qué conservar/eliminar. G1 dejó fronteras públicas/data estables. S05 extrajo `MaterialBroadphase`; S06-S09 establecieron fronteras causales y físicas reales alrededor de `MaterialIntervalRuntime`, `MaterialPhysicsRuntime`, `AnchoredTransportPlanner`, `TemporalResponse`, `AnatomySeparation` y el dispatcher. S10 extrajo `TransportLedger`; S11 retiró la doble autoridad de gravedad; S12 extrajo el estado retenido de contacto a `AnatomyContactState`; S13 extrajo membership/index bounded a `AnatomySpatialIndex`; S14 extrajo live binding state a `AnatomyBindingState`.

La partición G2 de `AnatomyMovement` se considera completa **para el alcance estructural Q2**. Históricamente conservó activación, endpoint serials, root history, sweep metrics y la orquestación/query live porque endpoint/root todavía formaban una unidad causal y su separación pertenecía a G3. Ese siguiente corte ya existe en el árbol actual: `collision.runtime.RootFrame` + `RootFrameLedger` poseen provenance root y `collision.internal.AnatomyEndpointLedger` posee serial/snapshot/invalidated state; `AnatomyMovement` conserva activación, sweep metrics y decisiones/orquestación/query live. Las métricas y activación aisladas siguen sin justificar una extracción propia.

Cualquier cambio de requisitos o implementación vuelve a ejecutar una pasada completa; si esa pasada cambia plan o clasificación, se repite hasta obtener una pasada sin cambios.
