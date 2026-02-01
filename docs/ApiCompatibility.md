# API Compatibility Guide

This guide explains how PulseKit maintains backward compatibility through automated API surface snapshot testing and binary compatibility validation.

## Overview

PulseKit uses a comprehensive API compatibility system to ensure that changes don't accidentally break existing users. This system includes:

- **API Surface Snapshots**: Baseline API signatures for comparison
- **Binary Compatibility**: JVM bytecode compatibility checking with japicmp
- **Automated Validation**: CI/CD integration with fail-fast behavior
- **Developer-Friendly Workflows**: Clear processes for intentional breaking changes

## Architecture

### API Surface Snapshots

The system generates and compares API signatures:

```
api/
├── baseline/              # Stable release baselines
│   ├── pulsekit-core-0.1.0.jar
│   ├── pulsekit-core-0.1.0-signature.txt
│   └── pulsekit-android-0.1.0.jar
└── current/               # Current build snapshots
    ├── pulsekit-core.jar
    └── pulsekit-core-signature.txt
```

### Compatibility Layers

1. **Source Compatibility**: API signature comparison
2. **Binary Compatibility**: JVM bytecode compatibility
3. **Runtime Compatibility**: Behavioral compatibility

## Usage

### Daily Development

During normal development, compatibility checks run automatically:

```bash
# Build and check compatibility
./gradlew build checkApiCompatibility checkBinaryCompatibility
```

### Intentional Breaking Changes

When you need to make breaking changes:

1. **Make the change** in your feature branch
2. **Update baseline** when ready:
   ```bash
   ./gradlew updateApiBaselines
   ```
3. **Document changes** in CHANGELOG.md
4. **Update version** (major version bump)
5. **Release** with new baseline

### Force Compatibility Checks

For emergency releases or testing:

```bash
# Force API compatibility check to pass
./gradlew checkApiCompatibility -PFORCE_API_COMPATIBILITY=true

# Force binary compatibility check to pass
./gradlew checkBinaryCompatibility -PFORCE_BINARY_COMPATIBILITY=true
```

## Available Tasks

### API Surface Tasks

```bash
# Generate API snapshot from current build
./gradlew generateApiSnapshot

# Update API baseline (for new releases)
./gradlew updateApiBaseline

# Check API compatibility against baseline
./gradlew checkApiCompatibility

# Check all modules
./gradlew checkApiCompatibility
```

### Binary Compatibility Tasks

```bash
# Download baseline artifacts
./gradlew downloadBaselineArtifacts -PBASELINE_VERSION=0.1.0

# Check binary compatibility
./gradlew japicmpJvm
./gradlew japicmpAndroidRelease
./gradlew japicmpRelease

# Check all modules
./gradlew checkBinaryCompatibility
```

### Combined Tasks

```bash
# Comprehensive compatibility check
./gradlew checkApiCompatibility checkBinaryCompatibility

# Update all baselines
./gradlew updateApiBaselines

# Ready for publishing (includes compatibility checks)
./gradlew checkAllModulesReady
```

## Configuration

### Baseline Version

Set the baseline version for comparison:

```bash
# Global baseline
./gradlew checkBinaryCompatibility -PBASELINE_VERSION=0.1.0

# Module-specific baseline
./gradlew :pulsekit-core:checkBinaryCompatibility -PBASELINE_VERSION_PULSEKIT_CORE=0.1.0
```

### Allowed Breaking Changes

For intentional breaking changes:

```bash
./gradlew checkBinaryCompatibility -PALLOWED_BREAKING_CHANGES="method.added,field.added"
```

### Force Compatibility

Override compatibility checks:

```bash
# Force API compatibility
./gradlew checkApiCompatibility -PFORCE_API_COMPATIBILITY=true

# Force binary compatibility
./gradlew checkBinaryCompatibility -PFORCE_BINARY_COMPATIBILITY=true
```

## CI/CD Integration

### GitHub Actions

The `.github/workflows/api-compatibility.yml` workflow:

- **Triggers**: Push to main/develop, pull requests
- **Steps**: Build → Generate snapshots → Check compatibility
- **Artifacts**: Uploads compatibility reports
- **PR Comments**: Adds compatibility status to pull requests

### Pull Request Process

1. **Create feature branch**
2. **Make changes**
3. **Compatibility checks run automatically**
4. **If breaking changes detected**:
   - Review the changes
   - Update baseline if intentional
   - Document in CHANGELOG
   - Bump major version

### Release Process

1. **Update version**: `./gradlew versionMajor`
2. **Update baselines**: `./gradlew updateApiBaselines`
3. **Run full checks**: `./gradlew checkAllModulesReady`
4. **Publish**: `./gradlew publishToSonatype`

## Breaking Change Categories

### Source-Level Breaking Changes

- **Removed Classes**: Classes removed from public API
- **Removed Methods**: Public methods removed
- **Changed Method Signatures**: Parameter types or return types changed
- **Changed Visibility**: Public members made private/protected
- **Changed Inheritance**: Class hierarchy changes

