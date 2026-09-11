# S00 — Foundation Audit de Entity Collisions

Estado: **CERRADO**. Registro de ejecución no normativo del prerrequisito definido en `ENTITY_COLLISIONS_FOUNDATION_AUDIT.md`.

Este documento explica qué se auditó, qué falló, qué se reparó y por qué cada pieza queda clasificada como `TRUSTED`, `REWORK` o `REPLACE`. Los requisitos siguen viviendo exclusivamente en `ENTITY_COLLISIONS_REQUIREMENTS.md`, la arquitectura en `ENTITY_COLLISIONS.md`, el estado global en `ENTITY_COLLISIONS_PLAN.md` y la evidencia ejecutada en `VALIDATION.md`.

## 1. Scope y orden de lectura

S00 se ejecutó antes de G1 y sin iniciar S01. El orden utilizado fue el exigido por `AGENTS.md`:

1. `AGENTS.md`;
2. `ENTITY_COLLISIONS_REQUIREMENTS.md`;
3. `ENTITY_COLLISIONS.md`;
4. `ENTITY_COLLISIONS_PLAN.md`;
5. `ENTITY_COLLISIONS_FOUNDATION_AUDIT.md`;
6. `ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`;
7. `VALIDATION.md`;
8. `TODO.md` y `CONTRIBUTING.md` como contexto operativo.

El modelo adversarial inicial se congeló **antes** de leer los tests históricos. Sólo después se inspeccionaron los tests existentes para clasificarlos y localizar huecos.

Incluido en la auditoría: geometría convexa, SAT, CCD, movimiento temporal, response/separation, soporte/carry, jerarquía de joints, multicontacto, causalidad, revision/epoch/generations, exact-once, histories/receipts, lifecycle/reload/rebind/reconnect, autoridad cliente/servidor, prediction boundary, fail-closed, determinismo, seis gravedades, SCALE y el motor `platform` legacy que sigue en paralelo durante la migración.

Exclusiones deliberadas: implementar G1+, conectar el backend vivo de `MaterialEventDispatcher` que pertenece a G2, sustituir el catálogo general de G3, terminar prediction/reconciliation de G4 o retirar el motor legacy de G5. Esas piezas se clasifican, no se adelantan.

## 2. Freeze clean-room

Artefactos de razonamiento inicial:

- modelo clean-room SHA-256: `6eeb1c2c703f30107e4daf15902253e2848e785f470192cc1d003284025e6eb2`;
- holdouts sellados SHA-256: `97be19ed30d27738a93edfae2b096c6ea24498d03f1b679acb7fe5ddb7f25e10`.

La revisión del modelo fue iterativa. CR1 corrigió huecos de causalidad y budgets; CR2 corrigió oráculos metamórficos demasiado fuertes; CR3 repitió el conjunto completo y produjo cero cambios. Dos correcciones importantes del propio modelo fueron: una rotación arbitraria no preserva una AABB como representación equivalente, y un muestreo denso puede encontrar contraejemplos de CCD pero no demostrar continuidad por sí solo.

### 2.1 Invariantes I01–I22

