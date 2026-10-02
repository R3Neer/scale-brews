# S27 — G4.3 observer path without local carry

## 0. Roles

### IMPLEMENTER

Rol activo de este documento inicial: **IMPLEMENTER**.

### ADVERSARY

**Modelo adversarial congelado antes de cualquier cambio productivo.** La primera pasada es no-change-first.

## 1. Scope

G4.3 cierra la tarea 3 de G4: un cliente observador puede reconstruir y presentar estado server-confirmed de un body remoto, pero nunca ejecutar carry/prediction física local para ese body.

Requisitos directamente implicados:

- FR-079 — observers remotos sólo presentan estado confirmado;
- FR-080 — frame/contact materializado sólo tras validar identidad/lifecycle;
- FR-081 — late tracking reconstruye estado actual sin replay de carry;
- FR-082 — tracking loss/replacement/dimension/teleport/rebind retiran material temporal.

Fuera de scope:

- reconciliación/drift del actor local (G4.4 / FR-083);
- interval presentation Q2 completa (G4.5 / FR-086);
- residual visual/cámara y fade/reset (G4.6 / FR-087..088).

## 2. Estado inicial reconstruido

La frontera física ya está cerrada por S26:

- `AnatomyMovement.predictsBody(...)` concede prediction completa a un único root actor local;
- `PlatformPhysics` usa `predictsBody(...)` en suppress/collide/afterMove/carry;
- el loop cliente llama carry sólo para entidades localmente autoritativas;
- S26 run `36400355085` mata simulate-all, unguarded-loop, duplicate-root, stale-vehicle, stale-metadata y bridge-owner mutants.

El observer productivo conserva dos superficies distintas:

1. `presentationContacts`: identidad/contacto S2C confirmado para presentación;
2. `AnatomyMovement.contact/anchor`: `bindPhysics()` materializa hoy también el contacto S2C mediante `AnatomyMovement.confirm(...)`, incluso para bodies remotos.

Esa coexistencia **no se declara defecto por sí sola**. G4.3 exige ausencia de física local, no necesariamente ausencia de una vista física read-only, y existen consumidores/API que leen `supported/support` como estado confirmado. Cualquier separación adicional debe estar justificada por un oracle, no por estética arquitectónica.

Evidencia heredada relevante:

- `S00ObserverBoundaryTests`: direct y transitive carry de replicas remotas no mueve el body;
- `AnatomyExportProof`: un mob remoto recibe contacto/presentation real mientras `TransportLedger` cliente permanece vacío; unavailable retira contacto/presentation;
- S24 cubre replay/lifecycle, reconnect, dimension y late tracking de frames/contactos;
- `AnatomyPresentationFrameTests`: `CURRENT_ENDPOINT` se materializa/cacha desde endpoint server-issued y no desde renderer local.

## 3. Plan de implementación — IMPLEMENTER

La hipótesis inicial es **no-change-first**: intentar cerrar G4.3 con las superficies existentes antes de introducir otro store o duplicar contacto.

Checklist:

- [x] I1 Inventariar todos los consumers cliente de `AnatomyMovement.contact/supported`, `presentationContact` y `presentationFrame` y clasificarlos como física local, API read-only o presentación.
- [x] I2 Confirmar las rutas de movimiento principales y registrar cualquier lateral que todavía no use `predictsBody(...)`.
- [ ] I3 Verificar que late tracking puede reconstruir `presentationFrame + presentationContact` para un body remoto sin crear `TransportLedger` ni staged movement reference.
- [ ] I4 Verificar STOP_TRACKING/entity replacement/dimension/teleport/rebind: presentation material se retira o fencea sin fabricar carry ni conservar una identidad física reutilizable.
- [ ] I5 Sólo si un holdout demuestra que `AnatomyMovement.confirm(remote)` expone autoridad física indebida a un consumer real, separar observer contact de prediction contact con una única vista server-confirmed reutilizable; no crear un segundo solver ni re-evaluar geometry.
- [ ] I6 Mantener `CURRENT_ENDPOINT` como presentación exacta de Q1; no adelantar `CERTIFIED_INTERVAL` ni residual visual de G4.5/G4.6.
- [ ] I7 Ejecutar build + real-client observer + late-tracking/lifecycle afectado; registrar evidencia ejecutada y hacer pasada zero-change.

