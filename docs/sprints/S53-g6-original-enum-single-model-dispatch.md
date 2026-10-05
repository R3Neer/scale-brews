# S53 — Selector original enum → modelo único

Estado: CERRADO scoped input/model selector; commit local propio Sprint:S53.

## 0. Roles

IMPLEMENTER raíz modifica producción/desarrollo. ADVERSARY existente conserva
contexto clean-room y holdouts, no repara producción. Comunicación operacional.

## 1. Scope y S00

G6 tarea2, misma frontera de procedencia completa S51: componente autoritativo
Holder<Record<ModelAndTexture<Enum>>> â†’ extractor original â†’ field de estado â†’
dispatch completo → Map[enum] → EntityModel único → slot real LivingEntityRenderer.
Sin eje de edad inventado. FR-004/016/024/026/033/038; NFR-007–010/015–018/024–027.
No binding ejecutable, pose/root/collider, FULL, física/red/mount/pasajeros/asientos.
HEAD97f8dc4b limpio, main b5e6253584b2233e5dd0039203c8a251bf10cdeb integrado.
S00 scoped: parser OriginalModelPrograms S51 y fingerprint ModelPartStructure S45
TRUSTED en sus límites actuales; selector/input existentes candidatos a reutilizar,
recertificados para extracción compartida. No nueva dependencia UNPROVEN asumida.

## 2. Investigación

Recurso original26.2 pinned ec9c864e8a304be37d1d273fefefcb4e4dcc2154a18ccd3c1fe9d2d0a298a7d9.
ZombieNautilusRenderer es witness de tecnología, no lista Java de especies.
Submit typed completo tiene guard null, Map.get(record.modelAndTexture().model()),
cast EntityModel, escritura model heredado y super MobRenderer.submit con mismos
argumentos. Extract typed captura equipo copiado y variante Holder original.
Mob.getBodyArmorItem completo es this.getItemBySlot(EquipmentSlot.BODY), sin otros
calls/aliases; resolver su despacho en actor, exigir owner Mob y dependencia
getItemBySlot LivingEntity original pinned. Getter variante/get(component)/bridges
reusan la tesis S51, sin inferir equivalencia a partir de nombre/clase/inventario.
Javap de investigación fuera de repo: ../s53-enum-single-selector.javap.txt y
../s53-input-getters.javap.txt.

## 3. Plan IMPLEMENTER

- [x] I1 Extraer verificación común de programas/input/origen/record/bridges de
  EnumAgeModelPreparation a helper package-private OriginalEnumSelectorProof.
  Selector recibe familia explícita ENUM_AGE o ENUM_ONLY; ninguna autodetección
  que amplíe silenciosamente la API histórica. Preservar records, límites,
  TOOL_VERSION, canonicalización, razones y resultados S51 para familia anterior.
  OriginalModelPrograms y S52 permanecen intactos. Conservar hashes de todos los
  demás owners y comparación de receta S51 previa/post bajo recursos originales.
- [x] I2 Certificar la tecnología ENUM_ONLY por programa completo, null guard,
  mapa/record/accessor/.model, cast EntityModel, model real y delegate original;
  cuatro bridges completos, input exactactor/componente/Holder/getter. Para la
  captura incidental body armor probar programa completo del getter real Mob,
  campo BODY original y getItemBySlot original LivingEntity, sin overrides.
  Esta extensión incidental sólo se admite en familia ENUM_ONLY. Helpers nuevos
  usan caps existentes y registran bytes/resource hashes reales; no execute bytecode.
- [x] I3 Añadir API client-only EnumModelPreparation.inspect(renderer) PREPARATION,
  receta inmutable sin edad/estado inventado, una fila por enum/source/slot exacto
  y fingerprint inicial. Dominio completo â‰¤256, filas/modelosâ‰¤256, cache identity
  pass-local, límites S45; ausencias totales y diagnósticos≤256, no shared mutation
  ni reset/no referencias renderer/model/world retenidas.