| ID | Propiedad observable derivada antes de leer tests históricos |
|---|---|
| I01 | Toda geometría/DTO se valida finita, acotada, no degenerada y acíclica antes de publicación; nunca existe publicación parcial. |
| I02 | Local→world→local, normal de cara, plano y bounds describen el mismo transform válido; un transform no soportado se rechaza, no se aproxima silenciosamente. |
| I03 | Jerarquía, pivots, gaps e identidad de pieza conservan causalidad; root y joints son dimensiones independientes. |
| I04 | Broadphase debe ser conservador, local y acotado; una optimización nunca puede ocultar una pieza. |
| I05 | El sweep cubre el intervalo completo, tangencias e interior temporal; agotamiento o incertidumbre nunca significan `CLEAR`. |
| I06 | Response no introduce penetración normal, conserva movimiento tangencial válido y limita recovery a la pareja/budget auditado. |
| I07 | Soporte usa `normal·antiGravity >= cos(45°)` con normal física, incluidas las seis gravedades y la frontera angular. |
| I08 | Anchor material conserva identidad y punto local; carry sigue trayectoria certificada, colisiona y nunca acumula deuda tras una obstrucción. |
| I09 | Cada contribución causal real se aplica como máximo una vez y exactamente una vez si no falla; el tick no basta como identidad. |
| I10 | No existen auto-soporte/ciclos, reentrancia oculta ni orden causal dependiente de hash; cadenas se procesan base→dependiente. |
| I11 | Un receipt prueba transporte realmente aplicado, no lo reaplica, fabrica ni permite replay sobre otra generación. |
| I12 | Histories, receipts, caches y colas tienen TTL/caps explícitos; una fence saturada vive todo su TTL. |
| I13 | Dimension, epoch, revision, UUID, network id, binding/tracking generation y serial se validan antes de materializar estado. |
| I14 | Unavailable/lifecycle invalida estado dependiente; reload inválido conserva exactamente el último snapshot válido, sin mezcla. |
| I15 | Geometría, pose física, contacto y carry son autoridad de servidor; common no depende de clases cliente y C2S no redefine física. |
| I16 | Sólo el owner local puede predecir; observer consume confirmado; late tracking/rebase no aplica carry pasado ni duplica contribuciones. |
| I17 | Presentación/cámara no cambia estado físico; discontinuidades resetean interpolación/presentación dependiente. |
| I18 | Existe un único owner físico por pareja/periodo; `BINDING` no cae al motor legacy y vanilla no relacionado sigue intacto. |
| I19 | Igual input, identidad y orden causal produce igual resultado sin depender de registration/hash/render order. |
| I20 | Ratio y SCALE usan dimensiones físicas efectivas, incluida la frontera 0,85 y la invalidación tras cambio de escala. |
| I21 | Passenger/vehicle no duplica carry; carry pasivo no genera walk/fall/stats; agua/rail/lifecycle tienen prioridad explícita. |
| I22 | Un preflight fallido no muta física/mundo/inventario/gravity; el owner de gravedad es único e idempotente. |

### 2.2 Familias de ataque A01–A16

A01 cubre null/NaN/infinito/extremos/ciclos/caps; A02 cara/arista/vértice, separación, tangencia, penetración y referencias analíticas; A03 tiempos 0/1/interior y combinaciones root/joint/scale; A04 seis gravedades y 44,9°/45°/45,1°; A05 piezas/gaps/corners/multicontacto/permutaciones/ties; A06 jerarquías profundas/pivots/root+joints; A07 duplicate/loss/reorder/replay y varias contribuciones en el mismo tick; A08 failure injection/reentrancia; A09 reuse y contradicción de ejes de identidad; A10 TTL/cap/overflow/prune/retry; A11 mode/lifecycle/reload/late join/unload/reconnect/teleport; A12 cliente malicioso, owner/observer y conflictos; A13 managed pair frente a terceros/legacy/passengers; A14 ratio/scale/water/rail/item/falling/landing; A15 geometría y cargas patológicas/budgets; A16 presentación/preflight/gravity owner.

## 3. Holdouts sellados

Los seis holdouts se reservaron antes de inspeccionar la suite histórica.

