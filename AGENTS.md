# Working on Scale Brews

- Read `TODO.md` and `CONTRIBUTING.md` before changes. `TODO.md` is an active-work index, not a requirements archive.
- For entity collisions, read `docs/ENTITY_COLLISIONS_REQUIREMENTS.md`, `docs/ENTITY_COLLISIONS.md`, `docs/ENTITY_COLLISIONS_PLAN.md` and `docs/VALIDATION.md` in that order. Do not recreate parallel anatomy/platform plans or status diaries.
- New requirements supplement unfinished work unless explicitly cancelled, but they belong in the canonical requirement source for their subsystem before plan/code changes.
- The owner may authorize autonomous implementation and scoped pushes. **The branch named by the current task is authoritative and overrides standing/default push guidance.** Do not force-push, rewrite history, create releases or tags without a separate request.
- Verify version-sensitive Minecraft/Fabric APIs against this project's 26.2 sources and dependencies.
- Keep registries, brewing, scale state and physical mechanics separate. Mixins should delegate rules to helpers rather than owning alternate implementations.
- Keep existing attribute balance unless rebalancing is explicitly in scope. Special head scaling is cancelled.
- Run the build/server GameTests and the client GameTest when changing shared/client mixins or resources. Record only executed results in `docs/VALIDATION.md`.
- The checkout deliberately lives in OneDrive. Use the optional `scalebrewsBuildDir` property for local generated output if necessary; do not move the source repository or delete broad cache directories.
- Art is original geometric pixel art from `tools/GenerateArt.java`, not image-generation models. Keep its generator and PNGs synchronized.