- [x] I4 Desarrollo nativo recorre componenteâ†’extractâ†’submit original y verifica
  identidad del modelo diferido para todo dominio enum, fingerprints, fuente,
  determinismo/inmutabilidad/shared state, ausencia/familia ajena/incompleto.
  Compilar artifact/GameTests y ejecutar cliente focal S53 y regresión S51 propia
  y pública por helper compartido. S45 mediante fingerprints de modelos afectados;
  S52/OriginalModelPrograms intactos no exigen repetir batería. No Tiny/full server
  ni default client ajenos; manifest normal se restaura, G9 completo obligatorio.
- [x] I5 Campaña adversarial, sensibilidad/oracles/snapshots/coste, reviews finales
  completos cero cambios, canon/VALIDATION/receipts/cleanup y commit local Sprint:S53.

## 4. Revisión IMPLEMENTER y presupuestos

PREPARATION explícita sin caller hot/initializer. Caps parser64classes/1024methods,
512instructions,1MiB/class/8MiB total intactos;2 familias separadas, ENUM_ONLYâ‰¤256
rows/models, ENUM_AGE límites S51 intactos. Complejidad por recursos/programas y
estructuras de modelos únicos, cache pass-local, sin worlds/locks/caches globales.
Primera revisión deriva cada tramo de original bytecode/resolución/descriptores/
alias/branch y conserva exclusión de familia anterior. Segunda revisión completa
plan/requisitos/ownership/deps/error/inmutabilidad/coste/regresión produce cero
cambios. Modelo y convergencia independiente previos completados en secciones5/7.

## 5. Modelo previo — ADVERSARY

Modelo previo independiente derivado de canon antes del plan S53 (nota privada
post-S52, HEAD97f8dc4b). G6 tarea2 distingue prueba de fuente del actor de prueba
state-predicate; la cadena exige actor original exacto -> getter/componente
Holder/record/model enum -> extractor efectivo -> field de estado -> dispatch
efectivo -> objeto exacto Map[enum] -> estructura inicial. La asociacion
EntityType/renderer, pose/root/layers/material/FULL permanece fuera de esta
prueba; inspect(renderer) no inventa ese tramo.

Invariantes: resolver declaraciones y despacho virtual/bridges reales, comprobar
programas completos con ramas/joins/aliases y provenance acotada, dominio completo
finito, recursos exactos, ausencia total ante un tramo desconocido. No atribuir
input de edad a esta familia; tampoco aceptar null como variante valida ni
recuperar modelo anterior si el submit nativo retorna sin consumo. Los modelos
se comparan por identidad en el consumidor diferido, no solo clase/geometria.
El estado compartido/inicial se preserva; receta determinista inmutable no
retiene referencias vivas. Un getter incidental de equipo no amplifica autoridad
ni altera family ENUM_AGE.

Fundamentos scoped: OriginalModelPrograms TRUSTED parser acotado S51 y
ModelPartStructure TRUSTED copia inicial S45. La extraccion/selector nuevo y el
helper compartido quedan UNPROVEN hasta recertificacion. Extraer un helper no
es equivalencia automatica: API/canonicalizacion/recetas/digests/ausencias/
caps de S51 deben conservarse con referencia anterior inmutable y replay
original propio/publico. S52 permanece solo predicate->slot y no lo promueve.

Coste PREPARATION/OFFLINE: H jerarquia, C clases/bytes, M metodos, I instrucciones,
D dominio, U modelos unicos y P estructura. Caps antes de expansion, parsing y
fingerprints compartidos por identidad dentro de la pasada; sin scans/refs
mundiales, caches globales ni reflection/hash/serializacion en paths hot.
Campana concreta/sensibilidad/snapshots independientes despues de convergencia
y candidata; regresion pertinente del helper afectado, sin Tiny/default ajenos.

## 6. Implementación — IMPLEMENTER

