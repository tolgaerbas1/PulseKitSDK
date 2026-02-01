# Publishing Guide

This guide covers publishing PulseKit SDK to Maven Central, including setup, versioning, and release processes.

## Overview

PulseKit uses a fully automated publishing pipeline with proper semantic versioning, GPG signing, and Maven Central compliance. The publishing process is designed to be safe, reliable, and easy to follow.

## Prerequisites

### 1. Sonatype Account

1. Create a Sonatype account at [https://oss.sonatype.org](https://oss.sonatype.org)
2. Request publishing rights for the `com.pulsekit` groupId
3. Wait for approval (usually 1-2 business days)

### 2. GPG Key Setup

Generate a GPG key for signing artifacts:

```bash
# Generate GPG key
gpg --gen-key

# List keys (find your key ID)
gpg --list-secret-keys --keyid-format LONG

# Export public key
gpg --armor --export YOUR_KEY_ID > public-key.asc

# Upload public key to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

### 3. Environment Variables

Set the following environment variables or add to `~/.gradle/gradle.properties`:

```properties
# GPG Signing
SIGNING_KEY_ID=YOUR_GPG_KEY_ID
SIGNING_KEY=YOUR_GPG_PRIVATE_KEY
SIGNING_PASSWORD=YOUR_GPG_PASSWORD

# Sonatype Credentials
SONATYPE_USERNAME=YOUR_SONATYPE_USERNAME
SONATYPE_PASSWORD=YOUR_SONATYPE_PASSWORD
```

## Version Management

### Semantic Versioning

PulseKit follows [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

### Version Format

```
X.Y.Z[-SNAPSHOT]
```

Examples:
- `1.0.0` - Release version
- `1.0.1` - Patch release
- `1.1.0` - Minor release
- `2.0.0` - Major release
- `1.1.0-SNAPSHOT` - Development version

### Version Bumping

Use the provided Gradle tasks to bump versions:

```bash
# Patch version (1.0.0 -> 1.0.1)
./gradlew versionPatch

# Minor version (1.0.0 -> 1.1.0)
./gradlew versionMinor

# Major version (1.0.0 -> 2.0.0)
./gradlew versionMajor

# Add SNAPSHOT suffix
./gradlew versionSnapshot

# Remove SNAPSHOT suffix
./gradlew versionRelease
```

## Publishing Process

### 1. Preparation

Before publishing, ensure:

1. All tests pass
2. Documentation is up to date
3. Version is correct
4. Changelog is updated

```bash
# Validate everything
./gradlew checkPublishingReady
```

### 2. Local Publishing

Use the provided script for local publishing:

```bash
# Make script executable
chmod +x scripts/publish.sh

# Publish release version
./scripts/publish.sh

# Publish snapshot version
./scripts/publish.sh --snapshot

# Dry run (validate without publishing)
./scripts/publish.sh --dry-run
```

### 3. Manual Publishing

If you prefer manual publishing:

```bash
# Prepare release
./gradlew prepareRelease

# Publish to staging
./gradlew publishToSonatype

# Or for snapshot
./gradlew prepareSnapshot
./gradlew publishToSonatype
```

## Automated Publishing

### GitHub Actions

The project includes a GitHub Actions workflow for automated publishing:

1. **Tagged Releases**: Automatically publishes when tags are pushed
2. **Manual Triggers**: Can be triggered manually with version input
3. **Validation**: Runs all tests and validations before publishing
4. **Staging Management**: Handles staging repository lifecycle

#### Triggering Release

```bash
# Create and push tag (triggers automated release)
git tag v1.0.0
git push origin v1.0.0
```

#### Manual Workflow

1. Go to GitHub Actions
2. Select "Publish to Maven Central" workflow
3. Click "Run workflow"
4. Enter version details
5. Wait for completion

## Maven Central Requirements

### Required Metadata

Each publication includes:

- **Group ID**: `com.pulsekit`
- **Artifact ID**: Module name (`pulsekit-core`, `pulsekit-android`)
- **Version**: Semantic version
- **Name**: Human-readable name
- **Description**: Brief description
- **URL**: Project URL
- **License**: Apache License 2.0
- **Developer Information**: Name, email, ID
- **SCM Information**: Git repository details

### Required Files

Each publication includes:

- **JAR**: Main artifact
- **Sources JAR**: Source code
- **Javadoc JAR**: Generated documentation
- **POM**: Maven metadata file
- **Signature Files**: GPG signatures for all artifacts

### Validation

The publishing process validates:

- ✅ Semantic version format
- ✅ Required metadata presence
- ✅ GPG signatures
- ✅ Documentation generation
- ✅ Test coverage
- ✅ No snapshot dependencies in releases

## Staging Repository

### What is Staging?

Maven Central uses a staging repository system:

1. **Upload**: Artifacts are uploaded to staging
2. **Validation**: Maven Central validates artifacts
3. **Close**: Repository is closed and validated
4. **Release**: Artifacts are promoted to Maven Central
5. **Sync**: Artifacts sync to search (10-30 minutes)

### Monitoring Staging

You can monitor the staging process:

1. Go to [https://oss.sonatype.org](https://oss.sonatype.org)
2. Log in with your Sonatype credentials
3. Navigate to "Staging Repositories"
4. Find your repository (starts with `compulsekit-`)
5. Check status and logs

### Manual Operations

If automated release fails:

```bash
# Get staging repository ID
./gradlew getStagingRepoId

# Close repository manually
curl -u "$SONATYPE_USERNAME:$SONATYPE_PASSWORD" \
  -X POST "https://s01.oss.sonatype.org/service/local/staging/bulk/close" \
  -d '{"data":{"stagedRepositoryIds":["REPO_ID"]}}'

# Release repository manually
curl -u "$SONATYPE_USERNAME:$SONATYPE_PASSWORD" \
  -X POST "https://s01.oss.sonatype.org/service/local/staging/bulk/promote" \
  -d '{"data":{"stagedRepositoryIds":["REPO_ID"]}}'
```

## Troubleshooting

### Common Issues

#### 1. GPG Signing Errors

**Problem**: `PGP signature verification failed`

**Solution**:
- Check GPG key is properly configured
- Verify key is uploaded to keyserver
- Ensure key password is correct

#### 2. Staging Repository Errors

**Problem**: Repository fails validation

**Solution**:
- Check staging repository logs
- Ensure all required files are present
- Verify no snapshot dependencies in release

#### 3. Version Conflicts

**Problem**: Version already exists

**Solution**:
- Increment version number
- Check if version was already published
- Use snapshot version for development

#### 4. Network Issues

**Problem**: Connection timeout to Sonatype

**Solution**:
- Check network connectivity
- Verify Sonatype credentials
- Try publishing during off-peak hours

### Debug Mode

Enable debug logging:

```bash
./gradlew publishToSonatype --info --debug
```

### Cleanup

If you need to clean up a failed release:

```bash
# Drop staging repository
./gradlew dropStagingRepository

# Clean local build
./gradlew clean
```

## Release Checklist

### Before Release

- [ ] All tests pass
- [ ] Documentation is updated
- [ ] Version is bumped correctly
- [ ] Changelog is updated
- [ ] GPG key is configured
- [ ] Environment variables are set
- [ ] Staging repository is empty

### After Release

- [ ] Verify artifacts appear in Maven Central
- [ ] Check documentation links work
- [ ] Update website/documentation
- [ ] Create GitHub release
- [ ] Announce release
- [ ] Monitor for issues

## Monitoring

### Maven Central

Check if your release is live:

- [https://search.maven.org](https://search.maven.org)
- [https://repo1.maven.org/maven2/com/pulsekit](https://repo1.maven.org/maven2/com/pulsekit)

### GitHub Actions

Monitor automated publishing:

- [GitHub Actions tab](https://github.com/pulsekit/pulsekit/actions)
- Check workflow logs
- Verify staging operations

### Analytics

Track download statistics:

- [Maven Central stats](https://search.maven.org/stats)
- GitHub repository insights
- Custom analytics if configured

## Best Practices

### Version Management

- Use semantic versioning consistently
- Keep CHANGELOG updated
- Tag releases in Git
- Use snapshots for development

### Security

- Never commit secrets to repository
- Use environment variables for credentials
- Rotate GPG keys periodically
- Monitor Sonatype account access

### Quality

- Maintain high test coverage
- Keep documentation current
- Validate before publishing
- Monitor for issues post-release

### Communication

- Announce releases clearly
- Provide migration guides for major versions
- Respond to user feedback promptly
- Document breaking changes

## Support

If you encounter issues:

1. Check this guide first
2. Review GitHub Actions logs
3. Check Sonatype staging repository
4. Search existing issues
5. Create new issue with details

For Sonatype-specific issues:
- [Sonatype Support](https://issues.sonatype.org)
- [Maven Central Documentation](https://central.sonatype.org/publish/publish-guide)

## Automation Scripts

### Full Release Script

```bash
#!/bin/bash
# Full release automation

set -e

echo "🚀 Starting PulseKit release process..."

# Validate environment
./gradlew checkPublishingReady

# Run tests
./gradlew test

# Build and generate docs
./gradlew build dokkaHtmlMultiModule

# Bump version
./gradlew versionPatch

# Publish
./scripts/publish.sh

echo "✅ Release completed successfully!"
```

### Snapshot Script

```bash
#!/bin/bash
# Snapshot publishing automation

set -e

echo "📸 Publishing snapshot..."

# Prepare snapshot
./gradlew versionSnapshot

# Publish snapshot
./scripts/publish.sh --snapshot

echo "✅ Snapshot published successfully!"
```

This publishing system ensures reliable, automated releases to Maven Central with proper validation and monitoring.
