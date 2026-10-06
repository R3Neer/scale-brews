# S55 - Export de geometria local inicial para el programa de reset

Estado: CERRADO scoped, commit local propio Sprint:S55. Rol raiz IMPLEMENTER; ADVERSARY existente
mantiene contexto independiente y comunica solo resultados operacionales.

## 1. Tesis y S00 scoped

G6 tareas2-3: desde un modelo admitido por S54, exportar el arbol de transforms
iniciales y sus cubos estructurales como ModelGeometry server-safe. La evaluacion
rest con scalebrews:static debe coincidir con las matrices del reset original
para estados ordinarios, sin resetear ni modificar el modelo durante preparacion.
La exportacion es numerica/estructural: no prueba visibilidad, material fisico,
renderer root, actor/model selector, bytecode transformado ni FULL.
FR-016/019/024/025/026/029/038, NFR-003..005/007..010/015/019..029.

HEAD 4e32a4ca limpio; main b5e6253584b2233e5dd0039203c8a251bf10cdeb integrado,
fetch previo al plan. No cambios a Tiny, pasajeros, solver, red o root providers.
S54 OriginalResetPosePreparation TRUSTED solo programa original/reset y receta.
S45 ModelPartStructure TRUSTED solo copia inicial/fingerprint/limites; S17/S18
extractor y ModelGeometry TRUSTED para local transforms/cubos/SourcePose en sus
limites ya auditados. Nuevo uso del engine static UNPROVEN: auditar S00 antes
de produccion; no convertir su existencia o un verde historico en especificacion.

## 2. Plan IMPLEMENTER

- [x] I1 Cerrar S00 de static en scope: parametro vacio, ordinary positivo,
  incompatible negativo, output rest vacio/inmutable, binding common sin clases
  cliente, sin reflection/hash/traversal hot. Conserva engine/registry/API.
- [x] I2 Exponer snapshot package-private de ModelPartStructure que devuelve
  fingerprint y DTO inicial del mismo arbol copiado, sin duplicar algoritmo de
  copia. inspect previo conserva resultado/semantica. Root DTO explicito identidad
  format2 para expresar coordenadas locales, nunca el default Living renderer.
- [x] I3 Nuevo owner OriginalResetGeometryPreparation, PREPARATION client thread:
  inspeccion S54, snapshot inicial, fingerprint exacto, mapping bidireccional
  entre paths framed del programa y IDs runtime de geometry, parent y nueve
  SourcePose exactos. Count no sustituye identidad. Ausencia total bounded ante
  mismatch/limites/desconocido. Copias DTO, sin modelos/parts/world retenidos.
  Fuente/version reproducibles derivadas de tool y digest S54. Cubos incluyen
  estructura completa, no leer current visible/skipDraw ni afirmar autoridad.
- [x] I4 Prueba propia cliente nativo de dos modelos de tecnologia: perturbacion
  de current/flags, initial rotaciones y escalas signed no unitarias, snapshot
  antes/despues; original setupAnim como oracle de cada matriz jerarquica.
  Cada cubo/bounds/part contrasta vertices originales, sin counts-only oracle.
  Round-trip DTO y static binding/evaluate en common; negativo animated/cache/
  malformed/thread/limites pertinente. Focal build + GameTest compile + cliente,
  y servidor focal si S00 o export exige demostrar dedicated. No suite Tiny ni
  full server/default client; ordinary manifest restaurado y G9 sigue completo.
- [x] I5 Campana adversarial independiente, snapshots/mutaciones/replay publico,
  coste/receipts exactos, dos reviews completas sin cambios, canon/VALIDATION,
  cleanup y commit local Sprint:S55 antes de avanzar.

## 3. Revisiones del plan y coste

Primera lectura: requisitos/autoridad, DTO/common-client, SourcePose signed/Euler,
parents/mapping completo, fingerprint, conservacion de API/semantica anterior.
Segunda lectura: ausencias/ownership/limites/coste, oracle original/cubos, runtime
server-safe y regresion por frontera. Cero cambios al plan; no produccion aun.
PREPARATION: H/C/bytes/metodos/instrucciones S54/S51; P<=512, cubes<=4096,
depth<=64, vertices<=65536 S45. Copy/extraction y mapping bounded O(P log P +
vertices + recursos). No callers hot ni registry global nuevo; static evaluacion
O(1), composition transforms O(P) preexistente. No benchmark TPS atribuido aqui.

## 4. Modelo y convergencia - ADVERSARY

Modelo previo clean-room post-S54 derivado de requisitos/arquitectura/protocolo
antes de este plan y sin tests historicos como fuente de casos. La arista nueva
es receta original de nueve campos + estructura inicial -> DTO local server-safe,
no actor/selector -> pose visible completa -> material/FULL. SourcePose conserva
nueve valores, incluida escala signed y representantes Euler, no solo matrices.
ModelPart root local sigue siendo distinto de renderer/world root. El DTO format2
con modelTransform identidad explicita evita heredar el default Living renderer.

