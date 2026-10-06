# S62 - Reloj comun de locomocion autoritativa

Estado: S62 cerrado scoped; campana/reviews verdes, commit local de cierre requerido.
IMPLEMENTER raiz / ADVERSARY separado. Base c8689372 S61 cerrado/limpio;
main b5e62535 actualizado y ancestro, preservar ScaleGait y comportamiento ajeno.

## 1. Tesis, requisitos y limites

Una sola evolucion privada del reloj por actor/authority tick, compartida por
AuthorityPoseTracker y la proyeccion Living S61. FR-027/028/029/061/077,
NFR-009/011/019/025; evidencia scoped, sin declarar estos requisitos globalmente cerrados.

El reloj usa WalkAnimationState common original: velocidad min(distance*4f,1f),
smoothing .4f y time scale ScaleGait.walkTimeScale(actor,baby?3f:1f).
Distance es la longitud tangencial respecto a gravedad del delta propio:
posicion actual - endpoint anterior - contribuciones aplicadas TransportLedger.
La correccion causal/gravedad es intencional: no se afirma igualdad con la
distancia mundial cruda de un cliente que no descuenta transporte.
La ley base Living se prueba contra calculateEntityAnimation/updateWalkAnimation
nativos transformados del producto para actores que heredan esa ley. No se
certifican overrides ajenos/omnidirectionalAirMover ni todas las familias.

El reloj entity.walkAnimation queda intacto. Un frame del reloj privado alimenta
solo WALK_POSITION/WALK_SPEED de la fuente S61; el resto conserva exactamente
su extractor original. El overload supplied anterior permanece read-only.
No alias de orientacion legacy, wiring automatico de adapters/bindings, catalogo,
wire, root/inclusion/material, nuevas especies FULL ni cierre G6/G7/G8/G9.

## 2. Investigacion y S00 - IMPLEMENTER

Vanilla Living.aiStep invoca calculateEntityAnimation solo en cliente. Su ley
base llama al WalkAnimationState.update original; el mixin vigente modifica
timeScale mediante ScaleGait. El tracker actual mantiene una copia privada de
la ley sin ScaleGait y expone getters no-partial. Son REWORK para este contrato,
no una prueba de evolucion original. El delta causal, cursor/generation, gravedad
y clock persistente del participante son candidatos reutilizables scoped.

S61 reader y recetas son candidatos TRUSTED solo en su dominio documentado.
Antes de produccion: recibo S00 independiente con bytes/recursos/evidencia
pertinentes actuales y clasificacion explicita de cada dependencia; reparar
el ownership/reloj REWORK en este sprint antes de ampliar fuentes/runtime.
Ni pruebas historicas ni modelos holdout privados definen el nuevo contrato.

## 3. Plan IMPLEMENTER

Revision propia1: ownership/callers/ley float/transport y continuidad; aclarada
la ley base y conservado reader supplied anterior. Revision propia2 completa
tras P1: requisitos, dependencia weak-key, errores, same-tick, lifecycle,
costes reales de locking, pruebas/regresiones y exclusiones; cero cambios
pendientes del implementador. Convergencia independiente aun requerida.

- [x] I1 Cerrar S00 scoped y convergencia previa con adversario. Dos lecturas
  completas sin cambios pendientes; fuentes/main y scopes fijados.
- [x] I2 Extraer LivingLocomotionClock common: una instancia por State existente
  del tracker, un WalkAnimationState privado, sin segundo mapa global. Identidad
  exacta del actor por referencia debil; no actor/Level retenidos fuertemente.
  Tick negativo/rewind/otro actor/client level rechazados antes de mutar.
  Advance y admision de frames se confinan al thread del servidor; avance
  reentrante se rechaza y no publica frame mientras hay evolucion en curso.
  First/gap/desplazamiento >4/gravedad distinta/Level distinto/generation o
  ventana no contigua -> stop/rebase, sin interpolar la discontinuidad.
  Pasajero/muerto -> stop. Posicion no finita -> frame unavailable y recovery
  con baseline fresco. Reloj original/getters partial1 conservan limites float.
- [x] I3 Frame inmutable no construible por datos/cliente: actor/Level debiles,
  tick/generation, valores finitos y owner actual. Mismo tick normal retorna el
  mismo frame sin avanzar; barrera lifecycle/Level/gravedad observada en advance
  invalida y rebasa aun en el mismo tick. Frames antiguos/ajenos/retirados/otra
  generacion o tick solicitado no se admiten; el ledger generation se comprueba
  tambien al leer, sin consumir cursor. Consultar/current nunca avanza.
