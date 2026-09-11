# S01 — Frontera pública y backend explícito

Estado: **CERRADO**.

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

## 4. Modelo adversarial previo

Se cubrieron runtime con exactamente un backend, API-only sin backend, provider duplicado, consultas en DISABLED/BINDING/READY y adapter de gravedad presente/ausente. La ausencia de provider no puede convertirse en READY ni inventar contacto/raycast. Un provider duplicado no se selecciona por orden de classpath: se rechaza explícitamente.

El holdout reservado fue inspeccionar por reflexión las firmas declaradas de `AnatomyApi`, buscando cualquier tipo de `collision.internal` que hubiera sobrevivido por accidente.

## 5. Implementación

- [x] Contrato `AnatomyBackend` con defaults fail-closed.
- [x] `ScaleAnatomyBackend` como única implementación de producción.
- [x] Service descriptor de producción.
- [x] `AnatomyApi` delegando sin imports internos.
- [x] Tests S01 añadidos.

No hubo cambios de plan después de P3.

## 6. Test matrix

| Ataque / propiedad | Requisito | Nivel | Resultado |
| --- | --- | --- | --- |
| Backend ausente falla cerrado | FR-003, NFR-021 | GameTest | PASS dentro de `./gradlew build` |
| Único backend Scale-owned | FR-001 | GameTest | PASS dentro de `./gradlew build` |
| Holdout: firmas sin `collision.internal` | NFR-021/025 | GameTest/reflection | PASS dentro de `./gradlew build` |
| Suite previa completa | no regresión | GitHub Actions `./gradlew build` | PASS |

### Segunda pasada adversarial

La pasada posterior a implementación no encontró una ruta alternativa de backend, estado duplicado ni exposición de tipos internos en la fachada. Las mutaciones mínimas reservadas, devolver READY con backend ausente, aceptar dos providers o reintroducir un tipo internal en una firma, tienen oráculo directo en los tests añadidos.

## 7. Fallos encontrados y bucles

Ninguno. El candidato compiló y pasó a la primera ejecución CI.

## 8. Revisión final

La revisión final recorrió frontera API, lifecycle, ownership, fail-closed, gravedad, service loading, tests y exclusiones G2/G3. No produjo cambios.

## 9. Cierre

- [x] evidencia CI de la suite nueva y existente;
- [x] segunda pasada adversarial;
- [x] pasada final completa sin cambios;
- [x] S01 cerrado.

### Evidencia

Commit candidato: `f87e4e1d406fb7c58bf1df80d3b8f8cd831806dd`.

GitHub Actions run `34582769682`, job `103209945504`, finalizó **success** el 2026-09-11. El paso `build` ejecutó `./gradlew build` y finalizó correctamente; wrapper validation, Java 25 setup y captura de artefactos también finalizaron correctamente.

La evidencia acumulada de S01 se incorporará también al bloque G1 de `VALIDATION.md` cuando cierre el gate, para no convertir cada commit documental intermedio en una segunda fuente de verdad de estado.
