# Reconstrucción del proyecto y del sistema anatómico

Fecha: 2026-09-10. Documento de análisis, no de implementación ni de aceptación.

## 1. Alcance y fotografía de Git

La revisión se hizo sobre `R3Neer/scale-brews`, rama **`chatgpt-editing`**, fijando las lecturas al commit **`5a8bf915d22d52cf4e79a68e2506d199bb6e848e`**. Su mensaje es `Descripción de los cambios` y su padre es `2557f4d1d540c3e61f55661621636c19e087d993`.

La referencia de `main` observada al comenzar fue `cdf6a3062c173438d2e145d017a64f9296cae4fe`. La comparación inicial mostró divergencia: un commit exclusivo de cada rama, con `2557f4d` como ancestro común. No se ha incorporado el commit exclusivo de `main` a esta reconstrucción ni se propone fusionarlo automáticamente.

**Autorización de esta tarea:** leer el proyecto y añadir estas notas únicamente en `chatgpt-editing`. No cambiar implementación, pruebas, configuración, recursos, artefactos instalados, ramas ajenas, tags o releases. Las autorizaciones históricas para publicar en `main` que aparecen en `AGENTS.md` y `CONTRIBUTING.md` no se aplican a esta tarea.

Método: lectura estática de documentación, fuentes, recursos, configuración de pruebas y metadatos de GitHub; consulta del log real de CI del commit auditado. **No se ejecutaron nuevas compilaciones ni sesiones de Minecraft durante esta revisión.** No se inspeccionaron la instalación local del pack, archivos locales no publicados ni el código actual del repositorio de Clinging. Las afirmaciones sobre esos ámbitos se identifican como evidencia histórica registrada por el proyecto, no como comprobaciones nuevas.

Las conclusiones usan cuatro categorías: **observado en código**, **observado en CI**, **registrado históricamente** y **pendiente de aceptación**. Tener una clase o un test escrito no equivale a demostrar que la funcionalidad funciona.

## 2. Conclusión principal

Scale Brews ya es un mod de tamaños con una base funcional amplia. El trabajo interrumpido no consiste simplemente en añadir otra montura o permitir subirse a la espalda de un mob. Consiste en **sustituir las plataformas superiores históricas y la física AABB de Clinging por un único sistema de contacto y transporte sobre la anatomía animada original, relativo a la gravedad y con autoridad del servidor**.

Hay una base geométrica, física y de sincronización considerable. Sin embargo, el código más reciente muestra una frontera explícita entre **Q1: consultar un estado geométrico causal e instantáneo** y **Q2: consumir intervalos materiales de movimiento de forma continua, ordenada y exactamente una vez**. Q2 tiene contratos y un planificador con pruebas simuladas, pero no aparece conectado como el motor completo de los hooks de movimiento examinados. La reconciliación anatómica también tiene infraestructura preparatoria, no aceptación final.

Además, el build del commit exacto está bloqueado por dos errores de compilación de tests. No es correcto describir la rama como validada, ni interpretar los éxitos de snapshots anteriores como prueba de su estado actual. El runtime anatómico sigue restringido a activación explícita de pruebas; la experiencia ordinaria conserva el sistema superior anterior. [S1][S2][S3][S4][S5]

## 3. Dónde están los requisitos y cómo interpretarlos

| Fuente | Función y cautela |
| --- | --- |
| `TODO.md` | Backlog aditivo del mod completo; conserva decisiones sustituidas e hitos anteriores. Una casilla antigua no siempre expresa el estado más reciente. |
| `docs/ANATOMY_PLAN.md` | Plan consolidado aprobado el 2026-09-09: decisiones, H0–H4, 26 requisitos y reparto de responsabilidades. Es la referencia principal de intención para esta migración. |
| `docs/ANATOMY_API.md` | Frontera pública entre Scale y consumidores. Describe ownership, estados, gravedad, contacto y transporte. Su referencia a V1 está desfasada respecto al código. |
| `docs/ANATOMY_IMPLEMENTATION.md` | Diario de implementación y evidencia por snapshot, con fallos y límites conservados. No debe leerse como una lista homogénea de pendientes actuales. |
| `README.md` | Distingue explícitamente la funcionalidad de la beta y el prototipo anatómico no habilitado. |
| `docs/PLATFORMS.md` | Documenta las plataformas superiores existentes, sus categorías, transporte, red y cámara. El término histórico «anatomical profiles» no significa que esas superficies sean ya los nuevos convexos animados. |
| `docs/VALIDATION.md` | Registro de pruebas y entregas. Se revisó su encabezado de beta.5 y sus secciones iniciales; no se reejecutó ni revalidó su historial. |
| `AGENTS.md`, `CONTRIBUTING.md` | Reglas de separación de módulos, balance, pruebas, arte y preservación del checkout. La restricción actual de rama tiene prioridad. |
| `build.gradle`, `.github/workflows/build.yml`, `tools/PrepareAnatomyProof.ps1` | Qué se selecciona y ejecuta realmente, frente a qué dice un resumen de documentación. |

