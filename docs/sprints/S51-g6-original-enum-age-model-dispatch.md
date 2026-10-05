# S51 — Preparación del selector original enum/edad de modelos

Estado: CERRADO scoped PREPARATION; commit local propio Sprint:S51.

## 0. Roles

IMPLEMENTER raíz: plan/producción/desarrollo. ADVERSARY existente: modelo previo
independiente, holdouts y evidencia; no corrige producción. Comunicación operacional.

## 1. Scope

Tesis: una familia de selector original enum→AdultAndBabyModelPair podrá generar
filas de selección de modelos originales mediante PREPARATION, verificando la
semántica del método original que usa el mapa, no suponiendo que cualquier campo
con modelos sea el que se dibuja. Su evidencia será contraste con renderer real
y entradas nativas; no certifica pose/root/geometría física/FULL.
Contribuye G6 tarea2; FR-004/016/024/026/033/038 y NFR-007–010/015–018/024–027.
Dependencias S00/G1–G5/S45–S50 TRUSTED únicamente en sus scopes. No nuevo resolver
runtime, catálogo general, red, solver, mount/passenger/seat/render hook ni compat.
La preparación no se invoca desde initializer/tick/query/renderer normal. No Tiny
Mounts ajeno; pruebas de cliente focales y estructura/censo afectados, G9 intacto.

## 2. Investigación / estado inicial

HEADaec8e4d637b5f990c7e39c2c2e3e5e0327a64411 limpio, main vigente
b5e6253584b2233e5dd0039203c8a251bf10cdeb ya integrado, sin cambios fuera de scope.
S45 inventaría campos/slots y fingerprints iniciales sin probar dispatch. S48–S50
observan age y modelo enum original, pero faltan las relaciones originales entre
esas entradas y los slots que realmente elige el renderer. No se promueve FULL
por existencia de campos, clases similares ni mapas aparentemente compatibles.

JAR Minecraft26.2 pinned ec9c864e8a304be37d1d273fefefcb4e4dcc2154a18ccd3c1fe9d2d0a298a7d9.
Recursos merged y runtime split/access-widened Loom no tienen necesariamente
el mismo hash de clase. Se ha comparado cada programa completo de las siete
dependencias originales inspeccionadas: mismos opcodes/operandos simbólicos y
flujo tras normalizar índices CP, anchura ldc y offsets de targets. EntityRenderer
y LivingEntity sólo difieren en esas anchuras/offsets; los otros programas
coinciden directamente. Cada variante aprobada mantiene su digest exacto y se
registra la observada; no aceptar bytes nuevos sólo por nombre/version.
La clase original Entity contiene531 cabeceras de métodos en javap, por encima
del cap512 inicialmente propuesto; se propone cap1024 por clase para inspección
PREPARATION, manteniendo512 instrucciones/método,64 clases,1MiB/clase y8MiB total.
Sin expansión de dominio/runtime ni permiso para saltar la prueba de procedencia.

Bytecode de Cow/Pig/ChickenRenderer muestra misma familia en submit typed no-bridge:
guard variant null→return; map de modelos por variant.modelAndTexture().model();
AdultAndBabyModelPair.getModel(state.isBaby); asigna el campo heredado LivingEntityRenderer.model;
delegación a MobRenderer.submit. ZombieNautilus usa otra familia y no se adapta
forzadamente. No son listas Java de soporte: sólo witnesses de investigación.
Extract typed conserva entity/state/partial al delegar al extract heredado; variant
procede de entity.getVariant().value(). El get(DataComponentType) de la clase
original devuelve ese mismo getter para la identidad del componente. El extract
de LivingEntityRenderer copia entity.isBaby() al campo original isBaby. Pig y
Chicken tienen otras capturas (saddle/flap), por lo que no basta encontrar sólo
el bloque de variant ni ignorar invocaciones que puedan alterar esas entradas.
Java25 instalado proporciona ClassFile/CodeModel/Instruction para analizar recursos
class originales; se fija digest de entrada. Sólo PREPARATION, sin nueva dependencia.

## 3. Plan IMPLEMENTER

