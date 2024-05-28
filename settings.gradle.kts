pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        flatDir(
            "dirs" to listOf("core/android/libs", "core/ui/libs", "core/utils/libs")
        )

        maven { url = uri("https://developer.huawei.com/repo/") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MxSTemplate"
include(":app")

include(":lint")
include(":core:android")
include(":designsystem:compose")
include(":designsystem:shared_res")
include(":core:network")
include(":core:domain")
include(":core:database")
include(":core:ui")
include(":core:utils")
include(":core:di")
include(":feature:address_sorter:data")
include(":feature:address_sorter:ui")
include(":feature:mobile_services")
include(":feature:download:data")
include(":feature:download:ui")
include(":feature:preferences:data")
include(":feature:preferences:ui")
include(":feature:webview:data")
include(":feature:webview:ui")
