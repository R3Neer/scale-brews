# Flujo de trabajo por sprints para Entity Collisions

Estado: **protocolo operativo** para implementar el subsistema de colisiones entre entidades. Este documento define **cómo se trabaja**, no qué debe hacer el sistema, cómo se diseña su arquitectura ni qué tareas globales quedan abiertas.

Fuentes canónicas relacionadas:

- `ENTITY_COLLISIONS_REQUIREMENTS.md`: requisitos normativos y criterios de aceptación.
- `ENTITY_COLLISIONS.md`: arquitectura, ownership, API y modelo de datos.
- `ENTITY_COLLISIONS_PLAN.md`: gates, dependencias y estado global de implementación.
- `VALIDATION.md`: evidencia realmente ejecutada.

Los documentos de sprint son registros de ejecución. **No pueden redefinir requisitos, arquitectura ni estado global en conflicto con las fuentes anteriores.**

---

## 1. Principio general

La implementación se divide en sprints pequeños, coherentes y cerrados por dependencias. Cada sprint debe tener **una tesis técnica principal** y cubrir sólo los requisitos que tenga sentido resolver juntos.

Ejemplos de tesis válidas:

- desacoplar la API pública de su backend interno;
- introducir el binding canónico y sus codecs;
- integrar exactamente una clase de evento material en el pipeline real;
- cerrar una frontera causal concreta;
- implementar un GeometryEngine reusable;
- completar una transición concreta de lifecycle.

Ejemplos de sprints demasiado grandes:

- «hacer G1»;
- «terminar networking y prediction»;
- «soportar todos los mobs»;
- cualquier sprint que necesite cambiar simultáneamente API, catálogo, solver, cliente, red y compatibilidad sin una única razón técnica que los una.

La prioridad no es maximizar líneas de código por sprint, sino minimizar el número de supuestos simultáneos que pueden estar equivocados.

---

## 2. Ciclo obligatorio de un sprint

Todo sprint sigue este ciclo:

```text
SELECCIÓN DE SCOPE
      ↓
INVESTIGACIÓN / ESTADO ACTUAL
      ↓
PLAN DE IMPLEMENTACIÓN ↺
      ↓
MODELO ADVERSARIAL ↺
      ↓
IMPLEMENTACIÓN ↺
      ↓
DISEÑO + EJECUCIÓN DE TESTS ADVERSARIALES
      ↓
¿FALLO?
 ├─ sí → CLASIFICAR FALLO → volver a la fase correcta
 └─ no
      ↓
REVISIÓN FINAL ↺
      ↓
PASADA COMPLETA SIN CAMBIOS
      ↓
EVIDENCIA + DOCUMENTACIÓN CANÓNICA
      ↓
COMMIT DEL SPRINT
```

`↺` significa que la fase se revisa después de completarse. Si la revisión produce cambios, se aplican y **la revisión de esa fase vuelve a empezar desde el principio**. Una fase converge únicamente cuando una pasada completa no produce cambios.

Un test verde no permite saltarse la revisión. Una revisión satisfactoria tampoco sustituye a los tests requeridos.

---

## 3. Fase 1 — selección del sprint

Antes de modificar código se define el scope.

El sprint debe identificar:

- requisito(s) funcional(es) incluidos;
- requisito(s) no funcional(es) incluidos;
- criterio(s) de aceptación aplicables;
- gate del plan al que contribuye;
- dependencias que deben estar satisfechas;
- invariantes de otros requisitos que no pueden romperse;
- exclusiones explícitas: trabajo relacionado que **no** entra en el sprint.

### 3.1 Regla de cohesión

Los requisitos se agrupan porque comparten una frontera técnica o una dependencia, no porque estén cerca en el documento.

Si durante el análisis aparece un requisito adicional que es **necesario** para cerrar correctamente el sprint, se incorpora al scope **antes de implementar** y se vuelve a revisar el scope.

Si aparece trabajo útil pero no necesario, se registra en el plan canónico si procede y queda fuera del sprint.

### 3.2 Condición para empezar

No se empieza implementación hasta poder expresar el sprint en una frase del tipo:

> «Al terminar este sprint, X será cierto y podrá demostrarse mediante Y, sin asumir Z.»