- [x] I1 Crear un preparador client-only de la familia enum/age que acepte un
  EntityRenderer y produzca una receta de filas inmutables, o un diagnóstico
  acotado sin filas admitidas. No recibe ni inventa un EntityType: asociación de
  actor/catálogo sigue responsabilidad del discovery/caller certificado.
  Leer class bytes pinned (<=1MiB), métodos<=1024/instrucciones<=512; localizar
  submit typed no-bridge con firma original. Aceptar sólo programa completo de
  la familia inspeccionada: guard null, target correcto, loads/fields/accessor/
  map.get/casts/getModel/putfield/super-submit/return, sin handlers ni operaciones
  extras. No encontrar una subsecuencia dentro de comportamiento distinto.
- [x] I2 Resolver el field Map realmente consumido por ese programa, la variante
  record y el campo isBaby original de LivingEntityRenderState. Asignación debe
  ser al slot heredado LivingEntityRenderer.model, no otro campo homónimo. Reflection/linkage sólo
  PREPARATION. Clase del accessor record y enum del mapa deben coincidir con una
  firma única DataComponentType<Holder<V>> compatible S50; key pública .model
  y source registrada con dominio exacto. Ausencia/ambigüedad/ID/key/source mal
  formado falla cerrado, sin escoger un candidato cómodo ni una lista de especies.
  Certificar también la cadena original completa de entradas: clase exacta del
  actor declarada por extract typed; mismo entity/state/partial al extract base;
  variant = Holder.value del mismo getter que devuelve get(component) para la
  identidad exacta de DataComponents; age = entity.isBaby en extract base, sin
  inversión, sustitución ni posteriores escrituras a esos campos. Analizar el
  programa completo de extract typed y get(component), con targets/returns,
  preservación de alias y resolución real de métodos/campos. Sólo patrones de
  capturas originales auditadas y acotadas; una llamada desconocida, flujo extra,
  write/alias potencialmente relevante o override no probado rechaza la receta.
  La extracción común de EntityRenderer/LivingEntityRenderer y helpers usados
  constituyen dependencias originales pinned: recursos/digests de programas
  completos auditados, no subsecuencia age dentro de una función arbitraria.
  Registrar cada recurso realmente leído/digest/owner y la cadena de métodos.
  No certificar actor subclasses ni bytecode transformado por compat no probado;
  la receta declara clase exacta original y su scope. Ausencia de cualquier tramo
  de procedencia => diagnóstico sin filas, aunque submit y mapa sean válidos.
- [x] I3 Enumerar mapa original completo (<=256keys, <=512filas): enum propietario
  exacto y nombres serializados originales, dominio íntegro sin faltantes/extra,
  pair válida con modelos adult/baby no nulos. Cada fila declara age+model source
  y provenance del slot/selector/model class, fingerprint inicial S45. Reusar
  estructuras por identidad dentro de la pasada; no resetear ni mutar modelos
  compartidos. Resultado serializable/determinista, sin referencias mutables a
  renderer/model/world. No ejecutar esas filas como segundo resolver físico;
  servirán al generador de bindings sobre el resolver canónico.
- [x] I4 Desarrollo positivo con más de un renderer nativo de la familia y casos
  fuera de familia/ausentes. Compilar artefacto/GameTests y pruebas client focales
  de preparación con contraste que recorra entity nativa -> extractRenderState
  original -> submit original para el dominio íntegro enum x adult/baby. No
  rellenar manualmente state.variant/isBaby como oráculo de esta procedencia.
  Contrastar source pública con estado extraído y modelo finalmente seleccionado;
  negativas de extracción alterada/puente ausente no pueden admitir filas.
  Regresión pertinente S45 de estructura/censo y snapshot de estado compartido;
  no full Tiny/servidor repetidos sin cambio de esa frontera. No afirmar que el
  fingerprint equivale a collider correcto ni que comparar elección prueba pose.
- [x] I5 Reviews completos sin cambios, negativos/sensibilidad/holdouts independientes,
  costes/provenance/originales y capturas útiles; canon/VALIDATION/recibos/cleanup,
  commit local propio Sprint:S51 con HEAD/árbol verificados.

## 4. Revisión y presupuestos IMPLEMENTER

