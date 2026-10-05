# S49 — Fuentes de variantes de componentes enumerados

Estado: CERRADO en scope de readers comunes/lifecycle; commit local Sprint:S49.

## 0. Roles

IMPLEMENTER: raíz, plan y producción. ADVERSARY: agente existente independiente,
modelo previo, holdouts y verificación; no modifica producción. Comunicación
operativa; los escenarios privados no dirigen el plan.

## 1. Scope

Tesis: un componente común cuyo valor sea un enum StringRepresentable puede
alimentar el selector server-safe con su nombre serializado original mediante
una única familia reusable, sin clases físicas por especie ni reflexión por query.
Contribuye a G6 tarea2. FR-004,024,026,033,034; NFR-004,007–010,015–018,024–027.
Dependencias S00/G1–G5 y S46–S48 cerradas en sus ámbitos recertificados.

No certifica modelos/poses originales ni FULL, skins o componentes Holder de
registros dinámicos, familias nuevas de pose, Clinging, VP26 o benchmark G9.
No cambia solver, catálogo/pose wire, monturas, pasajeros, seats ni render.
La batería Tiny Mounts no es regresión pertinente de esta frontera; la integración
completa de G9 conserva su gate. No push/tag/release/install.

## 2. Estado inicial e investigación

HEAD56060818ed3a266eb3135953629c93179be06177, árbol limpio. Fetch origin/main
b5e6253584b2233e5dd0039203c8a251bf10cdeb ya integrado. S48 toma readers/dominos
inmutables por revisión; desconocido retira material y recuperación genera nueva
identidad. Sólo age tiene reader builtin. Estos mecanismos se reutilizan, no se
crea otro resolver/lifecycle.

JAR real Minecraft26.2 comprobado por javap: Entity implementa DataComponentGetter
y expone get(DataComponentType) y setComponent. DataComponents tiene campos públicos
tipados DataComponentType<Enum>, incluidos Fox.Variant, Salmon.Variant, Rabbit.Variant,
Horse Variant y DyeColor; implementan StringRepresentable. Los IDs originales se
obtienen de BuiltInRegistries.DATA_COMPONENT_TYPE, no del nombre Java del campo.
Holder<Variant> es otra familia y queda explícitamente fuera.

## 3. Plan de implementación — IMPLEMENTER

- [x] I1 Factory pública CollisionVariantSources.enumComponent(type,enumClass)
  produce un Source finito inmutable. PREPARATION copia nombres serializados
  una vez, valida dominio/tamaño/Unicode y rechaza nombres duplicados. Reader local
  obtiene el componente y devuelve el String preparado por ordinal. Ausencia,
  tipo incorrecto o enum ajeno nunca se convierten en ordinal compartido/default.
- [x] I2 BuiltInVariantSources descubre en inicialización los campos públicos
  static DataComponentType<E> de DataComponents cuyo E sea enum StringRepresentable.
  Reflexión únicamente PREPARATION, sin lista de mobs. Source key estable
  pública componentKey(Identifier) desde ID de registry: prefijo component.,
  namespace/path separados por un punto; '_' se codifica _5f, '.' _2e y '/' _2f
  dentro de cada segmento. Se escapan también los '_' literales, evitando alias
  con una secuencia de escape. Ejemplo minecraft:fox/variant corresponde a
  component.minecraft.fox_2fvariant. El resultado debe cumplir <=64 caracteres;
  exceso falla explícitamente al preparar, sin truncar/filtrar sources prometidas.
  Orden determinista, budget existente, rechazo
  de colisión/ownership en vez de sobreescritura. Cada source builtin conserva
  identidad del reader para initialize idempotente, incluido age original.
- [x] I3 Pruebas IMPLEMENTER con componentes reales de varias entidades y ausencia
  en tercero, nombres originales y transiciones live usando índice/lifecycle S48;
  fake enum de extensión valida factory/budgets sin reflection runtime. Compile
  producción/GameTests y regresiones seleccionadas de preparación/selección/lifecycle.
- [x] I4 Review completo cero cambios, verificación independiente, costes/negativos
  y snapshots si la evidencia espacial lo requiere, canon/VALIDATION y commit propio.

## 4. Presupuestos y revisión del plan — IMPLEMENTER

PREPARATION: F campos de DataComponents y E<=256 constantes por enum; O(F+E)
más orden de keys. Ningún renderer/model introspection ni dependencia cliente.
HOT_TICK/HOT_MOVE_QUERY: una lectura local de componente y acceso array O(1),
sin reflexión, registry lock, stream, hash ni Strings temporales. Estado fijo de
la source/revisión; no cache por entidad/mundo ni crecimiento por query.
Seleccionar inputs cambiados conserva coste/eventos y causalidad S48.