- [x] I4 AuthorityPoseTracker delega toda su evolucion al owner unico; conserva
  canales/adapters/transacciones, ordinary toggle same-tick y reloj persistente
  del participante. API read-only de frame actual; outputs walk partial1.
  LivingPoseInputSource.Bound.sample(actor,frame,authorityTick) valida frame
  completo incluso si no pide walk y proyecta walk del frame, memoizado; ninguna
  escritura al entity.walkAnimation. Invalid frame -> ausencia total sin prefijo.
  Validar al inicio y otra vez al commit despues de todos los getters: un frame
  invalidado durante lectura no puede publicar el mapa staged.
  Tracker tambien comprueba frame al commit despues de getters/adapters:
  retired -> ordinary=false. State sampling guard rechaza tick reentrante y
  oculta current/locomotion provisionales; RuntimeException/Error retirados antes
  de propagar, sin inputs/frame publicos previos tras fallo.
- [x] I5 Prueba propia nativa server con al menos Cow/Pig, adultos/babies y
  escalas diversas: oracle real calculateEntityAnimation sobre actor control
  desplazado por delta propio, extractor S61 partial1 y secuencias fijadas.
  Transporte/mismo tick/reset/recovery/queries/rebind clock y no contaminacion
  del reloj original observables; snapshots temporales cuando aporten senal.
  Compilar/build y regresion tracker/transport/gravity/gait/lifecycle pertinente.
  No Tiny suite ajena: no se altera movimiento/seat/render de pasajeros.
- [x] I6 Candidata -> campana adversarial independiente/holdouts/mutaciones;
  clasificar fallos antes de corregir, repetir bateria afectada. Reviews finales
  completas cero cambios, canon/VALIDATION/recibos/cleanup y commit Sprint:S62
  local antes de avanzar. Necesidades del propietario solo al final de epica.

Coste HOT_TICK propio O(1), ledger.since bounded <=64 aportaciones. El tracker
conserva su monitor de instancia; TransportLedger.generation/since usan un
monitor static global existente, NO locking local por actor. Advance mantiene
las consultas anteriores del clock; commit nuevo del tracker agrega una lectura
de generation despues de getters/adapters. Sample del nuevo overload agrega dos lecturas de
generation en ese monitor global por muestra valida (admision/commit), nunca
por canal. Esta contencion
se audita explicitamente; no se certifica escalabilidad de locks ni 20TPS aqui.
Sin nuevo monitor o scan mundial. Una instancia/state vivo y un frame por tick; referencias
debiles sin cadena value->Level->actor. Sampling O(C<=64), scratch/map/boxing
S61 declarados; validacion de frame consulta generation identity-local dos veces (lock
existente). Getters de actor conservan coste original dependiente del dato.
No hash/reflection/parser/cold binding en advance/sample, ni trabajo por observer,
ni promesa de cero allocations/20TPS sin benchmark G9.

## 4. Modelo/convergencia/S00 - ADVERSARY

Modelo clean-room formado despues de S61 y antes del plan: autoridad de evolucion
por tick no se deduce de lectura de walkAnimation supplied. FR-025..029/030/061,
077..082 y NFR-004/005/009/011/015/017/019/020/025/027/029/033/034/036 limitan
el dominio. Un participante tiene una evolucion por authority tick en contexto
de mundo autorizado; queries, canales y observadores no avanzan el reloj.
La correccion causal/tangencial esta declarada, sin afirmar igualdad con cliente
que incluye transporte pasivo u otro frame. La ley native no prueba autoridad.

Clases generales: locomocion activa/quieta, gait/scale/frame cambiantes, vivo/
pasajero, fuente conocida/ausente/no finita, continuidad/reemplazo de actor,
Level/revision/generation, observaciones repetidas/tardias y tick vs partial.
Duplicar consumidores no duplica evolucion; barreras requieren rebasing/ausencia,
no interpolar saltos ni congelar material. Un descriptor nuevo no reemplaza el
clock del participante continuo. Root/joints permanecen separados. Sin casos
concretos nuevos ni holdouts expuestos antes de candidata.

Evidencia prevista: secuencia temporal contra original transformado del producto,
estado/tick fijados y snapshots cuando aporten senal; capas common/dedicated/
original separadas. Source S61 se reutiliza partial1, no como proof de evolucion.
Confinement/reentrancia, ownership/frame y continuidad requieren campana nueva.
No material/root/GPU/layers/FULL o cierre G6 inferidos.

