# S47 — Identidad completa del binding en los frames autoritativos

Estado: CERRADO dentro del alcance de transporte/consumo exacto del binding; commit local propio Sprint:S47.

## 0. Roles

IMPLEMENTER: agente principal; producción y pruebas de desarrollo.
ADVERSARY: agente independiente existente; modelo, holdouts, fixtures, pruebas,
tooling y evidencia. No modifica producción.

## 1. Scope

Al terminar, un endpoint enviado por el servidor identifica el binding completo
aceptado y todos los consumidores del cliente usan exactamente ese miembro del
catálogo, sin deducirlo del renderer ni sustituirlo por el default. Se demuestra
con round-trip real del codec, histories/replay, variantes sin default y cliente
integrado. Es una dependencia de G6 tarea2, que permanece abierta.

Incluye FR-004, FR-024, FR-026, FR-033 y la frontera de identidad/lifecycle de
FR-080..082; NFR-004, NFR-007..010, NFR-014..018, NFR-027. El transporte completo
de una selección no certifica todavía que el servidor derive sus variants del
estado vivo. No añade readers de traits, engines de nuevas familias, catálogo
FULL, Clinging/VP26 ni aceptación humana.

Invariantes: resolver canónico único; bundle y revisión atómicos; geometría
ausente para identidad desconocida; gen de binding y tracking independientes;
seriales, intervalos certificados, retirement y relojes anteriores conservados;
main independiente preservado; ninguna serialización/hash/reflection nueva en
tick/query/render. No se modifica el protocolo del catálogo ni sus recursos.

## 2. Investigación

HEAD inicial 6e7044a6b575b39336e54be077b8ff0af4faed29; árbol limpio. origin/main
b5e6253584b2233e5dd0039203c8a251bf10cdeb ya integrado después del fetch.
S00 y dependencias de identidad/intervalo vigentes; S46 certifica únicamente el
índice preparado. El runtime actual registra defaults. El packet v6, histories,
tombstones, descriptor y presentación identifican modelo/pose/root y generaciones,
pero no los parámetros, filtros o policy de la fila. Cliente y renderer consultan
bindings() por tipo, perdiendo variantes ya preparadas.

Durante la inspección se observa que la asignación de generación de un descriptor
con generación0 reconstruye una sobrecarga que pierde el root explícito. Preservar
todas las coordenadas del descriptor al asignar generación es necesario para la
migración completa de esta frontera; no se reabre evidencia histórica de S46.

## 3. Plan — IMPLEMENTER

- [x] I1 Extender descriptor/GeometryIdentity con el token preparado de S46 y
  conservar las sobrecargas anteriores sólo para fixtures/registraciones locales.
  Asignar generación preserva root y token; publishedFrame los copia sin lookup.
- [x] I2 El registro causal real del runtime recibe el ID exacto del índice
  aceptado. El frame inmutable conserva esa selección hasta el envío; no consultar
  la selección mutable durante sendPose, no calcular hashes en ese camino.
- [x] I3 Packet anatomy_pose_v7 transporta el token completo. Codec exige un ID
  de formato v1 válido y falla antes de publicar datos incompletos. Constructores
  anteriores pueden representar una fixture sin token, pero esa fixture no se
  puede codificar ni aceptar como autoridad live. Un helper de copia explícita
  permite a las fixtures declarar el ID preparado sin debilitar sus oráculos.
- [x] I4 Histories de frames/pose y tombstone incorporan el token a la identidad
  estable. Cambiar sólo token con gen estable o retrack puro se rechaza; binding
  nuevo requiere restart y retiro de material anteriores. Ejes restantes siguen
  independientes. Compatibilidad local sin token no inventa uno.
- [x] I5 Resolver receptor exacto por tipo + ID y verificar modelo/pose/root del
  packet contra esa misma fila. Todos los consumidores de física, evaluador y
  presentación, incluido CausalModelRender, usan esa resolución. Catálogo sólo
  con variants ejecutables puede activar cliente; no usar bindings().isEmpty().
  PresentationIdentity/cache contienen el mismo token. Unknown/wrong-type/mismatch
  no acceden al default. Recepción sin entidad valida al menos existencia y tuple
  del miembro, antes de guardar historia, y valida tipo cuando aparece la entidad.
- [x] I6 Pruebas de implementación para codec/identidad preservada/resolución y
  regresión servidor/cliente. ADVERSARY migra las fixtures wire afectadas al
  contrato explícito y crea su batería independiente después de la producción.
