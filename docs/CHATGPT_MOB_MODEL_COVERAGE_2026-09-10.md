# Cobertura de mobs por familias de modelo y animación

Fecha: 2026-09-10. Análisis únicamente. No modifica implementación ni cierra gates.

## Objetivo reconstruido

Scale Brews debería aspirar, como mínimo, a cubrir todos los `LivingEntity` relevantes de Minecraft 26.2 y todos los mobs que incorpora el modpack VanillaPlus-26.2, sin convertir esa cobertura en una lista de adapters Java escritos a mano por especie.

La unidad de compatibilidad adecuada no es el mod ni el mob. Es la combinación de:

1. **representación geométrica** del modelo;
2. **motor de pose/animación** que transforma sus partes;
3. **estado/channels server-safe** necesarios para evaluar esa animación;
4. **transformación raíz** adicional, si otro mod orienta o compone la entidad;
5. pequeños **overrides por especie/variante** sólo cuando la información anterior no basta.

Esto encaja con la arquitectura que Scale Brews ya ha empezado a construir: `GeometryExtractor` tiene extractores genéricos para `ModelPart` y `AdvancedModelBox`; `PoseProviders` ya registra familias como quadruped, equine, feline, bee, villager, etc., y no solamente providers por entidad.

## Mods del pack que afectan realmente a mobs/modelos

La revisión del repositorio privado `R3Neer/VanillaPlus-26.2` identifica como relevantes para anatomía viviente: Minecraft vanilla, Alex's Mobs Continued 2.1.9, Friends & Foes 4.0.27, Wilder Wild 4.2.11, Stormie's Spiders 3.3.0 y Deeper Dark 4.4.1. Better Archeology tiene entidades propias no vivientes como su proyectil bomba, pero no aparece como una familia adicional de `LivingEntity` que Scale deba anatomizar.

El pack además usa EMF 3.3.5 y activa Fresh Animations 1.10.5, FA Details y FA Player. Por tanto hay que distinguir **anatomía canónica de gameplay** de **modelo visual sustituido por resource pack**. Ignorar esa diferencia produciría contactos físicamente correctos contra el modelo original pero visualmente desalineados con la piel que el jugador ve.

## Familias encontradas

| Fuente | Geometría | Animación/pose | Estrategia correcta |
| --- | --- | --- | --- |
| Minecraft 26.2 | `ModelPart` | familias procedurales + animaciones/keyframes vanilla | extractor `ModelPart` genérico + engines de pose por familia/animación |
| Wilder Wild | `ModelPart` / `EntityModel` | fórmulas propias y `AnimationDefinition`/`KeyframeAnimation` de Mojang | reutilizar extractor vanilla; añadir engine genérico de keyframes y channels del mod |
| Friends & Foes | `ModelPart` / `EntityModel` | `KeyframeModelAnimator`, `AnimationDefinition`, `AnimationContextTracker`, más ajustes de partes | reutilizar extractor vanilla; adapter del motor de animación F&F, no un provider Java por mob |
| Alex's Mobs Continued | `AdvancedModelBox` / `AdvancedEntityModel` | helpers Citadel + `ModelAnimator` + campos de render state | extractor AdvancedModelBox ya existente + evaluador/compilador genérico del programa de pose Alex/Citadel |
| Stormie's Spiders | conserva el modelo del mob | orientación 3D del root según normal de adhesión | adapter genérico de **root transform**, no adapter de geometría por araña |
| Deeper Dark Shockwave | raíz lógica = cerdo invisible; aspecto = `block_display` + muchos `text_display` pasajeros | transformaciones de display/datapack | familia genérica `DisplayRig`, con perfil semántico para agrupar root y displays |
| EMF/Fresh Animations | CEM/EMF sobre modelos visuales | expresiones/animación CEM | sidecar de export/compilación del stack visual activo, manteniendo autoridad server-side |

## Minecraft y la familia ModelPart

Para geometría no tiene sentido exportar a mano cada vaca, pollo, villager o mob de un mod que use el pipeline normal. `GeometryExtractor.vanilla(...)` ya recorre recursivamente cualquier `ModelPart`, sus cubos y children y conserva sus transforms. Ésa debe ser la base común.

El problema difícil no es extraer la forma estática, sino reconstruir la pose sin cargar clases cliente en un servidor dedicado. Scale ya apunta en la dirección correcta con `QuadrupedPose` y `VanillaFamilyPose`: una fórmula común puede servir a muchas especies cuando comparten convención de partes y cinemática.

El siguiente salto de generalización debería ser separar aún más los engines de animación de las especies. En vez de una clase Java por mob, una especie debería declarar o descubrir qué engine usa y qué channels necesita.

