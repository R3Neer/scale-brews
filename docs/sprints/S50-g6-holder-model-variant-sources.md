# S50 — Fuente estructural de modelo en componentes Holder

Estado: CERRADO en scope de proyección modelo/lifecycle; commit local Sprint:S50.

## 0. Roles

IMPLEMENTER raíz: plan/producción/desarrollo. ADVERSARY existente independiente:
modelo previo y holdouts/evidencia; no modifica producción. Comunicación operacional.

## 1. Scope

Tesis: al terminar, un componente Holder de un record con un único
ModelAndTexture<Enum StringRepresentable> podrá observar su modelo estructural
original mediante una familia server-safe reusable, demostrable con componentes
reales y lifecycle existente, sin depender de IDs dinámicos ni texturas/modelos cliente.
Contribuye G6 tarea2; FR-004/024/026/033/034; NFR-004/007–010/015–018/024–027.
No certifica geometría/pose original, FULL, todos los Holder, skins, G7–G9.
Solver, wire, monturas/pasajeros/render no cambian; regresión Tiny ajena no aplica.

## 2. Investigación y dependencias

HEAD1c29e620a32558212488762c06cabd49d8213d41 limpio. Fetch main actual
b5e6253584b2233e5dd0039203c8a251bf10cdeb, sin commits pendientes de integración.
S00/G1–G5 y S45–S49 TRUSTED sólo en sus scopes; índice/captura/lifecycle se reutilizan.
JAR common Minecraft26.2 SHA ec9c864e8a304be37d1d273fefefcb4e4dcc2154a18ccd3c1fe9d2d0a298a7d9:
DataComponents contiene Holder<CowVariant/PigVariant/ChickenVariant>. Esos records
contienen ModelAndTexture<ModelType>, modelo enum serializable finito, y texturas
y condiciones cosméticas/de spawn separadas. Renderers originales consultan
modelAndTexture().model() para elegir su mapa de modelos. Holder direct/reference
exponen value/isBound; no se exige una identidad de registry para este eje de modelo.
Otras firmas Holder no implican esta tecnología y no se interpretan por conjetura.

## 3. Plan IMPLEMENTER

- [x] I1 CollisionVariantSources.holderModelComponent(component,recordType)
  prepara Source finito desde el único RecordComponent de tipo genérico exacto
  ModelAndTexture<E>, E enum StringRepresentable. Clase record pública, schema
  ausente/ambiguo/no tipado no se acepta; sin seleccionar un primer campo arbitrario.
  Reusar captura/validación de nombres de S49, incluido enum propietario y budgets.
  Accesor se enlaza una vez con MethodHandles.publicLookup/unreflect y se adapta
  a firma exacta Object→ModelAndTexture. Runtime no usa Method.invoke/varargs.
- [x] I2 Reader local obtiene componente, exige Holder bound y valor del record
  propietario, usa accesor preparado y ModelAndTexture.model; enum ajeno/null/
  componente ausente devuelve unknown. Nunca default, nombre registry, textura,
  serializer ni reflexão runtime. Excepción del accesor falla cerrado vía cursor.
  No cache por entidad/mundo ni memoria que crezca con las lecturas.
- [x] I3 Discovery PREPARATION amplía firmas DataComponentType<Holder<V>> sólo
  cuando V cumple la tecnología anterior. Keys públicas modelComponentKey(id)
  usan componentKey(id)+".model", validando otra vez límite64. La separación de
  proyección no colisiona con '.' de IDs (codificado _2e); exceso falla explícito.
  No lista de especies, registros dinámicos ni clientes. Readers/ownership/idempotencia
  originales se conservan. Clase compatible con schema inválido no se omite silenciosamente.
- [x] I4 Desarrollo con componentes nativos de familias distintas, Holder direct
  y reference, ausencia/modelo/textura, transición A-B-A en runtime S48. Geometría
  sintética explícita sólo como fixture de selección. Compilar build/GameTests y
  repetir frontera S49 afectada, no Tiny/client ajenos. Promover sólo tests sin
  ownership de runtime global; selector aislado para lifecycle.