Un helper package-private nuevo OriginalEnumSelectorProof comparte programas,
record/schema, componente/getter/extractor y cuatro bridges, con familia explicita.
EnumAgeModelPreparation conserva su API, version, dominio/edades/canonical/digests;
JSON integro previo/post S51 igual para tres recetas14 filas. Nueva API cliente
EnumModelPreparation admite solo mapa de modelo unico por enum, una fila/key sin
edad. Captura incidental body armor demuestra getter Mob completo y despacho
getItemBySlot original sin overrides. Caps y parser S51/S52 intactos;369 owners
previos byte intactos, un owner modificado y dos nuevos, total372.

Build/compileGameTests y cliente focal corregido PASS90s limpio: S53 cuatro
selecciones nativas de dos modelos originales con predicado age perturbado,
source/getter/extract/queued object, fingerprints, dominio completo, shared state
actual animado, determinismo/inmutabilidad. S51 propio14 y publico14/12 verdes.
Manifest normal112server/34client restaurado PASS2s, igual fuente/procesado.
JAR764 entradas/1264774 bytes, SHA0f7a9edeb42d8c69daf873db4a303e49c892d6b78cca9468e75c2454db3d1bcb,
sin tests leaked. Recursos14 exactos; nueve comparaciones merged/runtime de todos
sus programas conservan instrucciones/targets tras normalizar CP/ldc widths/offset
ordinal. No whole-class trust nuevo: dependencias existentes reusan aprobacion S51,
prueba nueva restringida a programas declarados. Evidencia/recetas/logs/fallo previo
conservados en docs/evidence/S53, recibo propio status DEVELOPMENT_PASS hasta
cierre independiente. Dos reviews completos cero cambios productivos; ultimo
fixture temporal corregido antes de GREEN, sin reparar produccion ni Tiny.


## 7. Verificación — ADVERSARY

Revision previa: funciona, continua. Inspeccion independiente del bytecode
local26.2 (javap propio sobre clientOnly/common originales) confirma guard null
con RETURN antes de consumo, selector Map.get/model cast/field model y delegado
Mob completo, captura SADDLE/getItemBySlot y bodyArmor/getBodyArmorItem antes
de Holder variante; bridges erased/typed deben seguir siendo prueba completa.
I2 limita el getter incidental a ENUM_ONLY y exige despacho real sin overrides;
la extension no puede colarse en ENUM_AGE.

Primera pasada del modelo/plan recorre cada arista de autoridad, origen,
descriptores/branches/dispatch, objetos/aliases y resultados. Segunda pasada
completa desde requisitos/dependencias/fundamentos, errores/ausencias,
inmutabilidad/presupuestos, scope y regresion produce cero cambios. El plan
converge: familia explicita evita autodeteccion ampliada, recertificacion del
helper shared exige referencia S51 anterior/post y su replay publico original.
No RED de plan pendiente. Ninguna prueba de produccion/campana realizada aun;
los nuevos owners y extraccion shared siguen candidatos hasta la entrega.
G6 tareas2-6, extraccion de edad S52, FULL y G9 siguen abiertos.


Aceptacion final scoped: funciona, continua. OriginalEnumSelectorProof y
EnumModelPreparation recertificados TRUSTED solo para ENUM_ONLY y ENUM_AGE
originales26.2 dentro de PREPARATION: actor exacto/componente/getter -> extractor
real -> estado -> enum -> objeto Map/slot real -> estructura inicial. La
asociacion EntityType/renderer no se inventa. Pose/root/layers/material/FULL y
G6 tareas2-6 permanecen abiertos; tampoco se promueve extractionAuthority S52.

Campana independiente real: dos valores del dominio ZombieNautilus, componente
mutado por setter nativo y extractor original, source reader concordante,
identidad exacta del objeto consumido diferidamente y fingerprints iniciales.
Null retorna antes de submit sin repetir modelo anterior. Map incompleto,
modelo null, enum ajeno, familias discordantes, renderer ausente, thread ajeno
y origen externo son negativos; alias comparte una sola estructura/identidad.
Recetas inmutables/deterministas y estado compartido actual/inicial preservados.

