# Arquitectura del sistema de colisiones entre entidades

Estado: arquitectura objetivo del subsistema. Los requisitos normativos viven exclusivamente en [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md), el orden y estado de implementación en [ENTITY_COLLISIONS_PLAN](ENTITY_COLLISIONS_PLAN.md), y la evidencia ejecutada en [VALIDATION](VALIDATION.md).

## 1. Propósito y frontera

Scale Brews ofrece un único motor físico para que cuerpos elegibles contacten, se apoyen, se desplacen y sean transportados por la anatomía de entidades vivientes. El contacto no crea una relación de pasajero y no sustituye la física vanilla fuera de la pareja gestionada.

La arquitectura separa **forma**, **pose**, **transformación raíz**, **política**, **resolución física**, **autoridad de red** y **presentación**. Esa separación permite cubrir familias completas de mobs sin escribir una implementación por especie.

Clinging Reoriented es consumidor de la frontera pública: conserva input, cargas, Gravity Changer, efectos, persistencia y cámara propios. No posee un collider, carry o reconciliador alternativo para entidades que Scale gestiona.

`chatgpt-editing` tampoco es propietario de las demás mecánicas de producto de Scale Brews. `main` puede seguir evolucionando Tiny Mounts, controles, equipamiento, inventarios, alcance, gravedad efectiva, render/attachments, gamefeel u otros subsistemas mientras este proyecto continúa. Al integrar cambios, la arquitectura de entity collisions debe adaptarse a la **ruta productiva vigente de `main`**, no restaurar una fotografía histórica del repositorio. Sólo un conflicto explícito con un requisito canónico de entity collisions justifica cambiar una mecánica mainline ajena.

## 2. Fuentes de verdad

| Información | Documento canónico |
| --- | --- |
| Qué debe hacer el sistema y cómo se acepta | `ENTITY_COLLISIONS_REQUIREMENTS.md` |
| Arquitectura, API, data model y ownership | este documento |
| Orden de implementación y estado de tareas | `ENTITY_COLLISIONS_PLAN.md` |
| Pruebas realmente ejecutadas y límites de evidencia | `VALIDATION.md` |
| Historial de versiones publicadas | `../CHANGELOG.md` |

README, GUIDE, CONFIGURATION y TODO sólo enlazan o resumen a nivel de producto; no mantienen otra especificación del subsistema. No obstante, el comportamiento de producto que ya existe en el `main` actual sigue siendo una dependencia externa real: que no se replique aquí como especificación no autoriza a borrarlo durante una migración.

## 3. Modelo conceptual

```text
Entity binding
   │
   ├── GeometryEngine ──> ModelGeometry
   ├── PoseEngine ──────> local part transforms
   ├── RootTransformProvider ──> entity/world transform
   └── CollisionPolicy ──> eligibility/friction/state gates
                              │
                              ▼
                    Causal geometry endpoint
                              │
                ┌─────────────┴─────────────┐
                │                           │
          material interval           instantaneous queries
                │                           │
                ▼                           ▼
      continuous collision            raycast/clearance
        + contact + carry
                │
                ▼
        authoritative contact
                │
         ┌──────┴──────┐
         ▼             ▼
   local prediction   remote presentation
```

Una geometría instantánea responde «dónde está ahora». Un intervalo material certificado responde «qué espacio recorrió entre dos estados». El solver no infiere el segundo a partir de dos snapshots inconexos.

## 4. Capas

### 4.1 API pública

La API pública expresa capabilities y operaciones de alto nivel; no expone mapas internos, colas, networking concreto ni clases de renderer.

La superficie pública incluye equivalentes versionados de:

- `mode`, `ready` y ownership;
- consulta de soporte y estado supported;
- liberación explícita de contacto;
- clearance anatómico;
- raycast material;
- attach después de un placement validado;
- lectura de gravity frame;
- registro de adapters/engines permitidos por el SPI.

`spaceClear` consulta únicamente anatomía configurada. El consumidor sigue siendo responsable de bloques y de su propia semántica de placement/preflight. `attachAtContact` crea un anchor después de validar identidad y elegibilidad; no teletransporta el cuerpo.

La gravedad externa tiene un único owner registrable. Scale la lee; no decide la política de input del propietario.

### 4.2 Data model y catálogo

Un binding declarativo asocia un tipo/variant de entidad con cuatro decisiones:

```json
{
  "entity": "example:animal",
  "geometry": {
    "engine": "scalebrews:model_part",
    "model": "example:animal"
  },
  "pose": {
    "engine": "scalebrews:mojang_keyframes"
  },
  "root_transform": "scalebrews:entity",
  "policy": {
    "enabled": true
  }
}
```