Si esa frase necesita varias conjunciones independientes, probablemente el sprint es demasiado grande.

---

## 4. Fase 2 — investigación y estado actual

Antes del plan se reconstruye el estado real del código afectado.

Se deben leer, según corresponda:

- requisitos incluidos y requisitos vecinos que puedan ser invariantes;
- arquitectura relevante;
- gate y dependencias del plan;
- clases de producción afectadas;
- tests existentes relevantes;
- mixins/hooks/recursos/network payloads consumidores;
- evidencia previa en `VALIDATION.md`;
- fuentes de Minecraft/Fabric/mods externos cuando la API o el comportamiento sean version-sensitive.

El resultado de esta fase debe distinguir explícitamente:

- **estado actual**;
- **estado objetivo del sprint**;
- deuda o limitaciones que permanecen fuera del sprint;
- código legacy que todavía conserva un requisito vigente;
- código sin consumidor/requisito que puede ser candidato a eliminación;
- supuestos que la implementación no debe convertir en invariantes accidentales.

No se modifica arquitectura normativa desde esta fase sin actualizar primero la fuente canónica correspondiente.

---

## 5. Fase 3 — plan de implementación iterativo

El plan del sprint se escribe antes de implementar y utiliza el método iterativo.

### 5.1 Pasadas mínimas de revisión

El plan se revisa como mínimo contra:

1. requisitos y criterios de aceptación;
2. arquitectura y ownership;
3. código real y consumidores;
4. dependencias y orden de migración;
5. estados de error/fail-closed;
6. compatibilidad/regresiones;
7. simplicidad: eliminar pasos o abstracciones que no sean necesarios;
8. verificabilidad: cada tarea debe tener una condición objetiva de finalización.

Si una pasada cambia el plan, las revisiones vuelven a empezar sobre la nueva versión.

La fase termina sólo cuando una pasada completa no cambia nada.

### 5.2 Checklist de implementación

Cada tarea se registra como checklist y debe ser concreta.

Correcto:

```text
[ ] I1 Crear interfaz RootTransformProvider sin dependencia cliente.
[ ] I2 Hacer que el binding valide el provider antes de aceptar revisión.
[ ] I3 Migrar el único caller actual al registry nuevo.
[ ] I4 Eliminar el registro hardcodeado sustituido.
```

Incorrecto:

```text
[ ] Mejorar arquitectura.
[ ] Arreglar networking.
[ ] Hacerlo robusto.
```

Una tarea marcada `[x]` significa únicamente **implementada**, no «requisito demostrado».

---

## 6. Fase 4 — modelo adversarial previo

Antes de implementar se crea, en el mismo documento de sprint pero en una sección separada, un **modelo adversarial**.

Todavía no se diseñan tests concretos. Se diseña cómo podría estar equivocada la futura implementación.

La actitud requerida es deliberadamente hostil: intentar descubrir la suposición más cómoda, plausible y errónea que podría introducir el implementador.

### 6.1 Técnicas mínimas a considerar

Para cada requisito o propiedad aplicable se estudian:

- **clases de equivalencia** válidas, inválidas y parcialmente válidas;
- **análisis de valores frontera**, incluyendo exactamente el límite y ambos lados;
- **tablas de decisión** cuando varias condiciones interactúan;
- **transiciones de estado** y transiciones ilegales;
- **combinatoria/pairwise** para variables que puedan interactuar;
- **secuencias y permutaciones de orden**;
- **duplicación, pérdida, replay y datos atrasados**;
- **identity reuse**, epoch/revision/generation equivocadas;
- **null/ausencia**, referencias inexistentes y recursos retirados a mitad de lifecycle;
- **datos corruptos, contradictorios, NaN/∞, degenerados o extremos**;
- **reentrancia y ciclos**;
- **límites de presupuesto**, tamaño y profundidad;
- **metamorphic properties**: cambios que no deberían alterar el resultado;
- **propiedades algebraicas/geométricas** e invariantes;
- **differential/reference checks** cuando exista implementación o modelo de referencia;
- **fault injection** en puntos de transición;
- **fuzzing estructurado** cuando el input tenga un dominio suficientemente amplio;
- **mutation-testing mindset**: qué pequeño bug debería hacer fallar la futura suite.

