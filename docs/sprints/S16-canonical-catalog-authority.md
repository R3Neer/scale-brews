# S16 — Canonical catalog authority

Estado: **CERRADO — 2026-09-13, tras reapertura adversarial post-cierre**.

## Tesis

S16 cierra G3 tarea 1: una revisión aceptada y sincronizada de anatomía queda definida por `CollisionBinding` canónico. `PlatformDefinition` sólo participa antes de esa frontera mediante migración legacy explícita; ningún camino live de catálogo, runtime o cliente lee `PlatformDefinition.anatomy()` para decidir modelo, pose, filtro, policy de perfil o identidad causal.

La ejecución precomputada existente se conserva mediante un bridge acotado, sin fingir todavía que los `GeometryEngine`, `PoseEngine` y `RootTransformProvider` generales de G3 tareas 3–6 ejecuten bindings arbitrarios.

## Scope

### Gate

- G3 tarea 1: sustituir `WorldAnatomyCatalog` acoplado a perfiles legacy por catálogo/binding canónico.

### Requisitos incluidos

- **FR-023**: la migración legacy no convierte superficies unilaterales en anatomía.
- **FR-033**: la selección aceptada usa `CollisionBinding` (`entity/variant -> geometry + pose + root + policy`).
- **FR-035**: catálogo server/world-owned y sustitución atómica en cliente.
- **FR-036**: candidato inválido conserva exactamente la revisión aceptada previa.
- **FR-037**: revision/epoch y los identificadores de binding forman parte de la identidad causal pertinente.
- **NFR-012/NFR-013**: se conservan límites de catálogo/protocolo, digest y fragmentación ya demostrados.

### Invariantes preservados

- S15/NFR-010: un bundle se prepara una vez por revisión y se reutiliza entre receptores compatibles.
- `BINDING` sigue fail-closed y no usa el snapshot anterior para queries nuevas mientras entra un replacement.
- Los paquetes atrasados de otra epoch/revisión no sustituyen el estado aceptado.
- El runtime precomputado mantiene la misma geometría y `PoseProvider` efectiva hasta que G3 tareas 3–6 migren la ejecución a engines genéricos.
- Living Platforms conserva sus superficies legacy sólo para el modo legacy; S16 no las revive dentro del core anatómico.

### Exclusiones explícitas

- No implementar todavía el `GeometryEngine` ModelPart general (G3.3).
- No migrar todavía las familias de `PoseEngine` vanilla/AnimationDefinition/Citadel (G3.4–5).
- No convertir todavía la física raíz a `RootTransformProvider` general (G3.6).
- No resolver variants runtime distintos del selector vacío; S16 conserva y sincroniza todos los selectors canónicos, pero la obtención de estado variant vivo queda fuera hasta tener su fuente autoritativa explícita.
- No rediseñar lifecycle/rebind/unload/dimension/reconnect (G3.9), salvo fences imprescindibles para conservar la atomicidad ya existente.
- No retirar todavía `PlatformDefinition` ni `PlatformPolicy` del motor Living Platforms.
- No cambiar solver, CCD, broadphase, contacto ni transporte.

## Estado inicial reconstruido

1. `CollisionBinding`, `CollisionCodecs.BINDING` y `CollisionBindingCatalog` ya eran la representación canónica G1.
2. `LegacyCollisionData.decode(...)` ya convertía anatomía legacy en binding canónico y mantenía superficies legacy como `LegacyPlanes`.
3. `WorldAnatomyCatalog` seguía publicando `Snapshot(models, bindings, profiles)` y obtenía autoridad de `PlatformDefinition`.
4. `AnatomyRuntime` y el descriptor causal volvían a leer anatomía legacy.
5. `AnatomyCatalogTransfer` enviaba `{models, profiles}` por protocolo v3.
6. `AnatomyClientNetworking` reinstalaba perfiles legacy en una tabla temporal de `Platforms`.
7. La elegibilidad material terminaba dependiendo de esa tabla temporal.
8. Los tres IDs de compatibilidad legacy todavía no estaban registrados en `CollisionEngines`.
9. El bundle S15 ya tenía ownership correcto y debía conservarse.