También se localizaron `CHANGELOG.md`, `docs/GUIDE.md`, `docs/MECHANICS.md`, `docs/CONFIGURATION.md`, `docs/SCALE_LOOT.md`, `docs/WOLF_MOUNT.md`, `docs/ART.md`, `docs/README_RELEASE_PLAN.md` y ejemplos JSON. No se afirma una lectura exhaustiva de todos estos documentos auxiliares. Las búsquedas realizadas no devolvieron issues ni pull requests; el backlog encontrado está principalmente en archivos versionados. No se auditaron tableros de GitHub Projects.

## 4. Estado del mod que no debe perderse en la migración

Según README, TODO y el registro de validación, la base desarrollada incluye Growth/Shrinking I–III, transiciones físicas de 20 ticks, composición con SCALE externo, atributos y movimiento dependientes del tamaño, elaboración de pociones y balizas. Incluye interacciones ambientales, impactos de aterrizaje, explosiones de creepers, cámara para tamaños pequeños, plataformas vivientes, monturas pequeñas y reglas JSON del mundo.

Las monturas configuradas incluyen pollo, abeja y lobo. Montar mediante pasajeros y apoyarse físicamente sobre una entidad son sistemas distintos: el segundo no requiere silla ni crea una relación de pasajero. La migración anatómica debe conservar esa distinción. El límite de anchura de las plataformas, por defecto **0,85**, tampoco es el límite de SCALE relativo de las monturas pequeñas, **0,53** para pollo y abeja. [S1][S6]

Decisiones posteriores que evitan reconstruir una versión equivocada del proyecto: el loot utiliza `scale^1.6`, no el antiguo cuadrado; la confianza probabilística de lobos reemplaza la regla fija de tres paseos; la animación completa de la abeja montada reemplaza su anterior cuerpo congelado; el miedo de aldeanos se basa en diferencia relativa de tamaño frente a entidades visibles, no solamente en un jugador con Growth II. El escalado especial de la cabeza está cancelado y no hay autorización para reequilibrar atributos. [S1]

El encabezado de `VALIDATION.md` registra beta.5, un snapshot anterior con 122 tests de servidor y la suite cliente completa, prerelease de GitHub e instalación del JAR en un perfil existente de Modrinth. **Instalar en un perfil de Modrinth no es publicar el proyecto en Modrinth, ni demuestra aceptación humana del pack completo.** La anatomía se describe allí como código dormido. Los pendientes históricos de QA y publicación no desaparecen por desarrollar este motor nuevo. [S7]

## 5. Objetivo funcional reconstruido

El resultado buscado permite que un cuerpo elegible se apoye sobre la cabeza, el lomo, un costado o el vientre de una entidad grande, según su gravedad; que conserve un contacto material mientras el soporte se desplaza, gira, se anima o cambia de escala; y que pueda caminar o saltar por su cuenta sin duplicar el movimiento recibido del soporte.

«Piel» significa aquí **superficie de las piezas geométricas originales del modelo**, no una caja envolvente global ni una máscara de los píxeles transparentes de la textura. La extracción debe conservar piezas separadas, transformaciones, jerarquía y huecos, y filtrar equipo, cosméticos, segunda capa de piel y piezas degeneradas según las reglas aprobadas. No se rellenan los huecos entre patas para simplificar el collider. [S2][S3]

«Cualquier dirección» tiene una precisión importante: Gravity Changer aporta **seis direcciones cardinales de gravedad**. El soporte físico admite normales inclinadas cuando `normal · antiGravity >= cos(45°)`. La selección del eje cardinal dominante y el rechazo de empates pertenecen a Clinging, no restringen todas las normales físicas a seis caras alineadas con los ejes. Esta distinción existe también en `GravityFrame.supports` y `dominant`. [S2][S8]

### Invariantes de diseño

