plugins {
    id("com.android.application") version "8.1.4" apply false
    id("com.android.library") version "8.1.4" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.10" apply false
    id("org.jetbrains.kotlin.multiplatform") version "1.9.10" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("org.jetbrains.dokka") version "1.9.10" apply false
    id("com.vanniktech.maven.publish") version "0.25.3" apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.4" apply false
}

// Apply versioning configuration
apply(from = "gradle/versioning.gradle.kts")

// Spotless for root Gradle Kotlin scripts
apply(plugin = "com.diffplug.spotless")
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlinGradle {
        target("*.gradle.kts", "gradle/*.gradle.kts", "gradle/*.kts")
        ktlint("0.50.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Spotless & detekt for Kotlin modules
subprojects {
    if (listOf("pulsekit-core", "pulsekit-android", "sample-app").contains(name)) {
        apply(plugin = "com.diffplug.spotless")
        apply(plugin = "io.gitlab.arturbosch.detekt")

        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                ktlint("0.50.0")
                trimTrailingWhitespace()
                endWithNewline()
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint("0.50.0")
                trimTrailingWhitespace()
                endWithNewline()
            }
        }

        detekt {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(files("${rootProject.projectDir}/config/detekt.yml"))
        }
    }
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
                remoteUrl.set(java.net.URL("https://github.com/pulsekit/pulsekit/tree/main"))
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