## Wilder Wild

Wilder Wild registra Firefly, Butterfly, Jellyfish, Tumbleweed, Crab, Ostrich, Zombie Ostrich, Scorched, Moobloom/FlowerCow y Penguin como entidades relevantes, además de entidades no vivientes.

Sus modelos inspeccionados usan la infraestructura normal `EntityModel` + `ModelPart`. Varios, como Crab y Penguin, consumen `AnimationDefinition` y keyframes de Mojang. Por tanto **no necesita un extractor geométrico nuevo**.

La cobertura escalable sería:

- exportar cualquier árbol `ModelPart` automáticamente;
- reconocer/evaluar `AnimationDefinition` de forma server-safe;
- obtener del servidor los AnimationState/channels requeridos;
- conservar pequeños hooks sólo para animaciones procedurales específicas que no entren en el engine común.

Eso debería permitir que añadir otro mob de Wilder Wild o de otro mod construido con el mismo patrón sea principalmente una cuestión de datos y validación, no una nueva implementación de colisiones.

## Friends & Foes

La versión moderna registra Crab, Glare, Iceologer, Illusioner, Mauler, Moobloom, Rascal, Tuff Golem y Wildfire como mobs relevantes; Ice Chunk es una entidad no viviente.

Sus modelos también se basan en `ModelPart`. El Crab examinado delega gran parte de su animación en componentes genéricos (`KeyframeModelAnimator`, `ModelPartModelAnimator`, `AnimationContextTracker`). El propio `KeyframeModelAnimator` es una máquina reutilizable sobre `AnimationDefinition`/`AnimationState`.

Esto es precisamente un caso donde escribir `CrabPose`, `GlarePose`, `MaulerPose`, etc. sería desperdiciar la estructura que ya proporciona el mod. Scale debería implementar **un adapter del motor de animación de Friends & Foes** y representar las animaciones particulares como datos/programas exportados. Sólo las transformaciones que dependan de lógica especial del mob deberían requerir metadata o un pequeño channel adapter.

## Alex's Mobs Continued

Es la mayor oportunidad de generalización y también la más importante para el pack. Gran parte de sus modelos hereda de `AdvancedEntityModel` y está formada por árboles de `AdvancedModelBox`. Scale ya dispone de un extractor reflectivo genérico para esta representación, incluyendo `cubeList`, `childModels` y herencia de escala.

La geometría, por tanto, ya está razonablemente resuelta por familia. El actual `GrizzlyPose` debe considerarse un vertical slice de prueba, **no el patrón que repetir para cada criatura de Alex's Mobs**.

`AdvancedEntityModel` define un vocabulario común de animación (`walk`, `swing`, `flap`, `bob`, `faceTarget`, progresiones de posición/rotación, etc.) y `ModelAnimator` implementa el tweening de keyframes a partir de identidad de animación + tick. Muchos modelos de Alex usan exactamente esos mecanismos.

La dirección escalable sería disponer de un **pose program Alex/Citadel** server-safe. Durante preparación, usando el JAR exacto y sus hashes, se extrae o traza el programa de animación de cada modelo y se convierte a una representación de datos/bytecode interno de Scale. El servidor ejecuta después un engine genérico con channels autoritativos. Si un modelo usa una primitiva aún no soportada, queda explícitamente `UNSUPPORTED` hasta añadir esa primitiva; no se escribe de cero toda su pose ni se cae a AABB.

La ganancia es multiplicativa: añadir una primitiva correcta puede desbloquear muchos mobs de Alex a la vez.

## Stormie's Spiders

Stormie's Spiders no debe modelarse como una nueva familia de geometría. Su problema relevante es ortogonal: cambia la **orientación tridimensional del cuerpo completo** respecto a una normal de adhesión, y su sistema puede aplicarse a más entidades mediante rotation overrides.

Su `Orientation` común construye una base local X/Y/Z a partir de la normal. Scale debería integrar esto como un `RootTransformProvider` opcional. Así el mismo modelo anatómico de Spider, Cave Spider o un mob de otro mod puede rotarse íntegramente sin saber nada nuevo de sus huesos.

La transformación raíz del soporte y la gravedad del cuerpo soportado son conceptos distintos y no deben fundirse. Clinging/Gravity Changer determina el frame del cuerpo que se apoya; Stormie's puede determinar el frame raíz del mob-soporte.

## Deeper Dark y los mobs de datapack

El Shockwave demuestra una clase distinta que un escaneo de `EntityType` no descubre semánticamente: se invoca un `minecraft:pig` invisible y el aspecto visible se construye con `block_display` y múltiples `text_display` montados sobre él.

