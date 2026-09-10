# Plan consolidado: colisiones anatómicas Scale Brews + Clinging

Estado: plan de implementación aprobado el 2026-09-09. Este documento reemplaza la planificación dispersa para la migración anatómica; no afirma que una ruta de prueba, un build ordinario o un export sintético active el comportamiento final.

## Decisiones que no se reabren

- **Scale Brews** es el único dueño de geometría, query, contacto, resolución y transporte. Conserva políticas existentes de elegibilidad (ratio por defecto 0.85, categorías, fricción, overrides y SCALE efectivo externo), sin pathfinding ni `noCollision` global.
- **Clinging** conserva el contrato alpha.5/6: Space fresco en aire, una carga Clinging, Reorientation ilimitado, prioridad Elytra, efectos/sonidos/anchors/persistencia/recuperación y cámara configurable. Consume APIs anatómicas para preflight/contacto; no vuelve a exigir una selección de cara como input.
- El contacto físico se forma contra piezas del modelo original y no contra un AABB global. No hay fallback AABB ni `automatic_top` para especie/pose/proveedor incompatible o sesión pendiente: no hay collider/carry y se emite un diagnóstico único. Un perfil legacy de tercero se traduce sólo a plano superior unilateral dentro del mismo core, con warning; jamás a anatomía falsa ni a un segundo motor.
- El soporte físico acepta cualquier normal con `normal · antiGravity >= cos(45°)`. Normal cardinal dominante y empate rechazado se aplican sólo al cambio/selección cardinal de Clinging.
- Servidor es autoridad de catálogo, pose y contacto. Clientes no suben geometría. No se ejecutan renderers cliente en dedicado; una pose desconocida no se congela ni obtiene collider.
- Sin commits/push durante esta fase salvo instrucción posterior de coordinación. Cuando se ordenen tras validación, siguen la autorización existente para `origin/main` e instalación coordinada; nunca crear repos, tags o releases por inferencia.

## Estado que condiciona el plan

`2557f4d` es el HEAD publicado. La ampliación local dirty `VanillaFamilyPose`/protocolo v3 sí compiló y comprobó 640 poses vanilla nuevas y 240 originales vaca/jugador. La suite posterior falló en un fixture legacy ghast sin catálogo; Alex 2.1.9 no cargó por el flag incorrecto. Es evidencia parcial: no es validación completa ni commit. El runtime anatómico continúa opt-in y los recursos empaquetados siguen usando perfiles legacy `surfaces`; por tanto un build de la ruta ordinaria no cierra el plan anatómico.

## Hitos y orden obligatorio

### H0 — contrato y frontera de módulos

Antes de activar gameplay se publica una API común: `PROTOCOL_VERSION`, capacidades `CONTACTS`, `CLEARANCE`, `RAYCAST`, `GRAVITY_FRAME`, `ROOT_TRANSPORT`, `SERVER_AUTHORITY`, y operaciones de datos/contacto. `active(Entity)` es interno al core. Terra Networking centraliza catálogo/epoch/revisión/binding y el modo `DISABLED`/`BINDING`/`READY`; Terra Physics expone el contrato físico y Terra Integration consume el modo resuelto sin combinar estados. `BINDING` falla cerrado para anatomía (sin AABB/carry/referencia/placement legacy); `READY` usa sólo core y una query de especie/pose ausente deniega esa query. El único C2S permitido, si se requiere confirmar aceptación, es un ACK de versión/capacidades/epoch, nunca geometría, pose o contacto. La negociación conserva invalidez por epoch/revisión/reconnect/host sin retransportar acumulados.

H0 también fija DTOs inmutables: `GeometryProvider`, `GravityFrame`, `SurfaceContact` (UUID de soporte, revisión, pieza/cara estables, punto local, normal y tick), `SupportTransport`, `PoseProvider` y codecs. La secuencia pertenece a la publicación de contacto, wire y receipt; no se atribuye a un componente físico que no la tenga. Ningún cambio de Terra Networking decide la semántica física de carry o contacto de Terra Physics.

### H1 — prueba original, no catálogo completo

El cierre H1 exige sólo: jugador wide/slim, vaca y grizzly Alex 2.1.9; modelo original con vértices, jerarquía, cajas/huecos y transformaciones; reposo y animación original con canales v3 no vacíos; proveedor server-safe; contacto en vientre, costado, cabeza y hueco; seis direcciones; carry invertido y lateral; cliente real y dedicado sin clases cliente; e independencia real de host/resource-pack. Entradas/inputs del tracker se comparan con el renderer original para los mismos inputs: una fórmula sintética con los mismos valores no basta. H1 ejecuta dos dedicated lanes: (1) DTO/JSON aislado sin Alex ni clases cliente, y (2) vínculo vivo con Alex común real y entidad grizzly, para demostrar el guard/proveedor en vivo. El export usa `-PscalebrewsCompatMods=...`, nunca `exportModDir`, y no ejecuta la suite legacy global con `anatomyRuntime`.

