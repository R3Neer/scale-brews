# S15 — Prepared catalog bundle reuse

Estado: **CERRADO**.

## Objetivo

Abrir G3 con una tesis única y medible: un catálogo anatómico aceptado debe serializarse, hashearse y fragmentarse **una sola vez por revisión/epoch de servidor**, y todos los receptores compatibles deben reutilizar ese mismo bundle preparado.

S15 cubre G3 tarea 2 y NFR-010, además de reforzar FR-035..037/NFR-016 en la frontera de publicación. No migra todavía `WorldAnatomyCatalog` desde `PlatformDefinition` a `CollisionBindingCatalog`, no implementa built-in `GeometryEngine`/`PoseEngine`/`RootTransformProvider`, no cambia física y no rediseña lifecycle de entidades.

## Estado inicial auditado

El runtime actual hace lo contrario de NFR-010:

1. `AnatomyRuntime.catalog(state, player)` llama `AnatomyNetworking.sendCatalog(player, revision, models, profiles)` cada vez que un jugador necesita la revisión.
2. `sendCatalog` llama `AnatomyCatalogTransfer.encode(...)`.
3. `encode` vuelve a validar el catálogo, serializa models/profiles a JSON, calcula SHA-256 y fragmenta el byte stream.
4. Por tanto N receptores pueden repetir N veces preparación idéntica de una revisión inmutable.
5. `WorldAnatomyCatalog.replaceValidated(...)` ya serializa el bundle una vez para comprobar que es enviable, así que el primer envío vuelve a repetir trabajo que ya se hizo al aceptar la revisión.

`AnatomyCatalogPayload` ya es reutilizable con seguridad: clona `fragment` al construir y al exponerlo.

## Frontera propuesta

### Owner

`WorldAnatomyCatalog` conserva junto a cada snapshot aceptado un `AnatomyCatalogTransfer.PreparedBundle` inmutable creado durante la validación candidata.

El bundle preparado posee exclusivamente:

- bytes/celdas ya fragmentados de la revisión aceptada;
- digest SHA-256;
- tamaño y orden de fragments;
- materialización cacheada de `AnatomyCatalogPayload` para un `epoch + revision` concretos.

No posee players, conexiones, world lifecycle, bindings activos ni física.

### Publicación

`AnatomyNetworking` separa dos operaciones:

- aceptar/registrar la revisión de servidor;
- enviar una lista ya preparada de `AnatomyCatalogPayload`.

La ruta live no recibe `models/profiles` y no puede volver a serializar en el hot send por receptor.

### Atomicidad

`WorldAnatomyCatalog` construye y valida **snapshot + bundle** antes del swap. Si falla codec, referencia, size/hash/fragmentation o preparación, no cambia ni el snapshot aceptado ni su bundle preparado.

## Exclusiones deliberadas

- Migrar `WorldAnatomyCatalog.Binding` a `CollisionBinding` canónico.
- Ejecutar `GeometryEngine.prepare(...)` en servidor.
- Adaptar `PoseEngine` a `ModelGeometryProvider`.
- Aplicar quaternion de `RootTransformProvider` en física live.
- Cambiar protocolo/catalog schema.
- Caching por variant/model más fino que una revisión completa.
- Prediction/reconciliation.

Esas fronteras se abordan después de que la publicación de revisión sea barata y atómica.

## Plan de implementación

- [x] I1 Añadir holdouts rojos registrados para prepared bundle inexistente y recipient-time reserialization.
- [x] I2 Introducir `AnatomyCatalogTransfer.PreparedBundle` inmutable y bounded.
- [x] I3 Preparar serialización + digest + fragments una sola vez durante `WorldAnatomyCatalog.replaceValidated`.
- [x] I4 Hacer atómico el swap `Snapshot + PreparedBundle`.
- [x] I5 Cachear materialización de payloads por `epoch + revision` dentro del bundle.
- [x] I6 Separar en `AnatomyNetworking` aceptación de revision y envío de packets preparados.
- [x] I7 Migrar `AnatomyRuntime.catalog(...)` a packets preparados; la ruta live deja de pasar models/profiles.
- [x] I8 Conservar `AnatomyCatalogTransfer.encode(...)` sólo si sigue teniendo consumidores de fixture; debe delegar al mismo prepared path, no mantener una segunda implementación.
- [x] I9 Probar invalid replacement: snapshot y bundle anteriores sobreviven juntos.
- [x] I10 Probar new revision: bundle nuevo, revisión nueva y payloads coherentes.
- [x] I11 Ejecutar suite ordinary completa; prepared sólo si paths relevantes la disparan o si la revisión detecta dependencia física accidental.
- [x] I12 Revisión adversarial post-verde hasta una pasada sin cambios.

