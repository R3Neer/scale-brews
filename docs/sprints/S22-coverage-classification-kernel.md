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

El discovery usa la clase base declarada por `EntityType`; no instancia ni spawnea entidades para inferir su tipo. El adaptador sólo se invoca desde tooling/acceptance y no está conectado a tick, spawn, movimiento ni lifecycle runtime.

## 3. Reglas de clasificación

La clasificación base es derivada, no una segunda tabla de autoridad:

- `FULL`: existe binding canónico por defecto y no declara estados incompatibles conocidos;
- `SAFE_PARTIAL`: el binding por defecto declara estados excluidos, sólo existen bindings de variant, o existe una limitación explícita que rebaja conservadoramente cobertura real;
- `EXCLUDED`: excepción técnica explícita con razón y sin binding canónico que afirme cobertura;
- `UNRESOLVED`: el target fue descubierto pero no existe binding ni exclusión técnica aprobada.

Una declaración explícita nunca puede fabricar `FULL`. `SAFE_PARTIAL` tampoco puede fabricarse sin al menos un binding canónico. `EXCLUDED` no puede ocultar silenciosamente un binding existente.

## 4. Identidad reproducible del target

`CollisionCoverageDiscovery.Target` congela:

- id del target;
- versión exacta de Minecraft;
- namespaces incluidos;
- mapa ordenado de inputs versionados/hashes aportados por el workflow de acceptance.

`CollisionCoverageDiscovery.Artifact` combina esa identidad con la representación canónica del coverage report antes de calcular SHA-256. Por tanto, el mismo target + mismos bindings/clasificación regeneran el mismo digest y cualquier cambio declarado en los inputs del target cambia la identidad del artefacto aunque las filas casualmente sean iguales.

## 5. Fases implementador

- [x] I1 — introducir kernel puro/determinista `CollisionCoverageScanner` sobre `CollisionBindingCatalog`.
- [x] I2 — emitir una fila única por id, ordenada de forma reproducible, con evidencia mínima de selectors/engines/root/estados excluidos.
- [x] I3 — exponer counts, `requireResolved()` y SHA-256 sobre una representación canónica estable.
- [x] I4 — tests implementer para FULL derivado, variant/state partial, exclusión técnica, unresolved gate, orden y digest.
- [x] I5 — conectar discovery de tooling al registry objetivo usando `EntityType.getBaseClass()`, con target/version/input identity y sin scans en runtime/hot path.
- [ ] I6 — ejecutar build/GameTests del corte completo y registrar únicamente evidencia realmente ejecutada.
- [ ] I7 — revisión implementer completa; cualquier holdout adversarial permanece independiente.

## 6. Fuera de scope de este corte

- G6: completar y cerrar exhaustivamente cada fila de todos los `LivingEntity` Minecraft 26.2;
- completar familias/engines que el reporte revele como parciales o unresolved;
- target privado VanillaPlus y sus namespaces/hashes concretos;
- lifecycle S21, network/prediction G4 y física G2/FR-053;
- cualquier invocación del scanner desde el hot path del servidor.

G6 y los targets de acceptance alimentan este mismo discovery/kernel; no redefinen sus estados ni mantienen una segunda tabla de cobertura.
