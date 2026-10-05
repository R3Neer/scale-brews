# S48 — Selección autoritativa de variantes y retirada local de bindings

Estado: CERRADO localmente para selección/retirada/recuperación S48; no FULL ni cierre global G6/G9. Commit propio identificado por Sprint:S48.

## 0. Roles

IMPLEMENTER: agente principal, producción y probes de desarrollo.
ADVERSARY: agente existente independiente, modelo previo, holdouts, fixtures,
mutaciones, snapshots, rendimiento y evidencia; no modifica producción.
Comunicación operativa; los casos privados no dirigen la implementación.

## 1. Scope y dependencias

Tesis: el runtime selecciona una fila ejecutable desde traits del servidor;
un cambio que selecciona otra fila o produce ausencia retira inmediatamente el
material anterior y admite después una nueva generación física. Una fila no
puede conservar geometría/contacto de la anterior, ni un retrack lavar su replay.
Contribuye a G6 tarea2 sin certificar aún familias/poses originales ni FULL.

FR-004,024,026,033,080–082; NFR-004,007–010,014–018,025–027,033.
S00/G1–G5 están cerrados; S46 prepara todas las filas con selector canónico y
S47 transporta su identidad exacta. Esos ámbitos son TRUSTED. Las extensiones
nuevas de selección/retirada son UNPROVEN hasta recertificación independiente.

HEAD18070f2309fa20af6e737c2dc6fa03145c087382, árbol limpio. Fetch actual confirma
origin/main b5e6253584b2233e5dd0039203c8a251bf10cdeb ya integrado. Se preservan
mainline y policy/interacción de participantes ajenos a las transiciones.
No se implementan skins/component-model selectors generales, nuevas familias de
pose, FULL Minecraft, consumer Clinging, VP26, benchmark global ni QA humana.
No nuevos esquemas de binding ni protocolos de catálogo/pose. No release/push.

## 2. Investigación

AnatomyRuntime sólo registra snapshot.bindings().get(type), proyección default.
ENTITY_LOAD y enumeración inicial son los puntos de admisión; prepare sólo
retira activos muertos. No hay registro de fuentes autoritativas de variant ni
participantes pendientes capaces de recuperar una fila desconocida→conocida.
AnatomyMovement.invalidateSupport limpia contactos/espacio pero deja provider y
binding: por sí solo no retira la geometría en queries posteriores.

El resolver CollisionBindingCatalog tiene semántica SUBSET most-specific;
una ambigüedad falla cerrada. Esa semántica no cambia. Un default es una selección
explícita aplicable a inputs conocidos, no autorización para sustituir inputs
indisponibles. S47 resuelve por token exacto en cliente y no deriva traits allí.

JAR Minecraft26.2 verificado por javap: SynchedEntityData.entity es un
SyncedDataHolder; set(accessor,Object,boolean) instala el dato y llama el callback
antes de retornar. El overload corto delega en él. AgeableMob.setAge actualiza
DATA_BABY_ID al cruzar la frontera y setBaby delega en setAge. Un hook genérico
tras el set y refresh local antes de queries permiten usar isBaby sin una clase
por mob. No se inspecciona renderer desde servidor ni se replica una pose cliente.

## 3. Plan — IMPLEMENTER

- [x] I1 Fuente reusable de variant en collision.api.spi y registro init-time
  collision.api: clave compatible con variant JSON, dominio finito conocido,
  reader local puro de LivingEntity. Cada tecnología/familia aporta un reader;
  los mobs seleccionan ejes/valores por datos. Built-in age usa isBaby y valores
  adult/baby, sin lista de especies. Registro rechaza duplicados/texto/budgets;
  snapshot inmutable se toma sólo al preparar revisión. Valores ausentes, fuera
  de dominio, malformados o fallo del reader representan ausencia, nunca map vacío.
  Fuentes externas notifican cambios no sincronizados mediante hook explícito;
  no pueden leer renderers ni decidir geometría/policy ni resolver bindings.
- [x] I2 Plan por tipo preparado desde TODAS las filas de catálogo, incluso
  catalog-only/disabled, con unión de ejes ordenada <=32 y referencias a fuentes.
  Eje inexistente o presupuesto excedido deja ese tipo indisponible, sin fallback
  al default. Registro de fuentes no cambia una revisión live sin reload.
  Tipo sin variant reusa selección vacía canónica preparada; no paga reads por tick.
  Resultado de resolución sigue siendo únicamente el índice/resolver S46.
