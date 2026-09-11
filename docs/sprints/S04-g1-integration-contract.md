# S04 — Integración estable y cierre funcional de G1

Estado: **CERRADO**. La reapertura adversarial posterior se resolvió sin cambiar requisitos ni invadir G2. El estado global vive en `ENTITY_COLLISIONS_PLAN.md`.

## 1. Scope

Tesis única: conectar las fronteras estabilizadas por S01-S03 sin iniciar el solver Q2 ni sustituir el lifecycle/catálogo preparado que G3 posee explícitamente.

Incluye capabilities/versionado, body adapters/categorías/policy en integration, fixture externo API+JSON, selección canónica de bindings fuera de `collision.internal`, validación de referencias y cierre adversarial del gate.

Excluye división de `AnatomyMovement`, Q2, prepared geometry catalog/lifecycle, engines completos, prediction/reconciliation y retirada del motor legacy.

## 2. Implementación cerrada

- [x] `BodyAdapter` y registry `CollisionAdapters` sin ownership físico.
- [x] `BodyClassification` y `CollisionRules` en `collision.integration`.
- [x] policy legacy convertida sólo por decoder de migración, sin importar `automatic_top`.
- [x] motor legacy puenteado temporalmente a la misma policy canónica.
- [x] `CollisionBindingCatalog` canónico, determinista y fail-closed.
- [x] geometry/pose/root refs validados contra registries.
- [x] protocol/capabilities versionados.
- [x] fixture externo init-time registrado por API y seleccionado por JSON.
- [x] codecs estrictos para campos canónicos y anidados.
- [x] ratio exacto `< / == / >` sin epsilon permisivo.
- [x] legacy warning deduplicado, selector identity estructural y orden canónico.
- [x] body adapters fail-closed.
- [x] `PlatformEligibility` como bridge deprecated a `CollisionRules`.
- [x] campaña adversarial paralela integrada sin borrar ni rebajar holdouts.
- [x] reapertura NFR-025 reparada con frontera API/runtime unidireccional.
- [x] server + client/integrated/dedicated repetidos sobre la reparación.
- [x] pasada final completa de G1 sin cambios de implementación.

## 3. Campaña adversarial

El snapshot `19626950569110963cd47611b245234c650f2ca0` dejó dos defectos reales:

1. S01: `AnatomyBackend` seguía siendo API pública de consumer;
2. S03: los codecs canónicos aceptaban unknown/legacy fields.

`3ba014dc2e19b6b700a707bd3177c55ac443328f` reparó ambos, pero una pasada posterior añadió `publicApiAndRuntimeDoNotFormALayerCycle` y reveló que mover el backend a `collision.runtime` había dejado `collision.api ↔ collision.runtime` cíclico.

El nuevo holdout S03 `constructorAcceptedBoundaryDataRoundTripsCanonically` pasó desde su introducción; la única reapertura posterior fue S01/NFR-025.

## 4. Reparación definitiva de la frontera

`a09c881a6a526bb735fe9e5f9f4a26d74325e615` mantiene `AnatomyBackend` fuera de `collision.api`, pero reemplaza en su contrato los DTOs públicos por tipos neutrales propios (`Mode`, `ContactData`, `RayHit`) más tipos JDK/Minecraft. `AnatomyApi` realiza la traducción en la façade y `ScaleAnatomyBackend` adapta los owners internos.

Resultado arquitectónico: `collision.api → collision.runtime`; `collision.runtime` no vuelve a depender de `collision.api`. No se añadió estado, no se duplicó ownership físico y no se adelantó Q2.

## 5. Test matrix final

| Propiedad | Resultado |
| --- | --- |
| S01 façade/backend/fail-closed | PASS |
| backend no público de consumer | PASS |
| `collision.api ↔ collision.runtime` sin ciclo | **PASS** |
| S02 registries/DTO bounds/order | PASS |
| engine/pose/root inexistentes | PASS, fail-closed |
| S03 schema/policy/legacy migration | PASS |
| unknown/legacy fields canónicos | PASS, reject |
| constructor→codec round-trip en límites máximos | PASS |
| fixture mod API + JSON | PASS |
| variant ambiguity / structural identity | PASS |
| ratio nextDown/exact/nextUp | PASS |
| body adapter invalid input/output | PASS |
| legacy policy bridge | PASS |
| suite servidor actual | **268/268 PASS** |
| client real + integrated + dedicated | **PASS** |

## 6. Evidencia final

Servidor sobre `a09c881a6a526bb735fe9e5f9f4a26d74325e615`:

- GitHub Actions run `34600301662`, job `103265686046`;
- **268/268 required GameTests passed**;
- `BUILD SUCCESSFUL`;
- artifact `10264495705`;
- SHA-256 `8676c2ddc0099ac0a34d647c426539b88b323fee53d9ccab552e34630f9534b4`.

Cliente sobre producción/tests idénticos, trigger temporal `5d725ae700c812f7864a22dc0459b9617f52e822`:

- `g1-client-proof` run `34600577409`, job `103266578934`;
- `xvfb-run -a ./gradlew runClientGameTest` → `BUILD SUCCESSFUL`;
- cow 240 vertices / 10 pieces, 80 pose comparisons;
- player wide/slim 144 vertices / 6 pieces, 80 pose comparisons cada uno;
- **640** comparaciones vanilla-family adicionales;
- `S00_CLIENT_RECEIPT_AUTHORITY PASS`;
- `S00_OBSERVER_AUTHORITY PASS`;
- dedicated **0 / 100 / 200 ms RTT, 120 ticks cada uno**.

El workflow temporal fue retirado en `1c591578f2fac8406bd69fa81dd08000b19b5b7e` sin modificar producción ni tests.

Corrección documental: el artifact histórico `10260227920` del run `34593762577` tiene SHA-256 correcto `67622ea0e2f440d6ac6f66abe860ffc6d8403f3649e129ff9c98b035a59e45f0`; cualquier valor anterior distinto fue una transcripción errónea.

## 7. Revisión final

La pasada posterior recorrió las nueve tareas G1, API/data/catalog/integration/migration, ownership, versionado/capabilities, strict codecs, policy/ratio, adapters, fixture init-time, engine refs, determinismo y frontera de paquetes.

Se confirma además:

- no queda `AnatomyBackend` en `collision.api`;
- `collision.runtime.AnatomyBackend` no referencia `collision.api`;
- `CollisionBindingCatalog` valida geometry/pose/root;
- codecs canónicos rechazan unknown/legacy fields;
- legacy planes sólo migran a planos explícitos;
- `PlatformEligibility` no conserva semántica paralela;
- `AnatomyMovement` no fue dividido ni rediseñado durante G1;
- no se inició material pipeline Q2 antes del cierre;
- G3/G5 permanecen diferidos.

No apareció otro cambio dentro de G1. **G1 vuelve a estar cerrado.**

## 8. Cierre

- [x] nueve tareas G1 implementadas;
- [x] campañas adversariales integradas;
- [x] fallos S01/S03 iniciales reparados;
- [x] strict codecs y holdout de límites máximos verdes;
- [x] ciclo `collision.api ↔ collision.runtime` eliminado;
- [x] suite servidor 268/268;
- [x] client/integrated/dedicated repetido tras la reparación common/API;
- [x] workflow temporal retirado;
- [x] evidencia registrada en `VALIDATION.md`;
- [x] pasada final sin cambios;
- [x] G1 cerrado formalmente.

El siguiente gate canónico es G2.