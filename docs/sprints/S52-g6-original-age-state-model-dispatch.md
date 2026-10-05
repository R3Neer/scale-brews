# S52 — Selector original de modelos adult/baby desde render state

Estado: CERRADO scoped state-predicate→slot; commit local propio Sprint:S52.

## 0. Roles

IMPLEMENTER raíz mantiene producción/desarrollo; ADVERSARY existente conserva
modelo clean-room, holdouts y sus propias secciones. Comunicación operacional.

## 1. Scope y dependencia

G6 tarea2: certificar otra tecnología original de selección de modelos, el
dispatch heredado AgeableMobRenderer que consume LivingEntityRenderState.isBaby.
La receta declara un predicado del estado original, no un source autoritativo
de entidad. No publica sourceKey age, actor, binding ejecutable ni FULL: falta
certificar por separado la extracción concreta de cada familia. Esta distinción
es contractual, no un supuesto silencioso de equivalencia state.isBaby/isBaby().
FR-004/016/024/026/033/038; NFR-007–010/015–018/024–027. Sin física/red/solver,
mount/pasajeros/asientos/hooks hot/pose/root ni catálogo general.

HEAD3762a2ece71d1b96578761908bee0c4ed21895db limpio. main vigente comprobado
b5e6253584b2233e5dd0039203c8a251bf10cdeb integrado. S00 recertificación scoped:
OriginalModelPrograms S51 y ModelPartStructure S45 TRUSTED para inspección
acotada/fingerprints iniciales; no confiar en variantes de extracción desconocida,
ni inferir selección de campos inventariados por S45. G1–G5 conservan sus scopes.

## 2. Investigación

Bytecode original 26.2 AgeableMobRenderer.submit: test isBaby original -> toma
babyModel/adultModel -> escribe LivingEntityRenderer.model -> super MobRenderer
submit sin cambiar argumentos. Bridge EntityRenderState castea/delega al método
LivingEntityRenderState. Clase base almacena modelos finales por constructor.
Subclases pueden introducir submit typed/bridges o modificar extractRenderState;
no asumir equivalencia por instanceof. Wolf y PolarBear sirven como witnesses,
no listas Java de soporte. JAR original pinned ec9c864e8a304be37d1d273fefefcb4e4dcc2154a18ccd3c1fe9d2d0a298a7d9.
Captura de investigación ../s52-original-age-selection.javap.txt.

## 3. Plan IMPLEMENTER

- [x] I1 Añadir preparación client-only OriginalAgeStateModelPreparation con
  inspect(renderer) de diagnóstico acotado/receta inmutable. Client thread;
  resource class/programas completos y origen/loader nativo. Sólo tecnología
  AgeableMobRenderer: exigir resolución real de ambos entrypoints submit (estado
  living y estado entity) a AgeableMobRenderer sin override en clase concreta.
  No aceptar un overload parecido ni puente que desvíe al slot/selector distinto.
- [x] I2 Probar programa completo del selector base y bridge, con aliases,
  tipos/descriptores, owners reales y targets de ramas; no handlers/extras.
  isBaby debe ser el campo original LivingEntityRenderState; baby/adultModel
  finales no estáticos del owner base, model escrito al owner LivingEntityRenderer;
  delegación exacta a MobRenderer y resolución heredada nativa LivingEntityRenderer.
  Constructor base completo prueba instalación de ambos parámetros en sus slots.
  Recursos originales de dependencias auditados/pinned; digests reales registrados.
- [x] I3 Leer ambos slots completos (no nulos, EntityModel) y producir dos filas
  con state predicate false/true, slot/modelClass y fingerprint inicial S45.
  Capturar estado de shared models sin reset/mutación. Cache de identidad pass-local,
  modelos compartidos producen dos filas y una estructura; metadata determinista,
  sin Model/renderer/world references. No copiar semántica de source age en receta:
  exact renderer y field/predicate demostrados, extractionAuthority UNPROVEN.
