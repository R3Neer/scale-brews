# S21 — Generic root-transform authority

Estado: **OPEN / IMPLEMENTATION ACTIVE**.

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

- [ ] I1 — registrar `scalebrews:entity_root` como provider real y retirar ownership sentinel legacy.
- [ ] I2 — binding canónico resuelve y conserva `RootTransformProvider` junto a geometry/pose.
- [ ] I3 — muestrear root en autoridad y almacenar DTO separado del joint sample.
- [ ] I4 — evolucionar pose/root wire explícitamente y conservar atomicidad/identidad de revisión.
- [ ] I5 — `ModelGeometryProvider` compone root transportado después del cache de joints; root-only update no incrementa `jointEvaluations`.
- [ ] I6 — discontinuidad/interpolación root fail-closed y causalmente identificada.
- [ ] I7 — fixture externo reusable con orientación transversal que reutiliza la misma geometry + pose.
- [ ] I8 — documentación y regresiones de compatibilidad exacta del root vanilla/gravity actual.

## 5. Invariantes preservados

- S15/S16: catálogo/revisión atómicos.
- S17/S19: geometry y `modelTransform` siguen siendo autoridad de preparación.
- S18/S20: joints data-backed/reusables y cacheados por pose input, no por root.
- G2: blocker, carry y pairwise vanilla push no cambian.
- La física de gravedad continúa siendo propiedad de `GravityFrame`/adapter de gravedad; el root provider sólo orienta geometría material.