## 4. Invariantes

- un observer remoto nunca cambia posición/AABB/delta/gravedad por material anatomy recibido;
- ningún observer crea `TransportLedger`/receipt reference local;
- contacto/frame remoto sólo procede de S2C server-confirmed y pasa epoch/revision/dimension/UUID/network-id/binding/tracking fences pertinentes;
- late tracking parte del estado actual y no reproduce transportes previos;
- retirar tracking/lifecycle no deja material que una identidad nueva pueda reutilizar;
- no consultar renderer/local animation como autoridad;
- no tocar reconciliación local ni cámara/residual en este sprint.

## 5. Handoff al ADVERSARY

Antes de producción, el ADVERSARY debe congelar ataques concretos para al menos:

- remote body con presentation válida y transport inexistente;
- late observer que entra después de múltiples transportes server-side;
- STOP_TRACKING → retrack con mismo UUID y/o network id reutilizado;
- contact antes/frame después y frame antes/contact después;
- support unavailable/rebind mientras el observer conserva body vanilla;
- mutation que quite `predictsBody`/local-owner fence de una ruta física relevante;
- mutation que permita revivir presentation/contact viejo tras lifecycle.

Hasta ese freeze, **S27 no autoriza cambios productivos**.

## 6. Investigación IMPLEMENTER — consumers y rutas laterales

Pasada realizada sobre el HEAD posterior al cierre S26.

### 6.1 Rutas correctamente root-owner gated

- `PlatformPhysics.suppressPair(...)` anatomy: exige `AnatomyMovement.predictsBody(body)`.
- `PlatformPhysics.collide(...)`: sólo entra al solver anatomy si `predictsBody(e)`.
- `PlatformPhysics.afterMove(...)`: sólo actualiza física anatomy si `predictsBody(e)`.
- `PlatformPhysics.carry(...)`: sólo ejecuta carry anatomy si `predictsBody(e)`.
- `AnatomyMovement.edge(...)`: exige `predictsBody(body)`.
- `AnatomyClientNetworking` ordinary carry loop: exige `entity.isLocalInstanceAuthoritative()` y el core vuelve a cerrar por `predictsBody(...)`.
- `sendMovementReference(...)`: exige `refreshPredictionOwner(client)==body`, autoridad local y control del root vehicle.

Estas rutas no ofrecen actualmente una vía obvia para que un observer remoto mueva posición/AABB o genere transport/reference.

### 6.2 Vistas read-only / presentación

- `AnatomyApi.supported/support` y `Platforms.supported/support` pueden reflejar un contacto server-confirmed sin conceder por sí mismos prediction.
- `presentationContact(...)` valida body/support ids, revisión, binding generation y usa `presentationFrame(...)` del endpoint recibido.
- `presentationFrame(...)` usa `CURRENT_ENDPOINT`, cacheado por identidad + frame serial, sin renderer local como autoridad.

Que un observer conserve una vista `contact/anchor` read-only no se considera por sí solo un defecto mientras ninguna ruta física la consuma sin ownership.

### 6.3 Candidato de defecto para el modelo adversarial: friction

`PlatformLivingFrictionMixin` modifica `LivingEntity.travelInAir` llamando incondicionalmente a `Platforms.friction(entity, original)`.

En modo anatomy, `Platforms.friction(...)` obtiene `support(entity)` y puede devolver la fricción de la policy canónica **sin comprobar `AnatomyMovement.predictsBody(entity)`**. Un observer remoto con contacto S2C materializado podría, por tanto, usar fricción anatomy durante un tick físico cliente aunque no tenga authority de prediction.

Esto debe ser atacado por S27 antes de tocar producción. Oracle mínimo sugerido: body remoto con contacto server-confirmed y policy friction distinta de vanilla; ejecutar la ruta de travel/movement del replica y demostrar que velocidad/posición vanilla no cambia por anatomy. Mutante/control complementario: retirar el eventual owner gate debe hacer fallar ese oracle.

### 6.4 Otras rutas revisadas