- **Un solo dueño de física:** Scale Brews posee geometría, consultas, resolución, contacto, transporte, referencias y baselines. Clinging consume la API y conserva controles, gravedad, efectos y cámara. No debe existir un segundo carry o reconciliador paralelo.
- **Autoridad del servidor:** catálogo, poses y contactos proceden del servidor. Los clientes no envían geometría, poses o contactos propios. El jugador local y el vehículo controlado pueden predecir; un observador no vuelve a transportar entidades remotas.
- **Fallo cerrado:** una especie o pose sin proveedor válido no recibe geometría congelada ni un AABB de sustitución. Durante `BINDING` tampoco se reactiva el motor legacy. El proveedor `static` es una elección explícita del autor del mundo, no un recurso automático para esconder una animación desconocida.
- **Conservación del juego existente:** elegibilidad, fricción, categorías, SCALE externo, protección de bordes, salto, cadenas, vehículos, asientos vanilla, items, bloques cayendo, colocación y cámara deben sobrevivir a la migración. No se altera globalmente `noCollision` ni el pathfinding.
- **Movimiento material una sola vez:** varias contribuciones genuinas dentro de un tick deben agregarse; una misma contribución nunca se aplica dos veces. El transporte pasivo no puede contabilizarse como caminar, generar agotamiento o caída extra ni activar artificialmente impactos Growth. [S2][S4]

Clinging debe conservar su contrato posterior alpha.5/6: pulsación nueva de Space en aire, una carga Clinging, Reorientation ilimitado, prioridad Elytra, anchors, sonidos, persistencia/recuperación y cámara configurable. No se debe volver al antiguo gesto Crouch ni exigir una nueva selección de cara como input. El cambio voluntario de gravedad exige preflight de bloques y anatomía; si falla, no se modifica gravedad ni posición. Debe desaparecer el impulso heredado del soporte al saltar. Esta parte es reconstrucción de requisitos y evidencia histórica, no una auditoría del Clinging actual. [S2][S3]

## 6. Qué está realmente construido

Los nombres de clases de esta sección pertenecen, salvo indicación, a `src/main/java/io/github/r3neer/scalebrews/platform/anatomy/`.

### 6.1 Contrato, sesión y autoridad

`AnatomyApi` expone versión **2** y capacidades `CONTACTS`, `CLEARANCE`, `RAYCAST`, `GRAVITY_FRAME`, `ROOT_TRANSPORT` y `SERVER_AUTHORITY`. `AnatomySession` centraliza `DISABLED`, `BINDING` y `READY`. Los aliases antiguos tienen semánticas distintas: `active` significa ownership, incluido BINDING; `usesSharedPhysics` significa READY. La integración no debe confundir ambos. [S4]

`AnatomyRuntime` administra catálogo mundial, bindings, generaciones del servidor, publicación y limpieza. Su arranque automático requiere `scalebrews.anatomyRuntime`; también existe `startPrepared` para fixtures. Hay hooks de tracking, conexión, desconexión y reload. El catálogo candidato se carga antes de reemplazar el estado aceptado. Esto demuestra implementación de mecanismos, no aceptación completa de reload con cuerpos siendo transportados. [S9]

`AnatomyCatalogTransfer` recibe fragmentos, comprueba compatibilidad, identidad de sesión, revisión y hash, y sustituye el catálogo atómicamente. El cliente tiene un gate de aceptación. El mapa servidor `sent` registra revisiones enviadas, no es por sí solo una prueba de aceptación por el cliente. No debe cerrarse la negociación integral de H0 únicamente porque ese mapa esté actualizado. [S9][S10]

### 6.2 Geometría, poses y causalidad

`GeometryProvider` separa `PublishedFrame`, `QueryFrame`, `CausalEndpoint` y `MotionIntervalHandle`. Un estado instantáneo consultable no implica que exista un intervalo temporal validado entre dos estados. `Availability.UNAVAILABLE` permite publicar la retirada de geometría conservando el orden de autoridad. [S11]

El código examinado conserva procedencia separada para `frameSerial`, tick de autoridad, tick de muestra de articulaciones y secuencia/tick del root. Un cambio de posición, yaw o escala dentro del mismo tick puede generar un endpoint nuevo sin reevaluar la animación de articulaciones. Las identidades incluyen mundo, UUID, network ID, época, revisión, modelo, proveedor y generación del binding. Los mapas de entidades usan identidad de objeto para evitar mezclar instancias cliente/servidor con el mismo network ID. [S5][S12]

