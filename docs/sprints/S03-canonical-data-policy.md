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
- [x] codecs v1 estrictos, acotados y deterministas;
- [x] unknown version y unknown canonical fields fallan cerrados;
- [x] decoder legacy anatomy→binding mediante adapters nombrados;
- [x] decoder legacy surfaces→registro explícito de planos one-sided, nunca `CollisionBinding`, con warning de migración;
- [x] tests de roundtrip, precedence, version rejection, unknown-key rejection y separación legacy.

P1 descartó reutilizar `PlatformPolicy` porque `automatic_surfaces` reintroduciría fallback top/AABB. P2 convirtió legacy migration en `Decoded(binding xor legacyPlanes)`. P3 hizo explícito el orden de policy y añadió `excluded_states`. Revisiones posteriores añadieron warning FR-023, frontera exacta FR-009 y strict decoding de claves desconocidas.

## 4. Modelo adversarial

Ataques: schema desconocido, claves legacy inyectadas, variant/params inválidos, policy conflictiva, profile override parcial, datos anatomy/planes incompatibles, filtros inválidos y campos desconocidos en objetos top-level y anidados.

El holdout final exige que el JSON canónico no acepte `surfaces`, `automatic_surfaces` ni `anatomy`; un plano legacy nunca produce binding anatómico; los errores de decode se expresan como `DataResult.error`, no como excepción que escape del parse.

## 5. Implementación

- [x] `collision.data.CollisionBinding`;
- [x] `collision.data.CollisionPolicy`;
- [x] `collision.data.CollisionCodecs`;
- [x] `collision.migration.LegacyCollisionData`;
- [x] tests S03 registrados;
- [x] warning deduplicado para migración de planos legacy;
- [x] codecs estrictos para binding, policy y objetos anidados;
- [x] codec de `AnatomyFilter` convierte construcción inválida en error de decode.

## 6. Test matrix

| Propiedad | Nivel | Resultado final |
| --- | --- | --- |
| binding v1 roundtrip | GameTest | PASS |
| unknown schema rejected | GameTest | PASS |
| unknown/legacy fields rejected | adversarial GameTest | PASS tras reparación |
| policy precedence | GameTest | PASS |
| legacy anatomy/plane separation | GameTest | PASS |
| filter inválido devuelve codec error | regression GameTest | PASS |
| warning de plano legacy | runtime log | OBSERVADO |
| suite servidor completa | `./gradlew build` | PASS, 266/266 |
| client/integrated/dedicated | `runClientGameTest` | PASS |

## 7. Fallos/bucles

El candidato inicial `94d5c6eabfaf6c17cb8a22e8b604dc11af9367fb` pasó run `34583393180`, pero los tests S03 aún no estaban registrados; ese run se conserva como build/regresión, no como aceptación de S03.

Durante S04, run `34583969083` encontró un bug real del codec canónico de `AnatomyFilter`: una entrada inválida escapaba como `IllegalArgumentException` en vez de `DataResult.error`. Se clasificó como bug de implementación y `fbd9e31ef0c78c4666f840816d167f24b5e9fb2b` lo reparó con DTO intermedio + `comapFlatMap`.

La campaña adversarial final sobre `19626950569110963cd47611b245234c650f2ca0` produjo el segundo fallo real de S03 en run `34592683287`, job `103241351742`: `S03CanonicalCollisionDataTests.canonicalCodecsRejectInjectedLegacyKeys` demostró que `RecordCodecBuilder` ignoraba campos desconocidos como `surfaces`, `anatomy` y `automatic_surfaces`. Se clasificó como **bug de implementación**.

`3ba014dc2e19b6b700a707bd3177c55ac443328f` introdujo wrappers de decode estricto para binding, policy y sus objetos anidados, manteniendo la reparación fail-closed de filtros. La batería completa quedó verde.

## 8. Revisión final

Se revisaron schema/versioning, unknown-field semantics, determinismo, precedencia, bounded data, dual-semantics legacy y exclusiones G2/G3. La pasada completa posterior a la reparación adversarial no encontró otra ruta permisiva ni cambió el modelo canónico.

## 9. Cierre

- [x] implementación del scope;
- [x] tests S03 realmente registrados;
- [x] bugs de codec descubiertos, clasificados y reparados;
- [x] campaña adversarial integrada;
- [x] suite final servidor verde;
- [x] client/integrated/dedicated final verde;
- [x] revisión final posterior a la campaña sin cambios;
- [x] S03 cerrado.

Evidencia final compartida de G1: implementación/test `3ba014dc2e19b6b700a707bd3177c55ac443328f`; GitHub Actions run `34593762577`, job `103244714934`, **266/266 required GameTests passed**. Client/integrated/dedicated: trigger code-identical `23a1072ef451a14f59f707019415e1f4ac60dbab`, run `34593970131`, job `103245364717`, **success**.
