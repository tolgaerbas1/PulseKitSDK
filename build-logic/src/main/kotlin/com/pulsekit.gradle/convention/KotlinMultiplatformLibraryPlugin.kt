package com.pulsekit.gradle.convention

import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class KotlinMultiplatformLibraryPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        with(project) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.dokka")
                apply("com.vanniktech.maven.publish")
                apply("signing")
                apply("com.pulsekit.api-compatibility")
            }
            
            extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                jvm {
                    jvmToolchain(17)
                }
                
                androidTarget {
                    publishLibraryVariants("release")
                }
                
                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.kotlinx.coroutines.core)
                    }
                    
                    commonTest.dependencies {
                        implementation(libs.kotlinx.coroutines.test)
                        implementation(libs.junit)
                    }
                    
                    jvmMain.dependencies {
                        implementation(libs.kotlin.stdlib)
                    }
                    
                    jvmTest.dependencies {
                        implementation(libs.mockito.core)
                    }
                    
                    androidMain.dependencies {
                        implementation(libs.kotlin.stdlib)
                    }
                }
            }
            
            // Configure Dokka for documentation generation
            tasks.register<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtmlMultiModule") {
                outputDirectory.set(project.layout.buildDirectory.dir("dokka/htmlMultiModule"))
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
            
            extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
                publishToMavenCentral(SonatypeHost.DEFAULT, true)
                signAllPublications()
                
                coordinates("com.pulsekit", project.name, project.version.toString())
                
                pom {
                    name.set(property("POM_NAME").toString())
                    description.set(property("POM_DESCRIPTION").toString())
                    url.set(property("POM_URL").toString())
                    
                    inceptionYear.set("2024")
                    
                    licenses {
                        license {
                            name.set(property("POM_LICENSE_NAME").toString())
                            url.set(property("POM_LICENSE_URL").toString())
                            distribution.set("repo")
                            comments.set("Apache License, Version 2.0")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set(property("POM_DEVELOPER_ID").toString())
                            name.set(property("POM_DEVELOPER_NAME").toString())
                            email.set(property("POM_DEVELOPER_EMAIL").toString())
                            url.set("https://github.com/pulsekit")
                            roles.addAll("developer", "architect")
                        }
                    }
                    
                    scm {
                        url.set(property("POM_SCM_URL").toString())
                        connection.set(property("POM_SCM_CONNECTION").toString())
                        developerConnection.set(property("POM_SCM_DEV_CONNECTION").toString())
                        tag.set("HEAD")
                    }
                    
                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/pulsekit/pulsekit/issues")
                    }
                    
                    ciManagement {
                        system.set("GitHub Actions")
                        url.set("https://github.com/pulsekit/pulsekit/actions")
                    }
                }
                
                // Configure publishing for different platforms
                publications {
                    create<MavenPublication>("kotlinMultiplatform") {
                        artifactId = project.name
                    }
                }
            }
            
            // Configure signing
            extensions.configure<org.gradle.plugins.signing.SigningExtension> {
                val signingKeyId = project.findProperty("SIGNING_KEY_ID") as? String
                val signingKey = project.findProperty("SIGNING_KEY") as? String
                val signingPassword = project.findProperty("SIGNING_PASSWORD") as? String
                
                if (signingKeyId != null && signingKey != null && signingPassword != null) {
                    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
                    sign(extensions.getByName("publishing"))
                }
            }
            
            // Configure Java compatibility
            java {
                withSourcesJar()
                withJavadocJar()
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
            
            // Add validation tasks
            tasks.register("validatePublishing") {
                description = "Validates that all required publishing properties are set"
                group = "publishing"
                
                doLast {
                    val requiredProperties = listOf(
                        "POM_NAME", "POM_DESCRIPTION", "POM_URL",
                        "POM_LICENSE_NAME", "POM_LICENSE_URL",
                        "POM_DEVELOPER_ID", "POM_DEVELOPER_NAME", "POM_DEVELOPER_EMAIL",
                        "POM_SCM_URL", "POM_SCM_CONNECTION", "POM_SCM_DEV_CONNECTION",
                        "SIGNING_KEY_ID", "SIGNING_KEY", "SIGNING_PASSWORD"
                    )
                    
                    val missingProperties = requiredProperties.filter { prop ->
                        project.findProperty(prop) == null
                    }
                    
                    if (missingProperties.isNotEmpty()) {
                        throw GradleException(
                            "Missing required publishing properties: ${missingProperties.joinToString(", ")}\n" +
                            "Set these in gradle.properties or environment variables."
                        )
                    }
                    
                    println("✅ All publishing properties are configured")
                }
            }
            
            // Add publishing check task
            tasks.register("checkPublishingReady") {
                description = "Checks if the project is ready for publishing"
                group = "publishing"
                
                dependsOn("validatePublishing", "test", "dokkaHtmlMultiModule", "checkApiCompatibility")
                
                doLast {
                    println("🚀 Project is ready for publishing to Maven Central")
                }
            }
        }
    }
}
