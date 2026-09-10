# S00 — Foundation Audit de Entity Collisions

Estado: **prerrequisito obligatorio antes de cualquier sprint de implementación funcional** del subsistema de colisiones entre entidades.

Este documento define la auditoría de cimientos que debe ejecutarse una vez, antes de empezar la secuencia normal de sprints descrita en `ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`.

No redefine requisitos, arquitectura, roadmap ni evidencia. Las fuentes canónicas siguen siendo:

- `ENTITY_COLLISIONS_REQUIREMENTS.md`: qué debe hacer el sistema y cómo se acepta.
- `ENTITY_COLLISIONS.md`: arquitectura, ownership, API y modelo de datos.
- `ENTITY_COLLISIONS_PLAN.md`: gates, dependencias y estado global.
- `ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`: cómo se ejecutan los sprints.
- `VALIDATION.md`: evidencia realmente ejecutada.

---

## 1. Objetivo

Antes de construir G1 o cualquier trabajo posterior, se debe demostrar que las piezas fundamentales existentes son una base razonable para continuar.

El principio de S00 es:

> **Nada del código existente se considera correcto sólo porque ya exista, compile o tenga tests históricos verdes.**

El código actual es un candidato a reutilización, no una especificación.

S00 debe reconstruir la confianza desde los requisitos y la arquitectura actuales, especialmente para física, causalidad, identidad, lifecycle, geometría temporal y separación cliente/servidor.

La finalidad no es reescribir por sistema. La finalidad es impedir que decisiones antiguas, tests incompletos o abstracciones accidentales se conviertan en cimientos de la arquitectura nueva.

---

## 2. Regla de confianza

Cada componente fundamental auditado debe terminar clasificado como exactamente uno de estos estados:

### TRUSTED

El componente:

- encaja con los requisitos actuales;
- encaja con la arquitectura actual;
- tiene ownership y dependencias compatibles con el diseño objetivo;
- ha sobrevivido al modelo adversarial y a la evidencia exigida;
- no depende de una propiedad legacy que esté destinada a desaparecer para funcionar correctamente.

`TRUSTED` no significa «perfecto para siempre». Significa «hay evidencia suficiente para construir encima dentro del alcance actual».

### REWORK

La abstracción o idea es válida, pero la implementación contiene supuestos, acoplamientos o comportamientos que deben corregirse antes de confiar en ella.

### REPLACE

La abstracción contradice los requisitos o la arquitectura actuales de forma suficientemente profunda como para que conservarla obligue a deformar el diseño nuevo.

### UNPROVEN

No existe evidencia suficiente para afirmar que la pieza sea válida ni suficiente evidencia para justificar todavía su reemplazo.

**La ausencia de fallos conocidos no permite promover `UNPROVEN` a `TRUSTED`.**

---

## 3. Regla clean-room para el modelo adversarial

La primera estrategia adversarial de S00 se construye **antes de usar los tests existentes como fuente de ideas**.

Orden obligatorio:

1. leer requisitos normativos relevantes;
2. leer arquitectura relevante;
3. derivar invariantes y propiedades observables;
4. construir un modelo adversarial nuevo a partir de esas propiedades;
5. sólo entonces inspeccionar los tests históricos para mapear qué cubren, qué no cubren y qué regresiones conocidas deben conservarse.

Esto evita que la nueva estrategia adversarial quede limitada por los bugs que ya conocemos o por la forma concreta de la implementación existente.

Los tests históricos se conservan como evidencia de regresión cuando siguen siendo válidos, pero **no definen el espacio de comportamiento que debe auditarse**.

---

## 4. Qué debe auditar S00

S00 trabaja de abajo hacia arriba. Como mínimo debe revisar las piezas fundamentales que los sprints posteriores utilizarán como base.

El orden conceptual es:

```text
REQUISITOS FUNDAMENTALES
        ↓
INVARIANTES MATEMÁTICOS / FÍSICOS / CAUSALES
        ↓
MODELO ADVERSARIAL CLEAN-ROOM
        ↓
PRIMITIVAS GEOMÉTRICAS
        ↓
GEOMETRÍA TEMPORAL Y MOVIMIENTO
        ↓
IDENTIDAD / REVISION / EPOCH / LIFECYCLE
        ↓
CONTACTO / SOPORTE / TRANSPORTE
        ↓
RUNTIME / AUTORIDAD / NETWORK BOUNDARIES
        ↓
CLASIFICACIÓN DE CONFIANZA
```