PREPARATION: bytes/métodos/instrucciones acotados; campos/componentes/keys/filas
y suma de partes/cubos por modelos únicos, con budgets S45. No coste nuevo en
HOT_TICK/HOT_MOVE_QUERY/HOT_NETWORK/HOT_PRESENTATION: ninguna ruta hot llama al
preparador. Clases, reflexión, parser y hashing permanecen allí; resultado sólo
metadatos/copias inmutables, no retiene mundos. Fallo no produce filas parciales.
Una receta de elección es dependencia previa a pose/root/geometría física, no su
certificación. Cambios en bytes originales o programa fuera de familia requieren
revalidación/rechazo, no fallback a una heurística de campos.
Dos pasadas iniciales contra canon/scope/límites no detectaron la brecha de
procedencia. RED1 PLAN independiente obliga a revisar I2/I4 y el owner real del
slot; no eran convergencia suficiente. Tras esta reparación, la revisión completa
incluye fuentes -> extracción -> selección, programas completos, aliases y
scope original pinned. Falta nueva convergencia independiente antes de producción.

## 5. Modelo previo — ADVERSARY

Modelo previo independiente desde canon tras S50, antes de este plan. Separar
observacion autoritativa, estado de renderer, eleccion de slot y material fisico:
ninguno demuestra automaticamente el siguiente. Un programa submit que lee un
campo no prueba de donde procede el valor de ese campo. FR-024/027/033 y
NFR-029 requieren que age/model source declarados por la receta correspondan a
las entradas nativas originales, no solo a clases/firmas compatibles.

Validar el programa completo, target heredado correcto, dominio/mapa completo,
identidad y copia sin mutacion es necesario. La procedencia de variant/isBaby
hasta el estado recibido por submit debe quedar certificada o declarada ausente;
la unicidad de DataComponentType<Holder<V>> no basta para inferir esa procedencia.
Unknown/rechazo no admite filas parciales, default ni aliases de slots. No pose,
root, geometria ni FULL por un fingerprint. S45/S48-S50 permanecen TRUSTED solo
en scopes previos; parser/receta/puente de inputs vuelve UNPROVEN.

PREPARATION debe acotar bytes/metodos/instrucciones/keys/filas y estructuras
unicas; ningun caller hot. Es necesaria identidad reproducible de bytes y
recursos realmente observados, no solo nombre de clase o hash del JAR common
para clases client. Oraculos originales y snapshots fijados observan seleccion
y ausencia sin mutar el modelo compartido; regresion cliente focal/censo,
no Tiny Mounts ajeno. Casos concretos y mutaciones se reservan para produccion.

## 6. Implementación — IMPLEMENTER

Dos owners client-only nuevos: EnumAgeModelPreparation y OriginalModelPrograms.
No owner existente de producción, hook, wire, solver, mount ni hot path cambiado.
El preparador analiza programas completos typed submit/extract, sus cuatro bridges
reales de entrada, getter/component bridge, accessor record puro y dependencias
originales exactas auditadas. Owner real del slot, aliases/targets/descriptores y
tipos reference/float son obligatorios. Origin/loader del actor/renderer deben
corresponder al artefacto original common/client; subclasses/entradas no probadas
no se admiten. Source ownership se verifica con initialize idempotente PREPARATION.

Filas inmutables de dominio íntegro enum x edad, provenance/digests, model class y
fingerprint inicial S45; nombres enum capturados una vez y estructuras reusadas
por identidad dentro de la pasada. Ninguna referencia mutable en receta ni caller
hot; budgets convergidos1024methods/512instructions/64classes/1MiBclass/8MiBtotal,
256keys/512rows/512modelos, límites por estructura S45 y suma finita de estructuras.
No prueba de pose/root/geometría física/FULL ni catálogo ejecutable.

build -x runGameTest + compileGametestJava + cliente focal S51/S45 PASS37s, clean
exit en ../s51-original-selection-r6.log. Tres renderers nativos, siete enum keys,
14combinaciones completas; manifest112server/34client restaurado PASS6s, fuente
y procesado iguales sin suites; setter público de componente -> getter autoritativo ->
extract original -> source pública -> submit/queued ModelFeatureRenderer original.
Determinismo, inmutabilidad, snapshots de shared parts durante PREPARATION,
familia ZombieNautilus fuera de scope y renderer ausente permanecen rechazados.
S45 censo real PASS93living/120groups/561slots/464structures/4775parts después de
los submits originales: tres slots .model ahora aliasan entradas ya existentes
del mapa y dejan de retener los tres modelos duplicados iniciales. Es censo de
candidatos, no cambio de cobertura ni promoción FULL. No full servidor/Tiny.

