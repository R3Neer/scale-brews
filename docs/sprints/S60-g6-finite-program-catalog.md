# S60 - Tablas finitas originales en la revision del catalogo

Estado: cerrado adversarialmente scoped; cierre local Sprint:S60.
IMPLEMENTER raiz, ADVERSARY independiente. Base54e58ca4 S59 cerrado y committed,
98 evidence blobs HEAD exactos/arbol limpio; main b5e62535 vigente/ancestro.
Sin produccion antes de cierre S00/convergencia independiente.

## 1. Tesis y scope

G6 tareas2-3/6; FR-025/026/029/038/039 y NFR-009/010/016/019/021/022/023/028:
seleccionar en common una fila de la tabla original S59, desde inputs explicitos
y recursos de una revision aceptada. Se reutiliza NumericPoseEngine/program;
no nuevo evaluator de aritmetica ni SELECT eager. La tabla solo referencia los
programas numericos ya existentes, evitando otra copia de geometria en el bundle.

No fuente autoritativa desde actor, continuidad de inputs, renderer/world root,
flags/layers/material/selectores fisicos/bindings de especies ni FULL. El selector
de programa responde exclusivamente a bool channels explicitos y exactos; no
certifica que esos channels procedan de un actor. Baseline general intacta.

## 2. Plan IMPLEMENTER

- [x] I1 S00 antes de produccion: reutilizar compiler finite S59 y numeric DAG
  solo con owners/capturas vigentes; auditar Resources/SAM, NumericPoseEngine
  binding/retenidos, registry ownership, catalog transaction y bundle/transfer/
  negotiation/prepared cache en la nueva frontera. No repetir Tiny ni ataques
  previos verdes. Modelo adversarial clean-room previo independiente.
- [x] I2 DTO common FinitePoseProgram format1, source/version bounded, lista
  canonica de boolean channel names (0..5) y filas completas mask -> numeric
  resource ID (1..32). Mask calculado por orden canonico; sin filas duplicadas,
  omitidas, fuera de rango ni default. Deep validatedCopy reconstruye records y
  valida caps antes de copiar/expandir. Boolean names estrictamente ascendentes
  (rechazar desorden, no cambiar bits silenciosamente); filas ordenadas por mask.
  Resource IDs y keys finite <=256chars antes de parse, IDs canonicalizados. Ningun modelo,
  reflection, geometria, arithmetic nodes o estado cliente dentro de la tabla.
- [x] I3 FinitePoseEngine common, reservado scalebrews:finite_numeric_program;
  unico parametro program, tabla revision-local. Bind resuelve tabla y todos sus
  numeric resources, valida cada fila antes de admitir la familia: modelo exacto,
  schema requerido max64 y sin usar boolean channels como float INPUT numericos.
  RequiredChannels incluye predicates y todos los INPUT utilizados por cualquier
  fila; contrato existente exige declarados presentes, aunque rama inactiva.
  Cache local de Bound por ID unico, tabla por mask O(1); despues de bind no
  resources/parser/hash/reflection ni registro global. Bound retiene nombres y
  Bound[] privados, sin resources/DTO/program/geometria/cubos embebidos.
  Evaluate exige ordinary y cada bool presente con Float bits exactos +0.0/1.0;
  -0.0, thresholds, desconocido/ausente no seleccionan. Solo evalua fila elegida
  con el Numeric Bound existente. Direct evaluate sin revision sigue ausente.
- [x] I4 Export PREPARATION desde Prepared S59: tabla y mapa deduplicado de
  NumericPoseProgram existentes. Unificar los schemas float por owner/name/F
  ordenados, remap declarativo de nombres INPUT a float.N; boolean.N mantiene
  orden original owner/name/Z. Union float+bool <=64 antes de emitir. No asumir
  que input.0 de dos filas representa el mismo field. Sin modificar operaciones,
  orden/INITIAL/alias ni compilar formulas de especie. Metadata de fields solo
  describe input explicito; no autoridad actor. IDs/digests reproducibles; reuse
  geometria inicial del Prepared, programas equivalentes deduplicados. B0/1fila
  permitido. APIs inspect finite v1 y linear v2 conservan contrato/captura estrictos.