- [x] I5 Review completo sin cambios, verificación independiente posterior,
  negativos/sensibilidad/localidad/allocs, canon/VALIDATION/recibos y commit Sprint:S50.

## 4. Revisión y presupuestos IMPLEMENTER

PREPARATION O(F+C+E) en campos/record components/constantes <=256, nombres
<=256chars, keys<=64, sources<=1024; reflection/method linking sólo allí.
HOT_TICK/HOT_MOVE_QUERY O(1): componente/Holder/record/model y array preparado,
sin scan registry/mundo, serializer, strings/copias/locks ni invocación varargs.
Igual modelo con distinta textura/ID conserva selección; modelo diferente retira
material con lifecycle/clock existentes. No se afirma coste del accessor por
estructura solamente: probe de allocs y revisión bytecode forman aceptación focal.
Primera pasada contra requisitos/API/ownership/ausencia/tipos/mainline/costes
concluye cero cambios de plan. Segunda pasada confirma que el eje es modelo,
no identidad de variante completa; la deuda de Holder sin modelo enum permanece.

## 5. Modelo previo — ADVERSARY

Modelo previo independiente construido desde canon tras S49, antes de este
plan y sin tests concretos. La observación de un eje de modelo no equivale a la
identidad completa de Holder ni certifica estructura/pose/FULL. Su corrección
exige que ese eje coincida con el valor autoritativo original y que textura/ID
cosméticos no provoquen decisiones materiales. Unknown no equivale a default;
valor/modelo/propietario fuera del contrato debe retirar material mediante el
cursor ya certificado, con control positivo del estado admitido.

El esquema preparado debe ser único y tipado: record público, accessor exacto,
ModelAndTexture de enum propietario serializable y dominio finito válido. Un
campo conveniente o una reflexión de runtime no demuestra esa tecnología. La
preparación debe rechazar ambigüedad/malformación de schemas declarados compatibles;
no reducir discovery silenciosamente. Direct/reference representan el mismo eje
cuando su valor es resoluble; ausencia no obtiene una identidad registry inventada.

S46–S49 permanecen TRUSTED sólo en scopes índice/token/retirada/enum ya probados.
Extensión Holder/accessor/discovery vuelve UNPROVEN hasta ejecución adversarial.
Retiro local, nueva generación/clock y ownership manual/captura de revisión se
preservan, sin tabla cliente alternativa ni intervalos entre filas. El plan debe
mantener aparte deuda de otros Holder/skins y decisiones originales no verificadas.

PREPARATION escala por campos/record components/dominio dentro de bounds;
HOT_TICK/HOT_MOVE_QUERY por una lectura local y accessor preparado, sin scans,
hash/codec, varargs, strings por lectura ni retención por entidad. Declaring enum
no se confunde con clase de constante que implementa cuerpo propio. La medición
local y sensibilidad posterior verifican coste y no sustituyen benchmark G9.

Regresión de preparación S49 y lifecycle S48 pertinente, sin Tiny Mounts ni cliente
renderer ajeno. Si el scope sigue sin cambiar selección espacial/render, se deja
ese límite explícito; certificar modelo original requerirá oracle/capturas reales
en el futuro scope de familia. Holdouts concretos permanecen por diseñar tras
producción, según el protocolo.

## 6. Implementación — IMPLEMENTER

Dos owners productivos: CollisionVariantSources y BuiltInVariantSources. Factory
aditiva prepara schema/MethodHandle y reutiliza enumProjection con dominio y guard
S49. Accesor invokeExact Object→ModelAndTexture enlazado una vez; Holder bound,
record exacto y enum propietario se comprueban localmente. Discovery distingue
enum directo y Holder record con model field; candidatos mal tipados/ambiguos
llegan a rechazo de factory. Keys tienen proyección .model con bounds explícitos.
Sin cache nueva ni modificaciones en eventos/cursor/solver/wire/render.

