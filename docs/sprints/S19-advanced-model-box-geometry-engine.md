# S19 — AdvancedModelBox GeometryEngine

Estado: **PLAN CERRADO / IMPLEMENTACIÓN PENDIENTE**.

Snapshot pre-implementación: **`b5ce86a54f8954afcb2d2f5eddff587f7a874720`**.

## Tesis

Al terminar S19, `AdvancedModelBox` será una familia `GeometryEngine` real y reutilizable, identificada en common pero preparada sólo desde tooling/cliente, sin lógica productiva por especie y con geometría estrictamente validada. La misma familia deberá exportar y reproducir la geometría original de **Grizzly Bear** y **Gazelle** de Alex's Mobs Continued **2.1.9** mediante el mismo engine, sin convertir Alex/Citadel en dependencia obligatoria del core.

S19 cubre **sólo la mitad geométrica de G3 tarea 5**. La pose Citadel/`ModelAnimator` queda deliberadamente fuera y tendrá sprint propio: cerrar geometría y pose a la vez obligaría a mezclar extracción cliente, un nuevo lenguaje/programa de pose, channels, catálogo/wire y evaluación server-safe sin una única frontera técnica.

## Requisitos incluidos

Funcionales: **FR-014, FR-015, FR-017, FR-019, FR-020, FR-021, FR-022, FR-024, FR-033, FR-034** en la parte necesaria para registrar/seleccionar una familia geométrica.

No funcionales: **NFR-001..005, NFR-012, NFR-019..025, NFR-028..031, NFR-036..039** en lo aplicable a preparación de geometría externa.

Invariantes que no pueden romperse:

- S15/S16: catálogo world-owned, preparación/reemplazo atómicos y fail-closed;
- S17: engine common server-safe con preparación client/tooling separada;
- S18: pose y geometría siguen siendo autoridades distintas;
- solver/broadphase no conocen Citadel, Alex ni clases de renderer;
- ningún cliente se convierte en fuente de autoridad física;
- el build ordinario sigue funcionando sin jars de Alex/Codx/Citadel.

## Fuera de scope

No entran en S19:

- compilar/evaluar `ModelAnimator`;
- locomoción Citadel (`walk`, `swing`, `flap`, `bob`, `faceTarget`, progress helpers);
- channels de Alex, estados Grizzly/Gazelle o bindings de criaturas concretas;
- declarar Alex's Mobs completo `FULL` o ejecutar el scanner G3.8;
- `RootTransformProvider`, lifecycle G3.9-11 o coverage VP26;
- soporte automático para versiones distintas de las que tengan evidencia exacta;
- EMF/CEM, GeckoLib o `DisplayRig`.

El `GrizzlyPose` de GameTest sigue siendo un oracle H1. No se promociona a engine de producción.

## Estado actual

1. `GeometryEngine` público ya existe como SAM `prepare(Request)` y S17 estableció el patrón common dispatcher + delegate client/tooling.
2. `BuiltInGeometryEngines` sólo registra `scalebrews:model_part`.
3. `ModelPartGeometryEngine` posee un registry acotado/determinista de fuentes y materializa árboles frescos.
4. `GeometryExtractor.alex(...)` sigue siendo un helper reflectivo legacy, no un engine de familia.
5. El helper actual ya conserva jerarquía, pivots, cubos, rotaciones y escala Citadel mediante reflexión, pero mezcla extracción con exclusiones y contiene semánticas demasiado permisivas.
6. `AnatomyFilter` ya puede incluir/excluir por `piece.id` o `piece.part`; por tanto una exclusión declarativa no necesita destruir geometría durante extracción.
7. El extractor Alex actual propaga la exclusión del padre a todos los descendientes y descarta silenciosamente cubos vacíos/degenerados. Eso no satisface FR-020/021.
8. El proof cliente existente usa el modelo Grizzly original 2.1.9, compara vertices y 80 poses, pero llama directamente a `GeometryExtractor.alex(...)` y excluye `hat`/`microphone` durante extracción.
9. `PrepareAnatomyProof.ps1` ya fija Alex's Mobs Continued **2.1.9** cuando ejecuta esa prueba opcional y registra hashes de inputs, pero la CI ordinaria no aporta esos jars.
10. No existe todavía una segunda especie real que demuestre FR-017 en la familia `AdvancedModelBox`.

