# S24 — delayed contact across support rebind RED

Rol: **ADVERSARY**.

Estado: **RED reproducible / contacto no cercado por la vida causal del soporte**.

## Contrato atacado

Este caso es independiente de los REDs de cambio de dimensión. No hay teleport ni cambio de `ClientLevel`.

Ataca principalmente:

- **FR-080**: cada contacto de red debe validarse contra la identidad causal pertinente antes de materializarse;
- **FR-082**: un rebind/reemplazo de binding es una discontinuidad que no puede reutilizar estado temporal de la vida anterior;
- **NFR-017**: eventos retrasados anteriores a una barrera de lifecycle deben rechazarse.

## Hipótesis

`AnatomyContactPayload` ordena el stream por la identidad del **cuerpo** (`trackingGeneration + sequence`) y nombra el soporte mediante UUID/network id/revision/piece/local coordinates.

No transporta la `bindingGeneration` de la vida física del **soporte**.

Por tanto, si un contacto se crea cuando el soporte está en binding `N`, queda retrasado en la red y el soporte pasa a binding `N+1` conservando UUID, network id, epoch, revisión, modelo y nombres de pieza, el receptor no dispone de una prueba explícita de que ese contacto pertenece a `N`.

## Holdout integrado

Test: `S24ContactSupportRebindReplayClientProof`.

Commit del test: `c1505125b0eeed82aacc4c8367342feb23a93124`.
Workflow: `.github/workflows/s24-adversarial-contact-rebind-replay.yml`.
Commit workflow: `6ec184cf15e8f595308bded3a30a4da241f554bc`.

El test volvió a ejecutarse automáticamente sobre el implementer fix de dimensión:

- candidato: `cb98dfbf03f19b01ac12b991115540503842751c`;
- run `35130030485`;
- job `104908455740`;
- resultado: **failure**.

Error exacto:

`Pre-rebind contact became authoritative against support binding N+1`

Los avisos de narrator/OpenAL/Realms del runner no son la causa; el cliente integrado completa el escenario y falla exactamente en la aserción causal final.

## Escenario

1. Se arranca una vaca anatómica estática y se espera a que el cliente tenga catálogo y presentation frame válidos.
2. En servidor se captura el `PublishedFrame` autoritativo del soporte bajo `bindingGeneration = N`.
3. Se crea un cerdo pequeño y se construye un contacto geométricamente válido sobre `root/body/cube_0` de la vaca.
4. Se asigna al paquete la tracking generation server-side real del **cuerpo**. El contacto **no se envía todavía**: simula un paquete válido creado bajo `N` pero retrasado en la red.
5. Se detiene `AnatomyRuntime` para impedir publicación automática adicional, manteniendo viva la conexión y las entidades vanilla.
6. Se verifica en cliente que el cerdo existe y que no hay ningún contacto previo aceptado.
7. Se envía manualmente una pose del mismo soporte con misma UUID/entityId/epoch/revision/model, pero `bindingGeneration = N+1`; el cliente acepta la nueva presentation identity.
8. Se vuelve a verificar que el rebind por sí solo no ha inventado un contacto.
9. Se entrega por primera vez el contacto construido bajo `N`.
10. Producción actual lo acepta y `presentationContact(...)` vuelve a materializarlo contra el soporte `N+1`.

Como el paquete nunca había sido visto antes, este RED no depende de perder un watermark cliente previo.

## Causa observable

El contacto sólo contiene identidad suficiente para localizar el soporte actual, no para demostrar qué **vida causal del binding del soporte** certificó sus coordenadas locales.

`AnatomyContactInbox` puede ordenar el stream del cuerpo por `trackingGeneration/sequence`, pero ese eje no cambia necesariamente cuando el soporte se re-bindea.

`AnatomyMovement.confirm(...)` comprueba UUID/revisión/pieza/cara y geometría actual. Si la nueva vida del soporte conserva esos nombres y revisión, las coordenadas locales de un contacto antiguo pueden reinterpretarse contra el binding nuevo.

En otras palabras: la identidad causal del cuerpo y la identidad causal del soporte son dos ejes distintos. El primero no sustituye al segundo.

## Restricciones de la reparación

No se prescribe una arquitectura concreta, pero el comportamiento correcto debe demostrar simultáneamente que:

- un contacto creado bajo soporte binding `N` y retrasado hasta después de `N+1` es rechazado aunque nunca hubiera sido aceptado antes por el cliente;
- un contacto realmente producido bajo `N+1` sí puede aceptarse;
- el receptor no depende sólo de nombres de pieza/revisión si la vida física del soporte cambió;
- la autoridad de esa identidad procede del servidor, no de un contador sintetizado por el cliente;
- el ordering del cuerpo (`trackingGeneration + sequence`) sigue cumpliendo su función y no se reutiliza artificialmente como sustituto de la vida del soporte sin una justificación causal equivalente;
- si se añade identidad al wire, codec/type/version, constructores, fixtures y send paths deben evolucionar de forma coherente;
- late join, reconnect, dimension barriers, unload/replacement y contactos clear/present existentes siguen verdes;
- no se reinterpreta un contacto viejo contra una pieza del nuevo binding sólo porque conserve el mismo id local.

Una solución natural sería transportar/certificar la `bindingGeneration` del soporte en el contacto, pero el holdout acepta cualquier mecanismo equivalente que ordene inequívocamente el contacto respecto al lifecycle del soporte.

## Gate de cierre

Este RED queda abierto hasta que `S24ContactSupportRebindReplayClientProof` pase sin debilitar su expectativa y exista además una prueba positiva de que un contacto fresco del binding `N+1` sigue materializándose correctamente.