Los datos seleccionan comportamiento registrado, no contienen un lenguaje procedural arbitrario. Pueden aportar filtros include/exclude, variants, channels y parámetros de engine, estados temporalmente no soportados, overrides de policy/friction/ratio y metadata de fuente/compatibilidad.

El catálogo candidato se valida completo antes de hacerse visible. Una revisión aceptada es inmutable. El servidor prepara un bundle una vez por revisión; los clientes verifican identidad e integridad antes de sustituir atómicamente su snapshot.

`WorldAnatomyCatalog.Snapshot` conserva todos los bindings ejecutables de la
revisión en `PreparedBindingIndex`. La selección por mapa de variant delega al
único selector `CollisionBindingCatalog`: una fila seleccionada sin ejecución
preparada devuelve ausencia aunque exista un default ejecutable. `bindings()`
sigue siendo una proyección de compatibilidad sólo para el selector vacío;
no expresa el conjunto completo de variants.

Cada binding preparado tiene una identidad de contenido completa, versionada y
separada por dominio, derivada del codec canónico con orden determinista. Incluye
variant, engines/modelo, parámetros, channels, filtros, root, policy y estados
excluidos. Su construcción y validación de texto ocurren sólo en preparación;
las consultas recuperan objetos ya preparados sin repetir hash/codec/compilación.
El receptor reconstruye el índice desde los mismos bindings y recursos del
bundle, sin una segunda tabla autoritativa enviada por separado. Esta identidad
no certifica los bytes del modelo ni sustituye epoch/revisión/provenance o las
generaciones de binding/tracking. La disponibilidad del índice no implica que
el runtime haya seleccionado una variant del estado vivo de la entidad.

El frame causal conserva además esa identidad completa de contenido, desde el
descriptor instalado hasta su publicación. `anatomy_pose_v7` exige el token;
las sobrecargas locales históricas sin token no son codificables como autoridad
wire. Catálogo mantiene su versión independiente. El cliente resuelve el miembro
preparado por token, verifica su tuple modelo/pose/root y, al existir la entidad,
su tipo. Física, filtros, policy, evaluador y presentación usan esa misma fila,
incluso en un catálogo sólo de variantes. History y tombstone consideran el token
parte de la identidad estable: tracking nuevo por sí solo no permite cambiarlo.
La procedencia de traits vivos se verifica separadamente del transporte preparado.

S48 aporta `VariantSource` server-safe y `CollisionVariantSources`:
cada fuente declara una clave y dominio finito; la revisión captura sus lectores
al preparar la unión de ejes de todas las filas del tipo. `age` lee `isBaby()` y
los datos eligen adult/baby. Fuente ausente/fallida/fuera de dominio deja ausencia;
no equivale a inputs vacíos conocidos ni permite sustituirlos por default.
`PreparedVariantBindings` reusa arrays y selección mientras los inputs sean
iguales; sólo sus cambios invocan el selector canónico. El estado live se admite
por carga y se indexa por participante/nivel, incluido el participante ausente
recuperable. No se enumeran mundos en steady-state ni se resuelve en cliente.

Los cambios sincronizados notifican mediante el hook genérico de entity data;
otras fuentes usan `AnatomyApi.variantChanged` a través del backend neutral.
Refresh local antes/después de captura y antes del tick/consumo material retira
la registración anterior. La retirada congela un último UNAVAILABLE sin evaluar
el provider, conserva su token/generación para destinatarios autorizados y libera
contactos, cola local, espacio y slot. Un registro manual conserva ownership.
Rebind conserva el clock del participante, pero abre generación física nueva:
no adelanta joints desde queries ni produce un intervalo entre filas. Esta
frontera está TRUSTED en el scope independiente S48 de fuentes finitas capturadas,
retirada/recuperación local, clock y transporte exacto. No certifica selectores
originales generales, familias FULL ni el benchmark global de G9.

La familia aditiva S49 `CollisionVariantSources.enumComponent` prepara los nombres
serializados de un componente enum `StringRepresentable`; la lectura server-safe
obtiene el valor original, verifica su enum propietario y retorna el nombre
preparado por ordinal. Ausencia/tipo ajeno permanece unknown. La inicialización
descubre campos enum tipados de `DataComponents`, sin dispatch físico por especie;
la reflexión sólo ocurre en preparación y la revisión conserva sus readers.
`componentKey` expresa el ID original mediante `component.<namespace>.<path>`
con `_`→`_5f`, `.`→`_2e` y `/`→`_2f` dentro de segmentos, respetando el límite64.
Así `minecraft:fox/variant` usa `component.minecraft.fox_2fvariant` sin alias con
escapes literales. Mods pueden preparar/registrar sources de esta misma familia
por API. Holder/skin/dispatch original de modelos son fronteras diferentes;
Los readers están TRUSTED en el scope independiente S49 de componentes enum,
dominios preparados y lifecycle existente; no promueve FULL ni certifica modelos.

