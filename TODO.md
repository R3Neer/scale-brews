# Scale Brews — active work

New requirements are appended here; they do not replace unfinished work.

- [x] Save and push the compiling tutorial prototype.
- [ ] Document commit/push policy and current behavior.
- [ ] Linear sprint bonuses: growth +20/+10/0%; shrinking +60/+90/+120%; walking unchanged.
- [ ] Beacon primary powers at pyramid tier 3, level II upgrade at tier 4.
- [ ] Physical scale transition over 20 ticks, including removal and power changes.
- [ ] Original programmatically drawn pixel art and English/Spanish translations.
- [ ] Validate runtime behavior, package, commit by concern and push.

## Decisions and follow-up

## Additional mechanics package (added; original work retained)

- [ ] Growth landing shockwave above 3 blocks: level-dependent bounded knockback; only III above 6 blocks deals radial damage, capped at 4 HP.
- [ ] Gust/ground particles and a heavy landing sound; no explosion or block damage.
- [ ] Shrinking fall damage x0.92/x0.84/x0.76, composed with vanilla protection.
- [ ] Shrinking movement-vibration detection radius: walking x0.75/x0.5/x0.25, sprinting x1/x0.75/x0.5; vanilla crouch and interactions preserved.
- [ ] Swift Sneak synergy only with Shrinking II/III, capped at walking speed.
- [ ] Pressure plates: Shrinking II ignores insensitive stone/iron plates; III ignores all; tags allow extensions.
- [ ] Shrinking III cannot trample farmland or turtle eggs.
- [ ] Growth resists soul sand and berry slowdown; preserves Soul Speed and berry damage; tagged, scoped integration.
- [ ] Regression tests for these mechanics and documentation of deviations.

The package's exclusions apply to its new mechanics; the previously approved beacon, sprint rebalance and scale blend remain scheduled.

- Special head scaling is cancelled; head proportions remain vanilla.
- Preserve the current attribute balance, including growth scale +0.96 per level.
- Levels III remain potion-only for beacons.
- Existing brewing remains in scope; extended redstone variants are future work.
- Detailed collision/mount/elytra and Combatify/Alex's Mobs integration QA remain follow-up work.
