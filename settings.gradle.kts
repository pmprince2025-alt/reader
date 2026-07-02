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
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "Folio"
include(":app")
include(":core:core-ui")
include(":core:core-common")
include(":core:core-database")
include(":core:core-datastore")
include(":data:data-library")
include(":data:data-reader")
include(":domain:domain-library")
include(":domain:domain-reader")
include(":feature:feature-bookshelf")
include(":feature:feature-reader")
include(":feature:feature-settings")
include(":pdf-engine")
include(":benchmark:macrobenchmark")
include(":benchmark:baseline-profile")