La extensión S50 `holderModelComponent` proyecta un único campo tipado
`ModelAndTexture<E>` de un record público contenido en `Holder`, siendo E un enum
serializable finito. Schema y accessor se preparan una vez; la lectura local exige
Holder bound, record propietario y enum exacto. El accessor enlazado usa
`invokeExact`, sin introspección ni invocación varargs durante queries. La key
`modelComponentKey` añade `.model` al ID codificado y conserva el límite64.
Textura e identidad de registry no forman este eje: igual modelo mantiene la
selección, distinto modelo usa retirada/rebind S48. Discovery reconoce esta
tecnología desde firmas de componentes sin lista de especies. No observa todos
los Holder ni skins. Los readers están TRUSTED en el scope independiente S50
de schema/enum/accesor y lifecycle: cuatro proyecciones nativas reconocidas,
13 Holder fuera de esta tecnología. No certifica geometría/poses originales ni FULL.

S51 `EnumAgeModelPreparation` está TRUSTED para PREPARATION cliente explícita de
una familia original enum/AdultAndBabyModelPair. Lee programas completos y
resources originales acotados, verifica la cadena componente/getter -> estado
variant/age -> submit real y sus bridges, y produce metadatos de filas inmutables
con slots/digests/fingerprints iniciales. Ausencia de un tramo no admite filas.
No recibe ni infiere EntityType; el caller/discovery debe certificar asociación.
Actor/renderer deben proceder del artefacto original y la clase actor es exacta;
bytecode transformado/compat no probado queda fuera del scope. No instala un
resolver físico alternativo ni llama desde un hook hot. Campaña independiente y
replay original verifican esta frontera; elección/estructura no certifican pose, root, geometría
física, catálogo FULL ni cobertura G6. Evidencia ejecutada y estado en VALIDATION.

S52 `OriginalAgeStateModelPreparation` prepara explícitamente la relación
`LivingEntityRenderState.isBaby` → slots adult/baby del selector heredado
AgeableMobRenderer. Verifica entrypoints reales, bridge/selector/constructor
completos y slots originales; dos filas inmutables describen el predicado del
estado y la estructura inicial. `extractionAuthority=UNPROVEN`: no define source
age, actor ni binding físico, ni presume cómo llega la edad de la entidad al
estado. Sólo PREPARATION sin callers hot; TRUSTED scoped tras campaña
independiente y replay. G6, poses/root/capas/material físico/FULL permanecen abiertos.

S53 `EnumModelPreparation` prepara otra tecnologia original: componente Holder
con record/model enum hacia Map[enum] con un EntityModel unico. No publica un eje
de edad. `OriginalEnumSelectorProof` comparte con S51 las pruebas completas de
record/accessor, getter/componente/extractor, submit y cuatro bridges, pero exige
familia explicita ENUM_ONLY/ENUM_AGE. La API y recetas S51 se conservan. La captura
incidental de body armor se permite solo en ENUM_ONLY tras probar getter Mob
completo y despacho original getItemBySlot sin overrides. Una fila por key con
slot/fingerprint inicial y resource hashes, metadata inmutable y cache pass-local;
ningun caller hot ni binding ejecutable. Actor exacto/origen original sigue siendo
requisito; asociacion EntityType/renderer, pose/root/layers/material/FULL fuera de
scope. TRUSTED solo en esta PREPARATION tras recertificacion independiente y replay;
evidencia ejecutada en VALIDATION. G6 sigue abierto.

S54 `OriginalResetPosePreparation` certifica un programa local original que
restaura nueve transforms iniciales: setupAnim(Object) efectivo de Model,
resetPose final y loop exacto sobre allParts, ModelPart final/loadPose y getters
de PartPose. Comprueba que el cache y el arbol observado tienen las mismas
identidades; emite paths estructurales framed y transforms iniciales inmutables,
fingerprint y recursos originales exactos. Solo PREPARATION, sin ejecutar reset
sobre el modelo compartido ni retenerlo. La receta no certifica visible/skipDraw,
transformacion del renderer, layers/material, animacion externa, binding, engine
ejecutable ni pose fisica completa. La autoridad numerica queda limitada a los
programas originales auditados; no se presume equivalencia del bytecode
transformado. TRUSTED solo en este scope tras campana independiente y replay,
sensibilidad y revisiones finales. Estado global y ejecuciones en PLAN/VALIDATION.