| Holdout | Ataque | Evidencia final | Estado |
|---|---|---|---|
| H01 | Root vuelve al endpoint original mientras joint contrarota; endpoints libres pero colisión sólo en el interior. | `S00TemporalTests.holdoutH01RootReturnAndCounterJointCannotHideInteriorCollision`. | PASS |
| H02 | Saturar receipts/history, cruzar frontera TTL, podar y reintentar identidad. | `S00ReceiptTests.holdoutH02SaturationSurvivesBoundaryAndRejectsExpiredReplay`. | PASS |
| H03 | Reutilización de identidad tras cambio de generación y replay del estado viejo. | `S00CausalTests.holdoutH03IdentityReuseCannotRestorePriorGeneration`. | PASS |
| H04 | Cadena A→B→C, callback reentrante y fallo durante cleanup. | `S00DispatcherTests.holdoutH04ScopedChainCallbackFailure` más tests de cleanup/reentry. El kernel está cerrado; el backend vivo del dispatcher no existe todavía y su integración sigue asignada a G2. | PASS en responsabilidad actual; integración G2 diferida |
| H05 | TOI/normal empatados, tangencia, permutation order y `+0/-0`. | `S00TemporalTests.holdoutH05EqualContactsIgnoreMapOrderAndSignedZero`, 32 permutaciones deterministas. | PASS |
| H06 | Catálogo válido → candidato casi válido que falla al final → replay/fragmentos; debe sobrevivir exactamente el snapshot previo. | `S00CausalTests.holdoutH06InvalidFinalFragmentPreservesAcceptedCatalog`. | PASS |

La segunda inspección hostil añadió ataques no sellados originalmente. Entre ellos quedaron permanentes los tests de rebind de cuarentena y reemplazo de soporte con UUID reutilizado.

## 4. Inventario y clasificación final

`TRUSTED` significa reutilizable **dentro de su responsabilidad y dominio actual**. Si G1+ amplía ese dominio, vuelve a requerir evidencia para el nuevo alcance. `REWORK`/`REPLACE` no bloquea S01 cuando precisamente el gate indicado es dueño de ese cambio y el componente no se usa como cimiento confiable para saltárselo.

### 4.1 Common collision core

