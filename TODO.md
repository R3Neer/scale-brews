# Scale Brews — active work

New requirements are appended here; they do not replace unfinished work.

- [x] Save and push the compiling tutorial prototype.
- [x] Document commit/push policy and current behavior.
- [x] Revised walking/sprint balance from the latest request (supersedes the original walking-unchanged balance).
- [x] Beacon primary powers at pyramid tier 3, level II upgrade at tier 4.
- [x] Physical scale transition over 20 ticks, including removal and power changes.
- [x] Original programmatically drawn pixel art and English/Spanish translations.
- [x] Validate runtime behavior, package, commit by concern and push.

## Latest additions (retaining the other packages)

- [x] Walking multipliers: Growth 1.08/1.16/1.24, Shrinking .92/.84/.76.
- [x] Sprint relative to walking: Growth 1.20/1.12/1.05, Shrinking 1.50/1.90/2.50; stack with Speed but remain slower alone than equivalent Speed while sprinting.
- [x] Physical exhaustion multipliers: Growth 1.10/1.20/1.30, Shrinking .95/.90/.85; scoped to movement, jumping and attacks, not global hunger processing.
- [x] Replace rejected bottle effect icons with carefully reviewed person silhouettes and up/down arrows; study vanilla art and references, no image generation.

## Additional mechanics package (added; original work retained)

- [x] Growth landing shockwave above 3 blocks: level-dependent bounded knockback; only III above 6 blocks deals radial damage, capped at 4 HP.
- [x] Gust/ground particles and a heavy landing sound; no explosion or block damage.
- [x] Shrinking fall damage x0.92/x0.84/x0.76, composed with vanilla protection.
- [x] Shrinking movement-vibration detection radius: walking x0.75/x0.5/x0.25, sprinting x1/x0.75/x0.5; vanilla crouch and interactions preserved.
- [x] Swift Sneak synergy only with Shrinking II/III, capped at walking speed.
- [x] Pressure plates: Shrinking II ignores insensitive stone/iron plates; III ignores all; tags allow extensions.
- [x] Shrinking III cannot trample farmland or turtle eggs.
- [x] Growth resists soul sand and berry slowdown; preserves Soul Speed and berry damage; tagged, scoped integration.
- [x] Regression tests for these mechanics and documentation of deviations.

The package's exclusions apply to its new mechanics; the previously approved beacon and scale blend were retained. The latest explicit walking/sprint rework supersedes the earlier speed requirements.

## Decisions and follow-up

- Special head scaling is cancelled; head proportions remain vanilla.
- Preserve the current attribute balance, including growth scale +0.96 per level.
- Levels III remain potion-only for beacons.
- Existing brewing remains in scope; extended redstone variants are future work.
- Detailed collision/mount/elytra and Combatify/Alex's Mobs integration QA remain follow-up work.
- The new walking penalty means Swift Sneak III + Shrinking III reaches Shrinking's walking speed, not unmodified player speed.