P1: una familia de observación de componentes, no nuevos colliders por mob.
P2: nombres preparados por ordinal y guard de enum propietario exacto
(getDeclaringClass admite cuerpos propios de constantes); no enum.name/toString
en runtime, ni asumir ordinal equivalente entre enums. P3: IDs serializados en vez
de nombres Java y ownership estable; los campos Holder no se interpretan como enums.
P4: pasada contra requisitos, arquitectura, límites, clientes, lifecycle y mainline
produce cero cambios. RED1 previo obliga a revisar I2: los IDs originales incluyen
'/' y no admiten interpolación literal. P5 fija encoding inyectivo de separadores
y escape, helper público único para datos/consumers, sin cambiar el vocabulario
de keys existente ni presupuestos. Revisión completa posterior produce cero
cambios; la nueva convergencia independiente se completa antes de producción (§5/§7).

## 5. Modelo adversarial previo — ADVERSARY

Modelo independiente preparado desde canon antes de consultar este plan; base
56060818, privado en ../g6-adversary-private/next-preimplementation-model.md.
Scope S49 aceptable como dependencia acotada de G6 tarea2: observar un componente
server-safe real no certifica el dispatch de modelo/pose original ni promueve FULL.

Obligaciones: dominio finito completo por identidad del enum declarado, nombre
serializado original preparado una vez y lectura local sin reflexión/codec/hash.
Ordinal sirve sólo dentro de ese enum propietario; ausencia/tipo incompatible es
unknown, no input vacío ni default. La factory reusable debe conservar identidad
semántica también cuando una constante tenga implementación particular: la clase
propietaria del enum se distingue de la clase runtime de la constante. Descubrir
campos no prueba que las sources sean registrables ni que lean el estado real.

Las fuentes deben ser expresables en el vocabulario canónico de keys sin omitir
las originales prometidas. IDs diferentes no se confunden; bounds/colisiones y
ownership tienen outcome explícito. Preparación captura inputs y registro conforme
S48; cambios live reales deben atravesar retirada/rebind exactos, y la misma
lectura estable debe reusar Source/fila sin crecimiento por entidad/query.

TRUSTED con alcance anterior: índice/token S46, transporte S47, observación,
lifecycle/clock local S48 y censo S45 como inventario. UNPROVEN: nueva factory,
identidad de sources por componente, descubrimiento/codificación de keys y su
admisión real; estos owners extendidos necesitan recertificación scoped.
PREPARATION escala con campos y suma de constantes, dominio <=256 por enum;
HOT_TICK/HOT_MOVE_QUERY debe conservar O(1) por reader además del resolver S48.
No reflexión ni recorridos registry/model por query. Sin memoria por mundo/actor.

Regresión pertinente: factory/discovery/preparación, componentes nativos y la
frontera live afectada. Cliente/dedicated sólo si cambia su ruta o lo exige el
scope convergido; Tiny Mounts no se repite por esta observación local sin modificar
monturas/pasajeros/render. Modelo no diseña aún fixtures ni holdouts concretos.
Primera revisión del plan: NO CONVERGE por RED1 observable de key original.
Segunda pasada independiente I1–I4/P1–P5: CONVERGE tras corregir RED1. El encoding
admite '/' real, separa namespace/path y escapa los propios caracteres de escape:
segmentos distintos no colapsan en una key común. Límite64/outcome explícito no
trunca ni oculta la familia original. P2 precisa getDeclaringClass y conserva
constantes con implementación propia sin aceptar un enum ajeno por ordinal.
Factory/captured registry/localidad y ownership S48 mantienen sus contratos.
La frontera acotada de readers originales es una dependencia válida anterior a
certificar dispatch de modelos/familias. No se exige esa certificación fuera de
scope ni se interpreta esta convergencia como FULL/G6 cerrado. Segunda revisión
contra el modelo completo no exige más cambios de plan. Holdouts concretos y
batería independiente se construirán después de producción; owners nuevos o
extendidos siguen UNPROVEN hasta su recertificación, y no hay resultados runtime.

## 6. Implementación — IMPLEMENTER

Producción: únicamente CollisionVariantSources y BuiltInVariantSources. La
factory prepara un array privado de nombres y dominio inmutable, el reader hace
get(component), verifica getDeclaringClass y retorna un nombre preparado. La
inicialización descubre tipos enum serializables desde firmas genéricas de campos
DataComponents y IDs reales del registry; mapa ordenado inmutable y readers
estables. No nueva enumeración de mundo, renderer, pose, solver ni estado de entidad.
componentKey es el helper público único para sources/datos externos; escapes y
límite64 se validan en PREPARATION, nunca durante lectura.

