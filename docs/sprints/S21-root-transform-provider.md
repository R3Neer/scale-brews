# S21 — Generic root-transform authority

Estado: **CLOSED — INDEPENDENT ADVERSARIAL ACCEPTANCE COMPLETE**.

Rol principal: **IMPLEMENTER**. Holdouts, mutantes y segunda lectura siguen siendo del adversario.

S21 ejecuta G3.6 y no amplía el contrato funcional. Congela FR-030, FR-031, FR-032 y FR-033 sobre la arquitectura S15–S20.

## 1. Requisitos congelados

- **FR-030** — posición raíz, orientación/yaw, escala y orientación externa son autoridad distinta de los joints locales. Un cambio sólo de root obtiene identidad causal nueva sin reevaluar joints.
- **FR-031** — un `RootTransformProvider` genérico puede introducir orientación transversal externa sin reimplementar geometría ni pose; el mismo catálogo debe funcionar con otro proveedor de root.
- **FR-032** — la orientación externa del root no se confunde con la gravedad física del body. Son autoridades independientes.
- **FR-033** — el binding canónico selecciona el root provider por id.
- NFR common/dedicated: ninguna implementación built-in de root depende de `net.minecraft.client` ni de un mod externo.
- NFR-009: el cache de joints continúa dependiendo sólo de los inputs de pose; root no entra en su clave.
- Fail-closed: provider ausente, excepción, quaternion/escala/origen inválidos o estado externo desconocido hacen el endpoint no disponible; nunca se reutiliza un root antiguo como fallback.

## 2. Autoridades

`RootTransformProvider.RootTransform` es el DTO neutral de autoridad y contiene:

- origen world-space;
- quaternion de orientación world-space;
- escala uniforme.

El root se muestrea en autoridad una vez por endpoint causal. Render/prediction consume el DTO transportado; no vuelve a invocar el provider externo en cliente.

Los joints continúan siendo transformaciones **locales** del modelo y se evalúan/cachan independientemente.

## 3. Compatibilidad exacta

El built-in `scalebrews:entity_root` debe reproducir el comportamiento anterior:

`gravityFrame * rotateY(180° - bodyYaw) * uniformScale`, con `entity.position()` como origen.

Esta compatibilidad no convierte la gravedad en la API de root. Es sólo la implementación default. Otro provider puede suministrar un quaternion transversal diferente conservando la misma geometría y pose.

## 4. Fases implementador

- [x] I1 — registrar `scalebrews:entity_root` como provider real y retirar ownership sentinel legacy.
- [x] I2 — binding canónico resuelve y conserva `RootTransformProvider` junto a geometry/pose.
- [x] I3 — muestrear root en autoridad y almacenar DTO separado del joint sample.
- [x] I4 — evolucionar pose/root wire explícitamente y conservar atomicidad/identidad de revisión.
- [x] I5 — `ModelGeometryProvider` compone root transportado después del cache de joints; root-only update no incrementa `jointEvaluations`.
- [x] I6 — discontinuidad/interpolación root fail-closed y causalmente identificada.
- [x] I7 — fixture externo reusable con orientación transversal que reutiliza la misma geometry + pose.
- [x] I8 — documentación y regresiones de compatibilidad exacta del root vanilla/gravity actual.

La aceptación independiente, incluidos los reds históricos, mutantes y segunda lectura, está registrada en `S21-adversarial-model.md` y en `VALIDATION.md`.

## 5. Invariantes preservados

- S15/S16: catálogo/revisión atómicos.
- S17/S19: geometry y `modelTransform` siguen siendo autoridad de preparación.
- S18/S20: joints data-backed/reusables y cacheados por pose input, no por root.
- G2: blocker, carry y pairwise vanilla push no cambian.
- La física de gravedad continúa siendo propiedad de `GravityFrame`/adapter de gravedad; el root provider sólo orienta geometría material.

## 6. Repair implementer: invalidación inmediata tras removal

La campaña adversarial detectó una ventana real de lifecycle: después de `Entity.discard()`, un `MotionIntervalHandle` certificado podía seguir pasando `AnatomyRuntime.acceptsIntervalIdentity(...)` hasta que el siguiente `prepare()` retirase la entrada de `state.entities`. Durante esa ventana, un consumidor podía intentar resolver un intervalo contra una entidad ya eliminada.

El repair productivo **`c481194bb6156906768c12c499fc6ad8650a6595`** añade el fence mínimo en la propia puerta de identidad:

`entity.isRemoved() -> false`

antes de consultar server/state/provider. `AnatomyRuntime.interval(...)` ya depende de ese gate, por lo que el mismo cambio impide volver a muestrear root/provider después del `discard()`. No se altera el holdout, no se fuerza limpieza anticipada de mapas y no se cambia el orden del lifecycle normal.

### Evidencia implementer sobre `c481194b...`

- ordinary build **`35102351879`**, job **`104814662103`**: `compileJava`, client y GameTest correctos; servidor Minecraft 26.2 ejecutó **424/424 required GameTests** y terminó `BUILD SUCCESSFUL`;
- holdout adversarial de removal **`35102351750`**, job **`104814662473`**: **2/2 required GameTests** y `BUILD SUCCESSFUL`; valida invalidez inmediata del handle y ausencia de resample tras removal;
- holdout adversarial de rebind **`35102351833`**, job **`104814661968`**: **success** en la lane productiva;
- holdout adversarial de session teardown **`35102351804`**, job **`104814662087`**: **success** en la lane productiva.

## 7. Cierre adversarial independiente

El adversario añadió en **`0c99e2f1a4d158dd47b3d5b9f092dce1fa644986`** un mutation-kill dirigido que elimina únicamente el nuevo fence `entity.isRemoved()` de `AnatomyRuntime.acceptsIntervalIdentity(...)`, compila ese mutante y vuelve a ejecutar sin relajar el mismo holdout de lifecycle.

Run **`35105998877`** sobre ese snapshot:

- job **`104827240871`**, `root-removal-lifecycle`: **success** con producción intacta;
- job **`104827781422`**, `removal-fence-mutation-kill`: **success**; el mutante compila y el holdout lo **KILL**, demostrando que la invalidez inmediata del handle depende causalmente del fence de removal.

El ordinary workflow del mismo snapshot, run **`35105998939`**, job **`104827241696`**, terminó **success**.

### Segunda lectura final

Después del último repair productivo se releyeron las fronteras de preparación y hot path:

- `WorldAnatomyCatalog.replaceValidated(...)` valida el root-provider id y `prepareCanonical(...)` resuelve el `RootTransformProvider` durante aceptación de revisión; el runtime recibe un `Binding` ya materializado, sin lookup de root provider en HOT_TICK;
- `RootTransformProvider.RootTransform` normaliza/canonicaliza el quaternion y valida origen/escala en el DTO common;
- `ModelGeometryProvider.refreshRoot(...)` actualiza únicamente root authority conservando el último sample local de pose;
- `ModelGeometryProvider.joints(...)` cachea exclusivamente por `PoseEngine.Inputs`; el root no forma parte de su clave y `jointEvaluations` sólo avanza al evaluar inputs de pose distintos;
- `sampleAt(...)` compone después los joints ya cacheados con el root aceptado;
- el repair de removal sólo añade un fence de identidad antes de resolver un intervalo; no introduce parsing, reflection, resource lookup ni reevaluación de joints.

No se encontró otro cambio productivo necesario. Los reds históricos de causality, binding, wire, availability, same-tick, rebind, teleport, teardown y removal quedan preservados como evidencia de convergencia, no como blockers actuales.

**S21 queda cerrado adversarialmente y G3.6 puede marcarse completado.**