- [x] I5 WorldAnatomyCatalog posee mapa inmutable finitePosePrograms; overloads
  anteriores delegan mapa vacio. Leer scalebrews/finite_pose_programs bajo limites
  actuales4096/bytes. Canonical IDs/deep validation y referencias numericas
  existentes/geometry consistente en todas filas antes de prepare/swap. Missing,
  malformed, foreign o schema invalido rechazan candidato y preservan snapshot,
  Bound y bundle aceptados anteriores. Nuevo Resources.finiteProgram default empty
  conserva SAM externo y datos de otros engines. Sin parser/lookup hot.
- [x] I6 Bundle lleva finite_pose_programs sorted obligatorio; protocol8 y
  capability aditiva FINITE_POSE_PROGRAMS, DATA_SCHEMA1 y limites/fragments actuales.
  Transfer valida metadata/referencias y todos bindings antes de swap. Preservar
  canonical bytes/revision/epoch cache y rollback; peer7/unknown capability
  rechazado. Overloads viejos source-compatible con mapa vacio. Actualizar asserts
  historicos de generacion del seam afectado, conservando negativos; ADVERSARY
  posee sus fixtures. No editar manifests normales para selectors temporales.
- [x] I7 Build/compile GameTests y lane server focal catalog/binding/transfer/
  negotiation. Original BabyTurtle y Cow explicit inputs: export/captura nativa
  y replay nuevo common con veto cliente, seleccion de todas filas y alias.
  Ausencia/IDs/model mismatch/schema/unavailable y revision isolation/rollback/
  malformed/caps/bundle reproducibility/cache/SAM. Snapshots deterministas contra
  matrices originales de la captura nueva; usar tooling public reutilizable.
  Regresion solo del seam modificado: S57 catalog/transfer, S59 export/finite y
  strict linear si afectado. Sin Tiny/fullclient/server ajenos ni repetir todos
  los mutantes previamente verdes. Registrar solo lo ejecutado.
- [x] I8 Campana independiente posterior a candidata; clasificar fallos antes
  de reparar, coste continuo y sensibilidades pertinentes. Dos reviews completas
  finales cero cambios, receipts/raw/canon/VALIDATION, commit local propio
  Sprint:S60 con INDEX/HEAD byte exactos y arbol limpio antes de avanzar.

## 3. Revisiones y coste previo - IMPLEMENTER

Primera pasada: el runtime numeric actual ya resuelve recursos por revision;
S59 produce filas, pero input.0 no es identidad global de field. El export debe
remapear schemas antes de conectar filas. Reusar el evaluador actual permite
branch selection sin ejecutar operaciones de ramas inactivas. Tabla de referencias
evita repetir geometria/programas adicionales al bundle numeric existente.

Segunda pasada: los booleans exactos son parte de un contrato explicito, nunca
flag() con default/threshold. Familias incompletas/missing resource no admiten
solo fila feliz. Source compatibility no implica peer7 compatibility. Revision
owner, bundle y Bound se cambian atomicamente; nada obtiene authority de actor.

Tercera pasada completa sobre requisitos, ownership/callers, ausencia, schemas,
bounded cost, pruebas pertinentes y limites: cero cambios de plan pendientes.
La lista de required channels conserva contrato actual global (max64); solo el
programa elegido calcula aritmetica. Numeric DAG y strict inspect no necesitan
otra implementacion. La geometria/provenance no certifica render inclusion/FULL.

Cuarta pasada explicita dos bounds de metadata: nombres booleanos ascendentes
obligatorios sin reinterpretar masks y resource IDs/finite keys<=256chars antes
de parse. Relectura completa del plan acotado: cero cambios pendientes; produccion
intacta. No es una ampliacion de semantica ni nueva autoridad.

