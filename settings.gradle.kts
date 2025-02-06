pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()  // Add Maven Central repository for plugins if not already included
        gradlePluginPortal()  // The Gradle Plugin Portal for accessing Gradle-specific plugins
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)  // Ensures no project-specific repositories
    repositories {
        google()      // Google's Maven repository for Android dependencies
        mavenCentral() // Maven Central for other common dependencies
        jcenter()     // Optional: Add JCenter if you need additional dependencies hosted there
    }
}

rootProject.name = "Navigram"
include(":app")
