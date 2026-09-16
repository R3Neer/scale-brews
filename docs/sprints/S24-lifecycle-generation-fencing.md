# S24 — lifecycle generation fencing

Rol activo: **IMPLEMENTER**.

Estado: **EN CURSO / subcontrato de tracking+binding verde**. Este documento no cierra G3 tarea 9 ni modifica el estado canónico del plan. Registra únicamente el trabajo implementador ya demostrado y el siguiente alcance pendiente para revisión/adversario.

## 1. Alcance de este tramo

G3.9 exige que reload, tracking/unload, rebind, dimensión, reconnect y reutilización de identidad no permitan reutilizar estado temporal de otra vida causal. El primer hueco aislado estaba en el stream de pose: el payload transportaba la generación del binding físico, pero no la generación de tracking específica del receptor.

`bindingGeneration` y `trackingGeneration` son ejes distintos:

- `bindingGeneration`: vida del binding físico server-side de la entidad;
- `trackingGeneration`: ventana de visibilidad de una pareja receptor+UUID;
- ninguno puede sustituir al otro ni legitimar rollback del otro.

## 2. Wire y autoridad server-side

El payload de pose pasó a `anatomy_pose_v6` y transporta ambos ejes. La ruta live no usa un contador local del cliente:

1. `AnatomyRuntime` mantiene `trackingGenerations` por `ServerPlayer + UUID`;
2. `STOP_TRACKING` publica/limpia con la generación vigente y avanza la siguiente;
3. `AnatomyNetworking.sendPose(...)` resuelve la generación del receptor justo antes de construir el payload;
4. los overloads con tracking fijo existen sólo como fixtures/diagnóstico.

La generación de binding sigue siendo monotónica por epoch/server runtime y no se reutiliza como tracking.

## 3. Unload y replay tardío

`AnatomyFrameHistory` conserva un watermark `retiredTrackingGeneration`. `ENTITY_UNLOAD` retira la generación aceptada antes de descartar material. Un paquete tardío de esa misma ventana se rechaza aunque tenga serial superior.

Se detectó además un cruce más sutil: un paquete podía avanzar `bindingGeneration` manteniendo una `trackingGeneration` ya retirada; el receiver creaba un historial nuevo y con ello perdía el tombstone. La reparación final hace que `transition(...)` consulte primero el watermark retirado, de forma que **binding++ nunca revive tracking retirado**.

## 4. State machine de los dos ejes

`AnatomyFrameHistory.transition(...)` clasifica un paquete ya filtrado por sesión como:

- `CONTINUE`: misma identidad de binding y misma tracking generation;
- `RESTART`: avance lifecycle autorizado que requiere un historial nuevo;
- `REJECT`: rollback, tracking retirado o combinación de identidad incompatible.

La matriz implementer cubre:

- continuidad;
- retrack puro (`tracking++`, binding estable);
- rebind puro (`binding++`, tracking estable mientras siga viva);
- avance conjunto;
- rollback de binding aunque tracking avance;
- rollback de tracking aunque binding avance;
- cambio de network id bajo retrack puro rechazado;
- cambio de network id con rebind explícito permitido como `RESTART`;
- `binding++` dentro de una tracking generation retirada rechazado.

## 5. Reutilización de network id

El tick cliente ya detectaba que un `entityId` conocido podía pertenecer a otro UUID y descartaba material. S24 endurece esa transición: una sustitución **observada** de network id retira también la tracking generation antigua. Un timeout ordinario no lo hace, porque ausencia de paquetes no concede autoridad lifecycle al cliente.

## 6. Regresión S00 descubierta y corrección

La primera extracción del state machine hizo que `accept(...)` devolviera `false` para ciertos cambios de identidad o tratara un avance generation como restart implícito. El build general detectó correctamente la regresión en `S00CausalTests.frameHistoryRejectsEachIdentityAxisAtomically`: un historial individual debe rechazar atómicamente cualquier cambio de epoch/revision/dimension/entityId/UUID/model/provider/binding sin que el caller pueda mezclar vidas causales.

La solución final separa responsabilidades:

- `transition(...)` **sólo clasifica** si el receiver puede conservar, reiniciar o rechazar;
- `accept(...)` **sólo acepta continuidad de identidad exacta**;
- un `RESTART` obliga al receiver a crear un `AnatomyFrameHistory` nuevo antes de aceptar el paquete.

Así se conserva el contrato S00 sin perder el lifecycle explícito de S24.

## 7. Evidencia final de este subcontrato

Candidato: `4f20fac40c85a97032c0238c5214c397fd388565`.

- S24 focal `s24-lifecycle-generation`: run `35118836247`, job `104871104804`, **success**;
- build general: run `35118836489`, job `104871106614`, **success**, incluido el holdout S00 que había detectado la regresión;
- la workflow diagnóstica temporal usada para capturar el primer rojo se eliminó después de obtener la causa; no forma parte de la infraestructura permanente.

## 8. Pendiente antes de considerar G3.9 listo para handoff adversarial

Este verde no equivale a cerrar G3 tarea 9. Falta demostrar sobre lifecycle/runtime real, no sólo sobre kernels:

1. cambio de dimensión: catálogo de conexión sobrevive; poses/contactos/providers del `ClientLevel` anterior no;
2. disconnect/reconnect: epoch/catalog/poses/contactos/tombstones de la conexión previa no sobreviven;
3. reload válido: nueva revisión reemplaza temporal state de forma atómica y rebind real usa identidad nueva;
4. reload inválido: snapshot/revisión aceptados y estado útil anterior permanecen;
5. orden real START/STOP/unload/replacement con packets live y reutilización de identidad;
6. revisión adversarial posterior y performance/lifecycle pass sin cambios de producción.

El siguiente trabajo implementer continúa por esos ejes, empezando por dimensión/reconnect sobre el harness cliente integrado.
