# Arquitectura del sistema de colisiones entre entidades

Estado: arquitectura objetivo del subsistema en `chatgpt-editing`. Los requisitos normativos viven exclusivamente en [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md). El orden y el estado de implementación viven en [ENTITY_COLLISIONS_PLAN](ENTITY_COLLISIONS_PLAN.md). La evidencia ejecutada vive en [VALIDATION](VALIDATION.md).

## 1. Propósito y frontera

Scale Brews ofrece un único motor físico para que cuerpos elegibles puedan contactar, apoyarse, desplazarse y ser transportados por la anatomía de entidades vivientes. El motor no convierte ese contacto en una relación de pasajero y no reemplaza la física vanilla que no pertenece a la pareja gestionada.

La arquitectura separa deliberadamente **forma**, **pose**, **transformación raíz**, **política**, **resolución física**, **autoridad de red** y **presentación**. Esa separación es la condición que permite cubrir familias completas de mobs sin escribir una implementación por especie.

Clinging Reoriented es consumidor de la frontera pública: conserva input, cargas, Gravity Changer, efectos, persistencia y cámara propios. No posee un collider/carry/reconciliador alternativo para entidades que Scale gestiona.

## 2. Fuentes de verdad

| Información | Documento canónico |
| --- | --- |
| Qué debe hacer el sistema y cómo se acepta | `ENTITY_COLLISIONS_REQUIREMENTS.md` |
| Arquitectura, API, data model y ownership | este documento |
| Orden de implementación y estado de tareas | `ENTITY_COLLISIONS_PLAN.md` |
| Pruebas realmente ejecutadas y límites de evidencia | `VALIDATION.md` |
| Historial de versiones publicadas | `../CHANGELOG.md` |

README, GUIDE, CONFIGURATION y TODO sólo deben enlazar o resumir a nivel de producto; no mantienen otra especificación de este subsistema.

## 3. Modelo conceptual

```text
Entity binding
   │
   ├── GeometryEngine ──> ModelGeometry
   ├── PoseEngine ──────> local part transforms
   ├── RootTransformProvider ──> entity/world transform
   └── CollisionPolicy ──> eligibility/friction/state gates
                              │
                              ▼
                    Causal geometry endpoint
                              │
                ┌─────────────┴─────────────┐
                │                           │
          material interval           instantaneous queries
                │                           │
                ▼                           ▼
      continuous collision            raycast/clearance
        + contact + carry
                │
                ▼
        authoritative contact
                │
         ┌──────┴──────┐
         ▼             ▼
   local prediction   remote presentation
```

Una geometría instantánea responde «dónde está ahora». Un intervalo material certificado responde «qué espacio recorrió entre dos estados». El solver no debe inferir el segundo a partir de dos snapshots inconexos.

## 4. Capas

### 4.1 API pública

La API pública expresa capabilities y operaciones de alto nivel; no expone mapas internos, queues, networking concreto ni clases de renderer.

La superficie pública objetivo incluye equivalentes versionados de:

- `mode` / `ready` / ownership;
- consulta de soporte y estado supported;
- liberación explícita de contacto;
- clearance anatómico;
- raycast material;
- attach después de un placement validado;
- lectura de gravity frame;
- registro de adapters/engines permitidos por el SPI.

`spaceClear` consulta únicamente anatomía configurada. El consumidor continúa siendo responsable de colisión con bloques y de su propia semántica de placement/preflight. `attachAtContact` crea un anchor después de validar identidad y elegibilidad; no teletransporta el cuerpo.

La gravedad externa tiene un único owner registrable. Scale lee esa gravedad; no decide la política de input de quien la posee.

### 4.2 Data model y catálogo

Un binding declarativo asocia un tipo/variant de entidad con cuatro decisiones:

```json
{
  "entity": "example:animal",
  "geometry": {
    "engine": "scalebrews:model_part",
    "model": "example:animal"
  },
  "pose": {
    "engine": "scalebrews:mojang_keyframes"
  },
  "root_transform": "scalebrews:entity",
  "policy": {
    "enabled": true
  }
}
```

El schema exacto se cerrará en la fase de data model del plan; el ejemplo sólo ilustra ownership. Los datos seleccionan comportamiento registrado, no contienen un lenguaje de programación arbitrario.

Los datos pueden añadir:

- filtros include/exclude;
- variants/model selection;
- channels y parámetros que entiende un engine;
- estados temporalmente no soportados;
- overrides de policy/friction/ratio;
- metadata de fuente y compatibilidad.

El catálogo se construye y valida como candidato completo antes de hacerse visible. Una revisión aceptada es inmutable. El servidor distribuye un bundle preparado por revisión y los clientes verifican identidad e integridad antes de sustituir su snapshot.

### 4.3 GeometryEngine