| Componente | Responsabilidad actual | Estado final | Evidencia / destino |
|---|---|---|---|
| `api.AnatomyApi` | façade pública actual | REWORK | G1 debe romper `api↔internal`, estabilizar DTO/capabilities y crear SPI explícito. |
| `api.AnatomyMode` | DISABLED/BINDING/READY | TRUSTED | Semántica fail-closed coherente con FR-002/003; session tests. |
| `api.GravityFrame` | basis cardinal, tangent/vertical/support | TRUSTED | Seis gravedades, frontera cos45 y normal finita cubiertas por S00. |
| `api.SurfaceContact` | identidad geométrica de contacto parcial | REWORK | Es válida como valor local, pero G1/G4 debe cerrar la identidad wire/canonical completa y generations. |
| `geometry.AnatomyFilter` | include/exclude y límites | TRUSTED | Validación/caps; filtros y jerarquía cubiertos. |
| `geometry.ConvexBox` | convexidad affine, SAT, raycast, separación, coordenadas | TRUSTED | Reparada clausura numérica; propiedades analíticas, reflexión y subnormales verdes. |
| `geometry.ModelGeometry` | jerarquía/piezas/transforms del modelo | TRUSTED | Ciclos/orphans/caps/degeneración/unknown joints rechazados; G3 puede envolverlo en engines sin asumir más dominio. |
| `internal.AnatomyCatalogPayload` | fragmentos wire de catálogo | REWORK | G3/G4 consolidan catálogo/protocolo y bundle reutilizable. |
| `internal.AnatomyCatalogTransfer` | recepción atómica de catálogo | REWORK | Atomicidad/revision autoritativa reparadas y probadas; ownership final pertenece G3/G4. |
| `internal.AnatomyClientSession` | scope conexión/nivel | REWORK | Invariantes de reset probados; session/prediction final es G4. |
| `internal.AnatomyCodecs` | codecs actuales | REWORK | G1 debe versionar codecs/capabilities canónicos. |
| `internal.AnatomyContactInbox` | watermark/pending/TTL cliente | REWORK | Replay/TTL/generation probados; ledger final pertenece G4. |
| `internal.AnatomyContactOrdering` | orden packet-only por generation/sequence | TRUSTED | Independiente de hash y sin ownership físico. |
| `internal.AnatomyContactPayload` | DTO wire de contacto | REWORK | G4 debe cerrar tracking/lifecycle/receipts final. |
| `internal.AnatomyDefinition` | data model actual de binding/policy | REWORK | G1 crea binding/policy canónicos sin `PlatformDefinition.Surface`. |
| `internal.AnatomyFrameHistory` | watermark de frames causales | TRUSTED | Compara todos los ejes de identidad y serial; replay/rebind/overflow cubiertos. |
| `internal.AnatomyMovement` | monolito Q1: registro/index/query/contact/carry | REWORK | Bugs fundacionales reparados; G2 debe dividirlo, eliminar scan oversized y conectar Q2 material. |
| `internal.AnatomyNetworking` | wiring de epoch/revision/packets | REWORK | G3/G4 poseen protocolo/lifecycle/prediction final. |
| `internal.AnatomyPoseEligibility` | adapters de estados soportados | REWORK | G3 debe generalizar engines/coverage y eliminar conocimiento específico restante. |
| `internal.AnatomyPoseHistory` | historial/interpolación causal de pose | TRUSTED | Rebind/replay/discontinuity y canales v3 cubiertos. |
| `internal.AnatomyPosePayload` | DTO wire de pose | REWORK | G4 estabiliza publicación/prediction/reconciliation final. |
| `internal.AnatomyRuntime` | catálogo vivo, scans, tracking/publicación | REWORK | G3 debe eliminar scans mundiales y consolidar lifecycle/runtime real. |
| `internal.AnatomySession` | ownership de modo/session | REWORK | Fail-closed es válido; G1/G3 desacoplan backend y binding. |
| `internal.AnatomyTransportReceipts` | ledger provisional de provenance | REWORK | Tick/surface/delta/TTL/saturation probados; tracking-generation y reconciliación completos son G4. |
| `internal.AuthorityPoseTracker` | clock/locomotion server y canales actuales | REWORK | Clock/transport cursor seguros; conocimiento Java de familias/canales se generaliza en G1/G3. |
| `internal.GeometryCatalog` | validación atómica de catálogo geométrico | TRUSTED | Candidate completo se valida antes del swap, orden TreeMap y caps explícitos. |
| `internal.GeometryProvider` | snapshot/causal endpoint/motion contract interno | TRUSTED | No inventa history estática; DTOs y clocks fail-closed; Q2 consume el intervalo explícito. |
| `internal.GravityFrames` | owner único del adapter de gravedad | TRUSTED | Ownership/idempotencia y seis frames cubiertos. |
| `internal.HierarchyMotion` | evaluación temporal root+joints y bounds | TRUSTED | Tiny rotations, unknown joints, shear soportado/no soportado y H01 verdes. |
| `internal.MaterialEventDispatcher` | scheduler causal síncrono | TRUSTED | Reentry/abort/queue/ancestry/budgets/batches/invalid derivation + H04 + mutaciones. Backend vivo se integra en G2. |
| `internal.ModelGeometryProvider` | bridge Q1 modelo+pose→frames | REWORK | Sus invariantes están probados, pero G3 lo sustituye/consolida dentro de Geometry/Pose engines. |
| `internal.WorldAnatomyCatalog` | carga resource/registry legacy-aware | REWORK | Swap inválido es atómico; G3 exige catálogo/binding canónico sin perfiles legacy. |
| `physics.AnatomySeparation` | recovery local acotado | TRUSTED | Clip null/no-finito fail-closed, budgets y orden de piezas probados. |
| `physics.BodyPath` | path/certificados relativos | TRUSTED | Normal certificates, interior failure y provider unavailable probados. |
| `physics.ConservativeSweep` | CCD conservador | TRUSTED | TOI analítico, interval interior, invariant planes, tiny rotation y budget exhaustion. |
| `physics.SupportTransport` | contribución pasiva confirmada | TRUSTED | DTO finito/clock/sequence y consumo causal cubiertos. |
| `physics.TemporalResponse` | multicontacto/sliding temporal | TRUSTED | H05, tangencia, ordering y seis gravedades dentro del dominio actual. |
| `pose.PlayerWalkingPose` | evaluator puro player walking | TRUSTED | Comparación original-model en cliente real; pureza/determinismo. |
| `pose.PoseChannels` | política de canales continuos/discretos | TRUSTED | Interpolación no inventa flags; networking/pose history cubiertos. |
| `pose.PoseProvider` | contrato evaluator actual | REWORK | La semántica de input es válida, pero G1/G3 crea `PoseEngine`/registry público canónico. |
| `pose.PoseProviders` | registry estático de providers | REWORK | G1/G3 reemplaza registry/selección y evita que el core dependa de familias Java. |
| `pose.QuadrupedPose` | evaluator puro quadruped | TRUSTED | Comparación original-model y transform tests dentro del dominio actual. |
| `pose.VanillaFamilyPose` | familias vanilla actuales | REWORK | Evidencia amplia de equivalencia existe; G3 debe consolidarlo como engine general/coverage, no lista Java final. |

