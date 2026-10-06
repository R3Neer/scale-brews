# S61 - Fuente comun de inputs del extractor Living original

Estado: cerrado adversarialmente scoped; fuente supplied partial1 TRUSTED solo en esta frontera.
IMPLEMENTER raiz / ADVERSARY separado. Base2799687c S60 cerrado, arbol limpio;
main b5e62535 actual/ancestro, sin cambios ajenos requeridos.

## 1. Tesis y limites

G6 tareas2-3; FR-025/026/027/028/029 y NFR-006/009/019/025/028/029.
Un schema de inputs base original probado puede prepararse como lector common
del actor exacto y compararse con extractRenderState nativo a partialTicks=1.
No se deduce equivalencia por nombres headYaw/yRot ni se leen renderers en server.

Se admite la tecnologia original enum/edad o enum/modelo unico ya probada por
S51/S53. Su proof completo de extractor/bridges permite demostrar que la captura
typed delega al extractor Living y no sustituye los campos base solicitados.
El sampler reproduce solo el vocabulario base del programa original 26.2 fijado:
ageInTicks, walkAnimationPos/Speed, xRot/yRot/bodyRot, scale/ageScale y flags
isBaby/isInWater/isUpsideDown/isFullyFrozen/isAutoSpinAttack. Campos fuera de ese
vocabulario quedan ausentes, no se convierten en canales por coincidencia nominal.

El reloj leido es entity.walkAnimation y nunca se avanza: se certifica la
extraccion del estado suministrado, NO su evolucion autoritativa en el servidor.
AuthorityPoseTracker y su reloj/transport no se modifican ni se presumen
equivalentes. Sin registry/packet/catalog hooks, physical binding, renderer root,
layers/material inclusion, continuidad, transformed bytecode o FULL. Tipos/
schemas nuevos de esta tecnologia se conectan por datos, sin dispatch por especie.

## 2. Plan IMPLEMENTER

- [x] I1 S00 scoped: auditar cold original class resources/proofs S51/S53,
  schema/export S60 y SPI common; verificar owners/evidencia pertinente vigentes.
  No repetir pruebas historicas verdes ni Tiny ajeno. Clock evolution, nuevo
  sampler y nuevo proof son UNPROVEN; ninguno es dependencia preexistente asumida.
- [x] I2 Common LivingPoseInputSource: vocabulario de fuentes enumerado y schema
  <=64 canales unicos bounded; datos inmutables/revalidacion antes de bind.
  Bind captura actor Class exacto y arrays privados. Sample read-only a partial1,
  sin clase cliente/parser/hash/reflection/registry/locks ni caches globales.
  Rotaciones conservan rotLerp/wrap/clamp y orden float original, incluido
  vehicle Living; upside-down usa nombre original y multiplica x/y por -1f.
  Walk solo alive && !passenger, getter partial1; age usa I2F+1f. Flags +0/1.
  Evaluar solo fuentes solicitadas; invalidez/fallo no publica prefijo ni default.
  Actor null/otra clase, NaN/inf de input requerido o excepcion local -> ausencia.
- [x] I3 OriginalLivingInputPreparation cliente cold explicita: schema tipado
  owner/name/F o Z -> Source common; resolver fields reales por identidad sin
  shadow/alias, nombre de channel acotado/unico. Reusar proofs completos
  enum/edad y enum/modelo unico; fijar EntityRenderer/LivingEntityRenderer/Mth
  resources originales auditados y registrar recursos/getters comunes usados.
  Guardar dispatch original de extractNameTags (ambos descriptores Entity/Living)
  porque el extractor base pasa el estado a ese helper virtual.
  Missing/unsupported/overrides/foreign origin -> ausencia total. Campos base
  previamente capturados no pueden ser sobrescritos por la captura typed.
  Receta inmutable/reproducible vincula renderer, exactActor, state, schema,
  fuentes, partial1 y hashes. Inputs/exporters previos no cambian de contrato.
- [x] I4 Pruebas propias nativas: actores reales y renderers originales de al
  menos dos tipos usando una fuente comun; schema real S60/compilacion existente
  donde admitido. Comparar bits de todos los inputs y matrices originales en
  estados fijados, con snapshots locales cuando aporten observabilidad.
  Capturar recetas/actor inputs/oracle matrices para replay common con veto
  cliente; source no retiene renderer/model/state. No sintesis equivalente usada
  como oracle original. Negativos de schema/actor/unsupported/missing/nonfinite.