- `PlatformEntityMixin.move/collide`: termina en bridge `PlatformPhysics`, ya root-owner gated para anatomy.
- lifecycle teleport/remove invalida contacto/transport y publica barrera cliente para tokens/references.
- `PlatformPhysics.touching(...)` y `Platforms.supported(...)` no están owner-gated, pero son lecturas de estado; no se clasifican como bug hasta que un consumer remoto las convierta en mutación física.

**Resultado de investigación:** I1/I2 cerrados. No se autoriza todavía el fix de friction ni una separación de stores hasta que el ADVERSARY congele S27 y clasifique el oracle.

## 6. Freeze adversarial previo a producción

El ADVERSARY congela el gate S27 sin autorizar cambios de producción.

### 6.1 Evidencia heredada que sí cuenta

- S26 `S00ObserverBoundaryTests` demuestra que direct carry y recursive carry no mueven físicamente un body remoto.
- S26 run `36400355085` mata `simulates->true`, ordinary loop sin local-owner, bridge sin unique-root owner y stale control authority.
- `AnatomyExportProof` ya demuestra tráfico real donde un mob remoto:
  - recibe `AnatomyMovement.contact` confirmado;
  - expone `presentationContact`;
  - mantiene `AnatomyMovement.transport(remote)==null`;
  - es transportado durante 60 ticks por el servidor;
  - pierde geometry/contact/presentation en UNAVAILABLE sin revivir material viejo.
- S24 cubre frame replay, reconnect, dimension y support-rebind fences.

Esta evidencia cierra la mitad negativa de FR-079 y buena parte de FR-080/082, pero no sustituye el late-observer contact bootstrap de FR-081.

### 6.2 Holdout nuevo congelado

`S27LateObserverPresentationClientProof` debe permanecer sin relajar expectativas.

Escenario:

1. cow anatómica + pig soportado se mantienen fuera de tracking range;
2. el servidor acumula varios `SupportTransport` reales del pig antes de que el cliente lo observe;
3. el player entra en tracking range;
4. el cliente debe reconstruir inmediatamente:
   - body remoto actual;
   - `presentationFrame` del soporte;
   - `presentationContact` del pig;
   - contacto read-only confirmado;
5. el cliente remoto debe conservar simultáneamente:
   - `simulates==false`;
   - `predictsBody==false`;
   - `TransportLedger==null`;
6. un carry server-side posterior debe avanzar posición vanilla y presentation frame, sin crear transport local;
7. STOP_TRACKING debe retirar la generation activa;
8. mientras el observer está fuera de rango, el servidor acumula más transportes;
9. retrack debe crear una generation nueva y reconstruir sólo el estado actual, con `TransportLedger` cliente todavía vacío.

El oracle diferencia explícitamente presentation/contact de prediction/carry. Un fallo de cualquiera de las dos mitades se clasifica por separado.

### 6.3 Mutation adequacy propia de G4.3

Workflow `s27-adversarial-observer-path` incluye inicialmente dos mutantes compilables:

1. **presentation-local-only**: `presentationContact(remote)` se oculta para bodies no local-authority. El holdout debe morir porque un observer remoto sí necesita presentación.
2. **contact-publication suppression**: se suprime publicación S2C de contacto tanto en START_TRACKING como en el publish ordinario, manteniendo pose/frame. El late observer debe morir por ausencia de estado confirmado.

Los mutantes físicos de carry remoto no se duplican aquí: S26 ya mata las rutas `simulates`, tick-loop y bridge-owner. Si S27 descubre una ruta física distinta, se añadirá un mutante específico a esa ruta.

### 6.4 Gate de clasificación

- Si baseline S27 es verde y ambos mutantes mueren, la hipótesis inicial es **no-change/product already correct** para el observer path básico.
- Si baseline es rojo por `presentationContact`/frame ausente pero transport local sigue nulo, se clasifica **presentation/bootstrap RED**.
- Si baseline detecta `TransportLedger` o `predictsBody/simulates` en el observer, se clasifica **PRODUCT RED de authority/carry** y vuelve al IMPLEMENTER.
- Un fallo de fixture/compilación no autoriza cambios productivos.

Hasta ejecutar esta matriz, **S27 no autoriza producción**.

## 7. Cierre adversarial final

