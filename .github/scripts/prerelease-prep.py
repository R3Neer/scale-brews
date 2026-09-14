from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected text not found in {path}: {old[:160]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "docs/MECHANICS.md",
    "Potion levels only determine the scale target. All derived attributes and physical rules read the current `Attributes.SCALE`, including the blend and external modifiers. For scale `s >= 1`, Growth-equivalent strength is `clamp((s - 1) / 0.96, 0, 3)`; below 1, Shrinking-equivalent strength is `clamp((1 - s) / 0.242, 0, 3)`. Only one family can apply at once. Values beyond the level-III anchors keep the bounded level-III balance.\n\nContinuous properties interpolate between normal and the existing I/II/III anchors: movement, sprint, health, damage, reach, resistance, jump, terrain relief, exhaustion, melee knockback, fall protection, vibration radius, Swift Sneak synergy and creeper power. Discrete abilities use reached size thresholds: insensitive plates at <= 0.516; all plates and fragile-block protection at <= 0.274. Landing knockback requires >= 1.96 and damage >= 3.88, independently of active potion names. Mounting and platform eligibility retain their actual-dimension policies.",
    "Potion levels only determine the scale target. All derived attributes and physical rules read the current `Attributes.SCALE`, including the blend and external modifiers. For scale `s >= 1`, most Growth mechanics use equivalent strength `clamp((s - 1) / 0.96, 0, 3)`; below 1, Shrinking-equivalent strength is `clamp((1 - s) / 0.242, 0, 3)`. Only one family can apply at once. The deliberate exception is **Growth entity interaction reach**, which uses the uncapped positive equivalent `(s - 1) / 0.96` so its linear curve can continue beyond Growth III for externally enlarged entities.\n\nMost continuous properties interpolate between normal and the existing I/II/III anchors: movement, sprint, health, damage, resistance, jump, terrain relief, exhaustion, melee knockback, fall protection, vibration radius, Swift Sneak synergy and creeper power. Reach has its own final contract: Growth block reach is +20% per equivalent level and caps at III (**5.4 / 6.3 / 7.2** blocks from the 4.5 survival baseline); Growth entity reach is +30% per equivalent level (**3.9 / 4.8 / 5.7** from the 3.0 baseline) and continues the same linear curve beyond III; Shrinking block and entity reach both decrease by 10% per equivalent level and cap at III (**4.05 / 3.60 / 3.15** block, **2.70 / 2.40 / 2.10** entity). See [REACH_BALANCE.md](REACH_BALANCE.md) for the canonical reach contract. Discrete abilities use reached size thresholds: insensitive plates at <= 0.516; all plates and fragile-block protection at <= 0.274. Landing knockback requires >= 1.96 and damage >= 3.88, independently of active potion names. Mounting and platform eligibility retain their actual-dimension policies."
)

replace_once(
    "docs/MECHANICS.md",
    "## Mounted-bee rendering\n\n`BeeRiderPose` samples the actual adult bee model at the same interpolated time as its rider. It computes the transform from the resting body frame to the animated `bone` frame, including mount scale, yaw and renderer rotations, then expresses it relative to the passenger. `AnimatedRiderRendererMixin` applies that immutable snapshot around the complete living-entity render submission, so rider, armor and held items follow the saddle's translation and tilt. It does not duplicate vanilla bobbing formulas or freeze the bee. Selecting the adult model explicitly avoids reusing a renderer's last submitted baby model. Snapshots clear on dismount; entity positions, collision boxes, camera and first-person hands remain untouched. The saddle layer still copies the animated body pose for deferred rendering.",
    "## Animated mount attachment rendering\n\nMounted saddles and riders share one **final rendered attachment transform** instead of reconstructing a species-specific animation. `MountPoseCaptureLayer` runs in the living mount's render-layer phase, after vanilla `setupAnim` and optional model animation have already updated the model. It resolves the configured `body` / `bone` anchor from `MountPoseState` and stores the animated attachment delta through `MountRiderPose`. `TinySaddleLayer` consumes that same final frame for saddle submission, while `AnimatedRiderRendererMixin` applies the cached rider transform around the complete passenger render so body, armor and held items move together.\n\nThe cached delta is reduced to a rigid translation + rotation transform before passenger-relative application, preventing model scale/shear from contaminating the rider. There is no second `setupAnim` call and no bee-only pose recreation. The same path is exercised for Chicken, Bee, Wolf and native Horse animation; the separate optional-mod proof also passed with EMF 3.3.5, ETF 7.2 and Fresh Animations 1.10.5, where EMF applies its animation after `setupAnim` and before render layers are iterated. Entity positions, collision boxes, first-person camera and physical movement remain untouched by this rendering path."
)

replace_once(
    "TODO.md",
    "- [x] Audit the live GUIDE/CONFIGURATION/VALIDATION/TODO/example datapack after beta.7, remove stale reach and Tiny Mount wording, and label older validation entries as historical rather than current contracts.",
    "- [x] Audit the live GUIDE/CONFIGURATION/MECHANICS/VALIDATION/TODO/example datapack after beta.7, remove stale reach, Tiny Mount and removed-renderer wording, and label older validation entries as historical rather than current contracts."
)

Path(__file__).unlink()