## Estado objetivo

### 1. Identidad common de familia

Existirá un ID built-in estable, **`scalebrews:advanced_model_box`**, registrado en common mediante un dispatcher neutral equivalente al patrón de S17.

Common/dedicated:

- conoce el ID y el SPI `GeometryEngine`;
- no importa ni referencia `AdvancedModelBox`, Alex, Citadel/CodxLib, renderer ni otra clase client-only;
- devuelve vacío si no hay tooling delegate instalado;
- no permite que otro engine robe/reemplace el ID built-in.

### 2. Preparación client/tooling reusable

Un engine client/tooling de familia posee fuentes versionadas y acotadas. Una fuente describe como mínimo:

- `model` canónico;
- versión/dialecto exacto de la tecnología;
- supplier/factory que produzca estado original nuevo o reproducible;
- transform de modelo/renderer necesario para llevarlo al frame común.

La selección de fuente es por `GeometryEngine.Request.model`, no por `EntityType` ni clase Java de una especie. Dos modelos distintos de la misma tecnología pasan por el mismo engine.

Unknown parameters, fuente inexistente, versión/dialecto no soportado o material incompleto => preparación vacía/rechazada de forma determinista; nunca AABB global ni `automatic_top`.

### 3. Extractor `AdvancedModelBox` estricto

La autoridad de extracción deja de ser `GeometryExtractor.alex(...)` y pasa a un extractor de familia dedicado.

Debe preservar:

- jerarquía y parentage;
- IDs estables de parts/pieces dentro de la revisión;
- pivots, rotaciones locales y escalas relevantes;
- cubos separados e inflation ya materializada por el modelo original;
- huecos entre piezas;
- semántica de herencia de escala que realmente presente el dialecto fijado.

Debe validar antes de producir `ModelGeometry`:

- finitud de transforms, escalas y vertices;
- nombres/IDs válidos y no ambiguos;
- ciclos/duplicación de objeto;
- profundidad/trabajo/tamaños máximos explícitos;
- volumen estrictamente positivo de cada cubo físico;
- ausencia de alias de nombres que produzcan IDs iguales.

Un cubo degenerado/NaN/∞ no se omite: **falla la preparación**. Una jerarquía cíclica o sobredimensionada tampoco publica un prefijo parcial.

### 4. Filtros y visibilidad

La extracción física base no debe usar una lista por especie como autoridad de qué huesos son cosméticos.

- `AnatomyFilter.include/exclude` sigue siendo la selección declarativa por part/piece;
- excluir un padre por filtro no excluye implícitamente un descendiente que no coincida con el filtro;
- la visibilidad realmente estructural del modelo fuente puede marcar una pieza como no física, pero no debe inventar una regla transitiva distinta de la tecnología original;
- el proof Grizzly migra `hat`/`microphone` a filtro/metadata declarativa en vez de borrarlos de la extracción.

### 5. Versión/dialecto y compatibilidad

S19 no afirma compatibilidad genérica con cualquier Citadel presente o futuro.

La preparación valida el contrato reflectivo que usa para el dialecto fijado. Miembros requeridos ausentes, firma incompatible o semántica de escala no reconocida => `UNAVAILABLE`/rechazo de preparación, no heurística silenciosa.

La evidencia de aceptación usa **los jars exactos de Alex's Mobs Continued 2.1.9 y sus dependencias exactas**, con versiones y SHA-256 registrados. El source tree público puede informar el diseño, pero no sustituye al jar exacto de aceptación.

### 6. Proof de familia real

El mismo ID `scalebrews:advanced_model_box` prepara al menos:

- `alexsmobs:grizzly_bear`;
- `alexsmobs:gazelle`.

Para ambos, el proof cliente usa el modelo/renderer original de la versión fijada y compara geometría exportada con la geometría/render source real. No se acepta una réplica sintética como sustituto de NFR-029.

Grizzly conserva además el oracle histórico de pose como regresión, pero esa comparación no convierte la pose en alcance de S19.

