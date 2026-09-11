# S03 — Data model, policy y decoder legacy canónicos

Estado: **implementación y CI real verificadas; cierre formal pendiente de la campaña adversarial paralela**.

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
- [x] decoder legacy surfaces→registro explícito de planos one-sided, nunca `CollisionBinding`, con warning de migración;
- [x] tests de roundtrip, precedence, version rejection y separación legacy.

P1 descartó reutilizar `PlatformPolicy`, porque `automatic_surfaces` permitiría reintroducir el AABB/top fallback prohibido. P2 convirtió legacy migration en `Decoded(binding xor legacyPlanes)` para impedir dual semantics. P3 hizo explícito el orden de policy y añadió `excluded_states`. La siguiente pasada fue zero-change para el scope inicial.

La revisión transversal posterior de G1 añadió el warning deduplicado exigido por FR-023 y endureció en S04 la frontera numérica de ratio a `< / == / >` exactos.

## 4. Modelo adversarial base

Ataques: schema desconocido, key legacy inyectada, variant/params inválidos, policy conflictiva, profile override parcial, `PlatformDefinition` anatomy, planes y shape inválido anatomy xor planes. Holdout base: el JSON canónico no contiene `surfaces` ni `automatic_surfaces`, y un plano legacy nunca produce binding anatómico.

La campaña adversarial ampliada está delegada al agente paralelo del propietario y puede reabrir S03.

## 5. Implementación

- [x] `collision.data.CollisionBinding`;
- [x] `collision.data.CollisionPolicy`;
- [x] `collision.data.CollisionCodecs`;
- [x] `collision.migration.LegacyCollisionData`;
- [x] `S03CanonicalCollisionDataTests`;
- [x] warning deduplicado para migración de planos legacy, sin convertirlos en anatomía.

## 6. Test matrix

| Propiedad | Nivel | Resultado ejecutado |
| --- | --- | --- |
| binding v1 roundtrip | GameTest | PASS en suite G1 registrada |
| unknown schema rejected | GameTest | PASS |
| policy precedence | GameTest | PASS |
| legacy anatomy/plane separation | GameTest | PASS |
| suite previa | GitHub Actions `./gradlew build` | PASS |
| warning de plano legacy | runtime log de GameTest | OBSERVADO en run G1 vigente |

## 7. Fallos/bucles

El candidato inicial `94d5c6eabfaf6c17cb8a22e8b604dc11af9367fb` pasó run `34583393180`, pero los tests S03 aún no estaban registrados; se conserva como evidencia de build, no de las aserciones S03.

Durante S04, run `34583969083` hizo visible un bug real del codec canónico de `AnatomyFilter`: una entrada inválida escapaba como `IllegalArgumentException` en vez de `DataResult.error`. Se clasificó como bug de implementación y `fbd9e31ef0c78c4666f840816d167f24b5e9fb2b` restauró decode fail-closed mediante DTO intermedio + `comapFlatMap`.

Con los tests ya registrados, run `34584717556` intento 2/job `103216390687` ejecutó **256/256**. El snapshot posterior `1a19ec31cdaeb8c0d98cc86b327adb1661e09632` repitió **256/256** en run `34585112975`, job `103217403352`; el log muestra además el warning de migración legacy para `minecraft:cow`.

## 8. Revisión final

La revisión de implementación cubrió schema/versioning, determinismo, precedencia, bounded data, dual-semantics y exclusiones de G2/G3. Los cambios transversales posteriores han sido revalidados por la suite G1. Debe repetirse si la campaña adversarial modifica el árbol.

## 9. Cierre

- [x] implementación del scope;
- [x] CI con tests S03 realmente ejecutados;
- [x] bug de codec descubierto posteriormente reparado y revalidado;
- [ ] campaña adversarial paralela integrada o sin fallos pendientes;
- [ ] revisión final posterior a esa campaña;
- [ ] S03 cerrado formalmente.

Candidato lógico inicial: `94d5c6eabfaf6c17cb8a22e8b604dc11af9367fb`.

Evidencia vigente: `1a19ec31cdaeb8c0d98cc86b327adb1661e09632`, run `34585112975`, job `103217403352`, **256/256 required GameTests passed**.