No es obligatorio que todos los paquetes futuros estén ya separados durante S00. Sí es obligatorio que no se declare confiable una pieza cuya responsabilidad o dependencia contradiga una frontera arquitectónica ya decidida.

---

## 5. Auditoría física mínima

Para primitivas como geometría convexa, sweeps, respuesta temporal, jerarquías de movimiento, soporte y transporte, el modelo adversarial debe reconstruirse desde propiedades físicas y geométricas, no desde los tests actuales.

Como mínimo deben considerarse cuando apliquen:

- continuidad temporal;
- ausencia de tunneling;
- contactos tangenciales;
- penetración inicial y recuperación;
- geometría degenerada o casi degenerada;
- SCALE extremo;
- múltiples piezas;
- múltiples contactos;
- movimiento simultáneo de root y joints;
- movimiento relativo entre cuerpo y soporte;
- trayectorias no lineales o intervalos conservadores;
- cambio de pose dentro del intervalo;
- cambio de escala durante movimiento;
- las seis direcciones de gravedad soportadas;
- invariancia bajo traslaciones, rotaciones o representaciones físicamente equivalentes cuando corresponda;
- determinismo;
- estabilidad ante pasos temporales equivalentes;
- jerarquías profundas;
- transición, pérdida y reacquisición de soporte;
- ausencia de ciclos de auto-soporte.

Cuando una propiedad no sea aplicable a una primitiva concreta, el documento de S00 debe justificarlo en lugar de omitirla silenciosamente.

---

## 6. Auditoría causal, identidad y lifecycle mínima

La causalidad se trata como cimiento independiente de la geometría.

Como mínimo deben considerarse cuando apliquen:

- eventos duplicados;
- eventos perdidos;
- reorder;
- replay;
- estado atrasado;
- dos o más cambios legítimos en el mismo tick;
- revisiones que avanzan con huecos;
- reuse de entity ID;
- reuse de UUID o identidad aparente bajo distinto epoch/generation;
- unload/reload;
- dimension change;
- reconnect;
- root update sin joint update correspondiente;
- varios joint updates dentro de un intervalo;
- invalidación de cache;
- lifecycle parcial;
- desaparición de recursos durante una operación;
- reentrancia;
- publicación atómica frente a estado parcialmente construido;
- exact-once de contribuciones causales cuando el requisito lo exija;
- separación entre autoridad del servidor, prediction local y presentación remota;
- imposibilidad de que datos del cliente definan geometría física autoritativa.

Un bug geométrico y un bug causal son categorías distintas. Un componente no puede considerarse `TRUSTED` sólo porque su resultado geométrico aislado sea correcto si puede consumir material perteneciente a otra revisión, identidad o causalidad.

---

## 7. Tratamiento de los tests existentes

Después de construir el modelo adversarial clean-room, los tests existentes se clasifican como:

- **legacy regression**: conserva una regresión o comportamiento todavía requerido;
- **acceptance evidence**: demuestra directamente un criterio de aceptación actual;
- **property/adversarial evidence**: cubre una propiedad o familia de fallos del nuevo modelo;
- **obsolete**: verifica comportamiento que ya no pertenece al sistema objetivo;
- **misleading**: puede estar verde aunque la propiedad que aparenta validar siga rota;
- **duplicate**: no añade evidencia respecto a otro test más claro o de nivel adecuado.

Un test histórico verde **no promueve por sí solo** el código que toca a `TRUSTED`.

Los tests obsoletos, misleading o duplicados se eliminan o reescriben cuando sea seguro hacerlo, dejando trazabilidad suficiente en el documento de S00 y en la evidencia correspondiente.

---

## 8. Reparaciones descubiertas durante S00

S00 no es una auditoría pasiva.

Si una pieza fundamental resulta `REWORK` o `REPLACE` y los sprints siguientes dependen de ella, S00 debe sanearla antes de cerrar.

Toda reparación sigue el mismo rigor que un sprint normal:

```text
ANÁLISIS DEL FALLO
      ↓
PLAN DE REPARACIÓN ↺
      ↓
ACTUALIZACIÓN DEL MODELO ADVERSARIAL ↺
      ↓
IMPLEMENTACIÓN ↺
      ↓
TESTS ADVERSARIALES
      ↓
REVISIÓN COMPLETA ↺
```

Si una reparación revela que un requisito o la arquitectura eran incorrectos o incompletos, se vuelve primero a la fuente canónica correspondiente.

S00 **no debe aprovecharse para implementar features nuevas de G1+** que no sean necesarias para establecer una base confiable. Si una mejora puede esperar al sprint que le corresponde sin contaminar la auditoría, espera.