Existe infraestructura de convexos, consultas, jerarquía animada, barrido conservador y separación. El diario documenta extracción de los modelos originales y comparaciones con renderers reales; esta revisión no volvió a ejecutarlas. No se deduce de esos resultados históricos que todas las familias o poses nuevas estén verificadas. [S3]

### 6.3 Contacto y transporte

`AnatomyMovement` identifica soporte, pieza, cara y punto local; verifica normales frente a la gravedad y conserva anchors materiales. Integra resolución de movimiento, consultas, bordes al agacharse, liberación y suspensión localizada de parejas que no pueden separarse de forma segura. Hay límites explícitos de trabajo y separación; no se presenta el algoritmo como solución global óptima para cualquier unión geométrica. [S5]

El carry actual sigue un punto local de la pieza, procesa soportes base antes de sus dependientes, mueve la raíz, recoloca asientos vanilla y actualiza baselines de jugadores/vehículos. Registra contribuciones aplicadas, incluso varias en el mismo tick, y conserva historia suficiente para descontar transporte pasivo del muestreo de locomoción. **No es correcto decir que el transporte no existe.** [S5][S13]

`PlatformPhysics` y `PlatformEntityMixin` conectan el prototipo con movimiento real y separan la ruta compartida de la histórica. En mundo ordinario permanece el motor superior legacy; en el ámbito compartido BINDING no permite reentrar a ese carry. Esta convivencia es un estado de migración, no el diseño final de dos motores simultáneos. [S14][S15]

### 6.4 Cliente y red

`AnatomyClientNetworking` conserva barreras anti-replay, valida UUID/network ID, revisión y generaciones, elimina material de poses no soportadas y distingue datos de conexión de datos de dimensión. La simulación local se restringe a entidades bajo autoridad local; los observadores no reciben otro carry. [S12]

La presentación nueva expone por ahora solamente `CURRENT_ENDPOINT`. Aunque existe el nombre `CERTIFIED_INTERVAL`, el constructor rechaza ese modo y deja documentada su reserva para Q2. No debe confundirse la existencia de utilidades de interpolación con una trayectoria de presentación certificada ya integrada. [S12]

`AnatomyTransportReceipts` registra desplazamientos ya aplicados y sus convexos anterior/posterior. No mueve entidades ni reejecuta transporte. Su propio contrato deja para un futuro validador la correlación de referencias C2S de metadatos con movimientos absolutos vanilla. `AnatomyPredictionBaselineProof` se declara prueba de **medición N2**, sin referencias anatómicas C2S, rollback o replay; que tenga la palabra Prediction en su nombre no demuestra reconciliación terminada. [S13][S16]

## 7. Hallazgos y bloqueos concretos

### F1. El commit auditado no completa el build

**Observado en CI:** run `34449689179`, job `102782357848`, commit `5a8bf91`. El checkout, el wrapper y Java se prepararon correctamente. `compileJava` y `compileClientJava` terminaron, al igual que el ensamblado del JAR. El fallo se produce en **`:compileGametestJava`**. [S17]

`AnatomyMaterialEventDispatcherTests.java`, líneas 80 y 96, invoca respectivamente:

```java
new MaterialEventDispatcher<String>(2, 3, key -> key);
new MaterialEventDispatcher<String>(2, 2, key -> key);
```

El constructor disponible exige:

```java
MaterialEventDispatcher(
    int maximumBodies,
    int maximumDepth,
    int maximumEvents,
    Function<E, Object> identity
)
```

Falta un argumento entero en ambos tests. El log contiene dos errores de compilación, además de warnings de deprecación que no son la causa del fallo. La suite no llegó a ejecutarse y se omitió la subida de artefactos del workflow. No se corrigió nada durante esta tarea. La corrección futura deberá actualizar los fixtures con un presupuesto de eventos coherente con sus aserciones, no debilitar el contrato del planificador para conseguir un build verde. [S18][S19]

### F2. Q2 no equivale todavía al pipeline vivo completo

En `AnatomyMovement.collide`, el movimiento propio se barre contra convexos **estáticos del endpoint causal actual**, construidos como `new ConservativeSweep.Motion(t -> piece, 0)`. El comentario explica que un futuro consumo Q2 aplicará cada intervalo material del soporte desde su cursor. El índice espacial también se define como instantáneo, reservando las envolventes temporales a Q2. [S5]