PREPARATION: B<=5/R<=32; tablas4096 y recursos numeric4096 bajo bytes16MiB globales
de bundle actuales. Tabla O(B+R), sin geometry/nodes duplicados. Export union
inputs <=64 y row nodes/writes<=512; estructura/geometria shared. Catalog bind
O(R+U*(geometry+nodes+writes)), U<=32 IDs unicos, costo de revalidacion numeric
existente explicitado. No caching global. Hot O(B<=5) para mask y acceso O(1),
mas evaluacion numeric existente y sus scratch/resultados por llamada; no nuevo
Map de selector, hashing ni resource lookup por tick. Limites estructurales y
contadores de lookups/retenciones se verificaran; timings totales no TPS/zero
allocation. Packet serialization/hash una vez por revision, reuse por epoch.

## 4. Modelo/convergencia/S00 - ADVERSARY

Rol ADVERSARY. Modelo clean-room post-S59 derivado de requisitos/arquitectura
antes de consultar este plan; SHA256
bffcd0d09fe41c387475a10c7a1b89e8885b5896b22d21263f9a4b22d47b27f3.
No casos concretos nuevos antes de candidata ni divulgacion de holdouts.

Primera pasada independiente: seleccionar un programa no prueba origen de inputs
ni cobertura FULL. Cada tabla debe tener identidad de schema estable, asignaciones
completas y todas sus filas admitidas antes de publicar la familia. Predicates
exactos, ausencia conservadora y operaciones exclusivas de la fila seleccionada;
la presencia de todos los channels declarados conserva el contrato previo.
La unificacion owner/name/F impide confundir labels locales iguales; Z y F siguen
siendo dominios distintos. Compartir geometria/recursos no mezcla revisiones.

Segunda pasada completa: I1-I8 contrastados contra autoridad, compatibilidad,
transaction/rollback, identidad canonica, budgets antes de expansion, lifecycle,
coste/localidad y scope de pruebas. Revision final del orden boolean/mask y cap
256 previo a parse incluida: cero cambios adicionales requeridos. SAM antiguo
compatible por default empty no implica compatibilidad con peer7. El Bound nuevo
no puede capturar Resources/DTO/geometria ni consultar registro en evaluacion.
O(B<=5) seleccion mas evaluator numeric con sus scratch/resultados existentes;
PREPARATION valida todas filas y paga revalidacion cold, sin claim copia unica,
zero allocation, TPS o seleccion autoritativa desde un actor.

S00 pertinente cerrado por reutilizacion scoped verificada y auditoria de codigo
actual. 378 archivos productivos (262 Java/116 recursos) exactos al recibo S59;
307 archivos comunes exactos a la recertificacion final S57. 112 artefactos raw
historicos comprobados por hash, sin volver a ejecutar mutantes ni suites verdes.
Numeric engine/program, Resources/SAM y reserved registry, transaction aceptada,
transfer/cache protocol7 y compiler finite final S59 quedan TRUSTED dentro de sus
contratos ya probados. La extension FinitePoseProgram/Engine/export/protocol8
queda UNPROVEN como nueva candidata hasta campaña independiente; no se promueve
por analogia. Actor/continuidad/root/flags/material/FULL quedan fuera.

Recibo docs/evidence/S60/adversarial-s00-verification.json SHA256
f95952c5117d9d15cca2200b5cae1a63c84e870a1838fdc99d21b1b13421ee48;
audit raw enlazado foundation-reuse-audit.json. Sin JVM/GameTests nuevos en esta
fase ni Tiny/suites ajenas. Snapshots nuevos contra captura original despues de
candidata, sin reutilizar imagen previa como prueba del nuevo export.

Dictamen previo: funciona, continua. Convergencia del plan y fundamentos existentes
cerrada scoped; no aceptacion de produccion futura ni cierre G6.

## 5. Implementacion - IMPLEMENTER