---

## 9. Documento de ejecución de S00

Al comenzar el trabajo real se crea:

`docs/sprints/S00-foundation-audit.md`

Ese documento es registro de ejecución, no fuente normativa.

Debe contener al menos:

### Scope

- requisitos fundamentales incluidos;
- componentes auditados;
- exclusiones explícitas;
- dependencias que S01 necesitará poder considerar confiables.

### Invariantes derivados

Propiedades obtenidas directamente de requisitos y arquitectura, antes de estudiar los tests históricos.

### Modelo adversarial clean-room

Ataques, clases de equivalencia, fronteras, transformaciones metamórficas, estados, secuencias, mutaciones conceptuales y demás técnicas aplicables.

### Inventario de componentes

Para cada componente:

```text
Componente | Responsabilidad | Dependencias | Estado inicial | Evidencia | Estado final
```

Estados finales permitidos: `TRUSTED`, `REWORK`, `REPLACE`, `UNPROVEN`.

### Mapa de tests históricos

Qué prueba realmente cada test existente y a qué categoría pertenece.

### Reparaciones

Checklist de cualquier rework/replacement necesario y su ciclo de revisión.

### Evidencia

Tests ejecutados, resultados, límites de la evidencia y referencias que procedan a `VALIDATION.md`.

### Revisión final

Registro de las pasadas iterativas hasta que una pasada completa produzca cero cambios.

---

## 10. Checklist mínima de S00

```text
[ ] S00-01 Releer requisitos y arquitectura fundamentales.
[ ] S00-02 Derivar invariantes sin usar los tests históricos como fuente.
[ ] S00-03 Construir y revisar hasta convergencia el modelo adversarial clean-room.
[ ] S00-04 Inventariar las primitivas y servicios fundamentales existentes.
[ ] S00-05 Auditar física y geometría temporal.
[ ] S00-06 Auditar causalidad, identidad, revision/epoch y lifecycle.
[ ] S00-07 Auditar fronteras de autoridad cliente/servidor relevantes.
[ ] S00-08 Inspeccionar y clasificar los tests históricos después del modelo clean-room.
[ ] S00-09 Clasificar cada componente como TRUSTED / REWORK / REPLACE / UNPROVEN.
[ ] S00-10 Reparar los cimientos necesarios para los siguientes sprints.
[ ] S00-11 Ejecutar evidencia adversarial y regresiones aplicables.
[ ] S00-12 Actualizar documentación/evidencia sin duplicar fuentes canónicas.
[ ] S00-13 Revisar el conjunto completo iterativamente.
[ ] S00-14 Obtener una pasada final completa con cero cambios.
[ ] S00-15 Crear el commit lógico de S00.
```

La checklist puede ampliarse, pero ningún punto aplicable puede omitirse sin justificación.

---

## 11. Definition of Done de S00

S00 sólo termina cuando:

1. existe un modelo adversarial nuevo derivado de requisitos y arquitectura, no de los tests existentes;
2. las piezas fundamentales necesarias para empezar S01 han sido auditadas explícitamente;
3. cada una tiene estado final y evidencia trazable;
4. ninguna dependencia fundamental necesaria para S01 permanece `UNPROVEN`;
5. todo `REWORK` o `REPLACE` bloqueante ha sido resuelto y revalidado;
6. los tests históricos relevantes han sido clasificados y conservados, corregidos o retirados justificadamente;
7. la suite aplicable está verde en los niveles exigidos;
8. `VALIDATION.md` contiene sólo evidencia realmente ejecutada;
9. la documentación canónica sigue siendo coherente y sin duplicidades normativas;
10. una revisión completa final del trabajo produce **cero cambios**;
11. S00 queda cerrado en un commit lógico propio.

Sólo después de cumplir esta Definition of Done puede comenzar S01 o cualquier sprint destinado a implementar requisitos nuevos del roadmap.

---

## 12. Relación con los sprints posteriores

Una clasificación `TRUSTED` es evidencia reutilizable, no inmunidad permanente.

Si un sprint posterior:

- amplía el dominio de una primitiva;
- introduce una nueva dimensión de estado;
- cambia su ownership;
- cambia sus garantías temporales;
- descubre una clase de fallo no considerada;
- o invalida una premisa de S00,

la pieza afectada vuelve a `UNPROVEN` para ese nuevo alcance y debe revalidarse dentro del sprint correspondiente.

Así se evita tanto reexaminar desde cero todo el sistema en cada sprint como convertir S00 en un certificado eterno emitido por una divinidad administrativa inexistente.
