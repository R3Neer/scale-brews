# S57 - Programa numerico en la revision aceptada del catalogo

Estado: CERRADO scoped. IMPLEMENTER raiz, ADVERSARY independiente.
Base cf7b2991 S56 cerrado; main vigente b5e62535 ancestro, sin drift.

## 1. Tesis y scope

G6 tareas2-3/6: un programa numerico neutral validado puede ser seleccionado
declarativamente por un PoseEngine reusable dentro de una revision aceptada,
reload/transmitido atomicamente y evaluado con inputs explicitos en common.
FR-025/026/029/038/039; NFR-009/010/016/019/021/022/023/025/028.
No fuente de inputs desde actor, continuidad autoritativa, renderer root/flags,
material, selectors originales/familias generales ni FULL. No bindings de especies
productivos nuevos ni cambios de solver/mounts. Baseline general permanece abierto.

## 2. Plan IMPLEMENTER

- [x] I1 S00 previo: reusar S56 compiler/DAG solo scoped; auditar Resources,
  engines registrados, deep validation, WorldAnatomyCatalog transaction/prepared
  ownership y AnatomyCatalogTransfer integrity/bounds/cache/negotiation antes de
  modificar produccion. Clean-room y convergencia independiente obligatorios.
- [x] I2 NumericPoseProgram.validatedCopy reconstruye cada DTO/nodo/write y
  geometria/source9 por constructors; comprueba caps antes de copiar nested lists.
  Formato/topologia/caps/bind exacto intactos.
  NumericPoseEngine common exige unico parametro program, resource revision-local,
  requiredChannels declarados incluyen todos INPUT y max64; direct evaluate sin
  revision ausente. Recursos default numericProgram vacio preserva SAM externo.
  Bound retiene nodos/indices/fields iniciales, no geometria/cubos embebidos por
  binding: eliminar captura de todo NumericPoseProgram al habilitar vida de revision.
  Registrar scalebrews:numeric_program una vez bajo ownership reservado existente.
- [x] I3 WorldAnatomyCatalog posee mapa inmutable numericPosePrograms, overloads
  anteriores delegan mapa vacio. Leer directorio scalebrews/numeric_pose_programs
  bounded4096/bytes actuales; canonical IDs/deep validation antes de prepare/swap.
  Resources solo revision actual; bind al modelo exacto, missing/malformed/foreign
  rechaza candidato preservando snapshot+prepared bundle anterior. No lookup hot.
- [x] I4 Bundle y transfer llevan numeric_pose_programs sorted y validados,
  obligatorios en protocolo7; bump PROTOCOL_VERSION7 y capability aditiva
  NUMERIC_POSE_PROGRAMS. Datos schema1 sin otra semantica. Overloads anteriores
  conservan source compatibility. Decode valida antes de swap, integrity/fragment
  caps/cache por revision/epoch igual. Peer viejo/unknown capability se rechaza.
- [x] I5 Desarrollo: build -x runGameTest, compileGametestJava y lane server focal
  catalog/binding/transfer/negotiation; replay DTO originales S56 (datos publicos)
  a traves del nuevo engine, revision isolation/rollback/malformed/missing/bounds/
  byte reproducibility/cache y resources SAM control. No Tiny/fullsuite ajenos.
  Los asserts historicos de generacion6 deben reflejar la evolucion7 de contrato
  conservando sus controles; el ADVERSARY posee correcciones de sus propios tests.
  Un consumidor JVM con veto cliente prueba uso common de la nueva ruta.
- [x] I6 Campana independiente posterior, sensibilidad/coste y snapshots si
  aporta senal espacial nueva; dos reviews completas cero cambios, receipts raw,
  canon/VALIDATION y commit local Sprint:S57 antes de avanzar.

## 3. Revisiones y coste previo

Primera pasada: revision-local engine/DTO/binding, deep validation de nested
records, IDs/caps/required channels, rollback/prepared lifetime, protocolevolution.
Segunda pasada: overloads/SAM y callers actuales, integrity/hash determinista,
resource cap/transfer aggregate, warm reuse, no actorauthority ni FULL implicitos.
Relectura explicito cap antes de copiar y mantenimiento de asserts historicos de
generacion; dos pasadas completas adicionales de scope/deps/ownership/compat/
ausencia/coste no piden otros cambios. S00/modelo independiente pendientes.
Produccion sin tocar hasta cierre S00 independiente de seccion4, comprobado raw
por IMPLEMENTER antes de empezar I2-I4.

Primera revision de implementacion detecta que el Bound S56 capturaba this y
retenia tambien geometria/cubos. En S57, con muchos bindings preparados por
revision, esa retencion duplicaria geometria validada por binding. Se cambia
solo ownership de la lambda a nodos/indices/fields iniciales; operaciones float
y ausencia intactas. Dos revisiones completas del plan ajustado no piden mas
cambios; se repite bateria afectada despues del cambio, sin suites ajenas.

