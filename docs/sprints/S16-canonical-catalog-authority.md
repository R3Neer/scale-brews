# S16 — Canonical catalog authority

Estado: **PLAN CERRADO / IMPLEMENTACIÓN PENDIENTE**.

## Tesis

Al terminar S16, una revisión aceptada y sincronizada de anatomía estará definida por `CollisionBinding` canónico. `PlatformDefinition` sólo podrá participar antes de esa frontera mediante el decoder explícito de migración legacy; ningún camino live de catálogo, runtime o cliente leerá `PlatformDefinition.anatomy()` para decidir modelo, pose, filtro, policy de perfil o identidad causal.

Esto se demostrará con holdouts que inspeccionen el bundle real, la publicación atómica, el runtime server/client y el protocolo, sin asumir todavía que los `GeometryEngine`, `PoseEngine` y `RootTransformProvider` generales de G3 tareas 3–6 ya ejecuten bindings arbitrarios.

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

### Invariantes que no se pueden romper

- S15/NFR-010: un bundle se prepara una vez por revisión y se reutiliza entre receptores compatibles.
- `BINDING` sigue fail-closed y no usa el snapshot anterior para queries nuevas mientras entra un replacement.
- Los paquetes atrasados de otra epoch/revisión siguen sin sustituir el estado aceptado.
- El runtime precomputado actual mantiene la misma geometría y `PoseProvider` efectiva hasta que G3 tareas 3–6 migren la ejecución a engines genéricos.
- El motor Living Platforms conserva sus superficies legacy para el modo legacy; S16 no revive esas superficies dentro del core anatómico.

### Exclusiones explícitas

- No implementar todavía el `GeometryEngine` ModelPart general (G3.3).
- No migrar todavía las familias de `PoseEngine` vanilla/AnimationDefinition/Citadel (G3.4–5).
- No convertir todavía la física raíz a `RootTransformProvider` general (G3.6).
- No resolver variants runtime distintos del selector vacío; S16 debe preservar y sincronizar todos los selectors canónicos, pero la obtención de estado variant vivo queda fuera hasta tener su fuente autoritativa explícita.
- No rediseñar lifecycle/rebind/unload/dimension/reconnect (G3.9), salvo fences imprescindibles para conservar la atomicidad ya existente.
- No retirar todavía `PlatformDefinition` ni `PlatformPolicy` del motor Living Platforms.
- No cambiar solver, CCD, broadphase, contacto ni transporte.

## Estado actual reconstruido

1. `CollisionBinding`, `CollisionCodecs.BINDING` y `CollisionBindingCatalog` ya son la representación canónica G1. El catálogo canónico valida engines registrados, ordena selectors de forma determinista y falla cerrado ante ambigüedad.
2. `LegacyCollisionData.decode(...)` ya convierte una definición anatómica legacy en `CollisionBinding` con los IDs de compatibilidad `precomputed_geometry`, `legacy_pose_provider` y `entity_root`; las superficies legacy se mantienen como `LegacyPlanes`, no como anatomía.
3. `WorldAnatomyCatalog` sigue publicando `Snapshot(models, bindings, profiles)` donde `Binding.policy` es un `PlatformDefinition`; valida modelo/pose/filtro leyendo `profile.anatomy()`.
4. `AnatomyRuntime.Active` conserva ese binding legacy. `prepare(...)`, `acceptsIntervalIdentity(...)` y el descriptor causal vuelven a leer `PlatformDefinition.anatomy()`.
5. `AnatomyCatalogTransfer` serializa `{models, profiles}` y el cliente reconstruye `PlatformDefinition` por wire. El protocolo actual es v3.
6. `AnatomyClientNetworking` vuelve a instalar `snapshot.profiles()` en `Platforms.anatomicalDefinitions(...)` y valida/evalúa packets leyendo otra vez `binding.policy().anatomy()`.
7. `MaterialPhysicsRuntime` usa `Platforms.eligible(body,support)`; el runtime anatómico depende por ello de la tabla temporal de `PlatformDefinition` instalada por servidor/cliente.
8. Los bindings de migración canónicos todavía no tienen registrados sus tres IDs de compatibilidad en `CollisionEngines`, porque G1 sólo definió la frontera y G3 aún no consumía esos bindings.
9. El bundle S15 ya tiene ownership correcto y debe conservarse: `WorldAnatomyCatalog.Accepted` publica snapshot+prepared bundle como una unidad.

## Estado objetivo