### 4.2 Client collision

| Componente | Estado final | Evidencia / destino |
|---|---|---|
| `client.collision.network.AnatomyClientNetworking` | REWORK | El observer path y separación de autoridad están probados; G4 posee prediction/reconciliation/presentation y lifecycle de red final. |
| `client.collision.preparation.GeometryExtractor` | TRUSTED | Cliente real exporta cow/player y compara poses; zero-thickness visual se rechaza y su mutación dirigida muere. G3 puede convertirlo en engine sin cambiar esta garantía. |

### 4.3 Motor `platform` legacy paralelo

El legacy no se considera cimiento de la arquitectura nueva. Se conserva únicamente porque aún implementa semánticas de gameplay que no pueden desaparecer durante la migración.

| Componente legacy | Estado final | Destino |
|---|---|---|
| `PlatformBody` | REPLACE | integración/categorías G5 |
| `PlatformConnection` | REPLACE | networking/reconciliation G4/G5 |
| `PlatformDefinition` | REWORK | G1 extrae binding/policy canónicos; sólo decoder legacy puede sobrevivir si FR-023 lo exige |
| `PlatformEligibility` | REPLACE | policy/integration nueva G1/G5 |
| `PlatformGeometry` | REPLACE | motor físico único G5 |
| `PlatformMovePayload` | REPLACE | red nueva G4/G5 |
| `PlatformMovementReference` | REPLACE | receipts/reference nueva G4/G5 |
| `PlatformNetworking` | REPLACE | red nueva G4/G5 |
| `PlatformPayload` | REPLACE | protocolo legacy G4/G5 |
| `PlatformPhysics` | REPLACE | motor físico único G5 |
| `PlatformPlacement` | REPLACE | placement nuevo G5 |
| `PlatformPolicy` | REWORK | policy canónica G1, semántica especial G5 |
| `PlatformState` | REPLACE | ownership/runtime nuevo G5 |
| `Platforms` | REPLACE | façade/motor legacy retirado en G5; no se usa como fallback durante `BINDING` |

**Resultado del inventario:** ninguna dependencia que S01 deba tratar como cimiento queda `UNPROVEN`. Los `REWORK` y `REPLACE` restantes tienen owner explícito G1–G5 y no se promocionan falsamente a `TRUSTED`.

## 5. Fallos encontrados y reparaciones

Todos los fallos se clasificaron antes de modificar producción.

