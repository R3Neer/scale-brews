# S17 — ModelPart GeometryEngine

Estado: **CERRADO — 2026-09-13, implementación + revisión adversarial final**.

## Tesis

S17 cierra G3 tarea 3: convertir la extracción `ModelPart` que ya existe como utilidad cliente en un **`GeometryEngine` reusable de familia**, seleccionado como `scalebrews:model_part`, sin introducir ninguna dependencia de `ModelPart`, renderer o clases cliente en common/dedicated.

El resultado de preparación sigue siendo `ModelGeometry` server-safe. El servidor valida y consume datos preparados; nunca descubre renderers, recorre `ModelPart` ni reflecta modelos por query.

## Scope

### Gate

- G3 tarea 3: consolidar `ModelPart` GeometryEngine.

### Requisitos incluidos

- **FR-014**: piezas convexas identificables, separadas y jerárquicas.
- **FR-015**: `GeometryEngine` reusable por tecnología, no por especie.
- **FR-016**: soporte genérico para árboles `ModelPart`.
- **FR-018**: el contrato de engine sigue admitiendo familias futuras sin cambiar solver.
- **FR-019**: preservar cubos, jerarquía, pivots, rotaciones, transforms y huecos relevantes.
- **FR-020**: exclusiones estructurales/cosméticas y filtros declarativos siguen separadas.
- **FR-021**: geometría degenerada/no finita falla antes del solver.
- **FR-022**: ausencia de modelo/preparador no crea AABB ni `automatic_top`.
- **FR-024**: modelos estructuralmente distintos siguen identificables/versionables.
- **FR-033/FR-034**: bindings seleccionan un engine registrado y el registry público sigue siendo la frontera de comportamiento reusable.
- **NFR-003/NFR-004**: validación estricta y fail-closed.
- **NFR-005**: common/dedicated no carga ni referencia clases cliente/render.
- **NFR-028/NFR-029**: preparación reproducible y prueba contra modelos/renderers originales.
- **NFR-031**: ordinary + lane cliente reproducible.

### Invariantes que no se pueden romper

- S15/S16: catálogo, bundle, atomicidad, protocol v4, selectors y bridge legacy permanecen intactos.
- El `GeometryEngine.Request` público sigue conteniendo sólo IDs y parámetros server-safe.
- `ModelGeometry` continúa siendo el único DTO que cruza la frontera cliente/tooling → common/server.
- El servidor no recibe geometría de un cliente como autoridad live.
- No aparece una ruta runtime que reconstruya `ModelPart` por entidad/tick/query.
- El extractor no hardcodea especies; una segunda fuente `ModelPart` usa el mismo engine.
- Filtros de binding (`AnatomyFilter`) no se sustituyen por listas Java por especie dentro del engine.

### Exclusiones explícitas

- No convertir todavía `AdvancedModelBox`/Citadel/Alex en engine general; queda para G3 tarea 5.
- No implementar engines de pose ni `AnimationDefinition`; G3 tareas 4–5.
- No implementar `RootTransformProvider`; G3 tarea 6.
- No resolver variants runtime; S16 los conserva canónicamente pero su autoridad viva sigue fuera de scope.
- No integrar todavía engines arbitrarios en ejecución server live; S17 consolida **preparación de geometría**, no sustituye el bridge precomputado de S16.
- No implementar scanner/coverage, lifecycle G3.8–11, prediction G4 ni retirar Living Platforms G5.
- No modificar solver, CCD, broadphase, contacto o transporte.

## Estado actual reconstruido

