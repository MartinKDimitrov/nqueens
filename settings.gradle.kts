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

include(":core:boardlogic")
include(":core:puzzletype")
include(":core:solves")
include(":core:scope")
include(":core:ui")
include(":core:settings")
include(":core:database")
include(":games:nqueens")
include(":features:setup")
include(":features:play")
include(":features:scores")
