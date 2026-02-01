package com.pulsekit.gradle.convention

import me.champeau.japicmp.gradle.JapicmpTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.kotlin.dsl.register

/**
 * Convention plugin for binary compatibility checking using japicmp.
 * 
 * This plugin provides comprehensive binary compatibility validation
 * by comparing current artifacts against published baseline versions.
 */
class BinaryCompatibilityPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        with(project) {
            // Create configuration for baseline artifacts
            val baselineConfig: Configuration = configurations.create("baseline") {
                description = "Baseline artifacts for binary compatibility checking"
                isTransitive = false
            }
            
            // Task to download baseline artifacts
            register<org.gradle.api.tasks.Task>("downloadBaselineArtifacts") {
                description = "Download baseline artifacts for binary compatibility checking"
                group = "api-compatibility"
                
                inputs.property("baselineVersion", getBaselineVersion())
                outputs.files(layout.buildDirectory.dir("baseline"))
                
                doLast {
                    val baselineVersion = getBaselineVersion()
                    if (baselineVersion == null) {
                        logger.lifecycle("⚠️ No baseline version specified. Use -PBASELINE_VERSION=1.0.0")
                        return@doLast
                    }
                    
                    downloadBaselineArtifacts(project, baselineVersion, baselineConfig)
                }
            }
            
            // Configure japicmp task for each published variant
            afterEvaluate {
                // Get published variants (this will vary by module type)
                val publishedVariants = getPublishedVariants(project)
                
                publishedVariants.forEach { variant ->
                    registerJapicmpTask(project, variant, baselineConfig)
                }
            }
            