- [x] I5 Build/compilar GameTests y lane cliente focal nueva; regresion S51/S53
  solo si se modifica su proof compartido. Sin fullserver/fullclient/Tiny ni
  repetir mutaciones antiguas verdes. Campana independiente posterior a candidata,
  clasificar antes de reparar y repetir toda bateria afectada.
- [x] I6 Dos reviews finales completas cero cambios, coste y raw receipts,
  canon/VALIDATION, cleanup exacto, commit local propio Sprint:S61; comprobar
  INDEX/HEAD raw y arbol antes de avanzar. G6 general/G7-G9 y QA humana final abiertos.

## 3. Investigacion/revisiones y coste - IMPLEMENTER

Original Living extract aplica head rotLerp, solveBodyRot (Living vehicle y
clamp85/correccion50), wrap, getXRot(partial), inversion y walk gating. El
tracker existente calcula otro vocabulario/reloj y no sirve como prueba por alias.
Cow typed delega al Living completo y captura Holder variant sin tocar esos
campos; el proof compartido ya audita el resto del programa y ambos bridges.
Las capturas enum/modelo unico pueden escribir float fields: se debe comprobar
explicitamente que ninguno sobreescribe los inputs base solicitados, incluso
cuando el nombre simbolico de un field no es su declaring owner real.

Revision1 amplio originalLiving sampler al vocabulario base explicitado y fijo
partial1; no reloj nuevo ni sampler automatico. Revision2 exige projection por
identidad de field, comprobacion de todas escrituras typed y evaluar solo fuentes
solicitadas. Relectura completa de requisitos/ownership/callers/ausencia/costes/
pruebas/limites: cero cambios pendientes de plan; produccion intacta.

PREPARATION acotada por recursos/methods/instrucciones de OriginalModelPrograms
(64 classes,8MiB total,1MiB/class,512 instructions/method) y schema64. Cold reflect/
hash/linking permitido; no introspeccion hot. Sample O(C<=64), una lectura local
por fuente (derivados de orientacion compartidos si necesarios); scratch fijo
del vocabulario, staged map,
boxing y copia inmutable O(C) declarados, no zero-allocation/TPS claim. Sin scans,
trabajo por observer, locks/global caches ni avance de animacion. La cota O(C) describe trabajo propio de proyeccion: getters delegados y
Component.getString conservan su coste original dependiente del dato del actor;
la conversion del nombre se ejecuta como maximo una vez por muestra. No se
certifica un presupuesto temporal para getters/componentes ajenos. No hot caller
nuevo en este sprint; futura integracion debera respetar once/authority sample.

## 4. Modelo/convergencia/S00 - ADVERSARY

Rol activo ADVERSARY. Modelo clean-room previo post-S60, formado antes del plan:
SHA256 6ad9813d37960125919f5ceb097a6a5398665dd2fbeed5742b942b1c6540f3ef.
No tests ni casos concretos nuevos antes de candidata; holdouts futuros privados.

La cadena que exige el modelo es: identidad original completa del field/schema,
proof del programa de extraccion y sus llamadas que reciben state, fuente comun
probada para actor/instante exactos, inputs completos, programa numerico preparado
y joints locales. Ninguna flecha procede de un alias de channel, del nombre de
una especie, de un getter existente o de una formula sintetica equivalente.
La fuente debe preservar unidades, orden float, gating, orientacion y flags del
programa original admitido; ausencia requerida impide publicar toda la muestra.
Campos no solicitados no justifican lecturas ni fallos adicionales. Resolver
owner/name/descriptor conserva la identidad real del field ante herencia; todas
las escrituras y escapes de state de la ruta admitida deben quedar cubiertos.
El guard de extractNameTags en ambos descriptores forma parte de ese proof nuevo,
no de una confianza heredada por haber fijado la clase base.

partialTicks=1 limita este sprint a extraccion read-only del estado suministrado.
Leer entity.walkAnimation no demuestra su evolucion en servidor ni equivalencia
con AuthorityPoseTracker. No query avanza animacion, ninguna clase cliente es
necesaria en el consumidor comun y ninguna muestra conserva renderer/model/state.
Seleccion viva, scheduling autoritativo, continuidad tras reemplazo, root,
inclusion y material siguen separados. Esta etapa no habilita FULL ni cierra G6.

