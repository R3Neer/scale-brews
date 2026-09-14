# Flujo de trabajo por sprints para Entity Collisions

Estado: **protocolo operativo** para implementar y validar el subsistema de colisiones entre entidades. Este documento define **cómo se trabaja**, no qué debe hacer el sistema, cómo se diseña su arquitectura ni qué tareas globales quedan abiertas.

Fuentes canónicas relacionadas:

- `ENTITY_COLLISIONS_REQUIREMENTS.md`: requisitos normativos y criterios de aceptación.
- `ENTITY_COLLISIONS.md`: arquitectura, ownership, API y modelo de datos.
- `ENTITY_COLLISIONS_PLAN.md`: gates, dependencias y estado global de implementación.
- `VALIDATION.md`: evidencia realmente ejecutada.

Los documentos de sprint son registros de ejecución. **No pueden redefinir requisitos, arquitectura ni estado global en conflicto con las fuentes anteriores.**

---

## 1. Principio general

El trabajo se divide en sprints pequeños, coherentes y cerrados por dependencias. Cada sprint debe tener **una tesis técnica principal** y cubrir sólo los requisitos que tenga sentido resolver juntos.

Ejemplos de tesis válidas:

- desacoplar la API pública de su backend interno;
- introducir el binding canónico y sus codecs;
- integrar exactamente una clase de evento material en el pipeline real;
- cerrar una frontera causal concreta;
- implementar un `GeometryEngine` reusable;
- completar una transición concreta de lifecycle.

Ejemplos de sprints demasiado grandes:

- «hacer G1»;
- «terminar networking y prediction»;
- «soportar todos los mobs»;
- cualquier sprint que necesite cambiar simultáneamente API, catálogo, solver, cliente, red y compatibilidad sin una única razón técnica que los una.

La prioridad no es maximizar líneas de código por sprint, sino minimizar el número de supuestos simultáneos que pueden estar equivocados.

### 1.1 Roles operativos obligatorios

Antes de modificar código o tests debe declararse el rol activo del agente: **IMPLEMENTADOR** o **ADVERSARIO**. Ambos pueden trabajar en el mismo sprint y rama, incluso de forma intercalada, pero **no pueden cambiar silenciosamente de rol**.

#### IMPLEMENTADOR

El implementador:

- convierte el plan convergido en cambios de producción;
- modifica código, datos, integración y wiring necesarios para satisfacer el sprint;
- puede crear tests, diagnostics y tooling de desarrollo que necesite para implementar con seguridad;
- corrige bugs de producción una vez que un fallo haya sido clasificado como bug de implementación;
- no diseña la producción con conocimiento de escenarios holdout que todavía no hayan sido revelados;
- no rebaja ni reescribe un oracle adversarial correcto sólo para obtener verde.

#### ADVERSARIO

El adversario:

- construye y revisa el modelo adversarial;
- reserva y revela holdouts;
- revisa el código implementado de forma independiente;
- diseña y ejecuta tests adversariales, mutation/fault checks, fixtures, probes y evidencia diferencial;
- puede añadir o modificar **código de testing y tooling de testing**, incluso herramientas complejas o programas paralelos, cuando un test convencional no permita observar bien la propiedad;
- puede crear harnesses, visualizadores, replay/capture drivers, fuzzers, fixture generators, comparadores, inspectores de protocolo, renderers de depuración y workflows temporales;
- **no corrige producción** cuando descubre un defecto, salvo que la tarea actual le reasigne explícitamente el rol de implementador. Su responsabilidad es hacer el defecto observable, reproducible y correctamente clasificado;
- puede dejar un holdout rojo de forma deliberada si demuestra un bug real pendiente del implementador.

La libertad del adversario para programar tooling no convierte ese tooling en producción. Una herramienta temporal se retira después de capturar evidencia durable. Una herramienta reusable puede permanecer bajo una superficie de test/tooling si reduce coste de verificación futura y no crea una segunda implementación productiva.

#### Responsabilidad compartida

Ambos roles pueden actualizar el documento del sprint, pero deben mantener separadas las secciones de implementación y adversarial. `VALIDATION.md` sólo registra ejecuciones que realmente ocurrieron, independientemente de quién las ejecutó.

