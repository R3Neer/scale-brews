# Auditoría adversarial de rendimiento — Entity Collisions

Estado: **protocolo operativo transversal**. Complementa `ENTITY_COLLISIONS_SPRINT_WORKFLOW.md` para que el rendimiento se revise durante todo el desarrollo del motor físico, no sólo en el gate final G9.

Fuentes normativas relacionadas:

- `ENTITY_COLLISIONS_REQUIREMENTS.md`, especialmente NFR-007..014 y NFR-027;
- `ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`, que define los roles y el ciclo de cada sprint;
- `ENTITY_COLLISIONS_PLAN.md`, que conserva el benchmark/aceptación global final;
- `VALIDATION.md`, que sólo registra mediciones realmente ejecutadas.

Este documento **no inventa nuevos requisitos de producto ni sustituye NFR-014**. Define cómo debe comportarse el ADVERSARIO frente al riesgo de rendimiento durante cada sprint.

---

## 1. Principio

El rendimiento es una propiedad de corrección práctica de un motor físico. Un diseño semánticamente correcto que introduce trabajo no acotado, recomputación por observador, asignaciones masivas por tick o scans globales no se considera adversarialmente cerrado por el mero hecho de pasar los tests funcionales.

Por tanto:

- el adversario **DEBE considerar rendimiento en todo sprint de producción**;
- los sprints que tocan rutas por tick, movimiento, query, sweep, contacto, pose, networking o render/presentation física **DEBEN tratarlo como dimensión adversarial de primer nivel**;
- los sprints de preparación/offline/tooling también deben auditar complejidad, memoria y límites, pero no se les atribuye artificialmente un presupuesto de hot path si no ejecutan en runtime;
- G9 sigue siendo el benchmark integrado de aceptación del sistema completo, no la primera vez que se mira el rendimiento.

No se permite posponer una regresión arquitectónica obvia a G9 con el argumento de que «ya se optimizará al final».

---

## 2. Clasificación obligatoria de la ruta

Cada sprint debe clasificar explícitamente sus cambios productivos como una o varias de estas superficies:

1. **HOT_TICK** — ejecuta una vez o más por tick y entidad/soporte;
2. **HOT_MOVE_QUERY** — ejecuta por movimiento, sweep, raycast, narrowphase o consulta física;
3. **HOT_NETWORK** — ejecuta por paquete/receptor/replicación durante juego;
4. **HOT_PRESENTATION** — reconstrucción/interpolación física consultada por cliente/render/debug;
5. **PREPARATION** — carga, generación, compilación o preparación de catálogo/modelo fuera del hot loop;
6. **OFFLINE_TOOLING** — tooling/CI sin coste runtime del mod.

Una ruta puede pertenecer a varias categorías. Si el sprint no toca ninguna ruta productiva, debe dejarlo escrito en vez de omitir el análisis.

---

## 3. Variables de escalado que el adversario debe identificar

No basta con medir «un caso». El modelo adversarial debe nombrar las dimensiones que multiplican trabajo o memoria. Según el sprint pueden incluir:

- `S`: soportes anatómicos activos;
- `B`: cuerpos potencialmente soportados;
- `P`: piezas por modelo/soporte;
- `C`: piezas/candidatos que sobreviven al broadphase;
- `K`: contactos simultáneos;
- `M`: subintervalos/samples de CCD o movimiento material;
- `O`: observadores/clientes;
- `H`: tamaño de histories/receipts/colas causales;
- `R`: revisiones/variants del catálogo;
- bytes de catálogo/pose/contacto;
- frecuencia de invalidación de caches;
- número de cambios de pose/root por tick.

La segunda lectura debe preguntarse qué complejidad real aparece al crecer cada variable y qué mecanismos impiden que una dimensión global se cuele en una query local.

---

## 4. Auditoría estática adversarial obligatoria

En la segunda lectura de cada sprint, el adversario busca expresamente:

