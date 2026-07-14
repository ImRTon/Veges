pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Veges"

include(
    ":app",
    ":domain",
    ":data",
    ":design-system",
    ":release-tool",
    ":home",
    ":catalog",
    ":detail",
    ":alerts",
)