- [x] I3 Estado por participante loaded y nivel, incluido selected=null recuperable:
  admisión por ENTITY_LOAD/prepareExisting, retirada por ENTITY_UNLOAD/death/stop/
  reset/dimensión. Ningún scan del mundo steady-state ni de otros niveles.
  Cada participante guarda inputs previos y scratch acotados; reads locales sin
  maps nuevos mientras no cambien. Map inmutable sólo al cambiar inputs; se llama
  resolver sólo en esa frontera. Inputs que cambian pero seleccionan exactamente
  la misma fila no fuerzan nueva generación. Unsupported→known sí puede recuperar.
- [x] I4 Refresh server-only antes de pose/carry por nivel y antes de capturar un
  soporte local; confirmar selección también tras callbacks de evaluación para
  no publicar un frame si su trait cambió durante la captura. Hook genérico
  SynchedEntityData.set(...,boolean) TAIL notifica sólo el LivingEntity servidor
  participante. Hook API para otras fuentes. Evitar recursión mediante guard local:
  instalar estado Active antes de registrar, conservar guard de captura existente.
  Cliente conserva única autoridad de packet S47; no ejecuta selector vivo.
  La notificación pública pasa por AnatomyApi→AnatomyBackend neutral→backend
  interno, sin import inverso runtime→api ni registry→implementación.
  Un rebind conserva el clock AuthorityPoseTracker del participante entre providers,
  pero no avanza joints desde query/render: B empieza material disponible cuando
  el tick canónico acepte su pose/root; antes de eso hay ausencia, nunca A stale.
  Tracking start recibe el siguiente endpoint canónico actual sin replay previo.
- [x] I5 Transición por ownership exacto: congelar un último UNAVAILABLE del
  binding anterior con token/generación anteriores y serial superior, sin volver
  a evaluar su provider; enviar a sus observadores autorizados y self player.
  Limpiar contactos/dependientes, espacio, root/endpoint y provider/descriptor
  únicamente de esa registración. Slot retiene generación local y capture guard;
  no deactivar nivel ni tocar registraciones manuales ajenas. Si no existe endpoint
  previo no inventar material wire. B nueva instala token propio y generación
  server nueva; ausencia mantiene candidato pero ningún provider ejecutable.
- [x] I6 Retirar tracker/cola S06 de este soporte, incluidos queued handles viejos;
  checker de identidad refresca selección antes de aceptar trabajo. Los demás
  soportes/colas y su identidad quedan intactos. No unir intervalos entre A/B ni
  conservar carry/contactos/receipts procedentes de A. STOP/UNLOAD liberan refs;
  recuperación conserva fences cliente de S47 y abre generación física nueva.
- [x] I7 Probes IMPLEMENTER sobre age real, catálogo sólo de variantes, cambios
  same-tick, fuentes inexistentes/fallo/fuera de dominio y ausencia→recuperación;
  preservar default fixtures y modelo/API mainline. ADVERSARY desarrolla pruebas
  independientes después de producción, incluido cliente real y snapshots
  deterministas cuando la geometría/retirada pueda parecer correcta sólo por count.
- [x] I8 Revisión completa iterativa hasta cero cambios, build/server/client y
  prepared relevante, counters/localidad/fault sensitivity, evidencia durable,
  canon actualizado con lo ejecutado y commit local propio antes de avanzar.

## 4. Presupuestos y fronteras

PREPARATION: unión de claves y fuentes sólo una vez por revisión/tipo; catálogo
<=4096 filas, cada selector<=32ejes; plan live<=32ejes por tipo. Registro de fuentes
<=1024claves y dominio<=256valores por fuente, cada valor<=256caracteres Unicode
válidos. Presupuestos que no permitan preparar el tipo no fabrican geometría.