`OriginalResetGeometryPreparation` conecta la receta S54 con una copia inicial
del mismo arbol exportada como ModelGeometry. Usa el snapshot package-private
de ModelPartStructure, no un segundo algoritmo de copia; exige fingerprint
exacto y bijeccion framed-path/part ID con parents y nueve SourcePose iniciales.
El DTO format2 declara identidad como coordenadas locales. Solo el DTO cruza
al consumidor common; no retiene modelos/parts/cubos/world ni consulta flags
actuales para eliminar cubos estructurales. scalebrews:static puede evaluar
este rest para ordinary/parametros vacios con O(1);
la composicion O(P) permanece en ModelGeometry. Dos copias/extractions bounded
en PREPARATION (S54 y export), sin caller hot nuevo. No certifica renderer/world
root, visibilidad, material, selector de actor, bytecode transformado ni FULL.
La extension queda TRUSTED solo en este scope tras la aceptacion independiente
S55, replay/sensibilidad/recibos y revisiones completas sin cambios.

S56 `OriginalLinearPosePreparation` compila programas originales
setupAnim completos de flujo lineal: bridge efectivo, super/reset y operaciones
float/campos por identidad hacia un DAG numerico common. `NumericPoseProgram`
conserva nueve campos iniciales y orden/read-after-write, rounding float y F2D
para Mth.sin/cos originales; el binding explicito exige geometria exacta y la
evaluacion produce ausencia total ante missing/nonfinite/degenerate/incompatible.
El schema owner/name/F describe inputs de estado explicitos, no una fuente de
autoridad desde actor. Reflection/parser/hash solo PREPARATION bounded; no nuevo
caller normal, registro de engine, recurso de catalogo ni binding fisico. Este
compiler/evaluador y el seam reset para overrides quedan TRUSTED solo en este
scope tras campana independiente, replay publico y ocho kills semanticos S56.
Renderer/world root, flags/material, selectors,
bytecode transformado, FULL y G6 general permanecen abiertos.

S57 registra `scalebrews:numeric_program` como engine reusable y
resuelve `PoseEngine.Resources.numericProgram` solo al aceptar/bind una revision.
El directorio `scalebrews/numeric_pose_programs` y el mapa de bundle
`numeric_pose_programs` contienen DTO neutrales deep-validados; geometry exacta
y todos los INPUT declarados son obligatorios. Reload/transfer conservan snapshot
y prepared bundle anterior ante un candidato invalido. Protocolo7 y capability
aditiva `NUMERIC_POSE_PROGRAMS` versionan esta extension; datos binding schema1
y overloads/SAM anteriores conservan su contrato de fuente. Bound retiene nodos,
indices y fields iniciales, sin geometria/cubos duplicados por binding. Parsing,
copias/hash/fragments siguen en PREPARATION y packets se reutilizan por revision/
epoch; evaluate conserva scratch por llamada y no vuelve a buscar resources.
Extension TRUSTED solo para este scope tras campana independiente S57: codec
real,140 matrices originales, rollback/ownership y ocho kills semanticos.
No crea autoridad
de actor/renderer/root/material, bindings de especies ni familias FULL.

S58 cerrado scoped amplia `OriginalLinearPosePreparation` a DUP categoria1 para
actualizaciones compuestas: el simbolo duplicado conserva identidad y nodo,
sin recalcular receptor/INITIAL/INPUT. Categoria2, underflow y overflow no se
admiten; stack32 y programa completo siguen obligatorios. Tool provenance
`original-linear-pose-v2`, DTO format1 y protocol7. Solo PREPARATION, no engine
hot nuevo, branches, actor input source, capas/root/material/bindings ni FULL.
La ampliacion es TRUSTED scoped tras campana independiente y replay publico:
140 matrices/120 cubos de originales con aliases,36 asserts y seis kills
semanticos, veto cliente y snapshots locales corregidos. Bound/evaluate common
conservan sus bytes; no certifica inputs del actor ni una familia FULL.

S59 cerrado scoped: OriginalFinitePosePreparation especializa decisiones
originales Z forward en tablas completas de NumericPoseProgram existentes.
PREPARATION bounded5booleans/32filas/63discovery y1048576 instrucciones totales;
work se carga antes de verificar/admitir siguiente metodo. Una geometria/resource
pool compartida y programs equivalentes internados; typed branches y coverage de
todas las instrucciones. API lineal v2 y common conservan fronteras estrictas.
TRUSTED solo este scope tras campana independiente:280matrices/80cubos,15asserts,
seis kills nuevos, common clientVeto y snapshots de bases para planos no materiales.
RED1 de admission order reparado con oracle intacto. No evaluator hot nuevo ni
fuente actor/root/flags/layers/material/selector fisico/bindings/familiaFULL.