- scans de todas las entidades del mundo o de todos los soportes desde una query local;
- bucles anidados cuyo producto crezca como `B×S`, `P×P`, `O×P` o equivalente sin filtro/cota justificada;
- reevaluación de pose/geometría para cada observador o cada consumer;
- hashing, serialización, compresión o reconstrucción de catálogos en rutas por tick/receptor cuando deberían reutilizarse;
- reflexión, introspección de clases o traversal de modelos externos dentro de runtime físico;
- creación repetida de `Map`, `List`, `Set`, streams, lambdas capturantes, arrays temporales, `Matrix4f`, `Vec3`, AABB u otros objetos en loops de alta frecuencia cuando exista una alternativa razonable;
- copias defensivas grandes repetidas en query en vez de en frontera de ownership;
- boxing/unboxing y conversiones innecesarias en loops internos;
- locks/synchronized globales o contention potencial entre entidades/queries;
- caches sin clave causal suficiente, caches que invalidan demasiado o caches que crecen sin bound/TTL;
- logging/diagnostics por tick sin rate limit;
- retry loops, recovery/separation o CCD cuyo límite existe en teoría pero se reinicia accidentalmente dentro de otro loop;
- trabajo duplicado entre server, cliente, physics y presentation que viole el ownership/reuse ya exigido por requisitos.

Encontrar uno de estos patrones no implica automáticamente que la producción sea incorrecta. Sí obliga a justificarlo, medirlo o construir una prueba que demuestre que la ruta permanece acotada.

---

## 5. Evidencia dinámica

Cuando el cambio toca una ruta hot, el adversario debe preferir evidencia que mida **trabajo**, no sólo wall-clock ruidoso.

### 5.1 Contadores estructurales

Siempre que sea posible se instrumentan métricas como:

- candidatos consultados;
- piezas evaluadas;
- pose evaluations;
- sweep/narrowphase evaluations;
- iteraciones de recovery/CCD;
- cache hits/misses;
- reconstrucciones de catálogo;
- bytes/packets;
- entries de histories/receipts;
- asignaciones o proxies razonables de object churn.

Los tests deben comprobar scaling/locality cuando el requisito lo permite. Ejemplo: añadir entidades lejanas no debe aumentar los candidatos de una query local.

### 5.2 Benchmarks comparativos

Para cambios donde el tiempo real importa:

- medir baseline y candidato en la misma máquina/runner cuando sea posible;
- separar warm-up de ventana de medida;
- fijar seed, entidades, poses, escala, gravedad, workload y duración;
- registrar commit SHA, Java, Minecraft/Fabric, runner y parámetros;
- usar distribuciones/p95/p99 cuando la cola importa, no sólo media;
- distinguir CPU del subsistema de carga inicial, descarga de assets, logging y ruido externo;
- repetir suficiente trabajo para que la señal domine al overhead del harness.

Una regresión temporal observada no se «explica» con una única ejecución favorable. Se clasifica y se repite hasta distinguir señal de ruido.

### 5.3 Memoria y GC

Cuando el sprint introduce estado persistente o trabajo por tick, se audita también:

- crecimiento de entries con tiempo;
- estabilización tras TTL/caps;
- bytes aproximados por soporte/cuerpo/revisión cuando sea material;
- object churn y presión de GC en loops físicos;
- retención accidental de entidades/modelos/worlds por caches, listeners o maps estáticos.

Un soak que no pierde contactos pero hace crecer memoria indefinidamente no cuenta como PASS de rendimiento.

---

## 6. Reglas por tipo de ruta

### HOT_TICK / HOT_MOVE_QUERY

Son las superficies más estrictas. Deben demostrar bounded work y locality. No se acepta scan global, recomputación por consumer ni allocation storm conocida sin evidencia de que queda dentro del presupuesto.

### HOT_NETWORK

Debe demostrar reuse por revisión/recipient compatible y evitar reconstrucciones/serializaciones idénticas N veces. El adversario mira throughput, bytes y fan-out además de latencia.

### HOT_PRESENTATION

No puede convertirse en una segunda simulación física ni recomputar la misma geometría por cada query/render consumer. La caché debe respetar identidad causal y lifecycle.

