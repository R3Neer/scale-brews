# Requisitos del sistema de colisiones entre entidades

Estado: **normativo para el rediseño en `chatgpt-editing`**. Este documento es la única fuente de verdad de **qué debe cumplir** el subsistema. La arquitectura explica cómo se cumple; el plan sólo ordena trabajo; VALIDATION sólo registra evidencia.

## 1. Alcance

El subsistema cubre el contacto físico mediante el que un cuerpo pequeño puede apoyarse, caminar, ser transportado o adherirse a la superficie material de una entidad viviente mayor. No sustituye las colisiones vanilla generales entre entidades, no convierte el soporte en vehículo y no modifica el pathfinding global.

El término **anatomía** significa la geometría por piezas que representa el cuerpo visible/semántico de una entidad, con jerarquía y transformaciones. No significa el AABB global de la entidad.

El sistema objetivo sustituye el antiguo motor de `living platforms` y es la única física compartida que puede consumir Clinging Reoriented. Tiny Mounts y las relaciones vanilla de pasajero son sistemas distintos.

### 1.1 Convenciones normativas

- **DEBE / NO DEBE**: requisito obligatorio.
- **DEBERÍA**: requisito que sólo puede incumplirse con una decisión documentada en este archivo.
- **FULL**: especie y estados declarados cubiertos con geometría, pose y lifecycle verificados.
- **SAFE_PARTIAL**: especie reconocida, pero estados explícitos fallan cerrados sin collider.
- **EXCLUDED**: exclusión deliberada y justificada técnicamente.
- **UNRESOLVED**: el sistema no sabe clasificar/cubrir la entidad. No es una exclusión válida.

## 2. Requisitos funcionales

### A. Ownership, activación y alcance

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-001 | Scale Brews DEBE ser el único propietario de geometría anatómica, detección/resolución de contacto, transporte material, baselines y reconciliación de este subsistema. | Auditoría de hooks: ningún consumidor mantiene un segundo collider/carry/rebase para la misma relación. |
| FR-002 | El subsistema DEBE distinguir `DISABLED`, `BINDING` y `READY` por sesión/nivel. | Pruebas de transición y reconexión muestran ownership inequívoco y ausencia de estado residual. |
| FR-003 | En `BINDING` el sistema DEBE fallar cerrado: sin collider anatómico, contacto, carry, placement, clearance ni fallback legacy. | Un catálogo pendiente o reemplazo incompleto produce cero queries materiales válidas. |
| FR-004 | En `READY`, una especie, variant o pose no resoluble DEBE devolver ausencia de geometría/contacto, nunca una forma inventada. | Fixtures de especie/pose desconocida producen resultado vacío y diagnóstico acotado. |
| FR-005 | Tiny Mounts y monturas/pasajeros vanilla NO DEBEN convertirse en contactos anatómicos independientes. | Un pasajero conserva su seat/vehicle y no obtiene un segundo anchor material. |
| FR-006 | El sistema NO DEBE introducir navegación de mobs sobre entidades vivientes. | Pathfinding vanilla conserva el mismo resultado con el subsistema activo. |

### B. Política y elegibilidad

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-007 | DEBE soportar como cuerpos: jugadores, mobs compatibles, barcos/balsas incluidos cofres, minecarts fuera de raíles, items y falling blocks. | Al menos un GameTest real por categoría usa el mismo core de contacto/transporte. |
| FR-008 | Proyectiles y entidades sin una semántica de cuerpo soportable DEBEN quedar fuera por defecto. | Flechas/tridentes y un tipo misc no compatible no adquieren contacto. |
| FR-009 | El ratio máximo por defecto de anchura física cuerpo/soporte DEBE ser `0.85`, calculado con dimensiones físicas efectivas actuales. | Límites `<`, `==` y `>` y SCALE externo pasan. |
| FR-010 | La política DEBE permitir habilitar/deshabilitar globalmente, por categoría de cuerpo, por tipo de soporte y por perfil, además de override de ratio y fricción. | Datapack reemplaza cada dimensión de política y servidor/cliente observan la misma revisión. |
| FR-011 | Las decisiones físicas DEBEN usar SCALE efectivo, incluyendo modificadores de otros mods y transiciones en curso. | Cambio de escala externo durante contacto actualiza elegibilidad/geometría sin estado fantasma. |
| FR-012 | Un cuerpo NO DEBE soportarse a sí mismo ni crear ciclos de soporte. | Cadena A→B→C funciona; intento C→A se rechaza y no muta contactos válidos. |
| FR-013 | Estados incompatibles DEBEN poder excluirse por regla general o perfil sin perder la especie completa. | Un estado excluido pasa a SAFE_PARTIAL y recupera FULL al salir del estado. |