Revisión completa IMPLEMENTER descubre durante desarrollo la necesidad de probar
bridges reales (un overload typed por sí solo no prueba el entrypoint), accessor
record puro, artefacto original y tipos de load; corregidos antes de entregar.
Dos pasadas finales completas sin cambios después del focal GREEN: ownership,
procedencia completa, scope original, ausencia, límites/localidad, aliases,
inmutabilidad, inputs/domain/original oracle y consumidores. Campaña independiente
y replay público14/12, parser18/8 y snapshot inicial inspeccionado: PASS scoped.
369 hashes y JAR idénticos; no cambios productivos tras focal GREEN.

## 7. Verificación — ADVERSARY

PREIMPLEMENTACION: no funciona, arreglalo. RED1 PLAN/procedencia de inputs.
I1 valida submit; I2 convierte el tipo del campo variant en un source .model y
usa isBaby para etiquetar filas. Falta certificar que extractRenderState y las
rutas que lo alimentan conservan precisamente el componente nativo y isBaby
que leen S48-S50. Firma unica del componente no prueba esa relacion.

Contraejemplo operativo de contrato: renderer de la misma familia con el mismo
submit completo y mapa enum/AdultAndBabyModelPair, pero extractRenderState
asigna state.isBaby = !entity.isBaby(). Todos los checks de I1-I3 sobre submit,
Map, record, enum, fuente unica y campos siguen satisfechos; receta para eje
age=baby predice babyModel, mientras submit original para ese cuerpo usa
adultModel. Variante record sustituida por otra instancia del mismo tipo en
extractRenderState tiene el mismo problema para .model. No se modifica pose
ni solver, ni es necesario violar el contrato del Map.

Debe cerrarse la prueba del vinculo fuente autoritativa -> campos del estado
original antes de admitir filas etiquetadas por esos ejes, o dejar ausencia
cuando no se certifica. El contraste nativo obligatorio debe atravesar ese
vinculo, no construir manualmente un render state equivalente a la misma
suposicion del parser. I4 promete submit/nativos pero no fija actualmente esa
frontera ni dominio completo de age/model. No hay GameTests ni produccion nueva;
este RED es del plan, no un bug observado en Cow/Pig/Chicken actuales.

Inspeccion de bytecode local confirma la familia submit y que
AdultAndBabyModelPair es record final; model es un slot heredado de
LivingEntityRenderer (referencias en bytecode pueden nombrar CowRenderer), no
un campo declarado propio de MobRenderer. La resolucion debe preservar el
owner real del slot, como obliga I2. Holdouts futuros no revelados.


Nueva pasada independiente del plan completo: RED1 resuelto, funciona, continua.
I2 exige ahora cadena original completa hasta los campos consumidos, alias de
entity/state/partial conservados, componente por identidad exacta, getter
correspondiente y age original sin escrituras posteriores. Tramo no probado,
llamada desconocida u override relevante rechaza la receta sin filas; la clase
del actor y scope original quedan declarados. No se infiere autoridad de una
firma compatible. El owner heredado real del slot model queda correctamente
explicitado como LivingEntityRenderer.

I4 contrasta source publica -> entity nativa -> extract original -> submit
original para dominio completo enum x adult/baby, con negativos de puente
alterado/ausente. Asi el contraejemplo de inversion/sustitucion queda fuera del
contrato admitido y no se valida un render state construido segun la misma
suposicion. Digests de cada recurso y cadena de metodos preservan provenance;
la semantica original y las transformaciones de compat no probadas no se mezclan.

