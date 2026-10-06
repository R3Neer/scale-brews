# S56 - Programa numerico original de pose lineal

Estado: CERRADO adversarialmente scoped; commit local Sprint:S56 antes de avanzar.
Rol IMPLEMENTER raiz y ADVERSARY independiente.
HEAD3d3f34fb S55 cerrado; main b5e62535 verificado por fetch antes del plan.

## 1. Tesis y limites

G6 tareas2-3: compilar una familia de setupAnim original de flujo lineal a un
programa numerico comun, y evaluar sus transforms locales con inputs explicitos.
Comportamiento se descubre del programa completo, no formulas/especies/bones
hardcoded. Cow/Pig y otros modelos son fixtures diferenciales, no dispatch productivo.
FR-016/019/024/025/026/028/029/038; NFR-003..010/015/019..029.
No certifica extraccion de autoridad desde actor, renderer root/flags/material,
selectors/EntityType, bytecode transformado, catalogo general ni FULL.

## 2. Plan IMPLEMENTER

- [x] I1 S00 previo sobre reset compartido, Mth sin/cos y semantics float/source9:
  reutilizar certificados vigentes solo en su scope y auditar dependencias nuevas;
  confirmar exactitud original/runtime y consumidor common sin clases cliente.
- [x] I2 DTO/evaluador NumericPoseProgram common: DAG inmutable bounded512 nodos,
  inputs float finitos con schema explicito, constantes float exactas, operaciones
  FADD/FSUB/FMUL/FDIV/FNEG y Mth.sin/cos con promocion F2D explicita. Nueve fields
  iniciales por joint; asignaciones preservan orden/read-after-write. Bind a geometria
  exacta en preparacion; evaluate ordinary/missing/invalid/degenerate fail-closed.
  IDs part exactos, no suffix/name inference. No registry/catalogo nuevo en este scope.
- [x] I3 Certificado base reset reusable desde S54 sin relajar su API publica;
  seam package-private certifica solo reset base cuando el caller prueba overrides.
  Compiler cliente OriginalLinearPosePreparation: effective Object bridge completo,
  metodo tipado completo y despacho super hacia reset base; toda instruccion/handler/
  branch/call desconocido rechaza, no truncacion ni whitelist por especie.
  GETFIELD ModelPart enlaza identidad real al arbol; GETFIELD estado F crea schema
  determinista de field owner/name/type; no estado vivo retenido. Solo campos modelo
  final ModelPart; campos de estado F, sin lectura de flags. Copia estructural inicial
  mediante S55 snapshot y mismas validaciones parent/SourcePose9. Stack/local/recursion/
  bytes/instruction caps, ausencia total bounded, inspect no muta modelo compartido.
- [x] I4 Prueba propia diferencial nativa: al menos dos modelos originales distintos,
  estados fijados, current/initial signed scales y Euler, original setupAnim contra
  evaluator comun por cada part/matriz/vertices de cubos. Inputs explicitos no prueban
  actor channels. Roundtrip DTO, replay JVM common, negativos/unavailable/pureza.
  Build focal + compileGameTests + cliente focal; sin Tiny ni suites ajenas repetidas.
- [x] I5 Campana adversarial posterior, sensibilidad/snapshots/consumer common/coste;
  repetir solo fallo/frontera afectada, dos reviews finales cero cambios, receipts,
  canon/VALIDATION y commit local Sprint:S56 antes de avanzar.

## 3. Revisiones y coste previo

Primera pasada completa: original completo/bridge/super/reset, bind exacto, fields
por identidad, float rounding/promocion, schema y read-after-write, source9 firmado,
DTO pureza/ausencias/limites. Segunda pasada completa: coste/ownership/callers,
original differential oracle/common isolation/snapshots/meaningful negatives.
Plan convergido sin cambios tras S00 previo y modelo independiente de la seccion4.
PREPARATION bounded de class resources/symbolic stack y snapshot; limite512 nodos,
512 parts/4096cubes/64depth del DTO; programas64classes/8MiB/512instructions.
EVALUATION common O(nodes + written parts), scratch numerico local por llamada,
map/matrices solo por joint escrito, sin reflection/hash/traversal cliente hot.
No invocacion por tick/query/publication en este sprint: API preparacion explicita,
no autoridad/engine registrado ni claim TPS. No reinterpretar metadata como FULL.

