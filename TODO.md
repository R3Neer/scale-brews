# Scale Brews — active work

## Living platforms

- [x] Prove moving support and authoritative synchronization with players, mobs and occupied boats; 0/100/200 ms flight-disabled dedicated connections and a 12,000-tick player soak.
- [x] Synced platform policies/profiles, oriented top surfaces, physical transport without pathfinding changes.
- [x] Vehicle/item/falling-block adapters, lifecycle, chains, edge protection and passive-motion accounting; scoped anvil impact without repeated damage.
- [x] Sixteen anatomical profiles, shared model sampling, adapter hooks and bounded first-person camera correction.
- [x] Final build and 54 server tests, real-client suite, scoped implementation commit/push and successful GitHub CI; installed artifact hashes match VP26, Modrinth and the exported MRPack. Previous beta JARs retained outside the mods folders.
- [ ] Full VP26 client acceptance: beta.3 passes all 98 server tests with e4mc restored; permission compatibility, Wilder Wild loaded-registry validation and the regeneration comparison are resolved. The client now reaches dedicated connection but fails network registry loading on duplicate `minecraft:enchantment / enchancement:empty`. This new integration issue remains open; do not suppress duplicate-key validation or remove production mods to turn the suite green.
- [ ] Human QA: all species' animated contact, wall/camera corner cases, lead-dragged boat on a giant with passengers, water transitions, minecart rail transitions, falling blocks hardening/placing after release, crowded mixed chains and late-joining observers. Automated cases do not cover every permutation.
- Existing beta QA and publication tasks below remain pending.

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
- [x] Block all Growth levels from riding living entities and safely dismount existing riders, leaving boats/minecarts alone. Historical baseline: superseded by the effective-scale ratio policy below.
- [x] Synced data-driven tiny mount definitions; generic control/movement types; preserve native mount implementations.
- [x] Chicken: Shrinking II+, vanilla saddle, direct control, one rider, held-Space glide without ordinary jump or extra HUD. Level-based size eligibility is superseded below; other behavior is retained.
- [x] Bee: Shrinking II+, vanilla saddle, Flower on a Stick, bounded look-directed 3D flight; restore AI without steering item. Level-based size eligibility is superseded below; other behavior is retained.
- [x] Original hand-painted saddle textures and separate animated equipment geometry; JSON selects texture/anchor. Client screenshots reviewed with equipment added/removed; vanilla base skins retained.
- [x] Prevent mounted bees entering hives; external storage uses native dismount placement and clears accumulated rider fall distance.
- [x] Flower on a Stick: one item, hand-authored vanilla-based model, small-flower-tag recipe, EN/ES feedback and names.
- [x] Growth II/III villager fear through local vanilla panic, without gossip, reputation or global hostility changes.
- [x] JSON master/individual tiny-mount switches, independent villager fear, combined landing impact and environmental switches, enabled by default and server-synchronized. Growth mounting restriction stays independent.
- [x] Server/client integration tests, documentation, scoped commits and push.

## Effective-scale mounts, creeper explosions and living-entity landings (implemented)

Implemented from the latest specification and covered by the expanded runtime suite. Earlier completed items remain historical records. Only the explicitly replaced mount-size eligibility rules change; retain the other mechanics, saddle artwork/configuration and unfinished follow-up work.

### Living-mount size policy

- [x] Replace direct Growth/Shrinking-level mount-size gates, including existing-rider enforcement, with `riderScale / mountScale <= maxRiderScaleRatio`. Read both entities' real effective SCALE, including other mods' modifiers and gradual transitions; no blanket Growth ban or minimum-Shrinking-level size gate.
- [x] Preserve vanilla mount restrictions: age, state, taming, passenger capacity and other native eligibility rules. Boats and minecarts are exempt from this size check.
- [x] Add data-driven general living-mount ratio definitions/overrides, not scattered Java constants. Establish and document a conservative fallback for unconfigured species during implementation; the specification does not prescribe one.
- [x] Add explicit `"max_rider_scale_ratio": 0.53` to chicken/bee tiny-mount JSON and migrate the old level-based configuration without losing texture/anchor, control, movement or enable/disable settings. General living-mount size policy remains independent of the tiny-mount master switch.

