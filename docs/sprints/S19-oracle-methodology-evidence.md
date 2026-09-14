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

## Diseño del oracle S19: capas que no deben colapsarse

El oracle retroactivo S17 demuestra muy bien paridad de `Piece` final, pero S19 necesita distinguir capas que `AdvancedModelBox` separa semánticamente. Este apartado fija **qué observables comparar**, no fixtures concretas ni una representación productiva obligatoria.

### 1. Identidad de fuente

Cada resultado de aceptación debe quedar ligado a:

- `GeometryEngine` esperado;
- `Request.model` exacto;
- versión/dialecto de la fuente;
- hashes de jars fijados;
- `ModelGeometry.source` y `version` devueltos.

Un resultado espacialmente correcto preparado desde otra fuente/versión no es evidencia equivalente.

### 2. Grafo semántico fuente

La referencia debe construirse directamente desde `parts()`/`AdvancedModelBox.childModels` del modelo original y registrar por part semántica:

- nombre canónico/`boxName`;
- parent semántico;
- orden/cobertura de children sólo donde el orden sea contractualmente relevante;
- transform local que afecta a la geometría propia;
- transform que debe propagarse a los hijos según `scaleChildren`;
- visibilidad estructural (`showModel`).

No debe usar `getDeclaredFields()` del modelo concreto como autoridad primaria de identidad ni `getAllParts()` como lista de roots.

### 3. Normalización de helpers de representación

No debe exigirse igualdad bruta del número de `ModelGeometry.Part`. El DTO posee un único transform local por `Part`, mientras el renderer `AdvancedModelBox` puede aplicar escala a su geometría propia y cancelarla antes de recorrer hijos cuando `scaleChildren=false`.

Una implementación válida puede por tanto necesitar parts sintéticas sin piezas propias para representar esa separación. El legacy ya usa un nodo `.../unscaled_children` con la inversa de escala.

El oracle debe comparar **semántica normalizada**:

- cada part fuente canónica debe tener una representación no ambigua;
- el world transform que alcanza su propia geometría debe coincidir con el renderer original;
- el transform propagado desde una part fuente hasta cada child semántico debe coincidir con la ruta del renderer original;
- parts auxiliares extra sólo son tolerables si son estructurales, deterministas y su composición no inventa ni elimina grados de libertad, piezas o transform neto;
- un helper no puede ocultar duplicación, cycles, alias de IDs ni geometría fantasma.

Así se evita tanto un falso positivo contra una compensación legítima como un falso negativo que acepte un árbol distinto sólo porque, en la pose canónica, termina colocando cajas parecidas.

### 4. Geometría local por cubo

Antes de aplicar `modelTransform`, la referencia debe derivar de los quads/vertices materializados del cubo original y registrar:

- identidad estable de la pieza dentro de su part;
- bounds/vertices efectivos post-inflation;
- intención dimensional pre-inflation necesaria para distinguir corrupción de render-only;
- clasificación física vs render-only.

La comparación debe ser bidireccional por identidad, no un pool global de vertices: source -> candidate y candidate -> source, con cardinalidad suficiente para detectar omisiones y cajas fantasma.

### 5. `modelTransform` por fuente

La matriz de renderer/modelo se comprueba como autoridad separada de los transforms locales de parts. Para una fuente dada deben coincidir:

- la matriz declarada/materializada por la `Source`;
- el frame derivado del renderer original fijado;
- el resultado espacial después de componerla con la jerarquía.

Esto evita que un error de `modelTransform` quede compensado accidentalmente por el error inverso dentro del extractor. Dos errores que se cancelan en world-space siguen siendo dos errores de autoridad.

### 6. Geometría base antes de filtros

La paridad contra modelo original se realiza sobre `ModelGeometry` base **antes** de aplicar `AnatomyFilter`. Sólo después se valida selección declarativa.

De lo contrario una implementación podría borrar durante extracción una part que casualmente también está excluida por el filtro de aceptación y obtener un falso verde. En particular, una comparación filtrada no demuestra que la geometría canónica completa se haya preservado.