El carry actual calcula la diferencia entre el punto material anterior y el nuevo y pasa ese desplazamiento por la colisión vanilla. Eso no demuestra por sí mismo una trayectoria curva certificada de animación/giro ni todos los contactos intermedios. Hay una diferencia entre disponer de un barrido matemático y consumirlo correctamente en cada evento real del juego. [S5]

`MaterialEventDispatcher` sí implementa orden causal síncrono, lotes simultáneos de articulaciones, derivados de carry, límites y cuarentenas. Pero delega la consulta del mundo y la resolución en un backend; su test usa `FakeBackend` y declara no activar hooks, queries de Level, receipts ni red. Los hooks de movimiento revisados capturan/observan root frames, pero no conectan ese dispatcher como backend completo. La conclusión es **integración incompleta**, no ausencia del trabajo geométrico. [S15][S18][S19]

### F3. El catálogo se reconstruye para cada destinatario

`AnatomyNetworking.sendCatalog(player, ...)` llama a `AnatomyCatalogTransfer.encode(...)` en cada envío. `encode` crea y valida otro `WorldAnatomyCatalog`, serializa modelos y perfiles, calcula hash y fragmenta el bundle. El control por revisión de Runtime evita reenviarlo continuamente al mismo jugador, pero no reutiliza un bundle ya preparado entre destinatarios. [S9][S10][S20]

Esto deja abierto el requisito explícito de no reconstruir catálogo/perfiles/geometría por receptor. La revisión demuestra trabajo repetido, **no mide su impacto en milisegundos**. Además, `AnatomyMovement.spatial()` recorre los stamps de los soportes del índice para comprobar su vigencia; la complejidad por consulta merece instrumentación. No se afirma un coste global concreto sin perfilado. [S2][S5]

### F4. Los recursos ordinarios todavía no son el catálogo final

El directorio inspeccionado de perfiles contiene 18 JSON; por ejemplo `cow.json` sigue declarando `surfaces` de espalda y cabeza, con posiciones calibradas y referencias visuales, no una referencia `anatomy`. En el directorio de recursos propio inspeccionado no aparece un catálogo built-in `entity_geometry`. Esto concuerda con la documentación: aún no se ha migrado la distribución ordinaria a las geometrías originales exportadas. La mera presencia del codec/registro nuevo no proporciona ese catálogo. [S21]

Los perfiles antiguos de terceros deberán traducirse a planos unilaterales explícitos dentro del core, con advertencia. No deben transformarse en supuesta anatomía ni mantener `automatic_top` como fallback del nuevo sistema. [S2]

### F5. Documentación y evidencia necesitan reconciliación

| Diferencia | Lectura correcta |
| --- | --- |
| `ANATOMY_API.md` presenta versión 1; `AnatomyApi.PROTOCOL_VERSION` vale 2. | Revisar documentación y compatibilidad coordinada de consumidores; no reutilizar un artefacto antiguo por su nombre. |
| El plan habla de HEAD `2557f4d` y trabajo local dirty. | Esa descripción es histórica: el snapshot WIP revisado ya está publicado en `chatgpt-editing` como `5a8bf91`. |
| El diario contiene «pending» de mecanismos que después se conectaron. | Resolver cada afirmación mediante su fecha, fuente y ruta viva. No reconstruir el estado leyendo sólo el primer párrafo. |
| El resumen de preparación enfatiza A/B. | El script actual exige también la vía C de tracker lifecycle antes de copiar resultados verificados. |
| Hay evidencia anterior de red con latencia en plataformas superiores. | No cierra la aceptación de la nueva ruta anatómica ni la migración de Clinging. |
| Hay tests con nombres de predicción y presentación. | El contenido muestra N2 de medición y presentación Q1 instantánea; no implica rollback/replay o intervalos Q2 certificados. |

La mezcla de capas históricas explica parte de la dificultad de reconstrucción. No justifica borrar requisitos inacabados ni marcar hitos completos sin evidencia. [S1][S2][S3][S4][S12][S16][S22]

## 8. Evidencia que existe y lo que no demuestra

Los siguientes son resultados **registrados por el repositorio para snapshots anteriores**, no tests ejecutados en esta revisión:

| Evidencia | Alcance y límite |
| --- | --- |
| Extracción de vaca, jugador wide/slim y grizzly; 320 comparaciones animadas originales. | Prueba de extracción/poses concretas, no de todo el gameplay ni del catálogo VP26. |
| Pruebas de seis caras, huecos y barco ocupado. | Fixtures acotados; algunos establecen el frame de gravedad directamente y no sustituyen la integración real de Clinging. |
| 200 pasos deterministas con vaca original. | No son 200 ticks reales de servidor. |
| 60 ticks integrados de movimiento real de vaca con un cuerpo soportado. | Evidencia de contacto/transporte de observador; no aceptación general de jugador local/vehículo bajo latencia. |
| Host019 con dos hosts dedicados, épocas distintas y export inmutable. | Se rechazó una época antigua y se reutilizó el UUID, pero no el network ID. SharedWorld y la matriz real de modelos/resource packs seguían abiertos. |
| Clinging compilado contra snapshot 017, con 22 tests de servidor. | No certifica el consumidor actual frente a API 2/poses v4 ni cámara, transiciones y VP26 completos. |
| Snapshot 021: 134 tests de vía A; dos fixtures requeridos fallidos. | Fallo de contacto inicial de suelo con yaw/ascenso y fallo de readquisición tras squeezing animado. No hubo XML JUnit en ese run; se conservó hash del log. |
| Beta.5: 122 tests y cliente completos. | Snapshot anterior y gameplay ordinario con anatomía dormida; no valida `5a8bf91`. |

El resultado nuevo consultado es el fallo de compilación de F1. No deben confundirse sus **dos errores de compilación** con los **dos fixtures de runtime fallidos** del snapshot 021. [S3][S7][S17]

## 9. Matriz de cierre de los 26 requisitos

Esta tabla conserva los identificadores del plan. «Parcial» significa que hay infraestructura o evidencia acotada, no un cierre firmado.

| ID | Estado reconstruido y cierre necesario |
| --- | --- |
| RQ-CORE-01 | Parcial: core y seams existentes; completar la sustitución coordinada sin segundo AABB/carry. |
| RQ-CORE-02 | Políticas históricas preservadas como objetivo; repetir límites .85, overrides, categorías, fricción y SCALE externo en la ruta final. |
| RQ-GEO-01 | Extracción original con evidencia histórica; cerrar la prueba viva exacta player/cow/grizzly, poses y jerarquía. |
| RQ-GEO-02 | Filtros y decisiones por pieza documentados; conservar reporte de motivos, grosor .5/16, aspecto .025 y volumen relativo .00001. Transparencia no perfora por sí misma. |
| RQ-GEO-03 | Contratos sin fallback y sustituciones JSON existentes; completar aceptación de ausencia/incompatibilidad y diagnóstico único. |
| RQ-CAT-01 | Transferencia y bindings implementados; falta catálogo de distribución y aceptación integral de host/sesión. |
| RQ-CAT-02 | Rechazo atómico implementado; probar reload inválido, recuperación y transporte en mundo vivo sobre el snapshot exacto. |
| RQ-POSE-01 | Autoridad y procedencia separadas presentes; verificar todos los inputs vivos originales, cache 20 Hz e instrumentación completa. |
| RQ-PHYS-01 | Kernel temporal y respuesta existentes; conectar consumo Q2 y demostrar CCD de traslación/giro/animación/escala sin perder contactos intermedios. |
| RQ-PHYS-02 | Gravedad y normales correctas en contrato; cerrar contacto sostenido inclinado, bordes, escalones y multicontacto. |
| RQ-TRANS-01 | Anchors, carry raíz, cadenas y asientos presentes; completar contribuciones materiales/root-frame exactamente una vez, incluidos vehículos y negativas de raíles. |
| RQ-TRANS-02 | Adapters históricos; repetir items/bloques cayendo, agua, hardening, expiración y yunque una vez en la nueva ruta. |
| RQ-SAFE-01 | Recuperación acotada por pareja y contabilidad de transporte presentes; quedan squeezing sostenido y regresiones de caída/agotamiento/estadísticas/Growth. |
| RQ-PLACE-01 | API de raycast/attach y trabajo de integración presente; aceptación de huevos/barcos con cara, gravedad, permisos, espacio e inventario. |
| RQ-CLING-01 | Contrato conservado en plan y evidencia parcial externa; verificar 30 transiciones, carga, Elytra, salto y ausencia de arrastre en aire. |
| RQ-CLING-02 | API de gravedad/clearance existente; validar preflight con Gravity Changer real y sin modificar posición/gravedad al fallar. |
| RQ-NET-01 | Autoridad, identidad y separación observador/local implementadas; cerrar predicción y lifecycle en el juego real. |
| RQ-NET-02 | Baselines/receipts y fixture N2 preparatorios; falta reconciliación final y aceptación allow-flight=false a 0/100/200 ms. |
| RQ-CAM-01 | API de presentación Q1 y cámara histórica; completar consumo coherente, residual único, reset/fade y compatibilidad First Person. |
| RQ-MIG-01 | Pendiente: recursos siguen con surfaces; retirar fallback automático del final y adaptar legacy de terceros a plano unilateral explícito. |
| RQ-CATLG-01 | Pendiente de expansión controlada y reporte por especie/variante/proveedor/hash. No equiparar fórmulas nuevas con catálogo validado. |
| RQ-EXCL-01 | Exclusiones definidas; probar ausencia de collider/fallback para bebés, multipartes, dormir, nadar, élitros y camello sentado. |
| RQ-H1-01 | Abierto: evidencia original viva player/cow/grizzly y dedicated/host/resource-pack aún no cerrada. |
| RQ-H2-01 | Abierto: catálogo, lifecycle y migración con equivalencia/regresiones demostradas. |
| RQ-H3-01 | Abierto: consumidor Clinging actual y cobertura coordinada VP26. |
| RQ-REL-01 | Sólo se añade este análisis. Builds, documentación final, compatibilidad, artefactos y backups requieren sus gates e instrucción correspondiente. |