Para el motor físico, tratarlo simplemente como cerdo sería incorrecto si el objetivo es poder apoyarse sobre la forma que el jugador ve. Tampoco conviene hardcodear Shockwave entero.

Hace falta una familia **DisplayRig** capaz de asociar una raíz viviente con displays vinculados, leer sus transforms y producir piezas físicas. Esa familia puede servir a muchos mobs implementados mediante datapacks/commands. En casos como los `text_display`, que son planos gráficos usados para fingir volumen, probablemente siga haciendo falta un perfil semántico pequeño que indique qué elementos constituyen volumen físico y cuáles son sólo decoración.

Éste es un buen ejemplo de dónde la generalización puede llegar muy lejos pero no eliminar toda metadata.

## EMF + Fresh Animations

El pack no usa únicamente los modelos originales: EMF y Fresh Animations están activos. Esto plantea una decisión de producto importante.

La ruta base más robusta sigue siendo: el servidor posee un catálogo generado de modelos originales/versionados y los clientes no pueden subir geometría. Pero para el pack administrado puede existir una segunda fase de preparación: arrancar un cliente aislado con **exactamente el stack visual del pack**, exportar la geometría CEM/EMF y compilar sus expresiones a un formato server-safe. El bundle resultante queda hash-pinned y se distribuye/sincroniza desde el servidor como cualquier otro catálogo.

Así se mantiene la autoridad de servidor sin ignorar que Fresh Animations cambia literalmente la silueta visible. Una instalación arbitraria con un resource pack desconocido no debería poder enviar su propia geometría al servidor; simplemente usaría la anatomía canónica o quedaría marcada como incompatibilidad visual según política.

## Arquitectura propuesta

### A. Coverage discovery

En build/prueba, enumerar automáticamente todos los `EntityType` que son `LivingEntity` de `minecraft` y de los namespaces presentes. Cada uno debe terminar en uno de estos estados:

- `FULL`: geometría y estados requeridos validados;
- `SAFE_PARTIAL`: especie cubierta, pero estados especiales identificados desactivan anatomía de forma segura;
- `EXCLUDED`: exclusión deliberada con razón técnica (multipart, física nativa especial, etc.);
- `UNRESOLVED`: no sabemos cubrirlo. Este estado debe hacer fallar el gate de cobertura del pack.

También debe existir un registro de **entidades semánticas compuestas** para cosas como Shockwave, porque no aparecen como un nuevo `EntityType`.

El reporte debe guardar como mínimo id, mod/version, modelo, familia geométrica, engine de pose, variants, states no soportados, motivo, hashes de fuentes/JAR y evidencia de validación.

### B. Geometry families

Mantener pocas representaciones explícitas:

- `ModelPartGeometry`: vanilla, Wilder Wild, Friends & Foes y muchos mods Fabric convencionales;
- `AdvancedModelBoxGeometry`: Alex/Citadel;
- `DisplayRigGeometry`: mobs compuestos de displays/datapacks;
- futura `GeoModelGeometry`/GeckoLib sólo si entra un mod que realmente lo necesite;
- CEM/EMF como variante/export sobre el catálogo visual, no como permiso para que el cliente improvise geometría online.

### C. Pose engines

La pose debe ser extensible por **motor**, no por especie:

- procedural vanilla/familias (`quadruped`, humanoid, equine, feline, etc.);
- Mojang `AnimationDefinition`/keyframe;
- Friends & Foes keyframe/runtime;
- Alex/Citadel helper + ModelAnimator;
- EMF/CEM expression VM para catálogo visual administrado;
- `static` sólo como elección explícita, nunca fallback.

Los datos particulares de cada mob pueden ser generados/exportados y versionados. La lógica Java específica debe ser el último recurso.

### D. State/channel adapters

Crear un vocabulario común de channels autoritativos: walk phase/speed, yaw/pitch de cabeza, age, ground, crouch, sprint, attack, baby, etc. Después cada motor añade sus channels de familia: AnimationState, animation id/tick, progress values, variant flags y similares.

El export/proof debería detectar automáticamente qué inputs necesita un pose program. Si un input sólo existe en cliente y no puede derivarse autoritativamente, ese estado no se considera soportado. No se arregla dejando que el cliente mande la pose.

### E. Root/composition layer

La pose local y la transformación global deben permanecer desacopladas. Sobre el esqueleto evaluado se aplican, en orden definido, traslación/yaw/scale de entidad, frame de gravedad/orientación del soporte y adapters como Stormie's. Los rigs de display se componen respecto a su raíz semántica.

