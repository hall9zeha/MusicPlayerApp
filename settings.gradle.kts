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
        // for jitpack libraries
        maven ( url = "https://jitpack.io" )
    }
}

rootProject.name = "KTMusicPlayer"
include(":app")
include(":core")
include(":data")
include(":di")
include(":features")
include(":features:mfilepicker")
include(":features:audioeffects")
include(":bass")
