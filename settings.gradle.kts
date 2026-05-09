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

rootProject.name = "devtoys-android"
include(":app")
include(":core:ui")
include(":core:designsystem")
include(":core:common")
include(":tools:base")
include(":tools:encoders")
include(":tools:text")
include(":tools:data")
include(":tools:generators")
include(":tools:crypto")
include(":tools:developer")
include(":tools:reference")