S60 cerrado adversarialmente scoped: FinitePoseProgram common
referencia filas numeric revision-local y FinitePoseEngine reservado selecciona
por channels booleanos explicitos +0/1 exactos. Bind valida toda la familia y
solo retiene nombres/Bounds numericos, sin lookup de recursos en evaluacion.
OriginalFiniteResourcePreparation unifica owner/name/F y remapea INPUT labels;
no modifica operaciones originales. Catalogo/bundle atomicos protocol8 con
finite_pose_programs obligatorio y capability FINITE_POSE_PROGRAMS; schema1
conservado y peers7 rechazados. TRUSTED solo esta seleccion explicita tras
280matrices originales/common, codec/rollback y seis kills semanticos;
actor input source/continuidad/root/flags/material/bindings/FULL abiertos.

S61 cerrado adversarialmente scoped: LivingPoseInputSource common
proyecta13fuentes originales Living a partialTicks1 para actor Class exacto y
schema64canales. Bind frio revalida datos y conserva solo Class/arrays; sample
read-only memoiza fuentes/derivados y publica un mapa completo inmutable o
ninguna muestra. OriginalLivingInputPreparation cold prueba schema por field
real, extractor/bridges y nameTags virtual originales de tecnologias enum/edad
y enum/unico, registra resources/getters y genera receta reproducible. No
registra adapter ni modifica tracker/catalog/wire. El clock supplied es
entity.walkAnimation: su evolucion server-side sigue UNPROVEN (vanilla actualiza
calculateEntityAnimation client-side; el producto modifica gait time scale).
TRUSTED solo proyeccion/read-only original partial1 tras36muestras nativas/
504bits/168matrices, common clientVeto/alias counters y7kills semanticos.
No certifica root, inclusion, continuidad, transformed bytecode ni FULL.

S62 cierra scoped el owner privado LivingLocomotionClock del State existente
por actor. Usa WalkAnimationState common original con ScaleGait vigente y delta
propio tangencial, restando transporte aplicado; entity.walkAnimation permanece
intacto. Frame privado revocable por actor/Level/tick/generation/current owner,
con confinement de servidor; lecturas no evolucionan. Source explicita valida
admision y commit despues de todos los getters; tracker prepara inputs staged,
valida al commit y oculta estado publico durante sampling/reentrancia. Fallos
RuntimeException/Error del tracker retiran inputs/frame antes de propagarse.
TRUSTED solo ley base Living y esta proyeccion explicita tras diferencial nativo
Cow/Pig adulto/baby, escalas/gravedades/barriers y dos sensibilidades nuevas.
El caller trusted suministra authority tick: no prueba scheduling universal,
overrides ajenos ni omnidirectionalAirMover. No registro automatico, nuevas
familias FULL, root/material o integracion G6 completa. El overload supplied S61
sigue read-only y no certifica evolucion server-side de entity.walkAnimation.
HOT_TICK O(1)+historia ledger<=64; sample O(C<=64) con allocations S61. Ledger
mantiene monitor static global: commit tracker agrega una generation query y
Source dos por muestra valida, nunca por canal. Sin nuevo monitor ni claim TPS.

### 4.3 GeometryEngine

Un `GeometryEngine` conoce una **tecnología de modelo**, no una especie. Produce `ModelGeometry`, formado por parts jerárquicas y pieces convexas identificadas.

Familias previstas:

- `ModelPart`: Minecraft y mods que usan el pipeline estándar;
- `AdvancedModelBox`: familia Citadel/Alex-style mediante integración reusable;
- `DisplayRig`: composición semántica de entidades display alrededor de una raíz;
- extensiones futuras como GeckoLib cuando exista un target real.

La extracción que necesita clases cliente ocurre en tooling/preparación. El runtime de servidor no recorre renderers ni reflecta modelos por query.

El tooling cliente `ModelFamilyCensus`, invocado explícitamente, consume el
censo y clasificador canónicos `CollisionCoverageDiscovery/Scanner`. Inventaría
slots de modelos en renderers, layers y contenedores estructurales acotados,
incluidos los dispatch de avatar, y agrupa candidatos por tecnología/clase/familia
de animación original. `ModelPartStructure` identifica la estructura inicial en
una copia independiente sin resetear el modelo compartido. Los slots, grupos y
fingerprints no prueban selectors ejecutables, pose ni root transform; tampoco
conceden FULL ni publican un collider. Sus fallos/límites quedan visibles en cada
fila y ninguna ruta normal de física, tick, red o render invoca este censo.

### 4.4 PoseEngine y channels

`PoseEngine` convierte inputs autoritativos en transforms locales. La unidad reusable es el motor de animación, no el mob.

El vocabulario base contiene locomoción, orientación de cabeza, edad/tick y flags físicos comunes. Engines añaden channels tipados cuando los necesitan. Para keyframes o Citadel, el programa exportado debe poder ejecutarse en un runtime server-safe.