HOT_TICK: participantes locales S_l × ejes A<=32; sin escanear entidades del mundo
ni soportes ajenos al nivel. HOT_MOVE_QUERY: sólo este soporte O(A), reutilizando
resultado/arrays; el coste no crece con observadores o con filas mientras los
inputs permanecen iguales. Transición O(filas_tipo×ejes) por resolver canónico,
reutiliza binding/provider-programs preparados; no codec/SHA/compilación/reflection
ni registry lock en consulta. Nuevo ModelGeometryProvider sólo en cambio efectivo.
Reader age devuelve referencias constantes. La API obliga a reads acotados, no
sampling geométrico ni consultas globales en fuentes externas.

Retirada: reverse membership existente de contactos afectados; cola del nivel
acotada512; envío O(observadores del soporte). No scan de contactos de otro
soporte ni reset global. Estado permanece limitado a participantes cargados y se
libera en lifecycle; ninguna nueva cache de bindings/renderer fuera de esa autoridad.

## 5. Revisión del plan

P1: una tesis técnica selección→retirada→rebind, sin adelantar familias FULL.
P2: dominio explícito de fuentes y desconocido→ausencia, manteniendo SUBSET;
se conserva el candidato sin provider para recuperar y se fija ownership exacto
para no retirar fixtures/registraciones manuales. P3: se incluye mutación sincronizada
same-tick antes de que el broadphase pueda conservar el envelope anterior; se
confirma selección tras callbacks y se retiran queued intervals locales.
P4: se fija la frontera neutral del hook público y se conserva el clock de pose
entre providers; no se adelanta ese clock desde consultas para fabricar B.
Pasada completa IMPLEMENTER contra canon/dependencias/ownership/causalidad/
performance produce cero cambios adicionales. Modelo ADVERSARY convergido §6.

## 6. Modelo previo — ADVERSARY

Modelo conceptual construido desde requisitos, arquitectura, protocolo y workflow
vigentes, antes de leer este plan y sin usar pruebas históricas como fuente de
casos. Base S47: HEAD `18070f2309fa20af6e737c2dc6fa03145c087382`.
Todavía no hay producción S48 ni batería concreta; este dictamen es de plan.

A1 Autoridad y conocimiento. Sólo lectores registrados, reutilizables, server-safe
y confinados al mundo observan estado estructural. Observación vacía conocida no
es observación fallida/desconocida. Una fuente requerida ausente, inválida o fuera
de dominio no se sustituye por default. Resolver canónico conserva especificidad,
ambigüedad y ausencia de filas no ejecutables; pertenencia preparada no demuestra
la procedencia del estado observado.

A2 Una declaración. Fila aceptada, token completo, geometría/pose/root, parámetros,
filtros, canales, política y estados permanecen unidos hasta consumidor físico y
visual. Cliente/render no ejecutan otro selector. Seams manuales no sustituyen una
selección live aceptada por proyección default ni pierden su ownership propio.

A3 Retirada y recuperación. Cambio efectivo retira A localmente antes de permitir
otra query, carry o publicación de A; B abre generación física nueva y sólo existe
como material tras su endpoint canónico. No hay intervalo A→B ni devolución al
material histórico A tras A→B→A. Inputs distintos que resuelven la misma fila no
crean churn. Ausencia libera provider/material sin fabricar identidad o collider;
el participante sigue recuperable. Clock de pose y registro físico no son el
mismo owner: conservar el primero no autoriza reutilizar endpoints del segundo.

A4 Tiempo y reentrada. Tick polling solo no prueba la barrera: cambios posteriores
e intra-captura deben alcanzar los consumidores locales y rechazar capturas stale.
La combinación del hook síncrono, refresh previo y confirmación posterior tiene
que demostrarlo, también ante callbacks antes de completar el mutador. Rebind y
notificación no preparan modelos, mueven cuerpos ni adelantan joints. B ausente
hasta el tick canónico es resultado válido; seguir consumiendo A no lo es.

A5 Wire y ownership. La retirada debe llegar aunque ninguna sustitución sea
ejecutable: endpoint UNAVAILABLE congelado de A con token/generación antiguos y
serial superior, sin recapturar A. No inventar endpoint cuando nunca hubo material
publicable. Clientes no reviven poses, certificados, contactos, recibos o muestras
visuales anteriores tras aprender la barrera. Reload rechazado preserva snapshot
más bundle anteriores; unload/reset/dimensión limpian candidatos activos y ausentes,
dependientes y destinatarios históricos locales, sin borrar registros ajenos.

