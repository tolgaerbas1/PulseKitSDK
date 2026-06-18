# Medium Article Series Plan

Working title: **Building PulseKit: A Production-Grade Android Telemetry SDK**

This series is designed to present PulseKit as a senior/staff-level showcase project. The narrative should make the engineering thinking visible: what problem was being solved, which constraints shaped the architecture, where complexity was deliberately accepted, and where scope was deliberately reduced.

## Series Positioning

PulseKit should be presented as more than a telemetry SDK. It is a compact case study in SDK design for host-app safety, offline-first reliability, Android platform integration, Kotlin Multiplatform boundaries, and production engineering discipline.

The article series should avoid reading like a feature tour. Each part should instead answer one senior-level question:

1. How do you design a public SDK API and architecture that is simple for consumers but robust internally?
2. How do you make telemetry reliable under mobile constraints such as process death, offline usage, memory pressure, and flaky networks?
3. How do you turn a working SDK into a production-ready library with quality gates, compatibility guarantees, docs, and release discipline?

## Part 1: Designing the SDK Surface and Architecture

Proposed title: **From API First to Architecture: Designing a Production-Grade Android SDK**

### Thesis

The first senior-level decision in an SDK is not the transport layer or the database. It is the consumer contract. A library that runs inside someone else's app must be easy to adopt, hard to misuse, and disciplined about what it exposes.

### Core Topics

- Product framing: telemetry for Android apps and games.
- SDK constraints: host-app safety, minimal setup, predictable behavior, no hidden heavy dependencies.
- Public API design:
  - `PulseKitAndroid` as the Android entry point.
  - `PulseKitInstance` as the core runtime boundary.
  - `PulseKitConfig` DSL for readable setup and production defaults.
  - Type-safe event hierarchy instead of loosely typed event blobs.
- Module structure:
  - `pulsekit-core` for Kotlin Multiplatform domain logic.
  - `pulsekit-android` for lifecycle, connectivity, storage, startup, and platform wiring.
  - `sample-app` as executable documentation.
  - build conventions and Gradle quality gates.
- Architecture boundaries:
  - Core owns event processing, sessions, flags, queueing, backpressure, errors, and abstractions.
  - Android owns platform services and adapters.
  - Public API remains smaller than the internal implementation surface.

### Decisions and Trade-Offs to Explain

- Why KMP was used for core logic even though the first target is Android.
- Why the Android module wraps core instead of leaking Android APIs into core.
- Why the configuration DSL uses production defaults instead of requiring exhaustive setup.
- Why events are modeled as sealed types while metadata remains flexible.
- Why AndroidX Startup is optional convenience, not the only initialization path.
- Why the SDK favors explicit host-app safety over aggressive background behavior.

### Suggested Diagrams

- Module dependency graph.
- Initialization flow from `Application.onCreate()` to configured runtime.
- Public API surface vs internal components.
- Event model hierarchy.

### Code Walkthrough Anchors

- `PulseKitAndroid.initialize(...)`
- `PulseKitConfig { ... }`
- `PulseKitInstance`
- `PulseEvent`
- `PulseKitError`

### Seniority Signals

- Consumer-first API design.
- Clear ownership boundaries.
- Minimal public API with extensible internals.
- Platform-specific integration isolated behind adapters.
- Explicit trade-offs instead of accidental complexity.

## Part 2: Reliability Under Mobile Failure Modes

Proposed title: **Offline-First Telemetry: Queues, Persistence, Backpressure, and Failure Handling**

### Thesis

Telemetry SDKs fail in the real world when they assume stable networks, infinite memory, and long-lived processes. PulseKit treats failure as the default environment and builds reliability around bounded resources.

### Core Topics

- End-to-end event pipeline:
  - track
  - validate
  - enrich
  - enqueue
  - persist
  - flush
  - retry or drop deterministically
- Offline-first design:
  - memory-first queue for speed.
  - disk-backed queue for process death and offline continuity.
  - SQLite through `DatabaseDriver` instead of Android-specific persistence in core.
- Backpressure:
  - bounded queue size.
  - priority-aware dropping.
  - protection of error, lifecycle, and critical events.
  - simplified metrics for debuggability.
- Networking:
  - `EventBatchSender` and `NetworkClient` abstractions.
  - Android network monitoring.
  - reconnect-triggered flush.