Familias previstas incluyen poses procedurales vanilla, `AnimationDefinition` de Mojang, adapters reutilizables de frameworks externos y pose programs compilados. Una primitiva, channel o estado desconocido no se improvisa: el endpoint queda `UNAVAILABLE`.

Una fórmula específica de una especie puede existir como fixture de aceptación para comparar contra el modelo original, pero no se registra como engine de producción cuando el objetivo es soportar una familia reusable.

### 4.5 RootTransformProvider

El root contiene posición, orientación y escala del soporte y se versiona causalmente por separado del sample de joints. Un root-only update no obliga a reevaluar joints idénticos.

La orientación externa de Stormie's Spiders es un ejemplo transversal: no cambia geometry/pose engine, sólo la composición global. La gravedad del cuerpo soportado es un estado independiente de la orientación raíz del soporte.

### 4.6 Solver físico

El solver consume convexos e intervalos materiales. Sus responsabilidades son:

1. broadphase espacial sobre bounds materiales;
2. narrowphase y sweep continuo;
3. sliding y mantenimiento de contacto tangencial;
4. separación/recovery localizado;
5. selección determinista de multicontacto;
6. creación/actualización de `SurfaceContact` y anchor;
7. aplicación exactamente una vez de contribuciones materiales;
8. carry de raíces/cadenas con colisión vanilla alrededor;
9. liberación o cuarentena cuando una pareja deja de ser resoluble.

La relación moving-platform **no convierte el AABB global del soporte en una pared**. La anatomía sustituye únicamente el bloqueo sólido global de la pareja cuando ésta es elegible como soporte-cuerpo. Fuera de esa elegibilidad no existe narrowphase anatómico entre ambos. Incluso dentro de una relación soporte-cuerpo válida, el **push vanilla** producido por la entidad soporte sobre el cuerpo se conserva exactamente una vez. Si la integración necesita evitar que el AABB global duplique el bloqueo anatómico, debe separar esa supresión del impulso/push vanilla, no borrar ambos como si fueran la misma cosa.

El solver no conoce Alex's Mobs, Fresh Animations, Clinging ni JSON concreto.

### 4.7 Contacto y causalidad

Un contacto identifica materialmente soporte, pieza, cara y punto local. La identidad de conexión, catálogo, binding, entidad y stream debe permitir decidir si un dato atrasado sigue perteneciendo a la misma realidad física.

No se fusionan estos conceptos:

- **joint sample**: evaluación de pose;
- **root frame**: mutación de transform global;
- **material/publication sequence**: orden de cambios consumibles.

Un tick puede contener varias contribuciones reales. «Exactamente una vez» se aplica a cada contribución, no a un tick agregado a ciegas.

### 4.8 Runtime y lifecycle

El servidor posee catálogo aceptado, bindings activos, trackers/pose cache, generaciones de tracking, publicación de endpoints/contactos e invalidación por unload, rebind, reload, teleport y dimensión.

El cliente posee sólo material recibido para la conexión/nivel actual y prediction de entidades localmente autoritativas. Una barrera de lifecycle invalida histories/material previos antes de aceptar otra identidad.

Las transiciones JOIN/DISCONNECT se ejecutan en el executor del cliente y
validan allí la identidad del listener que posee la sesión. Un callback
atrasado de una conexión retirada no puede limpiar el catálogo/material de
la conexión nueva; la desconexión vigente sí conserva su barrera completa.
La identidad se comprueba al ejecutar la transición, no sólo al encolarla.

### 4.9 Red, prediction y reconciliación

El servidor origina catálogo, frames y contactos. El cliente no aporta geometría ni pose. Para player/controlled vehicle se permite prediction del mismo core. La reconciliación correlaciona movimiento vanilla con transportes confirmados; un receipt acredita un delta aplicado y nunca lo vuelve a aplicar.

Un observador remoto no ejecuta carry físico local de la entidad observada. Usa posición vanilla y presentación derivada del material confirmado.
Cuando el servidor acredita un carry remoto, la acreditación pertenece al envío
real de un paquete de posición vanilla a un destinatario concreto. La identidad
de esa ocurrencia y el intervalo material Q2 confirmado deben concordar con el
contacto recibido. El target del paquete relativo se reconstruye desde la base
del códec **decodificada por ese destinatario**: la base física exacta de
`ServerEntity` puede diferir tras la cuantización de un envío anterior. Sin
paquete de posición, intervalo aceptado o continuidad nativa demostrable, el
residual remoto es cero. Esta acreditación sólo gobierna dibujo; nunca mueve la
entidad observada ni altera su AABB, velocidad o gravedad.

### 4.10 Presentación y cámara