### C. Geometría y modelo de extensiones

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-014 | La geometría DEBE componerse de piezas convexas identificables, separadas y jerárquicas. | Huecos entre piezas permanecen atravesables; IDs son estables dentro de una revisión. |
| FR-015 | Un `GeometryEngine` registrable por API DEBE convertir una familia de modelos a la representación común sin conocer la física de movimiento. | Dos entidades distintas de una misma tecnología se exportan con el mismo engine. |
| FR-016 | El core DEBE incluir soporte genérico para árboles `ModelPart`. | Minecraft y un modelo `ModelPart` externo se extraen sin extractor Java por especie. |
| FR-017 | Tecnologías externas reutilizables, como `AdvancedModelBox`, DEBEN integrarse como engines/adapters de familia, no como colliders por mob. | Grizzly y otro modelo de la misma familia comparten extractor. |
| FR-018 | El SPI DEBE admitir futuras familias (`DisplayRig`, GeckoLib u otras) sin cambiar el solver físico. | Un engine de prueba registra piezas y usa raycast/collision sin modificar el solver. |
| FR-019 | La extracción DEBE preservar cubos, jerarquía, pivots, rotaciones, escala/inflation relevante y huecos. | Comparación con geometría fuente para reposo y transforms seleccionadas. |
| FR-020 | Filtros declarativos DEBEN poder excluir equipo, cosméticos, segunda capa de skin y piezas auxiliares sin eliminar descendientes físicos válidos por accidente. | Reporte por pieza explica `included/excluded` y motivo. |
| FR-021 | Piezas degeneradas o numéricamente inválidas DEBEN rechazarse antes de entrar en broadphase/solver. | Coordenadas NaN/∞, volumen inválido y jerarquía cíclica fallan la carga. |
| FR-022 | El sistema NO DEBE crear un collider anatómico desde el AABB global ni desde `automatic_top` cuando falta geometría. | Ausencia de engine/modelo produce cero piezas. |
| FR-023 | Un perfil legacy de superficie sólo PUEDE migrarse a un plano unilateral explícito dentro del core y con warning; nunca se interpreta como anatomía. | Test de migración conserva el plano y no crea costados/vientre artificiales. |
| FR-024 | Variants estructuralmente diferentes (p. ej. player wide/slim) DEBEN ser identificables y versionables. | Cada variant selecciona geometría correcta y cambio de variant invalida material anterior. |

### D. Poses, animación y transformaciones raíz

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-025 | Un `PoseEngine` registrable por API DEBE evaluar transformaciones de una familia a partir de datos server-safe. | Dos modelos que usan el mismo motor comparten evaluador. |
| FR-026 | El core DEBE separar comportamiento de datos: Java/API implementa engines; JSON selecciona engine, modelo, channels, variants, filtros, parámetros y estados. | Añadir un mob ya cubierto por engines existentes no requiere una clase Java nueva. |
| FR-027 | El sistema DEBE disponer de un vocabulario común de channels autoritativos (walk phase/amount, yaw/pitch, age, ground, attack, flags, etc.) extensible por engine. | Un provider externo registra/consume channels sin enviar transforms cliente. |
| FR-028 | Engines basados en keyframes DEBEN evaluar el tiempo/estado de animación desde autoridad de servidor o datos reproducibles, no desde el renderer cliente. | Dedicated produce la misma pose discreta que el cliente de referencia para inputs fijados. |
| FR-029 | Una pose desconocida, un channel indispensable ausente o una primitiva no soportada DEBE marcar el endpoint `UNAVAILABLE`. | El collider desaparece en esa pose y no queda congelado. |
| FR-030 | La transformación raíz (posición, yaw, escala y orientación externa) DEBE estar separada de las transformaciones locales de huesos/piezas. | Cambiar sólo el root no obliga a reevaluar joints y mantiene identidad causal nueva. |
| FR-031 | Un `RootTransformProvider` DEBE permitir orientación externa transversal, como Stormie's Spiders, sin reimplementar geometría/pose de la especie. | Spider y otro soporte compatible rotan el mismo catálogo mediante el adapter raíz. |
| FR-032 | La gravedad del cuerpo soportado y la orientación raíz del soporte DEBEN ser estados independientes. | Combinación de soporte rotado y gravedad lateral resuelve correctamente local/world frames. |