1. `GeometryEngine` ya es SPI common/server-safe: `Optional<ModelGeometry> prepare(Request)` con `Request(model, parameters)` canónico.
2. `CollisionEngines` ya registra geometry engines y `CollisionBindingCatalog` rechaza IDs desconocidos.
3. No existe todavía un built-in real `scalebrews:model_part`; sólo el sentinel transitorio `precomputed_geometry` y engines fixture externos.
4. `GeometryExtractor.vanilla(...)` ya extrae un árbol `ModelPart`: jerarquía, transforms locales, cubos, exclusiones estructurales, límites, ciclos y vértices no finitos.
5. Ese extractor vive correctamente en `client.collision.preparation`, pero es una utilidad estática y los tests la llaman directamente; no pasa por `GeometryEngine`.
6. `GeometryExtractor` mezcla además la familia Alex/Citadel. Esa parte debe permanecer funcional pero no formar parte del engine S17.
7. `AnatomyExportProof` crea manualmente cow/player/familias vanilla y llama directamente a `GeometryExtractor.vanilla(...)`; después añade el `modelTransform` derivado del renderer.
8. `ModelGeometry` ya valida la representación resultante y permanece libre de clases cliente.
9. No existe una fuente genérica de preparación `model id -> fresh ModelPart/root + version + modelTransform`; sin ella un engine basado sólo en `Request` no puede materializar el modelo cliente.

## Estado objetivo

- `scalebrews:model_part` es un geometry engine built-in reconocido en common/dedicated desde inicialización normal.
- El objeto registrado en common es un dispatcher server-safe: sin preparador cliente instalado, `prepare(...)` devuelve vacío; no intenta cargar ninguna clase cliente.
- En cliente/tooling se instala exactamente una implementación `ModelPartGeometryEngine` que sólo existe bajo `client.collision.preparation`.
- La implementación cliente mantiene un registry acotado de **fuentes de preparación**. Cada fuente identifica model id, version, proveedor de un root `ModelPart` fresco y transform raíz/modelo reproducible.
- Dos o más fuentes `ModelPart` distintas se extraen mediante la misma instancia/algoritmo del engine, sin extractor Java por especie.
- `GeometryEngine.Request.model()` selecciona la fuente; parámetros desconocidos o no soportados fallan cerrados. S17 no inventa un mini-lenguaje procedural en `parameters`.
- El resultado se emite como `ModelGeometry` formato 2 con transform explícito; no depende del default histórico implícito cuando la fuente ya conoce su transform.
- La extracción `ModelPart` conserva IDs jerárquicos deterministas y orden de hijos estable, omite sólo primitivas materialmente degeneradas y marca exclusiones estructurales existentes (`hidden_or_cosmetic`, `skip_draw`).
- El proof real de cow/player atraviesa el engine registrado y sigue comparando contra vértices originales/render transform reales.
- El camino Alex existente sigue intacto y directo hasta su sprint propio.

## Diseño de frontera

### Common / dedicated

Se añadirá una pieza common pequeña, sin imports cliente, responsable de:

- constante/ID built-in `scalebrews:model_part`;
- registrar una vez el engine en `CollisionEngines` durante init common;
- actuar como dispatcher `GeometryEngine`;
- aceptar una instalación única de delegate mediante el propio tipo neutral `GeometryEngine`;
- devolver `Optional.empty()` antes de la instalación cliente.

El dispatcher no conoce `ModelPart`, Minecraft renderer, suppliers cliente ni source registry.

### Client preparation

`ModelPartGeometryEngine` implementará `GeometryEngine` y poseerá el registry de fuentes. Una fuente es tooling, no autoridad runtime: debe entregar un root nuevo/original, versión y transform reproducible.

La API concreta de fuente queda client-only. Registrar una fuente:

- valida id/version/nulls;
- rechaza duplicados;
- mantiene límites acotados;
- no extrae todavía el modelo;
- no cachea fallos como geometría válida.

`prepare(Request)`:

1. rechaza/retorna vacío para modelo desconocido o parámetros no soportados;
2. obtiene un root fresco;
3. ejecuta el extractor `ModelPart` reusable;
4. aplica el model transform explícito de la fuente;
5. devuelve `ModelGeometry` validado.

### Datos frente a comportamiento

