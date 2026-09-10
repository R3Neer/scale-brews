# Development and Git policy

- Keep changes grouped into reviewable commits by concern (`feat`, `fix`, `docs`, `test`, `art`, `chore`).
- Preserve unrelated changes; inspect the diff and stage explicit paths.
- Before committing code, run `./gradlew build`; exercise mixins in a Minecraft runtime. Record limitations honestly.
- Push validated commits to `origin/main` at completed milestones while working under the owner's standing push authorization. Never force-push or rewrite published history.
- No automatic releases or version tags: publishing a release is a separate decision.
- For OneDrive checkouts, create an ignored `gradle-local.properties` with `scalebrewsBuildDir=C:/your/local/build-output/scale-brews`. This redirects generated build output for IntelliJ and CLI without relocating source or `run/` worlds. Use a distinct output directory per checkout. `-PscalebrewsBuildDir=...` overrides this setting; without either, Gradle uses `build/`.
- Append incoming requirements to TODO.md, retaining unfinished requirements and documenting cancelled ones.
- Commit original art and its reproducible generator. Do not commit caches, generated game sources, worlds or secrets.
