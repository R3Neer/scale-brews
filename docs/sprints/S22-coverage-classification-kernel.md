# S22 — Coverage classification kernel

Estado: **OPEN / IMPLEMENTATION ACTIVE**.

Rol principal: **IMPLEMENTER**. Holdouts, mutation checks y cierre independiente siguen siendo propiedad del adversario.

S22 ejecuta el primer corte coherente de G3.8: la autoridad de clasificación del coverage report. No adelanta G6 ni convierte una lista manual de especies en catálogo.

## 1. Requisitos congelados

- **FR-038** — cada tipo descubierto por el tooling del target debe recibir exactamente una fila `FULL`, `SAFE_PARTIAL`, `EXCLUDED` o `UNRESOLVED`, con razón y sin omisiones silenciosas.
- **FR-039 / FR-040** — los gates posteriores podrán exigir `UNRESOLVED=0`; S22 debe exponer esa condición de forma mecánica.
- **FR-089..092** — estados incompatibles conocidos y exclusiones técnicas no pueden convertirse en collider por optimismo del scanner.
- **NFR-019 / NFR-020 / NFR-024** — la cobertura se deriva de engines/bindings canónicos; no introduce Java por especie ni conocimiento del modpack.
- **NFR-028 / NFR-032 / NFR-036** — el reporte debe ser reproducible, cambiar identidad cuando cambia la clasificación o cualquier input versionado del target y fallar cerrado ante evidencia insuficiente.

## 2. Tesis del sprint

Al terminar este corte, dado un target versionado y el `CollisionBindingCatalog` canónico, Scale descubrirá automáticamente sus tipos `LivingEntity` desde el registry y producirá una clasificación determinista y auditable de cada id, con digest reproducible y gate `UNRESOLVED=0`, sin asumir que la mera existencia de una entidad o de una integración visual implica compatibilidad física.

El discovery usa `DefaultAttributes.hasSupplier(EntityType<?>)` como criterio estático de tooling para la población `LivingEntity` registrada. No instancia ni spawnea entidades para inferir su tipo. El adaptador sólo se invoca desde tooling/acceptance y no está conectado a tick, spawn, movimiento ni lifecycle runtime.

El experimento adversarial que intentó usar `EntityType.getBaseClass()` como oracle independiente se descartó: en Minecraft 26.2 produjo un conjunto de referencia vacío y no representa de forma fiable la población living del registry.

## 3. Reglas de clasificación

La clasificación base es derivada, no una segunda tabla de autoridad:

- `FULL`: existe binding canónico por defecto y ningún binding aplicable declara estados incompatibles conocidos;
- `SAFE_PARTIAL`: cualquier binding aplicable declara estados excluidos, sólo existen bindings de variant, o existe una limitación explícita que rebaja conservadoramente cobertura real;
- `EXCLUDED`: excepción técnica explícita con razón y sin binding canónico que afirme cobertura;
- `UNRESOLVED`: el target fue descubierto pero no existe binding ni exclusión técnica aprobada.

Una declaración explícita nunca puede fabricar `FULL`. `SAFE_PARTIAL` tampoco puede fabricarse sin al menos un binding canónico. `EXCLUDED` no puede ocultar silenciosamente un binding existente.

## 4. Identidad reproducible del target y del artefacto

`CollisionCoverageDiscovery.Target` congela:

- id del target;
- versión exacta de Minecraft;
- namespaces incluidos;
- mapa ordenado de inputs versionados/hashes aportados por el workflow de acceptance.

`CollisionCoverageDiscovery.Artifact` combina esa identidad con la representación canónica del coverage report antes de calcular SHA-256 y verifica que las filas del report coinciden exactamente con la población descubierta del target. Un artifact construido manualmente no puede declararse resuelto omitiendo ids descubiertos.

El preimage del coverage report usa desde `8972f624` framing por longitud (`collision-coverage-v2`) para todos los campos variables y para cada evidencia de binding. De este modo valores legales que contengan delimitadores como `,`, `=`, `|`, saltos de línea o corchetes no pueden hacer alias antes del SHA-256.

## 5. Fases implementador

- [x] I1 — introducir kernel puro/determinista `CollisionCoverageScanner` sobre `CollisionBindingCatalog`.
- [x] I2 — emitir una fila única por id, ordenada de forma reproducible, con evidencia mínima de selectors/engines/root/estados excluidos.
- [x] I3 — exponer counts, `requireResolved()` y SHA-256 sobre una representación canónica estable e inyectiva sobre los datos admitidos.
- [x] I4 — tests implementer para FULL derivado, variant/state partial, exclusión técnica, unresolved gate, orden y digest.
- [x] I5 — conectar discovery de tooling al registry objetivo usando `DefaultAttributes.hasSupplier`, con target/version/input identity y sin scans en runtime/hot path.
- [ ] I6 — ejecutar build/GameTests del corte completo y registrar únicamente evidencia realmente ejecutada.
- [ ] I7 — revisión implementer completa; cualquier holdout adversarial permanece independiente.

## 6. Hallazgos adversariales ya absorbidos por producción

1. **Completeness bypass**: un `Artifact` ensamblado manualmente con report parcial podía aparentar `UNRESOLVED=0`. `4484fd7` ligó completitud a discovery automático y el holdout adversarial pasó después sin cambios.
2. **Canonical digest alias**: selectores semánticamente distintos como `{a="b,c=d"}` y `{a="b", c="d"}` producían el mismo preimage delimitado. `8972f624` sustituyó el escaping parcial por framing de longitud integral. La prueba adversarial original permanece sin modificar y debe certificar el cierre antes de considerar convergido S22.

## 7. Fuera de scope de este corte

- G6: completar y cerrar exhaustivamente cada fila de todos los `LivingEntity` Minecraft 26.2;
- completar familias/engines que el reporte revele como parciales o unresolved;
- target privado VanillaPlus y sus namespaces/hashes concretos;
- lifecycle S21, network/prediction G4 y física G2/FR-053;
- cualquier invocación del scanner desde el hot path del servidor.

G6 y los targets de acceptance alimentan este mismo discovery/kernel; no redefinen sus estados ni mantienen una segunda tabla de cobertura.