### PREPARATION

Puede usar reflexión, validación pesada, comparación doble o materialización de modelos si ocurre fuera del hot loop y está acotada. El adversario debe demostrar que esa lógica **no filtra a runtime** y que tamaños/profundidad/trabajo tienen límites explícitos.

### OFFLINE_TOOLING

No está sujeto al presupuesto runtime del mod, pero debe permanecer reproducible y no ocultar una implementación productiva paralela.

---

## 7. Relación con NFR-014 y G9

NFR-014 conserva el gate integrado final:

- 64 soportes anatómicos activos;
- 64 cuerpos contactando;
- 16 jugadores observadores;
- 12 000 ticks tras warm-up;
- p95 del tiempo añadido por el subsistema `<= 5 ms/tick`;
- p99 `<= 10 ms/tick`;
- objetivo efectivo de 20 TPS.

Los micro/meso benchmarks de sprints **no sustituyen** este benchmark. Su función es impedir que los costes se acumulen durante meses hasta que G9 descubra una arquitectura ya cara de corregir.

Si una optimización propuesta cambia semántica, ownership, determinismo o fail-closed, no se acepta «por rendimiento» sin modificar primero el requisito/arquitectura correspondiente.

---

## 8. Mutation/fault mindset de rendimiento

Cuando sea útil, el adversario debe construir mutantes que representen regresiones de coste, por ejemplo:

- desactivar una caché y forzar reevaluación por observer;
- reemplazar índice local por scan global;
- invalidar cache cada tick aunque inputs no cambien;
- serializar una vez por receptor en lugar de una vez por revisión;
- eliminar un cap/TTL;
- duplicar una sweep evaluation;
- mover reflexión/preparación al hot loop;
- convertir una estructura O(1)/O(log N) esperada en traversal lineal global;
- retener entries expiradas.

Un test de rendimiento/contadores que no distingue producción de estos mutantes es débil aunque los timings ocasionales parezcan buenos.

---

## 9. Cierre adversarial por sprint

Todo sprint de producción debe dejar respondido:

- [ ] clasificación de ruta (`HOT_*`, `PREPARATION`, `OFFLINE_TOOLING`);
- [ ] variables de escalado relevantes identificadas;
- [ ] segunda lectura estática de complejidad, allocations, caches, reflection, locks y trabajo duplicado;
- [ ] límites/bounds aplicables comprobados;
- [ ] contadores/locality tests añadidos o una justificación concreta de por qué no aportan señal;
- [ ] benchmark/soak/allocation evidence ejecutado cuando el riesgo temporal o de memoria es material;
- [ ] ninguna regresión de rendimiento observada queda sin clasificar;
- [ ] ningún trabajo de preparación/reflection se ha filtrado al hot loop;
- [ ] NFR-007..014/NFR-027 aplicables siguen trazables;
- [ ] la pasada final adversarial queda a cero cambios productivos y a cero huecos de evidencia de rendimiento pendientes.

Un sprint puede cerrar sin benchmark temporal si realmente no toca una ruta cuyo tiempo runtime sea material. No puede cerrar sin **auditar y documentar esa decisión**.

---

## 10. Auditoría retroactiva

Los sprints anteriores a este protocolo no se consideran automáticamente revalidados en rendimiento porque fueran funcionalmente verdes.

Antes del cierre global del sistema debe existir una auditoría retroactiva de los hot paths ya construidos, priorizando:

1. broadphase/spatial index;
2. narrowphase/sweeps/CCD/separation;
3. contact/carry/passive transport;
4. pose/root evaluation y `ModelGeometry` world materialization;
5. networking/catalog/pose/contact fan-out;
6. caches/histories/receipts/lifecycle;
7. client presentation queries;
8. full integrated NFR-014 workload.

La auditoría retroactiva puede reutilizar evidencia válida existente, pero debe distinguir claramente «boundedness demostrada», «locality demostrada», «timing medido», «memory/GC medido» y «aún no medido». No se infiere rendimiento a partir de un GameTest funcional.