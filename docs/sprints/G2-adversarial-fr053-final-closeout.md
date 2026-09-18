# G2 / FR-053 — cierre adversarial final de la reapertura push

Rol activo: **ADVERSARY**.

Estado: **CERRADO / G2.10 RECERTIFICADO**.

Este cierre revisa la reparación final de la frontera entre bloqueo geométrico moving-platform y `Entity.push` vanilla. No modifica producción.

## 1. Historial de la reapertura

La reapertura original demostró que una pareja anatómica elegible conservaba correctamente un único owner geométrico, pero perdía el impulso vanilla de `Entity.push` al adquirir contacto material.

El primer fix productivo retiró la cancelación anatómica directa de `push`, pero una revisión adversarial posterior encontró una segunda ventana: una relación legacy adquirida antes de activar anatomy podía conservar `PlatformState.support` hasta el siguiente cleanup tick y seguir cancelando `push` durante el handoff a READY.

La reparación final es:

- **`1437795ce18689282b4532fc3644bc4566cd1f58`** — `fix(g2): release legacy push guard on anatomy handoff`.

El hook legacy de `PlatformEntityMixin.push(Entity)` deja de actuar cuando cualquiera de los participantes pertenece ya a la ruta shared-physics. Esto no cambia `PlatformPhysics.suppressPair(...)`, que sigue poseyendo la sustitución del blocker geométrico para una pareja elegible.

## 2. Holdout de ownership legacy → anatomy

El adversario endureció el oracle en:

- **`5b4b516b0d460671d15ec6e53fe42011dff85a9d`** — `test(g2): pin legacy-to-anatomy push ownership boundary`.

La misma pareja atraviesa tres estados:

1. sin soporte gestionado: `push` vanilla produce respuesta medible;
2. tras adquirir soporte por la ruta legacy real mediante `Entity.move`: ambas direcciones de `push` quedan suprimidas por el owner legacy;
3. sin esperar un cleanup tick, se activa READY y se establece contacto material anatómico: ambas direcciones recuperan exactamente la respuesta vanilla medida en el control.

Evidencia:

- workflow `g2-vanilla-push-proof`: run **`35327758929`**, job **`105544729000`** — **success**;
- build del mismo SHA: run **`35327758984`** — **success**;
- artifact **`10539368207`**, SHA-256 **`865ff29a05c58c7f4205f11713c005d5e000352abaf5eab4bbfd0417d4095c6b`**.

Este resultado mata tanto una reparación que mantenga la supresión legacy después del handoff como una reparación demasiado amplia que desactive la supresión antes de que shared physics tome ownership.

## 3. Pareja no elegible: sin pared anatómica

La lane `g2-ineligible-no-wall-proof` no escuchaba cambios en `PlatformEntityMixin.java`, pese a que ese archivo forma parte de la frontera FR-053. El adversario corrigió el hueco de CI en:

- **`47db343dde7db545e48f39f548840956977ab9ad`** — `ci(g2): recertify eligibility on push-hook changes`.

Evidencia sobre la misma producción:

- run **`35327844908`**, job **`105545001610`** — **success**;
- build del mismo SHA: run **`35327844879`** — **success**;
- artifact **`10539502957`**, SHA-256 **`4a4c8f7159217a2804d98da7fb749847bccddf914c153968cabc7297390ceb45`**.

La pareja fuera de ratio/policy sigue sin adquirir collider anatómico ni contacto retenido.

## 4. Pareja elegible: un único owner geométrico

Sobre el commit productivo `1437795...`:

- workflow `g2-single-blocking-owner-proof`: run **`35205403082`**;
- job normal **`105149830359`** — **success**;
- job `pair-suppression-mutant-must-die` **`105149830787`** — **success**.

Artifacts:

- normal **`10489777286`**, SHA-256 **`4f4ed28a6f592f0075607a17dfa12d1923d3c4ea9492e5dd2af5be927ecf1077`**;
- mutant **`10489462405`**, SHA-256 **`ec93434638b9b1c89348823f81abc6d3fb0af43552d3018a3969f1e2b2859a2f`**.

El mutante que reactiva el blocker vanilla sigue muriendo. Por tanto el fix de `push` no reintroduce doble ownership geométrico.

## 5. Zero-change post-fix

La comparación `1437795... → 47db343...` contiene únicamente:

- `G2VanillaPushReadyTests.java` — hardening adversarial;
- `g2-ineligible-no-wall-proof.yml` — ampliación del trigger.

No existe ningún cambio posterior en `src/main` o `src/client`.

## 6. Rojos S08 coexistentes

El snapshot `1437795...` mantiene rojas las lanes `s08-prepared-adversarial-proof` y `s08-adversarial-boat-pair-suppression`. No se atribuyen a esta reparación G2:

- la lane del barco ya fallaba con la misma aserción en run `35130296400`, SHA `bdbd267d2862e55533609dfda9c229906e82bd1b`, anterior a `1437795`;
- el fallo preparado pertenece a la deuda S08/prepared ya separada del contrato de `push`.

Se conservan como trabajo independiente; no se usan ni como evidencia a favor ni como veto artificial del recierre FR-053.

## 7. Veredicto adversarial

FR-053/FR-093 quedan recertificados en las fronteras que motivaron la reapertura:

- no-platform: sin pared anatómica;
- platform: un solo blocker geométrico, anatomy;
- `Entity.push`: vanilla exactamente una vez;
- legacy → READY: cambio de owner inmediato, sin depender del siguiente tick;
- legacy puro: conserva su propia supresión mientras sigue siendo owner;
- el mutante de doble blocker continúa muerto.

**G2.10 puede marcarse cerrado. G2 vuelve a estado CERRADO.**

El siguiente gate abierto del plan es **G3 task 8 / S22**.
