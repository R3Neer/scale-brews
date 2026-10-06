# S54 - Programa original de reset de transforms locales

Estado: CERRADO scoped local numeric program; commit local propio Sprint:S54. Rol raiz IMPLEMENTER;
ADVERSARY existente conserva modelo clean-room y holdouts; comunicacion operacional.

## 1. Scope y S00

G6 tarea2: certificar familia de programa MODEL_LOCAL_RESET_INITIAL_TRANSFORMS
por despacho efectivo setupAnim(Object), programa original completo y estructura
observada. Receta de preparacion inmutable, no engine/binding ejecutable, no pose
fisica completa. FR-016/019/024/025/026/029/038, NFR-007..010/015..018/024..027.
No autoridad de visibilidad/skipDraw, root renderer, layers/material, animacion
externa o asociacion EntityType/renderer. No Tiny/pasajeros/red/fisica.
HEAD fc134997 limpio, main b5e6253584b2233e5dd0039203c8a251bf10cdeb integrado.
S00 scoped: OriginalModelPrograms (S51) y ModelPartStructure (S45) TRUSTED en
sus limites existentes. Los nuevos programas son candidatos bajo prueba, no
fundamentos asumidos. Legacy static PoseEngine no se declara TRUSTED ni se usa.

## 2. Investigacion previa

Original26.2 merged SHA ec9c864e8a304be37d1d273fefefcb4e4dcc2154a18ccd3c1fe9d2d0a298a7d9.
Model.setupAnim(Object) solo this.resetPose(); Model.resetPose final itera allParts;
ModelPart final resetPose llama loadPose(initialPose); loadPose escribe exactamente
nueve transforms de PartPose final record. No visible/skipDraw ni estado consultado.
Model.root/allParts son getters finales, cache allParts creada en constructor;
comparar identidades exactas del cache con traversal inicial evita cache obsoleto.
PartPose getters/constructor puros y estructura finita de S45. Investigar/auditar
programas completos merged y split/access-widened antes de aprobar pares de hashes.
Slime y SmallSulfurCube son witnesses de tecnologia, no lista de especies productiva.

## 3. Plan IMPLEMENTER

- [x] I1 Nuevo owner OriginalResetPosePreparation, API inspect(EntityModel) solo
  client thread PREPARATION. Resolver setupAnim(Object) efectivo exige Model.class;
  jerarquia original <=12, sin admitir overrides/bridges ajenos ni ejecutarlos.
  Dependencias Model/ModelPart/PartPose recursos exactos pares previamente auditados.
  Verificar programas completos setupAnim/resetPose/root/allParts/getInitialPose/
  resetPose/loadPose y nueve getters PartPose, descriptores, fields, stores, ramas
  y loop/joins; no handlers/extras. Parser existente intacto, caps S51 intactos.
- [x] I2 Validar initial structure S45 y traversal determinista <=512parts/64depth;
  cached allParts exacto por identidad sin null/dup/omision/extras, no sharedchild.
  Emitir una row por path estructural framed con nueve initial floats finitos,
  fingerprint/resource hashes/owner/family y canonical/digest. No referencia viva
  retenida, no reset/setupAnim del modelo compartido durante inspect; visibilidad
  preservada, no le atribuir autoridad. Errores ausencia total/diagnostico <=256.
- [x] I3 Test propio nativo dos modelos de la familia y modelo animado negativo;
  variar estado y transforms actuales/visibilidad con snapshot/restore, oracle
  original setupAnim contrasta nueve campos y preservacion visible/skipDraw.
  Determinismo/inmutabilidad/ausencias/thread/estructura/cached membership.
  Compilar JAR/GameTests y cliente focal S54; S45 usado en fingerprints afectados.
  Parser/selectores S51-S53 intactos, no repeats ni Tiny/full server/default client.
  Manifest ordinary112/34 restaurado; G9 completo obligatorio.