Invariantes: un unico algoritmo de copia inicial y su fingerprint/DTO coherentes;
modelos compartidos sin mutar current9/initial/flags/tree/cache. Paths estructurales
framed y IDs runtime requieren bijeccion exacta con parents y SourcePose, no counts.
Cubos/pivots/inflation/huecos jerarquicos preservados con referencia original;
visibilidad actual no elimina cubos estructurales ni se vuelve autoridad fisica.
DTO/receta inmutables, sin objetos modelo/parts/world retenidos; fallo/limite/mismatch
produce ausencia total y motivo bounded. Identidad tool/programa/recursos/initial
reproducible; no engine/binding ejecutable ni root provider nuevo por esta metadata.

S00 static cerrado TRUSTED solo en este scope antes de produccion. Evidencia
adversarial-s00-verification.json y17 artefactos en docs/evidence/S55/adversarial-s00.
JVM aislada con parent platform y veto de clases net.minecraft.client/Scale client:
actual registry/binding/evaluate comun pasa, sin arrancar renderer ni suite Minecraft.
Compilacion de harness PASS, positive/negative/common matrices PASS, owner init
idempotente y preclaim negativo PASS. Empty parameters + ordinary producen Map
rest vacio/inmutable; incompatible/parametro desconocido no producen output. Bound
copia parametros/schema, rechaza missing/malformed channels y null; snapshot de
parametros no revive al mutar mapa del caller. Rest compone matrices jerarquicas
con rotaciones y escalas signed no unitarias en DTO format2 identidad.
Tres mutants compilados killed semanticamente: ordinary, parameters, rest-output.
Los373 hashes previos estan intactos; clase BuiltInPoseEngines ejecutada coincide
byte a byte con JAR S54. No expectativa legacy ni verde historico define este oracle.

Coste S00: static evaluate O(1), resultado Map.of compartido; warm-up100k y medida
100k asignan1600000bytes (16bytes/evaluate de Optional.of). No claim zero-allocation
ni TPS; copy defensiva de bind solo preparacion, compose O(P) preexistente. Registry
sync solo inicializacion/lookup; el Bound preparado no hace lookup por sample.
No parser/reflection/hash/traversal/modelo cliente en static evaluate. La nueva
snapshot/export sigue UNPROVEN hasta campaña y requiere recertificar la extension
S45; reuse TRUSTED no exime sus cambios de owner/consumidor. Dos copias potenciales
S54 inspect y snapshot son PREPARATION y deben quedar contadas/justificadas, sin
atribuir trabajo unico ni filtrarlo a hot runtime.

Primera pasada completa modelo/tesis/requisitos/origen/authority/mapping/parents/
SourcePose/cubos/ausencias/thread/pureza/S00 no pide cambios al plan. Segunda pasada
completa owners/recursos/immutable DTO/identity/caps/coste/oracle original/snapshots/
regresion/claims tampoco. Funciona, continua: convergencia previa y S00 scoped.
Futura campaña concreta/holdouts export despues de candidata; sin consultar tests
IMPLEMENTER futuros ni compartir casos privados. Regresion focal pertinente, sin
Tiny ni suites amplias ajenas; G6 tareas2-6/G7-G9/FULL y QA final permanecen abiertos.

## 5. Implementacion - IMPLEMENTER

Owner nuevo OriginalResetGeometryPreparation y extension snapshot package-private
del unico algoritmo de copia S45. Inspeccion anterior conserva fingerprint exacto;
DTO format2 local identidad y SourcePose inicial. Mapping framed->runtime ID
verificado por identidad completa/parent/nueve campos; snapshot fingerprint debe
coincidir con receta S54. DTO no conserva referencias modelo/cubos/mundo.
Dos copias/extractions iniciales bounded por llamada explicita (certificado S54
y snapshot de export); ningun caller hot/registry/cache global nuevo.
Cada snapshot valida el DTO extraido format1 y su reconstruccion format2 mediante
withModelTransform, dos validaciones O(P + cubes) del DTO por copia. Tambien aplica
al inspect de inventario que delega al snapshot: trabajo PREPARATION adicional
bounded, no se afirma copia/validacion unica ni optimizacion de CPU/asignaciones.
No nueva evaluacion por tick/query/observador.
Bound static preexistente O(1),16B/evaluation de Optional segun S00,
compose O(P) preexistente.
Primer review completo code/plan/error/identity/parents/SourcePose/coste sin cambios
de produccion; segundo review ownership/mapping/inputs/flags/semantica previa sin
cambios. En ese freeze, la exportacion estaba pendiente de prueba nativa y
campana independiente; la ejecucion propia siguiente conserva la misma produccion.