Review completo IMPLEMENTER de tipos/ownership/nulls/exception paths, callers,
API aditiva, costes y límites produce cero cambios. Excepciones ordinarias del
accessor se propagan como RuntimeException al cursor fail-closed; errores fatales
no se reinterpretan como estado normal. Build+compilación GameTests y lane nueva
PASS3/3 en52s, clean exit. El bytecode del reader verifica invokeExact con firma
Object→ModelAndTexture, sin array varargs/reflexión, y guard enum compartido.
Regresión focal S49 PASS6/6 en15s, clean exit; guard/domains/ownership/discovery
y lifecycle nativo anterior,100000lecturas de enum directo0bytes. Recursos
ordinarios restaurados PASS920ms,111servidor/34cliente sin claim de full suite.
Dos reviews productivos completos cero cambios. Recibo IMPLEMENTER367hashes,
exactamente dos owners cambiados, JAR745entries/1210193bytes SHA256
6b822e029a27b265bb9b2630c743e47fdb02c63d64303486941b622fc95ed858.
Fixtures cúbicas son selección/lifecycle exclusivamente, nunca certificación
de Cow original. Público independiente5/5 PASS55s y reproducción root5/5 PASS49s.
Preparación promovida5/5 PASS18s, reader instrumentado100k0bytes y componente
Cow nativo100k0bytes tras warmup100k; no claim global de TPS/GC/p95/p99.
Restauración final ordinaria PASS1s,112servidor/34cliente. Recursos/test selectors
reutilizables, ninguna repetición Tiny/client ajena. La aceptación de §7 recertifica
Holder/schema/accessor como TRUSTED sólo en este scope.

## 7. Verificación — ADVERSARY

PREIMPLEMENTACIÓN: funciona, continúa. Primera pasada independiente contra
modelo completo/canon no detecta contradicción observable. La tesis acotada acepta
la proyección de modelo y no promete identidad completa del Holder ni cobertura.
Keys separan la proyección .model de puntos codificados del ID y exigen límite64;
schema/enum propietario y nombres preparados conservan admisión de S49. Ausencia,
Holder no resoluble y accessor fallido permanecen cerrados sin cache global.

Inspección independiente del bytecode Minecraft26.2 common confirma los records
CowVariant/PigVariant/ChickenVariant con ModelAndTexture<ModelType>; renderer real
Cow/Pig/Chicken consulta modelAndTexture().model(), Map.get y AdultAndBabyModelPair
según isBaby. Por tanto el eje de modelo es dependencia real reusable, pero la
fuente por sí sola no certifica la selección adicional por edad, transforms ni
pose. No se usa ese bytecode como lista Java de especies del core.

Segunda pasada integral de plan, ownership, exactitud/scope, tipos/bounds,
reproducibilidad, PREPARATION/runtime y regresión pertinente produce cero cambios.
Plan convergido; fundamentos nuevos siguen UNPROVEN para aceptación productiva.
No hay producción nueva, GameTests, mutaciones ni resultado runtime S50; G6 tareas
2–6, G7–G9 y necesidades humanas finales permanecen abiertos.


Aceptación productiva scoped: funciona, continúa. Lane pública independiente
PASS5/5 limpio55s: preparación/schema/reader, cursor ante fallo, población original,
y lifecycle real más sentinel. Cuatro proyecciones originales (Cow/Pig/Chicken/
ZombieNautilus),13 Holder restantes ausentes; referencias registry originales y
Holder direct observan todos sus enums. Textura no participa en el eje modelo.
Cow A-textura-B-A conserva contacto/material ante textura y retira contacto real
al cambiar modelo, sin mover cuerpo, query-endpoint ni revivir A.1000 consultas
estables conservan provider/generación. Cursor elimina selected ante RuntimeException
real del accessor y unknown; recuperación conocida positiva.