La presentación puede corregir el residual entre posición física y superficie animada, pero no es otra fuente de pose física. Usa el mismo endpoint/intervalo causal confirmado.

La cámara aplica sólo offset visual acotado y validado contra bloques/near plane. Teleports resetean el residual; pérdidas ordinarias pueden suavizarlo. Integraciones visuales consumen esta capa y no alteran contacto.

### 4.11 Interacción física entre entidades y precedencia de moving platforms

La decisión soporte-cuerpo es **relacional y direccional**. Para cada posible cuerpo y soporte se evalúan policy, ratio físico efectivo y estado. El ratio de FR-009 es la frontera por defecto y puede ser sustituido por la policy canónica.

Si la entidad objetivo es demasiado pequeña para servir como soporte material al actor, o la policy/estado la excluyen, **no existe pared anatómica** para esa pareja. El actor la atraviesa/empieza a desplazar exactamente como lo permitiría la física vanilla entre entidades; sólo permanecen sus colisiones y pushes vanilla normales.

Un cambio de dimensiones físicas revalida el contacto retenido por ese cuerpo
y los dependientes de ese soporte, y retira las relaciones cuyo ratio/estado
actual ya no permite contacto. `AnatomyContactState` conserva la única verdad
de contacto; su índice derivado de miembros usa identidad débil de ambos extremos
y se mantiene al reemplazar, limpiar o retirar el nivel. La consulta de dependientes
es local al soporte, sin recorrer contactos ajenos. Esta validación no mueve al
cuerpo ni prepara geometría. La ruta de carry comprueba
elegibilidad y gravedad antes de delegar el movimiento al runtime material del
servidor: delegar transporte no permite conservar un contacto inválido.

El retiro de recibos consulta sólo los destinatarios históricos de ese cuerpo,
incluidos pasajeros que ya desmontaron. Los historiales autoritativos siguen en
`AnatomyTransportReceipts`; el índice inverso usa UUID por valor para el cuerpo
e identidad débil para los destinatarios. Append, expiración, disconnect y clear
mantienen esa pertenencia. El bucket inverso usa valor débil y cada historial
autoritativo lo conserva mientras exista, sin retener entidades ni acumular UUIDs
retirados. Limpiar un contacto sin recibos no recorre historiales de otros jugadores.

Si la entidad sí califica como moving platform para el actor, Scale añade la geometría material necesaria para apoyar, caminar, deslizar, chocar con partes y recibir carry. Eso no concede ownership del push vanilla: la entidad soporte sigue empujando al actor como lo haría sin el subsistema. La integración debe garantizar que el push ocurra una vez aunque el bloqueo AABB global sea sustituido por anatomía. El mismo principio evita dos errores opuestos: una vaca pequeña convertida en muro por tener `ModelGeometry`, y una vaca grande convertida en suelo inerte que deja de empujar al jugador.

### 4.12 Clinging Reoriented: landing surfaces y Reorientation

Clinging Reoriented ya expone en su `main` un SPI público de geometría de aterrizaje: `LandingSurfaceProvider`, registrado mediante `LandingSurfaces`. Su provider vanilla sólo publica superficies derivadas de colisión de bloques. La predicción de aterrizaje y Reorientation consultan ese registro para distinguir un suelo válido de una pared/intersección y para revalidar el contacto durante la ventana de aterrizaje.

La integración correcta no consiste en que Clinging vuelva a descubrir la anatomía por AABB ni en duplicar el solver. Scale registra un **provider/adaptador de landing surfaces** que traduce su verdad material autoritativa a las tres operaciones geométricas que Clinging necesita:

- `currentSupport`: contacto material actual que realmente puede sostener al cuerpo bajo su gravedad;
- `sweep`: primer contacto geométrico certificado del segmento, indicando si esa cara es soporte válido;
- `revalidate`: comprobación de que la identidad material/revisión sigue vigente.

La identidad del contacto debe incluir suficiente material causal para no reusar una pieza tras reload/rebind/revisión. Scale sigue poseyendo geometría, contacto, sweep físico y carry. Clinging sigue poseyendo la **política de Reorientation**: predicción temporal de aterrizaje, decisión de giro, reposicionamiento del personaje, consumo de efectos/cargas, Gravity Changer y presentación/cámara. El provider no cambia gravedad ni mueve al jugador; sólo declara «esta superficie material existe y es un landing válido ahora».

Una entidad que no es moving platform para ese cuerpo tampoco se publica como landing surface Scale para Reorientation. Así, tocar o empujar una entidad pequeña no puede hacer que el personaje gire como si hubiera aterrizado sobre suelo.

## 5. API frente a JSON

La regla es:

> **comportamiento reusable = API/engine; selección y metadata = datos**.

