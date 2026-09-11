# S06 — Identidad causal de intervalos materiales

Estado: **EN EJECUCIÓN**. Segundo sprint de G2.

## 1. Tesis y scope

S06 tiene una tesis única: **cada cambio material aceptado de un soporte debe producir un intervalo certificado con identidad monotónica propia del binding, y esa identidad debe permitir distinguir avance, replay, gap/stale y varias contribuciones dentro del mismo tick antes de entrar al resolver físico**.

Requisitos primarios: FR-049, FR-050, FR-051 y FR-055. Requisitos de apoyo: NFR-001, NFR-002, NFR-004, NFR-007, NFR-015, NFR-017 y NFR-018.

S06 no resuelve todavía cuerpos contra el intervalo ni integra derived carry: `MaterialEventDispatcher` seguirá siendo el scheduler de S07. Tampoco cierra sliding/multicontacto/separation recovery, chains o receipts de transporte.

## 2. Estado inicial

- `GeometryProvider.MotionIntervalHandle` ya une dos `QueryFrame` bajo una `GeometryIdentity` y un `materialSerial`, pero nadie posee todavía la asignación/replay fence de ese serial en runtime.
- `MaterialEventDispatcher.EventId` es un contador del scheduler. Reingerir el mismo `MotionIntervalHandle` crea otro `EventId`; por sí solo no demuestra FR-050.
- `ModelGeometryProvider.motion()` conserva un único intervalo tick→tick y `MotionSnapshot` exigía `toTick == fromTick + 1`; eso no podía representar dos root mutations materiales durante el mismo tick.
- `PlatformEntityMixin.move` ya captura y observa root antes/después, pero sólo como `RootFrame`; no retiene todavía el `QueryFrame` material anterior.
- `AnatomyMovement.tick` avanza providers a 20 Hz, pero no publica todavía un batch de handles para las mutaciones de joints detectadas en esa cadencia.

## 3. Invariantes

- `materialSerial` es monotónico dentro de una `GeometryIdentity`/binding y se reinicia sólo cuando cambia esa identidad causal.
- Un intervalo válido conecta exactamente el frame material actualmente aceptado con un frame posterior del mismo identity; nunca reconstruye un before desde estado vivo posterior.
- Misma authority tick es legal si frame serial/root/joints avanzan: el reloj de Minecraft no colapsa dos mutaciones causales distintas.
- Replay exacto no ejecuta trabajo físico de nuevo.
- Un gap/out-of-order no se “rellena” inventando historia; se devuelve estado explícito y conservador.
- unavailable, cambio de gravity, cambio de binding/epoch/revision, teleport/rebind y rewind son barreras, no intervalos continuos.
- El tracker no posee entidades ni geometría; sólo identidad/serial/fence y se puede limpiar por lifecycle.
- La captura root del mixin sigue siendo síncrona en world thread.

## 4. Plan de implementación

- [x] I1 Añadir un tracker de intervalos causales con outcomes `ADVANCED`, `UNCHANGED`, `REPLAY`, `GAP_OR_STALE`, `DISCONTINUITY` y serial material monotónico.
- [x] I2 Hacer que el tracker compare `GeometryIdentity` + frame serial y no UUID/network id sueltos.
- [x] I3 Permitir `MotionSnapshot` con `toTick == fromTick` y tiempos no decrecientes, manteniendo prohibidos rewinds/non-finite data.
- [x] I4 Añadir `GeometryProvider.interval(entity, handle)` fail-closed por defecto.
- [x] I5 Implementar `ModelGeometryProvider.interval` desde los `QueryFrame` del handle, usando `motionBetween` y nunca estado root/joint actual.
- [ ] I6 Añadir a `AnatomyMovement` una captura/commit root explícita que conserve el `QueryFrame` before y produzca un intervalo después de `observeRoot` si hubo cambio material.
- [ ] I7 En el tick de providers, capturar before→after y producir una lista/batch de intervalos joint sin depender del orden de `IdentityHashMap`.
- [ ] I8 Mantener fences por soporte/binding en weak identity state y limpiarlos/incrementarlos en register, invalidation y deactivate según lifecycle.
- [ ] I9 Exponer una seam interna de `poll/consume` que S07 pueda conectar al dispatcher sin volver a deducir identidad.
- [x] I10 Añadir holdouts S06 del núcleo causal; faltan los holdouts runtime de I6–I9.
- [ ] I11 Ejecutar suite completa y revisión estructural final.

