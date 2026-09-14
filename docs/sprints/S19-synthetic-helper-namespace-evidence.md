# S19 — evidencia adversarial: namespace del helper sintético

Estado: **RED productivo confirmado**.

## Tesis

`AdvancedModelBoxGeometryExtractor` introduce un `ModelGeometry.Part` sintético para reproducir la semántica Citadel `scaleChildren=false`. En el snapshot causal, dicho helper usa el ID fuente:

```text
<source-part>/unscaled_children
```

Ese segmento pertenece también al dominio válido de `AdvancedModelBox.boxName` (`[A-Za-z0-9_.-]{1,64}`). Por tanto el namespace interno de Scale Brews no es disjunto del namespace estructural de la fuente.

La consecuencia es que un modelo Citadel válido puede ser rechazado únicamente porque un hijo real se llame `unscaled_children`.

## Holdout

Fuente de aceptación: **Alex's Mobs Continued 2.1.9 / Citadel dialect fijado por S19**, sobre `ModelGrizzlyBear` real.

El fixture hace únicamente lo necesario para ejercer la colisión de namespaces:

1. materializa un Grizzly adulto real;
2. fija `body` a escala `(1.5, 0.75, 1.25)` con `scaleChildren=false`, obligando al extractor a necesitar el helper transform-only;
3. renombra un hijo directo real de `body` a `boxName="unscaled_children"`;
4. renderiza primero ese modelo mutado mediante el renderer/model path real de Citadel y exige vértices no vacíos;
5. sólo después solicita preparación mediante `AdvancedModelBoxGeometryEngine`;
6. si la preparación fuese aceptada, compararía los colliders exportados con los vértices del renderer real.

La precondición del paso 4 importa: demuestra que el nombre no hace inválido el modelo para la tecnología fuente. El rechazo es introducido por Scale Brews.

## Resultado causal

Workflow: `s19-helper-namespace-proof`

Run: **34897913959**  
Job: **104156524999**  
Head: **d419d06805ecfb02f22c79e1ceb0088a27d1f170**

Artefacto: **10368698716** (`S19-helper-namespace-proof`)  
SHA-256: **52005ef19556942b24673ebcbcc420592b2af0efa2833b999eadffcd113694e3**

El renderer real supera la precondición. La preparación falla exactamente en:

```text
IllegalArgumentException: Failed to prepare AdvancedModelBox geometry test:s19_synthetic_helper_namespace
Caused by: IllegalArgumentException: Ambiguous AdvancedModelBox part id root/body/unscaled_children
    at AdvancedModelBoxGeometryExtractor.extractPart(...:116)
```

La causa coincide con la arquitectura del extractor: primero registra el helper sintético `root/body/unscaled_children`; después el hijo fuente real construye, a partir de su `boxName`, exactamente el mismo ID y dispara la protección anti-alias.

## Clasificación

**Bug de producción S19, no bug del oracle.**

Razones:

- la fuente exacta real renderiza el fixture antes de pasar por Scale Brews;
- `unscaled_children` satisface el contrato de nombres fuente del extractor;
- S19 exige preservar IDs estables derivados de la jerarquía/nombres canónicos de la tecnología y rechazar alias reales, no reservar silenciosamente un nombre fuente para infraestructura interna;
- `ModelGeometry.Part.id` no impone el alfabeto restringido de `boxName`, por lo que existe espacio para un namespace sintético disjunto sin alterar los IDs fuente.

## Frontera de reparación

El implementador debe mantener simultáneamente estas propiedades:

1. **No relajar la detección de IDs fuente duplicados/ambiguos.** Dos siblings fuente con el mismo nombre siguen siendo input inválido.
2. **No renombrar ni escapar los IDs canónicos de parts/pieces fuente** para acomodar el helper.
3. El helper transform-only debe usar un namespace/segmento sintético que una ruta derivada de `boxName` no pueda producir. Un carácter reservado fuera de `[A-Za-z0-9_.-]` es una solución posible si se mantiene dentro del contrato general de `ModelGeometry.Part.id`.
4. El helper sigue siendo transform-only: no aparece en IDs de `Piece`, filtros ni metadata física fuente.
5. Debe conservarse la semántica exacta de `scaleChildren=false`, incluidos los proofs positivos, firmados y de renderer real ya existentes.

## Revalidación requerida tras el fix

Como mínimo deben quedar verdes en el mismo candidato:

- `s19-helper-namespace-proof`;
- `s19-scale-children-real-proof`;
- `s19-signed-scale-children-proof`;
- `s19-hidden-hierarchy-proof`;
- `s19-source-determinism-proof`;
- `s19-advanced-model-box-proof`.

No se considera reparación válida hacer que el holdout deje de llegar al caso causal mediante una nueva exclusión de nombres o una heurística por especie.