Segunda pasada desde modelo/canon, inputs/ownership, programa completo/aliases,
mapa/dominio, reproducibilidad/inmutabilidad, presupuesto/localidad y regresion
pertinente produce cero cambios adicionales. Plan convergido, sin produccion ni
GameTests nuevos y sin adelantar casos concretos. Parser/receta/procedencia siguen
UNPROVEN hasta recertificacion adversarial. No se certifica pose/root/geometria
fisica/FULL/G6 ni benchmark/aceptacion humana G9 por cerrar este plan.


Reapertura de plan por F1/F2: funciona, continua. Inspeccion independiente del
JAR common fijado confirma531 firmas de Entity en javap (incluidas cuatro
lambda con throws Exception); cap512 no admitia esa dependencia nativa.1024
metodos/clase es ajuste PREPARATION acotado, no presupuesto de fisica ni permiso
para omitir programas. Se conservan512 instrucciones/metodo,64 clases,1MiB por
clase y8MiB total. Cualquier exceso sigue sin filas; no cambio de dominio hot.

Siete digests runtime del reporte de investigacion coinciden independientemente
con los class entries de los JAR split common/clientOnly locales. La comparacion
semantica normalizada del implementador es investigacion de provenance, no un
nuevo PASS adversarial de parser: variantes admitidas conservan digest exacto y
programa completo. La normalizacion de CP/ldc/targets no permite aceptar bytes
futuros por nombre/version ni relajar procedencia, aliases o extractions.

Nueva pasada integral de plan desde el modelo previo y segunda lectura scoped
de budgets/recursos/owners/domain/provenance/regresion produce cero cambios
adicionales. Ajuste converge. Los owners/tests ya escritos siguen candidatos
UNPROVEN; no se atribuye aceptacion productiva ni se ha iniciado la campana de
GameTests/mutaciones del adversario. La entrega compilable y la recertificacion
posterior deben verificar parser, recursos aprobados y limites reales. Se
conservan RED1/F1/F2 y las limitaciones de geometria/pose/FULL/G6/G9 anteriores.


Aceptacion ADVERSARY de candidata: funciona, continua. La lane publica propia
pasa30s con cierre limpio:14 selecciones originales (Cow/Pig/Chicken, dominio
integro enum x edad), comparadas con fuentes publicas, extractRenderState nativo
y el objeto exacto de modelo en SubmitNodeStorage diferido. No rellenar campos
de state como oracle.12 rechazos locales cubren mapa incompleto, pair nula,
owner enum ajeno y modelo adulto nulo; no hay filas parciales. Repeticion produce
la misma receta; colecciones inmutables, recuperacion tras rechazo, slots que
comparten modelo con uniqueModels reducido y estado animado/visibilidad de los
ModelPart compartidos preservados al inspeccionar estructuras iniciales.

Parser independiente Java25 PASS18:512/513 instrucciones,64/65 clases,
1024/1025 metodos, cap1MiB y total8MiB exacto/exceso, identidad de class bytes,
digest obligatorio, handlers rechazados, programa completo/truncado, tipo de
local reference/float, target de rama y cache por identidad sin IO repetido.
Ocho mutantes ejecutados/compilados son detectados (instrucciones, clases,
metodos, cola del programa, digest, ambos tipos de local y cache). No cuentan
fallos de compilacion como sensibilidad. Herramienta reusable:
JAVA_HOME Java25; python tools/s51_adversarial_parser_probe.py --output <dir>.
Lane cliente: runClientGameTest -I tools/s51_adversarial.init.gradle,
scalebrewsBuildDir y s51AdversarialRunDir propios, project-cache-dir propio.

Snapshot SVG/PNG inicial nativo de14 modelos, partial0 y proyeccion fija,
inspeccionado: tamanos/estructuras adult/baby y variantes originales se conservan.
Metadatos fijan familias0Cow/1Pig/2Chicken. Es evidencia de seleccion/estructura
inicial, no draw GPU ni transform root/pose/collider fisico. El replay IMPLEMENTER
pasa14/12 en57s limpio, parser18/8 y SVG identico byte a byte al snapshot propio.
Logs, hashes, fallos de fixture y recetas estan en docs/evidence/S51/adversarial/
y adversarial-verification.json; conservar replays fallidos. A1 fixture uso dos
nombres API incorrectos antes de runtime; A2 EnumMap impedia inyectar owner ajeno
antes del preparador, corregido con Map temporal en el mismo field y finally.
Ninguno fue clasificado como bug productivo ni debilitamiento del oracle.