Un `GeometryEngine` conoce una **tecnología de modelo**, no una especie. Produce `ModelGeometry`, formado por parts jerárquicas y pieces convexas identificadas.

Familias previstas:

- `ModelPart`: base general para Minecraft y mods que usan el pipeline estándar;
- `AdvancedModelBox`: familia Citadel/Alex-style mediante integración opcional reusable;
- `DisplayRig`: composición semántica de entidades display alrededor de una raíz;
- extensiones futuras como GeckoLib cuando exista un target real que las necesite.

La extracción de modelos cliente se realiza en tooling/preparación. El runtime server no recorre renderers ni reflecta modelos por query.

### 4.4 PoseEngine y channels

`PoseEngine` convierte inputs autoritativos en transforms locales. La unidad reusable es el motor de animación, no el mob.

El vocabulario base de channels contiene locomoción, orientación de cabeza, edad/tick y flags físicos comunes. Engines añaden channels tipados cuando lo necesitan. Para programas de keyframes o Citadel, la representación exportada debe ser reproducible por un runtime server-safe.

Ejemplos de engines reutilizables:

- familias procedurales vanilla;
- `AnimationDefinition`/keyframes de Mojang;
- runtime keyframe de Friends & Foes si no se reduce al anterior;
- helpers + `ModelAnimator` de Citadel/Alex;
- VM/compilación CEM sólo para catálogos visuales administrados.

Una primitiva, channel o estado desconocido no se improvisa: el endpoint queda unavailable hasta existir soporte explícito.

### 4.5 RootTransformProvider

El root contiene posición/orientación/escala del soporte en el mundo y se versiona causalmente por separado del sample de joints. Esto permite publicar root-only updates sin reevaluar una animación de huesos idéntica.

La orientación externa de Stormie's Spiders es un ejemplo de transformación raíz transversal: la geometría y pose del mob no cambian de engine; cambia la composición global.

La gravedad del cuerpo soportado no forma parte de ese root. Ambos frames se combinan en física, pero pertenecen a propietarios distintos.

### 4.6 Solver físico

El solver consume convexos y material intervals. Sus responsabilidades son:

1. broadphase espacial sobre bounds materiales;
2. narrowphase/sweep continuo;
3. sliding y mantenimiento de contacto tangencial;
4. separación/recovery localizado;
5. selección determinista cuando hay varios contactos;
6. creación/actualización de `SurfaceContact` y anchor;
7. aplicación exactamente una vez de contribuciones materiales;
8. carry de raíces y cadenas con colisión vanilla alrededor;
9. liberación/cuarentena cuando una pareja deja de ser resoluble.

El solver no conoce Alex's Mobs, Fresh Animations, Clinging ni JSON concreto.

### 4.7 Contacto y causalidad

Un contacto identifica materialmente la pieza y la cara y conserva un punto local. La identidad de stream/catalog/entity debe permitir decidir si un dato atrasado todavía pertenece a la misma realidad física.

Hay tres relojes/conceptos que no se fusionan:

- **joint sample**: momento de evaluación de pose;
- **root frame**: mutación de transform global;
- **material/publication sequence**: orden de cambios consumibles.

Un mismo tick puede contener varias contribuciones reales. «Una vez por tick» no significa «una contribución por tick».

### 4.8 Runtime y lifecycle

El runtime del servidor posee:

- catálogo aceptado;
- bindings activos;
- trackers/pose cache;
- streams y generaciones de tracking;
- publicación de endpoints/contactos;
- invalidación por unload/rebind/reload/teleport/dimensión.

El runtime cliente posee únicamente material recibido para la conexión/nivel actual y prediction de entidades localmente autoritativas. Una barrera de lifecycle invalida histories/material previos antes de aceptar otra identidad.

### 4.9 Red, prediction y reconciliación

El servidor origina catálogo, frames y contactos. El cliente no aporta geometría ni pose. El protocolo diferencia identidad de conexión, catálogo, binding, entidad y stream.

Para player/controlled vehicle se permite prediction del mismo core. La reconciliación correlaciona movimientos absolutos vanilla con transportes que el servidor confirmó. Un receipt acredita un delta aplicado; nunca lo vuelve a aplicar.

Un observador remoto no ejecuta carry físico de la entidad observada. Usa posición vanilla y presentación derivada del material confirmado.

### 4.10 Presentación y cámara

La presentación puede corregir el residual entre posición física y superficie animada, pero no es otra fuente de pose física. Debe usar el mismo endpoint/intervalo causal confirmado.

La cámara aplica sólo un offset visual acotado y comprobado contra bloques/near plane. Teleports resetean el residual; pérdidas ordinarias pueden suavizarlo. First Person y otras integraciones visuales consumen esta capa, no alteran contacto.

## 5. API frente a JSON

La regla de decisión es:

> **comportamiento reusable = API/engine; selección y metadata = datos**.

Se añade Java cuando aparece una nueva familia de comportamiento: un GeometryEngine, PoseEngine, RootTransformProvider o adapter físico genuinamente reusable. Se añade JSON/generated data cuando una entidad sólo necesita seleccionar esas piezas, mapear channels, filtrar parts o declarar variants/estados.

No se introduce una clase `TigerPose`, `OrcaPose`, etc. por rutina si ambas comparten un motor Citadel que puede interpretar el mismo vocabulario.

Si para expresar una pose empezamos a convertir JSON en un lenguaje procedural general, el comportamiento debe moverse a un engine/pose program compilado. No se diseña un segundo Java accidental dentro de JSON.

## 6. Compatibilidad general y VanillaPlus

El repositorio público de Scale Brews contiene el core y engines reutilizables. No debe fijar el modpack privado ni ids de sus componentes.

Un proyecto privado de compatibilidad de VanillaPlus puede contener:

```text
bindings/
overrides/
generated-catalog/
compat adapters realmente específicos/
coverage manifest + hashes/
```

Ese proyecto fija versiones concretas de mods/resource packs y exige su gate de cobertura. Si durante el trabajo aparece un adapter que es útil para cualquier usuario de la misma tecnología, vuelve al core o a un módulo reusable público.

La cobertura no se declara mediante una lista manual de especies. El scanner enumera los tipos del target y obliga a clasificar cada uno. Los composites que no son un `EntityType` propio necesitan un registro semántico adicional.

## 7. EMF y resource packs

La física no puede confiar en geometría enviada por un cliente. Para un pack administrado que usa CEM/EMF, la integración correcta es un **proceso de preparación** con el stack visual exacto:

```text
mods + resource packs fijados
        ↓
cliente/harness aislado
        ↓
export de geometría + pose program
        ↓
validación + hashes
        ↓
catálogo server-owned
```

Una configuración visual arbitraria no registrada no redefine la física del servidor. Según policy del target, se usa anatomía canónica o se diagnostica incompatibilidad visual.

## 8. Migración desde Living Platforms

El motor antiguo usa `PlatformDefinition.Surface`, `PlatformGeometry`, `PlatformState`, `PlatformPhysics`, networking y visual correction propios. Ese diseño no sobrevive como segundo motor al cierre de migración.

Sí pueden sobrevivir conceptos si se trasladan a la capa correcta:

- policy de categorías/ratio/fricción;
- placement semantics;
- comportamiento especial de boats/items/falling blocks;
- tests de regresión;
- un decoder legacy que convierta una superficie antigua en plano unilateral explícito.

No sobrevive `automatic_top`, la captura mediante altura/minY, un carry paralelo ni networking/reconciliation paralelos.

## 9. Paquetes objetivo

El código sobreviviente se organiza por responsabilidad, no por cronología de prototipos:

```text
io.github.r3neer.scalebrews.collision
├── api            // frontera pública y DTOs públicos mínimos
├── catalog        // codecs, bindings, catálogo, filtros, migration de datos
├── geometry       // ModelGeometry, ConvexBox, geometry engines
├── pose           // pose engines, channels, trackers y evaluación
├── physics        // broadphase, CCD, response, contacto y material-event solver
├── network        // payloads, streams, transfer, receipts/reconciliation
├── runtime        // lifecycle, activation, tracking y orchestration
└── integration    // hooks Minecraft y adapters de body/policy/placement

io.github.r3neer.scalebrews.client.collision
├── preparation    // extractors/exporters que sí pueden usar clases cliente
├── network
└── presentation   // render/camera; nunca autoridad física
```

Los mixins permanecen en el paquete de mixins, pero delegan inmediatamente en una única clase de integración del subsistema; no alojan una segunda implementación.

## 10. Estado de transición observado al iniciar esta reestructuración

La rama contiene una base causal avanzada, pero también capas simultáneas de prototipo:

- `PlatformPhysics` sigue seleccionando entre motor legacy y anatómico;
- `AnatomyMovement` concentra provider registry, histories, broadphase, solver, contacto, root tracking y carry;
- `MaterialEventDispatcher` existe como scheduler Q2 pero todavía no forma el pipeline vivo completo;
- `AnatomyTransportReceipts` prepara correlación futura de reconciliación;
- la presentación sólo certifica endpoint actual, no intervalos Q2;
- catálogo/bindings todavía reutilizan `PlatformDefinition` y rutas `entity_platform`;
- el build observado antes de esta reestructuración no compila GameTests por dos llamadas a un constructor antiguo del dispatcher.

Ese estado no es arquitectura final. El plan canónico ordena cómo reducirlo hasta las capas anteriores sin mantener compatibilidad interna con código que Git puede recuperar.
