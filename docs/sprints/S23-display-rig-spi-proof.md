# S23 — DisplayRig stays behind the generic geometry SPI

Estado: **READY FOR INDEPENDENT ADVERSARIAL CLOSE**.

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

## 4. Evidencia ejecutada

- commit de prueba: `27eb4f8aff784ad008ee23b0c49b055b1d04ede4`;
- registro permanente en la suite ordinary: `774b63d38d5d9eec0246c686a60035a1d639b3f0`;
- workflow focal: `s23-display-rig-spi-proof.yml`, introducido en `fb5cbc4bdce4648179177fa27299646eca20a57c`;
- proof focal **`35103246410`**, job **`104817742057`**: **success**; el mod de pruebas se aisló a `S23DisplayRigSpiTests`, Minecraft 26.2 ejecutó la lane aislada y terminó con **2/2 required GameTests** (el proof S23 más el test base del runner) y `BUILD SUCCESSFUL`;
- ordinary build **`35103246319`**, job **`104817741616`** sobre el mismo snapshot productivo/CI: **success**; la suite permanente aumentó de 424 a **425 required GameTests**, los **425/425** pasaron y el build terminó `BUILD SUCCESSFUL`.

No se modificó `src/main` para cerrar FR-018: el resultado es precisamente que la familia futura cabe por el SPI y solver ya existentes. Este documento entrega el candidato al ADVERSARY; no marca G3.7 cerrado unilateralmente.