A6 Coste. PREPARATION posee planes de lectores e IDs. Estado estable reutiliza
objetos/arrays preparados; consultas locales no rehashéan, serializan, compilan,
recorren modelos ni dependen del número de observadores o filas. Resolución sólo
en cambio observado puede pagar la cota declarada de filas del tipo por ejes.
Retirada depende de miembros/destinatarios del soporte y cola acotada, nunca del
mundo ni de contactos ajenos. Fallo de lector no causa preparación/rebind/log spam
repetido. Memoria sigue participantes vivos y caps/TTL existentes, no transiciones
históricas; teardown debe soltar entidades y niveles.

A7 Recertificación. S46 membership preparado y S47 transporte exacto permanecen
TRUSTED sólo en su alcance. Lectores vivos, barreras temporales, retirada wire,
recuperación e integración con carry son UNPROVEN hasta pruebas independientes.
No se deriva FULL, cierre global G6 o rendimiento G9 de esta aceptación de plan.

Convergencia previa: I1–I8 y P1–P4 cubren estas obligaciones. P4 mantiene façade /
backend neutral y separa continuidad del clock de disponibilidad nueva. La pasada
completa ADVERSARY del plan no requiere cambios adicionales: **funciona, continúa**.
Esto autoriza probar una candidata, no certifica ejecución. Tras producción se
concretarán holdouts privados, sensibilidades, localidad y snapshots/cliente real;
las necesidades humanas y benchmark global permanecen al final de G9.

## 7. Implementación — IMPLEMENTER

Plan/modelo convergieron antes de producción (§6). Implementados I1–I6: lectores
capturados por revisión, cursor reusable, un participante compartido por índices
de entidad/nivel y ownership exacto. El clock del participante sobrevive rebind.
La captura confirma selección antes/después; una notificación durante el reader
impide instalar/publicar su resultado. Callback que cambia ownership manual no
se sobreescribe al salir del reader. El backend físico live revalida los handles
tras planificar y su metadata antes de aplicar cada cuerpo; sus fixtures kernel
no reclaman autoridad S06. Eso cuesta O(eventos×ejes) al confirmar el batch y
O(cuerpos×eventos) en comprobaciones metadata, con caps existentes, sin resolver
ni nuevos maps en estado estable. La retirada local usa el nivel de registro.

Revisión IMPLEMENTER corrigió durante desarrollo la policy prestada al seam
manual: un participante loaded toma su fila live; sólo fixtures no administrados
conservan la proyección default histórica. Diagnóstico deduplicado incluye UUID,
tipo, revisión y provider variant/eje. Compilación main/client/gametest PASS53s.
Cinco probes propios + sentinel Fabric PASS6/6 en35s: age real variant-only,
ausencia/dominio/recuperación/default conocido/same-row reuse, fuente ausente,
ownership manual y retirada congelada. Estos probes son de desarrollo; no
sustituyen holdouts/cliente/snapshots/mutaciones/regresión del adversario.

Primera regresión normal PASS491/491 en1m42s y cliente33 PASS3m18s; repetición
de probes sobre la revisión manual/diagnósticos PASS6/6 en41s. Los resultados
preceden la corrección F1 y deben repetirse afectados antes del cierre.

## 8. Verificación — ADVERSARY

Registro inicial (supersedido en alcance por el resultado final debajo). Lane privada con fuentes,
cache de proyecto, build y run separados en `../s48-adversary-private/`.

A-P1: `lifecycle-local-deps.log` PASS2/2 (oracle + sentinel), BUILD SUCCESSFUL1m20,
cierre limpio. Factory Cow original, edad nativa adulto→baby→adult, token/registro
nuevo, ausencia inmediata sin tick adelantado por query y soporte ajeno intacto.
Cursor:10000 lecturas estables conservan la instancia preparada con exactamente
10000 lecturas de un eje; desconocido/fuera de dominio no permiten default y el
reader vuelve a resolver al recuperar. No es todavía certificación del coste
completo ni de transporte/render. Primer intento con Gradle user-home nuevo fue
detenido al configurar dependencias; clasificado tooling, sin resultado físico.
La lane efectiva reutiliza dependencias locales con cache de proyecto propia.