Fuente de la matriz y sus criterios: `ANATOMY_PLAN.md`. La tabla no marca nuevos requisitos como implementados ni altera el backlog aprobado. [S2]

## 10. Orden de continuación reconstruido

**Primero, recuperar un punto comprobable.** Corregir los dos fixtures incompatibles con el constructor, bajo una futura tarea de implementación, y registrar el resultado del build exacto. Recuperar también los dos casos rojos históricos sin asumir que este commit los resolvió. Revisar las versiones API/wire y el snapshot del consumidor antes de coordinar mods.

**H0: cerrar contrato y sesión.** Mantener la frontera Scale/Clinging y la distinción ownership/READY. Demostrar que BINDING, desconexión, cambio de catálogo o binding no reactivan rutas antiguas. Versionar y documentar el contrato que realmente se consume.

**H1: cerrar la prueba original mínima antes de ampliar.** Jugador wide/slim, vaca y grizzly Alex 2.1.9, con comparación de inputs del tracker vivo contra renderers originales, seis caras, vientre/costado/cabeza/huecos y carry invertido/lateral. Probar servidor dedicado sin clases cliente y la vía separada con Alex común real. Ni una fórmula sintética equivalente ni un export aislado bastan. La integración Q2 necesaria para cumplir esos escenarios debe resolverse sin confundirla con la predicción/reconexión más amplia de H3.

**H2: catálogo y migración Scale.** Completar consumo temporal y lifecycle físico, catálogo datapack atómico, cache e instrumentación, recuperación tras reload y migración de recursos built-in. Reutilizar bundles por revisión. Sólo después del gate original, ampliar familias adultas con extractores/proveedores comprobados y registrar explícitamente ausencias. Happy Ghast conserva su comportamiento vanilla.

**H3: Clinging y red coordinada.** Con el contrato compartido comprobado, migrar el consumidor real, retirar AABB/carry/referencias/reconciliación duplicados, conservar todos sus controles y cerrar el circuito local de predicción/reconciliación sin imponer carry a observadores. Ampliar VP26 por pruebas de geometría y de gameplay diferenciadas.

**H4: aceptación y artefactos.** Builds y suites de ambos mods, cliente y dedicado con `allow-flight=false`, 0/100/200 ms y soak de diez minutos según la matriz aprobada; reporte original-only con versiones/hashes/cobertura; JARs coordinados y backups fuera de `mods` sólo tras validación e instrucción. La QA humana del pack y multijugador no se sustituye con GameTests. No se infiere autorización para releases, tags o repositorios.

El plan distribuye física, red y aceptación en T1/T2/T3. Son fronteras históricas de responsabilidad que conviene conservar: el networking no redefine la semántica física del carry, y la integración no duplica la decisión de sesión. No significan que esta auditoría haya ejecutado tres agentes ni trabajo paralelo. [S2]

### Reproducción prevista por el repositorio

`PrepareAnatomyProof.ps1` exige un directorio nuevo y las dependencias originales Alex's Mobs Continued **2.1.9**, CodxLib y Cloth Config; copia un checkout aislado y registra hashes. El flag que usa para los mods de compatibilidad es **`-PscalebrewsCompatMods`**, no `exportModDir`.

