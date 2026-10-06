# S58 - Actualizaciones compuestas en el programa original de pose

Estado: CERRADO scoped. IMPLEMENTER raiz, ADVERSARY independiente.
Base5054607f S57 cerrado; main b5e62535 ancestro, sin drift.

## 1. Tesis y scope

G6 tareas2-3/6, FR-025/026/029, NFR-019/020/028/029: extender la familia
original lineal para reconocer DUP de categoria1, usado por actualizaciones
compuestas de campos ModelPart. Se conserva la identidad del receiver, el
valor previo (inicial o ya escrito), el orden float y el consumo completo del
programa. No formulas de especie ni eleccion por nombre de clase.
SheepModel/SheepFurModel originales son referencias de aceptacion de la misma
tecnologia; no se declaran material, layers ni una familia FULL por sus nombres.
Sin actor input source, renderer/world root, flags, branches, keyframes,
bindings de especies, cambios de catalogo/protocolo/solver/mounts ni hot hooks.

## 2. Plan IMPLEMENTER

- [x] I1 S00 pertinente previo: OriginalModelPrograms/Code y control completo
  del programa, identidad de Ref/Number, stack bounds, SourcePose9, reset y
  NumericPoseProgram float/INITIAL/read-after-write. Evidencia S56/S57 candidata
  a reutilizar scoped, sin repetir fundaciones ni suites no afectadas.
- [x] I2 Admitir solo DUP con top de categoria1 (Ref o Number float sin F2D).
  Underflow, categoria2 y overflow fallan; duplicar referencia simbolica
  inmutable exacta sin crear INPUT/INITIAL nuevos ni perder alias. Todos los
  demas opcodes/despachos/guards siguen iguales. Incrementar TOOL_VERSION a
  original-linear-pose-v2: nueva aceptacion con provenance distinta; DTO format1
  y API protocol7 no cambian. No guardar una copia alternativa del compilador.
- [x] I3 Oracle propio con setupAnim real de dos modelos originales, estados
  float variados, iniciales distintos de current, reset por muestra, matrices
  completas y corners contra ModelPart originales. Preparacion inmutable,
  determinista, repetida despues de animar y aliases/foreign geometry negativos.
  Registrar captura neutral y snapshot espacial reproducible donde aporte senal.
- [x] I4 Build y GameTest compile; cliente focal S58 y S56 existentes (la
  misma familia modificada), consumer JVM con veto cliente sobre captura nueva.
  No Tiny, server/catalog/protocolo intactos ni full client ajeno. Restaurar
  manifest ordinario sin atribuirle una ejecucion. No repetir sensibilidad verde.
- [x] I5 Campana independiente posterior, sensibilidad/coste y dos reviews
  completas sin cambios; receipts raw, canon/VALIDATION y commit Sprint:S58
  antes de avanzar. G6/G7-G9 y QA humana final siguen abiertos.

## 3. Revisiones y coste previo

Primera lectura de instrucciones originales: SheepModel y SheepFurModel usan
DUP de ModelPart para head.y += un producto de campos float de estado. El resto
del programa, bridge y super pertenece a la familia S56 existente. Inspeccion
javap del JAR26.2 vigente, sin reconstruir formulas como produccion.
Segunda lectura: DUP es categoria1; Number promoted corresponde a double y no
debe admitirse por la misma instruccion. El stack existente debe aplicar su
cap antes de crecer; Ref/Number son records inmutables. Copias de simbolos no
cambian geometria ni estado original. Cambiar version del tool altera provenance
pero no formato/evaluator common; callers antiguos conservan firmas y contrato.
Dos lecturas completas del plan/dependencias/ausencia/coste no piden cambios.
S00 independiente pendiente; no produccion hasta cerrarlo.

OFFLINE/PREPARATION: DUP O(1) con stack max32, sin nodo numerico ni recurso nuevo;
compiler mantiene limites64 clases/8MiB/512 instrucciones/nodos/parts. Bound y
evaluacion common no cambian. Ningun parse/reflection/cache/modelo nuevo hot,
ninguna asignacion/TPS inferida. Nuevas capturas y renders son tooling offline.

## 4. Modelo/convergencia/S00 - ADVERSARY