- `WorldAnatomyCatalog` no importa ni almacena `PlatformDefinition`/`Platforms`.
- El snapshot aceptado contiene modelos precomputados, el `CollisionBindingCatalog` canónico y sólo los bindings actualmente ejecutables por el bridge de compatibilidad.
- La carga desde resources acepta `scalebrews/entity_collision`; las definiciones anatómicas legacy sólo se convierten mediante `LegacyCollisionData` antes de entrar al catálogo. Superficies legacy no entran.
- El bundle wire contiene `bindings` canónicos, no `profiles` legacy.
- El protocolo aumenta porque el bundle interno cambia de schema incompatible.
- Server y cliente derivan modelo, pose engine, filtro y policy de perfil desde `CollisionBinding`.
- El bridge transitorio resuelve `legacy_pose_provider.parameters["provider"]` hacia el `PoseProvider` preexistente, pero la identidad causal publicada usa el engine canónico del binding, no el ID interno legacy.
- Bindings canónicos cuyo engine todavía no tenga ejecución G3 disponible se conservan en el catálogo aceptado pero producen cero geometría live, no fallback.
- La elegibilidad del core anatómico usa el `CollisionBinding.policy()` activo y deja de depender de `Platforms.anatomicalDefinitions(...)`.

## Plan de implementación convergido

- [ ] **I1** Registrar los tres IDs de compatibilidad de `LegacyCollisionData` en los registries públicos de engines de forma idempotente y explícitamente transitoria; no convertirlos en fallback general.
- [ ] **I2** Reestructurar `WorldAnatomyCatalog` para aceptar/almacenar `CollisionBindingCatalog` canónico y bindings ejecutables derivados, eliminando `PlatformDefinition` de `Binding`, `Snapshot` y `replaceValidated`.
- [ ] **I3** Añadir carga ResourceManager canónica desde `scalebrews/entity_collision` y migración explícita de anatomía legacy antes de aceptación. Duplicados/ambigüedad deben fallar cerrado; `LegacyPlanes` no entran en el catálogo anatómico.
- [ ] **I4** Mantener un bridge precomputado acotado: sólo `precomputed_geometry + legacy_pose_provider + entity_root` puede convertirse al `ModelGeometryProvider` actual. Otros bindings válidos permanecen aceptados pero `UNAVAILABLE` para ejecución live hasta sus sprints de engine.
- [ ] **I5** Cambiar `AnatomyCatalogTransfer` a `{models, bindings}` usando `CollisionCodecs.BINDING`; serializar todos los selectors canónicos en orden determinista y reconstruir el mismo catálogo en cliente.
- [ ] **I6** Subir `AnatomyApi.PROTOCOL_VERSION` de 3 a 4 y actualizar la evidencia de compatibilidad. `DATA_SCHEMA_VERSION` permanece 1 porque el binding canónico sigue siendo schema v1.
- [ ] **I7** Migrar `AnatomyRuntime.Active`, `prepare`, identity checks y descriptor causal a `CollisionBinding`. El descriptor usa `geometry.model` + `pose.engine`; el `PoseProvider` legacy efectivo sólo vive dentro del bridge de ejecución.
- [ ] **I8** Añadir elegibilidad/fricción anatómica desde `CollisionPolicy` + `CollisionBinding.policy()` sin instalar `PlatformDefinition` temporales. `MaterialPhysicsRuntime` debe consultar la autoridad anatómica para soportes activos; el motor legacy conserva `Platforms.eligible`.
- [ ] **I9** Migrar `AnatomyClientNetworking` para validar/evaluar contra el binding canónico aceptado y eliminar `Platforms.anatomicalDefinitions(...)` del lifecycle cliente.
- [ ] **I10** Migrar seams de preparación/tests (`startPrepared`, `encode`, digests) a bindings canónicos; cualquier helper legacy restante debe vivir explícitamente en migración y convertir antes de llamar al catálogo.
- [ ] **I11** Eliminar imports/campos/callers muertos de `PlatformDefinition.anatomy()` en la ruta live anatómica y demostrar por inspección que `WorldAnatomyCatalog`, `AnatomyCatalogTransfer`, `AnatomyRuntime` y `AnatomyClientNetworking` no dependen ya de perfiles legacy.
- [ ] **I12** Ejecutar suite ordinary completa y una lane focal S16 que pruebe bundle canónico, protocol fence, replacement atómico, migración legacy y ausencia de fallback; después ejecutar revisión adversarial iterativa hasta una pasada cero-cambios.

## Revisión iterativa del plan