### Binary-Level Breaking Changes

- **Method Signature Changes**: Even if source compatible
- **Field Type Changes**: Field type modifications
- **Class Structure Changes**: Internal structure changes
- **Annotation Changes**: Public annotation modifications

### Runtime Breaking Changes

- **Behavior Changes**: Method behavior changes
- **Exception Changes**: Different exceptions thrown
- **Thread Safety Changes**: Concurrency behavior changes
- **Performance Changes**: Significant performance regressions

## Compatibility Rules

### What's Allowed (Non-Breaking)

✅ **Adding new public classes**
✅ **Adding new public methods**
✅ **Adding new method overloads**
✅ **Adding new optional parameters**
✅ **Expanding return types (covariant)**
✅ **Adding new exceptions**
✅ **Improving documentation**
✅ **Internal implementation changes**

### What's Restricted (Breaking)

❌ **Removing public classes**
❌ **Removing public methods**
❌ **Changing method signatures**
❌ **Changing field types**
❌ **Making public members private**
❌ **Changing inheritance hierarchy**
❌ **Changing default behavior**

## Troubleshooting

### Common Issues

#### 1. "No API baseline found"

**Problem**: First time running compatibility checks

**Solution**:
```bash
./gradlew updateApiBaselines
```

#### 2. "API compatibility issues detected"

**Problem**: Unintentional breaking changes

**Solution**:
1. Review the reported changes
2. Fix the breaking changes
3. Re-run checks

#### 3. "Binary compatibility issues detected"

**Problem**: JVM bytecode incompatibility

**Solution**:
1. Check japicmp report for details
2. Review method signatures and types
3. Fix binary incompatibilities

#### 4. "Baseline version not found"

**Problem**: Baseline version doesn't exist in Maven Central

**Solution**:
```bash
# Use a published version
./gradlew checkBinaryCompatibility -PBASELINE_VERSION=0.1.0
```

### Debug Mode

Enable detailed logging:

```bash
./gradlew checkApiCompatibility --info
./gradlew checkBinaryCompatibility --debug
```

### Report Analysis

Compatibility reports are generated in:

- **API Reports**: `module/api/current/`
- **Binary Reports**: `module/build/reports/`
- **Combined Reports**: `build/reports/`

## Best Practices

### Development Workflow

1. **Run compatibility checks locally** before pushing
2. **Review breaking changes** carefully
3. **Update baselines** only for releases
4. **Document breaking changes** thoroughly
5. **Use semantic versioning** consistently

### Release Management

1. **Major releases**: Update baselines, bump version
2. **Minor releases**: No baseline updates needed
3. **Patch releases**: No baseline updates needed
4. **Snapshot releases**: No compatibility checks required

### Team Coordination

1. **Communicate breaking changes** early
2. **Review compatibility reports** together
3. **Update documentation** consistently
4. **Coordinate release timing** with users

## Migration Guide

When you need to make breaking changes:

### 1. Planning

```markdown
## Breaking Change: [Title]

### Impact
- Users affected: [description]
- Migration required: [yes/no]
- Deprecation timeline: [timeline]

### Migration Steps
1. [Step 1]
2. [Step 2]
3. [Step 3]

### Alternatives
- [Alternative 1]
- [Alternative 2]
```

### 2. Implementation

```kotlin
// Old API (deprecated)
@Deprecated("Use newMethod() instead", ReplaceWith("newMethod()"))
fun oldMethod(param: String): Result {
    return newMethod(param)
}

// New API
fun newMethod(param: String): Result {
    // Implementation
}
```

### 3. Documentation

- Update API documentation
- Add migration guide
- Update examples
- Communicate to users

## Tools and Dependencies

### japicmp Configuration

The binary compatibility checker uses japicmp with these settings:

```kotlin
japicmp {
    ignoreMissingClasses = true
    onlyModified = true
    packageIncludes = ["com.pulsekit.**"]
    packageExcludes = ["com.pulsekit.**.internal.**"]
}
```

### API Signature Generation

Uses `javap` to generate API signatures:

```bash
javap -public -s -p -v com.pulsekit
```

## Future Enhancements

### Planned Improvements

- **Automated baseline updates**: For release candidates
- **Enhanced reporting**: More detailed compatibility reports
- **IDE integration**: IDE plugins for compatibility checking
- **Version matrix**: Compatibility across multiple versions
- **Performance impact**: Compatibility check performance optimization

### Tooling Evolution

- **Better diff visualization**: HTML reports with side-by-side comparison
- **Integration testing**: Runtime compatibility testing
- **User impact analysis**: Estimate affected users
- **Automated migration**: Suggest migration paths

## Support

If you encounter compatibility issues:

1. **Check this guide** first
2. **Review compatibility reports**
3. **Search existing issues**
4. **Create new issue** with compatibility reports attached
5. **Contact maintainers** for guidance

---

This API compatibility system ensures PulseKit maintains its promise of backward compatibility while allowing for intentional evolution and improvement.