Dos programas sinteticos se rechazan con motivo exacto: BODY cambiado a HEAD y
null-guard con target incorrecto. Primer intento ADVERSARY_FIXTURE RED2m2s:
target sin label original causaba branch_target_missing antes del guard. Solo
cambio del operand hacia un label original existente; null_guard_target sigue
siendo obligatorio. Baseline reparado PASS49s y final PASS63s, sin produccion
reparada ni oracles relajados. Logs y fuente anterior conservados en evidencia.

Sensibilidad5/5: body-slot, null-target, origen, collapse de modelos, confundir
ENUM_AGE/ENUM_ONLY. Todos compilan y mueren por AssertionError semantico; no se
contabilizan errores de compilador. Driver reusable publico validado solo con
null-target: nativo exit1/68s por program fault admitted1, agregado PS exit0.
Regresion afectada S51 publica14 combinaciones/12 rechazos; tres recetas
previas completas iguales como objetos JSON, canonicalText/digests exactos.
No se repiten parser/S52/Tiny/default/full ajenos: owners/fundamentos intactos.

Lane publica S53AdversarialEnumProof + S51 via tools/s53_adversarial.init.gradle
PASS2m3s limpio; replay IMPLEMENTER PASS118s limpio. SVG y recipe.json del replay
byte identicos. Captura SVG/PNG de dos estructuras iniciales inspeccionada:
proyeccion determinista de cubos, sin claim GPU/layers/pose/root/material.

Rendimiento: PREPARATION explicita y cero callers runtime. Clases/bytes/metodos/
instrucciones bounded S51; dominio/filas/modelos256, source registry1024,
estructuras S45 bounded. Proof y fingerprint cache por identidad pass-local;
resultado solo hashes/metadatos inmutables, no actor/modelo/mundo retenido.
Verificados372 hashes/JAR764 entradas sin tests:369 owners intactos y un owner
previo delega prueba compartida con metadata literal preservada. No claim TPS
NFR014; G9 integrado sigue abierto. Dos reviews integrales de codigo/dispatch/
programas/origen/aliases/coste/canon/recibos producen cero cambios productivos.

Recibos/logs/capturas/tooling exactos en docs/evidence/S53/adversarial-verification.json
mas adversarial/. Ocho one-offs privados sustituidos retirados por paths exactos;
modelos, logs, caches/builds y evidencia preservados, sin borrado recursivo.
Tooling reusable: init y s53_adversarial_mutations.ps1. UTF8/whitespace revisados.

## 8. Fallos y bucles

IMPLEMENTER_FIXTURE: primer build/compile PASS, cliente RED69s por último oracle
shared state colocado después de submit nativo pero comparado contra pose previa
al submit. Primer inspect ya conservaba estado; NautilusModel.setupAnim y animación
original mutan legítimamente pose/visibilidad. Captura referencia posterior al
submit y anterior a los siguientes inspect; verificar preservación de ese estado
animado. Ninguna reparación productiva. Log original conservado fuera de repo,
../s53-original-enum-single.log; repetir la batería focal S53/S51 compartida completa.


## 9. Revisión final

Dos pasadas integrales IMPLEMENTER y ADVERSARY sin cambios productivos ni huecos
scoped. Caller ownership/client-only, dominio completo/identidad/aliases, cuatro
bridges/whole-program/getter/body reader/resolucion/origen, recursos/canonical/
fingerprints/ausencias/coste/caps, recetas previas S51 y snapshots revisados.
Replay publico118s PASS, driver root null-target native98s/exit1 por motivo
semantico y agregado0; SVG/recipe exactos, PNG inicial inspeccionado.372 hashes y
JAR764 intactos; main fresco b5e62535 integrado. Evidencia raw/index preparada,
ocho one-offs adversarios retirados, root no crea one-off script files.


## 10. Cierre

- [x] Bateria focal pertinente y sensibilidad scoped sin cambios productivos.
- [x] Ambas revisiones integrales cero cambios y evidencia/receipts/snapshots/coste.
- [x] Canon y herramientas reusables, cleanup, commit local propio Sprint:S53.

G6 tareas2–6/G7–G9 y QA humana al final de la épica siguen abiertos.