- [x] I4 Campana independiente, sensibilidad/replay/snapshots/coste, revisiones
  completas sin cambios, canon/VALIDATION/recibos/cleanup y commit propio Sprint:S54.

## 4. Revisiones IMPLEMENTER y coste

Primera pasada verifica instrucciones completas/despacho/aliases y cache exacto;
segunda pasada requisitos/ownership/origen/thread/errores/inmutabilidad/caps/coste
sin cambios al plan. Programas para recursos originales, no bytecode transformado.
PREPARATION explicita cero callers hot; H<=12, C/bytes/metodos/instrucciones S51,
P<=512/depth64/cubes4096/vertices65536 S45. Dos traversals bounded (fingerprint y
rows) documentados, O(P log P + geometria + recursos); cache/parser pass-local,
ningun mundo/lock/cache global. Sin claim TPS ni benchmark integrado.

## 5. Modelo y convergencia - ADVERSARY

Modelo previo independiente post-S53, derivado antes del plan desde canon.
La tesis nueva puede probar solo la arista modelo seleccionado -> programa de
transforms locales; no prueba seleccion, fuente de actor, pose visible completa,
root renderer, capas, material ni cobertura FULL. Reset de nueve campos tampoco
convierte visible/skipDraw en canales autoritativos ni revive la autoridad de
extraccion S52. Esta separacion converge con el scope explicito S54.

S00 scoped: OriginalModelPrograms TRUSTED solo parser/resource bounds S51;
ModelPartStructure TRUSTED solo estructura inicial copiada S45. El despacho
setupAnim/reset y las dependencias Model/ModelPart/PartPose permanecen candidatos
UNPROVEN hasta campaña. Legacy static PoseEngine no se usa como especificacion.
Ninguna prueba historica verde autoriza un nuevo evaluador fisico.

Invariantes del nuevo programa: resolver setupAnim(Object) efectivo y rechazar
overrides/bridges desconocidos; origen/jerarquia y recursos exactos. Probar
cuerpo completo de setupAnim, loop de resetPose, getters y loadPose con sus
nueve stores/getters/fields exactos, type/alias/local/branch/exit sin extras.
El estado del parametro no interviene en esta tecnologia; no inventar channels.
El cache allParts y traversal de estructura deben coincidir por identidad
actual, sin null/dup/missing/extras; rechazo explicito de shared child evita
identidad ambigua de paths. Membership no puede deducirse de counts solamente.
No invocar reset/setupAnim al inspeccionar ni modificar poses/flags compartidos.
Cada row guarda los nueve valores iniciales finitos, path framed no ambiguo,
metadatos/digests inmutables sin modelo/parts/mundo retenidos. Unknown/ciclo/
cache stale/limite tiene ausencia total y motivo bounded, no receta parcial.

Coste: PREPARATION/OFFLINE sin caller hot; H jerarquia, C clases/bytes, M metodos,
I instrucciones, P parts, depth y cubes/vertices. Caps previos S51/S45 intactos;
traversal/fingerprint y rows bounded, shared work solo pass-local. No reflection,
parsing/hash/world scan en una query de pose ni owner runtime nuevo.

Revision independiente de javap original split26.2 confirma Model.setupAnim
llama resetPose final, iteracion allParts con salida/back-edge, ModelPart final,
resetPose/loadPose y nueve campos PartPose; getters root/allParts finales.
Esta investigacion solo comprueba coherencia del plan, no PASS de una candidata.
Se debe conservar la separacion entre estado previo a inspect y el que el
oracle setupAnim nativo cambia legitimamente al resetear nueve transforms.

