import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "net.maxsmr.mxstemplate"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
//    compileOnly(libs.firebase.crashlytics.gradlePlugin)
//    compileOnly(libs.firebase.appdistribution.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mxs.template.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "mxs.template.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "mxs.template.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "mxs.template.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidFeatureData") {
            id = "mxs.template.feature.data"
            implementationClass = "AndroidFeatureDataConventionPlugin"
        }
        register("androidFeatureUi") {
            id = "mxs.template.feature.ui"
            implementationClass = "AndroidFeatureUiConventionPlugin"
        }
        register("androidFeatureGateway") {
            id = "mxs.template.feature.gateway"
            implementationClass = "AndroidFeatureGatewayConventionPlugin"
        }
        register("androidComposeFeature") {
            id = "mxs.template.feature.compose"
            implementationClass = "AndroidFeatureComposeConventionPlugin"
        }
        register("androidViewFeature") {
            id = "mxs.template.feature.view"
            implementationClass = "AndroidFeatureViewConventionPlugin"
        }
        register("androidHilt") {
            id = "mxs.template.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "mxs.template.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("jvmLibrary") {
            id = "mxs.template.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