Factory rechaza schema ausente/ambiguo/raw/genérico/no-enum/no-public; guarda record
y enum propietarios exactos, incluida constante con cuerpo propio. Unbound se
prueba con Holder.Reference real, nunca con un valor inventado. Keys64/65 y
separación .model contrastados independientemente.100000 lecturas tras100000 de
warmup:0bytes, bajo4096, exactamente un component get y un accessor por lectura;
no serializer runtime. Bytecode propio verifica invokeExact Object→ModelAndTexture.

Diez mutantes compilados/ejecutados RED esperados: bound, record owner, schema
ambiguo, enum ajeno, key bound, discovery omitido, serializer runtime, registry
copy, doble lectura y accessor varargs. Los dos mutantes de coste asignan
104000000 y23200000bytes/100k respectivamente. Tool público genera shadows
idénticos a los ejecutados, sin editar producción. Ocho gates PASS.

Fallos propios TEST preservados: suposición inicial de tres proyecciones era
incorrecta (bytecode/discovery confirma cuarta); Proxy sobre Holder sealed no
es fixture válido, sustituido por Reference real. Warmup20k midió4144bytes contra
4096; se aumenta warmup100k sin cambiar cota ni medición100k, final0bytes. Ninguno
se atribuye a producción. Dos reviews completos scoped cero cambios;367 hashes,
JAR real/sin test leakage y cuatro archivos públicos contrastados en
 docs/evidence/S50/adversarial-verification.json; logs durables en adversarial/.
Preparación3tests segura para manifest; lifecycle permanece aislado por Runtime.
No geometría/pose original/FULL ni benchmark global; no nueva frontera espacial
para una captura adicional. Tiny Mounts ajeno no repetido. Fuente Holder/accessor
queda TRUSTED en este scope y sus dependencia extendidas recertificadas; tareas
G6 restantes, G7–G9 y necesidades humanas permanecen abiertas.


Cierre documental final independiente PASS:25 one-offs sustituidos retirados
individualmente tras replay público, sin eliminar directorios/logs/cachés/modelos.
Inventario/hashes en docs/evidence/S50/adversarial-private-cleanup.json. Nueva
lectura UTF-8, recibos y cuatro archivos públicos exactos;367 hashes productivos
intactos. Ningún bloqueo de cierre scoped S50; no se avanza la cobertura global.

## 8. Fallos y bucles

Sin fallos productivos nuevos. Los fallos TEST/probe independientes de población,
Holder sealed y warmup están clasificados y preservados en §7/receipt adversarial;
se corrigen fixtures sin filtrar fuentes ni elevar la cota de allocs. Un defecto
documental de encoding en el párrafo de aceptación se devuelve a ADVERSARY antes
del commit; no requiere repetir GameTests ni modificar producción/oracles.

## 9. Revisión final

IMPLEMENTER: revisión completa después de3/3+6/6+replay5/5 y preparación5/5
produce cero cambios productivos. Segundo review integral contra scope/canon,
schema/tipos/unknown/ownership, captura/clock/contacto, callers, budgets/coste,
mainline y evidencia también cero cambios. Bytecode/JAR367hashes coinciden con
receipt independiente y sólo los dos owners cambian; ninguna modificación
cliente/mount/solver. Diez mutantes y ocho gates independientes aceptados con
sus límites. Cleanup individual conserva evidencia/tooling público. Canon y
resultados reflejan sólo ejecuciones reales; G6 general/G7–G9 no se cierran.

## 10. Cierre

- [x] Regresiones pertinentes y oracles independientes.
- [x] Dos reviews completos cero cambios y límites/costes verificados.
- [x] Evidencia/canon/tooling; commit local Sprint:S50 y HEAD/árbol verificados
  antes de avanzar al siguiente sprint.
