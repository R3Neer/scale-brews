# Alchemical Leather compatibility

Scale Brews owns the Alchemical Leather compatibility declarations for its own potion effects.

- Growth is a chestplate infusion.
- Shrinking is a chestplate infusion.
- Both effects explicitly use `wear: none` because maintaining a size state is persistent body state, not causal work that should consume armor durability over time.
- The integration is data-only. Scale Brews has no hard runtime dependency on Alchemical Leather and does not select armor, arbitrate effect ownership or mutate durability.

The complete implementation and TM validation record is in [`TM_ALCHEMICAL_LEATHER_COMPAT.md`](TM_ALCHEMICAL_LEATHER_COMPAT.md).
