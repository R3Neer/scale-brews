# S59 - Programas originales con decisiones booleanas finitas

Estado: CERRADO adversarialmente scoped; commit local propio Sprint:S59 antes de avanzar.
IMPLEMENTER raiz; ADVERSARY independiente. Base0c4298e8 S58 cerrado y committed;
main b5e62535 vigente/ancestro. Sin produccion nueva antes de cerrar S00.

## 1. Tesis y scope

G6 tareas2-3/6, FR-025/026/029, NFR-019/020/028/029: preparar una familia original
de setupAnim con decisiones sobre campos booleanos del estado como una tabla
finita de predicados y programas NumericPoseProgram existentes. Cada programa
representa exactamente el camino original para esa asignacion; Java no contiene
formulas ni dispatch de especie. Todos los caminos/instrucciones deben quedar
verificados y el modelo compartido permanece intacto.

Es PREPARATION explicita. Los predicados describen owner/name/Z de render-state;
no prueban un source desde actor ni instalan un selector/binding fisico. No nuevo
formato numeric/API/protocolo ni engine runtime: los programas neutrales usan la
familia S56/S57 actual. El consumidor recibe una fila explicitamente certificada.
No bool derivado de floats con threshold, loops, switches, handlers, comparacion
arbitraria, llamadas nuevas, flags visible/skipDraw, keyframes, root/material,
renderer/world, actualizacion de actor ni familias FULL.

## 2. Plan IMPLEMENTER

- [x] I1 Cerrar S00 pertinente antes de produccion: Code/labels y targets,
  bounds/consumo completo, bridge/super/reset, identidad de partes/fields,
  snapshot inicial y numerica common. Reutilizar pruebas vigentes scoped cuando
  los owners/contratos sean exactos; auditar el nuevo dominio de flujo por
  separado, sin Tiny ni suites ajenas ni repetir sensibilidad verde.
- [x] I2 Compartir la maquinaria del compiler S58 mediante un seam package-private
  de preparacion. La API OriginalLinearPosePreparation.inspect conserva version
  v2 y rechazo de branches/booleanos. El pass finito comparte resources bounded
  y una unica geometria/reset/part identity; no copia alternativa de algoritmos.
  Verificar la estructura compartida al terminar el pass. Resultados inmutables
  no retienen model/parts/resources loader/world.
- [x] I3 Nuevo OriginalFinitePosePreparation: descubrimiento acotado de campos Z
  originales efectivamente usados por el programa completo, orden canonico de
  owner/name/descriptor y max5 booleanos/32 filas. Expandir asignaciones hasta
  converger; cada fila cubre la asignacion completa y emite la numerica float
  original, respetando INITIAL/read-after-write/alias y rounding. No default
  para bool desconocido. INPUT float conserva schema propietario por fila.
  Aceptar solo IFEQ/IFNE sobre booleano tipado y GOTO estrictamente forward;
  targets validos, stack/locals/calls/512instructions/nodes existentes y
  presupuesto total1048576 instrucciones verificadas por pass (descubrimiento
  y emision incluidos), comprobado antes de admitir el siguiente metodo/fila.
  Validar todos los targets/instrucciones y cubrir el programa completo entre
  las filas, incluido RETURN y sufijos; no ignorar trabajo desconocido porque
  un camino no lo visita. Un fallo invalida el resultado entero, sin prefijo.
  Deduplicar programas numericos equivalentes y compartir geometria inicial;
  provenance original-finite-pose-v1 y digest incluyen schema/predicados/recursos
  exactos y rows canonicas. No tabla particular de una especie.
- [x] I4 Oracle propio con setupAnim original real que usa esta tecnologia,
  combinaciones booleanas completas y estados float/initial/current variados,
  matrices completas y corners de piezas materiales presentes, pureza y
  determinismo antes/despues de animar. BabyTurtleModel original es witness;
  sus primitivas visuales sin grosor siguen la exclusion tecnica vigente, no
  se convierten en piezas materiales. AdultTurtleModel con write visible queda
  rechazado entero, no se omite ese trabajo para admitir la parte numerica.
  Cow/Pig/Sheep previamente probados son regresion del seam modificado; elegir
  una lane existente pertinente, sin repetir todas sus campaÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â±as.
- [x] I5 Build/compile GameTests, cliente focal con originals y consumer neutral
  con veto cliente sobre captura nueva. Snapshots deterministas espaciales
  comparables. Restaurar manifest normal. No tests server/catalog/protocolo
  intactos, Tiny/full client ajenos ni mutaciones verdes repetidas.
