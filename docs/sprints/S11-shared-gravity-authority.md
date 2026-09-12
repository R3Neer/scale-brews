# S11 — Autoridad compartida de gravedad

Estado: **PLAN CERRADO / IMPLEMENTACIÓN PENDIENTE**

## Objetivo

Cerrar la reconciliación de autoridad de gravedad pendiente de G2: Scale Brews debe tener una sola fuente de verdad para la dirección de gravedad de una entidad y Entity Collisions debe consumirla, no poseer una autoridad paralela.

Este sprint no abre G3 ni porta cambios funcionales de Tiny Mounts. El alcance es estrictamente de autoridad, integración y seams de prueba necesarios para preservar FR-050 y los invariantes físicos ya cerrados en S08/S09.

## Baseline observado

En `chatgpt-editing` existen hoy dos capas que pueden decidir la gravedad efectiva dentro de Entity Collisions:

1. `collision.internal.GravityFrames`, con un adaptador global instalable por owner.
2. `AnatomyMovement.GRAVITY`, un mapa por entidad que tiene precedencia sobre ese adaptador.

Además, `ScaleAnatomyBackend.gravity(...)` delega en `AnatomyMovement.gravity(...)`, por lo que la API pública puede observar la autoridad local de collisions en vez de una autoridad compartida de Scale Brews.

`main` ya contiene la dirección arquitectónica que debemos reconciliar: `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como autoridad única de gravedad corporal. S11 adapta ese principio al contrato público de Entity Collisions sin duplicar el `GravityFrame` físico de la API.

## Decisiones arquitectónicas cerradas

- La autoridad global vivirá en `io.github.r3neer.scalebrews.integration.gravity.GravityFrames`.
- `collision.api.GravityFrame` sigue siendo la representación física canónica que usa el pipeline de Entity Collisions. No se introduce un segundo DTO de frame con semántica solapada.
- La autoridad compartida expone dirección cardinal y frame, con fallback vanilla `DOWN` y compat opcional con Gravity Changer.
- Solo puede existir un proveedor global externo. La instalación se identifica por `owner`; reinstalar el mismo owner es idempotente y un owner diferente falla explícitamente.
- Los overrides por entidad no forman parte de la autoridad de producción. Las pruebas que necesiten gravedad sintética deben usar un seam explícito de GameTest/test, no un segundo almacén físico dentro de `AnatomyMovement`.
- `AnatomyMovement` no conservará un mapa `GRAVITY` ni decidirá precedencia de fuentes de gravedad.
- `ScaleAnatomyBackend.gravity(...)` y `AnatomyMovement.gravity(...)` deben observar la misma autoridad compartida.
- `collision.internal.GravityFrames` deja de ser propietario. Se elimina, no se convierte en otro registro reenvasado.
- El ciclo de vida de un `Level` no puede resetear el proveedor global ni fabricar una autoridad nueva. Los seams de prueba, si requieren limpieza local, se limpian de forma explícita y acotada.

## Requisitos e invariantes cubiertos

- FR-050: compatibilidad con gravedad personalizada/cardinal.
- FR-049 / FR-051: transporte y soporte continúan interpretándose en el frame de gravedad corporal correcto.
- NFR-002: resolución determinista de autoridad.
- NFR-004: fallback seguro a gravedad vanilla cuando no hay proveedor.
- NFR-015 / NFR-017: lifecycle y rebind no dejan estado fantasma de gravedad.
- NFR-025: una sola autoridad/owner para el dato físico de gravedad.

## Plan de implementación

- [ ] Añadir el holdout rojo de S11 que demuestre que la autoridad local `AnatomyMovement.GRAVITY` puede ocultar al adaptador global y que aún no existe la autoridad compartida.
- [ ] Registrar evidencia roja en CI sin reinterpretar fallos preexistentes de S09 como fallo de S11.
- [ ] Introducir `integration.gravity.GravityFrames` con fallback vanilla, compat Gravity Changer y ownership explícito del proveedor.
- [ ] Migrar `ScaleAnatomyBackend.installGravityAdapter(...)` y `gravity(...)` a la autoridad compartida.
- [ ] Migrar `AnatomyMovement.gravity(...)` a lectura de la autoridad compartida y eliminar `GRAVITY`.
- [ ] Retirar `collision.internal.GravityFrames`.
- [ ] Mover la inyección de gravedad sintética de los GameTests existentes a un seam explícito de prueba sin crear una segunda autoridad de producción.
- [ ] Añadir holdouts verdes para DOWN, las seis direcciones cardinales, ownership de proveedor y coincidencia API/pipeline.
- [ ] Reejecutar los holdouts de S08/S09 que ejercitan gravedad lateral y transporte continuo.
- [ ] Ejecutar CI ordinario y prepared adversarial; documentar por separado cualquier fallo de baseline no atribuible a S11.
- [ ] Actualizar `ENTITY_COLLISIONS_PLAN.md` y `VALIDATION.md` únicamente cuando la evidencia verde permita cerrar S11.

## Holdouts adversariales

### A1 — autoridad compartida presente

Debe existir `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como servicio de Scale Brews.

### A2 — sin owner local en AnatomyMovement

`AnatomyMovement` no puede declarar un campo/registro de gravedad por entidad que preceda a la autoridad compartida.

### A3 — sin autoridad interna duplicada

`io.github.r3neer.scalebrews.collision.internal.GravityFrames` debe desaparecer como propietario de estado/proveedor.

### A4 — API y pipeline coinciden

Para una misma entidad, `AnatomyApi.gravity(entity)` y la gravedad consumida por `AnatomyMovement` deben resolver el mismo frame.

### A5 — vanilla seguro

Sin proveedor externo, la resolución debe ser `Direction.DOWN` / `GravityFrame.VANILLA`.

### A6 — seis direcciones cardinales

Un proveedor válido debe poder devolver DOWN, UP, NORTH, SOUTH, WEST o EAST sin perder las transformaciones `toLocal`/`toWorld` ni el criterio `supports(...)` del `GravityFrame` público.

### A7 — ownership exclusivo

Una segunda instalación con owner distinto falla; repetir el mismo owner no crea otra autoridad ni cambia silenciosamente de resolver.

### A8 — independencia body/support preservada

`S08GravityIndependenceTests` sigue demostrando que la gravedad del cuerpo puede ser distinta de la del soporte sin romper el carry certificado.

### A9 — sliding lateral preservado

Los casos laterales de S09 continúan verdes para EAST/WEST y no reaparece una suposición de `DOWN` global.

### A10 — lifecycle sin estado fantasma

Desactivar un `Level` limpia solo estado de collisions asociado al nivel; no rebobina ni sustituye la autoridad global compartida.

## Evidencia de baseline

Al abrir S11, el carril ordinario del baseline anterior estaba verde. El carril prepared adversarial tenía un fallo localizado de S09/A9 por `ITERATION_LIMIT` / presupuesto de manifold; ese fallo es anterior y ajeno a la reconciliación de gravedad. S11 no se considerará culpable ni verde basándose en ese carril hasta distinguir el estado del baseline del efecto de sus propios cambios.

## Criterio de cierre

S11 solo puede marcarse **CERRADO** cuando:

1. existe una única autoridad de gravedad compartida de Scale Brews;
2. `AnatomyMovement` ya no posee estado de gravedad por entidad;
3. la antigua autoridad `collision.internal.GravityFrames` ha desaparecido;
4. API pública y pipeline físico resuelven la misma gravedad;
5. los holdouts de gravedad lateral/transport de S08 y S09 permanecen verdes;
6. la evidencia CI queda registrada y cualquier fallo externo de baseline se documenta por separado.