Build/compilacion GameTests/cliente focal PASS134s, exit0/cierre limpio: dos modelos
originales,14 matrices y10 cubos; vertices bidireccionales, jerarquia, escalas signed,
initial/current/flags, identity, DTO roundtrip, static positive/negative y snapshot/
restore. Nuevo owner0c208ce9 y ModelPartStructure1c885c5c,372 previos byte intactos.
JAR774 entradas/1285867B SHA bf110a72ff62f134fc85966b3d3127337f43e65c89443f2ff13bac5ae7d6537d,
cero clases test. Manifest ordinary112/34 fuente/procesado igual, restauracion4s.
Primer fallo fue IMPLEMENTER_INVOCATION: argumento PowerShell -P sin cita produjo
valor vacio antes de compilar. Log retenido; corregir cita, sin cambios de fuente.
Common produccion intacta; S00 JVM veto cliente demuestra binding/evaluate comun,
no se afirma nueva suite servidor/dedicated nativa. No Tiny/full server/default
client/selector/parser S51-S54 ajenos. Recibo propio pendiente de cierre independiente.

Replay publico propio compileGametestJava/runClientGameTest con lane adversarial,
build/cache/run propios PASS140s exit0/cierre integrated/client limpio.21 matrices,
15 cubos,8 metadata faults, detachment/512-513/cache. Manifestordinary112/34 normal
restaurado2s. GeometryJSON bdad7d99 y SVG f67df776 byte iguales al run independiente.
PNG propia sharp900x600/9113B inspeccionada: dos pares de envolventes proyectadas
coinciden; no se afirma GPU, piezas completas por imagen, visibilidad o material.
La semantica completa usa asserts por parte/cubo. Recibo/outputs/logs durables.
Tercera lectura completa despues de replay/oracle/DTO/common-pureza/limites/coste
no produce cambios de produccion. Sensibilidad independiente sigue pendiente.

## 6. Campana y recertificacion - ADVERSARY

Campana independiente posterior a candidata, sin usar las pruebas IMPLEMENTER
como fuente de casos. Nativa original de dos modelos (Slime y SmallSulfurCube):
current9 e initial9 distintos, flags false/skipDraw true, initial signed scales,
Euler no triviales y jerarquia completa. setupAnim original con tres inputs
(null, Object, SlimeRenderState) es el oracle; cada matriz se compara con PoseStack
nativo y cada cubo/bounds inflado con vertices originales en ambas direcciones.
21 matrices/15 cubos pasan; no oracle de counts ni export como referencia de si mismo.
Fingerprint previo, parent/ID/SourcePose9 exactos, source/version derivadas del
programa, round-trip DTO, rest static vacio, detachment y repeticion/pureza pasan.
Cuatro defectos metadata por modelo se rechazan en el seam privado verify:
SourcePose alterada, parent alterado, identidad foreign y miembro omitido. Son
pruebas del validador de relacion, no prueba de admision de receta arbitraria
por API publica. Cache duplicado se rechaza por la API publica; limite exacto
512 partes se admite y 513 se retira; off-thread falla y null produce ausencia.

Lane privada ampliada PASS59s limpio; lane publica reusable PASS96s limpio.
S55AdversarialGeometryProof y tools/s55_adversarial.init.gradle ejecutan solo
esa frontera cliente, sin Tiny/default/full server. Init permite build/cache/run
propios, shadow test-only y tarea s55CommonClasspath. Consumidor del JSON real en
JVM nueva con parent platform y veto de todo paquete cliente pasa: 2 modelos,
7 partes y5 cubos, binding static/rest/composicion/convex corners e incompatible
negativo. tools/s55_adversarial_common.py y tools/s55-common permiten repetirlo
sin Minecraft cliente ni afirmar sesion dedicada nativa. Snapshot SVG/PNG usa
vista fija px=x+.35z, py=-y+.15z y bounds agregados; referencia original y DTO
coinciden visualmente. Es esquema de bounds iniciales, no captura renderer,
visibilidad/pose completa ni sustituto de los oracles por cada cubo/matriz.

Coste PREPARATION scoped: se cuentan dos copias/extractions iniciales (certificado
S54 y snapshot export). Cada copia valida DTO extraido format1 y reconstruye/
valida format2 identidad; inspect de inventario delegado paga tambien ese paso.
O(P log P + vertices + recursos) bounded por caps S45/S54, mapping O(P*depth)
con depth64; metadata O(P+cubes+recursos), sin cache/global registry nuevo,
referencias a modelos/world retenidas ni callers hot. Static Bound existente
O(1)/Optional16B por evaluacion medidos en S00; compose O(P). No claim de copia/
validacion unica, optimizacionCPU, zero-allocation, TPS o heap-soak integrado.