S00 scoped por lectura/hash/javap, sin Gradle/suites: 383 owners y 168 raw S61
byte exactos. ScaleGait completo y hook gait de LivingEntityMixin iguales a main;
no afirmacion de igualdad de todo el mixin (otras rutas colision evolucionaron).
Living native convierte distancia a float, min(distance*4f,1f), smoothing .4f
 y baby3f; aiStep lo llama client-side. WalkAnimationState original ofrece getters
partial y stop/reset. Bytes y javap fijados, sin inferir avance dedicado ni
que el resource crudo contenga mixins. Oracle transformado queda obligatorio.

Clasificaciones: S61 reader/proof TRUSTED solo supplied partial1; ledger bounded
cursor/generation/TTL y tangent cardinal TRUSTED en contratos existentes.
ScaleGait TRUSTED como owner main preservado; nueva aplicacion se revalida.
Tracker/reloj anterior REWORK para esta tesis, no oracle trusted. Nuevo clock/
frame/integracion UNPROVEN hasta candidata; no supuesto de reutilizacion. S00
cierra dependencias scoped, no certifica REWORK antes de su prueba posterior.

P1 PLAN/COSTE observado: static synchronized del ledger comparte monitor global,
aunque lookup sea identity-local. Plan corregido: una lectura nueva generation
por muestra, no por canal; sin claim lock locality/escalabilidad/TPS. Auditar
HOT_TICK O(1)+historia64 y sample C64, allocations declaradas, sin parser/
reflection/cold proof hot, scan mundial o trabajo por receptor. Coste original
de getters no queda acotado por C. Ownership autorizado y barreras se revalidan.

Dos pasadas completas tras P1, autoridad/lifecycle y despues ownership/coste/
compatibilidad/evidencia, sin cambios pendientes. Funciona, continua: convergencia
previa/S00 scoped, no aceptacion productiva. [Recibo independiente](../evidence/S62/adversarial-s00-verification.json)
fija bytes/clasificaciones/limites. Sin Tiny/full suites ni sensibilidades
historicas verdes repetidas; QA humana permanece G9.

P2 re-convergencia independiente: confinement de advance/admission al thread
real de servidor y rechazo de advance reentrante hacen explicita la autoridad
exigida por el modelo previo/NFR-015. Frame no es admisible durante evolucion;
lecturas no mutan ni consumen cursor. Rechazo no debe publicar un frame parcial
ni convertir un fallo de contexto en avance diferido. Dos pasadas completas del
plan refinado, semantica/lifecycle y ownership/coste/compatibilidad, sin cambios
pendientes. Funciona, continua solo para el refinement P2; guard aun no probado
en la candidata base. Sin Gradle, suites ni casos privados revelados. [Recibo P2](../evidence/S62/adversarial-p2-reconvergence.json).

## 5. Implementacion - IMPLEMENTER

I2-I5 completos;383 owners anteriores381byte-intactos y solo tracker/source
modificados, mas un owner nuevo. Guard P2 aplicado despues de re-convergencia.
I5: native640/3840bits/16000reads PASS, lifecycle/tangente/identity/rewind/
same-tick/barrier/dead/passenger/nonfinite/recovery/removal y tracker PASS.
Server inicial focal29/29 (20s), final postRED3 30/30 (38s); ledger aislado
10/10 (15s); build inicial40s, empaquetado final PASS6s tras restore manifest PASS2s. JAR619clases exactas/
GameTests sin leaks,609clases previas intactas;113server/34client restaurados. Common client-veto52 cold bindings/absence/ownership PASS,
SIN ejecutar actor real en ese JVM. Snapshot temporal20panels generado/visto,
axes compartidos phase154.085096/speed1/ticks0..31; no renderer/root/material.
Recibo final: docs/evidence/S62/implementation/verification.json.

## 6. Verificacion - ADVERSARY

RED1 PRODUCCION: admision de frame no transaccional durante lectura de source.
Oracle independiente S62AdversarialFrameAdmissionTests, exact actor manual con
getter callback: control positivo frame valido/sample presente; callback retira
generation del ledger dentro de getCustomName. Tras volver, validFor es false
pero sample publica Optional[{age=1.0,name=0.0}] en vez de ausencia completa.
FR-082/NFR-015/NFR-004; no claim de original-class extraction en este fixture.
Compile PASS, GameTest RED1/1/exit1, reported31s; [recibo RED1](../evidence/S62/adversarial-red1.json)
fija fuente candidata, oracle y tres logs. No produccion reparada por ADVERSARY.

