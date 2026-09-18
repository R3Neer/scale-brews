# S24 — adversarial dimension replay RED

Rol: **ADVERSARY**.

Estado: **HISTÓRICO RED → CERRADO ADVERSARIALMENTE**.

## Contrato

Este holdout ataca directamente:

- **FR-080**: los frames de red deben estar cercados por dimensión, epoch, revisión, UUID, network id y binding/tracking generation; los replays cruzados deben rechazarse;
- **FR-082**: un cambio de dimensión es una discontinuidad que debe invalidar estado temporal y fences de forma definida;
- **NFR-017**: cambio de dimensión/teleport son barreras de causalidad y un evento anterior a la barrera debe rechazarse.

El happy-path integrado existente demuestra `Overworld -> Nether -> Overworld` con catálogo de conexión estable y `trackingGeneration++` cuando llega la nueva publicación. No demostraba qué ocurre si un paquete de la primera visita a Overworld reaparece **después del round-trip pero antes de una publicación fresca de tracking**.

## Holdout

Commit de la prueba: `444cd9ed414bc6d25e2b0bf274ba64543aba3ec4`.

Workflow: `.github/workflows/s24-adversarial-dimension-replay.yml`, commit `e2b5cdb32d2011ac1a4ad8a7a1ea0fdde12b051b`.

Run: `35127780191`.
Job: `104900981040`.
Resultado: **failure** en el invariante esperado.

Escenario real del cliente integrado:

1. Se arranca runtime preparado con una vaca en Overworld y se espera hasta que el cliente tenga pose/presentation aceptadas.
2. Se captura el `PublishedFrame` autoritativo exacto y su `trackingGeneration` de la primera visita.
3. Se detiene únicamente `AnatomyRuntime` para impedir que el regreso publique una pose nueva; la conexión y el catálogo cliente siguen vivos.
4. El jugador cambia `Overworld -> Nether`.
5. Se verifica que la pose temporal de Overworld no sobrevivió en el nuevo `ClientLevel` y que el catálogo de conexión sí sobrevivió.
6. El jugador vuelve `Nether -> Overworld` y se verifica que no existe todavía una pose anatómica fresca.
7. Se reinyecta **exactamente el frame y tracking generation de la primera visita a Overworld**.
8. Se espera que la barrera de dimensión lo rechace.

Producción actual lo acepta y vuelve a materializar pose/presentation.

Error exacto:

`Pre-dimension-barrier pose replay became authoritative after A->B->A round trip`

El cliente, el servidor integrado y el escenario llegaron hasta esa aserción; los avisos de narrator/OpenAL/Realms del runner no son la causa del rojo.

## Causa observada

`AnatomyClientNetworking` posee un `TrackingReplayFence` compacto que conserva watermarks cuando una historia completa sale de `frames` por unload, timeout o sustitución de network identity.

Sin embargo, el cambio de `ClientLevel` entra por `useLevel(...)`, que llama a `clearPoses()`. Ese método borra correctamente material temporal del nivel anterior, pero también ejecuta `frameReplayFence.clear()`.

Por tanto, tras `A -> B -> A`:

- el catálogo conserva el mismo epoch/revision de conexión;
- el paquete antiguo vuelve a coincidir con la dimensión A activa;
- `frames` está vacío;
- el replay fence de la primera A también está vacío;
- el receiver no tiene ya ningún watermark con el que demostrar que el paquete pertenece a una vida anterior a la barrera.

El hecho de que el servidor vaya a emitir una `trackingGeneration` mayor cuando el tracking fresco se restablezca no protege la ventana **anterior a que ese paquete nuevo llegue al cliente**.

## Restricciones de la reparación

No se prescribe una arquitectura concreta. Una solución válida debe demostrar simultáneamente:

- el catálogo sigue siendo **connection-scoped** y sobrevive al cambio de dimensión;
- poses, contactos, providers, presentación y demás material del `ClientLevel` anterior se eliminan inmediatamente;
- queda información causal compacta suficiente para rechazar un frame pre-barrera si se vuelve a la misma dimensión antes de recibir tracking fresco;
- una `trackingGeneration` realmente posterior sigue pudiendo reabrir una historia limpia;
- disconnect/host replacement y cambio aceptado de revisión conservan sus propias semánticas de reset, sin arrastrar fences de una conexión/revisión distinta;
- la solución sigue acotada: no vale conservar historias completas indefinidamente para comprar causalidad con una fuga de memoria.

Una posibilidad es separar el reset de **material por nivel** del reset de **watermarks por conexión**, pero el holdout sólo exige el comportamiento observable y no esa implementación.

## Gate de cierre

S24 no debe considerar cerrada la barrera de dimensión hasta que `S24DimensionReplayClientProof` pase en el cliente integrado y el happy-path existente siga demostrando catálogo estable + tracking nuevo + binding estable.

## Resolución posterior

Producción separó la limpieza de material por `ClientLevel` de los replay watermarks de conexión y cercó los fences por dimensión. El holdout original quedó verde sin relajar su expectativa en run **`35130391518`** sobre el candidato integrado `ddc09c...`.

La campaña de dimensión volvió además a quedar mutation-sensitive tras reanclar el mutante al owner actual del tracking ledger: run **`35329062829`**, baseline `105548909539`, tracking-generation mutant `105548909156` y catalog-scope mutant `105548909537`, todos **success**.

**Clasificación vigente:** el replay pose A→B→A de este documento está cerrado. G3.9 permanece abierto por otros subcontratos, actualmente el RED de tracking read-purity.

