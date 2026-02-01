package com.pulsekit.gradle.convention

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        with(project) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }
            
            extensions.configure<AppExtension> {
                namespace = "com.pulsekit.sample"
                compileSdk = 34

                defaultConfig {
                    applicationId = "com.pulsekit.sample"
                    minSdk = 21
                    targetSdk = 34
                    versionCode = 1
                    versionName = "1.0"

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }
                
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
                
                kotlinOptions {
                    jvmTarget = "1.8"
                }
                
                buildFeatures {
                    viewBinding = true
                }
            }
            
            dependencies {
                add("implementation", project(":pulsekit-android"))
                add("implementation", libs.androidx.lifecycle.process)
                add("implementation", libs.androidx.startup.runtime)
                
                add("testImplementation", libs.junit)
                add("androidTestImplementation", libs.junit)
            }
        }
    }
}
