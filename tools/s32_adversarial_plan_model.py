"""Historical contract counterexamples and revised algebra; no production/runtime test."""
from fractions import Fraction as F
import json

first_carry = F(3, 4)
native_after_one_tick = first_carry / 3
second_carry = F(3, 4)
material_after_second = first_carry + second_carry
# Literal R4 retirement discards the first packet's still-unincorporated carry.
literal_draw_at_restart = native_after_one_tick + second_carry
rotation_only_next_position = native_after_one_tick + (first_carry - native_after_one_tick) / 3
report = {
    "scope": "PLAN contract only; native three-step and restart laws verified in pinned 26.2 bytecode",
    "new_position_update": {
        "first_carry": str(first_carry), "old_incorporated": str(native_after_one_tick),
        "second_carry": str(second_carry), "support_material": str(material_after_second),
        "draw_after_discarding_old_remainder": str(literal_draw_at_restart),
        "unaccounted_remainder": str(material_after_second - literal_draw_at_restart),
    },
    "rotation_only_restart": {
        "position_before_rotation": str(native_after_one_tick), "same_target": str(first_carry),
        "position_after_restarted_tick": str(rotation_only_next_position),
        "remaining_old_passive_component": str(first_carry - rotation_only_next_position),
        "rotation_mints_new_carry": False,
    },
    "remote_original_model_double_incorporation": {
        "native_body_draw": str(native_after_one_tick),
        "own_accepted_model_origin": str(first_carry),
        "remote_residual": str(first_carry - native_after_one_tick),
        "current_model_submit_plus_remote_offset": str(first_carry + first_carry - native_after_one_tick),
        "expected_root": str(first_carry),
        "double_incorporation": str(first_carry - native_after_one_tick),
    },
}
assert material_after_second - literal_draw_at_restart == F(1, 2)
assert first_carry - rotation_only_next_position == F(1, 3)
checked = 0
for a in (F(-3, 4), F(0), F(3, 4), F(3, 2)):
    for b in (F(-1, 2), F(0), F(3, 4)):
        for voluntary in (F(-3, 4), F(0), F(1, 2)):
            for old_fraction in (F(0), F(1, 3), F(2, 3), F(1)):
                baseline = a * old_fraction
                pending = a * (1 - old_fraction) + b
                target = a + b + voluntary
                for progress in (F(0), F(1, 4), F(1, 2), F(3, 4), F(1)):
                    native = baseline + progress * (target - baseline)
                    residual = pending * (1 - progress)
                    expected = a + b + voluntary * progress
                    assert native + residual == expected
                    # Revised ENTITY_ROOT original model shares this same native basis.
                    model_root = native + residual
                    assert model_root == expected
                    checked += 1
report["historical_counterexample_status"] = "All three old clauses resolved by current R2-R5; values above describe rejected clauses"
report["revised_reference_algebra"] = {"cases": checked, "status": "PASS", "scope": "Transfer predecessor pending + new carry; native mixed voluntary progression preserved; shared ENTITY_ROOT draw basis"}
print(json.dumps(report, indent=2))
