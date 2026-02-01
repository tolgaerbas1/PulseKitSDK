package com.pulsekit.gradle.convention

import me.champeau.japicmp.gradle.JapicmpTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Convention plugin for API compatibility checking.
 * 
 * This plugin adds tasks to:
 * 1. Generate API snapshots from published artifacts
 * 2. Compare current API against baseline snapshots
 * 3. Validate binary compatibility using japicmp
 * 4. Fail builds on breaking changes unless explicitly allowed
 */
class ApiCompatibilityPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        with(project) {
            // Create API directory structure
            val apiDir = layout.projectDirectory.dir("api")
            val baselineDir = apiDir.dir("baseline")
            val currentDir = apiDir.dir("current")
            
            // Task to generate API snapshot from current build
            register<org.gradle.api.tasks.Task>("generateApiSnapshot") {
                description = "Generate API snapshot from current build"
                group = "api-compatibility"
                
                inputs.files("build/libs")
                outputs.dir(currentDir)
                
                doLast {
                    generateApiSnapshot(project, currentDir)
                }
            }
            
            // Task to update baseline API (for intentional breaking changes)
            register<org.gradle.api.tasks.Task>("updateApiBaseline") {
                description = "Update API baseline for new release (use for intentional breaking changes)"
                group = "api-compatibility"
                
                inputs.files("build/libs")
                outputs.dir(baselineDir)
                
                doLast {
                    generateApiSnapshot(project, baselineDir)
                    logger.lifecycle("✅ API baseline updated for ${project.name}")
                }
            }
            
            // Task to check API compatibility
            register<org.gradle.api.tasks.Task>("checkApiCompatibility") {
                description = "Check API compatibility against baseline"
                group = "verification"
                
                inputs.dir(baselineDir)
                inputs.dir(currentDir)
                
                // Allow forcing API compatibility check to pass
                val forceApiCompatibility = project.findProperty("FORCE_API_COMPATIBILITY") == "true"
                
                doLast {
                    if (!baselineDir.asFile.exists()) {
                        logger.lifecycle("⚠️ No API baseline found for ${project.name}. Run './gradlew updateApiBaseline' to create one.")
                        if (!forceApiCompatibility) {
                            throw GradleException("API baseline required for compatibility checking")
                        }
                        return@doLast
                    }
                    
                    if (!currentDir.asFile.exists()) {
                        logger.lifecycle("📸 Generating current API snapshot for ${project.name}")
                        generateApiSnapshot(project, currentDir)
                    }
                    
                    val compatibilityResult = compareApiSnapshots(project, baselineDir, currentDir)
                    
                    if (compatibilityResult.hasBreakingChanges && !forceApiCompatibility) {
                        val errorMessage = buildString {
                            appendLine("🚨 API compatibility issues detected in ${project.name}:")
                            appendLine()
                            compatibilityResult.breakingChanges.forEach { change ->
                                appendLine("  - ${change.type}: ${change.member}")
                                appendLine("    ${change.description}")
                            }
                            appendLine()
                            appendLine("To fix:")
                            appendLine("  1. Review the breaking changes above")
                            appendLine("  2. If intentional, run './gradlew updateApiBaseline'")
                            appendLine("  3. Or force with './gradlew checkApiCompatibility -PFORCE_API_COMPATIBILITY=true'")
                        }
                        throw GradleException(errorMessage)
                    } else if (compatibilityResult.hasBreakingChanges && forceApiCompatibility) {
                        logger.lifecycle("⚠️ API compatibility issues ignored due to FORCE_API_COMPATIBILITY=true")
                    } else {
                        logger.lifecycle("✅ API compatibility check passed for ${project.name}")
                    }
                }
            }
            
