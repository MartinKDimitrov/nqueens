pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Puzzles"

include(":app")
include(":core:boardlogic")
include(":core:database")
include(":core:ui")
include(":core:solves")
include(":core:puzzletype")
include(":core:scope")
include(":core:settings")
include(":features:setup")
include(":features:play")
include(":features:scores")
include(":games:nqueens")