S00 scoped cerrado por reutilizacion verificada, no por repetir historicos:
381 fuentes/recursos productivos coinciden con el recibo final S60; los tres
owners del proof S53 conservan sus hashes; OriginalModelPrograms coincide con
la fuente reparada/certificada S59. Se comprobaron hashes y longitudes de
136 artefactos raw de los recibos S51/S53/S59/S60 (16/18/42/60). TRUSTED solo
los limites ya probados del parser acotado, dispatch/bridges/seleccion enum,
identidad/schema de export y evaluacion comun detached. El SPI es un contrato
existente; no se instala un adapter ni se importa su tracker como reloj original.
No queda fundamento preexistente bloqueante para esta extension.

UNPROVEN: lector nuevo; proof nuevo de EntityRenderer/LivingEntityRenderer/Mth,
getters y no-overwrite; compatibilidad de bytecode transformado; evolucion del
reloj servidor y continuidad; integracion autoritativa/root/material/FULL.
La candidata debe aportar prueba original nativa de extraccion y comparacion de
inputs/matrices, sin promover esas dependencias nuevas antes de su campana.
Las recetas y recursos exactos deben permitir distinguir dicha prueba original
de un replay comun alimentado con escalares ya capturados.

Coste esperado: PREPARATION bounded por parser y schema64; bind frio con arrays
privados; sample local O(C), scratch fijo del vocabulario, staging/boxing/copia
O(C) declarados. No reflection, parsing, hash, registry, locks, cache global,
scan de actores o trabajo por destinatario en sample. No claim zero-allocation
ni rendimiento TPS. El futuro caller autoritativo requerira su propia prueba.

Dos pasadas completas del plan vigente: primera sobre identidad/programa/ausencia
contra requisitos; segunda sobre ownership, escapes virtuales de state,
limites temporales, costes, evidencia y regresion. Ambas sin cambios requeridos.
Funciona, continua: solo convergencia previa/S00, no aceptacion de produccion.
Regresion posterior focal sobre source/proof y seams que cambien; sin Tiny,
full suites ajenas ni sensibilidades historicas verdes repetidas.

Evidencia durable: [recibo S00](../evidence/S61/adversarial-s00-verification.json)
y [auditoria de reutilizacion](../evidence/S61/foundation-reuse-audit.json).
Sin cambios productivos, compilaciones o GameTests nuevos en esta fase.

## 5. Implementacion - IMPLEMENTER

Dos nuevos owners: LivingPoseInputSource common (d9f9ab99) y
OriginalLivingInputPreparation cold client (c70f0c83). Los381 owners previos
siguen intactos. No actor sampler registrado, ni protocolo/catalog/tracker/mixin
modificado. Schema13fuentes/64canales; arrays privados exactActor; fuentes y
orientacion derivada memoizadas localmente; resultado completo inmutable o ausencia.
Proof original enum/edad o enum/unico, dispatch/nameTags/base writes por identidad
real de field, resources/getters registrados y receta reproducible.

Build inicial PASS58s; native final PASS59s: cuatro actores reales,8estados cada
uno,544escalares bit exactos y56matrices Cow originales. El modelo Cow es un
consumidor original representativo, no prueba del slot/render-root seleccionado.
Server focal PASS1m5s:2nuevos tests S61+harness always_pass=3;544escalares y
640relecturas stable, schema64/65/copia/ausencia. JVM nueva con veto cliente:
32bindings frios (sin ejecucion de actores),56matrices/delta1.1920928955078125e-7,
0lookups hot; replay usa consumer S60 intacto sobre captura nueva, no rerun de
capturas/sensibilidades historicas. Dos laminas8casos generadas y vistas: formas,
bases y centros N/C coinciden. Projection fija. Sin GPU/root/material claim.

Dos intentos nativos rojos se clasificaron como defectos de fixture: setXRot(NaN)
rechaza el input; despues getXRot(1) devuelve directamente pitch actual e ignora
pitch viejo. Final usa inyeccion del field actual exacto; demuestra NaN viejo
admitido a partial1 y NaN requerido actual rechaza muestra completa. Las dos
fuentes/logs fallidos se conservan. Ningun cambio de produccion por esos fallos.

Recibo propio [verification](../evidence/S61/implementation/verification.json).
La lectura del programa original Living confirma que aiStep solo invoca
calculateEntityAnimation en Level.isClientSide. Leer walkAnimation supplied no
resuelve el reloj dedicado. El mixin vigente updateWalkAnimation modifica
vanillaTimeScale mediante ScaleGait: futura integracion debe preservar ese
comportamiento de main, sin copiar el reloj del tracker por alias. Es investigacion
de un limite pendiente, no implementacion ni prueba de clock authority.

