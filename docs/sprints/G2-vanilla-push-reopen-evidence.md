# G2 adversarial reopen — vanilla push preservation

Estado: **ROJO PRODUCTIVO / FR-053 REABIERTO**.

Esta evidencia registra una reapertura localizada de G2 provocada por una precisión posterior del contrato físico. No invalida la evidencia de S05-S14 sobre broadphase, CCD, contacto, carry, ownership o budgets; invalida únicamente la parte de FR-053 que permitía suprimir el `push` vanilla de la pareja soporte-cuerpo.

## Contrato

El contacto anatómico puede sustituir el **bloqueo geométrico** de la pareja cuerpo/soporte cuando la pareja es realmente elegible como moving platform. No puede convertir una entidad no elegible en una pared. Incluso para una pareja elegible y con contacto material confirmado, `Entity.push` debe conservar la respuesta vanilla exactamente una vez: la moving platform sigue empujando al cuerpo con la semántica vanilla mientras Scale posee soporte, collider anatómico y carry.

El requisito canónico está en `ENTITY_COLLISIONS_REQUIREMENTS.md`, FR-053/FR-093.

## Ruta productiva observada

`PlatformEntityMixin` intercepta `Entity.push(Entity)` en `HEAD` y cancela la llamada cuando existe una relación legacy de soporte o cuando `AnatomyApi.ready(self) && AnatomyMovement.suppressesPush(self, other)`. `AnatomyMovement.suppressesPush` devuelve `true` precisamente para la pareja con contacto material soporte↔cuerpo.

Por tanto la sospecha adversarial era concreta: una pareja anatómica correctamente gestionada podía conservar soporte/carry pero perder por completo el impulso vanilla.

## Evolución del oracle

### Intento 1 — verde inválido

- Test inicial incorporado en `02dc82b1dcae1433e89284b6357a51c2bcb74d2d`.
- Workflow aislado incorporado en `4afcc19dcdb9c683125e874ec1e3f16894e7ebec`.
- Run `34878390388`: verde.
- Artifact `10361627823`, SHA-256 `47e8e7bd5afc2242c6d9a7cf4b056bb887cdc3059a0838222e608bf22974625c`.

**Clasificación:** oracle incompleto. El fixture activaba `AnatomyMovement` directamente pero no llevaba `AnatomyApi` a `READY`; por ello no atravesaba el guard real del mixin que cancela `push`. El verde no es evidencia de conformidad.

### Intento 2 — READY, pero sin binding canónico

- Test READY separado en `0167f7ea1fa9304acf97f59b575a9d81940e2b44`.
- Lane READY en `2bc0f574ee62dae982d0c8792010ca2184bd752a`.
- Run `34884834050`: rojo antes del punto causal.
- Fallo: `Holdout must acquire the exact material pair currently selected by production push suppression`.
- Artifact `10364592762`, SHA-256 `15b94a99abd2ca390cb09e8874019e7250ae0e50c75225813939ab3645c38ea6`.

**Clasificación:** fixture incompleto. En `READY`, una registration sin binding canónico falla cerrada por `Platforms.eligible`; el test atravesaba `READY`, pero no podía demostrar una pareja moving-platform válida. Este rojo tampoco prueba el defecto de `push`.

### Intento 3 — rojo causal

El fixture final, commit `6aba43f236ff7f675ca4c135f6fe981fd11a7cdc`, construye una sesión `READY` preparada con:

- modelo canónico mínimo `test:g2_push_model`;
- perfil canónico para `minecraft:cow`;
- policy con ratio máximo `0.85`;
- `AnatomyDefinition` válida y pose `scalebrews:static`;
- body a escala `0.2`;
- registration que hereda el binding canónico;
- precondiciones explícitas `AnatomyApi.ready(...)`, `Platforms.eligible(body, support)`, contacto material real y `AnatomyMovement.suppressesPush(body, support)`.

Run focal `34885201453`, job `104114021333`: **ROJO causal**.

Control vanilla READY pero todavía no gestionado:

```text
body    = ( 0.02236068006654798, 0.0, 0.0)
support = (-0.02236068006654798, 0.0, 0.0)
```

Misma llamada `support.push(body)` después de adquirir contacto material:

```text
body    = (0.0, 0.0, 0.0)
support = (0.0, 0.0, 0.0)
```

Mensaje exacto del holdout:

```text
A moving-platform contact must preserve vanilla Entity.push exactly once:
vanilla body=(0.02236068006654798, 0.0, 0.0)
vanilla support=(-0.02236068006654798, 0.0, 0.0)
managed body=(0.0, 0.0, 0.0)
managed support=(0.0, 0.0, 0.0)
```

Artifact `10364073081`, SHA-256 `40668506c3918d53d5540123990cdbfe560303cea137d0d77719c3236b5d79e6`.

## Clasificación

**Bug de producción confirmado.** La cancelación del `Entity.push` de la pareja material borra completamente el impulso vanilla. El adversario no modifica producción. La corrección pertenece al implementador.

## Condiciones de recierre

FR-053/G2 no vuelve a cerrado hasta que, sobre el mismo contrato:

1. el holdout READY/canónico conserve la respuesta vanilla exactamente una vez después de adquirir contacto material;
2. una pareja fuera de elegibilidad moving-platform no adquiera collider anatómico tipo pared y conserve la física vanilla;
3. la corrección no reintroduzca doble empuje, doble carry ni dos owners de bloqueo;
4. ordinary + lane focal queden verdes sobre el mismo candidato;
5. una revisión adversarial posterior no requiera nuevos cambios de producción.