- [x] I4 Desarrollo nativo con varios renderers que realmente hereden estos
  entrypoints. Recorrer createRenderState/extract/submit original y comparar
  selección real queued model para adult/baby; además perturbar state.isBaby
  directamente para probar que la receta describe ese predicado, independiente
  de la entidad. Esto es oráculo de dispatch, no prueba de source→state.
  Determinismo/shared state/aliasing/immutability; fuera de familia/override/slot
  incompleto rechaza sin filas parciales. Compilar artifact y GameTests; cliente
  focal y regresión pertinente de los helpers S51/S45 según fronteras cambiadas.
  No Tiny/full servidor ni default client ajenos repetidos; CI/G9 completo sigue obligatorio.
- [x] I5 Campaña/sensibilidad y reviews integrales independientes sin cambios;
  recursos/costes/predicado/oracles/snapshots, canon/VALIDATION/evidencia/cleanup,
  commit local propio Sprint:S52 y verificación HEAD/árbol antes de avanzar.

## 4. Revisión y presupuestos IMPLEMENTER

Sólo PREPARATION explícita, sin callers initializer/runtime/hot. Reusar límites
S51:64 clases,1024 métodos/clase,512 instrucciones/método,1MiB/clase/8MiB total;
dos filas/dos modelos únicos y límites de estructura S45. Coste por bytes/métodos/
programas y partes/cubos de modelos únicos; pass-local sin cache global ni worlds.
Ausencia de prueba/slot completo implica ausencia total, nunca default admisible.

Revisión completa inicial verifica distinción state predicate/autoridad input,
entrypoints reales, owner heredado de model, instalación de slots finales, flujo
íntegro y immutable recipe. Segunda pasada no cambia el plan. La convergencia
independiente previa a producción está en §7. G6 tareas2–6/G7–G9 siguen abiertas.

## 5. Modelo previo — ADVERSARY

Modelo previo independiente desde canon, antes del plan S52: G6 tarea2 sigue
abierta. S51 certifica una tecnologia de input enum/edad y no justifica herencia,
composicion o fuentes de otras familias. Separar input autoritativo, extraccion,
state, dispatch/slot, estructura inicial, pose/root y material fisico. Ninguna
flecha se presume por nombre de campo/clase, instanceof o censo previo.

Tesis adversarial scoped para S52: una relacion de seleccion puede certificarse
como predicado del render state aunque la procedencia entidad->state siga abierta,
si la receta lo declara de forma inequivoca y no es binding/source ejecutable.
Aqui el dominio probado es false/true del field LivingEntityRenderState.isBaby,
no edad de actor ni sourceKey age. extractionAuthority permanece UNPROVEN;
no derivar actor/variant/collider/FULL ni admitir una familia fisica por esta
metadata. La instalacion inicial de los dos slots no prueba como se construyeron
las formas, ni root transforms, capas/composicion o animacion futura.

Obligaciones de admision: origen/loader/recursos exactos, dispatch real de ambos
entrypoints erased/living al owner base, bridge y selector completos, aliases y
ramas/join sin handlers/extras, field de predicado original y slots finales
adult/baby, asignacion al model real heredado y delegacion original. No un patron
parecido dentro de un programa distinto. Constructor completo debe vincular los
parametros a esos slots; overrides que cambian los entrypoints no heredan prueba.

Output inmutable/determinista sin referencias a modelos/world/renderer. Slot
incompleto, modelo desconocido o ausencia de cualquier prueba devuelve ausencia
integral. Shared slots mantienen todas las filas pero inspeccion estructural una
vez por identidad/pass; nunca resetear estado animado para obtener una receta.
Probar submit y cola nativos con estados extraidos por ruta original; separar
ese contraste de la perturbacion de isBaby que prueba solo el predicado. No
usar esa perturbacion para atribuir autoridad a la edad de una entidad.

Coste PREPARATION: bytes/clases/metodos/instrucciones y arboles de hasta dos
modelos unicos, con limites individuales y acumulados. Reflection/parse/copia
no aparecen en initializer, tick, movimiento, query o render normal. Cache
pass-local no retiene worlds ni se convierte en owner de seleccion live.
Preservar snapshots deterministas pertinentes de modelos originales y su scope.
Fundaciones S45/S51 se reutilizan solo dentro de su trust previo; cualquier
extension requiere recertificacion scoped y sensibilidad tras entrega productiva.