### E. Catálogo, datos y cobertura

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-033 | El mundo DEBE definir bindings `EntityType/variant → geometry engine/model + pose engine + root transform + policy` mediante datos declarativos. | Codec rechaza referencias inexistentes y acepta override válido sin recompilar Scale. |
| FR-034 | La API DEBE permitir registrar nuevos engines/adapters de comportamiento durante inicialización. | Mod de prueba añade un engine y un binding JSON lo selecciona. |
| FR-035 | El catálogo DEBE ser propiedad del servidor/mundo y sincronizarse a los clientes como una revisión atómica. | Cliente nunca observa mezcla de dos revisiones. |
| FR-036 | Un reload candidato inválido NO DEBE sustituir la última revisión válida. | Tras fallo, queries siguen usando exactamente el snapshot anterior; reload posterior válido se acepta. |
| FR-037 | Cada revisión DEBE tener epoch/revision e integridad verificable; modelo/provider/binding generation forman parte de la identidad causal que corresponda. | Paquetes de otra epoch/revisión/generación son rechazados. |
| FR-038 | El tooling DEBE descubrir automáticamente todos los `LivingEntity` de namespaces objetivo y producir estado `FULL`, `SAFE_PARTIAL`, `EXCLUDED` o `UNRESOLVED` con razón. | Reporte reproducible enumera cada tipo una vez y no permite filas silenciosamente omitidas. |
| FR-039 | Todo `LivingEntity` ordinario adulto de Minecraft 26.2, salvo exclusión técnica explícita aprobada, DEBE alcanzar `FULL` antes de declarar completo el sistema general. | Reporte Minecraft tiene `UNRESOLVED=0`; cada EXCLUDED enlaza motivo/test negativo. |
| FR-040 | Un paquete de compatibilidad externo DEBE poder declarar un conjunto de namespaces/versiones y exigir `UNRESOLVED=0` para su modpack. | El gate VP26 falla al introducir un tipo nuevo no clasificado. |
| FR-041 | La compatibilidad específica de VanillaPlus (bindings, overrides, catálogo generado y adapters realmente específicos) NO DEBE convertirse en conocimiento hardcoded del core general. | Build de Scale sin VP26 no carga clases/IDs privados; compat separado puede completar el catálogo. |

### F. Query, contacto y resolución de movimiento

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-042 | El broadphase DEBE buscar candidatos usando bounds de las piezas anatómicas, no AABBs de soporte como collider final. | Piezas alejadas/huecos no crean falsos narrowphase. |
| FR-043 | El narrowphase DEBE resolver convexos/OBB afines frente al AABB físico del cuerpo y devolver contacto estable. | Casos cara/arista/esquina y tangencia pasan con tolerancias documentadas. |
| FR-044 | El movimiento propio del cuerpo DEBE resolverse en su frame de gravedad y permitir sliding tangencial sin penetración. | Fixtures para seis gravedades mueven en dos ejes tangentes y bloquean el normal. |
| FR-045 | Una cara física puede sostener al cuerpo si `normal · antiGravity >= cos(45°)`. | Casos a 44.9°, 45° y 45.1° fijan la frontera. |
| FR-046 | Selección cardinal dominante y rechazo de empates sólo DEBEN aplicarse donde un consumidor necesita elegir una dirección cardinal; NO DEBEN restringir normales físicas válidas. | Plano inclinado válido soporta sin convertirse previamente en cardinal. |
| FR-047 | El solver DEBE conservar un contacto tangencial previo si sigue siendo materialmente válido aunque el movimiento actual no genere un nuevo hit. | Caminar paralelamente sobre una pieza no parpadea contacto. |
| FR-048 | El solver DEBE manejar múltiples candidatos/contactos con orden determinista y seleccionar una solución estable, no depender del orden de hash/entidades. | Repetición con orden de registro invertido produce mismo resultado. |
| FR-049 | La colisión DEBE ser continua frente al intervalo material del soporte: traslación, yaw, escala y animación de joints no pueden reducirse a dos endpoints si eso permite tunneling. | Obstáculo/contacto intermedio que no existe en endpoints es detectado. |
| FR-050 | Los movimientos materiales ocurridos entre dos instantes de autoridad DEBEN consumirse una única vez y en orden causal. | Repetición/replay de intervalo no mueve el cuerpo de nuevo. |
| FR-051 | Varias contribuciones materiales genuinas dentro de un mismo tick DEBEN poder agregarse sin colapsarlas ni duplicarlas. | Root move + joint change + soporte base producen el delta esperado una vez cada uno. |
| FR-052 | El solver DEBE intentar separación inicial/recovery acotada; si no puede separar con seguridad, DEBE suspender sólo la pareja problemática y liberar su contacto. | Solapamiento irresoluble no desactiva otras entidades ni teletransporta el cuerpo. |
| FR-053 | La supresión de `canCollideWith`/push DEBE limitarse a la pareja soporte-cuerpo y al periodo en que el core anatómico la sustituye. | Terceras entidades siguen empujando/colisionando vanilla. |
| FR-054 | Sneak DEBE proteger el borde para jugador soportado; saltar DEBE liberar contacto. | Edge test en varias gravedades y salto sin sticky contact. |

