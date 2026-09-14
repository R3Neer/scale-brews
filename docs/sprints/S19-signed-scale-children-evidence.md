# S19 — Evidencia adversarial de `scaleChildren=false` con escala firmada

Estado: **DEFECTO PRODUCTIVO ABIERTO / IMPLEMENTADOR**.

Esta evidencia pertenece a S19 y no modifica código de producción. El objetivo es fijar una divergencia causal entre el renderer Citadel real del input de aceptación y la compensación de escala que serializa `AdvancedModelBoxGeometryExtractor`.

## Fuente exacta

El holdout usa los mismos inputs externos fijados por `tools/s19-external-inputs.lock.json`:

- Minecraft `26.2`;
- Alex's Mobs Continued `2.1.9`;
- CodxLib `1.5.1`;
- Cloth Config fijado por el lock;
- hashes SHA-256 comprobados antes de ejecutar la prueba.

La fixture parte de `ModelGrizzlyBear` real y muta únicamente la escala de `body` a `(-1.5, 0.75, 1.25)`. Se ejecutan dos variantes sobre material fresco:

1. `scaleChildren=true`, control de propagación firmada;
2. `scaleChildren=false`, caso que requiere compensar la escala del padre en la cadena de transform de sus hijos.

El oracle captura vertices del renderer Citadel real y comprueba además el transform helper materializado por el DTO. No deriva el resultado esperado de la implementación del extractor.

## Semántica exigida

Cuando `scaleChildren=false`, la compensación que se inserta bajo el part escalado debe conservar el signo y ser el recíproco componente a componente de la escala materializada por el renderer.

Para `(-1.5, 0.75, 1.25)`, el helper esperado es:

- X: `-2/3` (`-0.6666667f`);
- Y: `4/3` (`1.3333334f`);
- Z: `0.8f`.

Una reflexión en un eje no puede convertirse en una escala positiva enorme: el signo forma parte del transform espacial observado por el renderer y de la topología de la jerarquía resultante.

## Defecto observado

En el snapshot probado, `AdvancedModelBoxGeometryExtractor` calcula la compensación mediante:

```java
float ix = 1f / Math.max(sx, 1.0e-4f);
float iy = 1f / Math.max(sy, 1.0e-4f);
float iz = 1f / Math.max(sz, 1.0e-4f);
```

Para `sx=-1.5f`, `Math.max(-1.5f, 1.0e-4f)` devuelve `1.0e-4f`, de modo que el extractor produce `ix=10000f` y pierde por completo la reflexión original.

## Ejecución causal

Workflow: `s19-signed-scale-children-proof`

- run: `34893813120`;
- job: `104142688968`;
- artifact: `10367213456` (`S19-signed-scale-children-proof`);
- SHA-256 del artifact: `957f40f1f2c1da5b9ab3f09c0ed64e80e092c191f63108ddae01334b389381b7`.

El control `scaleChildren=true` pasa antes del fallo:

```text
S19_SIGNED_SCALE PASS source=test:s19_signed_scale_propagating scaleChildren=true helpers=0 comparedVertices=120 renderVertices=360
```

El caso `scaleChildren=false` falla exactamente en el recíproco firmado:

```text
Signed scaleChildren inverse is not the exact reciprocal:
expected=[-0.6666667, 1.3333334, 0.8]
actual=[10000.0, 1.3333334, 0.8]
```

Por tanto el rojo no procede de classloading, resolución de jars, OpenGL/OpenAL, el control de escala firmada ni un mismatch genérico de geometría. El primer punto de divergencia es la compensación X que ha perdido el signo.

## Condición de cierre

S19 no puede declarar cerrada la semántica `scaleChildren=false` hasta que:

1. producción reproduzca la compensación firmada exacta del dialecto Citadel fijado;
2. `s19-signed-scale-children-proof` quede verde con los mismos inputs pinned;
3. una segunda ejecución adversarial demuestre que reintroducir la pérdida de signo vuelve a matar el oracle por la causa esperada;
4. se revise por separado el comportamiento del dialecto para escalas de magnitud cercana a cero, sin inferirlo de este defecto ni sustituirlo por una regla inventada.

La corrección productiva corresponde al implementador. El adversario conserva el holdout y la evidencia y no cambia el extractor.
