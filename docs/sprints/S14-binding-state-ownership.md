# S14 — Binding state ownership

Estado operativo: **CERRADO**. S14 completa otra frontera real de G2 tareas 1/9, pero no fuerza una extracción artificial de endpoint/root/query que pertenece a una frontera posterior.

## 1. Tesis cerrada

El registro vivo de bindings anatómicos tiene un único owner dedicado y acíclico: `collision.internal.AnatomyBindingState`.

Provider actual, descriptor causal opcional, `localRegistrationGeneration`, quarantine de la generación actual y guard de captura reentrante ya no residen en `AnatomyMovement`. La extracción no movió `FRAME_SERIALS`, endpoint serialisation, root history, spatial membership, contacto, transporte ni solver.

Gate: **G2 — pipeline material continuo Q2**, tareas arquitectónicas 1/9.

## 2. Frontera final

`AnatomyBindingState` posee por identidad débil de `LivingEntity`:

- provider activo o ausencia;
- `GeometryIdentityDescriptor` activo o ausencia;
- generación local monotónica;
- quarantine exclusivamente de la generación activa;
- guard de captura reentrante;
- `Level` del binding activo para lifecycle;
- snapshot de providers activos para tick/rebuild.

No conoce ni llama a:

- `AnatomyMovement`;
- `AnatomyContactState`;
- `AnatomySpatialIndex`;
- `TransportLedger` / receipts;
- `Platforms`;
- CCD, separation o solver.

Dirección de dependencia demostrada: `AnatomyMovement -> AnatomyBindingState`.

## 3. Semántica preservada

- Cada rebind incrementa `localRegistrationGeneration` exactamente una vez.
- `deactivate(level)` retira provider/descriptor/quarantine activos del nivel sin rebobinar el watermark de generación de la misma instancia.
- Un rebind limpia quarantine de la generación anterior.
- Un causal rebind instala provider + descriptor + nueva generación en una sola transición. Ya no pasa primero por un binding descriptorless/fixture.
- Una captura exterior conserva el guard hasta su `finally` aunque el callback haga rebind/deactivate. El binding nuevo no puede abrir una captura anidada durante esa ventana.
- Una captura iniciada sobre binding A no puede publicar bajo binding B: el fence compara provider, descriptor y generación del snapshot de binding.
- El seam fixture `register(provider)` continúa admitiendo descriptor ausente sin convertirlo en binding causal.
- `FRAME_SERIALS`, `EndpointStamp`/`EndpointSerial`, `RootFrame`, `RootHistory` y `ROOTS` conservan su ownership y semántica previos.

## 4. Hallazgo de producción reparado

Antes de S14, `register(support, provider, descriptor)` llamaba primero a `register(support, provider)`. Con un índice espacial same-tick current, esa fase podía tratar temporalmente el support como binding fixture/legacy, muestrear el provider sin descriptor y después volver a muestrearlo al publicar el binding causal.

S14 elimina esa ventana. El overload causal ejecuta un único `AnatomyBindingState.rebind(support, provider, descriptor)` y sólo después invalida estado derivado y solicita `queryFrame`.

## 5. Implementación

- [x] I1 Añadir holdout estructural rojo que exija `AnatomyBindingState` y prohíba `PROVIDERS`, `REGISTRATIONS`, `DESCRIPTORS`, `QUARANTINED_REGISTRATIONS` y `CAPTURING` en `AnatomyMovement`.
- [x] I2 Añadir holdouts de generación/quarantine/deactivate/reentrancia sobre el owner real.
- [x] I3 Añadir holdout de causal rebind atómico y single-sample.
- [x] I4 Implementar `AnatomyBindingState` con weak identity slot, generación monotónica, quarantine y capture guard.
- [x] I5 Migrar lecturas/escrituras de provider/descriptor/generation/quarantine/capture desde `AnatomyMovement`.
- [x] I6 Separar fixture rebind y causal rebind como transiciones explícitas; causal rebind es atómico.
- [x] I7 Mantener endpoint/root ownership intacto y consumir el owner nuevo sin callback inverso.
- [x] I8 Conservar `AnatomyMovement.registrationGeneration` sólo porque mantiene consumidores reales; no se creó una façade nueva para justificar la extracción.
- [x] I9 Deactivate por nivel retira binding activo y quarantine preservando watermark monotónico.
- [x] I10 Ejecutar suite ordinaria y prueba prepared real-geometry sobre la cadena integrada final.
- [x] I11 Revisión adversarial post-verde de rebind durante capture, stale generation, deactivate/reactivate, weak identity y ciclos.
- [x] I12 Cerrar sólo después de una pasada completa sin cambio de producción S14.