Recertificacion scoped: OriginalModelPrograms y EnumAgeModelPreparation TRUSTED
para PREPARATION original26.2 de esta familia exacta. Revision integral y segunda
pasada de owners, aliases, fields, dispatch bridges, inputs, recursos, colecciones,
localidad/coste y ausencia producen cero cambios productivos. Solo2 owners nuevos;
369 hashes coinciden con recibo IMPLEMENTER y JARfb5a1a48 (753entradas,
1239403bytes), sin fixtures/test classes. No callers hot/initializer/runtime:
reflection/bytecode/arboles son explicitos PREPARATION. Coste escala por clases,
metodos/instrucciones/bytes, sources declaradas, enum/filas y modelos unicos;
limites y cache pass-local auditados/probados. Sin cache global de renderer/world,
mutable refs en receta ni preparacion por consulta fisica/render normal.
Regresion focal S51 y estructura/censo afectados; Tiny ajeno no repetido.

Cierre scoped no completa G6 tarea2/familias generales ni pose/root/material
fisico/FULL/catalogo UNRESOLVED0, compat transformada ni aceptacion humana G9.
Tooling publico conserva los oracles; one-offs sustituidos se retiran por paths
individuales, preservando evidencia/logs/builds/caches/modelos previos.

## 8. Fallos y bucles

RED1 PLAN: faltaba demostrar procedencia source -> extractRenderState -> campos
consumidos por submit; tipos/mapa no implican ese vínculo. No bug observado en
nativos ni producción nueva. I2 exige ahora cadena original completa y ausencia
sin prueba; I4 atraviesa extracción nativa para todo el dominio. También corregido
owner real heredado LivingEntityRenderer.model. Se devuelve el plan a revisión.

Desarrollo F1: build/compileGameTests PASS39s; primer client focal FAIL39s y
replay diagnóstico FAIL34s por checksum merged != recurso original runtime
split/access-widened. Rechazo conservado; captura de bytes/orígenes y comparación
completa de programas demuestra equivalencia, no se elimina validación. F2:
replay31s rechaza por límite512 métodos insuficiente para Entity original531.
Es error de presupuesto del plan, no rendimiento hot ni prueba física fallida.
Se devuelve sólo este ajuste acotado a fase PLAN/revisión independiente antes
continuar; producción S51 aún UNPROVEN y sin candidato entregado al adversario.
F3 desarrollo/fixture: replay32s llega a extracción pero entidad creada sin ID
provoca fallo original ItemModelResolver; asignar ID/registrar entidad local.
F4 desarrollo/fixture: replay30s asumía identidad del objeto pasado al setter.
SynchedEntityData puede conservar valor previo igual; oráculo correcto es getter
actual, contrastado además con enum solicitado y source pública. F5 fixture/API:
replay32s llega a Pig, cuyo setVariant es privado; usar setComponent público
original para las tres familias. No reparación de producción por estos fixtures.
Tras correcciones, r6 PASS37s con S51 y S45; no ocultar replays fallidos ni tratarlos
como fallos Tiny/solver. Preservar logs/clases capturadas/comparación durable.

## 9. Revisión final

Ambos roles completan dos pasadas integrales sin cambios productivos. Revisadas
procedencia source/extract/submit, dispatch real y bridges, recursos originales,
ausencia/dominio/colecciones, costes PREPARATION/localidad y límites de scope.
Tooling público reproducido por IMPLEMENTER: nativa14/12 PASS57s, parser18/8
exit0, snapshot idéntico e inspeccionado. Recibos369 hashes y JAR conservados;
primer output parser relativo localizado y preservado fuera de checkout.
15 one-offs adversarios y collector/cierre propios retirados por paths exactos;
conservar tools reusables, evidencia/logs/builds/cache. G6/FULL/G9 no cerrados.

## 10. Cierre

- [x] Frontera pertinente/client/oracles/bounds y sensibilidad.
- [x] Ambos reviews completos cero cambios y costes/provenance fijados.
- [x] Canon/evidencia/tooling y commit Sprint:S51, HEAD/árbol verificados.