## 5. Modelo adversarial previo

1. **same-tick double root**: dos avances frameSerial dentro del mismo game tick producen materialSerial N y N+1, no uno solo.
2. **exact replay**: volver a presentar before/after ya aceptados devuelve `REPLAY` y no incrementa serial.
3. **stale/out-of-order**: presentar un before anterior al último accepted no genera un nuevo intervalo.
4. **gap**: saltar desde before distinto del último accepted devuelve `GAP_OR_STALE`; no interpola ni inventa frames intermedios.
5. **identity reuse**: misma UUID/entityId con nueva binding generation/epoch inicia otro stream y no hereda replay fence.
6. **gravity discontinuity**: DOWN→NORTH no genera `MotionIntervalHandle`.
7. **unavailable barrier**: un endpoint unavailable corta continuidad y el siguiente available no barre a través del hueco.
8. **root+joints same tick**: dos contribuciones legítimas del mismo soporte en el mismo tick conservan dos seriales materiales.
9. **joint batch permutation**: orden de registro/mapa no altera el orden canónico del batch publicado.
10. **provider live-state trap**: `ModelGeometryProvider.interval` debe derivar de handle.before/after; mutar la entidad después de capturar el handle no cambia el motion certificado.
11. **rebind/teleport**: la barrera de lifecycle impide que un before del binding anterior continúe en el nuevo.
12. **serial overflow**: `Long.MAX_VALUE` no wrappea; falla explícitamente antes de publicar identidad ambigua.

## 6. Revisión del plan

P1 separa replay/serial de la resolución física para que S07 reciba una fuente causal ya estable.

P2 no pone la deduplicación dentro de `MaterialEventDispatcher`: ese scheduler debe poder procesar derived work; la identidad/replay de intervalos externos pertenece al runtime que conoce `GeometryIdentity` y lifecycle.

P3 trata same-tick como caso normativo, no excepción. `authorityTick` ordena tiempo de mundo; `frameSerial/materialSerial` ordenan causalidad dentro del tick.

P4 evita hacer del tracker otro owner de geometry. El provider certifica el motion del handle; el tracker sólo certifica continuidad e identidad.

P5 deja explícitamente fuera la aplicación CCD y derived carry. No aparece otro cambio de scope respecto de P4; el plan converge.

## 7. Reconciliación obligatoria antes de cerrar G2

`main@39824ddfeb708825e6aaf4abc5efb6bd0d9ac284` contiene el workstream de gravedad de Tiny Mounts. G2 no podrá considerarse cerrado hasta reconciliar `chatgpt-editing` con ese main y revalidar server+client.

La reconciliación debe dejar `io.github.r3neer.scalebrews.integration.gravity.GravityFrames` como única autoridad transversal Scale para leer gravedad efectiva. `collision.internal.GravityFrames` deberá desaparecer como autoridad duplicada o quedar únicamente como delegación transitoria sin ownership propio. `collision.api.GravityFrame` conserva el tipo/contrato de frame de Entity Collisions; `RootTransformProvider` conserva la separación conceptual entre root transform y body gravity. No se introducirá ninguna API Tiny-Mount-específica y G7/Clinging no se adelanta.

## 8. Criterio de cierre

S06 se cierra sólo cuando:

- intervalos same-tick y tick→tick están representados sin rewind;
- replay/gap/stale/discontinuity son outcomes explícitos;
- root y joint paths reales producen handles desde frames capturados, no reconstruidos;
- `ModelGeometryProvider.interval` usa únicamente provenance del handle;
- los holdouts anteriores están registrados y verdes junto con toda la suite;
- una pasada final no detecta otra fuente que pueda volver a aplicar un intervalo externo sin pasar por el replay fence.

G2, además, requerirá la reconciliación de gravedad con `main@39824dd…` descrita arriba antes de su cierre final.