S27 se cierra bajo la hipótesis **no-change-first**: no fue necesario modificar producción.

Run final `s27-adversarial-observer-path` **`36558367708`**:

- baseline `late-observer-baseline`, job **`109372922579`** — success;
- `presentation-local-only-mutant-must-die`, job **`109373610515`** — el mutante compiló y murió;
- `contact-publication-mutant-must-die`, job **`109373610565`** — el mutante compiló y murió.

Artifact baseline **`11028732300`**, SHA-256 **`af7b0bd38ebf03f67aaca3f530e3c53435d8cb7b9488d29e991aae85f254e898`**.

El holdout real-client demuestra en una misma vida:

1. el servidor acumula transportes reales antes de que el observer entre en tracking range;
2. late tracking materializa frame + presentation contact actuales;
3. el body remoto permanece simultáneamente `simulates=false`, `predictsBody=false` y sin `TransportLedger` cliente;
4. un transporte server-side posterior avanza posición vanilla y presentation frame sin crear carry local;
5. STOP_TRACKING retira la tracking generation;
6. el servidor acumula más transportes mientras el recipient está fuera;
7. retrack crea una generation nueva y reconstruye sólo el estado actual, sin replay de carry histórico.

Ordinary del mismo snapshot, run **`36558367581`**, job **`109372922475`** — success; artifact **`11029555862`**, SHA-256 **`56674825ebb18db0cc24e3e9be04e322355447c07061e282c9ab76520a7aa9ed`**.

Zero-change: compare `c39014a5374f6cb2348ee7c726ebb8c239dc613c → 7652be3b75ab264eb374f2eb428fb0c3544ee475` no contiene ningún cambio bajo `src/main` ni `src/client`; sólo tests, workflow y documentación.

**Conclusión ADVERSARY:** G4.3 queda cerrado. La coexistencia de `presentationContact` y una vista read-only de contacto físico no concede prediction remota mientras todas las rutas mutantes de carry permanezcan cercadas. El siguiente gate canónico es **G4.4: reconciliación sin double-apply ni drift**.


## 8. Reapertura por revisión IMPLEMENTER posterior al cierre

El cierre adversarial de §7 se produjo sobre `7652be3b...` bajo hipótesis no-change-first. Una pasada implementer posterior encontró una ruta física adicional que el freeze inicial no mutation-gateaba:

- `PlatformLivingFrictionMixin` llama a `Platforms.friction(...)` durante `LivingEntity.travelInAir`;
- un observer remoto puede conservar contacto anatomy server-confirmed para presentación/read-only;
- `Platforms.friction(...)` usaba ese contacto para sustituir la fricción vanilla **sin exigir `AnatomyMovement.predictsBody(entity)`**;
- por tanto una réplica remota podía recibir una modificación de integración física local aunque carry/collide/transport siguieran correctamente cerrados.

Clasificación: **PRODUCT RED de observer physics lateral / cobertura adversarial incompleta**.

Reparación IMPLEMENTER:

- `f661772e85a1ba916ad7a483123871f6dae70c7f` — en sesión anatomy, `Platforms.friction(...)` devuelve el valor original para cualquier body que no sea `predictsBody(...)`;
- `248a50ebae43d94aa0182f01fb4c96432a29096e` — regresión real-client en `S00ObserverBoundaryTests`: un body remoto con contacto confirmado y policy friction 0.6 debe conservar un valor vanilla arbitrario 0.91.

El cierre §7 permanece como evidencia válida del late-observer/bootstrap, pero **ya no cierra globalmente S27** porque fue anterior a este cambio productivo.

### Gate de recertificación requerido

Antes de volver a cerrar S27:

1. build del snapshot con el fix debe ser verde;
2. remote-observer baseline debe demostrar que la regresión de fricción pasa junto a direct/transitive carry;
3. el ADVERSARY debe añadir un mutante compilable que elimine el nuevo owner gate de `Platforms.friction(...)` o equivalente y demostrar que el oracle lo mata;
4. repetir el late-observer S27 para confirmar que presentation/contact remoto sigue disponible tras el nuevo gate;
5. pasada zero-change posterior.

Hasta entonces, **S27 / G4.3 queda REABIERTO** y G4.4 permanece bloqueado.
