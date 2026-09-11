# S03 — Data model, policy y decoder legacy canónicos

Estado: **CERRADO**.

## 1. Scope

Tesis única: definir el contrato de datos versionado de G1 sin `PlatformDefinition.Surface`, separar policy global/category/support/profile y convertir el formato legacy mediante un decoder unidireccional que nunca inventa anatomía.

Cubre FR-009..013, FR-019..020, FR-022..024, FR-026..027, FR-029..033 en su dimensión de datos; cierra la parte de G1 de NFR-019..025 y NFR-036 relativa a schemas. La integración runtime final queda para S04; G2+ permanece fuera.

## 2. Estado inicial

`PlatformDefinition` mezclaba entity policy, planos top-surface y una `AnatomyDefinition` opcional. `PlatformPolicy` conservaba `automatic_surfaces`, semántica prohibida para el core anatómico. El formato carecía de `schema_version` y no expresaba engine ids separados para geometry/pose/root.

## 3. Plan y convergencia

- [x] binding v1 con entity + variant selectors + geometry engine/model/filter + pose engine/channels + root provider + profile policy + excluded states;
- [x] policy v1 con precedence global→category→support→profile para enabled/ratio/friction;
- [x] codecs v1 estrictos y deterministas con unknown-version rejection;
- [x] decoder legacy anatomy→binding mediante adapters nombrados;
- [x] decoder legacy surfaces→registro explícito de planos one-sided, nunca `CollisionBinding`;
- [x] tests de roundtrip, precedence, version rejection y separación legacy.

P1 descartó reutilizar `PlatformPolicy`, porque `automatic_surfaces` permitiría reintroducir el AABB/top fallback prohibido. P2 convirtió legacy migration en `Decoded(binding xor legacyPlanes)` para impedir dual semantics. P3 hizo explícito el orden de policy y añadió `excluded_states`. La siguiente pasada fue zero-change.

## 4. Modelo adversarial

Ataques: schema desconocido, key legacy inyectada, variant/params inválidos, policy conflictiva, profile override parcial, `PlatformDefinition` anatomy, planes y shape inválido anatomy xor planes. Holdout: el JSON canónico no contiene `surfaces` ni `automatic_surfaces`, y un plano legacy nunca produce binding anatómico.

## 5. Implementación

- [x] `collision.data.CollisionBinding`;
- [x] `collision.data.CollisionPolicy`;
- [x] `collision.data.CollisionCodecs`;
- [x] `collision.migration.LegacyCollisionData`;
- [x] `S03CanonicalCollisionDataTests`.

## 6. Test matrix

| Propiedad | Nivel | Resultado |
| --- | --- | --- |
| binding v1 roundtrip | GameTest | PASS dentro de `./gradlew build` |
| unknown schema rejected | GameTest | PASS |
| policy precedence | GameTest | PASS |
| legacy anatomy/plane separation | GameTest | PASS |
| suite previa | GitHub Actions `./gradlew build` | PASS |

La segunda pasada adversarial confirmó que el binding canónico no contiene claves legacy, las versiones desconocidas fallan cerradas y la migración de planos no crea anatomía. Las mutaciones reservadas tienen oráculos directos en la suite.

## 7. Fallos/bucles

Ninguno. El candidato pasó a la primera ejecución CI.

## 8. Revisión final

Se revisaron schema/versioning, determinismo, precedencia, bounded data, dual-semantics y exclusiones de G2/G3. La pasada completa posterior a CI no produjo cambios.

## 9. Cierre

- [x] CI verde;
- [x] segunda pasada adversarial;
- [x] revisión completa sin cambios;
- [x] S03 cerrado.

Commit candidato: `94d5c6eabfaf6c17cb8a22e8b604dc11af9367fb`.

GitHub Actions run `34583393180` finalizó **success** el 2026-09-11. La suite normal `./gradlew build` pasó con Java 25. La evidencia acumulada se consolidará en `VALIDATION.md` al cierre de G1.