Initial configurable limits:

| Mount | Maximum rider/mount SCALE ratio |
| --- | --- |
| Horse, donkey, mule, skeleton horse, zombie horse | 1.00 |
| Pig | 1.00 |
| Strider | 1.00 |
| Llama, trader llama | 1.00 |
| Camel | 1.10 |
| Happy ghast | 2.00 |
| Chicken tiny mount, bee tiny mount | 0.53 |

- [x] Test Growth II rider on Growth II/III horse (allowed), Growth II rider on Growth I horse (denied), and Growth I rider on normal horse (denied).
- [x] Test the intentional tiny-mount matrix: Shrinking II rider + normal mount allowed; II + Shrinking I mount denied; III + Shrinking I mount allowed; III + Shrinking II mount denied. Preserve the exact 0.53 limit.
- [x] Test external SCALE modifiers, both rider/mount transitions, safe enforcement for existing riders, vanilla restrictions, boat/minecart exemptions and data overrides/server-client consistency.

### Creeper explosion power

- [x] Multiply the creeper's actual final vanilla explosion power: Shrinking I/II/III x0.90/x0.80/x0.65; normal x1.00; Growth I/II/III x1.15/x1.30/x1.50. Preserve this explicit level table rather than silently replacing it with a different scale formula.
- [x] Apply after the normal/charged power is determined; no additional charged-creeper cap. Vanilla base 3/6 must yield Growth III 4.5/9.0 and Shrinking III 1.95/3.9. Extremely strong charged Growth explosions are intentional.
- [x] Modify the existing vanilla explosion, not a separate simulated blast. Radius, damage and block destruction must naturally follow its real power, preserving vanilla explosion rules.
- [x] Test normal/charged creepers and every level, including direct, splash, lingering and other normal effect application paths; verify actual explosion behavior, not only the multiplier helper.

### Growth landing impact for compatible LivingEntity

- [x] Generalize the existing player landing wave to compatible LivingEntity with Growth, including vanilla/modded mobs. Keep unrelated player-only mechanics scoped as before.
- [x] Retain existing balance: valid falls above 3 blocks, Growth I small radial knockback/no damage, II stronger knockback/no damage, III stronger knockback and incidental damage only above 6 blocks, capped at 4 HP. Strength/radius still depend on level and real fall distance.
- [x] Require accumulated real fall distance and a real landing; ordinary jumps and flying mobs briefly touching ground must not trigger a wave without a qualifying fall.
- [x] Retain gust/Wind Burst and terrain particles for mobs, existing landing feedback and configuration switches; no explosion or block destruction and no mace-like damage escalation.
- [x] Preserve the entity's own vanilla fall damage and normal landing vibrations/game events. Avoid species lists: use LivingEntity + Growth + valid fall + landing, with narrow explicit exceptions only for technically incompatible physics.
- [x] Add runtime coverage for player/vanilla-mob landings, generic compatible-mob behavior, flight/no-fall negatives, all levels, bounded damage, particles, self fall damage, landing events and the combined impact switch.

### Compatibility and completion

- [x] Final switch clarification (supersedes separate push/damage options): use one `growth_landing_impact` switch for the entire combined landing effect (radial knockback and bounded Growth III damage plus its feedback). Disabling it never changes ordinary vanilla/Combatify recoil or self fall damage. Migrate obsolete separate switches conservatively and cover the combined behavior in runtime tests/docs.

- [x] Use real effective scale wherever sensible without overriding the explicitly specified creeper multipliers or landing balance. Do not add special Ender Dragon support; preserve vanilla behavior for incompatible multipart entities, bosses or special physics instead of risky hacks.
- [x] Update mechanics/configuration documentation and examples, validate server/client regressions, and commit/push validated milestones. Retain all earlier unfinished QA below.

