# Contributing to PulseKit

Thanks for your interest in contributing. This guide covers local setup, code style, and the PR process.

## Prerequisites

- **JDK 17**
- **Android SDK** (for Android modules)
- **Gradle 8.x** (wrapper included)

## Failed terminal commands (do not retry)

Record commands that failed in this environment so automation/prompts don’t waste time retrying:

- **PowerShell:** `&&` is not a valid statement separator. Use `;` instead (e.g. `cd path; .\gradlew task`).
- **Gradle:** If `JAVA_HOME is not set` or `java not found`, set JDK 17 and ensure it’s on PATH before running `gradlew`.

## Build & test

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Build sample app
./gradlew :sample-app:assembleDebug
```

## Code style & lint

We use **Spotless** (ktlint) for formatting and **detekt** for static analysis.

### Format (required before commit)

```bash
# Format all Kotlin and Gradle Kotlin DSL
./gradlew spotlessApply
```

### Lint

```bash
# Run detekt; report in build/reports/detekt/
./gradlew detekt
```

- **Config:** `config/detekt.yml` (complexity, naming, style, potential-bugs).
- Fix reported issues before opening a PR, or document why something is suppressed.

### CI

On push/PR we run:

- `spotlessCheck` — fails if code is not formatted.
- `detekt` — fails if max issues threshold is exceeded.
- `checkApiCompatibility` / `checkBinaryCompatibility` — see [docs/ApiCompatibility.md](docs/ApiCompatibility.md).

## API compatibility

- Public API lives under `com.pulsekit.core.api` and `com.pulsekit.android`.
- Before merging changes that touch public API, run:
  ```bash
  ./gradlew checkApiCompatibility checkBinaryCompatibility
  ```
- For intentional breaking changes, after code changes run: `./gradlew updateApiBaselines`. Then document in CHANGELOG and bump major version. See [docs/ApiCompatibility.md](docs/ApiCompatibility.md).

## Pull requests

- Use a descriptive branch name (e.g. `feature/event-batching` or `fix/queue-full`).
1. Create a branch from `main` (or `develop` if used).
2. Make your changes; run `./gradlew spotlessApply` and fix `detekt` findings.
3. Ensure `./gradlew build` and `./gradlew test` pass.
4. Open a PR; CI will run format, detekt, API compatibility, and build.

## Documentation

- Update [docs/Architecture.md](docs/Architecture.md) and relevant `Module.md` when changing module behavior or public API.
- Document breaking changes in [CHANGELOG.md](CHANGELOG.md).

## Questions

- [GitHub Discussions](https://github.com/pulsekit/pulsekit/discussions)
- [Issues](https://github.com/pulsekit/pulsekit/issues)