| Familia | Clasificación | Fallo | Reparación / evidencia |
|---|---|---|---|
| Dispatcher D01 | implementación, I09/I10/I12 | abort podía dejar queue y ejecutar trabajo con otro backend | `finally queue.clear`; regresión + mutación `dispatcher-abort-clear`. |
| Dispatcher D02 | implementación, I10 | callback de quarantine podía reentrar y resolver nested work | gate `notifyingQuarantine`; mutación `dispatcher-reentry`. |
| Dispatcher D03 | implementación/diagnóstico | fallo de cleanup sustituía causa primaria y cortaba notificaciones | cleanup completo, causa primaria + suppressed failures. |
| Dispatcher D04 | implementación | `Error` podía dejar pending oculto si se reutilizaba instancia | limpieza incondicional del queue; test fatal failure. |
| Dispatcher batch | implementación, fail-closed | derived orphan podía cuarentenar pero devolver outcomes engañosos y dejar escapar child previo | prevalidación atómica de parents; run rojo 124 y verde posterior; mutación `dispatcher-joint-orphan`. |
| Convex G01/G02 | implementación numérica | finite extremo podía producir normal cero/NaN; AABB/Vec3 no finitos cruzaban comparaciones | numeric domain + finite closure; SAT/subnormal mutations. |
| CCD G03/G04 | implementación física | `acos(abs(float quaternion dot))` redondeaba giro pequeño a 0 y permitía tunneling | cota angular conservadora double por chord ratio; tiny-rotation mutation. |
| Support G05 | implementación | normal infinita/no normalizada podía contar como support | normalización lógica + rechazo no finito; seis gravidades. |
| Recovery G06 | implementación fail-closed | callback clip null/no-finito podía autorizar recovery o explotar | retorno no separado/desplazamiento cero; test adversarial. |
| Temporal history | arquitectura/implementación | `GeometryProvider.motion()` fabricaba intervalo estático desde foto instantánea | default `Optional.empty`; history sólo explícita. |
| Frame/lifecycle | implementación causal | rebind/invalidation durante capture, first-frame failure y local generation reset podían publicar/reusar identidad retirada | capture stamps, quarantine persistente y watermark de registration no rebobinado; mutaciones dirigidas. |
| Client authority | implementación | observer o recursión de carry podía mover root server-owned en cliente | `simulates(body)` en cada recursión; real-client mutation. |
| Catalog revision | implementación causal | transfer aceptaba revision servidor pero snapshot local podía autoincrementar a otra | `replaceAtRevision`; mutation `catalog-authority-revision`. |
| Model/extraction | implementación | piezas degeneradas y quads visuales zero-thickness podían entrar como física | strict volume/bounds; extractor descarta cero espesor; server/client mutations. |
| Hierarchy boundary | implementación | key de joint desconocida podía convertirse silenciosamente en rest pose | rechazo en kernel `HierarchyMotion`; mutation dedicada. |
| Suspension identity | implementación lifecycle | pair quarantine se ligaba a UUID; rebind retenía estado y una instancia nueva podía heredarlo | weak instance key + `registrationGeneration`; run rojo 131 con dos fallos exactos; 2 mutaciones lifecycle muertas. |
| Entorno inicial | entorno/evidencia | checkout local inicial carecía Java 25/conectividad suficiente | no se reclasificó como bug; aceptación final trasladada a GitHub Actions Java 25.0.3. |

No se cambió un requisito normativo para hacer verde una reparación. Los cambios fueron compatibles con los invariantes ya derivados.

## 6. Tests históricos después del freeze clean-room

| Familia histórica | Clasificación | Qué conserva | Limitación explícita |
|---|---|---|---|
| `AnatomyGeometryTests` | legacy regression + property evidence | gravedad, temporal, catálogo, filtros, root/pose y selected model behavior | no sustituye holdouts nuevos ni demuestra todo Q2; bucles repetidos no son automáticamente ticks reales. |
| `AnatomyMaterialEventDispatcherTests` | legacy regression | orden de ingestión y batch/reentry básicos | antes no cubría callback quarantine, cleanup failure, fatal queue leak ni orphan outcomes. |
| `AnatomyNetworkingTests` | legacy regression + acceptance evidence | session scope, pose history, root/joint separation, tracking generation y receipts | no demuestra prediction/reconciliation G4 completo. |
| `AnatomyQueryFrameTests` | property/regression evidence | snapshot actual sin history inventada, TRS/identity del frame | dominio Q1; Q2 usa intervalos materiales. |
| `AnatomyAnimatedSqueezingTests` | legacy regression | suspensión local de trapped pair y provenance de contribuciones | no era una prueba completa del scheduler causal. |
| pruebas cliente de export/pose/live | acceptance evidence | original-model cow/player, 80+80+80 y 640 comparaciones adicionales, authority path | no certifica todas las especies/mods; cobertura general es G6/G7/G8. |
| `S00*Tests` | property/adversarial/holdout evidence | modelo adversarial clean-room y fallos descubiertos | scoped a la responsabilidad actual; integración futura se revalida en su gate. |