RED1 PRODUCCIÓN/selección-policy: adulto live A, registro explícito raw-core de
provider manual con binding A, mutación nativa `cow.setBaby(true)`. El provider
manual permanece intacto (control positivo), pero `AnatomyRuntime.catalogBinding`
devuelve la fila adult anterior, aunque el participante cargado ahora es baby.
Contradice la policy live del participante descrita en §7; no exige recuperar ni
sobrescribir el provider manual. `manual-policy.log`:119; BUILD FAILED19s por
oracle. SHA256 AnatomyRuntime `a86d978e3c591296d512b06c7bd1db3fe3b2b1770f5399db2c275bbe35308de8`.
Reproducción: JDK25 y `gradlew.bat --project-cache-dir
../s48-adversary-private/project-cache -I ../s48-adversary-private/lane.init.gradle
-PscalebrewsBuildDir=../s48-adversary-private/build runGameTest --console=plain`
(propiedades/rutas entre comillas en PowerShell). Oracle conservado sin reparación
productiva en `../s48-adversary-private/src/io/github/r3neer/scalebrews/collision/internal/S48PrivateLifecycleProof.java`:39,
SHA256 `8217c47dada417c099803ead4076ba4a3d18951017fd827bb14bd16ab5edefc5`.
Caso devuelto al IMPLEMENTER; otras fronteras continúan pendientes.


### Resultado final independiente

**Funciona en el alcance S48.** Recibo durable:
`docs/evidence/S48/adversarial-verification.json`; 367 hashes productivos coinciden
con el recibo IMPLEMENTER, sin modificaciones productivas del ADVERSARY.
AnatomyRuntime SHA256 aa2819b9876796521acbac2c09a9d47189e0b4a2e1acc318ff120fcf834fbe05;
AnatomyMovement SHA256 97dae200efd6db211005a5bc7b4c07d25313a6a932adc70bf1da19f30d393736.
Las evidencias anteriores conservan su clasificación histórica, no sustituyen
la pasada final sobre F3/F4.

Lane pública promovida: `promoted-verification.log`, servidor3/3 (dos oráculos y
sentinel) y cliente integrado original real, cierre limpio PASS1m38s. Código
reusable en S48AdversarialVariantFixtures, VariantPreparationTests,
LiveLifecycleTests y LiveBindingClientProof. El test puro de preparación pertenece
al manifest normal; el lifecycle con start/reload/stop global se ejecuta sólo en
la lane aislada, sin coexistir con otras sesiones Runtime de GameTest.

Preparación prueba resolución canónica de known-empty/default frente a unknown;
getter de policy retirado además del retorno null; recuperación, reader/domain
capturados y registro tardío que sólo afecta una nueva preparación. Unión de32
ejes acepta y lee32; unión de33 rechaza antes de leer. Lifecycle real prueba
edad nativa A→B→A sin default, generaciones físicas nuevas y ausencia hasta
endpoint canónico; el soporte ajeno conserva su descriptor. Un ItemEntity real
confirma contacto positivo sobre A y pierde ese contacto inmediatamente al
cambiar edad, sin mover el cuerpo ni aplicar A en un carry posterior. Registro
manual conserva slot/provider y observa policy B (RED1/F3 reparado). Provider
retirado aún samplea en aislamiento (control positivo), pero currentSnapshot no
lo readmite (F4). La query de raíz no avanza walkPhase; tras movimiento voluntario
real de .125 el endpoint B conserva avance del clock del participante. Baseline
`public-clock.log` PASS3/3,30s.

Cliente captura el modelo Cow26.2 original mediante su factory/bake real,
filas con mismo model/pose/root y distinto filtro, token exacto del servidor,
endpoint físico y selección renderer coherentes. Unknown retira ambos consumidores
antes de40 ticks, menor que TTL100; replay de la publicación A real no revive
material, y la recuperación B usa nuevo registro. Snapshot de cada endpoint
contiene raíz real, vértices world y normalización inversa de esa raíz. Comparación
`adversarial/s48-live-comparison.png` inspeccionada:10→9 piezas, retiro exacto de
root/body/cube_0, generaciones1→2 y error de vértices retenidos0. Son vértices de
colisión con pose estática original, no certificación de viewport ni animaciones
de toda la familia.