Repro focal: JDK25 JAVA_HOME; `./gradlew.bat --project-cache-dir ../s62-adversarial-cache
-PscalebrewsBuildDir=../s62-adversarial-build -I tools/s62_adversarial.init.gradle runGameTest`.
Primero fallo el sandbox lock (sin oracle); segundo fallo de fixture por nombre
EntityType.COW antiguo, corregido a EntityTypes.COW sin alterar criterio.
Candidata/campana/aceptacion pendientes. Ninguna Tiny/full suite ejecutada.

RED1 reparado pasa sin cambio de oracle. Diferencial independiente nativo
Cow/Pig adulto/baby x5scales x6gravity x12ticks PASS1440 samples/2880 bits/
36000 samples repetidas; entity.walkAnimation intacto. Transporte ledger aplicado
manualmente, no claim contacto real. Thread/identity/rewind/gap/jump/reset verdes.
Primer verde:3tests S62+harness=4required,22s y salida limpia.

RED2 PRODUCCION: AuthorityPoseTracker publica ordinary=true cuando adapter de
canales retira generation antes del commit. Control valido pasa; segundo tick
frame.validFor=false pero output ordinary=true,phase=.2,amount=.2. Nuevo oracle
S62AdversarialTrackerCommitTests, registry restaurado exactly en finally.
Compile PASS,19s/exit1,1rojo entre5required; otros controles siguen verdes.
[Recibo RED2](../evidence/S62/adversarial-red2.json) conserva tracker compilado,
oracle y logs. No produccion reparada por ADVERSARY. Aceptacion permanece abierta.

RED2 reparado pasa con oracle intacto. Controles nuevos de advance reentrante y
frame no admisible durante evolucion tambien pasan. RED3 PRODUCCION/NFR-004/015:
fallo de getter getXRot como RuntimeException retira inputs/frame y recupera;
el mismo punto como AssertionError propagado deja inputs publicos previos.
La inyeccion comprueba provisional oculto antes del fallo; no exige tragar Error.
Compile PASS,25s/exit1,1rojo entre7required; RED1/RED2 y1440native siguen verdes.
[Recibo RED3](../evidence/S62/adversarial-red3.json) preserva tres owners, oracle y log.
Snapshot temporal propio ya generado/visto:4grupos Cow/Pig adulto/baby,12ticks,
phase/speed originales vs privados coinciden sin recorte. Es curva de inputs,
no GPU/world-root/material/clock sincronizado cliente-servidor.

Instruccion humana vigente: cerrar S62, commit local y PARAR; ningun sprint ni
modelo siguiente. Campana y recibos se limitan a esta frontera.

### Aceptacion ADVERSARY final

Funciona scoped. RED1/RED2/RED3 pasan con oracles intactos en baseline focal
final: seis S62 tests + harness,7required PASS21s/exit0 y salida limpia.
Original transformed Cow/Pig adulto/baby x5scales x6gravity x12ticks:
1440muestras/2880bits/36000relecturas, entity.walkAnimation intacto.
Ledger aplicado manualmente; no claim contacto/paquete real.
Dos mutantes NUEVOS del clock (omit-gait/double-tick) compile0/native1,
semantic kills2/2 y driver0, outputs propios restaurados byteexact.
Las tres candidatas rojas naturales se preservan; no se cuentan como otros
mutantes nuevos ni se repiten sensibilidades historicas verdes.

Snapshot final generado e inspeccionado: cuatro grupos/48filas temporales,
phase/speed originales y privados con maxDelta0, sin recorte. Solo inputs;
no GPU/model/root/material/FULL ni server-client lockstep.
Ley, identidad, thread, resets, reentrancia, provisional, fallos y recuperacion
recertificados en el dominio ejecutado. Coste O(1)+ledger<=64 y O(C<=64);
dos lecturas generation bajo monitor STATIC GLOBAL por sample valido,
mas validacion commit del tracker. Sin claim TPS/escalabilidad/zeroalloc.

Dos pasadas finales cero cambios productivos.384files=268Java+116resources,
381previos byte intactos; JAR7c1fd5a0/816entries/1385341bytes verificado.
[Recibo final](../evidence/S62/adversarial-verification.json) fija fuentes,
oracles/logs/mutantes/snapshots y limites; [receta reusable](../../tools/s62-adversarial/README.md).
No Tiny/suites ajenas. Reusables retained, externos caches/builds/worlds/logs
preservados; ningun one-off privado sustituido permanece en repo.
G6/G9 y familias completas siguen abiertos. Cerrar S62, commit local y PARAR;
ningun modelo ni trabajo siguiente autorizado.

## 7. Fallos y bucles