H1 no exige predicción bajo latencia, reconnect, soak, catálogo VP26 completo ni la migración Clinging H3. Un fallo de H1 bloquea ampliación e instalación funcional; no rebaja requisitos.

### H2 — catálogo, lifecycle y migración Scale

Terra Networking convierte el catálogo en datapack mundial atómico, perfiles anatomy sincronizados y recursos built-in de referencias originales. Tras los gates, las entradas `surfaces` se migran a refs `anatomy`; `automatic_top` no permanece como fallback. Se validan reload inválido/recuperación, diagnóstico único, cache de pose por tick, tracking tardío, desconexión y host. Terra Physics migra resolución y lifecycle físico; Terra Networking valida que catálogos/poses/contactos no reactiven transporte acumulado.

Después de H1 se amplía el catálogo adulto, sólo con extractores y proveedores comprobados: jugador, ghast, abeja, pollo, vaca, oveja, cerdo, aldeano, caballo y variantes, burro, mula, camello y ambas llamas; luego golem, gato y modded genérico. Bebés, multipartes, dormir, nadar, élitros y camello sentado permanecen sin collider hasta tener proveedor específico. Happy Ghast queda vanilla intacto incluso si se desactiva su perfil. Camel puede usar animación baked declarativa sólo si vértices/render original la verifican.

### H3 — adaptador Clinging y expansión coordinada

Clinging pasa a ser consumidor de la API H0: preflight de su AABB orientada contra bloques/anatomía antes de mutar Gravity Changer, consulta de soporte/contacto y selección cardinal. Se retiran sólo tras regresión verde su bounding-box selection, `Shapes.create` AABB, `MovingSurface`, paquetes/referencias/carry/reconciliación duplicados y el impulso heredado de salto. Conserva Gravity Changer, cámara y controles propios. La red coordina contactos/poses/catálogo por revisión, UUID, pieza/cara, punto local y secuencia; jugador/vehículo local predice, observadores no hacen carry local.

### H4 — aceptación, empaquetado y QA

Se ejecutan builds y suites de ambos mods, dedicated allow-flight=false a 0/100/200 ms y soak de 10 min, informe VP26, export de catálogo original-only y cobertura/rechazos/versiones/hashes de extractor. Los JAR coordinados sólo se instalan con backup fuera de `mods` después de validación e instrucción de coordinación. La QA humana del pack queda explícita y no se sustituye por GameTests.

## Matriz de aceptación (26 IDs)

| ID | Requisito y evidencia mínima | Cierre |
| --- | --- | --- |
| RQ-CORE-01 | Núcleo común sustituye plataformas superiores/AABB Clinging; auditoría de mixins y seis caras reales. | Sin segundo carry ni legacy activo al habilitar final. |
| RQ-CORE-02 | Ratio .85, overrides, categorías/fricción y SCALE externo; negativas pathfinding. | Límites y combinaciones pasan sin ruta nueva. |
| RQ-GEO-01 | Extractores originales/variantes con vértices, jerarquía, cajas, transforms y huecos; hashes JAR. | Player/cow/grizzly original reposo+animación reproducibles. |
| RQ-GEO-02 | Filtros antes de escala: equipo/cosméticos/segunda piel, degeneradas/planas, grosor .5/16, aspecto .025, volumen .00001, IDs. | Reporte motivo por pieza y transparencia no perfora. |
| RQ-GEO-03 | JSON mundial alternativo; sin AABB fallback y diagnóstico único. | Ausencia/incompatibilidad deja cero collider de forma segura. |
| RQ-CAT-01 | Catálogo datapack server-safe, perfiles sincronizados y reutilizable en host nuevo. | Bundle atómico server→client con epoch/revisión/hash. |
| RQ-CAT-02 | Finitud, degeneración, refs, aciclicidad/límites y reload atómico. | Candidato inválido preserva snapshot aceptado y se recupera después. |
| RQ-POSE-01 | PoseProvider 20 Hz, canales declarativos, orden causal y variantes; sin renderer server. | Poses desconocidas no congelan ni colisionan; no eval por frame/observador. |
| RQ-PHYS-01 | Broadphase, convexos/OBB afines y CCD de traslación/giro/animación/escala con budget. | Sin tunneling en fixtures; remanente se detiene y diagnostica al agotar. |
| RQ-PHYS-02 | Seis caras y apoyo de cualquier normal física >= cos45, desempate y locomoción local gravedad. | Bordes/salto/escalón/inclinado/multicontacto estables. |
| RQ-TRANS-01 | Punto local, mirada intacta, cadenas base primero, vehículo raíz y asientos vanilla. | Exactamente una vez por contribución material/root-frame; varias contribuciones válidas por tick se agregan. Barcos/balsas/cofre preservados, rail negativo. |
| RQ-TRANS-02 | Ítems y bloques cayendo: expiración, agua/endurecimiento, no duplicar/colocar, yunque una vez. | Vanilla se reanuda tras liberar; proyectiles/sin física excluidos. |
| RQ-SAFE-01 | Sin doble caída/exhaustion/stats/Growth; lifecycle y anti-atrapamiento por pareja. | Sin aplastamiento/teleport/daño artificial; recuperación localizada. |
| RQ-PLACE-01 | Spawn eggs/barcos en cara alcanzada, espacio/permisos/inventario/gravedad nueva. | Sin duplicación y orientación/espacio correctos. |
| RQ-CLING-01 | Preservar Space/charge/Elytra/efecto/anchors; 30 transiciones y salto. | No reinicia gravedad ni arrastra soporte en aire. |
| RQ-CLING-02 | Preflight Gravity Changer real y cardinal dominante/empate sólo selección. | Bloqueo conserva posición/gravedad; sin `WithDownLift`. |
| RQ-NET-01 | Catálogo/pose/contacto server→client, anti-replay y predicción sólo local. | Observador sin carry ni uploads de geometría. |
| RQ-NET-02 | Paquetes unificados; `handleMovePlayer`/vehículos, allow-flight=false, 0/100/200 ms. | Sin kick, bucle de corrección ni acumulados reload/reconnect/host. |
| RQ-CAM-01 | Offset residual visual común una vez, ~100 ms/cap/fade/reset; First Person. | Sin física/bobbing doble; near clip/culling/horizonte preservados. |
| RQ-MIG-01 | Retirar minY/`automatic_top`/calibración y perfil manual; legacy JSON sólo plano unilateral+warning. | Sin anatomía falsa/AABB fallback y compat explícita. |
| RQ-CATLG-01 | Catálogo inicial/expansión, variantes/poses/informe y regeneración por versiones. | Fuente, hash, provider y estado por entrada; Happy Ghast intacto. |
| RQ-EXCL-01 | Bebés/multipartes/dormir/nadar/élitros/camello sentado excluidos. | Negativas demuestran ausencia de collider/fallback. |
| RQ-H1-01 | H1 exacto player/cow/grizzly, seis caras, inverted/lateral carry, dedicated y host/RP. | Fallo bloquea expansión/instalación; no requiere red H3. |
| RQ-H2-01 | Catálogo/lifecycle/migración/regresiones Scale. | Legacy sólo se retira con equivalencia demostrada. |
| RQ-H3-01 | Adaptador Clinging, catálogo VP26 y protocolo/red. | Se separa prueba geométrica de aceptación de animación/gameplay. |
| RQ-REL-01 | TODO aditivo, docs/changelog/compat/evidencia, artefactos+backups+remote. | Tras validación/instrucción: commit/push autorizados; sin repo/tag/release inferidos. |