### G. Contacto material y transporte

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-055 | Un contacto material DEBE identificar como mínimo soporte UUID, revisión, pieza, cara, punto local, normal, tick de autoridad y secuencia/serial aplicable. | Serialización round-trip y anti-replay conservan identidad. |
| FR-056 | El anchor DEBE permanecer ligado a un punto material local de la pieza, no a una coordenada mundial histórica. | Rotación/animación del soporte transporta el punto correcto. |
| FR-057 | El transporte DEBE seguir la trayectoria material certificada y pasar cada desplazamiento por colisión con bloques/entidades aplicable. | Soporte que atraviesa un muro no arrastra al cuerpo a través de él. |
| FR-058 | Una obstrucción que impide completar carry DEBE liberar/suspender de forma segura, sin teleport ni deuda de transporte. | Delta permitido parcial no se reaplica en el tick siguiente. |
| FR-059 | En cadenas de soporte, la base DEBE resolverse antes que sus dependientes y cada contribución derivada debe conservar ancestry para evitar ciclos. | Cadena móvil de tres niveles no duplica carry. |
| FR-060 | Si la raíz transportada es un vehículo/entidad con pasajeros, se mueve la raíz una vez y los seats vanilla se refrescan; los pasajeros no reciben contacto/carry independiente. | Barco ocupado conserva separación y seat tras root/joint movement. |
| FR-061 | El transporte pasivo NO DEBE contar como caminar, nadar, sprint, exhaustion, estadística de distancia ni caída propia del cuerpo. | Contadores comparados contra control permanecen invariantes. |
| FR-062 | El aterrizaje sobre entidad NO DEBE fabricar el impacto Growth del terreno; el fall damage vanilla propio se conserva cuando corresponda. | Fall/landing fixtures distinguen suelo de soporte viviente. |
| FR-063 | El salto desde soporte NO DEBE heredar un impulso adicional del soporte salvo la física vanilla que ya corresponda. | Comparación con salto equivalente desde plataforma estática. |

### H. Cuerpos especiales

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-064 | Barcos/balsas soportados DEBEN tratar el contacto como suelo con fricción configurada, manteniendo prioridad del agua. | Transición tierra-soporte-agua no bloquea navegación ni duplica carry. |
| FR-065 | Minecarts sólo DEBEN usar soporte anatómico fuera de raíles. | Entrada a rail libera contacto y vuelve a física de rail. |
| FR-066 | Items soportados DEBEN seguir recibiendo lifecycle vanilla salvo optimizaciones explícitamente suspendidas durante contacto. | Pickup/despawn tras release y carry durante resting funcionan. |
| FR-067 | Falling blocks DEBEN permanecer entidades mientras estén soportados, pausar sólo lo necesario y reanudar agua, endurecimiento, placement, timeout e impacto al liberarse. | Arena/concreto/yunque ejercitan cada salida una única vez. |
| FR-068 | Un yunque transportado NO DEBE infligir impacto repetidamente por el carry pasivo. | Un único evento de impacto real produce como máximo una aplicación correspondiente. |

### I. Raycast y colocación

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-069 | El core DEBE exponer raycast sobre caras anatómicas elegibles y devolver identidad material, no sólo entidad alcanzada. | Rayo atraviesa hueco, distingue dos piezas y devuelve la cara más cercana. |
| FR-070 | La colocación de spawn eggs y boat/raft items sobre una cara DEBE validar alcance, policy/ratio, footprint orientado, bloques/otras entidades, permisos e inventario antes de mutar el mundo. | Cada fallo deja mundo/inventario intactos. |
| FR-071 | Placement exitoso DEBE orientar/posicionar el cuerpo según cara y gravedad elegida, crear un único anchor Scale y respetar creative/no-consumption. | Repetición en seis caras, survival y creative. |

