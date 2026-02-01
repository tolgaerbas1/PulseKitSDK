plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.maven.publish) apply false
}

// Apply versioning configuration
apply(from = "gradle/versioning.gradle.kts")

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Multi-module documentation
tasks.register<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtmlMultiModule") {
    outputDirectory.set(layout.buildDirectory.dir("dokka/htmlMultiModule"))
    moduleName.set("PulseKit")
    
    dokkaSourceSets {
        configureEach {
            includes.from("Module.md")
            sourceLink {
                localDirectory.set(project.file("src"))
                remoteUrl.set(java.net.URI("https://github.com/pulsekit/pulsekit/tree/main"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}

// Publishing validation
tasks.register("validateAllModules") {
    description = "Validates all modules for publishing"
    group = "publishing"
    
    dependsOn(
        ":pulsekit-core:validatePublishing",
        ":pulsekit-android:validatePublishing"
    )
    
    doLast {
        println("✅ All modules are ready for publishing")
    }
}

// Check all modules are ready for publishing
tasks.register("checkAllModulesReady") {
    description = "Checks if all modules are ready for publishing"
    group = "publishing"
    
    dependsOn(
        "validateAllModules",
        ":pulsekit-core:test",
        ":pulsekit-android:test",
        "dokkaHtmlMultiModule",
        "checkApiCompatibility"
    )
    
    doLast {
        println("🚀 All modules are ready for publishing to Maven Central")
    }
}

// API compatibility checking
tasks.register("checkApiCompatibility") {
    description = "Check API compatibility for all modules"
    group = "verification"
    
    dependsOn(
        ":pulsekit-core:checkApiCompatibility",
        ":pulsekit-android:checkApiCompatibility"
    )
    
    doLast {
        println("✅ API compatibility check completed for all modules")
    }
}

// Binary compatibility checking
tasks.register("checkBinaryCompatibility") {
    description = "Check binary compatibility for all modules"
    group = "verification"
    
    dependsOn(
        ":pulsekit-core:japicmpJvm",
        ":pulsekit-core:japicmpAndroidRelease",
        ":pulsekit-android:japicmpRelease"
    )
    
    doLast {
        println("✅ Binary compatibility check completed for all modules")
    }
}

// Update API baselines
tasks.register("updateApiBaselines") {
    description = "Update API baselines for all modules"
    group = "api-compatibility"
    
    dependsOn(
        ":pulsekit-core:updateApiBaseline",
        ":pulsekit-android:updateApiBaseline"
    )
    
    doLast {
        println("✅ API baselines updated for all modules")
    }
}