## 4. Modelo/convergencia/S00 - ADVERSARY

Modelo clean-room post-S55 derivado de canon antes del plan futuro/tests. La arista
nueva es programa local original de flujo lineal -> DAG numerico comun; no arista
actor -> inputs autoritativos ni renderer/world/material/FULL. Campos finales
ModelPart enlazan identidad real, no nombres/suffixes; aliases de campos deben
ver el mismo joint y las lecturas posteriores observan escrituras previas. Schema
incluye owner/name/type reales y no guarda reflection/modelos/estado cliente.
FADD/FSUB/FMUL/FDIV/FNEG preservan cada rounding float y F2D exactamente donde
existe en el original; no fma/reasociacion/double implicito ni sustitucion Math.sin
por Mth.sin. DAG valida topologia/IDs/inputs/output/limites y bind de geometria exacta
antes de evaluar; ausencia es total, sin outputs parciales ni mutation del caller.
Invalid-node fail-closed de I2 debe aplicarse antes de sin/cos: el LUT nativo puede
mapear NaN a valor finito, que no vuelve valido un nodo invalidado. Cero/signed
scales y matrices degeneradas siguen su contrato de ausencia, no reparacion.

Proof completo del Object bridge efectivo, metodo tipado y cada despacho super,
mas base reset compartido, es obligatorio; hashes/clase/origen no certifican una
instruccion no leida. Flags/calls/branches/handlers/estado desconocidos cierran;
getters de actor y ordinary authoritative permanecen fuera de esta tesis. Registro
engine/binding/runtime/catalogo no se infiere de un evaluador numerico de laboratorio.
DTO inicial local conserva parent/SourcePose9/identity, cubos estructurales no son
visibilidad/material. Transform renderer/externa y gravedad siguen separados.

S00 scoped ejecutado antes de produccion: reset/SourcePose9 S54/S55 se reutilizan
solo dentro de su certificado,374 hashes actuales exactos. Mth/Util originales26.2
y runtime common byte iguales; sin/cos, lambda inicializadora SIN, clinit y Util.make
(cinco programas completos) iguales con solo CP indices normalizados. Referencia
independiente de65536 entradas,99645 consultas finitas seeded y specials raw float
bits pasa en JVM comun fresca que veta ambos paquetes cliente. Java float/F2D,
FNEG signed zero, overflow/division y SourcePose9 signed matrix contrastados.
3/3 shadows de Mth compilados killed por asercion semantica (cos-phase,sin-mask,
lut-source), no compiler/startup RED contado. Recibo adversarial-s00-verification.json
con21 artefactos fija el scope; no caller/script/test futuro consultado.

Coste de dependencia: vanilla LUT65536 floats, init acotado, lookup O(1); warmup100k
y medida100k pares sin/cos asigna0bytes. Solo primitiva Mth, no claim zeroalloc del
nuevo evaluador/scratch/maps/matrices ni TPS. Nueva ruta prevista PREPARATION
bounded resources/symbolic execution + evaluacion common O(nodes+written parts),
512nodes/parts,4096cubes/depth64 y resource caps declarados. Sin reflection/parser/
hash/traversal cliente hot, modelos/world retenidos ni estado numerico global mutable.
Compartir expresiones solo es valido si conserva rounding y read-after-write.

TRUSTED scoped: dependencias sin/cos inicializadas, reset comun y SourcePose9 en
limites anteriores. Nuevo NumericPoseProgram/compiler/seam override son UNPROVEN
hasta candidata/campana, no cimientos ya aprobados por este audit. Original actor/
inputs/root/visibilidad/material/transformado/FULL tambien siguen UNPROVEN.
Primera pasada modelo/plan/authority/float/schema/reset/bridge/field-binding/pureza/
DTO/ausencias/limites no pide cambios. Segunda pasada deps/S00/resources/coste/
original oracle/common/claims/regresion tampoco. Funciona, continua: plan convergido
y S00 previo cerrado scoped. No campana concreta ni holdouts futuros revelados;
regresion pertinente despues de candidata, sin Tiny ni suites amplias ajenas.


## 5. Implementacion - IMPLEMENTER