- comportamiento de extracción = `ModelPartGeometryEngine`;
- selección de engine/modelo/filtro = `CollisionBinding`;
- versión/fuente exacta de preparación = source tooling;
- exclusión física declarativa = `AnatomyFilter` del binding;
- propiedades estructurales del modelo que nunca son material (`visible=false`, `skipDraw`, etc.) pueden seguir marcadas por el extractor.

## Plan de implementación convergido

- [x] **I1** Añadir el built-in/dispatcher common `scalebrews:model_part` y registrarlo desde init normal antes de cualquier carga de catálogo; demostrar que no referencia clases cliente.
- [x] **I2** Añadir `ModelPartGeometryEngine` client-only con instalación única en el dispatcher y registry de fuentes acotado/determinista.
- [x] **I3** Separar el algoritmo `ModelPart` de la parte Alex dentro de la preparación existente sin romper `GeometryExtractor.alex(...)`; la ruta vanilla de producción/tests debe pasar por el engine.
- [x] **I4** Definir fuentes reproducibles con `model id + version + fresh root + modelTransform`, producir `ModelGeometry` formato 2 y validar todos los outputs mediante el DTO existente.
- [x] **I5** Registrar fuentes de aceptación vanilla (cow y player wide/slim como mínimo) sin crear un extractor por especie; parámetros desconocidos/modelos desconocidos deben fallar cerrados.
- [x] **I6** Migrar `AnatomyExportProof` para que cow/player y al menos una segunda familia vanilla obtengan geometría mediante `CollisionEngines.geometry(scalebrews:model_part).prepare(...)`; conservar Alex fuera de esta migración.
- [x] **I7** Añadir fixture de una segunda fuente `ModelPart` registrada por el mismo engine y demostrar IDs/jerarquía/piezas estables, determinismo y huecos/degenerados sin fallback.
- [x] **I8** Añadir holdouts estructurales common/dedicated: ninguna clase common del engine contiene referencias a `net.minecraft.client`/`ModelPart`; sin delegate cliente el engine existe pero devuelve vacío.
- [x] **I9** Añadir fallos dirigidos: duplicate source, unknown model, unsupported parameters, source exception/invalid output y repeat prepare; ninguno deja geometría stale/cached ni muta otro source.
- [x] **I10** Ejecutar ordinary, client export/original-model proof y dedicated/common smoke; después revisión adversarial iterativa hasta una pasada completa de cero cambios productivos.

## Revisión iterativa del plan

### Pasada 1 — requisitos y arquitectura

Se descartó mover `ModelPart` al SPI common. Violaba NFR-005 y confundía engine con input concreto. `GeometryEngine.Request` permanece neutral; la materialización cliente se resuelve por delegate instalado.

### Pasada 2 — comportamiento frente a datos

Se descartó codificar `excludedParts` por especie dentro del built-in engine. Los filtros declarativos pertenecen al binding; sólo exclusiones estructurales intrínsecas del modelo permanecen en extracción.

### Pasada 3 — reproducibilidad

La fuente debe poseer versión y model transform explícitos. El proof actual aplica el transform del renderer después de extraer; dejarlo fuera del engine produciría un `ModelGeometry` incompleto y haría que dos callers pudieran preparar resultados distintos para el mismo model id.

### Pasada 4 — dedicated

Registrar el ID sólo desde `ScaleBrewsClient` haría que un dedicated rechazara bindings `scalebrews:model_part` antes incluso de consumir geometría preparada. Por eso el engine registrado vive en common como dispatcher vacío hasta que tooling cliente instala el preparador.

### Pasada 5 — scope

Alex/Citadel queda deliberadamente fuera. Compartir hoy una clase `GeometryExtractor` no significa que las dos tecnologías deban compartir el mismo `GeometryEngine`; hacerlo ampliaría S17 sobre G3 tarea 5 y ocultaría fronteras de compatibilidad distintas.

**Pasada completa final: cero cambios. Plan convergido.**

## Modelo adversarial previo

### Dedicated / classloading