Implementados DTO completo, selector common reservado y export PREPARATION de
schemas tipados. La tabla referencia recursos numeric revision-local; todos
sus rows se validan antes de bind/swap. Unificacion owner/name/F evita labels
locales ambiguos; booleans exactos +0/1 y contrato global required preservados.
Overloads/Resources SAM anteriores delegan defaults vacios; protocol8/capability
FINITE_POSE_PROGRAMS y finite_pose_programs obligatorio, sin cambiar schema1.

Build inicial44s/testCompile3s/final16s PASS; servidor focal19/19 PASS31s
(S60/S57/S04). 400 evaluaciones/3 bind lookups/0 hot lookups, revision isolation,
all-row model/schema admission, caps y rollback/canonical transfer. Cliente focal
PASS26s: BabyTurtle normal+alias real y Cow,9assignments/252 matrices/104 cubos;
S59 afectado140 matrices/72cubos, captura semanticamente exacta al S59 committed.
Common fresh JVM PASS:252matrices/maxDelta3.8743e-7, clientVeto y0hotlookups.

381 archivos productivos (265Java/116recursos),372 previos byte intactos; seis
owners modificados y tres nuevos. JAR bf0eb5e710b9a912235fe30dcec0fa79a17eb4e5fa182c6f6bd61662e983c8d8,
804entries/1359037bytes;607 compiled classes exactos y603 GameTests sin leak.
Manifest ordinario113server/34client restaurado; main b5e62535 sigue ancestro.
112raw previos revalidados por root, sin rerun de sus baterias/mutantes.
Dos reviews propias completas cero cambios pendientes. Bound retiene solamente
names+numeric Bounds; ningun lookup/parser/reflection hot nuevo. Costes cold y
scratch/resultados numeric existentes declarados, sin claim TPS/cero allocation.

Receipt docs/evidence/S60/implementation-verification.json SHA256
3d1d5183cf558e52ff0c55a7dd3a546dc23a9e2c8e4a914e4b4547d2ec1949cc;
artefactos raw enlazados. Candidata UNPROVEN hasta campana independiente.
Seleccion focal: ningun Tiny/fullserver/fullclient ni sensibilidad verde repetida.

## 6. Verificacion y fallos - ADVERSARY

Funciona, continua: aceptacion SCOPED del candidato congelado. Nueva lane publica
S60AdversarialFiniteCatalogProof/tools/s60_adversarial.init.gradle PASS1m34s con
salida integrada/servidor/cliente limpia: BabyTurtle original 26.2, cuatro
asignaciones Z y dos aliases reales, 40 muestras/280 matrices/80 cubos. Nueva
exportacion seleccionada mediante FinitePoseEngine, no bind directo de una fila.
Pureza de current/initial nueve campos, flags y relaciones, repetibilidad e inputs
explicitos; matriz3e-6 y corner oracle S58 intactos. No fuente de actor certificada.

Consumer comun nuevo PASS dos familias/280 matrices/201362 asserts, veto cliente,
Bound detached y 100k evaluaciones por familia sin lookup posterior al bind.
Los 32 masks maximos se ejercitan con outputs distintos; bits -0/fraction/near1,
missing required y recurso inactivo ausente cierran la familia. Una rama numerica
no seleccionada con DIV indefinido no se evalua; al seleccionarla devuelve ausencia.
Cap boolean antes de traversal e ID256/257 cubiertos. No zero-allocation/TPS claim.

Catalogo JVM comun con codec RegistryFriendlyByteBuf real PASS140 matrices/272
asserts: identidad exacta Binding, seis candidatos invalidos con rollback de
snapshot/Bound, canonical bytes independientes de insercion, prepared100 con misma
identidad, table map4096/4097 y reader4097 rechazado antes de abrir recursos.
El Resources SAM previo conserva default empty; protocol8/schema1, peer7 y
capability desconocida rechazados. Export metadata-only PASS9 con local input.0
usado por dos fields distintos y union64/65: control de remap, no modelo original.

