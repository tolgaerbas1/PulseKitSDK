plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvm {
        jvmToolchain(8)
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
        
        // Ensure android source sets exist before configuring them
        val androidMainSourceSet = findByName("androidMain") ?: create("androidMain")
        androidMainSourceSet.dependsOn(commonMain.get())
        androidMainSourceSet.dependencies {
            implementation(libs.kotlin.stdlib)
        }
        
        val androidTestSourceSet = findByName("androidTest") ?: create("androidTest")
        androidTestSourceSet.dependsOn(commonTest.get())
        androidTestSourceSet.dependencies {
            implementation(libs.junit)
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