            // Register japicmp task for binary compatibility
            register<JapicmpTask>("japicmp") {
                description = "Check binary compatibility using japicmp"
                group = "verification"
                
                // Configure when we have baseline artifacts
                onlyIf { baselineDir.asFile.exists() }
                
                // This will be configured per module
            }
        }
    }
    
    private fun generateApiSnapshot(project: Project, outputDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>) {
        val projectName = project.name
        val buildLibsDir = project.layout.buildDirectory.dir("libs")
        
        // Find the published artifacts
        val artifacts = buildLibsDir.asFileTree
            .filter { it.name.startsWith(projectName) && it.name.endsWith(".jar") }
            .filter { !it.name.contains("-sources") && !it.name.contains("-javadoc") }
        
        if (artifacts.isEmpty()) {
            throw GradleException("No published artifacts found for $projectName. Build the project first.")
        }
        
        // Create output directory
        outputDir.get().asFile.mkdirs()
        
        // Copy artifacts to API snapshot directory
        artifacts.forEach { artifact ->
            val targetFile = outputDir.get().file(artifact.name)
            artifact.copyTo(targetFile, overwrite = true)
            project.logger.lifecycle("📸 API snapshot: ${artifact.name}")
        }
        
        // Generate API signature files
        artifacts.forEach { artifact ->
            generateApiSignature(project, artifact, outputDir)
        }
    }
    
    private fun generateApiSignature(project: Project, jarFile: File, outputDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>) {
        val signatureFile = outputDir.get().file("${jarFile.nameWithoutExtension}-signature.txt")
        
        project.javaexec {
            classpath = project.files(jarFile)
            mainClass.set("javap")
            
            args = listOf(
                "-cp", jarFile.absolutePath,
                "-public", // Only public API
                "-s",      // Suppress private members
                "-p",      // Package information
                "-v",      // Verbose output
                "com.pulsekit" // Only pulsekit packages
            )
            
            standardOutput = signatureFile
            errorOutput = project.logger.lifecycle
        }
        
        project.logger.lifecycle("📝 API signature: ${signatureFile.name}")
    }
    
    private fun compareApiSnapshots(
        project: Project,
        baselineDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>,
        currentDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>
    ): CompatibilityResult {
        val baselineFiles = baselineDir.asFileTree.filter { it.name.endsWith("-signature.txt") }
        val currentFiles = currentDir.asFileTree.filter { it.name.endsWith("-signature.txt") }
        
        val breakingChanges = mutableListOf<ApiChange>()
        
        baselineFiles.forEach { baselineFile ->
            val currentFile = currentDir.file(baselineFile.name)
            
            if (!currentFile.exists()) {
                breakingChanges.add(ApiChange(
                    type = "REMOVED",
                    member = baselineFile.name,
                    description = "API signature file removed"
                ))
                return@forEach
            }
            
            val baselineContent = baselineFile.readText()
            val currentContent = currentFile.readText()
            
            if (baselineContent != currentContent) {
                // Simple diff detection - in production, use a proper diff library
                val baselineLines = baselineContent.lines().toSet()
                val currentLines = currentContent.lines().toSet()
                
                val removed = baselineLines - currentLines
                val added = currentLines - baselineLines
                
                removed.forEach { line ->
                    if (line.contains("class ") || line.contains("interface ") || line.contains("public ")) {
                        breakingChanges.add(ApiChange(
                            type = "REMOVED",
                            member = extractMemberName(line),
                            description = "Public API member removed"
                        ))
                    }
                }
                
                added.forEach { line ->
                    if (line.contains("class ") || line.contains("interface ") || line.contains("public ")) {
                        // New members are not breaking changes, just informational
                        project.logger.lifecycle("➕ New API member: ${extractMemberName(line)}")
                    }
                }
            }
        }
        
        return CompatibilityResult(
            hasBreakingChanges = breakingChanges.isNotEmpty(),
            breakingChanges = breakingChanges
        )
    }
    
    private fun extractMemberName(line: String): String {
        // Extract member name from javap output
        return when {
            line.contains("class ") -> {
                val match = Regex("""class\s+(\w+)""").find(line)
                match?.groupValues?.get(1) ?: line.trim()
            }
            line.contains("interface ") -> {
                val match = Regex("""interface\s+(\w+)""").find(line)
                match?.groupValues?.get(1) ?: line.trim()
            }
            line.contains("public ") -> {
                val match = Regex("""public\s+\w+\s+(\w+)\s*\(""").find(line)
                match?.groupValues?.get(1) ?: line.trim()
            }
            else -> line.trim()
        }
    }
}

/**
 * Result of API compatibility comparison.
 */
private data class CompatibilityResult(
    val hasBreakingChanges: Boolean,
    val breakingChanges: List<ApiChange>
)

/**
 * Represents an API change detected during comparison.
 */
private data class ApiChange(
    val type: String,
    val member: String,
    val description: String
)
