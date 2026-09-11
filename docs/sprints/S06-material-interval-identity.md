# S06 — Identidad causal de intervalos materiales

Estado: **CERRADO**. Segundo sprint de G2.

## 1. Tesis y scope

S06 tiene una tesis única: **cada cambio material aceptado de un soporte debe producir un intervalo certificado con identidad monotónica propia del binding, y esa identidad debe permitir distinguir avance, replay, gap/stale y varias contribuciones dentro del mismo tick antes de entrar al resolver físico**.

Requisitos primarios: FR-049, FR-050, FR-051 y FR-055. Requisitos de apoyo: NFR-001, NFR-002, NFR-004, NFR-007, NFR-015, NFR-017 y NFR-018.

S06 no resuelve todavía cuerpos contra el intervalo ni integra derived carry: `MaterialEventDispatcher` sigue siendo el scheduler de S07. Tampoco cierra sliding/multicontacto/separation recovery, chains o receipts de transporte.

## 2. Estado inicial

- `GeometryProvider.MotionIntervalHandle` ya unía dos `QueryFrame` bajo una `GeometryIdentity` y un `materialSerial`, pero nadie poseía todavía la asignación/replay fence de ese serial en runtime.
- `MaterialEventDispatcher.EventId` era un contador del scheduler. Reingerir el mismo `MotionIntervalHandle` creaba otro `EventId`; por sí solo no demostraba FR-050.
- `ModelGeometryProvider.motion()` conservaba un único intervalo tick→tick y `MotionSnapshot` exigía `toTick == fromTick + 1`; eso no podía representar dos root mutations materiales durante el mismo tick.
- `PlatformEntityMixin.move` ya capturaba y observaba root antes/después, pero sólo como `RootFrame`; no retenía el `QueryFrame` material anterior.
- `AnatomyMovement.tick` avanzaba providers a 20 Hz, pero no publicaba un batch de handles para las mutaciones de joints detectadas en esa cadencia.

## 3. Invariantes cerrados

- `materialSerial` es monotónico dentro de una `GeometryIdentity`/binding y se reinicia sólo cuando cambia esa identidad causal.
- Un intervalo válido conecta exactamente el frame material actualmente aceptado con un frame posterior del mismo identity; nunca reconstruye un before desde estado vivo posterior.
- Misma authority tick es legal si frame serial/root/joints avanzan: el reloj de Minecraft no colapsa dos mutaciones causales distintas.
- Replay exacto no ejecuta trabajo físico de nuevo.
- Un gap/out-of-order no se rellena inventando historia; produce `GAP_OR_STALE` y sólo puede reanclar conservadoramente sin publicar el tramo perdido.
- unavailable, cambio de gravity, cambio de binding/epoch/revision, teleport/rebind y rewind son barreras, no intervalos continuos.
- El tracker no posee entidades ni geometría; sólo identidad/serial/fence y se limpia por lifecycle.
- La captura root del mixin sigue siendo síncrona en world thread.

## 4. Implementación cerrada

- [x] I1 Añadir `MaterialIntervalTracker` con outcomes `ADVANCED`, `UNCHANGED`, `REPLAY`, `GAP_OR_STALE`, `DISCONTINUITY` y serial material monotónico.
- [x] I2 Comparar `GeometryIdentity` + frame serial, no UUID/network id sueltos.
- [x] I3 Permitir `MotionSnapshot` con `toTick == fromTick` y tiempos no decrecientes, manteniendo prohibidos rewinds/non-finite data.
- [x] I4 Añadir `GeometryProvider.interval(entity, handle)` fail-closed por defecto.
- [x] I5 Implementar `ModelGeometryProvider.interval` desde los `QueryFrame` del handle, usando `motionBetween` y nunca estado root/TRS vivo posterior.
- [x] I6 Añadir captura/commit root explícita mediante `MaterialIntervalRuntime.RootCapture`, conservando el `QueryFrame` before y produciendo el intervalo después del root mutation.
- [x] I7 Observar frames post-tick de joints desde `AnatomyRuntime.publish`, ordenando soportes por UUID/id para no depender del orden de weak/identity maps.
- [x] I8 Mantener fences por soporte/binding en weak identity state; reset/stop, teleport/remove/unavailable y demás barreras cortan continuidad sin reciclar seriales dentro del mismo binding.
- [x] I9 Exponer cola canónica one-shot `MaterialIntervalRuntime.poll` y `AnatomyRuntime.pollIntervals` para que S07 conecte el dispatcher sin rededucir identidad.
- [x] I10 Añadir y registrar los siete holdouts S06.
- [x] I11 Ejecutar suite completa y revisión estructural final.

