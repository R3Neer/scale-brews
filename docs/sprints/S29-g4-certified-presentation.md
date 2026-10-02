# S29 — G4.5 adversarial model: certified causal presentation interval

Rol activo: **ADVERSARY**.

Estado: **RED DE CAPACIDAD / G4.5 ABIERTO**.

## 1. Scope

G4.5 cierra la tarea 5 de G4 y la parte intervalar de FR-086:

> La presentación cliente debe derivarse del mismo endpoint/intervalo causal que el contacto confirmado y no de una animación renderer independiente.

Este sprint **no** aplica todavía residual visual/cámara. Eso pertenece a G4.6. S29 sólo crea/certifica la primitive de presentación necesaria para que G4.6 no tenga que inventar causalidad.

## 2. Estado productivo actual

El cliente posee hoy:

- `AnatomyPosePayload`: endpoints causales server-issued con `frameSerial`, clocks root/joint, binding/tracking generation y root transform;
- `AnatomyContactPayload`: contacto server-confirmed con lifecycle/body/support identity;
- `AnatomyClientNetworking.PresentationFrame`;
- `PresentationKind.CURRENT_ENDPOINT` y el enum reservado `CERTIFIED_INTERVAL`.

Pero la única factory productiva es `PresentationFrame.current(...)`. El constructor rechaza explícitamente cualquier estado que no sea:

- `kind == CURRENT_ENDPOINT`;
- `before == after`;
- `fraction == 1`;
- `authorityTime == before.authorityTick()`.

No existe hoy un `VerifiedInterval` productivo ni un payload S2C que publique la identidad de un material interval certificado. El core servidor sí posee `GeometryProvider.MotionIntervalHandle(identity, materialSerial, before, after)`, pero esa certificación no cruza el wire.

Por tanto **dos pose frames recibidos no pueden convertirse por sí solos en un intervalo certificado**. La arquitectura prohíbe inferir un material interval a partir de snapshots inconexos.

## 3. Modelo de autoridad

Una presentación `CERTIFIED_INTERVAL` válida debe estar respaldada por un evento/identidad **server-issued** que nombre exactamente un intervalo material ya aceptado por el pipeline Q2.

El cliente puede reutilizar sus endpoints/evaluadores ya recibidos para materializar visualmente `before` y `after`, pero no puede decidir por sí solo que dos endpoints forman un intervalo continuo.

La certificación nunca puede venir de:

- proximidad de `frameSerial`;
- `before.frameSerial + 1 == after.frameSerial`;
- mismo tick;
- mismo root sequence;
- interpolación renderer/local animation;
- último/penúltimo frame del history sin token servidor;
- geometry/matrices enviadas como autoridad adicional.

## 4. Identidad mínima observable

El ADVERSARY no prescribe el schema exacto, pero el intervalo server-issued debe quedar inequívocamente cercado por los ejes que ya gobiernan la vida causal:

- epoch;
- catalog revision;
- dimension;
- support UUID + network/entity id actual o una identidad equivalente no reutilizable;
- binding generation;
- recipient tracking generation;
- material/interval serial server-issued o una identidad causal equivalente;
- before endpoint identity;
- after endpoint identity.

La identidad puede referenciar endpoints ya publicados. No debe duplicar model geometry, pose channels, matrices ni contacto físico completo.

## 5. Semántica de PresentationFrame

`CURRENT_ENDPOINT` sigue siendo válido para presentación exacta de un endpoint cuando no se reclama semántica intervalar.

`CERTIFIED_INTERVAL` debe cumplir:

- `before != after`;
- ambos endpoints pertenecen a la misma binding/lifecycle certificada;
- el intervalo fue emitido/confirmado por servidor;
- `fraction` está en `[0,1]`;
- `authorityTime` pertenece al intervalo certificado;
- `evaluated` se deriva de los mismos endpoints/pose/root server-confirmed, nunca del renderer local;
- una discontinuidad de gravity, rebind, tracking restart, dimension o unavailable no puede formar un intervalo continuo.

Si falta el evento causal, el cliente puede seguir mostrando `CURRENT_ENDPOINT`; **no puede etiquetar como CERTIFIED_INTERVAL un par inferido**.

## 6. Holdouts obligatorios del primer candidato

### A. Happy path

Servidor produce un `MotionIntervalHandle` real A→B y publica su certificación. Cliente ya conoce A/B o los recibe en cualquier orden permitido. Cuando ambos están materializables:

- `presentationFrame` puede exponer `CERTIFIED_INTERVAL`;
- before/after coinciden exactamente con A/B server-issued;
- no se re-evalúa geometry/catalog por llamada;
- repetir la consulta reutiliza cache/owner acotado.

### B. No inference

Dos endpoints A/B consecutivos sin certificación intervalar deben seguir produciendo `CURRENT_ENDPOINT`, nunca `CERTIFIED_INTERVAL`.

### C. Ordering

Interval certificate antes de A/B, entre A y B, y después de ambos debe converger al mismo resultado una vez estén presentes, dentro de bounds explícitos.

### D. Lifecycle

Debe fallar cerrado ante:

- epoch/revision/dimension mismatch;
- support UUID/network-id replacement;
- binding generation change;
- tracking generation restart;
- unavailable;
- gravity discontinuity;
- missing/gapped before/after serial.

### E. Contact coherence

Si `presentationContact(body)` consume un `CERTIFIED_INTERVAL`, éste debe corresponder al mismo support/binding/lifecycle del contacto server-confirmed. Un contacto viejo no puede apropiarse de un intervalo posterior incompatible.

### F. Bounds

Inbox/cache de interval certificates debe tener TTL/cap explícitos. Overflow debe fallar cerrado; no puede elegir arbitrariamente un intervalo cercano.

## 7. Mutation adequacy mínima

Antes de cerrar G4.5 deben morir mutantes compilables equivalentes a:

1. construir CERTIFIED_INTERVAL sólo por adjacency de frame serial;
2. ignorar binding generation;
3. ignorar tracking generation;
4. aceptar interval certificate de dimensión/epoch anterior;
5. unir endpoints a través de gravity discontinuity/unavailable;
6. usar un intervalo certificado de otro support/contact;
7. seleccionar nearest/last interval ante identidad ausente o ambigua;
8. permitir inbox/cache sin bound.

## 8. Gate de apertura

El árbol actual permanece RED porque no existe ninguna ruta productiva que pueda crear `PresentationKind.CERTIFIED_INTERVAL` ni una certificación S2C de material interval.

La presence gate temporal de S29 sólo demuestra esa ausencia. No será evidencia de cierre; se sustituirá por holdouts conductuales cuando exista el candidato.

Hasta entonces, **G4.5 permanece abierto y G4.6 no debe usar interpolación/residual como fuente de causalidad**.