Clasificaciones de infraestructura: primer harness no compilaba por wildcard
org.joml.Math vs java.lang.Math; imports explicitos conservan oracle. Invocacion
con --gradle-user-home confundio cache de proyecto/distribucion y fue detenida
antes de compilar; corregida a --project-cache-dir. Primer replay common omitio
initialize del owner built-in; inicializacion explicita conserva contrato S00.
Auditoria offline inicial comparo Git blobs LF con archivos CRLF y luego uso key
vieja del recibo; contraste byte exacto con recibo S54 aceptado pasa372 previos
intactos y solo owners previstos. Ninguno se contabiliza como RED de produccion. Root detecto un log vacio generado
por el replay common en cwd del repo: ADVER_TOOLING/housekeeping. Runner reusable
convierte paths con Path.absolute (no resolve) y lanza javac/JVM en output propio.
Log de cero bytes archivado/retirado por ruta exacta con ownership/timestamp
comprobados; replay solo JVM PASS y repo logs/latest.log ausente despues. Version
previa del runner preservada; no repeticion nativa/sensibilidad/Tiny por cwd.


Primera pasada completa codigo/authority/relacion/extractor/ausencias/pureza/
parents/SourcePose9/cubos/DTO/bounds/common/callers no pide cambios productivos.
Segunda pasada completa owners/programa/copia/fingerprint/DTO/parents/cubos/
static/common/ausencias/identidad/coste/receipts/claims sin cambios productivos.
Sensibilidad 8/8 kills compilados semanticamente: current-pose, renderer-root,
initial-channel, parent-identity, current-flags, shared-state, cube-bounds y
fingerprint. Baseline instrumentado PASS58s, el fault cambia solo fingerprint
exportado despues de la copia; retirar su guard admite drift y el oracle mata
el mutante en54s. No compiler/timeout/startup RED cuenta como sensibilidad.
Driver agregado exit0; pruebas negativas nativas exit1 esperado por asercion.
Baseline publico PASS96s; replay IMPLEMENTER PASS140s y JSON/SVG byte iguales.

Recibo adversarial-verification.json conserva hashes/artifacts y alcance exacto.
Produccion374 verificada;372 owners previos S54 byte intactos, solo ModelPartStructure
modificado y OriginalResetGeometryPreparation nuevo. JAR774/1285867B exacto,
579 nombres qualified GameTest contrastados contra JAR sin leak. Artefactos de
recibo IMPLEMENTER verificados. Funciona, continua: ambos owners TRUSTED solo
para export explicito PREPARATION de geometria inicial local y fingerprint previo.
No cierre global G6, visibility/renderer/actor selector/material/FULL/G7-G9 ni
aceptacion humana. S00 scoped conserva limites y16B/evaluate, sin claim TPS.

Receta publica: JAVA_HOME JDK25; ./gradlew.bat -I tools/s55_adversarial.init.gradle
-PscalebrewsBuildDir=<build propio> --project-cache-dir <cache propio>
-Ps55RunDir=<run propio> runClientGameTest. Captura en run/s55-adversarial-snapshots.
Sensibilidad: tools/s55_adversarial_mutations.ps1 -OutputDirectory <salida propia>
-Mutants fingerprint (o bateria8 por defecto). Common: mismo init y outputs
con tarea s55CommonClasspath -Ps55ClasspathFile=<archivo absoluto>; despues
python tools/s55_adversarial_common.py <classpath> <geometry.json> <salida>.
No pruebas nuevas ni baterias ajenas necesarias por cierre documental.

## 7. Cierre - IMPLEMENTER

Recibo independiente a5a8b024 y50 artefactos exactos verificados;374 hashes iguales
al recibo propio,372 previos intactos. Los8 logs confirman assertionsemantica con
compilacion valida y fuentes correctas; no repeat de sensibilidad ya verde sin
cambio/fallo pendiente. Driver agregado0, parametros/shadows fuera del checkout.
Housekeeping runner common cwd/argumentos absolutos comprobado solo en JVM;
log accidental archivado/retirado por ruta exacta, cinco one-offs independientes
retirados sin borrar caches/worlds/evidencia. Tooling reusable conservado.
Ultimas dos lecturas completas produccion/autoridad/mapping/parents/SourcePose/
cubos/DTO/ausencias/bounds/common/coste/recibos/canon no producen cambios de fuente.
Main fresco b5e62535 integrado, commit local separado Sprint:S55. Sin push/install/
release ni Tiny ajeno. G6 tareas2-6/G7-G9 y QA humana al final de epica abiertos.