- [x] I7 Revisión completa, evidencia durable, canon actualizado sólo con lo
  ejecutado, infraestructura temporal retirada y commit propio antes de avanzar.

PREPARATION: IDs ya construidos por revisión; registro copia el ID de objeto
propio. HOT: copia de referencias/lookup acotado y comparación de IDs, sin codec,
SHA ni compilación adicional. Wire añade un Identifier por frame; memoria añade
una referencia en identidades/histories/tombstones de presupuestos ya acotados.
No nuevo registry, cache de segunda selección, escaneo de entidades o fallback.

Compatibilidad: constructores antiguos de DTO/kernel continúan compilando y
representan autoridad local incompleta, nunca un wire válido. El payload cambia
de TYPE explícitamente; peer v6 no recibe silenciosamente bytes v7. Las pruebas
históricas que transportan fixtures deben declarar el binding aceptado o un token
de fixture explícito para probar sólo el codec; su propiedad original no cambia.
Fixtures que copian una identidad live preservan su token. No se sustituye un
token faltante derivándolo de modelo/pose/root, pues no determinan el binding.

Revisión P1: se separa transporte de selección viva para mantener una tesis única.
Revisión P2: se incorpora validación de miembro antes de guardar frames adelantados
a la entidad, y preservación del root al asignar generación0. Revisión P3 completa
contra canon/callers/lifecycle/performance: sin cambios. Modelo independiente
convergido en sección4 antes del primer cambio productivo.

## 4. Modelo previo — ADVERSARY

Rol activo: **ADVERSARY**. Modelo conceptual construido desde requisitos,
arquitectura y protocolo vigentes tras el cierre S46 en6e7044a6, antes de leer
este plan. No se han usado cuerpos de tests históricos ni fixtures IMPLEMENTER
como fuente de casos, ni diseñado batería concreta.

**Modelo acotado al transporte.** S46 certifica el miembro preparado y su ID de
contenido; S47 debe transportar la elección ya hecha por la autoridad y consumir
exactamente ese miembro. ID, modelo, pose, root, parámetros/channels, filtros,
policy y exclusiones no pueden resolver filas distintas entre registro, frame,
receptor, proveedor físico y presentación. El ID describe contenido declarativo:
no autentica al emisor, no certifica bytes de modelos y no sustituye revisión,
provenance, dimensión, UUID/network ID ni generaciones de binding/tracking.
La selección del estado vivo y su equivalencia con modelos originales quedan
UNPROVEN para el siguiente frente, sin bloquear esta dependencia coherente.

**Material y lifecycle.** El frame conserva la elección aceptada como valor
inmutable; el envío no puede derivarla de un catálogo/binding mutable posterior.
Un miembro desconocido, ajeno o nominalmente discordante no accede al default
ni inventa geometría. La llegada previa a la entidad exige validación del miembro
y tuple antes de guardar material, seguida de la validación de tipo cuando sea
posible. El cambio de token es un eje real de identidad aunque modelo/pose/root
sean iguales: una nueva identidad física requiere nueva generación/restart y
retiro de la anterior; retracking por sí solo no autoriza ese cambio. Histories,
tombstones, endpoints, intervalos y caches no pueden mezclar los tokens ni
revivir material viejo al retornar a un contenido anterior. Los fences restantes
y el material pendiente válido de un lifecycle nuevo conservan su contrato.
Consultas de render/cámara continúan puras; no se introduce una segunda elección.

**Contrato y preparación.** Wire v7 es una frontera explícita, sin interpretar
bytes nuevos como v6. Las sobrecargas locales antiguas no crean un token por
inferencia de tres IDs y no se vuelven autoridad wire por estar presentes en una
fixture. La asignación de generación conserva todas las coordenadas, incluido
root explícito y token. Sólo objetos del índice aceptado aportan el ID preparado;
normalización/hash/codec/compilación siguen fuera de registro hot/envío/query/render.
La proyección default sigue existiendo para compatibilidad de selección actual,
pero su vacío no implica ausencia de variantes ejecutables en el cliente.

**Fundamentos y coste.** Las extensiones de descriptor, transporte, histories,
recepción y presentación son UNPROVEN hasta recertificación independiente.
S46 conserva su confianza scoped, sin extenderla a esta frontera. Nombrar como
variables frames/endpoints, entradas retenidas, revisiones, receptores y frecuencia
de reinicios; comprobar localidad, asignaciones, recomputación y retención acotada
según las rutas HOT_TICK/HOT_NETWORK/HOT_MOVE_QUERY/HOT_PRESENTATION afectadas.
La comprobación de miembro antes de existir la entidad también necesita lookup
preparado, sin scans del catálogo ni trabajo proporcional a parámetros/filtros.
Pruebas posteriores requieren codec real, wiring de autoridad/receptor, lifecycle,
cliente físico/presentación y sensibilidad; evidencia espacial/temporal controlada
cuando aporte señal. Un round-trip aislado no demuestra autoridad live ni FULL.

