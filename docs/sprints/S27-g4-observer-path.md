# S27 — G4.3 observer path without local carry

## 0. Roles

### IMPLEMENTER

Rol activo de este documento inicial: **IMPLEMENTER**.

### ADVERSARY

Pendiente de congelar modelo adversarial antes de cualquier cambio productivo.

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

- [ ] I1 Inventariar todos los consumers cliente de `AnatomyMovement.contact/supported`, `presentationContact` y `presentationFrame` y clasificarlos como física local, API read-only o presentación.
- [ ] I2 Confirmar que ningún consumer de movimiento para body remoto evita `predictsBody(...)` mediante una ruta lateral (legacy bridge, edge, push suppression, reference sender o tick hook).
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
