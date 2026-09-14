# G2 adversarial reopen — FR-053 pair routing and vanilla push

Estado: **ROJO PRODUCTIVO LOCALIZADO / FR-053 REABIERTO SÓLO POR `Entity.push`**.

Esta evidencia registra una reapertura localizada de G2 provocada por una precisión posterior del contrato físico. No invalida S05-S14 sobre broadphase, CCD, contacto, carry, ownership, budgets ni selección material. La campaña adversarial posterior ha acotado la reapertura a una sola conducta productiva: una pareja soporte-cuerpo anatómica y elegible pierde el impulso vanilla de `Entity.push` cuando existe contacto material.

## Contrato

El contacto anatómico puede sustituir el **bloqueo geométrico** de la pareja cuerpo/soporte cuando la pareja es realmente elegible como moving platform. Tener geometría preparada no basta para crear esa relación.

Por tanto:

1. una pareja fuera de elegibilidad por ratio/policy/state no adquiere collider anatómico y conserva la ruta Minecraft/main;
2. una pareja elegible usa la anatomía seleccionada como owner geométrico de esa relación, sin conservar simultáneamente el blocker vanilla que está siendo sustituido;
3. incluso para una pareja elegible con contacto material confirmado, `Entity.push` conserva la respuesta vanilla exactamente una vez en ambas direcciones de llamada; Scale posee soporte, collider anatómico y carry, no un segundo propietario del impulso vanilla.

El requisito canónico está en `ENTITY_COLLISIONS_REQUIREMENTS.md`, FR-053/FR-093.

## Ruta productiva observada

`PlatformEntityMixin` intercepta dos cosas distintas:

- `canCollideWith(Entity)`: `PlatformPhysics.suppressPair(...)` elimina el blocker vanilla únicamente cuando la pareja compartida es realmente sustituida por anatomía;
- `push(Entity)`: un `@Inject(..., cancellable=true)` cancela la llamada si hay soporte legacy o si `AnatomyApi.ready(self) && AnatomyMovement.suppressesPush(self, other)`.

La primera ruta es necesaria para evitar doble ownership geométrico. La segunda es el defecto confirmado: `AnatomyMovement.suppressesPush` identifica precisamente la pareja soporte↔cuerpo con contacto material, pero cancelar `push` también borra una interacción vanilla que el contrato exige conservar.

## Evolución del oracle de `push`

### Intento 1 — verde inválido

- Test inicial: `02dc82b1dcae1433e89284b6357a51c2bcb74d2d`.
- Workflow aislado: `4afcc19dcdb9c683125e874ec1e3f16894e7ebec`.
- Run `34878390388`: verde.
- Artifact `10361627823`, SHA-256 `47e8e7bd5afc2242c6d9a7cf4b056bb887cdc3059a0838222e608bf22974625c`.

**Clasificación:** oracle incompleto. El fixture activaba `AnatomyMovement` directamente pero no llevaba `AnatomyApi` a `READY`; no atravesaba el guard real del mixin. El verde no es evidencia de conformidad.

### Intento 2 — READY, pero sin binding canónico

- Test READY separado: `0167f7ea1fa9304acf97f59b575a9d81940e2b44`.
- Lane READY: `2bc0f574ee62dae982d0c8792010ca2184bd752a`.
- Run `34884834050`: rojo antes del punto causal.
- Fallo: `Holdout must acquire the exact material pair currently selected by production push suppression`.
- Artifact `10364592762`, SHA-256 `15b94a99abd2ca390cb09e8874019e7250ae0e50c75225813939ab3645c38ea6`.

**Clasificación:** fixture incompleto. En `READY`, una registration sin binding canónico falla cerrada por `Platforms.eligible`; el test atravesaba `READY`, pero no podía demostrar una pareja moving-platform válida. Este rojo tampoco prueba el defecto de `push`.

### Intento 3 — rojo causal unidireccional

El fixture de `6aba43f236ff7f675ca4c135f6fe981fd11a7cdc` construye una sesión `READY` preparada con:

- modelo canónico mínimo `test:g2_push_model`;
- perfil canónico para `minecraft:cow`;
- policy con ratio máximo `0.85`;
- `AnatomyDefinition` válida y pose `scalebrews:static`;
- body a escala `0.2`;
- registration que hereda el binding canónico;
- precondiciones explícitas `AnatomyApi.ready(...)`, `Platforms.eligible(body, support)`, contacto material real y `AnatomyMovement.suppressesPush(body, support)`.

Run `34885201453`, job `104114021333`: **ROJO causal**.

Control vanilla READY todavía sin contacto material:

```text
body    = ( 0.02236068006654798, 0.0, 0.0)
support = (-0.02236068006654798, 0.0, 0.0)
```

Misma llamada `support.push(body)` después de adquirir contacto material:

```text
body    = (0.0, 0.0, 0.0)
support = (0.0, 0.0, 0.0)
```

Artifact `10364073081`, SHA-256 `40668506c3918d53d5540123990cdbfe560303cea137d0d77719c3236b5d79e6`.

### Intento 4 — rojo causal bidireccional vigente

`42ff3180cb42ad72310b98ebcc1b0487d1e197c5` endureció el mismo fixture para medir **ambas direcciones de llamada** antes y después de adquirir el contacto material, dentro del mismo runtime preparado.

Run `34889360628`, job `104127856451`: **ROJO causal**.

Antes del contacto material:

```text
support.push(body)
  body    = ( 0.022360680105594867, 0.0, 0.0)
  support = (-0.022360680105594867, 0.0, 0.0)

body.push(support)
  body    = ( 0.022360680105594867, 0.0, 0.0)
  support = (-0.022360680105594867, 0.0, 0.0)
```

Después de adquirir la relación material gestionada:

```text
support.push(body)
  body    = (0.0, 0.0, 0.0)
  support = (0.0, 0.0, 0.0)

body.push(support)
  body    = (0.0, 0.0, 0.0)
  support = (0.0, 0.0, 0.0)
```

Artifact `10365728249`, SHA-256 `6dc12817aa8f5200e158f499198d56936b8c32484247f182abfade6f679d7064`.

**Conclusión causal:** la cancelación productiva borra el `Entity.push` vanilla en ambas direcciones. Este es el rojo normativo vigente para recerrar FR-053.

## Holdout de frontera — geometría preparada no implica moving platform

`G2EligibilityNoWallReadyTests`, incorporado en `10e77ac8bad805434dbeee8354bdca3cf18c9d14`, usa la misma geometría preparada y el mismo soporte canónico para dos cuerpos:

- player escala `0.2`, dentro de `max_width_ratio=0.85`;
- player escala `2.0`, fuera de elegibilidad.

El test exige:

- `Platforms.eligible` verdadero sólo para el cuerpo pequeño;
- `AnatomyMovement.replacesPair` verdadero sólo para la pareja elegible;
- el cuerpo elegible es frenado por la pared anatómica;
- el cuerpo no elegible recibe exactamente el desplazamiento solicitado en el core anatómico y no adquiere contacto retenido.

Workflow aislado inicial `0c4730452c51026d18f01d42f2a4a8668ba5c016`.

Run `34888057831`, job `104123501242`: **VERDE**, 2 required tests incluidos sentinel.

Artifact `10364939818`, SHA-256 `1d99ce2f817499d20ca40934b201207a7fed371c908ecd9767d5ec5dbbc08647`.

Los triggers se estrecharon después en `b7a3f0eaf0a098a749395aee3506889e131e8072` para escuchar sólo owners causales de esta frontera y no convertir cambios S19 de pose/geometry no relacionados en ruido G2.

**Clasificación:** la frontera ratio/policy está correcta. Geometría preparada no convierte por sí sola a una entidad en pared.

## Holdout de ownership geométrico — un solo blocker para la pareja elegible

La segunda mitad del contrato exige lo contrario para una pareja realmente elegible: si Scale sustituye el blocker geométrico vanilla, debe existir **un único owner geométrico**, la anatomía seleccionada.

### Oracle descartado — cow

La primera versión empleó una vaca. La ejecución normal pasó, pero un mutante CI que deshabilitaba `PlatformPhysics.suppressPair` también sobrevivió. Eso demostró que una cow no era un oracle válido para un blocker rígido de `Entity.move`: reactivar `canCollideWith` no la convertía en la pared dura que el test pretendía observar.

