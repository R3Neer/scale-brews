# S24 — adversarial tracking-generation retention bound

Rol activo: **ADVERSARY**.

Estado: **BLOCKER DE DISEÑO / SIN UMBRAL INVENTADO**.

## 1. Hallazgo

El nuevo fencing S24 introduce en `AnatomyRuntime.State` un ledger server-side por receptor:

```text
ServerPlayer -> (support UUID -> trackingGeneration)
```

El outer map usa weak keys, pero el mapa interior es un `HashMap<UUID,Long>` ordinario. En el estado actual:

- `generation(...)` inserta una entrada para cada UUID visto por el receptor;
- `STOP_TRACKING` **no elimina** la entrada: avanza su generación y la conserva como fence anti-replay;
- no existe TTL, cap, poda ni outcome de saturación para el mapa interior;
- el mapa completo sólo desaparece en reset/reload o disconnect del receptor.

Por tanto, una conexión larga que recorra suficientes zonas puede acumular una entrada por cada entidad distinta observada durante toda la sesión. El weak outer key no acota ese crecimiento mientras el jugador siga conectado.

## 2. Requisitos afectados

- **NFR-011** exige TTL y caps constantes documentados para histories/estado temporal y que una entrada expirada no vuelva a ser autoritativa;
- **FR-080** exige validar `trackingGeneration` antes de materializar frames/contactos;
- **FR-082** y **NFR-017** convierten tracking loss, dimension/disconnect y otras discontinuidades en barreras causales cuyos eventos anteriores no pueden revivir.

Esto crea una tensión real: borrar tombstones sin diseño acotaría memoria pero reabriría replays; conservarlos para siempre preserva replay safety pero viola el requisito de memoria estable.

## 3. Qué NO fija este adversario

No se inventa un límite numérico como `4096`, `16384` o un TTL arbitrario. El contrato canónico no proporciona ese número y elegirlo desde el test sería convertir una decisión de diseño en dogma accidental.

Tampoco se exige que la solución sea un `Map` podado. Son válidos otros diseños, por ejemplo un esquema de generación/nonce que permita demostrar obsolescencia sin conservar indefinidamente cada UUID, siempre que respete autoridad server-side y replay safety.

## 4. Factura mínima para levantar el blocker

Antes de cerrar G3.9 debe existir una política explícita y demostrable para este ledger:

1. límite/TTL constante y documentado o representación equivalente de memoria estable;
2. stress con muchas UUID distintas donde el estado por receptor deje de crecer con el tiempo;
3. un paquete de una tracking window retirada no puede recuperar autoridad por el mero hecho de que su entrada individual haya sido podada;
4. saturación/poda debe tener outcome conservador y no degradar a un scan global;
5. disconnect debe seguir retirando toda autoridad de la conexión anterior;
6. la solución no puede reutilizar `bindingGeneration` como sustituto de `trackingGeneration`.

Hasta que exista ese diseño y su holdout, S24 puede estar verde en tracking/dimensión funcional y seguir **abierto** respecto a NFR-011.
