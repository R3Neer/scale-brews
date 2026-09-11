# S01 — Frontera pública y backend explícito

Estado: **candidato de implementación; pendiente de evidencia CI antes del cierre**.

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

### Checklist

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

### Clases de equivalencia y fronteras

- runtime con exactamente un backend;
- runtime/API-only sin backend;
- más de un backend visible;
- consultas mientras el feature está DISABLED/BINDING/READY;
- adapter de gravedad presente/ausente.

### Estados/secuencias/combinatoria

La inversión de dependencias no puede modificar la máquina DISABLED→BINDING→READY ni retener estado al cambiar de sesión. El backend es stateless y esas transiciones siguen perteneciendo a `AnatomySession`/runtime.

### Fault/lifecycle/network attacks

La ausencia de provider no puede convertirse en READY ni inventar contacto/raycast. Un provider duplicado no se selecciona por orden de classpath: se rechaza explícitamente.

### Propiedades metamórficas/diferenciales

Para un runtime con el backend Scale, cada método de la fachada debe ser semánticamente equivalente a la delegación previa directa. La transformación sólo cambia la dirección de dependencia.

### Holdout reservado

Tras implementar se inspeccionarán por reflexión las firmas declaradas de `AnatomyApi`, buscando cualquier tipo de `collision.internal` que haya sobrevivido por accidente.

## 5. Implementación

### Checklist

- [x] Contrato `AnatomyBackend` con defaults fail-closed.
- [x] `ScaleAnatomyBackend` como única implementación de producción.
- [x] Service descriptor de producción.
- [x] `AnatomyApi` delegando sin imports internos.
- [x] Tests S01 añadidos.

### Cambios de plan durante implementación

Ninguno después de P3.

## 6. Test matrix

| Ataque / propiedad | Requisito | Nivel | Resultado |
| --- | --- | --- | --- |
| Backend ausente falla cerrado | FR-003, NFR-021 | GameTest | pendiente CI |
| Único backend Scale-owned | FR-001 | GameTest | pendiente CI |
| Holdout: firmas sin `collision.internal` | NFR-021/025 | GameTest/reflection | pendiente CI |
| Suite previa completa | no regresión | `./gradlew build` CI | pendiente CI |

### Segunda pasada adversarial

Pendiente tras resultado CI.

### Mutation checks

Se consideran mutaciones mínimas: devolver READY en backend ausente, aceptar dos providers y reintroducir un tipo interno en una firma. Los tres ataques tienen oráculo directo en los tests del sprint.

## 7. Fallos encontrados y bucles

Ninguno registrado antes de CI.

## 8. Revisión final

La revisión de código previa a CI confirma que la fachada ya no importa internals y que no se ha movido ownership físico. El cierre final requiere la pasada posterior a CI y se registrará sólo con evidencia ejecutada.

## 9. Cierre

### Checklist de requisitos

- [ ] evidencia CI de la suite nueva y existente;
- [ ] segunda pasada adversarial;
- [ ] pasada final completa sin cambios;
- [ ] `VALIDATION.md` actualizado con evidencia ejecutada;
- [ ] S01 cerrado.

### Evidencia

Pendiente.

### Commit

Pendiente de commit candidato y validación CI.
