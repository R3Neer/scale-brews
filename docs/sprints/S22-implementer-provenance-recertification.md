# S22 / G3.8 — recertificación de provenance del IMPLEMENTER

Rol activo: **IMPLEMENTER**.

Estado: **LISTO PARA REVISIÓN ADVERSARIAL DE RECierre**. Este documento no cierra S22, no marca G3 tarea 8 y no sustituye la revisión independiente requerida por `ENTITY_COLLISIONS_SPRINT_WORKFLOW.md`.

## 1. Rojo adversarial

La reapertura vigente de S22 estaba localizada en provenance/autenticidad de `CollisionCoverageDiscovery.Artifact`: un caller podía construir manualmente un `Artifact` con exactamente todos los ids descubiertos y filas `FULL`/`SAFE_PARTIAL`/`EXCLUDED` plausibles pero fabricadas, y `requireResolved()` no podía distinguirlas de evidencia realmente emitida por el scanner canónico.

El holdout causal es `S22AdversarialCoverageProvenanceTests`; la evidencia roja inicial registrada en el plan corresponde al run `35106752630`, job `104829857809`.

## 2. Primera reparación y defecto detectado

Commit `8f72e5e8dc6637b2f6f01800ef617d949b1be448` — `fix(s22): bind resolved artifacts to scanner provenance`.

La reparación introdujo provenance privada emitida exclusivamente por `CollisionCoverageDiscovery.scan(...)` y mantuvo la comprobación exacta de completitud de ids descubiertos. Con ello los artifacts clasificados manualmente dejaron de poder pasar `requireResolved()`.

Ese primer parche, sin embargo, hacía fallar incondicionalmente por provenance cualquier artifact manual, incluido un artifact sin filas. Eso enmascaraba el mutation-kill de completitud: al mutar la comprobación `discover(target)` el holdout seguía fallando por provenance, de modo que la mutación de completeness sobrevivía causalmente.

Clasificación: **implementación incompleta**, no defecto del holdout. Provenance y completeness debían seguir siendo barreras independientes.

## 3. Reparación final

Commit productivo final de S22: `50296a6792f523e57fcd8daa71ba6f598254a6e6` — `fix(s22): keep completeness and provenance gates independent`.

Contrato final:

- el constructor público de `Artifact` conserva la comprobación exacta `discover(target).livingEntityTypes()` frente a las filas del reporte;
- `CollisionCoverageDiscovery.scan(...)` es la única ruta que emite el token privado de provenance del scanner;
- un artifact manual **con filas clasificadas** no puede pasar `requireResolved()` sin provenance canónica;
- un artifact manual **sin filas** no afirma clasificación alguna, por lo que su aceptación depende únicamente de que el target descubierto sea realmente vacío; así la completitud sigue siendo una barrera causal independiente;
- `coverage.requireResolved()` sigue rechazando estados `UNRESOLVED` incluso para artifacts emitidos por el scanner.

No se modificaron los holdouts adversariales para obtener verde.

## 4. Evidencia ejecutada sobre `50296a6...`

- `s22-adversarial-coverage-provenance`: run `35109432100` — **success**;
- `s22-adversarial-coverage-completeness`: run `35109432255` — **success**;
  - job normal `coverage-completeness`: success;
  - mutation-kill job `104839577148`: el mutante que sustituye discovery completeness por las propias filas del reporte fue **muerto** por el holdout;
- `s22-adversarial-coverage-residuals`: **success**;
- `s22-coverage-proof`: **success**;
- `build`: run `35109431854` — **success**.

La consulta de workflows completados para ese SHA devolvió siete runs terminados y ningún `conclusion=failure`.

## 5. Estabilidad posterior

El HEAD actual desciende de `50296a6...`. La comparación `50296a6... -> 612c5fe...` no contiene cambios en `CollisionCoverageDiscovery`, `CollisionCoverageScanner` ni los holdouts S22; los commits posteriores pertenecen al trabajo de lifecycle S24.

Por tanto la reparación productiva de provenance/completeness continúa integrada sin modificación posterior conocida.

## 6. Handoff

**Clasificación IMPLEMENTER:** el blocker productivo singular de S22 está reparado y recertificado; G3 tarea 8 queda **lista para revisión adversarial independiente**.

El ADVERSARY debe ejecutar/confirmar su pasada final, reconciliar `S22-adversarial-model.md`, `ENTITY_COLLISIONS_PLAN.md` y `VALIDATION.md`, y sólo entonces marcar S22/G3.8 como cerrado. Si aparece un rojo causal nuevo, vuelve al IMPLEMENTER con el fallo localizado.
