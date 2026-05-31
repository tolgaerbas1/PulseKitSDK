plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("com.android.tools.build:gradle:8.7.3")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.25.3")
    // implementation("me.champeau.japicmp:japicmp-gradle:0.4.1") // Temporarily disabled
}