## Estado final objetivo

- `WorldAnatomyCatalog` almacena modelos, `CollisionBindingCatalog` y sólo el subconjunto ejecutable por el bridge precomputado; no almacena `PlatformDefinition`.
- `scalebrews/entity_collision` entra directamente como binding canónico y la anatomía legacy cruza una migración explícita antes de publicación.
- Superficies legacy no se convierten en anatomía.
- El wire v4 contiene `{models, bindings}` y rechaza `profiles`.
- El protocolo público es v4; `DATA_SCHEMA_VERSION` permanece 1.
- Server y cliente derivan modelo, pose-engine, filtro, policy e identidad causal del binding canónico.
- El `PoseProvider` legacy efectivo queda privado dentro del bridge de compatibilidad; la identidad publicada usa `geometry.model` + `pose.engine` canónicos.
- Bindings válidos pero todavía no ejecutables se conservan en el catálogo canónico y no reciben provider ni fallback.
- `AnatomyBindingState` acompaña el provider vivo con su `CollisionBinding` canónico cuando existe.
- La ruta de policy/elegibilidad consulta el binding vivo y una sesión anatómica activa falla cerrada antes de cualquier fallback Living Platforms.
- Los registros manuales/prepared pueden heredar únicamente la policy canónica de la revisión aceptada mediante `AnatomyRuntime.catalogBinding(...)`, sin convertirse en bindings causales runtime.
- La tabla temporal `Platforms.anatomicalDefinitions(...)` desapareció por completo.
- **Todo binding del candidato, incluido un selector variant todavía no ejecutable, valida las referencias que pertenecen al bridge antes de la publicación atómica.**

## Plan de implementación convergido

- [x] **I1** Registrar los tres IDs de compatibilidad de `LegacyCollisionData` en los registries públicos de engines de forma idempotente y transitoria, sin fallback general.
- [x] **I2** Reestructurar `WorldAnatomyCatalog` sobre `CollisionBindingCatalog`, eliminando `PlatformDefinition` de `Binding`, `Snapshot` y publicación live, y validar el candidato canónico completo antes del swap.
- [x] **I3** Añadir carga canónica `scalebrews/entity_collision` y migración legacy explícita antes de aceptación.
- [x] **I4** Mantener un bridge precomputado limitado a `precomputed_geometry + legacy_pose_provider + entity_root`; el resto queda aceptado pero no ejecutable.
- [x] **I5** Cambiar `AnatomyCatalogTransfer` a `{models, bindings}` y preservar selectors en orden canónico.
- [x] **I6** Subir `AnatomyApi.PROTOCOL_VERSION` a 4 manteniendo `DATA_SCHEMA_VERSION = 1`.
- [x] **I7** Migrar runtime, identity checks y descriptor causal a `CollisionBinding`.
- [x] **I8** Obtener elegibilidad/fricción anatómica del binding canónico sin instalar perfiles temporales.
- [x] **I9** Migrar `AnatomyClientNetworking` a bindings canónicos y retirar lifecycle basado en `Platforms.anatomicalDefinitions(...)`.
- [x] **I10** Migrar seams y fixtures (`startPrepared`, `encode`, digests) para que cualquier entrada legacy convierta antes de cruzar la frontera autoritativa.
- [x] **I11** Retirar callers/imports muertos de `PlatformDefinition.anatomy()` en catálogo, red, runtime y cliente.
- [x] **I12** Ejecutar ordinary, lane focal y revisión adversarial hasta una pasada final sin cambios de producción.

## Revisión iterativa de implementación

### Primera reapertura — migración incompleta