Pruebas de desarrollo: S49ImplementerEnumPreparationTests se incorpora al manifest
normal por no mutar el runtime global; S49ImplementerComponentLifecycleTests corre
aislada con geometría sintética explícita para demostrar sólo selección/retirada
real vía componente, no modelo original. Selector reusable en tools/s49_implementer.init.gradle.
Build y compilación iniciales PASS45s; primera lane llega a native/lifecycle pero
F1 en fixture; corregida, lane completa4/4 y build PASS34s. Regresión pertinente
S48ImplementerLiveVariantTests+S48AdversarialVariantPreparationTests8/8 PASS38s:
fuentes capturadas/unknown,32/33axes, retiro/recuperación/manual/fences;
100000lecturas estables con312bytes. Se restaura manifest CI109servidor/34cliente
sin selector local (PASS1s), sin atribuirle una ejecución completa. JAR real745
entries/1207866bytes, SHA25665f06fd1c55bae5ae0adc78cb5ae951a4494024addbeffebbd051bb3258f756a;
cero test leakage y367hashes fijados, exactamente2owners productivos cambiaron
frente S48. Evidencia en docs/evidence/S49/implementation-verification.json.
Bytecode compilado del reader verifica get(component)/Enum.getDeclaringClass/
ordinal/aaload; no introspección, asignación, codec ni hash en ese camino.
Review completo IMPLEMENTER de
producción contra I1–I2, tipos, budgets, ownership, callers, fail-closed, API aditiva,
costes y mainline produce cero cambios productivos. La aceptación independiente
de §7 recertifica PREPARATION/reader como TRUSTED únicamente en este scope.

## 7. Verificación adversarial — ADVERSARY

PREIMPLEMENTACIÓN, sin GameTests ni producción nueva. RED1 PLAN/ADMISSION:
I2 forma component.<namespace>.<path> y admite sólo IDs compatibles con el
vocabulario existente. Minecraft26.2 original registra FOX_VARIANT como
minecraft:fox/variant, SALMON_SIZE como minecraft:salmon/size, RABBIT_VARIANT como
minecraft:rabbit/variant y HORSE_VARIANT como minecraft:horse/variant. Sus paths
incluyen '/'; CollisionVariantSources.register sólo acepta [a-z0-9_.-]{1,64}.
La construcción literal produce component.minecraft.fox/variant, rechazada.
Así esas fuentes originales no quedan registrables y I3 no puede atravesarlas:
sus ejes resultan desconocidos/PLAN_UNAVAILABLE antes de cualquier selección live.

Prueba inspeccionada: bytecode del JAR common26.2 original, SHA256
EC9C864E8A304BE37D1D273FEFEFCB4E4DCC2154A18CCD3C1FE9D2D0A298A7D9.
javap DataComponents:177/179/191/209 declara sus tipos enum;1648/1652/1676/1712
muestra los IDs de registro (../s49-adversary-private/DataComponents.javap.txt).
Regex evaluada para las cuatro keys literales devuelve false. No se ha diseñado
un test de implementación ni consultado fixtures históricos para encontrarlo.

Debe converger una representación explícita, determinista y no ambigua de los
IDs originales que permita esa familia real bajo el contrato de keys/bounds;
filtrar justamente los componentes prometidos no resuelve la dependencia.
Conservar ownership y diferenciar IDs aunque contengan separadores. Falta nueva
pasada de plan tras la corrección; FULL/G6/G9 siguen abiertos.

Resultado de esa pasada: RED1 resuelto en el contrato de I2/P5. La representación
component.minecraft.fox_2fvariant es registrable sin ampliar el vocabulario;
separadores/escape codificados preservan identidad, incluido el '_' literal.
No hubo nueva modificación productiva ni diseño de tests concretos. Review final
independiente del plan produce cero cambios adicionales: funciona, continúa.
Pruebas de implementación, sensibilidad/bounds y evidencia afectada siguen
pendientes; esta sección registra aceptación del plan, no ejecución de S49.


Aceptación independiente de producción, pasada final: funciona, continúa.
Lane pública reusable tools/s49_adversarial.init.gradle PASS4/4, cierre limpio
1m11s. Dos tests de preparación son promovibles al manifest normal; lifecycle
permanece aislado por start/reload/stop del runtime. Fox y Salmon originales
recorren sus enums reales; Fox A-B-A retira contacto confirmado sin desplazar el
cuerpo, evita material viejo y exige token/generación nuevos. Geometría cúbica
sintética explícita sólo prueba selección y retirada, no el modelo original.
18 dominios enum originales admitidos,17 Holder excluidos y132 IDs contrastados
con encoding independiente sin alias. Límites key64/65, dominio256/257 y texto
256/257; Unicode malformado y nombres duplicados rechazados. Constante enum con
cuerpo propio válida, enum ajeno con el mismo ordinal ausente.100000 lecturas
no vuelven a serializar;3400bytes en la lane pública bajo cota4096 (no cero ni
benchmark global).1000 queries live conservan provider/generación.