P1 PLAN/COSTE: la primera redaccion confundia lookup identity-local del ledger
con locking local. Adversario demostro monitor static synchronized compartido;
corregida declaracion y exigida auditoria del nuevo acceso por muestra.
P2 refinamiento IMPLEMENTER durante review de candidata: explicitar/enforzar
confinement al thread de servidor y rechazo de advance reentrante antes de
publicar/leer frame, en vez de dejarlo como precondicion implicita del caller.
No se cambia ley ni amplian familias. Adversario reconverge P2 con dos reviews
cero cambios y recibo adversarial-p2-reconvergence.json; aplicado despues.
F1 TOOL/ISOLATION: lane inicial38 tests,36PASS/2TTL rojos del ledger. La fixture
S48 llama AnatomyRuntime.stop(server), que desactiva TODOS los Levels y borra
las entradas Nether de TTL mientras sus callbacks aun esperan39/40ticks.
Nativos S62 640muestras/3840bits/16000reads, lifecycle y tracker PASS; no bug
productivo inferido de ese rojo. Separar ledger TTL en proceso propio sin
reescribir sus asserts ni la produccion; repetir ambas baterias tras P2.
RED1 IMPLEMENTATION/FR-082/NFR-015: una fuente manual explicita con getter
getCustomName que invalida TransportLedger durante sample publica age/name
aunque su frame ya no es valido al retorno. Adversario fija oracle/log/candidata
en adversarial-red1.json; no se altera oracle. Corregir validacion al commit
tras projection, sin fallback; coste pasa a dos consultas generation por muestra
valida, no canal. Repetir toda bateria de source/clock afectada y oracle revelado;
ledger no modificado por esta correccion y su PASS anterior no se reejecuta.
RED2 IMPLEMENTATION/FR-082/NFR-015: adapter publico retira generation mientras
tracker prepara channels; resultado ordinary=true con frame invalido. Oracle
independiente fijo en adversarial-red2.json, fuente/source-reader y diferencial
native1440 siguen verdes. Reparacion: construir inputs staged, validar frame
tras TODOS los getters/adapters y ordinary=false si retirado. Review propia del
mismo commit mueve publicacion frame/inputs al final y protege State.sampling:
callbacks no leen provisional ni inputs de tick anterior, ni muestrean dos veces
el mismo tracker. Nuevo control propio de transaccion y bateria afectada30/30 PASS29s.

RED3 IMPLEMENTATION/NFR-004/015: getter delegado como AssertionError propagado
retiraba sampling guard pero dejaba inputs anteriores publicos. Oracle independiente
fijo en adversarial-red3.json. Reparacion: limpiar inputs/frame tambien ante Error,
propagandolo sin convertirlo en resultado ni tragarlo. Bateria afectada30/30 PASS38s;
fresh common final52bindings PASS, client-veto/ownership/absence, no actor execution.

F2 TOOL/VALIDATION durante verificacion INDEX: primer probe normalizaba tambien
bytes PNG y segundo filtraba texto por extension, excluyendo service META-INF
sin extension. Falsos fallos del verifier, no bytes productivos alterados.
Comparar bytes primero y aceptar CRLF->LF solo en texto UTF-8 sin NUL;
398owners/publicSources PASS y80raw evidence INDEX byte exactos. Diff staged limpio.

## 8. Reviews y cierre

IMPLEMENTER pasada completa1 final de produccion postRED3: ley original/gait/
float y transporte, autoridad/identidad/Level/tick/generation, lifecycle y
callbacks/publicacion/fallos, weak ownership y costes/compatibilidad; cero
cambios productivos pendientes. Recibo implementation/review-pass1.json.
Packaging final PASS6s/restore manifest PASS2s y evidencia propia byte verificada.
Segunda review propia completa despues de campana: ownership/coste/compatibilidad,
recibos propios e independientes, artifact/manifest, canon/limites, snapshots
publicos y cleanup; cero cambios. Recibo implementation/review-pass2.json.
Dos reviews independientes cero cambios en adversarial-verification.json.
Convergencia final completa scoped; ningun defecto o cambio productivo pendiente.

## 9. Cierre

Frontera humana vigente: cerrar S62 y parar; no nuevo sprint/modelo posterior.
Tooling reproducible init focal, common client-veto y snapshots retained bajo
surface test/tools; logs/capturas durables. Builds/caches/worlds/shadows quedan
fuera del repo, sin cleanup destructivo. Commit local propio Sprint:S62 de cierre; verificar raw INDEX/HEAD y arbol limpio
como ultimo paso Git. La referencia se obtiene con git log --grep="Sprint: S62".
Tras ese cierre la ejecucion se detiene, sin siguiente sprint/modelo.
