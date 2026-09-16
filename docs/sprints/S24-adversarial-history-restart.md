# S24 — adversarial lifecycle/history restart model

Rol activo: **ADVERSARY**.

Estado: **HOLDOUT PLANTADO / CI PENDIENTE**.

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

## 3. Mutantes exigidos

El workflow adversarial debe matar al menos:

- **soft-reject mutant**: sustituir el hard reject de cambio de identidad en `accept(...)` por `return false`;
- **tracking-axis mutant**: retirar `trackingGeneration` de la identidad comprobada por `accept(...)`.

Ambos mutantes deben compilar. Si cualquiera conserva verde el holdout, la evidencia no demuestra la frontera lifecycle/history.

## 4. Factura de cierre

Este holdout no cierra G3.9. Sólo puede cerrar el subcontrato `transition vs accept` si:

- el test adversarial pasa sobre producción;
- ambos mutantes compilan y mueren;
- la suite general sigue verde;
- no se toca producción desde el rol adversarial.

Los ejes restantes de G3.9 siguen siendo dimensión, reconnect, reload válido/inválido y orden live START/STOP/unload/replacement.