Sensibilidad final del tooling público, compilado y ejecutado contra los mismos
oráculos: clock-reset RED54s (continuidad locomotora), generation-reuse RED32s,
stale-provider RED37s, stale-policy RED29s y withdrawal-omitted RED1m50s en la
espera limitada de retirada. Cinco kills semánticos; errores iniciales al generar
el statement vacío del último mutante se clasificaron como tooling y no se
contaron. El primer mutante de policy escapó un oracle que comprobaba sólo el
retorno read; se añadió el consumidor getter, sin debilitar el oracle ni cambiar
producción, y el mismo defecto quedó RED. Recetas y hashes durables en el recibo.

Rendimiento: PREPARATION captura dominios/planes; HOT_TICK/HOT_MOVE_QUERY consulta
identidad local y ≤32 ejes. HOT_NETWORK retira usando endpoint ya congelado y
destinatarios de ese soporte. Se revisaron participantes/byLevel, lifecycle y
retiro de contactos/recibos locales; no hay scan de otro nivel, introspección ni
preparación de modelo en la lectura estable. Contador de resolución positivo
instrumentado PASS3/3,1m13s:10000 lecturas estables/10000 lecturas del eje y cero
resoluciones canónicas repetidas;100000 lecturas asignan64bytes (cota4096 para
ruido de instrumentación/JVM). Bounds de registry1024/domains256/text256 y unión
axes32; cambios construyen un mapa de esos ejes, no una copia del catálogo.
No es benchmark global de tick/render. Ocho gates históricos pasan con sus
obligaciones vigentes, logs preservados.

Reproducción (JDK25, dependencias locales; citar argumentos -P en PowerShell):

```powershell
.\gradlew.bat --project-cache-dir ../s48-review/cache -I tools/s48_adversarial.init.gradle '-PscalebrewsBuildDir=../s48-review/build' runGameTest runClientGameTest
python tools/s48_adversarial_snapshots.py ../s48-review/build/s48-snapshots
python tools/s48_adversarial_mutations.py ../s48-review/shadows
```

Mutantes: añadir `-Ps48ShadowDir=../s48-review/shadows/<case>` y
`-Ps48ShadowOriginal=src/main/java/io/github/r3neer/scalebrews/collision/internal/<owner>.java`
según mutations.json; usar salida propia diferente a la baseline. Instrument usa
PreparedVariantBindings y `-Ps48RequireCounter=true`; los demás esperan RED.
CLI normal detectó un NPE de capture al no existir la propiedad de snapshot;
clasificado TEST/TOOLING y reparado exclusivamente con output default relativo
a la sesión, sin alterar oráculos. Repetición sin esa propiedad explícita
`no-snapshot-property-client.log` PASS1m25s, cierre limpio; snapshots durables
proceden de esta fuente final y vuelven a comparar error0,10→9,gen1→2.

El init permite s48AdversarialRunDir/s48SnapshotDir explícitos y no contiene JDK
ni ruta de máquina fija. Las shadow sources nunca reemplazan archivos productivos.

F5 sólo cambia la salida de una fixture. Su regresión pertinente es la lane
cliente pública S48, con los mismos oráculos, sin volver a ejecutar Tiny Mounts.
La repetición normal34 detenida por IMPLEMENTER no cuenta como PASS ni como bug;
se conserva normal33/F4 y servidor493 como evidencia anterior aplicable. Para
ejercer la ausencia de propiedad desde tooling reusable, el init admite
`-Ps48UseDefaultSnapshots=true`: omite la configuración de scalebrews.s47SnapshotDir
y la fixture escribe bajo el runDir en s48-adversarial-snapshots.
`public-default-snapshots-client.log` PASS57s con cierre limpio, un solo entrypoint
S48 y sin Tiny Mounts; captura/comparación final mantienen10→9 y error0. Recibo
actualizado conserva hashes de los fixtures/oráculos y del init reusable final.

```powershell
.\gradlew.bat --project-cache-dir ../s48-review/cache -I tools/s48_adversarial.init.gradle '-PscalebrewsBuildDir=../s48-review/build' '-Ps48UseDefaultSnapshots=true' runClientGameTest
python tools/s48_adversarial_snapshots.py ../s48-review/build/s48-run/s48-adversarial-snapshots
```

