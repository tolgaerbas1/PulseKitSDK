#!/bin/bash

# PulseKit Publishing Script
# This script handles publishing to Maven Central with proper validation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if required environment variables are set
check_environment() {
    print_status "Checking environment variables..."
    
    local required_vars=("SIGNING_KEY_ID" "SIGNING_KEY" "SIGNING_PASSWORD" "SONATYPE_USERNAME" "SONATYPE_PASSWORD")
    local missing_vars=()
    
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing_vars+=("$var")
        fi
    done
    
    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        print_error "Missing required environment variables:"
        for var in "${missing_vars[@]}"; do
            echo "  - $var"
        done
        echo ""
        echo "Please set these environment variables or add them to ~/.gradle/gradle.properties"
        exit 1
    fi
    
    print_success "All environment variables are set"
}

# Validate the project
validate_project() {
    print_status "Validating project..."
    
    ./gradlew validateVersion
    ./gradlew checkPublishingReady
    
    print_success "Project validation passed"
}

# Run tests
run_tests() {
    print_status "Running tests..."
    
    ./gradlew test
    
    print_success "All tests passed"
}

# Build the project
build_project() {
    print_status "Building project..."
    
    ./gradlew build
    
    print_success "Build completed successfully"
}

# Generate documentation
generate_docs() {
    print_status "Generating documentation..."
    
    ./gradlew dokkaHtmlMultiModule
    
    print_success "Documentation generated"
}

# Publish to staging
publish_to_staging() {
    print_status "Publishing to Maven Central staging..."
    
    ./gradlew publishToSonatype --no-configuration-cache
    
    print_success "Published to staging repository"
}

# Get staging repository ID
get_staging_repo_id() {
    print_status "Getting staging repository ID..."
    
    local repo_id=$(curl -s -u "${SONATYPE_USERNAME}:${SONATYPE_PASSWORD}" \
        -X GET "https://s01.oss.sonatype.org/service/local/staging/profile_repositories" | \
        jq -r '.[] | select(.repositoryId | startswith("compulsekit-")) | .repositoryId')
    
    if [[ -z "$repo_id" ]]; then
        print_error "Could not find staging repository ID"
        exit 1
    fi
    
    echo "$repo_id"
}

# Wait for staging repository to close
wait_for_staging_close() {
    local repo_id=$1
    print_status "Waiting for staging repository to close..."
    
    for i in {1..30}; do
        local status=$(curl -s -u "${SONATYPE_USERNAME}:${SONATYPE_PASSWORD}" \
            -X GET "https://s01.oss.sonatype.org/service/local/staging/repository/$repo_id" | \
            jq -r '.type')
        
        if [[ "$status" == "closed" ]]; then
            print_success "Staging repository is closed"
            break
        elif [[ "$status" == "open" ]]; then
            print_status "Waiting for staging repository to close... ($i/30)"
            sleep 60
        else
            print_error "Unexpected status: $status"
            exit 1
        fi
        
        if [[ $i -eq 30 ]]; then
            print_error "Timeout waiting for staging repository to close"
            exit 1
        fi
    done
}

# Release staging repository
release_staging_repo() {
    local repo_id=$1
    print_status "Releasing staging repository..."
    
    # Close the repository
    curl -s -u "${SONATYPE_USERNAME}:${SONATYPE_PASSWORD}" \
        -X POST "https://s01.oss.sonatype.org/service/local/staging/bulk/close" \
        -H "Content-Type: application/json" \
        -d "{\"data\":{\"stagedRepositoryIds\":[\"$repo_id\"]}}" || true
    
    # Promote the repository
    curl -s -u "${SONATYPE_USERNAME}:${SONATYPE_PASSWORD}" \
        -X POST "https://s01.oss.sonatype.org/service/local/staging/bulk/promote" \
        -H "Content-Type: application/json" \
        -d "{\"data\":{\"stagedRepositoryIds\":[\"$repo_id\"]}}" || true
    
    print_success "Staging repository released to Maven Central"
}

# Show usage information
show_usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --snapshot     Publish as snapshot version"
    echo "  --dry-run      Run all checks except actual publishing"
    echo "  --help         Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    # Publish release version"
    echo "  $0 --snapshot         # Publish snapshot version"
    echo "  $0 --dry-run          # Validate without publishing"
}

# Main script
main() {
    local snapshot=false
    local dry_run=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --snapshot)
                snapshot=true
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    print_status "Starting PulseKit publishing process..."
    
    # Check environment
    check_environment
    
    # Prepare version
    if [[ "$snapshot" == true ]]; then
        print_status "Preparing snapshot release..."
        ./gradlew versionSnapshot
    else
        print_status "Preparing release..."
        ./gradlew versionRelease
    fi
    
    # Validate project
    validate_project
    
    # Run tests
    run_tests
    
    # Build project
    build_project
    
    # Generate documentation
    generate_docs
    
    # Show version info
    ./gradlew versionInfo
    
    if [[ "$dry_run" == true ]]; then
        print_success "Dry run completed successfully"
        print_status "Run without --dry-run to actually publish"
        exit 0
    fi
    
    # Publish to staging
    publish_to_staging
    
    # Get staging repository ID
    local repo_id=$(get_staging_repo_id)
    print_status "Staging repository ID: $repo_id"
    
    # Wait for staging to close
    wait_for_staging_close "$repo_id"
    
    # Release staging repository
    if [[ "$snapshot" == false ]]; then
        release_staging_repo "$repo_id"
        print_success "Release published to Maven Central!"
        print_status "It may take 10-30 minutes to appear in search results"
    else
        print_success "Snapshot published to Maven Central!"
        print_status "Available immediately in snapshot repository"
    fi
}

# Run main function
main "$@"