Regresion por frontera: selector nuevo, selector S51 y estructura/censo afectados;
cliente real en rutas cambiadas, sin Tiny/monturas ajenos ni full servidor por
este tooling local. G9 mantiene integracion completa y necesidades humanas.
Sin bateria concreta ni produccion antes de convergencia; holdouts privados.


## 6. Implementación — IMPLEMENTER

Un owner client-only nuevo, OriginalAgeStateModelPreparation; los369 archivos de
producción previos mantienen hashes exactos. Inspección acotada de jerarquía
original (máx12), resolución de los dos entrypoints a AgeableMobRenderer,
programas completos de selector/constructor/bridge, owners/descriptores/flags y
branches/join. Recursos base merged/runtime aprobados tras comparar programas
completos (sólo índices CP/padding javap distintos), digests exactos registrados.
Dos filas de predicado false/true, slots/modelClass/fingerprints iniciales,
cache por identidad y extracción UNPROVEN explícita; ninguna API de source age,
actor, binding físico o caller hot. Metadata inmutable/canonicalText/digest.

build -x runGameTest compileGametestJava runClientGameTest con selector propio:
PASS55s, cierre limpio; seis renderers nativos y24 selecciones (12 desde edad
nativa extraída,12 con field state perturbado independientemente). Oráculo de
objeto exacto en ModelFeatureRenderer diferido; fingerprint original, shared parts,
determinismo/inmutabilidad, renderer ausente y familia enum-map rechazada.
Regresión por frontera: llamadas de estructura S45 sobre12 slots originales y
hashes S51/S45 sin cambios; no repetir esos clientes/censo completos porque no
se modificó ningún helper existente. No Tiny/full servidor/default client ajenos.
Manifest normal112server/34client restaurado PASS1s, fuente/procesado idénticos.
Recibos370 hashes/JAR/recetas/programas/logs duraderos en docs/evidence/S52.
Dos reviews completos IMPLEMENTER tras GREEN producen cero cambios; campaña
independiente y replay público73s PASS; driver join-target corregido reproducido,
mutante muerto55s por motivo semántico. Snapshot inicial inspeccionado.
No promoción de autoridad de extracción ni G6/FULL.

## 7. Verificación — ADVERSARY

Revision previa: funciona, continua. El plan converge con el modelo previo al
declarar de forma explicita la relacion state-predicate->slot, extractionAuthority
UNPROVEN y ausencia de sourceKey age/actor/binding ejecutable/FULL. No repite la
inferencia fuente->state que bloqueo inicialmente S51: es otra frontera de prueba,
no una relajacion de esa cadena ni aceptacion fisica.

Inspeccion independiente de bytecode local original26.2 confirma constructor
base, slots adultModel/babyModel finales y dispatch living mas bridge entity:
condition isBaby, dos ramas, join sobre model heredado y delegado MobRenderer.
Esta lectura es investigacion de plan, no PASS productivo ni nueva certificacion
de toda subclase. I1/I2 exigen resolver entrypoints reales y probar los programas,
en vez de promover cualquier instanceof o overload similar.

Primera pasada desde requisitos/owners/origen/provenance/dispatch/identidad/
inmutabilidad/presupuestos/regresion y segunda pasada completa del plan producen
cero cambios adicionales. No defectos bloqueantes de plan. Owners/predicado/
constructor/consumo siguen candidatos UNPROVEN hasta entrega y campaña scoped.
G6 tareas2-6, FULL, pose/root/material, compat transformada y G9 no se cierran.



Aceptacion scoped final: funciona, continua. OriginalAgeStateModelPreparation
recertificado TRUSTED solo para LivingEntityRenderState.isBaby -> asignacion del
slot original exacto. extractionAuthority sigue UNPROVEN: observar extractors
nativos en los controles no certifica una fuente de edad, pose/root/material,
FULL ni G6 tareas2-6. La segunda revision integral produce cero cambios.

