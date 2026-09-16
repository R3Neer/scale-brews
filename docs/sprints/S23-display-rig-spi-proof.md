# S23 — DisplayRig stays behind the generic geometry SPI

Estado: **CLOSED — INDEPENDENT ADVERSARIAL CLOSE**.

Rol principal: **IMPLEMENTER**. El cierre independiente y cualquier holdout adicional pertenecen al ADVERSARY.

S23 ejecuta el corte mínimo de **G3 tarea 7** y **FR-018**. La tarea es deliberadamente conservadora: mientras no exista un target real de `DisplayRig`/display-composite, Scale Brews no introduce un engine productivo, una jerarquía paralela ni conocimiento hardcoded de esa tecnología.

## 1. Contrato

FR-018 exige que el SPI pueda alojar futuras familias (`DisplayRig`, GeckoLib u otras) sin cambiar el solver físico. Su aceptación mínima es que un engine de prueba registre piezas y use raycast/collision sin modificar el solver.

Por tanto, S23 prueba exactamente esto:

1. un `GeometryEngine` externo/sintético se registra por id mediante `CollisionEngines`;
2. el engine prepara una jerarquía y piezas usando `ModelGeometry` común;
3. `ModelGeometry.evaluate(...)` materializa esas piezas como `ConvexBox` ordinarios;
4. raycast y overlap se resuelven con los métodos existentes de `ConvexBox`;
5. no aparece ninguna clase de producción `DisplayRig`, ningún branch del solver ni ningún binding por especie para satisfacer la prueba.

## 2. Implementación del proof

`S23DisplayRigSpiTests.syntheticDisplayCompositeUsesExistingGeometryAndSolver` registra `scalebrews_test:s23_display_composite` y devuelve un modelo sintético `proof-v1` con un root y una pieza `root/display_panel`.

La prueba prepara el modelo a través del `GeometryEngine` recuperado del registro, lo evalúa con root identidad y `AnatomyFilter.DEFAULT`, y comprueba:

- una única pieza material publicada;
- raycast transversal con impacto en la fracción geométrica esperada;
- overlap positivo para un body contenido en la pieza;
- overlap negativo para un body alejado.

Todo el recorrido posterior a `GeometryEngine.prepare(...)` utiliza las mismas clases comunes que cualquier otra familia de geometría.

## 3. Límites deliberados

S23 **no** implementa:

- extracción desde entidades `Display` reales;
- semántica de composición roots/children todavía no observada en un target;
- pose channels específicos de una tecnología desconocida;
- lifecycle o wire adicionales;
- un `DisplayRig` nominal sólo para poder afirmar que existe.

Si un target real posterior descubre una primitiva reusable que `GeometryEngine -> ModelGeometry` no puede expresar, ese hallazgo deberá volver al SPI con su propio requisito/evidencia. Hasta entonces, añadir abstracciones sería arquitectura especulativa, no compatibilidad.

## 4. Evidencia ejecutada por el implementer

- commit de prueba: `27eb4f8aff784ad008ee23b0c49b055b1d04ede4`;
- registro permanente en la suite ordinary: `774b63d38d5d9eec0246c686a60035a1d639b3f0`;
- workflow focal: `s23-display-rig-spi-proof.yml`, introducido en `fb5cbc4bdce4648179177fa27299646eca20a57c`;
- proof focal **`35103246410`**, job **`104817742057`**: **success**; el mod de pruebas se aisló a `S23DisplayRigSpiTests`, Minecraft 26.2 ejecutó la lane aislada y terminó con **2/2 required GameTests** (el proof S23 más el test base del runner) y `BUILD SUCCESSFUL`;
- ordinary build **`35103246319`**, job **`104817741616`** sobre el mismo snapshot productivo/CI: **success**; la suite permanente aumentó de 424 a **425 required GameTests**, los **425/425** pasaron y el build terminó `BUILD SUCCESSFUL`.

No se modificó `src/main` para el candidato de FR-018: el resultado es precisamente que la familia futura cabe por el SPI y solver ya existentes.

## 5. Cierre adversarial independiente

El ADVERSARY no dio por suficiente que un único engine sintético pudiera devolver una caja. Añadió una prueba metamórfica que obliga al camino físico común a ser **opaco a la familia y a los metadatos del `GeometryEngine`**:

- commit `ff074173e7a1b1e694f695fc52e4bbc8c4876d28`: `S23AdversarialGeometryEngineOpacityTests` registra dos engines con ids, model ids, parámetros, `source` y `version` diferentes que producen geometría material idéntica; exige igualdad de bounds, raycast y resultado de `ConservativeSweep`;
- commit `199bff049506692a2d104518b2588770a3c0612c`: `tools/s23_spi_only_gate.py` impide conocimiento nominal de `DisplayRig`/display-composite dentro de `src/main` mientras no exista un target real;
- commit `a02e04f695944686f4cc1dcbf1619670c2e45cbb`: workflow adversarial con baseline y dos mutation-kills dirigidos;
- commit `d76194273874b5ca9697cd6cb075e843fd648d82`: la lane adversarial pasa a vigilar también el registro permanente de GameTests;
- commit `2ffeb627bc9b397b417393eba6454c49a2a714e9`: el holdout adversarial queda registrado en la suite ordinary.

### Evidencia del árbol final adversarial

Run **`35104624130`** sobre `2ffeb627bc9b397b417393eba6454c49a2a714e9`:

- job **`104822510244`** `engine-opacity`: **success**; gate SPI-only limpio y holdout físico aislado verde;
- job **`104823016011`** `metadata-branch-mutation-kill`: **success**; un mutante que desplaza sólo geometría cuyo `ModelGeometry.source()` identifica la familia display compila y es **KILLED** por el holdout;
- job **`104823015908`** `source-boundary-mutation-kill`: **success**; una clase productiva `DisplayRigSpecialCase` inyectada deliberadamente es **KILLED** por el gate SPI-only.

Además, el primer ejercicio completo de la campaña, run **`35104004704`** sobre `a02e04f695944686f4cc1dcbf1619670c2e45cbb`, quedó igualmente verde y mató ambos mutantes antes de registrar el holdout de forma permanente.

No apareció un RED productivo: el adversario no necesitó modificar `src/main`. El riesgo detectado estaba en la **fuerza de la evidencia**, no en una desviación observada del producto.

## 6. Veredicto de cierre

**S23 queda cerrado adversarialmente.**

La evidencia ahora demuestra algo más fuerte que “un engine sintético funciona”: una vez materializada la geometría común, el solver no puede depender legítimamente de la familia, id o metadatos del engine sin romper el holdout; además, el árbol productivo actual no contiene hardcode nominal de DisplayRig/display-composite.

El gate nominal es deliberadamente temporal y pertenece a esta etapa pre-target. Cuando llegue un target real de DisplayRig, deberá refinarse o retirarse mediante evidencia nueva; no se deberá sortear introduciendo excepciones silenciosas.