Primera pasada completa: requisitos/autoridad/aristas, origen/despacho/loops/
fields/aliases, estructura/cache/identidad/flags, errores/limites y scope.
Segunda pasada completa: owners/S00/dependencias, immutability/provenance,
coste/caps, observacion original/sensibilidad/snapshots/regresion y cierre.
Ambas producen cero cambios: funciona, continua. Compartidos S45/parser y
selectores intactos se reusan solo en scope; regresion focal nueva y fingerprint
pertinente, sin Tiny/S51-S53 completos ajenos. Campaña concreta/holdouts despues
de candidata; ninguna produccion o tests nuevos creados por ADVERSARY aqui.
G6 tareas2-6/G7-G9 y QA humana al final de epica permanecen abiertos.

## 6. Implementacion - IMPLEMENTER

Nuevo owner OriginalResetPosePreparation, 372 owners previos byte intactos.
Programa completo original y despacho erased resuelto a Model, caches/tree iguales
por identidad, nueve initial fields en rows framed inmutables; no modelo/mundo
retenido ni llamada setup/reset durante inspect. Caps/parser previo intactos.
19 programas completos merged/runtime equivalentes bajo normalizacion CP/ldc/
branch ordinal, recursos exactos preservados y auditados antes de pinning.
Build/GameTest compile y cliente focal PASS111s exit0 limpio: dos modelos nativos,
seis estados/reset originales, snapshots current/initial/flags, cached tree stale,
modelo animado negativo, null/inmutabilidad/determinismo. Manifest normal112/34
restaurado PASS1s fuente/procesado igual. No Tiny/server/default/S51-S53 ajenos.
JAR 769 entradas, 1277585 bytes SHA5a4901c5914f3eb98e08d8a14db10f866e932b28c9d2a9bcb8a76758afce2521.
Dos pasadas completas codigo/autoridad/programas/origen/tree/coste/canon sin cambios.
Recibo propio y logs/recetas/native programas en docs/evidence/S54. No produccion
reparada ni test rojo. Correccion de cuatro lineas mojibake historicas S53 doc-only.


Replay publico IMPLEMENTER PASS172s exit0 limpio, build/cache/run separados.
Tres inputs/resets, nueve canales, cuatro caches invalidos, nueve NaN y512/513;
dos programas malformados con motivos exactos. SVG/recipe.json byte identicos
al public default run del adversario. PNG propia rasterizada con sharp bundled
e inspeccionada: initial=after reset, before diferente. Solo proyeccion de cubos,
no GPU/visibilidad/material. Espera de lock global Loom conservada, no cache
borrada ni proceso interrumpido. Produccion373 congelada sin cambios/retest propio.

## 7. Verificacion - ADVERSARY

Funciona, continua: aceptacion scoped del programa original de nueve transforms
locales iniciales. Recibo adversarial-verification.json y evidencia durable en
docs/evidence/S54/adversarial; lane reusable tools/s54_adversarial.init.gradle,
S54AdversarialResetProof y driver shadow tools/s54_adversarial_mutations.ps1.
No produccion modificada por ADVERSARY; los 373 hashes coinciden con el recibo
IMPLEMENTER y los 372 owners previos con S53. JAR 769/1277585B SHA5a4901c5 exacto,
sin clases de test; siete recursos runtime verificados contra JARs originales.

Nativa privada final PASS43s; replay publico propio PASS159s y replay IMPLEMENTER
PASS172s (incluida espera Loom), exit0/cierre limpio. Tres estados de parametro
(null/Object/SlimeRenderState) producen nueve campos identicos a cada row inicial
tras setupAnim nativo. Current pose perturbada no cambia receta; initial pose si.
Flags actuales se conservan, sin convertirlos en canales certificados. Restore,
repeticion, listas/maps inmutables, off-thread y null comprobados. Cache rechaza
duplicado/ajeno/omision/null y cambio del tree; mismo conjunto reordenado admite.
Exact512 admite,513 rechaza; nueve NaN rechazan. Modelo animado original no admite.
Framing directo rechaza alias flat/nested en el helper; preparacion completa ya
rechaza esa estructura ambigua en S45. No claim de admision mas amplia.