La primera implementación dejó el catálogo en `CollisionBinding` pero consumidores server/client todavía hablaban el contrato `{profiles, PlatformDefinition}`. La sonda CI falló por compilación y obligó a migrar la frontera completa en lugar de añadir adaptadores de compatibilidad a producción.

### Segunda reapertura — autoridad dual de policy

Tras retirar la tabla temporal, la policy necesitaba viajar con el mismo lifecycle que provider/descriptor. `774de560c0076802019924379b0b5cc37e7b51d9` añadió el binding canónico a `AnatomyBindingState` y `8704a30e6c11e147969937d7f58b9a013be83e3c` enrutó policy/elegibilidad por ese estado vivo.

### Tercera reapertura — fallback legacy silencioso

`744c777a5e8016d22832bc028edea9e6bd846958` cerró el caso en que una sesión anatómica activa sin binding ejecutable podía caer a una definición Living Platforms homónima. Desde entonces una selección disabled, variant-only o todavía no ejecutable falla cerrada.

### Cuarta reapertura — bindings manuales/prepared

El holdout final de policy demostró que un provider manual dentro de una sesión `startPrepared` podía perder la policy canónica y volver a depender del contexto legacy global. `3a3503b1cba944933eaaf3cf010e8be981ed7d8d` sustituyó los helpers duplicados de policy por el seam estrecho `catalogBinding(...)`; el registro manual toma esa selección canónica sin adquirir descriptor causal ni ejecución runtime.

### Quinta reapertura — selector variant elude validación integral

El holdout adversarial `variantBridgeReferencesMustValidateBeforeAtomicPublication`, añadido en `e3e49ac2a80ea1729c20dfe75ef3a7095e1ea25e`, construye un binding del bridge precomputado con selector variant no vacío y referencia a un modelo inexistente. La implementación actual sólo valida el resultado de `canonical.resolve(entity, Map.of())`; como el selector variant no coincide con el selector vacío, el binding inválido no entra en `references` y el candidato se publica.

Esto viola FR-033 y FR-036: que el estado variant vivo quede fuera de S16 no convierte sus datos declarativos en una zona sin validar. El catálogo candidato debe validarse completo antes de hacerse visible, aunque determinados selectors todavía no sean ejecutables en runtime.

Evidencia roja: focal S16 run **34753107721**, job **103712855904** sobre `e3e49ac2...`: compilación correcta, **1/6 required GameTests failed**, exactamente el nuevo holdout. La aserción falla porque `catalog.replace(...)` acepta el candidato en lugar de rechazarlo. El cierre automático posterior basado en la evidencia anterior queda por tanto invalidado.

### Reparación de la quinta reapertura — candidato completo antes del selector runtime

`5cff913349a4fc64921a81e0d8236e5aae235ac9` cambió `WorldAnatomyCatalog.replaceValidated(...)` para validar primero **todos** los bindings del candidato. Todo binding que toque parcialmente los IDs del bridge se rechaza; todo bridge completo valida referencia de modelo, `provider`, compatibilidad del provider y filtro en objetos temporales. Sólo después se resuelve `variant={}` para construir el subconjunto actualmente ejecutable. Así un variant declarativo inválido no puede publicarse y un variant válido tampoco se convierte accidentalmente en default.

La publicación sigue siendo atómica: `current` sólo cambia después de validar geometría, providers/filtros y preparar el bundle wire; un rechazo conserva exactamente el `Snapshot` y `PreparedBundle` anteriores.

### Sexta pasada adversarial — conflicto de migración y semántica wire de variants

`d218adc84a0180e02dd5908114bacf010c4ee86e` añadió dos holdouts sin tocar producción: conflicto entre binding canónico y binding legacy migrado para el mismo selector, y round-trip v4 de un selector variant sin inventar ejecución default. También permanecen el rechazo wire inválido y la restauración client-style mediante `rejectPending()`.

Los **8/8** métodos de `S16CanonicalCatalogAuthorityTests` pasan sobre ese snapshot. Esta ronda adversarial no exigió ningún cambio de producción después de `5cff913...`.