## Plan convergido

- [ ] **I1** Añadir `scalebrews:advanced_model_box` a `BuiltInGeometryEngines` con dispatcher common server-safe y seam de instalación client/tooling, sin clases externas en constant pool common.
- [ ] **I2** Crear `AdvancedModelBoxGeometryEngine` client/tooling con registry de fuentes acotado, determinista, repeat-safe y sin selección por especie.
- [ ] **I3** Crear `AdvancedModelBoxGeometryExtractor` como única autoridad de extracción de la familia, con validación estricta y límites N/N+1.
- [ ] **I4** Separar exclusión declarativa de extracción: exportar la jerarquía física completa y dejar include/exclude a `AnatomyFilter`, conservando sólo metadata de visibilidad estructural justificable.
- [ ] **I5** Rechazar degenerados, no-finitos, ciclos, IDs/nombres ambiguos, jerarquías/cubos oversized y dialectos incompletos sin publicar resultado parcial.
- [ ] **I6** Hacer que el transform de modelo/renderer forme parte de la fuente reusable y no de un post-procesado Grizzly-specific.
- [ ] **I7** Reducir `GeometryExtractor.alex(...)` a adapter deprecated hacia la nueva autoridad o eliminarlo si ya no tiene consumidores; no puede conservar un segundo algoritmo.
- [ ] **I8** Migrar el proof original Grizzly 2.1.9 al engine de familia y añadir Gazelle 2.1.9 como segundo modelo real del mismo engine.
- [ ] **I9** Añadir lane reproducible externa que fija jars/versiones/SHA-256 de Alex 2.1.9 + dependencias, ejecuta client/original-model y dedicated/common sin convertir esos jars en dependencia ordinaria.
- [ ] **I10** Ejecutar ordinary sin Alex + focal common/dedicated + client original Grizzly/Gazelle + malformed/bounds/determinism holdouts.
- [ ] **I11** Segunda lectura completa hasta cero cambios productivos; actualizar sprint/VALIDATION. G3 tarea 5 permanece abierta para el sprint de pose Citadel.

## Modelo adversarial previo

### Ownership / classloading

- el ID built-in puede ser robado por un registro externo;
- el delegate puede reemplazarse después de instalación;
- common puede filtrar accidentalmente nombres de clases `AdvancedModelBox`, Alex/Citadel/CodxLib o renderer en su constant pool;
- el helper legacy puede seguir siendo un segundo extractor divergente;
- el engine puede esconder un `if grizzly`/`if gazelle` y aparentar genericidad.

### Identidad y determinismo

- dos fields apuntan al mismo `AdvancedModelBox`;
- dos siblings tienen el mismo nombre y colisionan en ID;
- el orden de fields/reflection cambia entre JVMs;
- registrar Grizzly antes/después de Gazelle cambia sources/bytes;
- suppliers reutilizan una instancia mutada por un proof anterior;
- model transform se comparte/muta por referencia;
- misma fuente exacta produce IDs/hash distintos en repeticiones.

La identidad debe proceder de la jerarquía/nombres canónicos de la tecnología, no del orden accidental de `getDeclaredFields()`.

### Geometría inválida y límites

- part/cube exactamente en N y N+1;
- profundidad N/N+1;
- ciclo directo e indirecto;
- child repetido bajo dos parents;
- cube vacío o con dimensión 0/negativa tras inflation;
- vertex/scale/pivot/rotation NaN o ∞;
- escala cero/casi cero que el código intenta «arreglar» con epsilon y termina inventando collider;
- transform singular/no finito;
- excepción a mitad de recorrido después de haber acumulado pieces.

Todo fallo debe abortar el resultado entero; no se publica un prefijo válido de una fuente inválida.

### Filtrado / descendientes

- excluir `head/hat` no puede excluir automáticamente un descendiente físico ajeno;
- incluir explícitamente una pieza no puede revivir un degenerado;
- `showModel=false` en una parte debe seguir la semántica real del dialecto, no una regla inventada por el extractor;
- filtros equivalentes por part/piece deben producir selección determinista;
- `hat`/`microphone` Grizzly deben desaparecer por filtro declarativo sin cambiar la geometría base exportada.