- Error model:
  - typed errors for predictable failure handling.
  - SDK should protect the host app from internal failures.
- Concurrency:
  - coroutine-based async work.
  - clear lifecycle for initialize, flush, shutdown, and dispose paths.

### Decisions and Trade-Offs to Explain

- Why the queue is memory-first but disk-backed instead of every operation being database-first.
- Why persistence is best-effort and bounded rather than an unbounded durable log.
- Why deterministic dropping is preferable to out-of-memory risk.
- Why critical events deserve priority treatment.
- Why retry policy must be conservative inside a mobile SDK.
- Why the database boundary is an interface and not a direct SQLite dependency in common code.

### Suggested Diagrams

- Event processing sequence diagram.
- Queue state machine: accepted, persisted, sending, failed, retried, processed, dropped.
- Backpressure decision tree.
- Offline-to-online flush flow.

### Code Walkthrough Anchors

- `EventQueue`
- `DatabaseDriver`
- `EventSerializer`
- `SimplifiedBackpressureManager`
- `SimplifiedPriorityCalculator`
- `AndroidEventBatchSender`
- `NetworkMonitor`

### Seniority Signals

- Reliability is designed around real mobile constraints.
- Resource limits are explicit.
- Failure paths are first-class.
- Storage and networking are abstracted without overengineering.
- The implementation makes product promises testable.

## Part 3: Production Readiness, Feature Flags, Quality Gates, and Release Discipline

Proposed title: **From Working SDK to Production Library: Flags, Quality Gates, Publishing, and Documentation**

### Thesis

A showcase project becomes senior-level when it demonstrates operational maturity: compatibility, verification, documentation, release hygiene, and clear evolution paths. Production readiness is a system, not a final cleanup pass.

### Core Topics

- Feature flag architecture:
  - local defaults.
  - server-driven refresh.
  - disk persistence for offline reads.
  - runtime manager as the single decision point.
  - Android storage and network adapters wired into core.
- Build and quality system:
  - Gradle multi-module structure.
  - Kotlin and AGP version alignment.
  - Spotless formatting.
  - Detekt static analysis.
  - Android lint.
  - unit tests and instrumentation boundaries.
  - API compatibility and publishing checks.
- Documentation system:
  - quickstart.
  - architecture guide.
  - feature flag guide.
  - API key/backend guide.
  - trade-off docs.
  - staff-level review docs.
- Publishing posture:
  - Maven Central readiness.
  - semantic versioning.
  - API compatibility expectations.
  - changelog and release checklist.
- Remaining roadmap:
  - richer retry/backoff controls.
  - CI matrix hardening.
  - richer integration tests around persistence and network recovery.
  - binary compatibility enforcement as the public API stabilizes.

### Decisions and Trade-Offs to Explain

- Why feature flags are integrated into the SDK runtime instead of being a separate sample-only concept.
- Why docs are part of production readiness, not an afterthought.
- Why formatting and static analysis are useful only when noise is controlled.
- Why the project accepts some warnings temporarily, documents them, and tracks them intentionally.
- Why release automation matters even before the first public release.

### Suggested Diagrams

- Feature flag refresh and persistence flow.
- Quality gate pipeline from local development to release.
- Release readiness checklist.
- Documentation map linking architecture decisions to code modules.

### Code Walkthrough Anchors

- `FeatureFlagManager`
- `FeatureFlagService`
- `FlagPersistence`
- `AndroidFlagStorage`
- Gradle convention files.
- Detekt, Spotless, lint, and publishing configuration.

### Seniority Signals

- Production readiness is treated as architecture.
- Build tooling enforces standards instead of relying on memory.
- Documentation captures intent and trade-offs.
- The roadmap separates must-have production behavior from deliberate future investment.
- The project shows ownership across code, tests, docs, release, and maintainability.

## Recommended Publishing Flow

1. Publish Part 1 with an architecture diagram and a concise sample integration.
2. Publish Part 2 with failure-mode examples: offline app restart, full queue, reconnect flush, and critical event preservation.
3. Publish Part 3 with the quality gate checklist and the story of turning review findings into production readiness work.

Each article should end with a short "What I would improve next" section. This keeps the tone senior: confident about the current design, honest about trade-offs, and explicit about evolution.