### J. Integración de gravedad y Clinging

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-072 | El core DEBE exponer una API pública y versionada para `mode`, `ready`, `supported`, `support`, `clearContact`, `spaceClear`, `raycast`, `attachAtContact` y gravedad/frame equivalentes. | Consumer fixture compila sólo contra API pública y no accede a internals. |
| FR-073 | El adapter de gravedad DEBE tener un único owner; registrar otro owner debe fallar explícitamente y repetir el mismo debe ser idempotente. | Dos consumidores de prueba no pueden escribir frames simultáneamente. |
| FR-074 | Scale DEBE leer la gravedad efectiva para resolver física, pero NO DEBE decidir controles, cargas, efectos ni persistencia de Clinging. | Consumer cambia gravedad mediante su API; Scale sólo observa/resuelve. |
| FR-075 | Un cambio voluntario de gravedad de Clinging DEBE hacer preflight de la AABB destino contra bloques y anatomía antes de mutar posición/gravedad. | Preflight fallido conserva exactamente ambos estados. |
| FR-076 | La integración DEBE preservar el contrato Clinging vigente: Space fresco en aire, una carga Clinging, Reorientation ilimitado, prioridad Elytra, anchors/sonidos/efectos/persistencia y cámara del consumidor. | Suite de transición del consumidor pasa sin un segundo sistema de colisión. |

### K. Autoridad, red y predicción

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-077 | El servidor DEBE ser autoridad de catálogo, pose física, disponibilidad, contacto y transporte confirmado. | Cliente malicioso no puede crear geometría/contacto efectivo mediante C2S. |
| FR-078 | Clientes NO DEBEN subir geometría, matrices de pose ni contactos. Un ACK, si existe, sólo puede confirmar versión/capabilities/epoch. | Registry de payloads C2S no contiene DTO de geometría/pose/contacto. |
| FR-079 | Un cliente sólo PUEDE predecir física completa de entidades localmente autoritativas; observadores remotos sólo presentan el estado confirmado. | Dos clientes observan el mismo soporte y sólo owner ejecuta prediction/carry. |
| FR-080 | Cada frame/contacto de red DEBE validarse por dimensión, epoch, revisión, UUID, network id y binding/tracking generation pertinentes antes de materializarse. | Replays cruzados por cada eje son rechazados. |
| FR-081 | Tracking start DEBE enviar material suficiente para que un observador tardío reconstruya el estado actual sin reproducir historia anterior. | Late join obtiene endpoint/contacto actual y no carry acumulado. |
| FR-082 | Tracking loss, disconnect, entity replacement, dimension change, teleport y catalog/binding discontinuity DEBEN invalidar estado temporal y sus fences de forma definida. | Reutilización de UUID/network id no revive contacto de la instancia anterior. |
| FR-083 | La predicción local DEBE reconciliarse contra autoridad sin aplicar dos veces transportes confirmados. | 0/100/200 ms no producen drift creciente ni loop de corrección. |
| FR-084 | Los movement references/baselines necesarios DEBEN correlacionarse con transportes que el servidor realmente aplicó, consumirse una vez y estar acotados temporalmente. | Referencia duplicada, expirada, saturada o de otro tracking generation se rechaza. |
| FR-085 | Sólo un contacto físicamente confirmado PUEDE justificar el estado de apoyo usado por checks de vuelo/movimiento del servidor. | `allow-flight=false` no expulsa al jugador soportado ni permite vuelo sin contacto. |

### L. Presentación y cámara asociadas al contacto

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-086 | La presentación cliente DEBE derivarse del mismo endpoint/intervalo causal que el contacto confirmado; NO DEBE consultar una animación renderer independiente como verdad física. | Root-only y joint-only updates no desincronizan visual y contacto. |
| FR-087 | Cualquier offset visual/cámara residual DEBE aplicarse una sola vez y no modificar posición, AABB, movimiento ni gravedad. | Comparación de estado físico antes/después de render/camera es idéntica. |
| FR-088 | Pérdida ordinaria de contacto PUEDE suavizar el residual; teleport/discontinuity DEBE resetearlo inmediatamente. | Tests de fade vs reset sin offset viejo. |

### M. Exclusiones y comportamiento especial

| ID | Requisito funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| FR-089 | Bebés, sleeping, swimming, fall-flying, player flight y otras poses declaradas incompatibles NO DEBEN obtener collider hasta disponer de provider verificado. | Cada estado negativo da cero collider y vuelve a funcionar al estado ordinario. |
| FR-090 | Camello sentado y gato sentado/tumbado DEBEN quedar sin collider mientras esas poses no estén verificadas. | Tests específicos de entrada/salida. |
| FR-091 | Entidades multipartes/bosses con física incompatible, incluido Ender Dragon mientras no exista diseño específico, DEBEN quedar EXCLUDED en lugar de hacks AABB. | Coverage report explica exclusión y query devuelve vacío. |
| FR-092 | Happy Ghast DEBE conservar su plataforma/mecánica vanilla nativa; la política de Scale no debe reemplazarla. | Con Scale activo/desactivado se conserva comportamiento vanilla. |

