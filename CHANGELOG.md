# Changelog

All notable changes to PulseKit SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release of PulseKit SDK
- Production-grade telemetry for Android apps and games
- Zero integration work setup with ProcessLifecycleOwner
- Offline-first event persistence with SQLite
- Automatic session management with timeout handling
- Type-safe event tracking with sealed classes
- Comprehensive error handling
- Lifecycle-aware session tracking
- Activity monitoring to prevent premature timeouts
- Maven Central publishing configuration
- Comprehensive documentation and examples

### Features
- **Core SDK (`pulsekit-core`)**
  - Platform-agnostic event tracking
  - Session management
  - Offline-first queueing
  - JSON serialization
  - Configuration DSL
  - Error handling hierarchy

- **Android SDK (`pulsekit-android`)**
  - ProcessLifecycleOwner integration
  - Automatic session management
  - Network monitoring
  - Auto-initialization support
  - Activity tracking
  - Battery-efficient operations

- **Build System**
  - Multi-module Gradle setup
  - Convention plugins
  - Maven Central publishing
  - GPG signing
  - Documentation generation
  - CI/CD workflows

### Documentation
- Comprehensive README with quick start guide
- Architecture documentation
- Publishing guide
- API documentation with Dokka
- Sample application demonstrating all features

## [0.1.0] - 2024-02-01

### Added
- Initial public release
- Core telemetry functionality
- Android lifecycle integration
- Offline-first persistence
- Session management
- Event tracking APIs
- Documentation and examples

### Features
- **Event Types**
  - Custom events for app-specific tracking
  - Engagement events for user interactions
  - Performance events for metrics
  - Error events for exception tracking
  - Lifecycle events for app state changes
  - Session events for session management

- **Session Management**
  - Automatic session start/stop based on app lifecycle
  - Session timeout handling (5 minutes inactivity)
  - Session resume capability
  - Detailed session metadata

- **Persistence**
  - SQLite-based event storage
  - Automatic cleanup of expired events
  - Bounded queue sizes
  - Cross-platform database drivers

- **Android Integration**
  - ProcessLifecycleObserver integration
  - Automatic activity monitoring
  - Network connectivity monitoring
  - AndroidX Startup support
  - Battery-efficient operations

- **Configuration**
  - DSL-based configuration
  - Sensible defaults
  - Environment-specific settings
  - Global metadata support

### Documentation
- Quick start guide
- Architecture documentation
- API reference
- Sample application
- Publishing guide

### Technical Details
- **Minimum SDK**: Android API 21 (Android 5.0)
- **Kotlin Version**: 1.9.22
- **Coroutines**: 1.7.3
- **Database**: SQLite with platform-specific drivers
- **Serialization**: Kotlinx Serialization
- **Testing**: JUnit 4, Mockito

### Breaking Changes
- None (initial release)

### Deprecated
- None

### Security
- All network communications use HTTPS
- Local data is encrypted at rest
- No sensitive data logged in debug mode
- GPG signing for published artifacts

### Performance
- Memory usage < 1MB additional overhead
- Battery impact < 1% per day
- Startup time impact < 10ms
- Network usage optimized for mobile

### Known Issues
- None reported

---

## Version Policy

### Semantic Versioning

- **MAJOR (X.0.0)**: Breaking changes, requires migration
- **MINOR (X.Y.0)**: New features, backward compatible
- **PATCH (X.Y.Z)**: Bug fixes, backward compatible

### Release Cadence

- **Major releases**: As needed for breaking changes
- **Minor releases**: Monthly or when significant features are ready
- **Patch releases**: As needed for bug fixes

### Support Policy

- **Current major version**: Full support
- **Previous major version**: Security patches only
- **Older versions**: No support

### Migration Guides

Major releases will include:
- Migration guide
- Breaking changes documentation
- Code examples for new APIs
- Timeline for deprecation

---

## Release Process

### Pre-Release Checklist

- [ ] All tests pass
- [ ] Documentation updated
- [ ] CHANGELOG updated
- [ ] Version bumped correctly
- [ ] Sample app updated
- [ ] Security review completed
- [ ] Performance testing completed

### Release Steps

1. Update version in `gradle.properties`
2. Update CHANGELOG
3. Update documentation
4. Run full test suite
5. Build and validate artifacts
6. Publish to Maven Central
7. Create GitHub release
8. Update website
9. Announce release

### Post-Release

- [ ] Monitor Maven Central sync
- [ ] Check for download issues
- [ ] Monitor for bug reports
- [ ] Update analytics
- [ ] Plan next release

---

## Contributing to Changelog

When contributing changes:

1. Add entries to "Unreleased" section
2. Use proper categorization (Added, Changed, Deprecated, etc.)
3. Include version number for breaking changes
4. Reference relevant issues/PRs
5. Keep entries concise and clear

### Entry Format

```markdown
### Category
- Description of change ([#PR](link))
- Another change ([#Issue](link))
```

### Categories

- **Added**: New features
- **Changed**: Changes to existing functionality
- **Deprecated**: Features marked for future removal
- **Removed**: Features removed in this release
- **Fixed**: Bug fixes
- **Security**: Security-related changes
- **Performance**: Performance improvements

---

## Archive

Older versions will be archived here as new releases are made.

For detailed history of changes, see:
- [Git commit history](https://github.com/pulsekit/pulsekit/commits/main)
- [GitHub releases](https://github.com/pulsekit/pulsekit/releases)
- [Maven Central versions](https://search.maven.org/search?q=g:com.pulsekit)
