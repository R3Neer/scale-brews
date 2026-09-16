# S24 — adversarial runtime reload evidence

Rol: **ADVERSARY**.

Estado: **VERDE para el subcontrato de reload live probado aquí**. Esto no cierra por sí solo G3.9 ni sustituye dimensión/reconnect/tracking/replacement.

## Contrato atacado

El bloque live de reload debe separar dos resultados:

- candidato aceptado: la revisión avanza y una entidad viva conserva UUID/network id, pero recibe provider, registration generation y `bindingGeneration` causales nuevos;
- candidato rechazado: la autoridad aceptada anterior permanece exactamente intacta, sin reconstrucción equivalente ni mutación parcial previa al rechazo.

Esto implementa la barrera de catalog/binding de FR-082 y la atomicidad de NFR-016.

## Rojo de fixture encontrado

El primer run focal del candidato `e2523eb358b2078a28531e2a90b4ab7554c27b20` fue rojo:

- workflow `s24-runtime-reload`: run `35126859556`, job `104897921602`;
- causa: `Missing referenced model: scalebrews_test:s24_reload_cow_v2`.

No era un fallo del reload de producción. El `ResourceManager` de prueba publicaba el JSON bajo `scalebrews/entity_geometry/cow.json`, mientras el binding referenciaba `scalebrews_test:s24_reload_cow_v2`. `WorldAnatomyCatalog` deriva la identidad canónica del modelo desde la ruta del recurso, por lo que el fixture fabricaba `scalebrews_test:cow` aunque el contenido interno declarase otro id.

Se corrigió únicamente el fixture en `204aa5b427af57de067e6583ba80e42da66a0cb8`, haciendo que la ruta del recurso derive del id exacto del modelo. La lane focal posterior `35127504867`, job `104900062153`, pasó completa.

## Mutation kills

Workflow adversarial: `s24-adversarial-runtime-reload`, run `35127601848`, commit `58502027646d582a5c89ad3c326aac3fab68e3a5`.

Los tres jobs terminaron `success`:

1. `reload-baseline`: producción sin mutar pasa la suite focal.
2. `binding-generation-mutation-kill`: se mutó el allocator para devolver siempre `nextBindingGeneration` sin incrementarlo. El mutante compiló y murió exactamente en el invariante del rebind que exige provider/registration/binding causales nuevos.
3. `rejected-candidate-mutation-kill`: se mutó `reload(...)` para ejecutar `state.entities.clear()` antes de validar el candidato. El mutante compiló y murió exactamente en el invariante que exige que un candidato rechazado deje el soporte anterior todavía runtime-owned.

Por tanto, el verde no depende sólo de que `reload()` termine sin excepción: la suite distingue reutilización de identidad causal y mutación parcial previa al rechazo.

## Resultado

El subcontrato live demostrado queda adversarialmente defendido:

- reload aceptado conserva identidad Minecraft pero renueva autoridad causal;
- reload rechazado conserva la autoridad aceptada exacta;
- `bindingGeneration` no puede reutilizarse silenciosamente;
- la validación del candidato no puede destruir estado runtime antes de comprometer la revisión.

No se hicieron cambios de producción durante esta revisión; sólo se reparó el fixture de recursos y se añadieron pruebas/workflows de evidencia.