Se añade Java cuando aparece una nueva familia de comportamiento. Se añade JSON/generated data cuando una entidad sólo necesita seleccionar engines, mapear channels, filtrar parts o declarar variants/estados.

No se introduce una clase `TigerPose`, `OrcaPose`, etc. si ambas pueden interpretarse mediante un engine común. Si el JSON empieza a convertirse en un lenguaje procedural general, ese comportamiento pasa a un engine o pose program compilado.

## 6. Compatibilidad general y VanillaPlus

El repositorio público de Scale Brews contiene core y engines reutilizables. No fija el modpack privado ni IDs específicos de VanillaPlus.

El proyecto privado de compatibilidad puede contener:

```text
bindings/
overrides/
generated-catalog/
compat adapters realmente específicos/
coverage manifest + hashes/
```

Ese proyecto fija versiones concretas de mods/resource packs y exige su gate de cobertura. Un adapter útil para cualquier usuario de la misma tecnología vuelve al core o a un módulo reusable público.

La cobertura no se declara mediante una lista manual de especies. El scanner enumera los tipos del target y obliga a clasificar cada uno. Los composites que no son un `EntityType` propio requieren un registro semántico adicional.

## 7. EMF y resource packs

La física no confía en geometría enviada por un cliente. Para un pack administrado que usa CEM/EMF, la integración correcta es preparación con el stack visual exacto:

```text
mods + resource packs fijados
        ↓
cliente/harness aislado
        ↓
export de geometría + pose program
        ↓
validación + hashes
        ↓
catálogo server-owned
```

Una configuración visual arbitraria no registrada no redefine la física del servidor. Según policy del target, se usa anatomía canónica o se diagnostica incompatibilidad visual.

## 8. Migración desde Living Platforms

El motor antiguo usaba `PlatformDefinition.Surface`, `PlatformGeometry`, `PlatformState`, `PlatformPhysics`, networking y corrección visual propios. Ese diseño no sobrevive como segundo motor al cierre de migración.

Pueden sobrevivir, trasladados a la capa correcta: policy de categorías/ratio/fricción, placement semantics, comportamiento especial de boats/items/falling blocks, tests de regresión y un decoder que traduzca una superficie legacy a plano unilateral explícito.

No sobreviven `automatic_top`, la captura por altura/minY, un carry paralelo ni networking/reconciliation paralelos.

La integración nativa usa `MovingBodyContext` para la identidad scoped del cuerpo
que consulta colisiones; no almacena geometría ni contactos. `NativeMovementBaselines`
actualiza únicamente las coordenadas player/vehicle que Minecraft ya posee tras
transport aplicado. Cámara y renderer componen sólo residual causal y presentación
Tiny Mount de `main`, mediante `BodyCameraOffset`/`BodyRenderOffset`, sin sampling
de modelos como fuente física. Los nombres `Platform*` retenidos para datos/policy,
placement, categorías y lifecycle delegan al owner canónico. El decoder de planos
legacy conserva datos unilaterales explícitos con warning; no crea binding
ejecutable, collider, placement ni carry cuando falta anatomía preparada.

## 9. Organización de paquetes

Los paquetes representan responsabilidades, no la cronología del prototipo.

La frontera estructural mínima durante la migración es:

```text
io.github.r3neer.scalebrews.collision
├── api            // API y DTOs públicos mínimos
├── geometry       // ModelGeometry, ConvexBox y datos geométricos puros
├── pose           // providers/engines y channels reutilizables
├── physics        // kernel CCD, separation, response y trayectorias materiales
└── internal       // orquestación todavía acoplada; nunca API pública

io.github.r3neer.scalebrews.client.collision
├── preparation    // extractors/exporters que pueden usar clases cliente
└── network        // recepción/cache de material autoritativo
```

`collision.internal` es deliberadamente transitorio. Aloja piezas de catálogo, runtime, red, lifecycle y movimiento que todavía comparten contratos package-private o dependencias cíclicas reales. No se crean paquetes nominalmente “puros” si eso sólo desplaza el mismo acoplamiento mediante imports cruzados. Al cerrar las fronteras de G1/G2, `internal` se descompone hasta el objetivo:

```text
io.github.r3neer.scalebrews.collision
├── api
├── catalog
├── geometry
├── pose
├── physics
├── network
├── runtime
└── integration

io.github.r3neer.scalebrews.client.collision
├── preparation
├── network
└── presentation
```

La presentación/cámara que todavía pertenece al motor Living Platforms no se renombra para fingir que ya implementa la arquitectura nueva; se sustituye en la migración correspondiente.

Los mixins permanecen en el paquete de mixins, pero delegan inmediatamente en una única clase de integración del subsistema; no alojan una segunda implementación.