No todas las técnicas deben producir un ataque en todos los sprints. Sí deben considerarse y descartarse conscientemente cuando no apliquen.

### 6.2 Preguntas adversariales obligatorias

Como mínimo se intenta responder:

- ¿Qué pasa justo antes, exactamente en y justo después de cada frontera?
- ¿Qué ocurre si recibimos la misma operación dos veces?
- ¿Qué ocurre si falta una operación intermedia?
- ¿Qué ocurre si cambia la identidad pero los IDs aparentes se reutilizan?
- ¿Qué ocurre si un estado válido deja de serlo entre dos pasos?
- ¿Qué ocurre si dos cambios legítimos suceden en el mismo tick?
- ¿Qué supuesto de orden está haciendo probablemente el código?
- ¿Qué supuesto de «esto nunca será null/vacío/huérfano» estamos tentados de hacer?
- ¿Qué ocurre bajo SCALE extremo o geometría casi degenerada?
- ¿Qué resultado debería permanecer igual si cambio orden de registro, frame global o representación equivalente?
- ¿Qué código legacy podría seguir actuando en paralelo sin que nos demos cuenta?

### 6.3 Holdout adversarial

Una parte pequeña de las ideas adversariales debe reservarse como **holdout** para la revisión posterior a la implementación.

El plan puede conocer la propiedad general que debe respetar, pero no todos los escenarios concretos con los que se intentará romperla después. Esto reduce el riesgo de implementar únicamente para la colección de tests prevista.

---

## 7. Fase 5 — implementación iterativa

La implementación sigue exclusivamente el plan convergido.

Se recomienda trabajar en unidades pequeñas del checklist. Después de cada unidad se revisan:

- invariantes afectados;
- ownership;
- callers/consumidores;
- error handling y fail-closed;
- código anterior que ahora haya quedado muerto;
- necesidad real de la abstracción añadida.

### 7.1 Cambio del plan durante implementación

Si aparece información que invalida o amplía materialmente el plan:

1. se detiene esa línea de implementación;
2. se vuelve a la fase de planificación;
3. se modifica el plan;
4. se repiten sus revisiones hasta converger;
5. se actualiza el modelo adversarial si el cambio introduce nuevos riesgos;
6. se continúa implementando.

No se convierte una improvisación descubierta en mitad del código en «arquitectura» por el hecho de que compile.

### 7.2 Revisión de implementación

Al acabar las tareas se revisa el diff completo contra el plan y los requisitos.

Si la revisión produce cambios, se aplican y se vuelve a revisar desde el principio. La fase converge cuando una pasada completa de implementación no produce cambios.

---

## 8. Fase 6 — diseño adversarial de tests

Sólo después de disponer de implementación se convierte el modelo adversarial en tests concretos.

Los tests deben buscar fallos, no justificar la forma concreta del código.

Se priorizan propiedades y observables públicos frente a detalles internos. Un refactor correcto no debería obligar a reescribir tests que realmente expresan requisitos.

### 8.1 Matriz de tests

El sprint mantiene una matriz que relacione:

```text
Ataque / propiedad → requisito → nivel de test → resultado
```

Niveles posibles, según el requisito:

- unit/kernel;
- GameTest;
- client/integrated server;
- dedicated server;
- consumer/compat fixture;
- network/latency;
- benchmark;
- QA humana.

Un test de nivel inferior no sustituye a otro exigido por el criterio de aceptación.

### 8.2 Segunda pasada adversarial

Después de leer el código implementado se realiza una segunda pasada adversarial independiente.

Ahora se buscan específicamente:

- ramas que parecen no alcanzadas;
- caches y estado mutable;
- orden implícito de colecciones;
- equality/identity dudosas;
- límites `<=`/`<`;
- conversiones de frame/coordenadas;
- fallback involuntario;
- comportamiento distinto entre cliente y servidor;
- lógica repetida;
- errores que los tests iniciales podrían compartir con la implementación.

En esta fase se revelan y convierten en tests los ataques holdout reservados anteriormente.

### 8.3 Mutation testing

Cuando sea razonable, se utiliza mutation testing automático o una aproximación manual dirigida para comprobar la **calidad de la suite**.

