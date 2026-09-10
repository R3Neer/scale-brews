# Working on Scale Brews

- Read TODO.md and CONTRIBUTING.md before changes. New requirements supplement unfinished work unless explicitly cancelled.
- The owner has authorized autonomous implementation, scoped commits and pushes to origin/main at validated milestones. Do not force-push, rewrite history, create releases or tags without a separate request.
- Verify version-sensitive Minecraft/Fabric APIs against this project's 26.2 sources and dependencies.
- Keep registries, brewing, scale state and physical mechanics separate. Mixins should delegate rules to helpers.
- Keep existing attribute balance unless rebalancing is explicitly in scope. Special head scaling is cancelled.
- Run the build/server GameTests and the client GameTest when changing shared/client mixins or resources. Preserve unrelated worktree changes.
- The checkout deliberately lives in OneDrive. Use the optional scalebrewsBuildDir property for local generated output if necessary; do not move the source repository or delete broad cache directories.
- Art is original geometric pixel art from tools/GenerateArt.java, not image-generation models. Keep its generator and PNGs synchronized.
