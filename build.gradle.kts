plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

val sourceRoots = listOf(
    "app",
    "alerts",
    "catalog",
    "data",
    "design-system",
    "detail",
    "domain",
    "home",
    "release-tool",
)

tasks.register("verifyFormatting") {
    group = "verification"
    description = "Checks tracked source files for stable whitespace and newline formatting."
    notCompatibleWithConfigurationCache("The task scans source files directly at execution time.")

    doLast {
        val sourceExtensions = setOf("kt", "kts", "xml", "json", "md", "pro")
        val violations = sourceRoots
            .asSequence()
            .map { file(it) }
            .filter(File::exists)
            .flatMap { root ->
                root.walkTopDown()
                    .filter { candidate ->
                    candidate.isFile &&
                        candidate.extension in sourceExtensions &&
                            !candidate.path.contains("${File.separator}build${File.separator}") &&
                            !candidate.path.contains("${File.separator}schemas${File.separator}")
                    }
            }
            .filter { candidate ->
                val content = candidate.readText()
                content.replace("\r\n", "\n").contains("\r") ||
                    content.lines().any { line -> line.endsWith(" ") || line.endsWith("\t") } ||
                    !(content.endsWith("\n") || content.endsWith("\r\n"))
            }
            .map(File::getPath)
            .toList()

        check(violations.isEmpty()) {
            "Formatting violations found:\n${violations.joinToString("\n")}"
        }
    }
}

val staticAnalysis = tasks.register("staticAnalysis") {
    group = "verification"
    description = "Runs Android lint for every Android module."
}

subprojects {
    val androidProject = this
    plugins.withId("com.android.application") {
        staticAnalysis.configure { dependsOn(androidProject.tasks.named("lintDebug")) }
    }
    plugins.withId("com.android.library") {
        staticAnalysis.configure { dependsOn(androidProject.tasks.named("lintDebug")) }
    }
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn("verifyFormatting")
}
