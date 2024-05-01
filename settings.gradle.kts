pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.android.gms.strict-version-matcher-plugin") {
                useModule("com.google.android.gms:strict-version-matcher-plugin:${requested.version}")
            }
        }
    }

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

rootProject.name = "PictoChat"
include(":app")
 