Sensibilidad7/7 RED esperados tras compilar/ejecutar shadows aislados: enum ajeno,
getClass en vez de declaringClass, serializer runtime, alias de key, discovery
omitido, copia registry en reader y ownership ciego a un reader ajeno con el
mismo dominio. El control de ownership restaura exactamente su registro en
finally. Tooling público genera esos shadows sin editar producción. Dos primeras
compilaciones adversariales fallaron por import AnatomyFilter ausente/incorrecto;
se conserva evidencia, no se contabilizan como defecto productivo ni ejecución.

Ocho gates PASS;367 hashes de producción contrastados con receipt IMPLEMENTER;
sólo cambian los dos owners previstos. Las dos pasadas completas independientes
sobre typing, admisión, identidad, ownership, lifecycle, API/callers, ausencia,
coste y evidencia concluyen cero cambios productivos. Reflection/discovery y
codec son PREPARATION; reader hace un get local, guard enum y ordinal sobre array
capturado, sin scan de entidades ni asignación dependiente del registry.
Recibo reproducible docs/evidence/S49/adversarial-verification.json y logs durables
en adversarial/. No repetición de Tiny Mounts: no cambia su frontera; G9 mantiene
aceptación global/humana. Esta dependencia queda TRUSTED en su scope; familias,
selectors/engines originales restantes y FULL/G6/G9 no quedan cerrados por S49.


Revisión documental final y limpieza operativa PASS:20 one-offs reemplazados se
retiran por paths exactos, sin borrar directorios. Logs/cachés/builds, modelos
previos y recibos se conservan. Inventario/hash de retirada en
docs/evidence/S49/adversarial-private-cleanup.json. Tooling público, tres fixtures
y oráculos conservados; nueva lectura UTF-8 y contraste de recibos cero cambios.

## 8. Fallos y bucles

RED1 PLAN antes de producción: IDs originales con '/' no caben en una key literal.
Corregido el plan mediante encoding explícito sin alias y helper común; no se
modificó producción para parchear el fallo. La verificación independiente de la
nueva pasada converge antes de producción, como registra §5/§7.

F1 TOOL/fixture IMPLEMENTER: la primera lane S49 ejecutó 4 tests y pasó native
components/lifecycle, pero el builder de la extensión sintética carecía del Codec
que exige Minecraft26.2. BUILD FAILED30s; no es defecto productivo ni PASS global.
Primer intento Codec.unit falla compilación2s: DFU10.0.21 ya no expone ese método.
API instalada verificada y fixture usa Codec.STRING.xmap con valor fijo test-only
para alcanzar el oracle del dominio; ese Codec no es fuente de nombres del reader.
Se repite toda la lane nueva afectada; no se repite Tiny Mounts.

## 9. Revisión final

IMPLEMENTER: primera pasada completa de owners productivos y callers tras4/4
nuevos+8/8 afectados: cero cambios. Segunda pasada contra scope/canon, API aditiva,
identidad prepared/live, generación/material, límites, typing/ownership, mainline,
costes y pruebas: cero cambios productivos. Se verifica además ausencia de tests
en JAR real,367hashes con sólo2owners cambiados y selector público promovido
(processGametestResources PASS2s) equivalente en sus dos targets nuevos; nueva
restauración ordinaria PASS1s. Se conserva el tooling reusable y se retira el
one-off después de capturar recibos. No atribuye GameTests al proceso de recursos.

Convergencia final independiente sin cambios. Root reproduce lane pública4/4
PASS1m23s y preparación promovida5/5 PASS28s; reader nativo100000lecturas0bytes
en esos replays. No equivale al cursor312bytes ni al benchmark global. El último
proceso ordinario de recursos PASS1s restaura110servidor/34cliente. Recibos de
ambos roles coinciden en367hashes; dos owners cambian, API aditiva sin delta
cliente/mount/solver. Sensibilidad7/7 y ocho gates independientes no exigen
correcciones. Canon, límites, clasificación y cleanup revisados: cero cambios
adicionales. Cada resultado y fallo conserva su log; no se repite batería ajena.

## 10. Cierre

- [x] Regresiones pertinentes, suite nueva y niveles exigidos.
- [x] Build/GameTests compilados, oracles independientes y límites verificados.
- [x] Canon/VALIDATION/evidencia reproducible y tooling temporal resuelto.
- [x] Ambos reviews finales cero cambios; cierre en commit local Sprint:S49,
  HEAD/árbol verificados antes de avanzar al siguiente sprint.