Modelo clean-room post-S57 previo al plan: hash c7ea374e6198a856180f2359583086278f13d94bb02af0c500c5c8e59f42ef26.
La nueva tesis extiende solo la gramatica de PREPARATION: duplicar un simbolo
categoria1 conserva la misma identidad y el nodo ya existente, no recalcula el
receiver/INITIAL/INPUT ni reordena lecturas/escrituras. Categoria2/underflow/
overflow permanecen ausencia total; consumir todo el programa y reset inicial
siguen obligatorios. Nueva provenance v2 separa aceptacion ampliada sin cambiar
DTOformat1/protocol7. Witnesses originales certifican esa tecnologia, no especie,
actor input source, capas, world/root, material ni FULL.

S00 pertinente ejecutado: JVM fresca sobre helpers reales de produccion con24
asserts de identidad Ref/Number inmutables, float vs promoted, stack32/33 antes de
crecer, Code512/513, suffix/truncation y caps de recursos. No inicia Minecraft ni
se atribuye veto cliente: usa classpath cliente para enlazar esas clases. Dos
source shadows killed por AssertionError semantico: limite de stack y consumo
completo. Primer intento ADVER_INVOCATION omitio clientOnly jar del classpath
main, NoClassDefFoundError conservado; correccion de invocacion, sin oracle nuevo
ni fuente productiva. Evidence raw y recibo en
`docs/evidence/S58/adversarial-s00-verification.json`.

Reutilizacion independiente scoped: owners compilados original linear/Code/reset/
geometry/structure exactos entre JAR S56 y S57; byte-equivalence registrado.
Reset9, INITIAL/read-after-write y float/common se apoyan solo en la evidencia
scoped S56/S57 vigente, sin repetir controles verdes ajenos. TRUSTED esos cimientos
sin owner nuevo; categoria1 DUP/provenance/witness ampliado UNPROVEN hasta candidata
y campana posterior. No REWORK/REPLACE bloqueante observado.

Performance PREPARATION: DUP O(1) simbolico, stack cap32 antes de append, sin crear
nodo ni copiar geometria; limites actuales512instructions/nodes,64classes/8MiB y
parts siguen aplicables. Category2 consume dos slots JVM y no puede reinterpretarse
como categoria1 para DUP. Bound/evaluate common sin extension, no nuevo coste hot
ni claim zero allocation/TPS. Modelo y plan revisados completos dos veces; S00
pertinente cerrado, funciona/continua I1-I5 sin ajustes.377hashes intactos; no tests
concretos de candidata ni holdouts futuros compartidos. Regresion focal S56 es
pertinente al owner modificado; no Tiny/fullsuite ajeno. G6/G7-G9 abiertos.

## 5. Implementacion - IMPLEMENTER

I1 verificada antes de produccion: receipt S00 SHA8117fe7a64d93814060de82ef2f53e6bcf367d74e23630f5e6efc0ae89e64c25,
12 artefactos raw y377 hashes intactos. Implementacion I2 en un unico owner
PREPARATION: DUP conserva top exacto categoria1 con push bounded32; categoria2/
underflow/overflow rechazados. TOOL_VERSIONv2; common/runtime/catalog/protocol7
sin modificar. Oraculo diferencial propio y selector cliente S58/S56 preparados.

Build -x runGameTest y GameTest compile PASS58s. Cliente focal original S58/S56
PASS80s y salida limpia: nueva familia2 modelos/12muestras/84matrices/72cubos,
regresion S56 misma familia84matrices/102cubos. No formula de especie ni Tiny.
Consumer common mediante tool generico S57 reutilizado PASS4.5s: nueva captura
S58,84 matrices, engine NumericPoseEngine con lookup solo bind y veto cliente;
el prefijo S57 del log identifica el driver, no la captura ni una suite extra.
Manifest ordinario restaurado PASS2s,113server/34client, sin correrlos. Dos
lecturas completas de identity/category/stack/consumo/provenance/ausencia/coste
no requieren cambios. Produccion frozen377 hashes,376 previos byte intactos.

## 6. Verificacion y fallos - ADVERSARY

