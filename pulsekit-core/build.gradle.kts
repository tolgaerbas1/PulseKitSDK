plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvm {
        jvmToolchain(17)
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.junit)
            }
        }

        val jvmMain by getting {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.sqlite.jdbc)
            }
        }

        val jvmTest by getting {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.mockito.core)
            }
        }

        // NOTE: androidMain/androidTest should only be configured when Android plugin is present.
        // They are configured below inside plugins.withId("com.android.library").
    }
}

// Configure Android target and android source sets only when Android Gradle plugin is applied to this project
plugins.withId("com.android.library") {
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
        androidTarget {
            publishLibraryVariants("release")
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }

        sourceSets {
            val androidMain by getting {
                dependsOn(commonMain.get())
                dependencies {
                    implementation(libs.kotlin.stdlib)
                }
            }

            val androidTest by getting {
                dependsOn(commonTest.get())
                dependencies {
                    implementation(libs.junit)
                }
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }
}