## Decisions and follow-up

- [x] Small-player first-person camera: scale near clipping and culling together, reduce bobbing with actual size, validate in the client without changing movement or collision.
- [x] Shrinking JUMP_STRENGTH: +2.5/+5/+7.5%, compose with external modifiers and restore on removal.

- Special head scaling is cancelled; head proportions remain vanilla.
- Preserve the current attribute balance, including growth scale +0.96 per level.
- Levels III remain potion-only for beacons.
- [x] Redstone extends Growth I / Shrinking I to 8 minutes; extended Growth converts to extended Shrinking. Preserve splash/lingering conversions; no glowstone on extended variants or redstone on II/III.
- [x] Stabilize the bee's animated body while ridden so saddle and passenger stay aligned; retain wing animation and restore normal bobbing after dismount. Validate in the real client.
- Bounded collision/mount/elytra and Combatify/Alex's Mobs integration tests are implemented. Broader manual modpack playtesting, latency and unusual equipment/physics combinations remain ongoing QA, not a claim of exhaustive compatibility.
- The new walking penalty means Swift Sneak III + Shrinking III reaches Shrinking's walking speed, not unmodified player speed.

## Animated riders and steering-item attraction

- [x] Restore the bee's complete body animation and move/tilt the rendered rider with its saddle in each frame, including scale and yaw changes. Keep physics and first-person camera stable; supersedes the fixed bee body above.
- [x] Verify vanilla pig/strider attraction, then let enabled `item_steered` tiny mounts follow their JSON `steering_item` in either hand even without a saddle, Shrinking or a rider. Preserve native foods, AI priorities and configuration switches; support definitions for additional mobs.
- [x] Validate animated rider alignment, dismount/reset and actual unmounted following; update documentation, package and push.

## First public beta

### Effective-size and placement follow-up (2026-09-06)

- [x] Derive attributes and physical mechanics from actual SCALE, interpolating existing pure-potion anchors; mixed effects and external scale must compose. Landing waves require at least Growth I size; damage requires Growth III size.
- [x] Place boats/rafts and spawn-egg entities on eligible living surfaces by right click, with fit, obstruction, permissions and inventory checks.
- [x] Resolve e4mc command permission compatibility in the development pack.
- [x] Resolve Wilder Wild development registry validation bootstrap using loaded world registries in the test harness, not by changing production world generation.
- [x] Diagnose and validate the natural regeneration comparison with actual VP26 settings: match Combatify's random food-consumption draws between both subjects.
- [x] Rebuild, rerun and document base/server/full-pack checks; install beta.3 with matching hashes in VP26 and Modrinth. Base build, 63 server tests and real-client suite pass; 98 VP26 server tests pass. The separate full-pack client acceptance blocker above remains open.
- User reports successful singleplayer living-platform playtesting; this does not replace the remaining automated full-pack validation.

- [x] Use the explicit `0.1.0-beta.1` prerelease version, pin Loom 1.17.20 and require the tested Fabric API baseline.
- [x] Add a user-facing changelog and replace stale development-only wording.
- [ ] Validate the exact packaged JAR in the full VP26 client and a dedicated-server/client session.
- [ ] After manual approval, publish the same verified artifact as a GitHub prerelease and Modrinth beta; verify tag target, metadata, downloads and digest.

## Scale loot, Animal Weights space and wolf mounts (2026-09-06)

- [x] Audit actual installed entity/item IDs and loot tables; preserve existing effective-scale and tiny-mount architecture.
- [x] Reloadable entity-scoped material loot, scale squared, stochastic rounding, Looting and extra-roll composition.
- [x] Separate scale-aware Animal Weights space/crowding bridge.
- [x] Generic wolf mount extension with independent saddle/armor, direct controls and charged pounce.
- [x] Transient real-damage betrayal attribution without ownership/equipment changes.
- [x] Complete final modded/server/client validation and integration with concurrent work.
- [ ] Human two-player/latency acceptance of borrowed wolf combat and all requested gameplay cases.