---

## 2. Ciclo obligatorio de un sprint

Todo sprint sigue este ciclo:

```text
SELECCIÓN DE SCOPE                           [COMPARTIDO]
      ↓
INVESTIGACIÓN / ESTADO ACTUAL               [COMPARTIDO]
      ↓
PLAN DE IMPLEMENTACIÓN ↺                    [IMPLEMENTADOR]
      ↓
MODELO ADVERSARIAL PREVIO ↺                 [ADVERSARIO]
      ↓
IMPLEMENTACIÓN ↺                            [IMPLEMENTADOR]
      ↓
REVISIÓN + TESTS + TOOLING ADVERSARIAL      [ADVERSARIO]
      ↓
¿FALLO?
 ├─ sí → CLASIFICAR FALLO
 │        ├─ bug producción → IMPLEMENTADOR
 │        ├─ plan/requisito/arquitectura → fase canónica correspondiente
 │        └─ test/evidencia → ADVERSARIO
 └─ no
      ↓
REVISIÓN FINAL ↺                            [AMBOS, adversario independiente]
      ↓
PASADA COMPLETA SIN CAMBIOS
      ↓
EVIDENCIA + DOCUMENTACIÓN CANÓNICA
      ↓
COMMIT LÓGICO DEL SPRINT
```

`↺` significa que la fase se revisa después de completarse. Si la revisión produce cambios, se aplican y **la revisión de esa fase vuelve a empezar desde el principio**. Una fase converge únicamente cuando una pasada completa no produce cambios.

Un test verde no permite saltarse la revisión. Una revisión satisfactoria tampoco sustituye a los niveles de prueba requeridos.

---

## 3. Selección del sprint e investigación

Antes de modificar producción se define el scope y se reconstruye el estado real.

El sprint debe identificar:

- requisitos funcionales y no funcionales incluidos;
- criterios de aceptación aplicables;
- gate del plan al que contribuye;
- dependencias que deben estar satisfechas;
- invariantes vecinos que no pueden romperse;
- exclusiones explícitas;
- deuda o limitaciones que permanecen fuera del sprint;
- código legacy que todavía conserva un requisito vigente;
- código sin consumidor/requisito candidato a eliminación;
- supuestos que no deben convertirse en invariantes accidentales.

Se leen, según corresponda, requisitos, arquitectura, gate, producción afectada, tests relevantes, mixins/hooks/recursos/payloads consumidores, evidencia previa y fuentes version-sensitive de Minecraft/Fabric/mods externos.

### 3.1 Regla de cohesión

Los requisitos se agrupan porque comparten una frontera técnica o una dependencia, no porque estén cerca en el documento.

Si aparece un requisito adicional **necesario** para cerrar el sprint, se incorpora al scope antes de implementar y se vuelve a revisar. Trabajo útil pero no necesario queda fuera y se registra en el plan canónico si procede.

### 3.2 Condición para empezar

No se empieza implementación hasta poder expresar el sprint así:

> «Al terminar este sprint, X será cierto y podrá demostrarse mediante Y, sin asumir Z.»

Si la frase necesita varias conjunciones independientes, probablemente el sprint es demasiado grande.

---

## 4. Plan de implementación iterativo — IMPLEMENTADOR

El plan se escribe antes de implementar y se revisa como mínimo contra:

1. requisitos y criterios de aceptación;
2. arquitectura y ownership;
3. código real y consumidores;
4. dependencias y orden de migración;
5. estados de error/fail-closed;
6. compatibilidad/regresiones;
7. simplicidad;
8. verificabilidad objetiva de cada tarea.

Si una pasada cambia el plan, las revisiones vuelven a empezar. La fase termina sólo cuando una pasada completa no cambia nada.

Cada tarea se registra como checklist concreta:

```text
[ ] I1 Crear interfaz RootTransformProvider sin dependencia cliente.
[ ] I2 Hacer que el binding valide el provider antes de aceptar revisión.
[ ] I3 Migrar el único caller actual al registry nuevo.
[ ] I4 Eliminar el registro hardcodeado sustituido.
```