**Convergencia: funciona, continúa.** La pasada independiente completa de I1-I7
contra el modelo y el canon no encuentra una contradicción bloqueante. I4 conserva
la frontera entre binding y retracking; I5 evita tanto default fallback como
historia adelantada sin validación. La preservación del root al asignar generación
es necesaria en esta migración, con recertificación scoped. G6 tarea2-6 y G7-G9
siguen abiertas; este dictamen es conformidad de plan y no aceptación de una
implementación inexistente. Holdouts concretos se construirán después de esta
convergencia y permanecerán privados hasta ejercitarlos.

## 5. Implementación — IMPLEMENTER

I1–I5 implementados. No nuevos owners productivos ni recursos: se extienden las
identidades existentes, el índice y los consumidores. El registro de runtime
recupera el ID del Binding propio del snapshot y lo instala en el descriptor;
publication/factory/wire copian esa referencia sin consultar selección mutable.
La asignación de generación0 preserva root y token. GeometryIdentity incorpora
el ID también en equality de handles/intervalos; acceptsIntervalIdentity lo
contrasta con el binding activo propio del índice, conservando los demás ejes.

El packet TYPE v7 y su codec real requieren un token versionado completo; no se
infiere uno para sobrecargas locales. Encode rechaza ausencia antes de escribir
el cuerpo y sendPose la rechaza antes de encolar. La validación léxica es un
bucle constante de64hex, separado de of()/SHA/codec de preparación. No es prueba
de pertenencia; resolvePose hace lookup preparado y verifica revisión, tuple,
policy habilitada y tipo cuando éste existe. Admisión previa a la entidad no
puede guardar tokens inexistentes o tuples contradictorios.

Histories y tombstones retienen el ID. Cliente físico/evaluador/presentación y
renderer resuelven exactamente la misma fila, sin consultar defaults; el gate
cliente usa tamaño del índice completo. PresentationIdentity y su cache key
incluyen el token. El cache del adaptador original conserva su clave de geometría
modelo/filtro/resource-generation, porque ese objeto sólo prepara geometría:
puede compartirse entre poses/policies de la misma geometría sin elegir bindings.
No se duplica un cache autoritativo ni se cambia el sampler original.

Seis probes IMPLEMENTER cubren round-trip del codec real y lookup al mismo
objeto, rechazo de fixture local/mala versión, variantes sin default con mismo
modelo/pose/root y distintos filtros/policy, desconocido/wrong-type/tuple/revisión,
history/tombstone y preservación de root/token al asignar generación. Una
segunda lectura refuerza la fixture para que filtros/policy difieran materialmente;
la repetición481/481 y las pruebas independientes posteriores verifican esa fixture.

Revisión IR1: añade rechazo explícito antes de sendPose y comprobación léxica de
PresentationIdentity. IR2: elimina lambda capturante del lookup hot común y revisa
policy habilitada en física/render; no añade hash ni compilación. IR3: pasada
completa de source/callers/constructor compatibility/identidad/fences/recepción
adelantada/presentation/presupuestos/ownership produce cero cambios productivos.
IR4: revisión del presupuesto del nuevo campo acota readUtf al tamaño exacto
del token (86caracteres ASCII), antes de parsear Identifier; evita usar el máximo
genérico de32767caracteres para un campo fijo. Bytes válidos no cambian. IR5:
pasada completa repetida después de ese ajuste produce cero cambios; la batería
afectada debe repetirse. Primera compilación PASS48s y primer build/servidor PASS481/481 en1m29s; resultados
iniciales, no cierre. Default cliente inicial RED por fixture que pierde token.

## 6. Verificación — ADVERSARY

**Funciona, continúa dentro del scope S47.** Modelo previo independiente y
convergencia anteceden a estos casos. La prueba de transporte explícito no
certifica lectura de traits vivos ni promueve una familia a FULL.

Regresiones reutilizables: `S47AdversarialBindingTransportTests` (cinco casos)
y `S47AdversarialExactBindingClientProof`. La lane pública se reproduce con:

```powershell
./gradlew -I tools/s47_adversarial.init.gradle -PscalebrewsBuildDir=../s47-adversary-build --project-cache-dir ../s47-adversary-cache runGameTest runClientGameTest --console=plain
```

Servidor final intacto: **6/6 con sentinel**, cierre limpio,18s. Cliente público
focal repetido: **PASS52s**, cierre limpio. El combinado también PASS, pero
registró1h56m9 y un hueco entre finalizar servidor12:32 y arrancar cliente14:25;
no se atribuye esa pausa a un hot path ni se usa como medida de rendimiento.
La repetición focal acota el resultado funcional. La migración conserva root y
token de las copias live sin cambiar oráculos: AnatomyPresentationFrameTests,
S24ContactSupportRebindReplayClientProof y S29AdversarialLifecycleOrderingProof
pasaron juntos en cliente real1m44 con cierre limpio.

La batería independiente verifica codec v7 real y rechazo de identidad local
antes de escribir bytes; revisión/miembro/tipo/tuple exactos; variante ejecutable
sin default; generación0 con root independiente; token como eje de history y
watermark; y presupuesto fijo del lector86ASCII. En cliente real, dos filas con
los mismos IDs nominales cambian A→B con nueva generación: física, presentación
y selección del renderer usan el filtro exacto B. El replay A no restaura la
fila anterior. Un token inexistente serial100 llega antes de la entidad; el
miembro correcto serial1 se conserva y materializa tras aparecer el Cow.

Snapshot determinista del modelo original Cow26.2: A contiene10piezas y B9;
sólo retira `root/body/cube_0`, con todos los vértices restantes exactamente
iguales. JSON y overlay PNG en `docs/evidence/S47/snapshots/`, inspeccionados
visualmente. `tools/s47_adversarial_snapshots.py` reproduce la comparación;
no se equipara este overlay a aceptación visual de todo el resource pack.

Sensibilidad: **cinco RED esperados** al omitir token de history, omitir el tipo
al resolver, ampliar lector a32767, omitir token del tombstone y admitir un ID
inexistente antes de la entidad. El último conserva el positivo A→B y falla al
materializar el miembro temprano correcto. No se alteró producción: sombras
aisladas y recetas reproducibles en `tools/s47_adversarial_mutations.py` y el
recibo `adversarial-mutations.json`. El contador instrumentado se exige presente
con `-Ps47RequireHashCounter=true`: **200.000 lookups preparados, cero hashes
repetidos**,6/6 en19s; después el servidor intacto vuelve a6/6.

Coste: PREPARATION construye IDs e índice; HOT_NETWORK valida/copia token fijo,
HOT_TICK/MOVE_QUERY/PRESENTATION resuelven el mapa preparado y tuple acotado.
Variables R≤4096,S,O,H,P; no scan de catálogo/contactos ajenos, hash de records,
compilación o reflexión en esos hot paths. Token añade87bytes al wire y una
referencia en identidades/histories existentes; decode/Optional tienen coste
constante, no se afirma cero asignaciones. Caps/TTL previos siguen siendo owners
de retención. No se acredita el benchmark global de G9 con esta lane local.

Recertificación scoped **TRUSTED**: extensión de GeometryIdentity/BindingState;
codec/publicación de declaración congelada; resolución exacta de Snapshot e
índice; eje token de histories/tombstones; consumidores cliente físicos,
presentación y selección del renderer. El cache de adaptador comparte solamente
geometría por modelo/filtro/resource-generation, sin seleccionar pose/policy.
La preparación original y las familias aún no FULL conservan su clasificación.

Clasificación de fallos de infraestructura propios, sin reparar producción:
invocación combinada inicial carecía dependencia downloadAssets→runGameTest
(servidor4/4 llegó a PASS; runner corregido); primera aserción física se anticipó
al registro END_CLIENT_TICK (espera de setup); primera comparación de vértices
cambiaba umbrales y exclusión a la vez (setup ahora aísla la exclusión). Los
oráculos de autoridad, filtro, replay y codec no se rebajaron.

La revisión de infraestructura nueva sustituye el wrapper deprecado
GeometryExtractor.vanilla por el owner canónico ModelPartGeometryEngine.inspectVisible,
con el mismo árbol original Cow y matriz vanilla explícita. No cambia oráculos
ni producción. Cliente final PASS1m1s, cierre limpio; los dos JSON completos de
snapshots son lógicamente idénticos al público anterior. El recibo de fixtures
se actualiza tras esa repetición y queda congelado.