Fundamentos nuevos recertificados **TRUSTED sólo para S48**: plan/cursor preparado,
ownership por participante, barrera de retiro local, conservación de clock y
consumo cliente de token completo/withdrawal. Los readers SPI mantienen obligación
de pureza/coste acotado y notificación para cambios no sincronizados. Selectores
específicos de familias, cobertura física FULL, benchmark global y necesidades
humanas G9 siguen abiertos. Pasada final fuente/callers/ownership/bounds/lifecycle
y segunda pasada contra evidencia/promoción: cero cambios productivos requeridos.
UTF-8 estricto de las superficies propias y diff-check pasan; sólo se normalizó
whitespace al consolidar las fixtures, sin alterar ningún oracle ejecutado.
Limpieza final:24 archivos privados one-off retirados mediante rutas explícitas,
sin borrado recursivo. Los comandos/fuentes privados citados en los registros
históricos ya están sustituidos por la receta pública anterior;38 logs conservan
sus hashes y permanecen cachés, builds, snapshots y modelo previo. Inventario con
hashes de las fuentes retiradas: `docs/evidence/S48/adversarial-private-cleanup.json`.


## 9. Fallos y bucles

F0 TOOLING: primer comando Gradle pasó una propiedad -P sin comillas en
PowerShell y produjo `Cannot convert '' to File` antes de compilar. Corregido
quoting y exit-code propagation; compilación real PASS. Ningún cambio productivo
fue dirigido por ese fallo. Un apply_patch con contexto incompleto fue rechazado
atómicamente, confirmado sin cambios y reaplicado con contexto exacto.

F1 PRODUCTION, revisión IMPLEMENTER: la creación del provider y asignación de
generación estaban fuera del guard de instalación. Una autoridad agotada podía
propagar excepción tras retirar A y reintentarse cada query. El guard incluye
ambas operaciones; failedBinding mantiene ausencia sin reutilizar autoridad ni
reintentos para esa fila. Probe propio fuerza nextBindingGeneration=0 y observa
100 consultas, PASS7/7 con sentinel en43s junto al probe manual corregido F3.
Sólo un diagnóstico INSTALLATION_FAILED. No se modifica el contador para recuperarlo.

F2 TOOLING: ../s48-prepared-server.log falla52s antes de los escenarios porque
el comando omitió scalebrewsAnatomyCatalog. Fuente de la suite confirma que exige
el export aislado; se repite con ../s48-normal-run/anatomy-export recién producido
por cliente. No se cambia producción ni fixture para resolver este fallo.
Comando corregido PASS45s:24escenarios (índices0–23) + sentinel,2/2 GameTests.

F3 PRODUCTION/selection-policy, RED1 ADVERSARY: un participante con registro
manual conservaba provider/slot correctamente, pero el early-return de refresh
congelaba la observación catalogBinding en A después de age→B. Log privado
manual-policy.log:119 y assert independiente de policy actual; no se modifica
ese oráculo. El cursor también lee bajo ownership manual, pero esa lectura
nunca retira/reclama su slot. catalogBinding devuelve la observación del cursor,
no el Active histórico conservado para fixtures de publicación. Probe propio
observa policy B mientras el slot manual A permanece idéntico. Replay independiente
del mismo oráculo intacto PASS2/2, post-red1-server-client-2.log:693–696, cierre
limpio; AnatomyRuntime SHAaa2819b9… (§8 registra su evidencia final). Regresión
normal sobre F3 pasa492/492; cliente33 en curso. No se deriva cierre de ese replay.

F4 PRODUCTION, revisión IMPLEMENTER: confirm/carry pueden conservar una referencia
local al provider mientras queryFrame retira su descriptor. Su segundo intento
currentSnapshot podía entrar en sample() de ese provider viejo porque el slot ya
parecía no causal. currentSnapshot exige ahora identidad del provider actualmente
registrado antes de aceptar cualquiera de sus rutas. Probe de retirada congelada
intenta el provider capturado tras detach y exige ausencia, sin re-evaluar A.
El oráculo independiente no se modifica; requiere revalidación de rutas afectadas.
Probes F4 PASS7/7, cierre limpio en1m9s; prepared F4 PASS24escenarios/2tests en1m56s.
La última pasada local IMPLEMENTER de source/ownership/ausencia/reentrada/clock/
localidad no pide más cambios productivos. F3 normal492/492+cliente33 PASS4m30s
precede F4; la regresión normal final se reejecutará con los oráculos promovidos.