## 3. Requisitos no funcionales

### A. Corrección, determinismo y seguridad

| ID | Requisito no funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| NFR-001 | Mismos inputs autoritativos, catálogo y orden causal DEBEN producir el mismo resultado físico en ejecuciones repetidas. | Replay determinista de fixtures y orden de registro permutado. |
| NFR-002 | Ningún resultado físico PUEDE depender del orden de `HashMap`, identidad accidental de objeto o render order. | Tests randomizan orden y comparan hashes de resultado. |
| NFR-003 | Todos los DTOs físicos y datos cargados DEBEN validar finitud, rangos, tamaños, unicidad y aciclicidad antes de uso. | Fuzz/fixtures inválidos fallan sin mutación parcial. |
| NFR-004 | Ante incertidumbre de geometría, pose, identidad, presupuesto o lifecycle, el sistema DEBE fallar cerrado y liberar/cuarentenar de forma localizada. | Failure injection nunca inventa collider ni conserva uno stale. |
| NFR-005 | El código common/dedicated NO DEBE cargar ni referenciar en ejecución clases exclusivamente cliente/render. | Dedicated inicia y ejecuta catálogo/poses sin `ClassNotFound` cliente. |
| NFR-006 | El cliente no es una fuente de confianza para geometría/pose/contacto. | Pruebas de payload manipulado no cambian autoridad física. |

### B. Límites de trabajo, memoria y red

| ID | Requisito no funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| NFR-007 | Toda búsqueda espacial, sweep, separación, cola causal e historia DEBE tener límites explícitos y outcomes distinguibles al agotarse. | Tests fuerzan cada límite y obtienen `QUARANTINED/ITERATION_LIMIT` equivalente, nunca loop no acotado. |
| NFR-008 | El broadphase NO PUEDE recorrer `level.getAllEntities()` por cada movimiento/query. Debe usar índice espacial de bounds materiales o mecanismo de complejidad local equivalente. | Instrumentación demuestra que candidatos dependen de la región consultada, no del total mundial en query hot path. |
| NFR-009 | La pose física de un soporte DEBE evaluarse como máximo una vez por authority sample/joint tick y reutilizarse por queries/recipients. | Métricas con N observadores mantienen una evaluación del endpoint, no N. |
| NFR-010 | Serialización, hash y fragmentación del catálogo DEBEN prepararse una vez por revisión/variant de envío y reutilizarse entre receptores compatibles. | Añadir N clientes no ejecuta N reconstrucciones del catálogo. |
| NFR-011 | Historias de root, contacto, transporte, frames y receipts DEBEN tener TTL y caps constantes documentados; una entrada expirada no puede volver a ser autoritativa. | Stress prolongado estabiliza memoria; métricas de entries no crecen con tiempo. |
| NFR-012 | Un soporte/modelo patológico NO PUEDE forzar un número no acotado de piezas/celdas/candidatos/eventos/bytes. | Límites máximos rechazan/cuarentenan entradas sobredimensionadas antes del hot loop. |
| NFR-013 | El protocolo DEBE imponer tamaño máximo y número máximo de fragmentos, verificar digest y rechazar mezclas/conflictos. | Paquetes oversize, fragmentos conflictivos y digest incorrecto no reemplazan snapshot. |
| NFR-014 | El sistema DEBE mantener 20 TPS bajo el benchmark de aceptación: 64 soportes anatómicos activos, 64 cuerpos contactando y 16 jugadores observadores en una misma dimensión, con p95 del tiempo añadido por el subsistema <= 5 ms/tick y p99 <= 10 ms/tick durante 12 000 ticks después de warm-up. | Benchmark reproducible registra baseline sin feature y ejecución con feature; se mide delta del subsistema, no wall-clock de carga inicial. |

### C. Robustez de lifecycle y concurrencia