Una tarea `[x]` significa **implementada**, no «requisito demostrado».

---

## 5. Modelo adversarial previo — ADVERSARIO

Antes de implementar se crea, en una sección separada del sprint, un **modelo adversarial**. Todavía no se diseña la batería concreta de tests; se diseña cómo podría estar equivocada la futura implementación.

La actitud requerida es deliberadamente hostil: buscar la suposición más cómoda, plausible y errónea que podría introducir el implementador.

### 5.1 Técnicas mínimas a considerar

- clases de equivalencia válidas, inválidas y parcialmente válidas;
- valores frontera: justo antes, exactamente en y justo después;
- tablas de decisión;
- transiciones de estado e ilegales;
- combinatoria/pairwise;
- secuencias y permutaciones de orden;
- duplicación, pérdida, replay y datos atrasados;
- identity reuse, epoch/revision/generation equivocadas;
- null/ausencia y recursos retirados a mitad de lifecycle;
- datos corruptos, contradictorios, NaN/∞, degenerados o extremos;
- reentrancia y ciclos;
- límites de presupuesto, tamaño y profundidad;
- propiedades metamórficas;
- propiedades algebraicas/geométricas;
- differential/reference checks;
- fault injection;
- fuzzing estructurado;
- mutation-testing mindset;
- riesgo **visual/espacial/temporal** que una aserción numérica pueda no revelar.

No todas las técnicas deben producir un ataque en todos los sprints. Sí deben considerarse y descartarse conscientemente cuando no apliquen.

### 5.2 Preguntas adversariales obligatorias

Como mínimo:

- ¿Qué pasa justo antes, exactamente en y justo después de cada frontera?
- ¿Qué ocurre si recibimos la misma operación dos veces?
- ¿Qué ocurre si falta una operación intermedia?
- ¿Qué ocurre si cambia la identidad pero se reutilizan IDs aparentes?
- ¿Qué ocurre si un estado válido deja de serlo entre dos pasos?
- ¿Qué ocurre si dos cambios legítimos suceden en el mismo tick?
- ¿Qué supuesto de orden está haciendo probablemente el código?
- ¿Qué supuesto de «esto nunca será null/vacío/huérfano» estamos tentados de hacer?
- ¿Qué ocurre bajo SCALE extremo o geometría casi degenerada?
- ¿Qué debería permanecer igual si cambia orden de registro, frame global o representación equivalente?
- ¿Qué código legacy podría seguir actuando en paralelo?
- ¿Puede algo cumplir los números y aun así verse físicamente desalineado, invertido, saltar de frame o deformarse visualmente?

### 5.3 Holdout adversarial

Una parte pequeña de las ideas se reserva como **holdout** para después de leer la implementación.

El plan puede conocer la propiedad general, pero no todos los escenarios concretos. Esto reduce el riesgo de implementar sólo para la colección visible de tests.

No se añaden requisitos nuevos después del freeze únicamente para fabricar más tests. Un holdout debe derivar de una propiedad ya exigida.

---

## 6. Implementación iterativa — IMPLEMENTADOR

La implementación sigue exclusivamente el plan convergido.

Después de cada unidad se revisan invariantes, ownership, callers, error handling/fail-closed, código muerto y necesidad real de nuevas abstracciones.

Si aparece información que invalida o amplía materialmente el plan:

1. se detiene esa línea de implementación;
2. se vuelve a planificación;
3. se modifica el plan;
4. se revisa hasta converger;
5. el adversario actualiza su modelo si cambian los riesgos;
6. se continúa implementando.

Al acabar, el implementador revisa el diff completo contra plan y requisitos. Si produce cambios, vuelve a revisar desde el principio hasta una pasada sin cambios.

---

## 7. Verificación adversarial — ADVERSARIO

Sólo después de existir superficie productiva suficiente se convierte el modelo adversarial en tests, herramientas y evidencia concretos. El adversario debe buscar fallos, no justificar la forma del código.

Se priorizan propiedades y observables públicos frente a detalles internos. Un refactor correcto no debería obligar a reescribir tests que realmente expresan requisitos.

### 7.1 Matriz de verificación

Cada ataque se relaciona con requisito, nivel y resultado:

```text
Ataque / propiedad → requisito → nivel de prueba → resultado
```

Niveles posibles:

- unit/kernel;
- GameTest;
- client/integrated server;
- dedicated server;
- consumer/compat fixture;
- network/latency;
- benchmark;
- snapshot visual/differential capture;
- QA humana.

Un nivel inferior no sustituye a otro exigido por el criterio de aceptación.

### 7.2 Segunda pasada adversarial sobre el código real

Después de leer la implementación se buscan específicamente:

- ramas no alcanzadas;
- caches y estado mutable;
- orden implícito de colecciones;
- equality/identity dudosas;
- límites `<=`/`<`;
- conversiones de frame/coordenadas;
- fallback involuntario;
- divergencia cliente/servidor;
- lógica repetida;
- errores que los tests iniciales podrían compartir con la implementación.

Aquí se revelan los holdouts reservados.

### 7.3 Mutation testing

Cuando sea razonable, se usa mutation testing automático o manual dirigido. Ejemplos que deberían ser detectados:

- invertir una comparación de frontera;
- omitir epoch/revision;
- aplicar dos veces un delta;
- aceptar `UNAVAILABLE` como válido;
- eliminar una invalidación de cache;
- invertir una normal;
- ignorar una pieza;
- sustituir orden determinista por arbitrario.

Si una mutación relevante sobrevive, falta cobertura aunque la suite esté verde.

### 7.4 Tooling experimental y programas paralelos

El adversario **no está limitado a escribir tests tradicionales**. Si la propiedad requiere observabilidad más rica, puede implementar tooling específico de pruebas, incluso programas paralelos temporales.

Usos apropiados incluyen:

- cargar una versión externa fijada y comparar su comportamiento con el exportado;
- generar escenas/fixtures deterministas;
- capturar y reproducir secuencias de ticks o paquetes;
- visualizar geometría, colliders, transforms, normales, anchors o contactos;
- producir overlays y diffs de referencia/candidato;
- ejecutar fuzzing o mutation campaigns;
- inspeccionar bytecode/constant pools/protocolos;
- generar snapshots o contact sheets en CI;
- simular latencia, reorder, pérdida o lifecycle difícil de expresar en un único GameTest.

Reglas:

1. la herramienta no puede convertirse en una segunda implementación productiva;
2. debe ser reproducible y versionar/fijar sus inputs cuando sean externos;
3. si es temporal, se elimina tras capturar evidencia durable;
4. si es reusable, puede permanecer aislada bajo `tools/`, fuentes de test o infraestructura CI apropiada;
5. su resultado se trata como evidencia, no como autoridad normativa;
6. el adversario no usa la herramienta como pretexto para arreglar producción bajo su propio rol.

### 7.5 Snapshots visuales

El adversario debe **considerar explícitamente snapshots** cuando el comportamiento relevante sea espacial, geométrico, animado, temporal, compositivo, de cámara, rendering/debug overlay o gamefeel observable. Si son útiles, deben formar parte de la evidencia adversarial.

Un snapshot puede ser:

- screenshot de Minecraft;
- render de referencia de modelo/collider;
- overlay modelo original vs geometría física;
- visualización de jerarquía/transforms;
- heatmap/diff;
- contact sheet;
- secuencia corta de frames representativos.

Para que sea evidencia y no decoración:

- fijar cámara/view/proyección cuando aplique;
- fijar tick/frame/partial tick y estado de animación;
- fijar seed, entidad, pose, escala, gravedad e inputs relevantes;
- comparar referencia y candidato bajo condiciones equivalentes;
- preferir overlays/diffs o snapshots pareados;
- registrar artifact/hash y parámetros suficientes para reproducir;
- no depender sólo de «parece bien» cuando exista un oracle semántico automatizable;
- no reemplazar con snapshots una prueba machine-verifiable exigida por requisitos.

El adversario puede recomendar ya en el plan sitios donde los snapshots probablemente aportarán valor, pero conserva libertad para añadir capturas después de leer la implementación si descubre un riesgo real nuevo dentro del contrato congelado.

---

## 8. Clasificación y bucle de fallos

