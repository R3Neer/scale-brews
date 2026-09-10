# Arquitectura del sistema de colisiones entre entidades

Estado: arquitectura objetivo del subsistema. Los requisitos normativos viven exclusivamente en [ENTITY_COLLISIONS_REQUIREMENTS](ENTITY_COLLISIONS_REQUIREMENTS.md), el orden y estado de implementación en [ENTITY_COLLISIONS_PLAN](ENTITY_COLLISIONS_PLAN.md), y la evidencia ejecutada en [VALIDATION](VALIDATION.md).

## 1. Propósito y frontera

Scale Brews ofrece un único motor físico para que cuerpos elegibles contacten, se apoyen, se desplacen y sean transportados por la anatomía de entidades vivientes. El contacto no crea una relación de pasajero y no sustituye la física vanilla fuera de la pareja gestionada.

La arquitectura separa **forma**, **pose**, **transformación raíz**, **política**, **resolución física**, **autoridad de red** y **presentación**. Esa separación permite cubrir familias completas de mobs sin escribir una implementación por especie.

Clinging Reoriented es consumidor de la frontera pública: conserva input, cargas, Gravity Changer, efectos, persistencia y cámara propios. No posee un collider, carry o reconciliador alternativo para entidades que Scale gestiona.

## 2. Fuentes de verdad

| Información | Documento canónico |
| --- | --- |
| Qué debe hacer el sistema y cómo se acepta | `ENTITY_COLLISIONS_REQUIREMENTS.md` |
| Arquitectura, API, data model y ownership | este documento |
| Orden de implementación y estado de tareas | `ENTITY_COLLISIONS_PLAN.md` |
| Pruebas realmente ejecutadas y límites de evidencia | `VALIDATION.md` |
| Historial de versiones publicadas | `../CHANGELOG.md` |

README, GUIDE, CONFIGURATION y TODO sólo enlazan o resumen a nivel de producto; no mantienen otra especificación del subsistema.

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

Una geometría instantánea responde «dónde está ahora». Un intervalo material certificado responde «qué espacio recorrió entre dos estados». El solver no infiere el segundo a partir de dos snapshots inconexos.

## 4. Capas

### 4.1 API pública

La API pública expresa capabilities y operaciones de alto nivel; no expone mapas internos, colas, networking concreto ni clases de renderer.

La superficie pública incluye equivalentes versionados de:

- `mode`, `ready` y ownership;
- consulta de soporte y estado supported;
- liberación explícita de contacto;
- clearance anatómico;
- raycast material;
- attach después de un placement validado;
- lectura de gravity frame;
- registro de adapters/engines permitidos por el SPI.

`spaceClear` consulta únicamente anatomía configurada. El consumidor sigue siendo responsable de bloques y de su propia semántica de placement/preflight. `attachAtContact` crea un anchor después de validar identidad y elegibilidad; no teletransporta el cuerpo.

La gravedad externa tiene un único owner registrable. Scale la lee; no decide la política de input del propietario.

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

Los datos seleccionan comportamiento registrado, no contienen un lenguaje procedural arbitrario. Pueden aportar filtros include/exclude, variants, channels y parámetros de engine, estados temporalmente no soportados, overrides de policy/friction/ratio y metadata de fuente/compatibilidad.

El catálogo candidato se valida completo antes de hacerse visible. Una revisión aceptada es inmutable. El servidor prepara un bundle una vez por revisión; los clientes verifican identidad e integridad antes de sustituir atómicamente su snapshot.

### 4.3 GeometryEngine

Un `GeometryEngine` conoce una **tecnología de modelo**, no una especie. Produce `ModelGeometry`, formado por parts jerárquicas y pieces convexas identificadas.

Familias previstas:

- `ModelPart`: Minecraft y mods que usan el pipeline estándar;
- `AdvancedModelBox`: familia Citadel/Alex-style mediante integración reusable;
- `DisplayRig`: composición semántica de entidades display alrededor de una raíz;
- extensiones futuras como GeckoLib cuando exista un target real.

La extracción que necesita clases cliente ocurre en tooling/preparación. El runtime de servidor no recorre renderers ni reflecta modelos por query.

### 4.4 PoseEngine y channels

`PoseEngine` convierte inputs autoritativos en transforms locales. La unidad reusable es el motor de animación, no el mob.

El vocabulario base contiene locomoción, orientación de cabeza, edad/tick y flags físicos comunes. Engines añaden channels tipados cuando los necesitan. Para keyframes o Citadel, el programa exportado debe poder ejecutarse en un runtime server-safe.

Familias previstas incluyen poses procedurales vanilla, `AnimationDefinition` de Mojang, adapters reutilizables de frameworks externos y pose programs compilados. Una primitiva, channel o estado desconocido no se improvisa: el endpoint queda `UNAVAILABLE`.

### 4.5 RootTransformProvider

El root contiene posición, orientación y escala del soporte y se versiona causalmente por separado del sample de joints. Un root-only update no obliga a reevaluar joints idénticos.

La orientación externa de Stormie's Spiders es un ejemplo transversal: no cambia geometry/pose engine, sólo la composición global. La gravedad del cuerpo soportado es un estado independiente de la orientación raíz del soporte.

### 4.6 Solver físico

El solver consume convexos e intervalos materiales. Sus responsabilidades son:

1. broadphase espacial sobre bounds materiales;
2. narrowphase y sweep continuo;
3. sliding y mantenimiento de contacto tangencial;
4. separación/recovery localizado;
5. selección determinista de multicontacto;
6. creación/actualización de `SurfaceContact` y anchor;
7. aplicación exactamente una vez de contribuciones materiales;
8. carry de raíces/cadenas con colisión vanilla alrededor;
9. liberación o cuarentena cuando una pareja deja de ser resoluble.

El solver no conoce Alex's Mobs, Fresh Animations, Clinging ni JSON concreto.

### 4.7 Contacto y causalidad

Un contacto identifica materialmente soporte, pieza, cara y punto local. La identidad de conexión, catálogo, binding, entidad y stream debe permitir decidir si un dato atrasado sigue perteneciendo a la misma realidad física.

No se fusionan estos conceptos:

- **joint sample**: evaluación de pose;
- **root frame**: mutación de transform global;
- **material/publication sequence**: orden de cambios consumibles.

Un tick puede contener varias contribuciones reales. «Exactamente una vez» se aplica a cada contribución, no a un tick agregado a ciegas.

### 4.8 Runtime y lifecycle

El servidor posee catálogo aceptado, bindings activos, trackers/pose cache, generaciones de tracking, publicación de endpoints/contactos e invalidación por unload, rebind, reload, teleport y dimensión.

El cliente posee sólo material recibido para la conexión/nivel actual y prediction de entidades localmente autoritativas. Una barrera de lifecycle invalida histories/material previos antes de aceptar otra identidad.

### 4.9 Red, prediction y reconciliación

El servidor origina catálogo, frames y contactos. El cliente no aporta geometría ni pose. Para player/controlled vehicle se permite prediction del mismo core. La reconciliación correlaciona movimiento vanilla con transportes confirmados; un receipt acredita un delta aplicado y nunca lo vuelve a aplicar.

Un observador remoto no ejecuta carry físico local de la entidad observada. Usa posición vanilla y presentación derivada del material confirmado.

### 4.10 Presentación y cámara

La presentación puede corregir el residual entre posición física y superficie animada, pero no es otra fuente de pose física. Usa el mismo endpoint/intervalo causal confirmado.

La cámara aplica sólo offset visual acotado y validado contra bloques/near plane. Teleports resetean el residual; pérdidas ordinarias pueden suavizarlo. Integraciones visuales consumen esta capa y no alteran contacto.

## 5. API frente a JSON

La regla es:

> **comportamiento reusable = API/engine; selección y metadata = datos**.

Se añade Java cuando aparece una nueva familia de comportamiento. Se añade JSON/generated data cuando una entidad sólo necesita seleccionar engines, mapear channels, filtrar parts o declarar variants/estados.

No se introduce una clase `TigerPose`, `OrcaPose`, etc. si ambas pueden interpretarse mediante un engine común. Si el JSON empieza a convertirse en un lenguaje procedural general, ese comportamiento pasa a un engine o pose program compilado.

## 6. Compatibilidad general y VanillaPlus

El repositorio público de Scale Brews contiene core y engines reutilizables. No fija el modpack privado ni IDs específicos de VanillaPlus.

El proyecto privado de compatibilidad puede contener:

```text
bindings/
overrides/
generated-catalog/
compat adapters realmente específicos/
coverage manifest + hashes/
```

Ese proyecto fija versiones concretas de mods/resource packs y exige su gate de cobertura. Un adapter útil para cualquier usuario de la misma tecnología vuelve al core o a un módulo reusable público.

La cobertura no se declara mediante una lista manual de especies. El scanner enumera los tipos del target y obliga a clasificar cada uno. Los composites que no son un `EntityType` propio requieren un registro semántico adicional.

## 7. EMF y resource packs

La física no confía en geometría enviada por un cliente. Para un pack administrado que usa CEM/EMF, la integración correcta es preparación con el stack visual exacto:

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

El motor antiguo usa `PlatformDefinition.Surface`, `PlatformGeometry`, `PlatformState`, `PlatformPhysics`, networking y corrección visual propios. Ese diseño no sobrevive como segundo motor al cierre de migración.

Pueden sobrevivir, trasladados a la capa correcta: policy de categorías/ratio/fricción, placement semantics, comportamiento especial de boats/items/falling blocks, tests de regresión y un decoder que traduzca una superficie legacy a plano unilateral explícito.

No sobreviven `automatic_top`, la captura por altura/minY, un carry paralelo ni networking/reconciliation paralelos.

## 9. Paquetes objetivo

El código se organiza por responsabilidad, no por cronología de prototipos:

```text
io.github.r3neer.scalebrews.collision
├── api            // frontera pública y DTOs públicos mínimos
├── catalog        // codecs, bindings, catálogo, filtros y migración de datos
├── geometry       // ModelGeometry, ConvexBox y geometry engines
├── pose           // pose engines, channels, trackers y evaluación
├── physics        // broadphase, CCD, response, contacto y material-event solver
├── network        // payloads, streams, transfer y receipts/reconciliation
├── runtime        // lifecycle, activation, tracking y orchestration
└── integration    // hooks Minecraft y adapters de body/policy/placement

io.github.r3neer.scalebrews.client.collision
├── preparation    // extractors/exporters que pueden usar clases cliente
├── network
└── presentation   // render/camera; nunca autoridad física
```

Los mixins permanecen en el paquete de mixins, pero delegan inmediatamente en una única clase de integración del subsistema; no alojan una segunda implementación.
