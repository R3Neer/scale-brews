from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected text not found in {path}: {old[:160]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "docs/GUIDE.md",
    "The three default Tiny Mounts deliberately use different interaction families. A **Chicken** (`direct`) and a **tamed Wolf** (`tameable_direct`) can carry a passive rider without a saddle, but need a vanilla saddle for manual control. A **Bee** (`item_steered`) follows Pig/Strider-style rules: it must be saddled even to mount and then needs a Flower on a Stick for manual control. Equipping a saddle does not require being small; riding still requires the appropriate size ratio. Wild wolves are the deliberate exception to the taming gate because riding is also their alternate taming route described below.",
    "The three default Tiny Mounts deliberately use different interaction families. A **Chicken** (`direct`) can carry a passive rider without a saddle and needs the vanilla saddle only for manual control. A **Wolf** (`tameable_direct`) can also carry a passive rider before it is tamed, which allows the riding-based taming route; equipment is unavailable until the animal is tamed, and after taming the wolf can still be ridden unsaddled while the saddle is required only for manual control. A **Bee** (`item_steered`) follows Pig/Strider-style rules: it must be saddled even to mount and then needs a Flower on a Stick for manual control. Equipping a saddle never requires the rider to be small, but tameable mounts must first be tamed; riding still requires the appropriate size ratio."
)

replace_once(
    "docs/CONFIGURATION.md",
    "- `tameable_direct`: the entity must be a `TamableAnimal`. It may carry a passive passenger before control is available, while taming/ownership remains native or entity-specific. Taming plus a vanilla saddle are required for manual control and equipment access. The mount has the generic equipment menu. Wolf uses this family; horse/Nautilus-style saddle-and-menu behavior is the model, while Scale Brews keeps the wolf's own alternative taming and ownership rules.",
    "- `tameable_direct`: the entity must be a `TamableAnimal`. It may carry a passive passenger before taming or saddling, allowing horse-style or entity-specific taming routes. Taming is required before Tiny Mount equipment becomes available; once tamed, the generic equipment menu is available even while unsaddled, so the saddle can be installed or removed there. The vanilla saddle is required for **manual control**, not for opening that menu. Ownership restrictions remain entity-specific; Wolf uses this family and keeps its riding-based taming route plus owner-protected BODY equipment."
)

replace_once(
    "docs/WOLF_MOUNT.md",
    "Tiny Mount interaction/equipment grammar is now selected by `family` in the synced definition. `direct` mounts such as Chicken can carry a passenger without a saddle, require a saddle for control, and expose a saddle-only mount inventory; Crouch + Use from outside opens that equipment menu, and E opens the same menu while riding. `tameable_direct` mounts such as Wolf add the taming requirement and may declare native BODY equipment; Wolf declares Wolf Armor there. For this family Crouch + Use remains the mounting gesture so native normal-Use behavior is preserved, while E opens the equipment menu once mounted. `item_steered` mounts such as Bee follow the Pig/Strider grammar instead: they require a saddle even to mount, have no mount inventory, and E while riding opens the normal player inventory. The menu, when present, is only a view over the entity's native SADDLE/BODY equipment slots, so rendering, drops, commands and riding all observe one source of truth. Wolf BODY equipment remains owner-protected even when another compatible player is borrowing the wolf.",
    "Tiny Mount interaction/equipment grammar is now selected by `family` in the synced definition. `direct` mounts such as Chicken can carry a passenger without a saddle, require a saddle for control, and expose a saddle-only mount inventory; Crouch + Use from outside opens that equipment menu, and E opens the same menu while riding. `tameable_direct` mounts such as Wolf may also carry a passive passenger before taming, but Tiny Mount equipment remains unavailable until the animal is tamed and manual control additionally requires the saddle. Once tamed, the equipment menu remains available while unsaddled, which is how the saddle itself can be managed; Wolf also declares native BODY Wolf Armor there. For this family Crouch + Use remains the mounting gesture so native normal-Use behavior is preserved, while E opens the equipment menu once mounted. `item_steered` mounts such as Bee follow the Pig/Strider grammar instead: they require a saddle even to mount, have no mount inventory, and E while riding opens the normal player inventory. The menu, when present, is only a view over the entity's native SADDLE/BODY equipment slots, so rendering, drops, commands and riding all observe one source of truth. Wolf BODY equipment remains owner-protected even when another compatible player is borrowing the wolf."
)

Path(__file__).unlink()