Campana independiente y dos reviews propias completas finalizadas sin cambios
productivos pendientes. Build final29s y restore9s PASS:617classes/JAR exactos,
607previas intactas y613GameTests sin leak;383owners/381previos intactos. No se
promueve la evolucion del reloj servidor ni cambia92UNRESOLVED/0FULL.

## 6. Verificacion/fallos - ADVERSARY

Funciona, continua: aceptacion scoped de lectura de estado supplied a partial=1
con las dos fuentes congeladas d9f9ab99/c70f0c83. Dos pasadas finales de
codigo/ownership/metadata/coste y evidencia no requieren cambios productivos.
Los 381 owners anteriores permanecen byte intactos; 383 actuales verificados.

Lane nativa independiente PASS80s/exit0/cierre limpio: Cow, Pig y ZombieNautilus
originales, 36 muestras, 504 escalares bit exactos, 720 relecturas y 168 matrices
originales Cow/Pig. Cubre estados de nombre, vehicle, vida, walk, flags, rotacion,
escala/edad y precision; ausencia finita requerida, schema64/65, identidad exacta,
owner heredado y pureza. No se reclama compilacion lineal para ZombieNautilus.
JVM common nueva con veto cliente PASS168 matrices/340406 asserts; 24 lookups
frios/0 hot. Replay usa captura nativa; no ejecuta el actor original. El actor
sintetico contador solo comprueba memo local: 100k queries/64 aliases producen
100k getter reads. Se declaran staging/boxing/copia O(C), no zero-allocation/TPS. Ese bound cubre trabajo propio; getters del actor
y Component.getString conservan su coste original dependiente de datos.

Sensibilidad nueva PASS7/7 kills semanticos: seis mutantes del lector y uno del
proof de field no admitido, todos compilaron y fallaron por el oracle requerido.
El baseline cold pasa7 asserts; ese harness omite el gate publico de hilo solo
para auditar recursos originales, sin claim de actor vivo. La lane nativa usa
el entrypoint publico real. Los primeros fallos fueron infraestructura/fixture:
task inexistente, firma startRiding, nombres de canal, ID ausente, override final,
bootstrap, longitud de comando Windows y reporting tras cerrar classloader.
Se preservan intentos y fuentes ejecutadas; errores de compile/bootstrap o de
reporting no se contabilizan como kills ni defectos de produccion.

Snapshot offline de ocho estados Cow/Pig generado e inspeccionado: envelopes y
origenes de joints N/C comparables, sin recorte; max delta matricial 1.0000000006e-7.
No es prueba GPU/root/layers/material. Evolucion del reloj autoritativo, scheduling,
concurrencia, tracker, runtime y FULL siguen UNPROVEN; G6 permanece abierto.

[Recibo final](../evidence/S61/adversarial-verification.json) fija hashes de fuente,
raw, controles, mutantes, snapshots y limites. [Receta publica](../../tools/s61-adversarial-common/README.md)
reproduce solo esta frontera con cache/build/run propios. No Tiny/full suite ni
sensibilidades historicas ajenas repetidas. No one-offs de repositorio pendientes;
logs/caches/builds/shadows/worlds conservados. UTF-8 y sintaxis propia comprobados.

## 7. Cierre

Cierre scoped tras dos reviews completas de cada rol cero cambios productivos;
167raw(100adversario+67propios al review) comprobados por hashes/longitudes,
reviewed candidate b006ae72 conservada antes del receipt final. Root replay
publico common168/340406 PASS; snapshot adversarial8filas tambien visto, sin
recorte geometrico. Bounds/getters/component costs y limites explicitos.

TRUSTED solo lectura/proyeccion partial1 y proof original enum/edad/enum-unico;
clock evolution/integracion/tracker/root/material/continuidad/transformed/GPU/FULL
siguen UNPROVEN. Baseline93Living/92UNRESOLVED+1fixtureSAFE_PARTIAL+0FULL intacta.
Manifest ordinary113/34 restaurado sin ejecutarlo. No Tiny/fullsuite/mutaciones
historicas ajenas repetidas. Tools propios/adversariales reusables aislados;
shadows/caches/builds/worlds conservados, sin one-offs de repo pendientes.

Cierre con commit local propio Sprint:S61; verificar INDEX/HEAD raw y arbol
antes de avanzar. Sin push/tag/release/install. G6general/G7-G9 y QA humana al
final de epica siguen abiertos.