## Ownership y coordinación

| Paquete | Propietario | Límites |
| --- | --- | --- |
| T1 física | architecture_audit | `AnatomyMovement`, CCD/SAT, contacto material, `GeometryProvider`, `SupportTransport`, gravedad y pruebas físicas. No networking. |
| T2 red y predicción | requirements | `AnatomyRuntime`/sesión/modo, reloj autoritativo, history, DTOs/catálogo, `PlatformConnection`/Networking/MovementReference, ACK, ledger/baseline y reconciliación. |
| T3 integración y aceptación | implementation_audit | Clinging/Gravity Changer, categorías y seams de `PlatformPhysics`/`Platforms`/estado y mixins entidad/jugador/falling block, fórmulas family/export/resources, presentación/cámara/placement y pruebas adversariales. No implementa collider/contacto/CCD ni ledger/red. |

T1 y T2 revisan capabilities y firmas antes de registrar networking. T2 aporta payloads, lifecycle, sesión/modo y ledger; T1 expone física. T3 no duplica esa decisión. La convivencia legacy es sólo migración/tests: el final nuevo usa el core común; terceras definiciones legacy se adaptan a plano unilateral dentro de ese core.

## Próximo corte por roles

1. **Terra Networking:** cerrar el contrato de modo y los fixtures/API de H1: `-PscalebrewsCompatMods`, Alex 2.1.9 real, aislamiento del ghast legacy sin catálogo, y la corrección de codecs, watermark, historia y cleanup. El muestreo se publica una vez por entidad/tick sin reconstruir catálogo, perfiles o geometría por receptor.
2. **Terra Physics:** conservar snapshot físico coherente de root y articulaciones; cubrir varias contribuciones materiales/root-frame dentro de un tick, yaw/escala sin `move`, teletransporte pequeño, y no introducir broadphase o carry de red.
3. **Terra Integration:** migrar consumidores al modo central, de modo que `BINDING` no reactive colocación, visuales, carry ni referencias legacy. Preparar H1 live player wide/slim, vaca y grizzly contra renderer original, seis caras y transporte invertido/lateral; verificar que transporte pasivo no genera walk. La ausencia de `presentationContact` conserva offset cero y diagnóstico, no un offset antiguo.
4. Sólo tras H1, exportar/probar familias adultas por tandas y migrar recursos built-in a refs anatomy sin `automatic_top`; actualizar `TODO.md`, `ANATOMY_IMPLEMENTATION.md` y `CHANGELOG.md` con evidencia de las tres Terras.