Seis shadows separados killed por AssertionError semantico: loop-target,
field-identity, cache-membership, override, transform-swap y path-frame. No error
de compilacion/startup cuenta como kill. Programa-fault inyecta solo el operand de
store o la rama a un label real, para medir el guard completo sin ejecutar bytes
alterados. Fuentes/shadows/logs/hashes exactos preservados en recibo. El ultimo
arranque sufrio una pausa larga del entorno; no se usa su wall-time como coste.
Tres rojos de desarrollo fueron ADVERSARY_FIXTURE: CP name global cambiaba getter
ademas de store; storePose solo guarda seis canales y escala unitaria; extractor
S45 rechaza paths ambiguos antes del helper nuevo. Se conservaron fallos y fuentes.
Wrapper del driver salia con LASTEXITCODE1 del ultimo mutante negativo; reporte
6/6 valido. Driver publico devuelve agregado0 explicitamente; cambio solo de exit.

SVG y recipe de ambos replays byte iguales; PNG inspeccionado: initial y after
identicos, before distinto. Es un esquema determinista de transforms locales,
no captura del renderer ni autoridad de visibilidad/root/material. Bounds y
ownership revisados: PREPARATION explicita, cero callers hot, parser/caches
pass-local, H12 y caps previos, P512/depth64/cubes4096/vertices65536; dos traversals
bounded y scan identity de cache. Sin cache/mundo/lock global ni claim TPS.

Primera revision completa de codigo/programas/despacho/origen/ownership/estructura/
cache/mutabilidad/errores/coste y pruebas no pide cambio productivo. Segunda
revision completa de requisitos/canon/claims/receipts/provenance/publicacion/
regresion pertinente y snapshots tampoco. No Tiny ni bateria ajena repetida.
Solo familia MODEL_LOCAL_RESET_INITIAL_TRANSFORMS queda recertificada en su scope;
transformed/flags/root renderer/layers/material/asociacion/engine/binding/FULL
siguen sin autoridad nueva. G6 tareas2-6/G7-G9 y QA humana final siguen abiertos.

## 8. Fallos y revisiones finales

IMPLEMENTER_TOOLING: collector inicial de test leaks comparaba solo basename y
confundio fake de test GravityFrames con owner productivo de distinto package.
Comprobar nombres completos y companions exactos: cero leaks reales. Corregido
solo collector/recibo; no fuente ni test runtime cambiado, no requiere repetir.


## 9. Cierre

- [x] Plan/modelo previos convergidos y S00 scoped.
- [x] Implementacion, own focal111s y public replay172s limpios; manifest112/34.
- [x] Campana independiente nativa43s/public159s,6/6mutantes y snapshots.
- [x] Driver root loop-target agregado0, native1 por AssertionError semantico.
- [x] Ambas revisiones completas cero cambios productivos, recibos/evidencia/coste.
- [x] Canon/VALIDATION/cleanup12 one-offs exactos y commit local propio Sprint:S54.

Root verifica373 source hashes y40 artefactos independientes exactos, recibo
SHA0225d5e38dbdaa121485f0c219d5cede0b1404e28412b59dd236ab9ff1ed311a.
Sourcehelper a62a64fa y oracle851c73da congelados; JAR769 intacto. Driver corregido
agregado0 verificado en replay aislado de un solo mutante, source/shadow hashes
iguales al independiente, motivo program fault admitted0. Gradle reporta8h50m19s
para build completo con intervalo prolongado anterior a cargar mundo: no se usa
como tiempo de runtime ni evidencia de rendimiento. Main fresco b5e62535 integrado.

Ultimas pasadas completas source/canon/claims/evidencia/paths/coste/inmutabilidad/
regresion pertinente no dejan cambios productivos ni huecos scoped. TRUSTED solo
programa original de nueve transforms locales y cache/estructura; sin autoridad
transformed/flags/root renderer/layers/material/asociacion/engine/binding/FULL.
G6 tareas2-6/G7-G9 y necesidades humanas al final de epica permanecen abiertos.