| ID | Requisito no funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| NFR-015 | Mutaciones de estado físico/server DEBEN ejecutarse en thread/contexto de mundo autorizado; callbacks reentrantes no pueden crear un segundo evento material. | Reentrancy fixture se cuarentena sin diferir una mutación oculta. |
| NFR-016 | Reload de catálogo DEBE ser transaccional: construir/validar candidato antes de swap y conservar snapshot anterior si falla. | Failure injection en cada fase no deja mezcla observable. |
| NFR-017 | Teleport, cambio de dimensión, disconnect, unload y reemplazo de entidad DEBEN ser barreras de causalidad. | Eventos atrasados anteriores a barrera se rechazan. |
| NFR-018 | Saturación de histories/receipts DEBE ser explícita y conservadora durante todo su TTL. | Una referencia perteneciente a tick saturado se rechaza aunque entradas antiguas se poden. |

### D. Extensibilidad y mantenibilidad

| ID | Requisito no funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| NFR-019 | Añadir una especie que usa engines ya soportados DEBERÍA requerir sólo datos generados/declarativos y, como máximo, overrides pequeños; no solver/collider Java nuevo. | Fixture externo añade entidad completa sin modificar fuentes del core. |
| NFR-020 | Código Java específico por especie sólo PUEDE introducirse cuando represente comportamiento no expresable por un engine reutilizable; la excepción debe justificarse en el binding/coverage report. | Revisión rechaza provider específico sin razón/issue de generalización. |
| NFR-021 | La API pública DEBE ser mínima, versionada y orientada a capabilities; internals de catálogo, solver y networking no forman parte de compatibilidad binaria. | Consumer de prueba compila con artefacto API y no necesita paquetes internos. |
| NFR-022 | Cambio incompatible de firma/semántica pública DEBE incrementar versión; capacidades aditivas pueden evolucionar sin romper consumers que no las exijan. | Tests de negociación aceptan/niegan matrices de versión/capabilities. |
| NFR-023 | El schema de datos DEBE ser versionable y sus migraciones deterministas; claves legacy no pueden conservar simultáneamente dos semánticas. | Round-trip/migration tests producen una única representación canónica. |
| NFR-024 | Scale Brews general NO DEBE tener dependencia obligatoria ni conocimiento hardcoded de VanillaPlus o de un mod de fauna concreto. Integraciones reutilizables pueden vivir como adapters opcionales públicos; bindings de pack viven fuera. | `./gradlew build` del core funciona sin jars externos y búsqueda de IDs privados no aparece en producción. |
| NFR-025 | El subsistema DEBE organizarse por responsabilidad (`api`, `catalog/data`, `geometry`, `pose`, `physics/contact`, `network`, `runtime/integration`) y evitar clases monolíticas que posean varias de esas responsabilidades. | Auditoría de paquetes y dependencias no presenta ciclos entre capas de mayor nivel. |

### E. Observabilidad, reproducibilidad y pruebas

| ID | Requisito no funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| NFR-026 | Diagnósticos de incompatibilidad DEBEN incluir entity id, engine/provider, revisión y motivo, pero estar rate-limited/deduplicados. | 100 ticks de la misma pose inválida generan un único diagnóstico por identidad/motivo definido. |
| NFR-027 | Métricas DEBEN exponer al menos supports activos, frames/pose evaluations, piezas candidatas, sweep evaluations/exhaustion, eventos/quarantines, bytes/packets de catálogo/pose/contacto, cache hits y receipts. | Test/benchmark exporta un snapshot legible y consistente por tick/periodo. |
| NFR-028 | Todo catálogo generado para aceptación DEBE ser reproducible desde versiones exactas, hashes de JAR/resource pack, engine versions y tooling version. | Regeneración con mismos inputs produce el mismo digest; cambio de input cambia identidad. |
| NFR-029 | La prueba de una familia externa DEBE usar el modelo/renderer original correspondiente a la versión fijada; una fórmula sintética equivalente no basta como prueba de extracción/pose. | Harness compara vertices/transforms contra referencia real. |
| NFR-030 | La aceptación DEBE estar estratificada: unit/pure solver, GameTest server, cliente real, dedicated, consumer integration, 0/100/200 ms `allow-flight=false`, soak y QA humana de contacto/animación. | El release gate registra resultado separado por capa; una capa verde no sustituye otra. |
| NFR-031 | CI ordinaria DEBE compilar producción y GameTests y ejecutar la suite server correspondiente; suites cliente/dedicadas especiales deben tener comandos reproducibles documentados. | Workflow verde del commit candidato + evidencias especiales con hash del commit. |
| NFR-032 | El coverage report de un target declarado DEBE formar parte de su gate y `UNRESOLVED > 0` DEBE fallarlo. | Introducir entidad fixture sin binding vuelve rojo el gate. |

### F. Compatibilidad y no regresión