## Qué debería seguir necesitando trabajo por especie

Aunque el motor sea familiar, no todo debe adivinarse. Son razonables overrides declarativos pequeños para:

- filtrar sombreros, equipo, flores, micrófonos, segunda capa de skin y piezas decorativas ambiguas;
- elegir entre modelos/variants reales (wide/slim, formas infladas, adulto/bebé, etc.);
- declarar estados especiales no soportados temporalmente;
- adaptar raíces con física excepcional o multipart;
- identificar composites de datapack;
- pequeñas diferencias de channels cuando el motor común no puede descubrirlas con seguridad.

La regla debería ser: **metadata por especie cuando sea información, Java por familia cuando sea comportamiento**. Escribir una implementación completa por cada mob sería una señal de que la abstracción está mal colocada.

## “Todos los mobs” no es igual a “todas las poses”

Hay dos objetivos distintos. Puede lograrse cobertura de todas las especies ordinarias mucho antes que equivalencia perfecta de cada estado especial de cada criatura. Para el gate mínimo del pack, ningún mob adulto en idle/walk/head-look normal debería quedar sin anatomía. Estados excepcionales todavía podrían ser `SAFE_PARTIAL`, fallando cerrados mientras duren.

El objetivo final del pack puede entonces convertir progresivamente esos `SAFE_PARTIAL` a `FULL`. Esto evita la alternativa absurda de fingir un collider AABB sólo para poder poner una casilla verde.

## Orden recomendado de implementación futura

| Orden | Trabajo | Motivo |
| --- | --- | --- |
| 1 | Generador automático de coverage report y gate `UNRESOLVED=0` | hace imposible olvidar silenciosamente mobs del pack |
| 2 | Consolidar `ModelPart` genérico y engine Mojang keyframe | cubre Minecraft + gran parte de Wilder Wild y crea infraestructura reutilizable |
| 3 | Adapter del runtime keyframe de Friends & Foes | cubre casi todo el mod sin providers por especie |
| 4 | Generalizar Alex/Citadel desde el proof del grizzly a pose programs | mayor número de mobs externos y mayor ahorro frente a código manual |
| 5 | Root transform de Stormie's | pequeño en tamaño, transversal a cualquier geometría compatible |
| 6 | DisplayRig/composites | cubre Shockwave y futuros mobs datapack |
| 7 | Catálogo visual EMF/CEM administrado | alinea la física con Fresh Animations sin ceder autoridad al cliente |
| 8 | Barrido de variants/poses especiales hasta `FULL` | cierre real del pack, con excepciones explícitas |

Este orden se debe integrar con H0-H4 y Q1/Q2 del plan anatómico existente. Añadir familias no debe adelantarse al cierre del pipeline causal/material que garantiza que una geometría animada se consume correctamente.

## Criterio de éxito

Scale Brews no debería declarar “compatibilidad con VanillaPlus” porque existe un JSON para cada especie. Debería poder generar automáticamente un informe donde **todos los mobs presentes resuelvan mediante una familia conocida y cero entradas queden `UNRESOLVED`**, con hashes y pruebas reproducibles.

La conclusión de esta auditoría es que este objetivo es viable sin trabajo manual por mob. El pack actual concentra la mayoría de su fauna en unas pocas infraestructuras de modelado/animación muy repetidas. La mayor cantidad de trabajo está en construir bien los engines comunes y la extracción de estado autoritativo, no en escribir cien colliders distintos.

## Referencias de código inspeccionadas

- Scale Brews: `GeometryExtractor.java`, `PoseProviders.java`, `QuadrupedPose.java`, `VanillaFamilyPose.java`.
- VanillaPlus-26.2: manifests de Alex's Mobs Continued, Friends & Foes, Wilder Wild, Stormie's Spiders, Deeper Dark, EMF/Fresh Animations y stack de resource packs activo.
- Wilder Wild: registro de entidades y modelos/animaciones `EntityModel`/`ModelPart`/`AnimationDefinition`.
- Friends & Foes: registro de entidades, `CrabEntityModel`, `KeyframeModelAnimator`.
- Alex's Mobs Continued: `AdvancedEntityModel`, `AdvancedModelBox`, `ModelAnimator` y modelos que reutilizan esos componentes.
- Stormie's Spiders: `Orientation` y pipeline de rotation override.
- Deeper Dark: `shockwave/spawn.mcfunction` y `shockwave/texture.mcfunction`.

No se ejecutaron builds, Minecraft ni extractores durante este análisis. Las afirmaciones de cobertura futura son de arquitectura y deben convertirse en pruebas antes de marcar requisitos como cerrados.