### Revisión final

La revisión post-reparación volvió a recorrer catálogo, transferencia, runtime/server, cliente, binding state y frontera Living Platforms. No apareció otra ruta `PlatformDefinition.anatomy()` live, segundo owner del catálogo, fallback legacy desde sesión anatómica ni selector variant ejecutado como default. No se identificó otro cambio de producción. La última pasada adversarial amplió tests y quedó verde sin modificar producto.

## Evidencia de cierre anterior, conservada como histórica

Snapshot de evidencia anterior: `0b7a8940e783f3b8e08128f9d95fa04688376e79`.

- ordinary run **34752369790**, job **103710940047**: **398/398 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **10315852333**, SHA-256 **a1de8633e76ed2df2516b47fe549a83e4aff5eaf208765cca34088db4737f036**.
- focal S16 run **34752369719**, job **103710939888**: **5/5 required S16 GameTests passed**; artifact **10316377128**, SHA-256 **429aac1871a8fcb108766c12f0b7dba9eeeac06bf018408be181fbf7c5eba205**.
- adversarial post-cierre run **34753107721**, job **103712855904**: **1/6 required S16 GameTests failed** sobre `e3e49ac2...`; el candidato variant-only con modelo inexistente fue publicado cuando debía rechazarse.

Las dos primeras ejecuciones siguen siendo evidencia histórica útil, pero ya no satisfacen el criterio de cierre vigente porque no contenían el holdout post-cierre.

Esta reapertura no amplía S16 a G3.3–G3.12: sigue sin probar engines generales ModelPart/pose/root, coverage scanner, lifecycle completo ni prediction/reconciliation.


## Evidencia final renovada

Último cambio productivo S16: `5cff913349a4fc64921a81e0d8236e5aae235ac9`. Snapshot final con la campaña adversarial ampliada: `d218adc84a0180e02dd5908114bacf010c4ee86e`.

- ordinary run **34753580865**, job **103714077843**: **401/401 required GameTests passed**, `BUILD SUCCESSFUL`; artifact **10315699079**, SHA-256 **`9cd3c32a251988793cd2df1bcb145b0003d2c4b8e149e65832e0473b6d8a004e`**;
- focal S16 run **34753580858**, job **103714077812**: **8/8 required S16 GameTests passed**, `BUILD SUCCESSFUL`; artifact **10316528568**, SHA-256 **`f4ca1104ee19347997d456e5719da9ea5fd107fd1941eb39e9b8c7d9e2bb5f7a`**;
- prepared proof sobre `5cff913...`: el primer intento `34753380100`/`103713567025` cayó en un A9/S09 de `TemporalResponse` ajeno al catálogo; el rerun exacto del mismo job/SHA, **103714031864**, completó export original + prepared server con éxito. Se conserva como incidencia no reproducible y no se usa para sustituir la evidencia ordinary/focal de S16.

La ronda `d218adc...` añadió nuevos holdouts y no requirió cambios productivos. Ésta es la pasada adversarial de cero cambios exigida para cierre.

## Criterio de cierre

1. [x] ninguna clase live de catálogo/red/runtime/cliente anatómico obtiene identidad o configuración desde `PlatformDefinition.anatomy()`;
2. [x] el wire v4 transporta bindings canónicos y conserva atomicidad/replay fences;
3. [x] la ejecución precomputada existente sigue verde mediante un bridge explícito, no mediante fallback;
4. [x] bindings futuros no ejecutables fallan cerrados sin impedir su conservación canónica;
5. [x] todos los selectors del candidato validan sus referencias aplicables antes del swap, y ordinary + lane focal + holdouts adversariales quedan verdes;
6. [x] la revisión completa final produce cero cambios de producción después de la reparación.

**S16 queda cerrado. G3 tarea 1 está renovadamente verde tras la reapertura post-cierre; G3 continúa con tareas 3–12.**
