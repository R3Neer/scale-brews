# S01 — Frontera pública y backend explícito

Estado: **implementación y CI real verificadas; cierre formal pendiente de la campaña adversarial paralela**.

## 1. Scope

### Incluido

S01 pertenece a G1 y tiene una sola tesis: `collision.api` deja de conocer clases de `collision.internal`; una implementación Scale-owned queda detrás de un backend explícito y la ausencia de backend falla cerrada.

### Excluido

Quedan fuera de S01 el binding/policy canónico, registries de engines, codecs versionados, migración de planos legacy y body adapters. También quedan fuera, por pertenecer a G2+, la división de `AnatomyMovement`, el pipeline Q2, el catálogo/lifecycle final y prediction/reconciliation.

### Requisitos

Contribuye directamente a FR-001..004 y FR-072..074, y a NFR-021, NFR-025, NFR-034. No declara cerrados requisitos cuya aceptación completa necesita sprints posteriores de G1 o gates posteriores.

### Invariantes

- `BINDING` sigue siendo fail-closed.
- sólo Scale posee la física compartida;
- el backend no crea un segundo solver ni duplica estado;
- `GravityFrame` y `AnatomyMode`, clasificados TRUSTED por S00, no cambian de semántica;
- el motor legacy no se retira antes de G5.

## 2. Estado inicial e investigación

S00 clasificó `AnatomyApi` como REWORK porque importaba `AnatomyMovement`, `AnatomySession` y `GravityFrames`. La fachada pública era por tanto dependiente de la implementación que debía ocultar. Las operaciones públicas existentes ya coincidían con la frontera arquitectónica de alto nivel, así que no era necesario rediseñarlas para este sprint.

La investigación confirmó que `AnatomySession` conserva la semántica DISABLED/BINDING/READY y que `AnatomyMovement` sigue siendo el owner temporal de contacto/query/carry hasta G2. Mover esas responsabilidades sería adelantar G2, no desacoplar G1.

## 3. Plan de implementación

- [x] I1 Introducir un contrato de backend mínimo que reproduzca únicamente las operaciones ya expuestas por `AnatomyApi`.
- [x] I2 Proporcionar un backend inerte que falle cerrado cuando el runtime Scale no esté disponible.
- [x] I3 Implementar un único backend Scale-owned que delegue en el runtime existente sin duplicar estado.
- [x] I4 Descubrir el backend mediante Java services y rechazar cardinalidad distinta de uno cuando haya providers reales.
- [x] I5 Reescribir `AnatomyApi` para que no importe `collision.internal`.
- [x] I6 Añadir tests de fail-closed, unicidad de provider y ausencia de tipos internos en firmas de la fachada.

### Historial de revisiones hasta convergencia

P1 detectó que un `installBackend` público convertiría el bootstrap en una mutación accesible a consumers y lo sustituyó por descubrimiento de servicio.

P2 revisó lifecycle y determinó que el backend no debe poseer estado: sólo delega en los owners existentes, evitando un segundo ledger.

P3 contrastó el plan con G2/G3 y retiró cualquier intento de mover `AnatomyMovement` o `AnatomyRuntime` en este sprint. La pasada completa siguiente no produjo cambios.

## 4. Modelo adversarial base

Se cubrieron runtime con exactamente un backend, API-only sin backend, provider duplicado, consultas en DISABLED/BINDING/READY y adapter de gravedad presente/ausente. La ausencia de provider no puede convertirse en READY ni inventar contacto/raycast. Un provider duplicado no se selecciona por orden de classpath: se rechaza explícitamente.

El holdout base fue inspeccionar por reflexión las firmas declaradas de `AnatomyApi`, buscando cualquier tipo de `collision.internal` que hubiera sobrevivido por accidente. La campaña adversarial ampliada de los sprints G1 está delegada al agente paralelo indicado por el propietario y puede reabrir S01 si encuentra un fallo.

## 5. Implementación

- [x] Contrato `AnatomyBackend` con defaults fail-closed.
- [x] `ScaleAnatomyBackend` como única implementación de producción.
- [x] Service descriptor de producción.
- [x] `AnatomyApi` delegando sin imports internos.
- [x] Tests S01 añadidos.

No hubo cambios de plan después de P3.

## 6. Test matrix

| Ataque / propiedad | Requisito | Nivel | Resultado ejecutado |
| --- | --- | --- | --- |
| Backend ausente falla cerrado | FR-003, NFR-021 | GameTest | PASS en suite G1 registrada |
| Único backend Scale-owned | FR-001 | GameTest | PASS en suite G1 registrada |
| Holdout: firmas sin `collision.internal` | NFR-021/025 | GameTest/reflection | PASS en suite G1 registrada |
| Suite previa completa | no regresión | GitHub Actions `./gradlew build` | PASS |

## 7. Fallos encontrados y bucles

El candidato inicial `f87e4e1d406fb7c58bf1df80d3b8f8cd831806dd` pasó GitHub Actions run `34582769682`, pero una auditoría posterior de S04 detectó que `S01PublicApiBoundaryTests` todavía no figuraba en `fabric-gametest`. Ese run demuestra compilación/regresión del árbol, **no** la ejecución de las aserciones S01.

La evidencia de tests real se obtiene después de registrar S01-S04 en el test mod: run `34584717556`, intento 2, job `103216390687`, ejecutó **256/256 required GameTests**; el snapshot posterior `1a19ec31cdaeb8c0d98cc86b327adb1661e09632` volvió a ejecutar **256/256** en run `34585112975`, job `103217403352`.

## 8. Revisión final

La revisión de implementación recorrió frontera API, lifecycle, ownership, fail-closed, gravedad, service loading, tests y exclusiones G2/G3. No produjo cambios de implementación de S01. El cierre formal queda condicionado únicamente a integrar la campaña adversarial paralela y repetir la revisión final si esa campaña modifica el árbol.

## 9. Cierre

- [x] implementación del scope;
- [x] CI con tests S01 realmente ejecutados;
- [ ] campaña adversarial paralela integrada o sin fallos pendientes;
- [ ] pasada final completa posterior a esa campaña;
- [ ] S01 cerrado formalmente.

### Evidencia

Candidato lógico inicial: `f87e4e1d406fb7c58bf1df80d3b8f8cd831806dd`.

Evidencia vigente de las aserciones S01: `1a19ec31cdaeb8c0d98cc86b327adb1661e09632`, GitHub Actions run `34585112975`, job `103217403352`, **256/256 required GameTests passed**.
