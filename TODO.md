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

## Tiny mounts and social physics package

- [x] Extend jump tests for Jump Boost synergy and equivalent-level balance; retain existing jump values.
- [x] Scale direct-player attack knockback by Growth 1.10/1.20/1.30 and Shrinking .90/.80/.70, retaining existing sprint/enchantment contributions. Projectiles, thorns and the landing wave are excluded.
- [x] Preserve vanilla projectile damage independently of the shooter's Growth/Shrinking damage modifiers; runtime arrow/trident regression tests at normal size and levels I-III. No global inverse damage correction.
- [x] Block all Growth levels from riding living entities and safely dismount existing riders, leaving boats/minecarts alone.
- [x] Synced data-driven tiny mount definitions; generic control/movement types; preserve native mount implementations.
- [x] Chicken: Shrinking II+, vanilla saddle, direct control, one rider, held-Space glide without ordinary jump or extra HUD.
- [x] Bee: Shrinking II+, vanilla saddle, Flower on a Stick, bounded look-directed 3D flight; restore AI without steering item.
- [x] Original hand-painted saddle textures and separate animated equipment geometry; JSON selects texture/anchor. Client screenshots reviewed with equipment added/removed; vanilla base skins retained.
- [x] Prevent mounted bees entering hives; external storage uses native dismount placement and clears accumulated rider fall distance.
- [x] Flower on a Stick: one item, hand-authored vanilla-based model, small-flower-tag recipe, EN/ES feedback and names.
- [x] Growth II/III villager fear through local vanilla panic, without gossip, reputation or global hostility changes.
- [x] JSON master/individual tiny-mount switches, independent villager fear, landing push/damage and environmental switches, enabled by default and server-synchronized. Growth mounting restriction stays independent.
- [x] Server/client integration tests, documentation, scoped commits and push.

## Decisions and follow-up

- [x] Small-player first-person camera: scale near clipping and culling together, reduce bobbing with actual size, validate in the client without changing movement or collision.
- [x] Shrinking JUMP_STRENGTH: +2.5/+5/+7.5%, compose with external modifiers and restore on removal.

- Special head scaling is cancelled; head proportions remain vanilla.
- Preserve the current attribute balance, including growth scale +0.96 per level.
- Levels III remain potion-only for beacons.
- Existing brewing remains in scope; extended redstone variants are future work.
- Detailed collision/mount/elytra and Combatify/Alex's Mobs integration QA remain follow-up work.
- The new walking penalty means Swift Sneak III + Shrinking III reaches Shrinking's walking speed, not unmodified player speed.