Funciona scoped. Dos pasadas completas de codigo/plan/modelo/coste no requieren
reparacion productiva. Los377 hashes actuales coinciden con el recibo IMPLEMENTER;
376 owners anteriores son byte iguales, solo cambia OriginalLinearPosePreparation.
DUP conserva el simbolo exacto Ref/float sin nuevo nodo, comprueba categoria y
cap32 antes de append. Categoria2/underflow/overflow son ausencia; provenance v2
y consumo completo mantienen su autoridad. No owner common/hot modificado.

Cliente real independiente PASS1m53s y cierre limpio: dos modelos originales,
dos modalidades de alias,20 muestras,140 matrices y120 cubos. Regresion S56
pertinente PASS140 matrices/170 cubos; nodes/writes/partes/piezas/transform,
schema y samples previos exactos; solo metadata source/version cambia.
JVM simbolica fresca PASS36 asserts y cinco mutantes semanticos killed;
mutante nativo suppress-dup killed por AssertionError, no por compilacion.
Consumer neutral fresco PASS4 programas/140 matrices con veto de clases cliente.
Reproduccion publica IMPLEMENTER nativa PASS3m23s, consumer4.6s y snapshot offline
1.4s; son tiempos totales reportados, no medicion de CPU/TPS.

Fallos conservados y clasificados: ADVER_FIXTURE en el floor absoluto heredado
de corners a Y208.38 (un ULP float), incluido intento intermedio de exigir matriz
bit igual; oracle de matrices3e-6 intacto. Tras el floor anterior, el corner
acepta solo diferencia matricial medida propagada al radio local mas un ULP float
por eje. No reparacion productiva ni aumento arbitrario de tolerancia matricial.
ADVER_TOOLING: dos anchors de mutacion y parse Groovy antes de ejecutar tests,
corregidos; no se cuentan como kills. ADVER_SNAPSHOT: viewport inicial recortado,
preservado como intento y sustituido offline por cuatro filas comparables de
piezas recentered con centros N/C explicitos. Docstring del driver common corregido
a S58/s58CommonClasspath sin cambio de comportamiento ni repetir pruebas verdes.

Recibo final y receta reusable: `docs/evidence/S58/adversarial-verification.json`
y `docs/evidence/S58/adversarial/README.md`. Seis kills semanticos validos;
raw logs, shadows y snapshots preservados. Lectura UTF-8 y AST de tooling propios
comprobadas. Snapshot SVG/PNG inspeccionado: comparacion de forma local y centros,
no world/root, renderer GPU ni pose de capa completa. JAR789 entradas sin leaks
al contrastar591 clases GameTest; no claims actor input/material/FULL ni familia
completa. G6 general/G7-G9 permanecen abiertos. Sin Tiny ni baterias ajenas.

## 7. Cierre

IMPLEMENTER verifica receipt adversarial SHA
2153f9b713d24dd604293c848c0623d76e20680b65d1d49f36fcdb9ad6bfd952:
44 artefactos raw, nueve fuentes publicas y377 hashes coincidentes. Los seis
resultados semanticos son AssertionError tras compilacion, no fallos de tooling;
no se repiten mutantes verdes. Captura publica de programas/muestras/matrices
semanticamente igual a la independiente. Replay nativo PASS3m23s limpio,140/120
y S56 pertinente140/170; engine consumer generico PASS8s, consumer publico para
matrices del snapshot PASS4.6s, ambos140/veto cliente. Snapshot offline PASS1.4s
y PNG inspeccionado: cuatro filas comparables, forma local y centros explicitos.
Manifest normal restaurado PASS1s/113server/34client;591 clases GameTest sin leak.

Root conserva intento init invalido y snapshot recortado, corregidos solo en
tooling; su verificador de recibos asumio metadata donde S00 usa hash string
(IMPLEMENTER_TOOLING), reparado y repetida solo la comparacion raw. Dos revisiones
finales completas de codigo/contratos/plan/evidencia/ausencia/coste sin cambios.
Main b5e62535 actualizado y ancestro. JAR789 entradas SHAe242f1ee...;589/592 clases
productivas byte iguales al S57, tres propias del unico compiler modificado.
Recibos/index raw exactos, commit local Sprint:S58 y HEAD/working-tree verificados
antes de avanzar. G6 general/G7-G9 y asuntos humanos al final de epica abiertos.