| ID | Requisito no funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| NFR-033 | Activar el subsistema NO DEBE alterar colisión/pathfinding global, combate, interacción o física de entidades que no participan en un contacto gestionado. | Comparativas vanilla/feature para terceros y entidades fuera de policy. |
| NFR-034 | Debe conservar compatibilidad con SCALE efectivo externo y con Gravity Changer mediante adapters, sin convertir esos mods en dependencia obligatoria del core. | Tests con y sin cada mod cargado. |
| NFR-035 | La migración del sistema antiguo DEBE eliminar el motor físico legacy cuando la nueva ruta lo sustituya; sólo puede sobrevivir un adapter de datos legacy explícito si tiene requisito vigente. | Búsqueda de hooks/carry legacy tras gate de migración muestra cero segunda ruta. |
| NFR-036 | Datos/resources desconocidos o de versiones no validadas NO DEBEN producir una afirmación automática de compatibilidad. | Coverage queda UNRESOLVED/SAFE_PARTIAL hasta regeneración/prueba. |

### G. Documentación y trazabilidad

| ID | Requisito no funcional | Criterio mínimo de aceptación |
| --- | --- | --- |
| NFR-037 | Debe existir una sola fuente de verdad por tipo de información: este archivo para requisitos, `ENTITY_COLLISIONS.md` para arquitectura/API/datos, `ENTITY_COLLISIONS_PLAN.md` para orden/estado y `VALIDATION.md` para evidencia ejecutada. | Búsqueda documental no encuentra requisitos/planes completos duplicados. |
| NFR-038 | README/GUIDE/CONFIGURATION pueden resumir o enlazar, pero NO DEBEN redefinir contratos internos o estados detallados. | Links apuntan al documento canónico y no mantienen tablas divergentes. |
| NFR-039 | Cada elemento del plan DEBE referenciar requisitos de este archivo y cada evidencia de aceptación DEBE poder referenciar los mismos IDs. | Trazabilidad requisito→tarea→evidencia sin requisito huérfano de prioridad obligatoria. |

## 4. Matriz mínima de cobertura objetivo

Esta matriz define **qué familias deben poder enchufarse**, no bindings concretos del modpack.

| Familia | Responsabilidad del core general | Datos/compat externos |
| --- | --- | --- |
| Minecraft `ModelPart` | GeometryEngine genérico + engines vanilla/Mojang necesarios | overrides/variants excepcionales |
| Mojang `AnimationDefinition` | Pose engine server-safe/reproducible | definitions + channels por binding |
| Citadel/Alex `AdvancedModelBox` | SPI y engine reutilizable opcional, sin código por criatura | modelos/pose programs/overrides versionados |
| Friends & Foes | reutilizar `ModelPart`; adapter de su runtime de animación si no puede expresarse como Mojang keyframes | bindings/channels/versiones |
| Root orientation (Stormie's) | `RootTransformProvider` transversal | selección del provider por target |
| Display composites | SPI `DisplayRig`/composición | perfil semántico de roots/children físicos |
| EMF/CEM | catálogo visual precompilable y server-owned, nunca upload cliente | export del stack visual administrado y hashes |

VanillaPlus es un **target de compatibilidad separado**. Su proyecto debe fijar versiones y proporcionar bindings/overrides/catalog generated hasta `UNRESOLVED=0`; cualquier engine realmente genérico descubierto durante ese trabajo vuelve al core o a un módulo reusable público.

## 5. Iteración y criterio de cierre de requisitos

La especificación se revisa con este bucle:

1. recorrer experiencia de usuario, física, datos, lifecycle, red, extensiones, compatibilidad, rendimiento, seguridad, pruebas y operación;
2. para cada escenario buscar: comportamiento no especificado, requisito ambiguo, doble propietario, fallback implícito, condición de error no definida o criterio no verificable;
3. corregir la especificación;
4. repetir desde 1;
5. cerrar sólo cuando una pasada completa no introduce cambios.

En la elaboración inicial se realizaron tres pasadas. La primera consolidó requisitos de `ANATOMY_PLAN`, `ANATOMY_API`, `PLATFORMS` y TODO. La segunda incorporó el modelo API+datos/familias y coverage de modpacks. La tercera buscó huecos transversales y añadió explícitamente lifecycle, replay, composites, rendimiento medible, trazabilidad y diferencia FULL/SAFE_PARTIAL/EXCLUDED/UNRESOLVED. La pasada de control posterior no produjo requisitos nuevos.

Cualquier cambio futuro que altere un DEBE/NO DEBE se realiza aquí primero, con ID nuevo o modificación explícita del existente; el plan o el código no pueden redefinir silenciosamente el requisito.
