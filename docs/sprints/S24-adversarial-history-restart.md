# S24 — adversarial lifecycle/history restart model

Rol activo: **ADVERSARY**.

Estado: **SUBCONTRATO CERRADO ADVERSARIALMENTE**. Esto no cierra G3.9 completo.

## 1. Riesgo atacado

S24 separa dos conceptos que no pueden volver a mezclarse:

- `AnatomyFrameHistory.transition(...)` clasifica control de lifecycle del receiver (`CONTINUE`, `RESTART`, `REJECT`);
- `AnatomyFrameHistory.accept(...)` muta una única vida causal y, por tanto, sólo puede aceptar continuidad de identidad exacta.

La regresión observada durante S24 demostró que esta frontera es atacable: convertir un cambio de generación en un `return false` dentro de `accept(...)` hace que el caller no pueda distinguir una identidad causal incompatible de un simple paquete fuera de orden. Peor aún, omitir uno de los ejes de generación permitiría plegar dos vidas causales en el mismo history.

## 2. Holdouts

`S24AdversarialHistoryRestartTests` fija dos propiedades independientes:

1. **retrack puro**: `trackingGeneration++` con binding estable debe clasificarse `RESTART`, pero un `accept(...)` directo sobre el history viejo debe lanzar `IllegalArgumentException`, no devolver `false` ni mutar `current`; el mismo paquete sólo se acepta en un history nuevo;
2. **rebind puro**: `bindingGeneration++` con tracking estable también debe clasificarse `RESTART` y no puede absorberse dentro del history anterior.

El primer caso es especialmente importante porque `trackingGeneration` es el eje nuevo introducido por S24 y no estaba cubierto por los holdouts históricos de S00.

## 3. Mutantes ejecutados

El workflow adversarial mata:

- **soft-reject mutant**: sustituye el hard reject de cambio de identidad en `accept(...)` por `return false`;
- **tracking-axis mutant**: retira `trackingGeneration` de la identidad comprobada por `accept(...)`.

Ambos mutantes compilaron antes de ser ejecutados contra el holdout. Por tanto, su muerte prueba semántica y no una rotura trivial del build.

## 4. Evidencia

- holdout: commit `e77108892a55333eada2aadabc5147fa4fd77dd1`;
- modelo adversarial: commit `e44de2c9b43a9a78a5924ad88d96b2e199815dd3`;
- workflow mutation-kill: commit `4a69934aef1df28fd48f7efdd8abd5e58d729291`;
- run adversarial `35119326521`:
  - `history-restart-holdout`, job `104872764365`: **success**;
  - `tracking-axis-mutation-kill`, job `104872764527`: **success**, mutante compiló y murió;
  - `soft-reject-mutation-kill`, job `104872764657`: **success**, mutante compiló y murió;
- build general del mismo snapshot: run `35119326357`, job `104872763570`: **success**;
- registro permanente en ordinary: commit `0c0c6ffeb434d83b04c866911e9f802cff523a6a`;
- build posterior con el holdout ya incluido permanentemente: run `35120025978`, job `104875144999`: **success**.

## 5. Alcance del cierre

Queda cerrado adversarialmente únicamente el contrato `transition vs accept`: `RESTART` es control de lifecycle del receiver y jamás autorización para plegar una nueva identidad causal dentro del history anterior.

G3.9 sigue abierto por dimensión/replay, reconnect, reload válido/inválido, orden live START/STOP/unload/replacement y el bound de retención de generaciones documentado aparte.