## 5. Modelo adversarial y resultado

1. **same-tick double root**: frameSerial 1→2→3 en el mismo tick genera materialSerial 1 y 2.
2. **exact replay**: repetir before/after devuelve `REPLAY` y no incrementa serial.
3. **stale/out-of-order**: un before anterior al accepted no genera nuevo intervalo.
4. **gap**: saltar un frame devuelve `GAP_OR_STALE`; la reanudación no publica historia fabricada.
5. **identity reuse**: nueva binding identity inicia otro stream y no hereda replay fence.
6. **gravity discontinuity**: cambio de gravity frame no genera intervalo continuo.
7. **unavailable/lifecycle barrier**: corta continuidad; el siguiente frame válido sólo la vuelve a sembrar.
8. **root+joints same tick**: contribuciones causales distintas conservan seriales propios.
9. **joint batch permutation**: el batch se ordena canónicamente antes de exponerlo a S07.
10. **provider live-state trap**: mover, rotar y escalar la entidad después de capturar el handle no cambia el motion certificado por `ModelGeometryProvider.interval`.
11. **rebind/teleport**: las barreras invalidan pending work que no puede cruzar lifecycle.
12. **serial overflow**: incremento usa `Math.incrementExact`; frame serial máximo no se convierte en wraparound ambiguo.

Durante la revisión apareció además un defecto antes de la campaña final: una versión intermedia del runtime podía reanclar silenciosamente un `before` adelantado y convertir un gap en un avance. `f1d6290b598733459654b3df20808161074c78f7` lo corrigió: un gap nunca publica intervalo; como máximo reancla en el `after` como barrera conservadora. También se fijó que un serial ya asignado a pending work y luego invalidado por lifecycle no se reutiliza.

## 6. Evidencia de cierre

Snapshot de cierre funcional: `2e8aee0fa679705673f60ecfc38f6088f3c6bfff` (`test(collision): register S06 material interval holdouts`).

GitHub Actions run **`34612764313`**, job **`103307096858`**:

- **284 tests registrados y ejecutados**;
- **284/284 required GameTests passed**;
- `BUILD SUCCESSFUL in 1m 25s`;
- artifact **`10268749377`**;
- artifact SHA-256 **`6944ffc9c3ac9d204c6a9739dd1f3fc3d58b9b231e6ccd9d50190ad9fad975db`**.

La pasada inmediatamente anterior (`f1d6290…`) era verde pero seguía en 277 tests; eso reveló que la clase S06 compilaba sin estar registrada como `fabric-gametest`. `2e8aee0…` añadió el entrypoint y elevó el total esperado a 284, cerrando esa falsa sensación de cobertura antes de aceptar S06.

## 7. Revisión final

La revisión final no encontró otra ruta de producción S06 que pueda publicar un intervalo externo sin pasar por el replay fence. `MaterialIntervalTracker` conserva sólo identidad/serial/continuidad; `MaterialIntervalRuntime` posee la cola/fences de runtime; `ModelGeometryProvider` certifica la trayectoria desde endpoints ya capturados; `MaterialEventDispatcher` permanece sin absorber esta responsabilidad y se conecta en S07.

No se marca como cerrada todavía la aplicación exactly-once de **trabajo físico**: S06 garantiza identidad/replay de intervalos; S07 debe demostrar que el dispatcher y el solver consumen cada intervalo una sola vez y mantienen ancestry/derived work.

## 8. Reconciliación obligatoria antes de cerrar G2

`main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` contiene el workstream de gravedad de Tiny Mounts. G2 no podrá considerarse cerrado hasta reconciliar `chatgpt-editing` con ese main y revalidar server+client.

La reconciliación debe dejar `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como única autoridad transversal Scale para leer gravedad efectiva. `collision.internal.GravityFrames` deberá desaparecer como autoridad duplicada o quedar únicamente como delegación transitoria sin ownership propio. `collision.api.GravityFrame` conserva el tipo/contrato de frame de Entity Collisions; `RootTransformProvider` conserva la separación conceptual entre root transform y body gravity. No se introducirá ninguna API Tiny-Mount-específica y G7/Clinging no se adelanta.

**S06 cerrado. S07 es el siguiente frente de G2.**