El script actual ejecuta el cliente exportador y después **A: regresiones ordinarias**, **B: runtime preparado con catálogo no vacío** y **C: tracker lifecycle**, con manifests y reportes separados. Comprueba explícitamente los entrypoints de B y C. Sólo copia `verified-proof-catalog` y `filter-report` después del éxito. Su salida no es todavía un datapack de gameplay instalable. Estas vías no equivalen por sí solas a toda la prueba de Alex común vivo, SharedWorld, recursos o H4. [S22]

La CI ordinaria usa `./gradlew build`; no selecciona por sí sola todas las vías cliente/host/predicción preparadas en Gradle. Un build verde es necesario, pero no suficiente. [S23]

## 11. Pendientes ajenos a anatomía que siguen existiendo

El TODO conserva aceptación humana del JAR exacto en pack/multijugador, combate de lobos prestados con dos jugadores/latencia, interacciones de cámara y animaciones, barcos arrastrados con pasajeros, transiciones de agua y raíles, bloques cayendo y cadenas concurridas. También registra un problema de cierre limpio del harness cliente del pack completo, distinto del conflicto de registro Enchancement previamente resuelto. Esta auditoría no comprobó nuevamente ninguno de esos entornos.

La publicación pública pendiente debe distinguirse por plataforma y versión: el registro más reciente describe una prerelease beta.5 de GitHub, mientras una casilla histórica genérica de publicación permanece abierta. No se debe volver a publicar algo sólo por leer esa casilla ni dar por publicada la beta en Modrinth por haber instalado el JAR en un perfil. [S1][S6][S7]

## 12. Límites y criterio para retomar

Los objetivos y el orden de cierre son reconstruibles con alta confianza a partir del plan, del TODO y de las rutas de código examinadas. La salud de runtime de este commit no puede certificarse con esta revisión estática: la CI ni siquiera llegó a ejecutar sus tests. Tampoco se conoce desde esta auditoría el estado de trabajo local posterior al push ni el del consumidor Clinging actual.

La descripción más precisa del punto de interrupción es: **prototipo avanzado de geometría/contacto autoritativo, con Q1 causal y transporte básico conectados; Q2 de eventos materiales y reconciliación aún en integración; catálogo y consumidor final sin migrar; gates de aceptación abiertos; build del snapshot bloqueado por fixtures desactualizados**. No hay base seria para asignarle un porcentaje único de completitud.

Este documento añade trazabilidad. No modifica código, no activa anatomía, no reemplaza el plan aprobado y no cierra ningún gate.

## Fuentes fijadas al snapshot auditado

Todos los enlaces de código siguientes fijan `5a8bf915d22d52cf4e79a68e2506d199bb6e848e`; las ubicaciones no deben interpretarse como el estado de un futuro HEAD.

[S1]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/TODO.md
[S2]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/docs/ANATOMY_PLAN.md
[S3]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/docs/ANATOMY_IMPLEMENTATION.md
[S4]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/AnatomyApi.java
[S5]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/AnatomyMovement.java
[S6]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/docs/PLATFORMS.md
[S7]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/docs/VALIDATION.md
[S8]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/GravityFrame.java
[S9]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/AnatomyRuntime.java
[S10]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/AnatomyCatalogTransfer.java
[S11]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/GeometryProvider.java
[S12]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/client/java/io/github/r3neer/scalebrews/client/platform/anatomy/AnatomyClientNetworking.java
[S13]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/AnatomyTransportReceipts.java
[S14]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/PlatformPhysics.java
[S15]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/mixin/PlatformEntityMixin.java
[S16]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/gametest/java/io/github/r3neer/scalebrews/test/AnatomyPredictionBaselineProof.java
[S17]: https://github.com/R3Neer/scale-brews/actions/runs/34449689179/job/102782357848
[S18]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/MaterialEventDispatcher.java
[S19]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/gametest/java/io/github/r3neer/scalebrews/test/AnatomyMaterialEventDispatcherTests.java
[S20]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/java/io/github/r3neer/scalebrews/platform/anatomy/AnatomyNetworking.java
[S21]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/src/main/resources/data/scalebrews/scalebrews/entity_platform/cow.json
[S22]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/tools/PrepareAnatomyProof.ps1
[S23]: https://github.com/R3Neer/scale-brews/blob/5a8bf915d22d52cf4e79a68e2506d199bb6e848e/.github/workflows/build.yml