Campana independiente: Rabbit/Goat/Turtle originales, 12 selecciones con input
nativo y predicado perturbado; identidad del objeto consumido por submit
real/diferido y fingerprint inicial exactos. Seis slots ausentes, familia externa,
hierarquia no original, confinamiento al cliente, alias compartido, inmutabilidad,
determinismo y preservacion del estado actual/inicial comprobados. Tres programas
semanticos rechazados con motivos exactos (constructor alias, join, bridge flags).
Publico reusable: S52AdversarialStateProof y tools/s52_adversarial.init.gradle;
PASS36s limpio. Replay IMPLEMENTER PASS73s limpio, SVG byte identico.

Sensibilidad: seis mutantes semanticos muertos (autoridad, inversion de slots,
origen, constructor, flags bridge, join). No se contabilizan errores de compilador.
El primer mutante join sobrevivio por un defecto ADVERSARY_FIXTURE: el label
fabricado fallaba antes con branch_target_missing. Conservamos ese survivor y
el RED del oracle reforzado; corregimos solo el operand GOTO original preservando
frames, exigimos model_join_target, baseline PASS27s y mutante muerto. No hubo
RED productivo ni reparacion de produccion. El driver publico PS1 reproduce
sensibilidades en shadows y salidas propias; prueba del driver join PASS killed.

Coste/ownership: explicit CLIENT_PREPARATION, cero callers runtime; jerarquia
max12 y limites de recursos/programas S51 sin cambios; dos modelos como maximo,
cache por identidad dentro de la pasada y estructura S45 bounded. Resultado
inmutable con hashes/fingerprints sin retener modelos/mundo. Verificados370
hashes,369 owners previos intactos, un owner nuevo y JAR758 entradas sin tests.
No repetimos Tiny/censo/S51 completo: solo llamadas estructurales pertinentes y
sus hashes. Captura determinista de seis slots iniciales SVG/PNG inspeccionada:
proyeccion de cubos, sin claim GPU/layers/root/pose fisica.

Recibo duradero: docs/evidence/S52/adversarial-verification.json y adversarial/.
Guarda positivos, fallos de infraestructura, seis sensibilidades, replay y hashes
exactos del tooling publico. Dos pasadas finales de codigo/claims/coste/alias/
programas/origen/capturas/recibos sin cambios productivos. G9 humano al final.

## 8. Fallos y bucles

IMPLEMENTER: primer build/client focal PASS, sin RED productivo. Collector de
metadata tuvo output relativo erróneo por Path.resolve de Python314; usar
Path.cwd/paths absolutos, retirar sólo directorios vacíos creados, sin reparar
producción. ADVERSARY_FIXTURE: primeras inyecciones de label no observaban guard
join previsto; un mutante sobrevivió por branch_target_missing anterior. No
contabilizar errores de infraestructura/compilador como kill. Fixture corregido
mantiene frames/labels y parchea sólo operando GOTO; exige model_join_target.
Baseline27s y sensibilidad6/6 verdes; logs/survivor/fixture anterior preservados.
Root driver reproduce ese kill semántico con exit agregado0/native1, sin cambios
productivos ni repetir Tiny/baterías ajenas.

## 9. Revisión final

Ambos roles completan dos reviews integrales sin cambios: programas/dispatch,
origen/slot/predicado, dominio/aliases/shared state, immutability, límites/costes
PREPARATION, recursos/recibos y separación extracción/pose/material. Replay
público73s y driver join-target55s con kill semántico coinciden; visual inicial
inspeccionado y SVG idéntico.370 hashes y JAR758 preservados,369 owners intactos.
Once one-offs privados adversarios retirados por paths exactos; no one-off root
file creado, sólo directorios vacíos de collector retirados. Conservar evidencia,
logs/builds/caches y tools reusables. G6/FULL/G9 siguen abiertos.

## 10. Cierre

- [x] Frontera pertinente, oracles/bounds/sensibilidad y costes.
- [x] Ambos reviews integrales cero cambios.
- [x] Canon/evidencia/tooling/cleanup y commit Sprint:S52 verificados.
