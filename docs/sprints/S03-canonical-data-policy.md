# S03 — Data model, policy y decoder legacy canónicos

Estado: **candidato de implementación; pendiente de CI**.

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
| binding v1 roundtrip | GameTest | pendiente CI |
| unknown schema rejected | GameTest | pendiente CI |
| policy precedence | GameTest | pendiente CI |
| legacy anatomy/plane separation | GameTest | pendiente CI |
| suite previa | `./gradlew build` | pendiente CI |

## 7. Fallos/bucles

Pendiente de CI.

## 8. Revisión final

Pendiente tras CI.

## 9. Cierre

- [ ] CI verde;
- [ ] segunda pasada adversarial;
- [ ] revisión completa sin cambios;
- [ ] S03 cerrado.
