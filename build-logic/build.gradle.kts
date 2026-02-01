plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.2.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.10")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.25.3")
    implementation("com.github.siom79.japicmp:japicmp:0.17.2")
}