Todo fallo se clasifica antes de corregirse:

1. **bug de implementación**;
2. **defecto del plan**;
3. **requisito ambiguo, incompleto o incorrecto**;
4. **arquitectura incorrecta**;
5. **test/tool/snapshot incorrecto**;
6. **problema de entorno/evidencia**.

```text
bug de implementación ───────────────→ IMPLEMENTADOR / implementación
plan defectuoso ─────────────────────→ planificación
requisito defectuoso ────────────────→ requisitos → plan
arquitectura defectuosa ─────────────→ arquitectura → plan
test/tool/snapshot incorrecto ───────→ ADVERSARIO / verificación
problema de entorno/evidencia ───────→ corregir entorno y repetir evidencia
```

Después de cualquier corrección se repite **toda la batería afectada**, no sólo el test que falló. No se permite «parchear hasta verde» sin decidir primero qué estaba mal.

---

## 9. Revisión final iterativa

Con la batería verde se hace una revisión distinta de la suite.

Debe contrastarse:

- requisitos y criterios incluidos;
- invariantes vecinos;
- plan;
- diff completo;
- arquitectura y ownership;
- tests, tooling, snapshots y modelo adversarial;
- lifecycle/error paths;
- API/data compatibility;
- código muerto o duplicado;
- imports/dependencias;
- documentación canónica;
- observabilidad/diagnósticos;
- hot paths si aplica;
- ausencia de fallback o segundo motor no permitido.

La revisión final requiere al menos una pasada independiente del adversario sobre la producción ya verde. Si esa revisión exige cambiar producción, vuelve al implementador y se repite el ciclo afectado.

Sólo converge cuando **una pasada completa no produce ningún cambio productivo pendiente ni hueco de evidencia**.

---

## 10. Checklists y documento de cada sprint

No se mezclan estados distintos bajo una sola lista.

### 10.1 Requisitos

```text
[ ] FR-xxx — criterio aún no demostrado
[x] NFR-yyy — evidencia suficiente registrada
```

### 10.2 Implementación — IMPLEMENTADOR

```text
[x] I1 ...
[x] I2 ...
[ ] I3 ...
```

`[x]` significa sólo que la tarea está implementada.

### 10.3 Adversarial y verificación — ADVERSARIO

```text
[x] A1 — hipótesis adversarial cubierta
[x] A2 — holdout ejecutado
[x] T1 — test implementado
[x] V1 — snapshot/tooling ejecutado cuando aplica
[x] T1 — resultado PASS
[ ] T2 — resultado pendiente
```

Los fallos no se borran del historial del sprint; se registra su clasificación y la fase a la que obligaron a volver.

### 10.4 Cierre

```text
[ ] suite existente relevante
[ ] suite nueva
[ ] tooling/snapshots aplicables ejecutados
[ ] niveles exigidos por requisitos
[ ] build limpio
[ ] infraestructura temporal retirada o justificada como reusable
[ ] documentación canónica actualizada
[ ] código muerto/duplicado revisado
[ ] VALIDATION actualizado con evidencia ejecutada
[ ] pasada final completa sin cambios
[ ] commit lógico del sprint
```

### 10.5 Plantilla del documento de sprint

```markdown
# Sxx — Título

## 0. Roles del sprint
### IMPLEMENTADOR
### ADVERSARIO

## 1. Scope
### Incluido
### Excluido
### Requisitos
### Invariantes

## 2. Estado inicial e investigación

## 3. Plan de implementación — IMPLEMENTADOR
### Checklist
### Historial de revisiones hasta convergencia

## 4. Modelo adversarial previo — ADVERSARIO
### Clases de equivalencia y fronteras
### Estados/secuencias/combinatoria
### Fault/lifecycle/network attacks
### Propiedades metamórficas/diferenciales
### Riesgos visuales y sitios recomendados para snapshots
### Holdout reservado

## 5. Implementación — IMPLEMENTADOR
### Checklist
### Cambios de plan durante implementación

## 6. Verificación adversarial — ADVERSARIO
### Tests creados
### Tooling/harnesses/probes
### Snapshots/diffs visuales
### Segunda pasada adversarial
### Mutation checks
### Resultados

## 7. Fallos encontrados y bucles

## 8. Revisión final
### Pasadas del implementador
### Pasada independiente del adversario
### Convergencia

## 9. Cierre
### Checklist de requisitos
### Evidencia
### Infraestructura temporal retirada/retenida
### Commit
```