- Cargar `ScaleBrews` en dedicated no puede resolver `ModelPartGeometryEngine`, `ModelPart`, renderer ni ningún tipo `net.minecraft.client`.
- Construir/resolver un binding `scalebrews:model_part` en dedicated debe validar el engine ID aunque `prepare` no esté disponible allí.
- Invocar el dispatcher sin delegate devuelve vacío, no intenta reflexión ni fallback.

### Registry / lifecycle de preparación

- instalación de dos delegates distintos falla;
- registrar dos fuentes para el mismo model id falla;
- fuente desconocida o parameters no soportados no reutilizan el último resultado;
- fallo de una fuente no contamina otra ni deja un cache parcialmente aceptado.

### Geometría

- orden de children/hash distinto produce mismos IDs/output;
- árbol cíclico/oversized, vértice no finito y primitivas degeneradas fallan/omiten según contrato actual sin llegar al solver;
- `visible=false`/`skipDraw` siguen siendo no materiales;
- un include de binding no revive equipo/cosmético estructural marcado como `Piece.excluded`.

### Reproducibilidad

- dos prepares del mismo source/version/transform producen igualdad exacta de `ModelGeometry`;
- cambiar version/transform/source identity cambia el artefacto correspondiente y nunca reutiliza uno previo;
- el engine no lee estado de una entidad/render frame live durante queries server.

### Generalidad

- cow y otra fuente vanilla atraviesan el mismo engine;
- una fuente externa/test `ModelPart` no necesita añadir otro extractor ni tocar solver;
- `GeometryExtractor.alex(...)` no se registra bajo `scalebrews:model_part` accidentalmente.

## Evidencia adversarial final

Snapshot final: `11824a121b669eeab7cd77704afad6bce4b0c254`. La segunda lectura añadió únicamente tests: escaneo de constant-pool common, ownership único del delegate, fallos de source/output, transform degenerado, aislamiento sin stale reuse, version/transform distintos, `visible=false`/`skipDraw`, no-revival por include y snapshot determinista/inmutable. Ningún holdout exigió cambiar producción.

- ordinary **34755126613** / job **103718098834**: **407/407**, `BUILD SUCCESSFUL`; artifact **10317615494**, SHA-256 `93ce1322a6186ed211bb55dbfb6ba17de5b9d47a1f02ccf9df29f5ce9091dd0c`;
- focal **34755126630**, common **103718098895**: **6/6**, artifact **10317310881**, SHA-256 `3a1c2de697169e59256a75c4b78ea54d63820a1972d1ce70d0d2d99ed8598534`;
- client preparation **103718098982**: success, artifact **10317930031**, SHA-256 `501503b498e9cb3462b9ca7515839e4dd626ccc1dd15cd3f660830ca14c9b81a`;
- original-model/export **103718098983**: success; cow/player wide/player slim, 80 comparaciones animadas por familia principal y **640 comparaciones vanilla adicionales**; artifact **10316049789**, SHA-256 `22fc133027782e612b388a53197737ef4bfbb198679a06eb44da39e4e0181d9c`.

El ruido headless de narrator/flite, ALSA/OpenAL, Realms/auth y cursor X11 no cambia la conclusión: las lanes finalizaron `success` y los proofs contractuales emitieron los sentinels esperados.

## Criterio de cierre

S17 sólo puede cerrarse si:

1. `scalebrews:model_part` es un `GeometryEngine` real y reusable, registrado en common sin dependencias cliente;
2. la implementación que toca `ModelPart` vive exclusivamente en preparación cliente;
3. dos o más fuentes `ModelPart` distintas se preparan por la misma familia y el proof original cow/player sigue equivalente a sus modelos/renderers reales;
4. unknown/invalid input falla cerrado sin AABB, legacy o resultado stale;
5. ordinary + client/original-model + dedicated/common smoke + holdouts adversariales quedan verdes;
6. una revisión completa final produce cero cambios de producción.
