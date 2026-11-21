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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://www.jitpack.io") // dependensi transitif LiveKit (audioswitch)
        flatDir {
            dirs("unityLibrary/libs")
        }
    }
}

rootProject.name = "ven-mobile"
include(":app")
include(":unityLibrary")