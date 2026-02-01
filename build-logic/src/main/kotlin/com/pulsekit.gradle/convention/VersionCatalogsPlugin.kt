package com.pulsekit.gradle.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

class VersionCatalogsPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        with(project) {
            // Define version catalog access for convention plugins
            extra["libs"] = extensions.getByName("libs")
        }
    }
}