- [x] I6 Campana independiente posterior a candidata, clasificar fallos,
  dos reviews completas finales sin cambios, recibos raw/canon/VALIDATION y
  commit local propio Sprint:S59, HEAD/working-tree antes de avanzar.

## 3. Revisiones y coste previo - IMPLEMENTER

Primera revision completa: el bytecode original TurtleModel tiene ramas Z
forward y expresiones float/ternarias, todas dentro del scope propuesto. Su
subclase Baby hereda ese setupAnim; Adult aÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â±ade visible y su resultado completo
queda fuera. Las cuatro asignaciones Z cubren las ramas de tierra/agua/puesta
sin codificar esas formulas. Geometria de planos visuales ya se excluye por
la regla existente del extractor; no se amplia el dominio de convexos.

Revision de presupuesto: se fija1048576 instrucciones verificadas para todo
el pass, incluyendo descubrimiento y emision; no solo un limite por fila.

Segunda y tercera revisiones completas tras fijar ese limite: especializar
caminos en PREPARATION reutiliza el
NumericPoseProgram actual y conserva semantica de operaciones no ejecutadas,
sin introducir SELECT eager ni un evaluator paralelo. El limite5/32 y la
cobertura/validacion de todos los targets evitan explosion, loops y sufijos
ocultos. Un bool sin source no da autoridad fisica por tener metadata. Recursos,
reset y estructura se comparten en el pass; resultados no guardan referencias
cliente. Public linear estricto, protocolos/owners common intactos. Plan leido
contra requisitos, ownership, callers, ausencia y coste: cero cambios pendientes.

OFFLINE/PREPARATION: B<=5, R=2^B<=32, instrucciones por metodo<=512 y calls<=16;
trabajo total<=1048576 instrucciones verificadas por pass, con recursos64clases/8MiB y
arbol512parts/4096cubos compartidos. Una geometria inicial, programs equivalentes
internados; scratch independiente por fila, sin estado mutable en resultado.
No caller hot ni nueva busqueda/parse/hash por tick. Evaluacion runtime sigue
siendo NumericPoseProgram ya existente, sin claim zero allocation/TPS. Hash y
snapshot son tooling offline; resultados/timings solo tras ejecutar.

## 4. Modelo/convergencia/S00 - ADVERSARY

Modelo clean-room previo post-S58 SHAe976e9066bc0d1374f1e6ec0bd84782aa08ed8e35a3530146adbfe071ce1f97d,
derivado del canon antes de este plan. Tesis pertinente: decisiones finitas solo
sobre booleanos tipados originales, particion completa de asignaciones y ausencia
atomica si falta cualquier primitive/path/target/flag. El camino no ejecutado no
se evalua eager; identidad/INITIAL/alias/orden numerico permanecen originales.
Descubrimiento y emision comparten recursos/geometria y presupuesto total;5/32
antes de expansion. Predicados canonicos y provenance no certifican fuente desde
actor, renderer/world root, inclusion/layers/material ni FULL.

S00 pertinente cerrado: JVM fresca sobre Code real,15 asserts de labels/ordinales,
IFEQ/IFNE/GOTO, targets atrasados/finales expuestos al futuro verifier, missing,
opcode, suffix/truncation,512/513, handlers y caps recursos. Un shadow target+1
compila y muere por AssertionError semantico. No inicia Minecraft ni pretende
veto cliente; classpath cliente solo enlaza.24 clases de foundations compiladas
son byte exactas al JAR S58. Reset/estructura/float/DUP/alias/read-after-write y
common se reutilizan exclusivamente desde recibos scoped con owners exactos,
sin repetir sensibilidades verdes. TRUSTED esos cimientos; el nuevo verifier,
descubrimiento booleano y budget son trabajo UNPROVEN de la candidata, no un
fundamento existente concedido por el plan. No REWORK/REPLACE bloqueante observado.

Tres intentos ADVER_FIXTURE preservados: JDK rechaza target al final del code;
la alternativa sin stackmaps pierde metadata de labels; JDK parchea NOP inalcanzable.
Control final usa classfile valido e inyeccion explicita del mapa para end target,
y suffix independiente del opcode parcheado. Sin cambiar produccion ni criterios
de forward/range/whole-program que debera comprobar el compiler nuevo.
Recibo raw: `docs/evidence/S59/adversarial-s00-verification.json` y receta en
`docs/evidence/S59/adversarial-s00/README.md`.