El documento de sprint explica **qué ocurrió en ese sprint**. No mantiene una copia paralela del roadmap. Al cerrarlo, `ENTITY_COLLISIONS_PLAN.md` se actualiza sólo con el estado global que cambió y `VALIDATION.md` sólo con evidencia ejecutada.

---

## 11. Política de commits

La unidad lógica de integración es el sprint.

- cada sprint cerrado produce un commit lógico;
- no se mezclan dos sprints;
- no se declara cerrado con tests rojos o revisión pendiente;
- implementación, pruebas y documentación necesaria pertenecen al mismo cierre lógico aunque la herramienta obligue a commits intermedios;
- commits intermedios de tooling/probes no autorizan avanzar de sprint;
- no se toca otra rama salvo autorización explícita;
- no se force-pushea, reescribe historia, crea tag/release ni publica/instala artefactos por inferencia.

Los commits adversariales deben ser reconocibles como `test`, `ci`, `docs` o tooling equivalente y no esconder cambios productivos. Un commit `fix`/`feat` de producción pertenece al rol implementador salvo reasignación explícita.

---

## 12. Definition of Done

Un sprint termina únicamente cuando:

1. el scope final está explícito;
2. el plan del implementador convergió mediante una pasada completa sin cambios;
3. el modelo adversarial previo convergió de forma independiente;
4. las tareas de implementación están completas;
5. cambios materiales descubiertos durante implementación volvieron por planificación cuando correspondía;
6. ataques adversariales aplicables están cubiertos o justificados;
7. se realizó una segunda pasada adversarial sobre el código real;
8. los holdouts fueron revelados y ejecutados;
9. mutation testing o equivalente dirigido no deja sobrevivir mutaciones relevantes conocidas;
10. snapshots visuales/tooling complejo se usaron donde aportaban observabilidad material, o se dejó justificación explícita de por qué no aplicaban;
11. pasan tests nuevos y regresión relevante;
12. pasan los niveles exigidos por los criterios de aceptación;
13. ningún fallo se resolvió sin clasificar;
14. requisitos, arquitectura, plan y evidencia siguen consistentes;
15. se revisó/eliminó código muerto o duplicado;
16. tooling temporal se retiró y tooling reusable quedó aislado/justificado;
17. la revisión final completa produjo cero cambios productivos pendientes;
18. `VALIDATION.md` contiene sólo evidencia realmente ejecutada;
19. el estado global se actualizó en `ENTITY_COLLISIONS_PLAN.md`;
20. el sprint queda asociado a su commit lógico.

Si falla una condición, el sprint no está terminado aunque compile y tenga una pantalla llena de ticks verdes muy tranquilizadores.

---

## 13. Cómo iniciar un nuevo chat de trabajo

Un agente que retome Entity Collisions debe:

1. declarar si entra como **IMPLEMENTADOR** o **ADVERSARIO**;
2. leer `AGENTS.md`;
3. leer, en este orden:
   - `docs/ENTITY_COLLISIONS_REQUIREMENTS.md`;
   - `docs/ENTITY_COLLISIONS.md`;
   - `docs/ENTITY_COLLISIONS_PLAN.md`;
   - `docs/ENTITY_COLLISIONS_FOUNDATION_AUDIT.md` cuando afecte cimientos;
   - `docs/ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`;
   - `docs/VALIDATION.md`;
4. comprobar el HEAD de la rama autorizada y el primer gate abierto;
5. no asumir que evidencia histórica sigue siendo válida para el HEAD actual;
6. leer el documento del sprint activo y trabajar sólo en las secciones/responsabilidades de su rol salvo reasignación explícita;
7. ejecutar íntegramente este protocolo antes de avanzar al siguiente sprint.

La primera acción de un chat nuevo no es tocar producción. Es reconstruir el contexto suficiente y confirmar **qué rol está autorizado a ejercer**.