**Clasificación:** oracle débil, descartado. No es evidencia de producción correcta ni incorrecta.

### Hardening numérico del oracle

Una iteración intermedia construía una convexa fina usando directamente coordenadas mundiales aleatorias del GameTest. A varios millones de bloques, el camino `Matrix4f`/float podía colapsar un grosor de `0.2` hasta degenerarlo. Se corrigió en `94b13f9299f1bbc641237ae0cb71cf4d7c6d084d`: la convexa se construye cerca del origen y se traslada después en double.

**Clasificación:** inestabilidad del test, no bug de producción.

### Oracle válido — shulker + mutación

`4afe0f713ef551dde70388eeea3d7e4b49822f9b` sustituyó el soporte por `minecraft:shulker`, que sí aporta un blocker vanilla rígido observable. La anatomía seleccionada se coloca deliberadamente más allá de la AABB vanilla del shulker y el test usa `Entity.move` real.

Workflow endurecido `47eec83e271e65cbe570a0dae128807984ba5578` ejecuta dos jobs sobre el mismo snapshot:

1. **normal:** la pareja elegible atraviesa la AABB vanilla sustituida y se detiene sin penetración en la convexa anatómica;
2. **mutant:** sólo en el checkout CI, `PlatformPhysics.suppressPair` devuelve `false` para la ruta shared-physics, reactivando el blocker vanilla. El GameTest debe morir exactamente en la aserción de que el cuerpo debería haber atravesado esa AABB.

Run `34889213097`:

- job normal `104127363832`: **VERDE**;
- job mutante `104127364122`: **VERDE como harness de mutación**, porque el GameTest mutado murió en la aserción esperada.

Fallo causal del mutante:

```text
Eligible Entity.move must pass through the replaced rigid vanilla support AABB before reaching anatomy:
supportMax=1.2496951E7
finalX=1.2496949939999998E7
```

El body queda detenido por el shulker antes de la anatomía cuando se elimina la supresión de pareja, por lo que el holdout mata exactamente la mutación de doble blocker que pretende detectar.

Artifacts:

- normal `10366286284`, SHA-256 `63b55ef86bbfb134dd4f8c8b7e17627aa89a7e9d0adc7904bfb36e399f4425c3`;
- mutant `10366087011`, SHA-256 `d0a4fd3f3ece917f261cf15e2ba82c78dceb2ee13cf6de24ecf41d0896313586`.

**Clasificación:** ownership geométrico correcto y mutation-sensitive. La pareja elegible usa la anatomía como blocker seleccionado sin conservar simultáneamente el blocker vanilla sustituido.

## Clasificación final de la reapertura

La campaña ha separado tres responsabilidades que antes podían confundirse:

| Frontera FR-053 | Estado adversarial | Evidencia |
| --- | --- | --- |
| pareja no elegible no adquiere pared anatómica | **VERDE** | run `34888057831` |
| pareja elegible tiene un solo owner geométrico | **VERDE + mutante muerto** | run `34889213097` |
| pareja elegible conserva `Entity.push` vanilla exactamente una vez | **ROJO PRODUCTIVO** | run `34889360628` |

Por tanto **G2 no está reabierto como solver**. S05-S14 permanecen cerrados. La única deuda productiva conocida de esta reapertura es la cancelación de `Entity.push` en `PlatformEntityMixin`.

El adversario no modifica producción. La corrección pertenece al implementador.

## Condiciones de recierre

FR-053/G2 no vuelve a cerrado hasta que, sobre un mismo candidato posterior a la reparación:

1. `G2VanillaPushReadyTests` quede verde para `support.push(body)` y `body.push(support)`, preservando la respuesta vanilla exactamente una vez después del contacto material;
2. el holdout de no-wall/elegibilidad siga verde;
3. el holdout de single geometric owner siga verde y su mutante de `suppressPair=false` siga muerto;
4. la corrección no introduzca doble empuje, doble carry ni un segundo owner de bloqueo;
5. la suite ordinary pertinente y las lanes focales queden verdes sobre el candidato;
6. una revisión adversarial posterior no requiera otro cambio de producción.