### Pasada 1 — requisitos y arquitectura

Se añadió el cambio wire/protocolo: conservar `profiles` en red habría dejado una segunda autoridad legacy aunque el servidor usara `CollisionBinding`.

### Pasada 2 — código real y consumidores

Se añadió I8 tras comprobar que `MaterialPhysicsRuntime` todavía llama a `Platforms.eligible(...)`; eliminar sólo `snapshot.profiles()` habría roto policy/elegibilidad o forzado un fallback legacy silencioso.

### Pasada 3 — dependencias y orden

Se mantiene el bridge precomputado y se excluye la ejecución de engines genéricos. Esto permite cerrar autoridad de selección antes de G3.3–6 sin fingir que un binding externo ya tiene geometría runtime.

### Pasada 4 — fail-closed y regresiones

Se fijó que bindings válidos pero todavía no ejecutables se aceptan como selección del mundo y producen ausencia de geometría, nunca un `PlatformDefinition`, AABB o `automatic_top` inventado.

### Pasada 5 — simplicidad/verificabilidad

No se introduce un segundo catálogo ni un DTO intermedio público. `CollisionBindingCatalog` sigue siendo el índice canónico; el único objeto extra permitido es un binding interno ya resuelto para el bridge precomputado.

**Pasada completa final: cero cambios. Plan convergido.**

## Modelo adversarial previo

### Autoridad dual

- Cambiar servidor a `CollisionBinding` pero seguir enviando `PlatformDefinition` por wire.
- Mantener `Platforms.anatomicalDefinitions` como segunda tabla live y que policy/filtro difieran del binding aceptado.
- Construir descriptor causal desde el provider legacy en vez del engine canónico, permitiendo que dos bindings distintos parezcan la misma identidad.

### Migración

- Perfil legacy con superficies solamente: no debe producir binding anatómico.
- Perfil legacy anatómico válido: debe producir exactamente un binding canónico equivalente.
- Candidato con anatomy y surfaces contradictorios: falla antes de publicación.
- Binding canónico explícito y migrado con selector duplicado: no puede depender de orden de archivos.

### Wire / atomicidad

- Cliente v3 frente a servidor v4 y viceversa: incompatibilidad explícita, no parse parcial.
- Bundle con key `profiles` pero protocol v4: rechazo, no interpretación legacy silenciosa.
- Binding corrupto, selector duplicado o engine inexistente en replacement: la revisión previa permanece exacta.
- Reorder/replay de fragments mantiene epoch/revision/digest fences S15.

### Ejecución transitoria

- Binding canónico válido con engine externo registrado pero todavía no integrado al runtime: snapshot lo conserva; soporte no recibe provider y no cae a legacy.
- Binding de bridge con model ausente, provider parameter ausente/malformado o pose provider inexistente: candidato/endpoint falla cerrado según la fase, nunca static fallback.
- Cambio únicamente de `pose.engine` o `geometry.model` debe cambiar identidad causal aunque el `PoseProvider` legacy efectivo coincida.

### Policy

- Enabled false, ratio justo antes/en/después del límite y friction override deben venir del `CollisionBinding.policy()` activo.
- Un soporte activo anatómico no debe consultar un `PlatformDefinition` legacy homónimo.
- El motor Living Platforms sin sesión anatómica debe conservar su comportamiento existente.

### Variants

- Selectors no vacíos deben sobrevivir encode/decode y orden canónico aunque S16 no tenga todavía fuente runtime de variant.
- Dos selectors igualmente específicos siguen fallando cerrado.
- Un selector variant no debe convertirse por accidente en default al no existir fuente variant.

### Holdout reservado

Se reservarán escenarios concretos post-implementación sobre: conflicto canonical/migration, cambio de engine con mismo provider legacy, replacement inválido después de reutilizar bundle S15, selector variant no ejecutable y ausencia total de tablas `ANATOMICAL_DEFINITIONS` durante una sesión anatómica.

## Criterio de cierre

S16 sólo puede cerrarse si:

1. ninguna clase live de catálogo/red/runtime/cliente anatómico obtiene identidad o configuración desde `PlatformDefinition.anatomy()`;
2. el wire v4 transporta bindings canónicos y conserva atomicidad/replay fences;
3. la ejecución precomputada existente sigue verde mediante un bridge explícito, no mediante fallback;
4. bindings futuros no ejecutables fallan cerrados sin impedir su conservación canónica;
5. ordinary + lane focal + holdouts adversariales quedan verdes;
6. una revisión completa final produce cero cambios.