No se encontró un test histórico que debiera eliminarse por ser inequívocamente obsolete/duplicate dentro de S00. Los tests con nombres o bucles fáciles de sobreinterpretar se conservan sólo como regresión y no se citan como prueba de propiedades más fuertes.

## 7. Mutación adversarial

La primera campaña de mutaciones se descartó como evidencia porque algunas jobs demostraban fallo al **aplicar** la mutación, no un kill comportamental. Se creó la campaña v2 con identidad exacta de árbol candidato/mutado y un oráculo de fallo específico.

Evidencia final sobre el mismo candidato:

- run `34574727544`, `s00-mutation-v2`: **22/22** jobs verdes; cada mutación compila y el test exacto exigido la mata;
- run `34574727548`, `s00-hierarchy-mutation`: **1/1** mutation de unknown-joint muerta;
- run `34574727576`, `s00-suspension-mutation`: **2/2** mutations lifecycle muertas.

Total final: **25/25 mutaciones dirigidas muertas**. No se cuenta como kill un error de compilación, un fallo de transport o una aserción distinta del oráculo previsto.

Las mutaciones cubren dispatcher reentry/abort/orphan, SAT cutoff/subnormal, certificates/planes, tiny rotation, receipt tick/surface, frame binding generation, model unknown joint/degenerate piece, wrong-thread registration, endpoint serial reuse, first-capture quarantine, lifecycle watermark, authoritative catalog revision, client receipt authority, client carry authority, zero-thickness extraction y las dos garantías nuevas de suspension identity.

## 8. Evidencia de aceptación final

Snapshot de implementación/test exacto archivado por CI:

`68244f2acbebccfd0dec607220726b9ed7e56e8a`

Transport commit usado sólo para materializar ese árbol en Actions:

`a5503f97a7320e416cd253357c6c8f3cb5b002e3`

GitHub Actions run `34574727529`, Java Microsoft 25.0.3:

- materialización verificó exactamente `S00_SOURCE_TREE=68244f2acbebccfd0dec607220726b9ed7e56e8a`;
- Gradle wrapper validado;
- build completo verde;
- **242/242 required server GameTests** verdes;
- cliente real/integrado verde bajo Xvfb/llvmpipe;
- export original-model de cow y player wide/slim;
- 80 comparaciones animadas cow, 80 player wide, 80 player slim y **640 comparaciones vanilla-family adicionales**;
- dedicated/client proof con `allow-flight=false` y soaks **0/100/200 ms RTT, 120 ticks cada uno**;
- `S00_CLIENT_RECEIPT_AUTHORITY PASS`;
- `S00_OBSERVER_AUTHORITY PASS`.

Artefacto de evidencia del run: `S00-34574727529`, digest SHA-256 `eeaf7d9b323a5f14c5cb3819385b216e15ff50f662c194ec8d3622d3ccdbd064`.

Antes de la reparación final de suspension identity, run `34545247231` ejecutó el holdout v2 correctamente y falló **exactamente dos** server tests: rebind conservaba quarantine retirada y replacement con UUID reutilizado la heredaba. El client/integrated/dedicated lane del mismo candidato permaneció verde. Esa evidencia roja se usó para clasificar el bug antes de reparar.

## 9. Limitaciones conocidas y ownership posterior

Estas limitaciones son deliberadamente visibles; ninguna se convierte en “TRUSTED por proximidad”.