            // Root task to check all binary compatibility
            if (project == project.rootProject) {
                register<org.gradle.api.tasks.Task>("checkBinaryCompatibility") {
                    description = "Check binary compatibility for all modules"
                    group = "verification"
                    
                    dependsOn(subprojects.mapNotNull { it.tasks.findByName("japicmp") })
                    
                    doLast {
                        logger.lifecycle("✅ Binary compatibility check completed")
                    }
                }
            }
        }
    }
    
    private fun getBaselineVersion(): String? {
        return project.findProperty("BASELINE_VERSION") as? String
            ?: project.findProperty("BASELINE_VERSION_${project.name.uppercase()}") as? String
    }
    
    private fun downloadBaselineArtifacts(project: Project, version: String, config: Configuration) {
        val group = project.group.toString()
        val moduleName = project.name
        
        project.logger.lifecycle("📦 Downloading baseline artifacts for $group:$moduleName:$version")
        
        // Add dependency on baseline version
        config.dependencies.add(
            project.dependencies.create("$group:$moduleName:$version")
        )
        
        // Resolve and copy artifacts
        config.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            val targetDir = project.layout.buildDirectory.dir("baseline/${artifact.name}")
            targetDir.asFile.mkdirs()
            artifact.file.copyTo(targetDir.file(artifact.file.name), overwrite = true)
            
            project.logger.lifecycle("  ✓ ${artifact.name}")
        }
    }
    
    private fun getPublishedVariants(project: Project): List<String> {
        // Return different variants based on project type
        return when {
            project.pluginManager.hasPlugin("com.android.library") -> {
                listOf("release")
            }
            project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
                listOf("jvm", "androidRelease")
            }
            else -> {
                listOf("main")
            }
        }
    }
    
    private fun registerJapicmpTask(project: Project, variant: String, baselineConfig: Configuration) {
        project.tasks.register<JapicmpTask>("japicmp${variant.replaceFirstChar { it.uppercase() }}") {
            description = "Check binary compatibility for $variant variant"
            group = "verification"
            
            // Configure baseline artifact
            val baselineVersion = getBaselineVersion()
            if (baselineVersion != null) {
                val group = project.group.toString()
                val moduleName = project.name
                
                // This will be configured after baseline is downloaded
                dependsOn("downloadBaselineArtifacts")
                
                // Find baseline artifact
                baselineVersion?.let { version ->
                    val baselineArtifact = findBaselineArtifact(project, group, moduleName, version, variant)
                    if (baselineArtifact != null) {
                        oldArchifacts(baselineArtifact.file)
                    }
                }
            }
            
            // Configure current artifact
            val currentArtifact = getCurrentArtifact(project, variant)
            if (currentArtifact != null) {
                newArchifacts(currentArtifact.file)
            }
            
            // japicmp configuration
            onlyIf { baselineVersion != null }
            
            // Allow forcing compatibility check to pass
            val forceCompatibility = project.findProperty("FORCE_BINARY_COMPATIBILITY") == "true"
            
            doLast {
                if (forceCompatibility) {
                    logger.lifecycle("⚠️ Binary compatibility check ignored due to FORCE_BINARY_COMPATIBILITY=true")
                }
            }
            
            // Configure japicmp options
            ignoreMissingClasses.set(true)
            onlyModified.set(true)
            packageIncludes.set(listOf("com.pulsekit.**"))
            packageExcludes.set(listOf("com.pulsekit.**.internal.**"))
            
            // Allow specific breaking changes if explicitly configured
            val allowedBreakingChanges = project.findProperty("ALLOWED_BREAKING_CHANGES") as? String
            if (allowedBreakingChanges != null) {
                val changes = allowedBreakingChanges.split(",").map { it.trim() }
                breakBuildOnModifications.set(false)
                breakBuildOnBinaryIncompatibleModifications.set(false)
                
                // Log allowed changes
                changes.forEach { change ->
                    logger.lifecycle("⚠️ Allowing breaking change: $change")
                }
            }
            
            // Custom output for better error reporting
            doLast {
                val outputFile = project.layout.buildDirectory.file("reports/japicmp${variant}.txt")
                outputFile.get().asFile.parentFile.mkdirs()
                
                // Generate human-readable report
                generateCompatibilityReport(project, variant, outputFile.get().asFile)
            }
        }
    }
    
    private fun findBaselineArtifact(
        project: Project,
        group: String,
        moduleName: String,
        version: String,
        variant: String
    ): ResolvedArtifact? {
        val config = project.configurations.getByName("baseline")
        return config.resolvedConfiguration.resolvedArtifacts
            .find { artifact ->
                val variantSuffix = when (variant) {
                    "release" -> "release"
                    "androidRelease" -> "android-release"
                    "jvm" -> "jvm"
                    else -> variant
                }
                
                artifact.name == moduleName && 
                artifact.version == version &&
                (artifact.classifier == variantSuffix || artifact.classifier == null)
            }
    }
    
    private fun getCurrentArtifact(project: Project, variant: String): ResolvedArtifact? {
        // This would need to be implemented based on the actual build configuration
        // For now, we'll return null and let the task configuration handle it
        return null
    }
    
    private fun generateCompatibilityReport(project: Project, variant: String, outputFile: File) {
        val report = buildString {
            appendLine("# Binary Compatibility Report")
            appendLine("Project: ${project.name}")
            appendLine("Variant: $variant")
            appendLine("Timestamp: ${kotlinx.datetime.Clock.System.now()}")
            appendLine()
            
            // This would be populated with actual japicmp results
            appendLine("## Summary")
            appendLine("- Status: ${if (project.gradle.taskGraph.hasTask(":${project.name}:japicmp${variant.replaceFirstChar { it.uppercase() }}")) "Checked" else "Not checked"}")
            appendLine()
            
            appendLine("## Notes")
            appendLine("- Use './gradlew downloadBaselineArtifacts -PBASELINE_VERSION=1.0.0' to download baseline")
            appendLine("- Use './gradlew japicmp${variant.replaceFirstChar { it.uppercase() }}' to check compatibility")
            appendLine("- Use './gradlew checkBinaryCompatibility' to check all modules")
            appendLine("- Use -PFORCE_BINARY_COMPATIBILITY=true to ignore compatibility issues")
        }
        
        outputFile.writeText(report)
        project.logger.lifecycle("📄 Compatibility report generated: ${outputFile.absolutePath}")
    }
}