Dos pasadas finales de fuente y obligaciones no solicitan cambios productivos.
Recibo IMPLEMENTER verificado362/362, JAR734entradas/1184617bytes coincide con
SHA256a535f512… sin clases de fixture/instrumentación; ocho gates PASS. Evidencia
exacta, hashes, logs y límites en `docs/evidence/S47/adversarial-verification.json`.
La pasada final sobre el recibo IMPLEMENTER integrado106server/33client
(486/486 y cliente33PASS) y fuentes congeladas pide **cero cambios**. La versión
del fixture canónico coincide con el recibo adversarial. G6 tareas2–6, selección
viva y G7–G9 permanecen abiertos; el commit propio de S47 lo realiza IMPLEMENTER.

## 7. Fallos y bucles

F1 — fixture/migración de contrato. El primer default client run
../s47-client-i1.log falla en3m18s: AnatomyPresentationFrameTests.rebound copia
una identidad live mediante la sobrecarga histórica sin token y sendPose rechaza
la publicación incompleta antes de encolar. ADVERSARY recibe el observable para
migrar esa copia conservando el token/root live y todos los oráculos originales.
No se relaja el wire ni se inventa token en producción. ADVERSARY reproduce las tres fixtures cliente afectadas con salida limpia;
la repetición normal IMPLEMENTER PASS32 con cierre limpio4m46s
(../s47-client-i2.log). Oráculos intactos y token/root preservados.

F2 — comando/evidencia IMPLEMENTER. ../s47-prepared-i1.log PASS5m49s ejecutó
runClientGameTest con scalebrewsAnatomyPreparedSuite=true. El selector cambia sólo
fabric-gametest, no fabric-client-gametest: esa ejecución demuestra los32 clientes
normales con configuración de entrada servidor preparada, NO los24 escenarios
server de AnatomyPreparedIntegrationProof. Se corrige la atribución sin borrar
el log ni cambiar assertions; ../s47-prepared-server-i1.log ejecuta ahora el
runGameTest adecuado sobre el mismo catálogo: PASS35s,24 escenarios con índices
0–23 dentro de un test más sentinel2/2, cierre limpio. No hay defecto productivo.

## 8. Revisión final

IMPLEMENTER reproduce la lane pública independiente:6/6 servidor y cliente real
exact-binding, cierre limpio1m33s (`../s47-impl-public-final.log`). Reproduce el
comparador y examina el overlay de10→9piezas: únicamente la exclusión declarada,
vértices restantes idénticos. No certifica visualmente el pack completo.

Después de promover ambos entrypoints, el build normal final pasa486/48644s
(`../s47-build-final.log`) y el cliente normal33 pasa3m33s con cierre limpio
(`../s47-client-final.log`), incluida la factory canónica final de la fixture.
Los procesados normales coinciden con source106server/33client. Antes habían
pasado481/4813m10s, cliente32 4m46s y prepared server24escenarios/2tests35s.
F1/F2 mantienen sus clasificaciones y logs; no se atribuye el cliente selector
erróneo a los24 escenarios preparados.

Recibo final IMPLEMENTER fija362 hashes productivos y el JAR734entradas/
1184617bytes SHA256a535f512111e04df4f7174da70c8eb07a5edb90bff9365251b6de227dcbf9c19,
sin fixtures ni instrumentation.14 fuentes productivas existentes cambian;
ningún owner/recurso nuevo. Producción permanece idéntica a la revisión ADVERSARY.
Sus fixtures/logs se contrastan por hash; el cambio de helper posterior al primer
público tiene clasificación, repetición PASS y equivalencia exacta de snapshots.

Las revisiones completas finales de ambos roles producen cero cambios productivos;
los ocho gates se reproducen, con sus límites presence-only. Los tools públicos
son reutilizables, sólo de tests; infraestructura privada no entra en el producto.
Evidencia durable en docs/evidence/S47, VALIDATION y el plan canónico. TRUSTED sólo
para identidad transportada, miembro exacto, histories/replay y consumidores
recertificados. Selección viva, familias FULL, G6 tareas2–6 y G7–G9 siguen abiertas.
No se solicita intervención humana al cerrar este sprint.

## 9. Cierre

- [x] Scope y modelo convergidos antes de producción.
- [x] Implementación y tests relevantes completos.
- [x] Holdouts/mutantes/performance y nivel cliente ejecutados.
- [x] Revisión final de ambos roles produce cero cambios.
- [x] Evidencia y documentación canónica consistentes.
- [x] Commit local propio Sprint:S47; verificación HEAD/árbol obligatoria antes de avanzar.