PREPARATION: copia/validacion O(programs*(geometry+nodes+writes)), limite4096
recursos por mapa y MAX_BYTES en lectura/bundle; max512nodes/512writes/64inputs,
512parts/4096cubes por programa. Geometria embebida duplicada en bundle declarada,
acotada por bytes globales; no compresion/optimizacion inferida. Catalog bind puede
resolver dos veces por validacion/preparacion como callers actuales; registrar
coste y no introducir lookup/parsing/reflection/hash en evaluate. Transfer serializa/
hash/fragments una vez por revision, packets reuse por epoch/revision. Hot evaluation
usa Bound preparado y S56 scratch/matrices O(nodes+written parts), asignaciones
medidas no cero; no claim TPS. No cache global nueva ni modelo cliente retenido.

## 4. Modelo/convergencia/S00 - ADVERSARY

Modelo previo clean-room construido desde canon tras S56, antes de plan/tests
futuros. Tesis coherente: enlazar un DTO numerico validado con una revision
aceptada y un PoseEngine reusable common. Identidad de recurso/programa,
geometria exacta, schema INPUT, revision y binding tienen owners distintos; un
ID coincidente o programa evaluable no concede autoridad de actor/renderer.
La seleccion es declarativa, sin Java de especie, registro de material ni FULL.
Cambiar protocolo preserva version/capabilities/schema y rechazo de peer viejo;
una capability aditiva no implica compatibilidad con una version incompatible.

Invariantes previos: nested DTO valida antes de prepare/swap; caps antes de
copias; requiredChannels incluye todos los INPUT, max64; resources son solo
revision-local y Bound retiene esa seleccion, sin registry global ni lookup hot.
Un candidato malformado/missing/foreign deja snapshot y prepared bundle anterior
identicos. Canonical IDs/orden estable y framing reproducible no deben aceptar
alias ni cambiar identidad por orden de mapa. Ninguna ausencia devuelve rest
inventado. Old overloads y SAM externo conservan contrato anterior.

S00 pertinente ejecutado antes de produccion nueva. JVM fresca con veto de
Minecraft/Scale cliente verifica Resources SAM, registry duplicate/exact owner,
built-ins idempotentes y propietario reservado incompatible, nested constructor
validation, snapshot inmutable/detached, transaction rollback, transfer digest,
replay, rechazo peer viejo/unknown capability y codec real RegistryFriendlyByteBuf.
Cien consultas de paquetes de la misma epoch/revision conservan identidad cache.
Dos mutantes sobre source shadows de fundacion (swap prematuro y digest omitido)
son killed por AssertionError semantico, no por compilacion ni parsing accidental;
el segundo usa JSON aun valido. Receipt/logs/source exactos en
`docs/evidence/S57/adversarial-s00-verification.json`.

ADVERSARY_FIXTURE: primer probe esperaba capturar rechazo despues del Gson;
SourcePose record ya rechazaba escala degenerada en deserializacion. Oracle
corregido para observar ese rechazo y usar un ID invalido en la prueba de rollback;
log rojo conservado. Ningun defecto productivo ni tolerancia rebajada.

Fundaciones reusadas TRUSTED scoped: S56 numeric/geometry/reset seam; Resources
SAM y reserved registry; snapshot+prepared transaction; codec/integrity/cache
protocol6 existente. Las extensiones numeric resource/engine/protocol7 permanecen
UNPROVEN hasta candidata/campana: no se certifica anticipadamente codigo nuevo.
No fundacion REWORK/REPLACE bloqueante. Caps de map4096, bytes16MiB y fragments
16384/1024 existentes auditados; lectura limita characters por directorio, el
bundle limita UTF8 bytes total. Copias/validaciones repetidas y geometria embebida
se contabilizan PREPARATION. Evaluate usara Bound, sin parse/hash/reflection ni
consulta revision; scratch por llamada no se presume cero alloc. No claim TPS.

Dos pasadas completas modelo/plan/S00 convergen sin ajustes adicionales. S00
pertinente cerrado, funciona/continua a implementar I1-I6 sobre esta tesis; 376
hashes productivos permanecen exactos al cierre S56. Campana concreta/holdouts
solo posteriores a candidata y privados. No Tiny ni suites ajenas. G6 completo,
actor/root/visibility/material/FULL y G7-G9 permanecen abiertos.

## 5. Implementacion - IMPLEMENTER

I2-I4 implementadas solo despues del S00 pertinente cerrado y raw-verificado:
recibo SHA81bbab00892551d213efe3bfc5f63a6b2f92c3625e7af827d3b05a1e6234f917,
14 artefactos y376 hashes anteriores intactos antes de produccion. Nuevo engine
common y recursos revision-local, nested copies bounded antes de bind/swap,
overloads y SAM anteriores preservados, data-backed missing/foreign rechaza,
bundle numeric sorted obligatorio/protocolo7/capability10 aditiva. Sin actor source
ni nuevos bindings productivos. Metodo evaluate numeric pasa a static y lambda
captura solo nodos/indices/initial9 tras review de retencion; no cambia float math.