### 7. Resultado espacial como última capa, no única capa

Tras validar identidad, grafo, transforms y piezas locales, el oracle sí debe comparar world-space contra el renderer original y producir snapshots/overlays deterministas.

World-space sirve como prueba diferencial final y como detector visual, pero no sustituye las capas anteriores: distintas jerarquías o repartos de transforms pueden ser espacialmente equivalentes en una pose estática y divergir en cuanto una futura pose modifique un joint.

### 8. Diagnóstico de render-only

Una primitiva plana legítima no aparece como `Piece` física, pero su existencia debe permanecer observable en el lado de referencia/proof. El oracle debe poder afirmar simultáneamente:

- la primitiva existía en el renderer original;
- fue clasificada como no volumétrica bajo el dialecto soportado;
- no produjo collider;
- su omisión no alteró IDs/parentage de las piezas físicas restantes.

Esto evita que “no está en el output” sea indistinguible entre clasificación deliberada y extractor que simplemente perdió material.

### 9. Determinismo del propio oracle

La referencia adversarial también debe ser reproducible:

- misma fuente exacta => mismo manifest estructural;
- snapshots con cámara/proyección/tolerancias fijadas;
- serialización/orden estable para diffs;
- ninguna dependencia del orden accidental de reflexión;
- tolerancias numéricas declaradas y acotadas, no adaptadas al candidato.

Un oracle que cambia de opinión según el orden de un `HashMap` sería una contribución muy fiel al ecosistema Java, pero poco útil como evidencia.

## Frontera geometry/pose: `offsetX/Y/Z`

Una inspección temporal adicional del `AdvancedModelBox` del jar Alex 2.1.9 fijado buscó todas las referencias bytecode a `offsetX`, `offsetY` y `offsetZ`. Evidencia: run `34837027943`, job `103953065183`, artifact `10343927810`, SHA-256 `08b22ee90efa6f66b3f4171be06b3047df212da3675d433aa0e3c587f26567ed`. La workflow fue retirada después de capturar el artifact.

Los únicos accesos observados a esos tres fields están dentro de `transitionTo(AdvancedModelBox, float, float)`, donde se interpolan junto con `rotateAngle*` y `rotationPoint*`. No aparecen en `translateAndRotate(PoseStack)`, `render(...)` ni `doRender(...)`.

Consecuencia: para el dialecto exacto fijado, `offsetX/Y/Z` no forman parte del transform espacial usado por el renderer canónico de la geometría. Son estado auxiliar de transición/pose y no deben añadirse por duplicado al extractor S19. Si el futuro sprint de pose Citadel decide modelar `transitionTo`, esa semántica pertenece a aquella autoridad de pose, no a la geometría base de S19.

## Checklist de aceptación post-implementación

Cuando exista producción, la segunda lectura decidirá qué harness concreto materializa estas capas. Como mínimo, la evidencia de Grizzly/Gazelle debe poder responder de forma independiente:

1. ¿La fuente/version/hash es la fijada?
2. ¿Las roots y relaciones semánticas del modelo original están cubiertas una vez y sólo una vez?
3. ¿La geometría propia de cada part recibe el transform correcto?
4. ¿Los children reciben la herencia/cancelación de escala correcta?
5. ¿Cada cubo físico original corresponde a exactamente una pieza física y viceversa, salvo representación equivalente explícitamente demostrada?
6. ¿Las primitivas render-only quedan explicadas y no se transforman en colliders?
7. ¿`modelTransform` coincide por fuente sin compensaciones cruzadas con transforms locales?
8. ¿La geometría base completa es correcta antes de filtros?
9. ¿Los filtros posteriores seleccionan sin reescribir la geometría base?
10. ¿El world-space y los snapshots coinciden con el renderer original?
11. ¿Las mutations elegidas mueren por asserts causales de estas propiedades y no por fallos colaterales del harness?

La forma exacta de IDs auxiliares, fixtures malformed y mutations permanece deliberadamente sin concretar hasta leer la implementación S19 real.