Regresión normal F4 ejecutada sin selectors de suite: build492/492 y cliente33,
cierre limpio, PASS5m24s en ../s48-normal-f4.log. Manifest procesado coincide con
fuente107server/33client. Receipt IMPLEMENTER actualizado conserva estado OPEN:
367fuentes/recursos raw, JAR1204680bytes/745entries sin probes/test leakage,
SHA256 1ba19cdcd3f9503a158a601855dd2ab71810f8b856e812275534b5dfe4924ccd.
Los hashes de logs incluyen prepared/probes F4; la campaña independiente sigue
pendiente y una promoción posterior de sus tests requiere reproducir esa lane.

Reproducción pública IMPLEMENTER de los oráculos promovidos: servidor3/3 y
cliente original real PASS1m38s, cierre limpio en ../s48-impl-public-replay.log.
Snapshots regenerados e inspeccionados en docs/evidence/S48/implementer-replay:
10→9piezas, exclusión root/body/cube_0, error local retenido0, gen1→2, rootScale1.
Son piezas originales Cow con pose estática y filtro declarativo de fixture;
no prueban geometría/pose original baby ni FULL de esa familia. El cursor estable
100000lecturas asigna64bytes en la lane aislada. Build normal con preparación
promovida PASS493/493, cierre limpio1m6s; el mismo contador asigna312bytes allí.

F5 TEST/TOOLING: al integrar la prueba pública como cliente normal34, la captura
asume que scalebrews.s47SnapshotDir siempre está configurado por el init aislado.
../s48-client-promoted.log falla3m45s con NPE en LiveBindingClientProof.capture:45
(Path.of(null)), tras los controles normales anteriores. Devuelto al ADVERSARY
para proporcionar salida default reproducible sin cambiar aserciones ni producción.
El entrypoint permanece intacto. ADVERSARY repite sin propiedad explícita y pasa
en1m25s con cierre limpio. La repetición IMPLEMENTER de cliente normal34 fue
interrumpida mediante Ctrl+C tras la indicación del propietario de no repetir
Tiny Mounts por cambios locales de colisiones. Se clasifica INTERRUPTED_SCOPE,
no PASS ni fallo de producción. F5 sólo modifica output de fixture; el replay
pertinente es ese cliente focal sin propiedad, conservando sus oráculos. Producción
F4 ya tiene normal33 y servidor493 verdes. La política de regresión por frontera
queda en AGENTS/CONTRIBUTING; CI ordinaria y G9 conservan su integración completa.

Replay focal final IMPLEMENTER sin propiedad: PASS45s, cierre limpio en
../s48-impl-default-client.log con -Ps48UseDefaultSnapshots=true. Oráculos intactos;
snapshots default regenerados, comparación10→9/error0/gen1→2 y PNG idéntico byte
por byte al ya inspeccionado (SHA81efdc82…). Restaurado manifest normal en1s:
fuente/procesado108server/34client. No se atribuye PASS a la repetición interrumpida.
Última pasada completa IMPLEMENTER del diff, lifecycle, errores, API, memoria,
costes, oráculos y canon: cero cambios productivos y ningún hueco de evidencia
pendiente en este scope. ADVERSARY también converge con cero cambios (§8).
Receipt final compara los367 hashes productivos de ambos roles y el JAR real,
sin cambiar su SHA1ba19cdc…; límites de familias/benchmark/QA siguen explícitos.
Los init/collector one-off del implementador se retiran tras capturar evidencia;
los tres tools públicos del adversario son reusables, aislados de producción.

## 10. Cierre

- [x] Plan/modelo convergidos antes de producción.
- [x] Implementación, holdouts, mutantes y rendimiento del scope verificados; no benchmark G9.
- [x] Regresión servidor/cliente pertinente y evidencia espacial, sin convertir INTERRUPTED_SCOPE en PASS.
- [x] Revisión final cero cambios de ambos roles.
- [x] Canon/evidencia consistentes, infraestructura temporal retirada.
- [x] Commit local propio con trailer Sprint:S48; HEAD/árbol se verifican antes del siguiente sprint.