Compilacion inicial PASS52s. Primera invocacion build/GameTest compile PASS56s
excluyo runGameTest con -x aunque el task aparecia explicitamente: clasificacion
IMPLEMENTER_INVOCATION, no prueba servidor contabilizada. Corregida invocacion
separada runGameTest:36/36 PASS1m23s y cierre limpio, incluyendo cinco nuevos
tests de revision/rollback/invalid/schema y replay84 matrices/12 muestras/2
programas originales capturados S56 por codec real y dos fragmentos. No se vuelve
a ejecutar cliente original porque programa/captura S56 no cambia. Fixture
empacado en GameTest resources permite tambien ejecucion CI ordinaria sin paths
locales. Replay JVM client-veto PASS4.8s, Resource lookup solo bind y SAM vacio.
Tras ajuste final de retencion, build+compile PASS13s y common replay PASS4.8s;
segunda bateria servidor pertinente PASS1m5s36/36 y cierre limpio. Dos lecturas completas de ownership,
revision/bind/float/ausencia/schema/protocol/caps/coste no piden mas cambios.

## 6. Verificacion y fallos - ADVERSARY

Funciona, continua scoped. Campana independiente sobre los 377 hashes finales:
370 owners S56 byte intactos, seis modificados y NumericPoseEngine nuevo. Sin
reparaciones productivas ADVERSARY. Receipt final en
`docs/evidence/S57/adversarial-verification.json`, receta reusable en
`docs/evidence/S57/adversarial/README.md`; S00 previo conserva su recibo separado.

Lane servidor propia ejecuta solo las tres fronteras S18 afectadas: PASS16/16,
BUILD SUCCESSFUL4m15s y shutdown limpio. Migration de fixture S18 cambia version
6 a7, conserva sus controles y anade rechazo6/mapa numeric obligatorio. No Tiny,
cliente completo ni baterias ajenas. Parent reproduce16/16 limpio2m25s.

Common publico con veto de todas las clases cliente: cuatro programas originales
capturados S56,140 matrices nativas,100314 asserts, codec real y reconstruccion
catalog prepared. DTO recibido exactamente igual; replay parent6.6s byte igual.
Missing/foreign/malformed/revision/required-input/schema/SAM/alias/order/caps/
rollback/copy/cache pasan.4097 archivos rechazados antes de abrir ninguno;4096
programas aceptados.100k evaluaciones sin resource lookup; closure del Bound no
retiene geometria/programa/resources. JAR real verificado789 entradas y586 clases
GameTest comparadas sin leak; hash igual al recibido.

Sensibilidad8/8 kills semanticos: required-schema, required-sample, hot-resource,
exact-model, deep-copy, peer antiguo, packet-cache, canonical-alias. Siete kills
validos del primer driver y uno del replay focal corregido. ADVER_TOOLING: el
primer mutante de constante protocol no era valido por inline de callers ya
compilados; agregado inicial exit1 conservado, no contado como kill. Solo se
repitio el guard peer antiguo, AssertionError wire/schema y driver exit0. Dos
rojos de setup propios (var compuesto e engine test-only no registrado) corregidos
antes de medir; logs conservados, sin tolerancias/oraculos rebajados.

Coste PREPARATION O(geometry+nodes+writes+bindings), caps antes de copias; validacion
repetida, dos binds y geometria embebida se contabilizan. Evaluate captura solo
nodos/indices/initial arrays, sin parse/hash/reflection/resource nuevo; scratch
O(nodes+written parts) heredado S56, no claim cero alloc ni TPS. Revision snapshot
y packets inmutables/cache por epoch preservados. Proyeccion determinista S56
reinspeccionada: envelope local identico, nunca renderer/root/material. Nueva
frontera certificada con140 matrices completas y byte equality wire.

Dos reviews completos convergen cero cambios productivos; fuentes, oraculos,
recibos y claims exactos. Cuatro fuentes one-off sustituidas retiradas por path
exacto tras preservar versiones ejecutadas; logs/classes/builds/cache/modelos
privados permanecen. Tooling publico reutilizable conservado. G6 general, actor
inputs/visibility/root/material/FULL y G7-G9 siguen abiertos.


## 7. Cierre

IMPLEMENTER verifica receipt final SHA6c17cdc806eed96de8955a5711099be56ff366e448a51b958ff80ca86a5fa4c0,
40 artefactos raw,377 hashes productivos y6 fuentes publicas de tooling/tests.
Reproduce lane publica common PASS6.6s y servidor16/16 PASS2m25s; DTO replay
byte identico. Los ocho logs muestran AssertionError semantico, sin repetir
sensibilidad ya verde. Manifest ordinario restaurado PASS9s,113server/34client,
igual al fuente. JAR789 entradas/586 clases GameTest sin leak y hash intacto.
Dos reviews completos finales de ownership/source/claims/evidencia no piden
cambios. Main b5e62535 refrescado ancestro, canon/VALIDATION coherentes.
Tooling publico reusable conservado, cuatro one-offs adversariales retirados
por sus paths exactos tras capturar evidencia; sin eliminar caches/worlds.
Commit local propio con trailer Sprint:S57; INDEX y HEAD deben conservar todos
los bytes raw de evidencia. G6 general/G7-G9 y asuntos humanos al final de epica
abiertos, sin actor/root/flags/material/selectors originales ni FULL nuevos.