## Resultado de implementación y cierre

`WorldAnatomyCatalog` publica cada revisión aceptada como el par atómico `Snapshot + AnatomyCatalogTransfer.PreparedBundle`; serialización, SHA-256 y fragmentación ocurren antes del swap. `PreparedBundle.packets(epoch, revision)` reutiliza la lista preparada para el mismo `epoch + revision`; `AnatomyRuntime` y `AnatomyNetworking` sólo consumen packets preparados en la ruta live, y `AnatomyCatalogTransfer.encode(...)` delega en el mismo camino como seam de fixture.

Los holdouts se añadieron antes de producción en `6b360fe9ec5c635cf3e9cdf89b848b543241f017`; los cambios productivos principales fueron `a1f74865e47af37d644c52437b16d13526ad3c5e`, `e9f8d70aa8280bcd5267c8bece88ac3ef782f7a0` y `2044ee434df5777c07bef0d28e5f7484fa43c0cf`. La primera lane focal, run `34746882338`, job `103696316689`, tuvo **9/9** GameTests verdes pero falló su propia aserción al no contar el sentinel automático `minecraft:always_pass`; `c49087ec095160043db142dcc7774945dc2bfbdd` corrigió sólo esa aserción. La evidencia final sobre `e52766a71cf66c4157d31b8884d901b22d4de7a8` es run `34747160506`, job `103697083788`: **8/8 S15** + sentinel y ordinary **394/394**; artifact `10313754760`, SHA-256 `5a3a20f13c7f24726cee3ed6af5baa3a1f3f63c4c6b5347c69c5063dfba42f57`. Build `34747160495`: **success**.

La revisión adversarial final no encontró otro owner ni una segunda ruta live de preparación; invalid replacement conserva snapshot+bundle, nueva revisión invalida el cache anterior, epoch/bounds siguen fail-closed y fragments continúan defensivos. Desde el hardening S15 `684142f32003a29f255b8ff204d39a7e0a968ed2` no hubo otro cambio de producción/tests S15. **Pasada final sin cambios de producción: S15 cerrado.**

## Modelo adversarial

### A1 — N receptores, una preparación

Tras aceptar una revisión, múltiples solicitudes de packets con el mismo epoch/revision deben devolver el mismo objeto/lista preparada o una vista que no vuelva a leer/serializar models/profiles.

### A2 — hot send sin modelos

`AnatomyRuntime`/`AnatomyNetworking` no pueden reconstruir el bundle desde `models/profiles` al enviar a un player. La API live de send recibe packets/bundle preparados.

### A3 — candidato inválido no contamina bundle aceptado

Si una sustitución falla después de parsear/validar parte del candidato, el snapshot y el prepared bundle anteriores conservan identidad/revisión y siguen publicables.

### A4 — revisión nueva invalida cache anterior

Aceptar revisión R+1 produce un bundle distinto. No puede reutilizar packets con revision R ni un digest/totalBytes perteneciente al snapshot previo.

### A5 — epoch fence

Packets materializados para un epoch no se sirven como si pertenecieran a otro. El payload mantiene la epoch del servidor que lo publica.

### A6 — fragment immutability

Reutilizar un payload entre receptores no permite que un consumidor modifique bytes compartidos. `fragment()` sigue siendo defensivo.

### A7 — bounds intactos

`MAX_BYTES`, `CHUNK`, digest y conflicto/replay de fragments no se relajan. Preparar una vez no crea una ruta sin límites.

### A8 — revision authority no depende del primer receptor

La revisión aceptada del servidor debe registrarse al aceptar/preparar el catálogo, no sólo cuando casualmente se conecta el primer player.

## Criterio de cierre

S15 cierra sólo cuando:

1. serialización/hash/fragmentation son trabajo de aceptación de revisión, no de recipient send;
2. varios receptores reutilizan el bundle preparado;
3. invalid replacement conserva snapshot+bundle anteriores;
4. revisión/epoch/bounds siguen fail-closed;
5. la ordinary suite queda verde;
6. una revisión post-verde no encuentra una segunda ruta de preparación live ni un owner duplicado.
