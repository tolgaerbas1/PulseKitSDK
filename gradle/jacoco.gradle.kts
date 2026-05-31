// Jacoco configuration for test coverage
// Applied to pulsekit-core and pulsekit-android

apply(plugin = "jacoco")

configure<org.gradle.testing.jacoco.plugins.JacocoPluginExtension> {
    toolVersion = "0.8.11"
}

// Enable jacoco for all Test tasks (including jvmTest for KMP)
tasks.withType<Test>().configureEach {
    extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
        isEnabled = true
        excludes = listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*\$Lambda$*",
            "**/*\$inlined$*",
        )
    }
}

// For KMP (pulsekit-core): create jacocoTestReport that depends on jvmTest
afterEvaluate {
    val jvmTest = tasks.findByName("jvmTest")
    if (jvmTest != null && tasks.findByName("jacocoTestReport") == null) {
        tasks.register<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
            group = "verification"
            description = "Generate Jacoco coverage report from jvmTest"
            executionData.from(
                fileTree(layout.buildDirectory) {
                    include("jacoco/*.exec")
                },
            )
            additionalSourceDirs.from(
                files(
                    file("$projectDir/src/commonMain/kotlin"),
                    file("$projectDir/src/jvmMain/kotlin"),
                    file("$projectDir/src/androidMain/kotlin"),
                ).filter { it.exists() },
            )
            sourceDirectories.from(additionalSourceDirs)
            classDirectories.from(
                fileTree(layout.buildDirectory.get().asFile) {
                    include("**/classes/**/com/pulsekit/**/*.class")
                    exclude("**/R.class", "**/R$*.class", "**/*Test*")
                },
            )
            reports {
                html.required.set(true)
                xml.required.set(true)
            }
            dependsOn(jvmTest)
        }
    }
}