NumericPoseProgram common y OriginalLinearPosePreparation cliente implementados;
seams package-private de reset base y verify S55 reutilizan algoritmo/certificados
sin relajar la API reset publica. DAG float inmutable y topologia anterior estricta,
read-after-write por joint/field incluso campos alias, rounding float por nodo,
F2D marcado solo para Mth(D)F. Bind geometry exacta; scratch/map/matrices por llamada,
unknown/missing/nonfinite/degenerate produce ausencia total antes de trig.
Schema owner/name/F separado de caller inputs; no captura de estado/actor/modelo.
Cuerpo/bridge/super completos, ACC_SUPER resuelve desde superclass directa;
source/clase del estado original incluidos. Reflection solo PREPARATION.
512 nodos/512 targets/64 inputs/16 calls/32 locals/32 symbolic stack entries,
ademas de limites anteriores. Dos lectores recursos bounded8MiB cada uno
(base reset y compilacion), total max16MiB entre pases; dos copias/extractions
con dos validaciones DTO por copia igual que S55. No caller normal nuevo.
Bind O(parts+nodes+writes+geometry equality); evaluate O(nodes+written parts)
y asignaciones bounded no cero (scratch float, cloned9fields, SourcePose, matrices,
validacion float16 y mapa). Sin timing/TPS ni autoridad runtime inferida.

Primera pasada completa produjo ajustes ACC_SUPER/schema64 y origen del estado;
segunda pasada completa de produccion/ownership/scope/schema/float/identity/coste
sin nuevos cambios despues de la correccion de framing. Compilacion production PASS26s (log s56-compile-first.log).
Prueba propia focal/replay comun siguientes PASS, candidata frozen; no Tiny ni suites
generales. Campana independiente pendiente.

Primer run build/GameTest compile llego al cliente original y fallo137s/exit1:
S56 IMPL compiler absent por NullPointerException en canonical framing. Clasificado
IMPLEMENTATION: el compiler paso nulls de campos opcionales de Node a frame String
no-null. Corregido mediante presence tags y frame solo para valor presente; no
cambio de requisito/oracle ni tolerancia. Log s56-original-linear-first-red.log
retenido. Repite misma bateria focal, sin ampliar suites.

Build/GameTest compile/cliente focal propio PASS75s exit0 y cierre limpio:
Cow/Pig originales12 estados,84 matrices y102 cubos con vertices bidireccionales.
Initial/current/flags distintos, Euler/scales signed, pureza, determinismo, DTO
roundtrip y bind/missing/incompatible negativos. Vieja API reset sigue admitiendo
base y rechazando override; S55 export base sigue valido. JVM fresca con veto
Minecraft/Scale cliente PASS2models/12samples/84 matrices contra captura original.
Manifestordinary112server/34client restaurado sin ejecutar suite;376 hashes y372
previos intactos, JAR real sin test leaks fijado por recibo propio. Ultimas dos
lecturas completas de produccion/ownership/float/bridge/super/schema/identidad/
SourcePose/ausencias/caps/coste no producen cambios. Campana adversarial pendiente.


## 6. Verificacion y fallos - ADVERSARY

Campana independiente focal, produccion frozen y sin reparaciones ADVERSARY.
S00 previo conserva su recibo separado. La lane privada completa PASS 2m5s,
exit0 y cierre integrado/cliente limpio: Cow/Pig obtenidos por factory/renderer
reales, veinte muestras explicitas, 140 matrices PoseStack y 170 cubos con
vertices bidireccionales. Campos initial/current9 y flags son diferentes;
compilar/evaluar preserva el modelo, repeat y DTO roundtrip son deterministas.
La relacion head/leg por identidad se altera en el fixture y se exige que la
pose nativa cambie: el alias es no vacuo. Las APIs publicas reset-only/export
siguen rechazando los modelos animados. Null, thread, missing input y ordinary
incompatible se mantienen cerrados.

ADVERSARY_FIXTURE: la primera seleccion alfabetica de campos incluia body no
usado por setupAnim; no se contabiliza como prueba de alias. Se conserva el log
primero y el segundo PASS1m52s exige diferencia real de la pose nativa. El tercer
PASS conserva exactamente ese oracle y agrega dos pruebas del componente
compiler: read-after-write de un campo ya escrito y rechazo de NOP no consumido.
Estas dos pruebas inyectan un ClassModel en el recurso pass-local conservando el
receipt original, deliberadamente SOLO fault seam de gramatica/simbolica; no
prueban admision original de ese recurso sintetico ni whole-class trust.