Dos pasadas completas modelo/plan/coste convergen sin cambio requerido: budget
1048576 incluye discovery/emission antes de admitir siguiente trabajo; caps por
metodo/calls/resources siguen aplicables. Parsing/reflection/copia/hash son
PREPARATION bounded, no caller hot ni propiedad TPS/zero-allocation. La API lineal
estricta v2 y common/protocolo intactos conservan negativos; regresion focal del
seam modificado y captura original/common nueva seran pertinentes despues de
candidata. Sin Tiny ni server/catalog/protocolo/full client ajenos. Funciona,
continua I1-I6;377 hashes productivos intactos. G6 general/G7-G9 abiertos.

## 5. Implementacion - IMPLEMENTER

Candidata congelada: OriginalFinitePosePreparation compila tablas completas de
predicados Z originales en NumericPoseProgram branch-free existentes. El pass
comparte Code/labels con cursor propio, resource pool/reset/geometria y coverage
union; valida todos los targets forward/range, solo IFEQ/IFNE/GOTO tipados.
Descubrimiento sin default hasta5 campos/32filas y63 intentos; work1048576 antes
de admitir metodo en discovery o emision. Filas equivalentes internadas y una
geometria compartida; resultados neutrales inmutables sin modelos/classes/fields.
La API lineal v2 permanece estricta y la captura S58 es semanticamente exacta.

Original BabyTurtleModel,4filas Z y CowModel,1fila sin Z;20samples/140matrices/
72cubos materiales PASS30s salida limpia. Todas las matrices, tambien partes
visuales de grosor cero, comprobadas; planos no convierten en nuevos convexos.
AdultTurtleModel rechazado completo por escritura visible, sin alterar modelo.
Regresion S58 focal84matrices/72cubos, sin Tiny ni fullclient/server ajenos.
Consumer generico S57 sobre captura S59 PASS3.58s,140matrices, resource bindOnly,
veto cliente y oldSAM empty;5 filas de2 clases, no cinco modelos ni fuente actor.
Build primero40s y final3s tras bookkeeping de coverage; restore1s, manifest
semanticamente exacto113server/34client. Snapshots espaciales y campana pendientes.

378 archivos productivos:262Java/116resources,374 anteriores byte intactos,
tres owners cliente modificados y uno nuevo. JAR798entries/1340907bytes SHA
06f67bf5ac09950e2fde72709303bf27632dd2c3f9901b2fef90d990d9598a48.
581/592 clases previas byte intactas;11 cambiadas exclusivamente de esos tres
owners,9 nuevas exclusivamente de finite/linear.595 GameTestclasses sin leak.
Dos reviews completas cero cambios pendientes; sin caller hot ni claim TPS.
IMPLEMENTER_TOOLING al construir receipt: confusion Java/total en hash map,
corregida contando recursos aparte; no fallo productivo ni repetir tests.
Recibo docs/evidence/S59/implementation-verification.json SHA
83f1cacd9b6bbb9ae8b9a6396c93914f227100e88ecabf6569f7d36f170b3058.


RED1 reparado por IMPLEMENTER: work agotado se rechaza antes de resources;
el count se carga antes de verificar targets e insertar codes/coverage. Build5s,
probe public verde work1048576/cache0/coverage0 y native28s140/72 mas S58focal84/72.
Capture programs/tables/samples/matrices semanticamente exactas a candidata
previa: no repetir common ya verde con engine intacto. Restore1s exact113/34.
Primer replay publico sin client jar fallo IMPLEMENTER_TOOLING antes de oracle;
classpath corregido con clientOnly26.2 original, oracle intacto pasa, raw conservado.
Recibo actualizado REPAIRED_CANDIDATE_PASS_PENDING_ADVERSARY y candidato previo
raw preservado. Dos reviews completas propias tras reparacion cero cambios.

Reproduccion propia de lane publica focal PASS38s salida limpia:4asignaciones
por2aliases por5inputs,40samples/280matrices/80cubos; seam S58afectado140/120.
Consumer publico PASS3.57s8programas/280matrices clientVeto. Snapshot offline
PASS1.24s, PNGs inspeccionados: envolventes materiales locales y joint bases,
ocho filas con centros N/C explicitos. Los planos visuales se muestran solo por
bases de articulacion, sin conceder material convex/root/GPU. Restore5s manifest
113/34 exacto, GameTestclasses sin leak; fuente/JAR congelados intactos.

## 6. Verificacion y fallos - ADVERSARY