Ejemplos de mutaciones que deberían ser detectadas:

- invertir una comparación de frontera;
- omitir una comprobación de epoch/revision;
- aplicar dos veces un delta;
- aceptar `UNAVAILABLE` como geometría válida;
- eliminar una invalidación de cache;
- invertir una normal;
- ignorar una pieza en una agregación;
- sustituir una colección determinista por orden arbitrario.

Si una mutación relevante sobrevive, falta cobertura aunque todos los tests existentes estén verdes.

---

## 9. Fase 7 — clasificación y bucle de fallos

Todo fallo se clasifica antes de corregirse.

Categorías:

1. **bug de implementación**;
2. **defecto del plan**;
3. **requisito ambiguo, incompleto o incorrecto**;
4. **arquitectura incorrecta**;
5. **test incorrecto**;
6. **problema de entorno/evidencia**.

La categoría determina a qué fase se vuelve.

```text
bug de implementación ───────────────→ implementación
plan defectuoso ─────────────────────→ planificación
requisito defectuoso ────────────────→ requisitos → plan
arquitectura defectuosa ─────────────→ arquitectura → plan
 test incorrecto ────────────────────→ diseño de tests
problema de entorno/evidencia ───────→ corregir entorno y repetir evidencia
```

Después de cualquier corrección se repite **toda la batería afectada**, no sólo el test que falló.

No se permite «parchear hasta verde» sin decidir primero qué estaba mal.

---

## 10. Fase 8 — revisión final iterativa del sprint

Con todos los tests verdes se hace una revisión distinta de la suite.

Debe contrastarse:

- requisitos incluidos y criterios de aceptación;
- requisitos vecinos que debían preservarse;
- plan del sprint;
- diff completo;
- arquitectura y ownership;
- tests y modelo adversarial;
- lifecycle/error paths;
- API/data compatibility;
- código muerto o duplicado creado/dejado por la migración;
- imports/dependencias entre paquetes;
- documentación canónica;
- observabilidad/diagnósticos;
- rendimiento evidente/hot paths si aplica;
- ausencia de fallback o segundo motor no permitido.

Si se modifica cualquier cosa, se vuelve a la fase de implementación y se repite desde allí el ciclo necesario, incluidos los tests afectados.

La revisión final sólo converge cuando **una pasada completa no produce ningún cambio**.

---

## 11. Checklists del sprint

No se mezclan estados distintos bajo una sola lista.

Cada sprint mantiene al menos cuatro checklists independientes.

### 11.1 Requisitos

```text
[ ] FR-xxx — criterio de aceptación aún no demostrado
[x] NFR-yyy — evidencia suficiente registrada
```

`[x]` significa que el criterio de aceptación exigido por la fuente normativa está satisfecho con evidencia adecuada.

### 11.2 Implementación

```text
[x] I1 ...
[x] I2 ...
[ ] I3 ...
```

`[x]` significa únicamente que la tarea de código/datos está realizada.

### 11.3 Adversarial y tests

```text
[x] A1 — hipótesis adversarial cubierta
[x] A2 — holdout ejecutado
[x] T1 — test implementado
[x] T1 — resultado PASS
[ ] T2 — resultado pendiente
```

Los fallos no se borran del historial del sprint; se registra su clasificación y la fase a la que obligaron a volver.

### 11.4 Cierre

```text
[ ] suite existente relevante
[ ] suite nueva
[ ] niveles de test exigidos por requisitos
[ ] build limpio
[ ] documentación canónica actualizada
[ ] código muerto/duplicado revisado
[ ] VALIDATION actualizado con evidencia ejecutada
[ ] pasada final completa sin cambios
[ ] commit único del sprint
```

---

## 12. Documento de cada sprint

Los sprints viven bajo:

```text
docs/sprints/
  S01-<slug>.md
  S02-<slug>.md
  ...
```

Cada archivo contiene:

```markdown
# Sxx — Título

## 1. Scope
### Incluido
### Excluido
### Requisitos
### Invariantes

## 2. Estado inicial e investigación

## 3. Plan de implementación
### Checklist
### Historial de revisiones hasta convergencia

## 4. Modelo adversarial previo
### Clases de equivalencia y fronteras
### Estados/secuencias/combinatoria
### Fault/lifecycle/network attacks
### Propiedades metamórficas/diferenciales
### Holdout reservado

## 5. Implementación
### Checklist
### Cambios de plan durante implementación

## 6. Test matrix
### Tests creados
### Segunda pasada adversarial
### Mutation checks
### Resultados

## 7. Fallos encontrados y bucles

## 8. Revisión final
### Pasadas
### Convergencia

## 9. Cierre
### Checklist de requisitos
### Evidencia
### Commit
```

El documento de sprint explica **qué ocurrió en ese sprint**. No mantiene una copia paralela del roadmap. Al cerrarlo, `ENTITY_COLLISIONS_PLAN.md` se actualiza sólo con el estado global que haya cambiado y `VALIDATION.md` sólo con evidencia realmente ejecutada.

---

## 13. Política de commits

La unidad lógica de integración es el sprint.

Reglas:

- cada sprint cerrado produce **un commit lógico**;
- no se mezclan dos sprints en un commit;
- no se declara cerrado un sprint con tests rojos o revisión pendiente;
- el commit contiene implementación, tests y las actualizaciones documentales necesarias del sprint;
- no se toca otra rama salvo autorización explícita de la tarea actual;
- no se force-pushea, reescribe historia, crea tag/release ni publica/instala artefactos por inferencia.

Si la herramienta utilizada obliga técnicamente a generar commits intermedios y no permite squash seguro sin reescritura, esos commits se consideran infraestructura/transporte y **no autorizan avanzar al siguiente sprint**. La preferencia es preparar el cambio atómicamente y producir un único commit cuando sea posible.

---

## 14. Definition of Done de un sprint

Un sprint está terminado únicamente cuando se cumplen **todas** estas condiciones:

1. el scope final está explícito y no ha quedado ningún requisito implícito;
2. el plan de implementación convergió mediante una pasada completa sin cambios;
3. el modelo adversarial previo fue revisado y convergió;
4. todas las tareas de implementación están completas;
5. cualquier cambio material descubierto durante implementación volvió por planificación cuando correspondía;
6. todos los ataques adversariales aplicables están cubiertos o tienen una justificación explícita;
7. se realizó la segunda pasada adversarial sobre el código real;
8. los holdouts fueron revelados y ejecutados;
9. mutation testing o su equivalente dirigido no deja sobrevivir mutaciones relevantes conocidas;
10. pasan los tests nuevos y toda la regresión relevante;
11. pasan los niveles de aceptación exigidos por los requisitos incluidos;
12. ningún fallo se resolvió sin clasificar y repetir el ciclo afectado;
13. requisitos, arquitectura, plan y evidencia siguen consistentes y sin fuentes de verdad duplicadas;
14. se revisó y eliminó código muerto/duplicado que el sprint haya dejado obsoleto;
15. la revisión final completa produjo **cero cambios**;
16. `VALIDATION.md` registra únicamente la evidencia realmente ejecutada;
17. el estado global correspondiente se actualizó en `ENTITY_COLLISIONS_PLAN.md`;
18. el sprint queda asociado a su commit lógico.

Si falla una de estas condiciones, el sprint no está terminado aunque compile.

---

## 15. Cómo iniciar un nuevo chat de implementación

Un agente que retome Entity Collisions debe:

1. leer `AGENTS.md`;
2. leer, en este orden:
   - `docs/ENTITY_COLLISIONS_REQUIREMENTS.md`;
   - `docs/ENTITY_COLLISIONS.md`;
   - `docs/ENTITY_COLLISIONS_PLAN.md`;
   - `docs/ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`;
   - `docs/VALIDATION.md`;
3. comprobar el HEAD de la rama autorizada y el estado del primer gate abierto;
4. no asumir que evidencia histórica sigue siendo válida para el HEAD actual;
5. seleccionar el sprint más pequeño que cierre una frontera coherente del primer trabajo abierto;
6. crear su archivo `docs/sprints/Sxx-<slug>.md`;
7. ejecutar íntegramente este protocolo antes de pasar al sprint siguiente.

La primera acción de un chat nuevo no es implementar. Es reconstruir el contexto suficiente para seleccionar correctamente el siguiente sprint.