## 6. Historia red-before-green

### Rojo registrado

Commit `5019fd7f7116c0c79291556fe41a8d70f2c95941`, run `34696527544`, job `103560853344`:

- **386 GameTests ejecutados**;
- **383 verdes / 3 rojos**;
- falló `dedicatedBindingOwnerMustExistAndMovementMustShedBindingStorage` porque el owner aún no existía;
- falló `bindingOwnerMustRemainOneWayAndPhysicsBlind` porque la dirección de dependencia todavía no podía auditarse;
- falló `causalRebindMustNotPassThroughDescriptorlessDoubleSample` con `samples=3`, demostrando el tránsito real por binding descriptorless.

El resto de la suite permaneció verde. Este run es el baseline rojo válido de S14.

### Implementación

- `692d52e...` introdujo `AnatomyBindingState`.
- `f8693c1d17ac0b73b95aeec74e5b2a57490fe9e1` migró el ownership vivo desde `AnatomyMovement` y convirtió el causal rebind en una transición atómica.
- `72c8f8c...` incorporó `AnatomyBindingState.java` a los triggers de la lane prepared para que cambios futuros de binding no puedan evitar la geometría real.

## 7. Evidencia final integrada

### Ordinary

Run `34708981487`, job `103594130965`, snapshot `27a915aba32e0113f5e587c594187783498375a4`:

- **386/386 required GameTests passed**;
- `BUILD SUCCESSFUL`;
- artifact `10302359158`;
- SHA-256 `c6f506d4cc33cb3d2071d4534eb61057114ab116d53626d4350e4776038ce363`.

Ese snapshot contiene la implementación y todos los holdouts S14. La única modificación productiva posterior es la reparación independiente S09 `d04874085b60ff4c9aaef7246928cf8379240484` en `TemporalResponse`; no modifica binding state ni sus consumidores.

### Real client + prepared

Workflow de validación `34708981488`, job `103594131113`:

1. hizo checkout exacto de `27a915a...`;
2. aplicó en workspace únicamente el parche S09 que después se convirtió en `d048740...`;
3. pasó la suite ordinaria;
4. exportó la geometría original cliente;
5. ejecutó la suite prepared servidor **2/2** con éxito;
6. sólo después creó y empujó `d048740...`.

Por tanto la fuente final de S14 fue validada junto con la reparación S09 que forma el HEAD actual. El workflow no rebajó budgets, tests ni geometría.

## 8. Revisión adversarial post-verde

Pasada completa sobre `d048740...`:

- no queda segundo mapa/owner de provider, descriptor, generación, quarantine o capture guard en `AnatomyMovement`;
- el owner no tiene callback ni dependencia inversa al orquestador/physics/contact/spatial;
- el rebind causal no atraviesa estado descriptorless;
- rebind/deactivate durante captura no liberan prematuramente el guard exterior;
- el fence de captura stale compara el snapshot completo del binding;
- generation watermark no se rebobina en lifecycle;
- weak-key storage conserva identidad de instancia, no igualdad por entity id;
- endpoint serial/root history no cambiaron de owner ni de contrato;
- no se identificó un cambio adicional de producción S14.

**Resultado de revisión:** cero cambios de producción. S14 queda cerrado.

## 9. Frontera restante tras S14

El estado persistente que aún reside en `AnatomyMovement` es esencialmente:

- activación por `Level` (`ACTIVE`);
- `FRAME_SERIALS` / `EndpointStamp` / `EndpointSerial`;
- `ROOTS` / `RootHistory`;
- métricas de sweep.

No constituyen otra frontera G2 independiente que pueda extraerse limpiamente ahora:

- `GeometryProvider.CausalEndpoint` contiene todavía `AnatomyMovement.RootFrame`;
- `queryFrame/currentSnapshot` combinan binding, endpoint causal, root y sampling;
- extraer endpoint/query manteniendo `RootFrame` anidado introduciría dependencia inversa hacia `AnatomyMovement`;
- mover/rediseñar `RootFrame` y root history anticiparía la generalización de `RootTransformProvider` y lifecycle prevista en G3;
- métricas y `ACTIVE` aislados son demasiado pequeños para justificar un sprint que no reduzca el acoplamiento principal.

Por tanto **no se abre un S15 por numerología**. El plan canónico debe decidir si G2 puede cerrar con esta frontera explícitamente diferida a G3 o si necesita reformular una tarea arquitectónica restante sin violar ese límite.