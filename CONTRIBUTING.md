# Development and Git policy

- Keep changes grouped into reviewable commits by concern (`feat`, `fix`, `docs`, `test`, `art`, `chore`).
- Preserve unrelated changes; inspect the diff and stage explicit paths.
- Before committing code, run `./gradlew build`; exercise mixins in a Minecraft runtime. Record limitations honestly in `docs/VALIDATION.md` only after the corresponding run actually happened.
- The repository owner may authorize autonomous implementation and pushes. **Task-specific branch restrictions override any standing/default push policy.** Never force-push or rewrite published history.
- No automatic releases or version tags: publishing a release is a separate decision.
- For OneDrive checkouts, create an ignored `gradle-local.properties` with `scalebrewsBuildDir=C:/your/local/build-output/scale-brews`. This redirects generated build output for IntelliJ and CLI without relocating source or `run/` worlds. Use a distinct output directory per checkout. `-PscalebrewsBuildDir=...` overrides this setting; without either, Gradle uses `build/`.
- `TODO.md` is an index of currently open work, not a requirements archive. For the entity-collision subsystem, normative requirements live in `docs/ENTITY_COLLISIONS_REQUIREMENTS.md`, architecture/API/data ownership in `docs/ENTITY_COLLISIONS.md`, implementation order/status in `docs/ENTITY_COLLISIONS_PLAN.md`, and executed evidence in `docs/VALIDATION.md`. Other subsystems should likewise avoid duplicating normative truth across files.
- When requirements change, update their canonical specification before changing the plan or implementation. Preserve historical decisions through Git/changelog rather than keeping superseded plans in the active tree.
- Commit original art and its reproducible generator. Do not commit caches, generated game sources, worlds or secrets.