- G1: façade/API, canonical binding/policy, codecs/capabilities y registry/engines públicos siguen `REWORK`.
- G2: `AnatomyMovement` sigue monolítico; el oversized broadphase contiene el fallback `seen.addAll(index.bounds().keySet())`; `MaterialEventDispatcher` aún no tiene backend vivo. Son trabajo explícito de G2.
- G3: `AnatomyRuntime` todavía hace scans `level.getAllEntities()`, `WorldAnatomyCatalog` conoce perfiles legacy y providers/pose engines aún no son el catálogo general final.
- G4: receipts, contact/network DTOs, prediction/reconciliation y presentation interval final siguen `REWORK`.
- G5: `platform` mantiene el motor físico legacy y scans globales. Está clasificado `REPLACE`, pero retirarlo ahora perdería semántica vigente y violaría el propio plan.

Ninguna de estas piezas se utiliza para fingir que su gate posterior ya está implementado. S00 certifica los cimientos reutilizables y hace explícito qué debe reescribirse encima de ellos.

## 10. Revisión final iterativa

- R1: inspección hostil de exact-once, identity, lifecycle, authority y determinismo encontró la divergencia de revision del catálogo y se reparó.
- R2: nueva inspección encontró orphan derived outcomes del joint batch; test rojo antes de repair, reparación y mutation.
- R3: nueva inspección encontró unknown joint en `HierarchyMotion`; reparación + mutation.
- R4: inspección lifecycle encontró la hipótesis de UUID-reuse en pair quarantine. Dos primeros fixtures se clasificaron correctamente como **test defects** y se descartaron; el fixture v2 sembró el estado de forma independiente, produjo dos fallos reales y llevó a la reparación instance+generation.
- R5: campaña final exact-tree: build/client/dedicated verdes y 25/25 mutations muertas.
- R6: verificador estático hostil sobre `source.tar` del run final comprobó inventario, client/common split, ausencia de random/wall-clock/TODO en production collision, orden explícito de candidates/sweep, identity axes, known later-gate scans, holdouts H01-H06 y exact source identity. La primera ejecución detectó un error del propio verificador (`entityUuid()` frente al accessor real `entity()`), no del código. Se corrigió y se reinició la revisión.
- R7: pasada completa **45/45** verde.
- R8: repetición completa **45/45**, mismo output y hash del verificador sin cambios: **zero-change review**.

## 11. Checklist de cierre

- [x] S00-01 Releer requisitos y arquitectura fundamentales.
- [x] S00-02 Derivar invariantes sin usar tests históricos como fuente.
- [x] S00-03 Construir y revisar hasta convergencia el modelo adversarial clean-room.
- [x] S00-04 Inventariar primitivas y servicios fundamentales existentes.
- [x] S00-05 Auditar física y geometría temporal.
- [x] S00-06 Auditar causalidad, identidad, revision/epoch y lifecycle.
- [x] S00-07 Auditar fronteras de autoridad cliente/servidor.
- [x] S00-08 Inspeccionar y clasificar tests históricos después del freeze clean-room.
- [x] S00-09 Clasificar componentes como TRUSTED / REWORK / REPLACE / UNPROVEN.
- [x] S00-10 Reparar cimientos bloqueantes para el siguiente sprint.
- [x] S00-11 Ejecutar evidencia adversarial y regresiones aplicables.
- [x] S00-12 Actualizar documentación/evidencia sin duplicar fuentes canónicas.
- [x] S00-13 Revisar el conjunto completo iterativamente.
- [x] S00-14 Obtener una pasada final completa con cero cambios.
- [x] S00-15 Cerrar S00 en un commit lógico propio.

## 12. Resultado

S00 queda cerrado con cimientos reutilizables explícitamente `TRUSTED`, deuda arquitectónica explícitamente `REWORK/REPLACE`, ningún cimiento necesario para S01 en `UNPROVEN`, los seis holdouts ejecutados, suite aplicable verde y mutaciones dirigidas sin supervivientes.

**El siguiente trabajo del plan es G1/S01. Este documento no lo inicia.**