JVM fresca con veto de todos los paquetes cliente: replay PASS140 matrices del
JSON capturado nativo, mas IEEE float ordenado, FNEG/signed zero, sin/cos originales,
no-finite antes del LUT, initial fields, exact bind, ausencia/null/ordinary/missing,
degeneracion/signed scale, outputs detached, mapa inmutable, topologia/DAG,
512/513 nodos, 64/65 inputs y 8.000 evaluaciones concurrentes. Seis mutantes del
owner comun producen AssertionError semantico; dos mutantes cliente producen AssertionError semantico en read-after-write y
programa NOP no consumido. Total8/8 kills, driver agregado0; negativos nativos1.
Warm100k/measure100k del caso minimo de un input/un write asigna 54.825.600 bytes
en la corrida publica independiente; es evidencia de esa JVM/caso, no cota global
ni cero alloc, TPS o claim de optimizacion. Recursos/clases/metodos/nodos y copias
siguen acotados PREPARATION; no nuevo caller HOT ni descubrimiento por entidad.

Lane publica: tools/s56_adversarial.init.gradle, runClientGameTest y
s56CommonClasspath con build/cache/run y classpath-file propios. Consumidor
JVM tools/s56_adversarial_common.py recibe classpath/programs.json/output absoluto,
lanza en su propio output y veta cliente. Sensibilidad reusable separada en
tools/s56_adversarial_common_mutations.py y tools/s56_adversarial_mutations.ps1.
Projection SVG/PNG inspeccionada: envolventes agregadas coinciden; el oracle
numerico/vertices es independiente. No es captura renderer, pose articulada
visual completa, root/filter, material ni FULL. No Tiny ni suites ajenas.

Ultima lectura de los cuatro cambios y callers: effective bridge/ACC_SUPER,
field owner/type, object identity, read-after-write, float intermediates,
resource framing/hash, pass-local bounds, reset-only guard y matrices locales
convergen sin cambios. 376 hashes coinciden con recibo IMPLEMENTER; 372 owners
anteriores intactos. Dos pasadas completas finales no piden cambios de produccion. Funciona, continua
con cierre scoped S56: docs/evidence/S56/adversarial-verification.json fija recibo
final, ocho kills y superficies publicas. El recibo conserva fuente privada
exactamente ejecutada; su promocion solo cambia el nombre de clase. G6 completo
y la autoridad de actor/renderer/material/FULL siguen abiertos.

## 7. Cierre

IMPLEMENTER reproduce la lane publica focal: PASS2m35s exit0, cierre limpio,
dos modelos/veinte muestras/140 matrices/170 cubos; controles de alias no vacuos
y APIs reset/export estrictas. Replay comun publico PASS6.8s, veto cliente,
140 matrices y8.000 evaluaciones concurrentes. Warm/measure100k del caso minimo
asigna54.393.384 bytes en esta JVM; no cota global ni TPS. Los ocho kills previos
se verifican en sus logs/hashes/driver agregado0 sin repetir la sensibilidad verde.
Recibo independiente SHA39fad4b65e0d4dd5fb6ba78660c0d6df6301ce9c404fb8d9900cfec7a836eb28:
44 artefactos y376 hashes productivos comprobados exactamente,372 owners intactos.

SVG publico byte igual al independiente; JSON semanticamente igual, con orden
de claves distinto de mapas del fixture. IMPLEMENTER_INVOCATION: el primer probe
de comparacion exigio bytes JSON sin canonicalizar el orden; corregido a igualdad
del documento parseado, sin cambiar produccion/oracle/tolerancias. PNG publico
inspeccionado: solo envolventes locales, sin representacion renderer/root/material.
Manifest ordinario112server/34client restaurado PASS4s sin ejecutar otras suites.
Main refrescado b5e62535 ancestro de HEAD; no drift ni cambios de Tiny.

Dos pasadas finales completas de produccion, ownership, autoridad, float/dispatch/
identidad/ausencias/caps/coste y evidencias/callers/claims no producen cambios.
Compiler/DAG/reset seam TRUSTED dentro de este scope explicito. No engine registrado,
catalog resource, inputs de actor ni binding fisico nuevo. G6 tareas2-6/G7-G9 y
asuntos humanos al final de epica permanecen abiertos. Commit local propio
Sprint:S56; no push/tag/release/install. Cleanup adversarial exacto de cuatro
one-offs sustituibles; tooling publico reusable conservado, sin borrar caches/worlds.