RED1 PRODUCTION/PREPARATION_COST reproducible: FinitePass.code crea/valida e
inserta el template y coverage antes de comprobar el presupuesto global work.
Con work=1048576 agotado, la llamada rechaza finite_work_limit pero ya admitio
una nueva entrada en codes y coverage (1/1). Contradice I3: comprobar antes de
admitir el siguiente metodo/fila, no despues de verificar sus instrucciones.
Probe JVM local sobre el owner real con seed explicito del borde interno;
compile exit0, JVM exit1 por AssertionError semantico. No se atribuye fallo
fisico/Minecraft ni incremento de memoria indefinido. Fuente/hash y logs raw en
`docs/evidence/S59/adversarial-red1/verification.json`; oracle intacto para repetir.
RED1 reparado por IMPLEMENTER; el mismo oracle intacto PASSwork1048576,
codes0/coverage0. Snapshot/clases/source nuevo contrastados:601 clases productivas
compiladas propias byte iguales al JAR actual;378 archivos=262Java+116resources,
374 anteriores intactos,596 clases GameTest sin leak en798 entradas JAR.

Funciona scoped tras dos revisiones completas independientes sin reparacion
adicional requerida. Lane nativa original PASS38s limpia: cuatro asignaciones,
dos aliases, cinco estados float=40 muestras/280 matrices/80 cubos. API linear y
reset siguen estrictas; Adult visible rechazado entero, metadata repeat y pureza
antes/despues de setupAnim pasan. S58 pertinente140/120; captura JSON completa
igual a baseline S58. JVM boundaries15 asserts y cinco kills semanticos nuevos;
branch-direction nativo sexto kill semantico (exit1/56s, PS agregado0). No
repeticion de sensibilidades verdes ajenas. Consumer nuevo8 programas/280 matrices
con veto cliente. Source/schema/resources compartidos y coverage/targets/cursors/
budget quedan TRUSTED solo en esta frontera, sin autoridad de actor.

ADVER_FIXTURE conservado: primer RED1m7s construyo NaN Inputs,
el constructor los rechaza correctamente antes de bound.evaluate; el oracle ahora
comprueba ese mismo rechazo en la frontera real. Matrices3e-6 y forward-error
corners S58 intactos, sin produccion reparada por ese fallo de test. Logs/source
anteriores preservados. Snapshot offline inspeccionado: ocho filas de envolventes
materiales y bases articuladas con centros N/C; muestra branch/alias de piernas
planas excluidas sin convertirlas en colliders. No GPU/world root ni layer claim.
Reproduccion IMPLEMENTER native38s/common3.57s/snapshots1.24s limpia, manifest
113server/34client restaurado. Son totales de comando, no CPU/TPS/zero allocation.

Recibo `docs/evidence/S59/adversarial-verification.json` y receta reusable en
`docs/evidence/S59/adversarial/README.md`. Seis kills validos, evidencia byte exacta,
UTF-8/AST/diff check comprobados; one-offs sustituidos se retiran tras verificar
copias durables, conservando logs/caches/builds/worlds. ADVERSARY no toca produccion.
Tooling PS final retira boilerplate inactivo fingerprint/instrumented; receta y
branch-direction siguen iguales. Parser y hashes verificados; recibo conserva
SHA ejecutado y final. Sin repetir el mutante verde por limpieza sin semantica.
No Tiny/fullclient/server/catalog/protocolo ajenos. G6 general/G7-G9 siguen abiertos.

## 7. Cierre

S59 cerrado scoped: implementacion y campana convergidas; RED1 de coste reparado
con mismo oracle; seis kills nuevos validos y15asserts, native280/80 y common280
clientVeto. Reproduccion publica independiente propia verde y snapshots comparados.
Dos reviews finales completas propias y dos independientes cero cambios adicionales.
Foundation Code fork/targets/coverage/work y finite PREPARATION TRUSTED en este scope.
Boilerplate PS retirado sin semantica; no nuevo rerun verde. Tooling reusable aislado
bajo test/tools, one-offs sustituidos retirados con evidencia durable; builds/caches/
worlds preservados. Recibos raw byte exactos; main vigente b5e62535 ancestro.

Commit local propio Sprint:S59 con comprobacion INDEX/HEAD/raw y arbol limpio
antes de siguiente sprint. Baseline93Living/92UNRESOLVED+1fixtureSAFE_PARTIAL+0FULL
intacta; sin fuente actor, renderer/world root, flags/layers/material/selectores
fisicos ni bindings/FULL. G6 tareas2-6/G7-G9 abiertos; asuntos humanos al final
de epica. No Tiny ni suites ajenas repetidas.
