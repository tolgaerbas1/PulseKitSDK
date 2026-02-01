/**
 * Version management for PulseKit SDK.
 * 
 * This file handles semantic versioning, release management,
 * and version bumping automation.
 */

plugins {
    base
}

// Version configuration
val pulsekitVersion = project.property("VERSION_NAME") as String
val isSnapshot = pulsekitVersion.endsWith("-SNAPSHOT")
val isRelease = !isSnapshot

// Validate version format
tasks.register("validateVersion") {
    description = "Validates that the version follows semantic versioning"
    group = "verification"
    
    doLast {
        val versionRegex = Regex("""^\d+\.\d+\.\d+(-SNAPSHOT)?$""")
        if (!versionRegex.matches(pulsekitVersion)) {
            throw GradleException(
                "Invalid version format: $pulsekitVersion. Expected format: X.Y.Z or X.Y.Z-SNAPSHOT"
            )
        }
        println("✅ Version format is valid: $pulsekitVersion")
    }
}

// Version bumping tasks
tasks.register("versionPatch") {
    description = "Increments the patch version (X.Y.Z -> X.Y.Z+1)"
    group = "versioning"
    
    doLast {
        val currentVersion = pulsekitVersion.removeSuffix("-SNAPSHOT")
        val parts = currentVersion.split(".")
        val newVersion = "${parts[0]}.${parts[1]}.${parts[2].toInt() + 1}"
        updateVersion(newVersion)
    }
}

tasks.register("versionMinor") {
    description = "Increments the minor version (X.Y.Z -> X.Y+1.0)"
    group = "versioning"
    
    doLast {
        val currentVersion = pulsekitVersion.removeSuffix("-SNAPSHOT")
        val parts = currentVersion.split(".")
        val newVersion = "${parts[0]}.${parts[1].toInt() + 1}.0"
        updateVersion(newVersion)
    }
}

tasks.register("versionMajor") {
    description = "Increments the major version (X.Y.Z -> X+1.0.0)"
    group = "versioning"
    
    doLast {
        val currentVersion = pulsekitVersion.removeSuffix("-SNAPSHOT")
        val parts = currentVersion.split(".")
        val newVersion = "${parts[0].toInt() + 1}.0.0"
        updateVersion(newVersion)
    }
}

tasks.register("versionSnapshot") {
    description = "Adds SNAPSHOT suffix to current version"
    group = "versioning"
    
    doLast {
        val currentVersion = pulsekitVersion.removeSuffix("-SNAPSHOT")
        val newVersion = "$currentVersion-SNAPSHOT"
        updateVersion(newVersion)
    }
}

tasks.register("versionRelease") {
    description = "Removes SNAPSHOT suffix from current version"
    group = "versioning"
    
    doLast {
        val currentVersion = pulsekitVersion.removeSuffix("-SNAPSHOT")
        updateVersion(currentVersion)
    }
}

// Helper function to update version in gradle.properties
fun updateVersion(newVersion: String) {
    val gradlePropsFile = file("gradle.properties")
    val properties = java.util.Properties()
    
    // Read existing properties
    gradlePropsFile.reader().use { properties.load(it) }
    
    // Update version
    properties["VERSION_NAME"] = newVersion
    
    // Write back to file
    gradlePropsFile.writer().use { properties.store(it, "PulseKit SDK Version") }
    
    println("🔖 Version updated to: $newVersion")
}

// Release preparation tasks
tasks.register("prepareRelease") {
    description = "Prepares the project for release"
    group = "release"
    
    dependsOn("validateVersion", "versionRelease", "checkPublishingReady")
    
    doLast {
        println("🚀 Project is ready for release")
        println("Version: ${project.property("VERSION_NAME")}")
        println("Run './gradlew publishToSonatype' to publish to Maven Central")
    }
}

// Snapshot preparation tasks
tasks.register("prepareSnapshot") {
    description = "Prepares the project for snapshot release"
    group = "release"
    
    dependsOn("validateVersion", "versionSnapshot", "checkPublishingReady")
    
    doLast {
        println("📸 Project is ready for snapshot release")
        println("Version: ${project.property("VERSION_NAME")}")
        println("Run './gradlew publishToSonatype' to publish to Maven Central")
    }
}

// Version information task
tasks.register("versionInfo") {
    description = "Displays current version information"
    group = "information"
    
    doLast {
        println("PulseKit SDK Information:")
        println("  Version: $pulsekitVersion")
        println("  Type: ${if (isSnapshot) "Snapshot" else "Release"}")
        println("  Build: ${System.getProperty("user.name")}@${System.getProperty("user.dir")}")
        println("  Java: ${System.getProperty("java.version")}")
        println("  Gradle: ${gradle.gradleVersion}")
    }
}
