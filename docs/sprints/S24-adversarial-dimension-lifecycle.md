# S24 — adversarial dimension lifecycle proof

Rol activo: **ADVERSARY**.

Estado: **SUBCONTRATO DE DIMENSIÓN CERRADO ADVERSARIALMENTE**.

## 1. Contrato observado

El proof integrado implementer `S24DimensionLifecycleClientProof` ejecuta una transición real en la misma conexión:

`Overworld A -> Nether -> Overworld A`

La evidencia exige simultáneamente:

- el catálogo de conexión conserva `epoch` y `revision` durante el cambio de `ClientLevel`;
- el estado temporal del nivel anterior desaparece al entrar en otra dimensión;
- al volver a la misma entidad se conserva `bindingGeneration`;
- la nueva ventana de visibilidad usa `trackingGeneration` estrictamente mayor;
- la entidad recuperada conserva UUID/network id y la dimensión del payload coincide con el nivel activo.

## 2. Campaña adversarial

Workflow: `s24-adversarial-dimension-mutations.yml`, commit `f84cc908fa1a14470d4fb9e1b6d80c333e12aa46`.

Se ejecutaron dos mutantes productivos plausibles, ambos compilables:

1. **tracking reuse mutant**: `STOP_TRACKING` deja de avanzar `trackingGeneration`;
2. **catalog scope mutant**: `AnatomyClientSession.useLevel(...)` reemplaza indebidamente el catálogo al cambiar de `ClientLevel`.

Run `35120025963`:

- baseline, job `104875144812`: **success**;
- tracking-generation mutation-kill, job `104875145022`: **success**; el mutante compiló y el proof lo mató;
- catalog-scope mutation-kill, job `104875145260`: **success**; el mutante compiló y el proof lo mató.

Build general del snapshot: run `35120025978`, job `104875144999`: **success**.

## 3. Qué demuestra

La transición de dimensión real es sensible a dos regresiones importantes:

- reutilizar una tracking window previa al volver a la misma entidad;
- confundir ownership de catálogo por conexión con ownership temporal por `ClientLevel`.

Esto aporta evidencia fuerte para FR-080/FR-082/NFR-017 sobre la frontera de dimensión.

## 4. Qué NO demuestra todavía

No se considera cerrado el replay completo de dimensión hasta inyectar o reproducir explícitamente un frame retrasado de la **primera visita A** después del round-trip A->B->A y demostrar que no puede recuperar autoridad frente a la nueva tracking window.

El adversario no exige conservar el mismo `AnatomyFrameHistory` ni una implementación concreta de tombstones. Cualquier diseño que rechace ese replay y mantenga memoria acotada es válido.

G3.9 sigue además abierto por reconnect, reload válido/inválido, orden live de replacement y el bound NFR-011 del ledger server-side de `trackingGeneration`.

## 5. Cierre posterior del replay explícito

La deuda que este documento dejaba abierta quedó cubierta después por dos holdouts integrados:

- pose pre-barrera A→B→A: run **`35130391518`**, verde;
- contacto pre-barrera A→B→A: run **`35130391543`**, verde.

La campaña de mutación de dimensión se reancló posteriormente al owner actual del ledger y volvió a quedar íntegramente verde en run **`35329062829`**: baseline, tracking-generation mutant y catalog-scope mutant.

**Conclusión vigente:** dimensión, replay de pose/contacto y scope del catálogo quedan cerrados como subcontrato. G3.9 sigue abierto por la costura independiente de tracking read-purity.

