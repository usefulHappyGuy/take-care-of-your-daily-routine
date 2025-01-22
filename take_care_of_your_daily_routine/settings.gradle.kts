pluginManagement {
    repositories {
        google()  // Repozytorium Google
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()   // Repozytorium Google
        mavenCentral()  // Repozytorium Maven Central
    }
}

rootProject.name = "Take care of your daily routine"
include(":app")
