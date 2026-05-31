plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

apply(from = rootProject.file("gradle/jacoco.gradle.kts"))

kotlin {
    jvm {
        jvmToolchain(17)
    }

    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
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

        val androidMain by getting {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }

        val androidUnitTest by getting {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.junit)
            }
        }
    }
}

android {
    namespace = "com.pulsekit.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
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
            "-Xjvm-default=all",
        )
    }
}