### Dialecto / versiones

- falta `scaleChildren`, cambia el método de transform o cambia el layout interno de cubos;
- clase con nombre terminado en `AdvancedModelBox` pero contrato incompatible;
- Alex/Citadel versión distinta de la fijada;
- modelo válido del mismo framework con estructura distinta a Grizzly;
- jar actual de source público diverge del jar 2.1.9 fijado.

No se concede compatibilidad por nombre de clase. Sólo dialectos demostrados y versionados.

### Proof real de familia

- Grizzly pasa pero Gazelle falla porque el extractor depende de fields públicos/orden/nombre de root propios del bear;
- Gazelle introduce múltiples roots o nombres privados;
- renderer transform de uno difiere del otro;
- la comparación sólo verifica AABB y oculta errores de vertices/pivots/huecos;
- el test sintético comparte el mismo bug que el extractor.

El proof de aceptación compara contra modelo/renderer original exacto y debe demostrar piezas/vertices/transforms seleccionados, no sólo que «hay cubos».

### No regresión

- ordinary sin jars externos sigue verde;
- S17 `model_part` conserva ownership/constant-pool/determinismo;
- catálogo inválido con engine/model inexistente sigue rechazándose antes del swap;
- no cambia protocolo ni schema por introducir sólo una nueva familia de GeometryEngine;
- solver no recibe imports ni branches Citadel/Alex.

## Holdouts reservados

Antes de implementación se reservarán escenarios concretos para no programar contra la lista visible. Las propiedades reservadas son:

1. **genericidad real**: segundo modelo 2.1.9 con una forma estructural distinta a Grizzly;
2. **identidad**: jerarquía equivalente construida/registrada en orden diferente;
3. **filtrado**: exclusión de parent con descendiente físico válido;
4. **fail-closed**: degenerado/no-finito después de material ya recorrido;
5. **dialecto**: objeto que satisface parte de la reflexión pero no el contrato completo;
6. **ownership**: legacy adapter no puede ejecutar un algoritmo distinto del engine canónico.

Los casos concretos se revelan después de leer la implementación. No se añadirán requisitos nuevos para «hacer más tests» una vez congelado este plan.

## Matriz mínima de evidencia

| Propiedad | Nivel | Resultado requerido |
| --- | --- | --- |
| ID built-in / ownership / no id theft | common GameTest | verde |
| constant pool common sin cliente/Alex/Citadel | common/static + dedicated | verde |
| malformed/cycles/degenerate/N/N+1 | common/client fixture según seam | rechazo exacto |
| determinismo y orden de registro | client/tooling | bytes/ModelGeometry equivalentes |
| Grizzly 2.1.9 engine geometry vs original | real client | paridad |
| Gazelle 2.1.9 mismo engine vs original | real client | paridad |
| filtro parent/child y hat/microphone | GameTest/client | selección declarativa correcta |
| build ordinario sin jars externos | ordinary CI | suite completa verde |
| catálogo/binding fail-closed | common/dedicated | conserva snapshot aceptado |
| revisión final | diff/revisión manual adversarial | cero cambio productivo |

## Criterio de cierre

S19 sólo cierra si:

1. `AdvancedModelBox` es un `GeometryEngine` de familia real con ID common y preparación client/tooling;
2. no existe lógica productiva Grizzly/Gazelle ni segundo algoritmo legacy;
3. el extractor conserva geometría/hierarquía relevante y rechaza inválidos sin resultado parcial;
4. filtros nombrados son declarativos y no destruyen descendientes por accidente;
5. Grizzly **y Gazelle** 2.1.9 usan el mismo engine y pasan comparación contra modelo/renderer original exacto;
6. common/dedicated no cargan clases cliente/Alex/Citadel y el build ordinario no necesita esos jars;
7. inputs externos de aceptación quedan versionados/hasheados/reproducibles;
8. ordinary + focal + real-client externo están verdes;
9. una segunda pasada adversarial produce cero cambios de producción.

Cerrar S19 **no** marca G3 tarea 5 como `[x]`. Esa tarea sólo cerrará cuando la pose Citadel reusable quede implementada y demostrada en un sprint posterior.