Sensibilidad nueva: seis shadows compilados con exit1 y AssertionError semantico:
signed-zero, mask-order, resource-capture, field-alias, union-budget e inactive-eager.
Cinco primeros contra proof inicial preservado; sexto solo tras agregar el control
nuevo de aritmetica inactiva. No se repitieron sensibilidades historicas verdes.
Un fallo clasificado ADVERSARY_FIXTURE en negativo de namespace: IdentifierException
es RuntimeException distinta de IllegalArgumentException. Produccion ya rechazaba;
se amplio solo el catch, raw oracle/log previos preservados. Sin repair productivo.

Snapshots nuevos de ocho filas/sample3 contra referencia de esta captura, ambas
PNG inspeccionadas: envelopes locales recenterados con centros N/C y bases de los
siete joints. Piezas planas articuladas no se presentan como material. No prueba
GPU/render inclusion/root/flags/material/FULL; no imagen previa usada como oracle.

381 productivos (265 Java/116 recursos) exactos a frozen receipt IMPLEMENTER;
607 clases produccion compiladas en lane propia byte iguales al JAR final
bf0eb5e710b9a912235fe30dcec0fa79a17eb4e5fa182c6f6bd61662e983c8d8.
Auditoria cold/hot: nombres y Bounds privados, no DTO/resources/geometria/cubos
retenidos; lookup unico por ID en bind y ninguno en muestras. Preparacion conserva
copias/revalidacion numeric y evaluacion paga sus scratch/results existentes.
Dos reviews completas sobre modelo/I1-I8/callers/ownership/ausencia/coste/caps/
wire/compatibilidad/recibos/snapshots terminan sin cambios productivos requeridos.

Recibo docs/evidence/S60/adversarial-verification.json SHA256
d80c6cc1ded897038112825f10ecc92e8b4070d8db5cb92c21d9834f269d7894;
60 raw y13 superficies publicas pinadas. Receta tools/s60-adversarial-common/README.md.
Solo fronteras modificadas: sin Tiny/fullserver/fullclient ni mutantes previos.
Manifiesto fuente normal113server/34client intacto, init altera solo output propio.
Tools reutilizables conservados; sin one-off privado nuevo en repo ni logs generados
en source tree. Logs/build/cache/worlds/shadows se preservan en outputs propios.
La familia nueva/export/protocolo8 quedan TRUSTED scoped para inputs explicitos;
G6 tareas2-6/G7-G9 y necesidades humanas finales siguen abiertos.

## 7. Cierre

Campana independiente PASS:280matrices/80cubos originales, common201362asserts,
codec/catalog140matrices/272asserts, metadata9 y seis semantic kills nuevos.
Un fallo ADVERSARY_FIXTURE de tipo de excepcion ya rechazado se corrigio en catch;
ningun defecto/reparacion productiva ni debilitamiento del oracle. Root reprodujo
common/publiccatalog y snapshots, sin Tiny/fullsuite ni old green mutations.
Snapshots propios9assignments y publicos8filas vistos; regeneracion propia PNG
byte exacta. Retention javap confirma String[]/Bound[]; source/JAR boundaries exactos.

Dos reviews finales completas propias e independientes requieren cero cambios.
381productivos/372 anteriores byte intactos,607clases exactas al JAR y603GameTests
sin leak; recibos raw verificados, hash root final 78b7c9904e8cce9a4884283b85aa3f118c5d25eb0434273757e2886c504c60ab,
adversarial d80c6cc1ded897038112825f10ecc92e8b4070d8db5cb92c21d9834f269d7894.
Cleanup de docstrings/line endings publico auditado por AST/texto sin rerun verde;
sources ejecutados preservados. One-offs de edicion retirados; tooling reusable
con recetas y scope. Commit propio Sprint:S60 y comprobacion INDEX/HEAD/working
 tree antes de avanzar. G6 general/G7-G9 y necesidades humanas final de epica
siguen abiertos; baseline93Living/92UNRESOLVED+1fixtureSAFE_PARTIAL+0FULL intacta.
TRUSTED exclusivamente finite explicit-input/revision resource selection/export;
ninguna certificacion actor/continuidad/root/flags/layers/material/FULL.
