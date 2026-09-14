# S19 — Evidencia metodológica del oracle adversarial

Estado: **evidencia auxiliar preimplementación**. Este archivo no añade requisitos ni concreta holdouts S19. Documenta un experimento retrospectivo sobre S17 que demuestra qué calidad mínima debe tener un oracle estructural para S19.

## Hipótesis

El proof histórico basado en comprobar que cada vértice renderizado aparece en alguna caja exportada puede aceptar una extracción incompleta. Una prueba más fuerte debe construir la referencia desde el modelo fuente original, no desde el extractor bajo prueba, y comparar también identidad/cardinalidad en ambas direcciones.

## Mutación que sobrevivió al oracle histórico

En el commit `cd5b840023b2dbb27be457630d646bcb290b149e`, una workflow temporal modificó **sólo dentro del runner** `ModelPartGeometryExtractor` para omitir deliberadamente el cubo volumétrico `root/body/cube_0`.

Se ejecutaron la suite cliente focal S17 y el proof original contra el modelo de Minecraft. Ambos terminaron verdes: la mutación sobrevivió pese a faltar geometría real.

Evidencia:

- run `34835030941`;
- job `103946799939`;
- artifact `10343891913` (`S17-retro-missing-cube-mutation`);
- SHA-256 del artifact `4847b9bddb5d446c2d5aab523b2e980891410a96e94c854ba2c2a8eef661e7cc`.

Conclusión: un run verde de engine + inclusion unidireccional `render -> export` no demuestra conservación completa de geometría.

## Oracle estructural independiente

El commit `87089d20d37307f8182d11f44966e320cf793af6` añadió `S17OriginalModelStructuralParityClientTests`, que recorre directamente el `ModelPart` original y construye la referencia sin llamar a `ModelPartGeometryExtractor`.

Ese oracle compara como mínimo:

- conjunto exacto de IDs de pieces;
- cardinalidad de vertices por piece;
- cobertura source -> candidate;
- cobertura candidate -> source;
- snapshots SVG deterministas de referencia y geometría preparada.

El commit `01475b26cef0e795b49b4d580c8e0a9934e5d458` lo integró en la lane S17; el proof sin mutación quedó verde.

## La misma mutación muere

La workflow de mutation testing repitió la omisión de `root/body/cube_0` y ejecutó sólo el oracle estructural independiente. Tras corregir un fallo del propio harness en `875beb58999316b55c062925b99531fb74e403d9`, el experimento terminó correctamente: el GameTest falló por la ausencia exacta esperada y el wrapper clasificó ese fallo como mutación matada.

Mensaje causal observado:

`Original ModelPart structural piece mismatch for cow missing=[root/body/cube_0] extra=[]`

Evidencia:

- run `34835751555`;
- job `103949057971`;
- artifact `10344780088` (`S17-retro-missing-cube-mutation-kill`);
- SHA-256 del artifact `7fed43a2cfdcdbbe87e4990895223a1356932f1cfe04c91aa595f8a35e220701`;
- marcador del harness: `S17_RETRO_MUTATION_KILLED independent original-tree oracle detected missing volumetric source cube`.

## Consecuencia para S19

Cuando exista la implementación S19, los proofs Grizzly/Gazelle no deben reutilizar el extractor productivo ni el legacy para construir su geometría esperada. La referencia debe caminar el modelo/renderer original del jar exacto fijado y poder detectar, como mínimo, tanto piezas/vertices ausentes como piezas/vertices fantasma.

Las mutations relevantes de S19 no se consideran cubiertas porque una lane cualquiera se ponga roja. El harness debe demostrar que mueren por el assert causal previsto. Los escenarios concretos y mutations S19 permanecen reservados hasta la segunda lectura de producción, conforme al TM.
