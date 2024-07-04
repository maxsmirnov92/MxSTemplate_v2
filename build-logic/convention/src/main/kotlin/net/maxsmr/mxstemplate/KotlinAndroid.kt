package net.maxsmr.mxstemplate

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = BuildConfig.compileSdk

        defaultConfig {
            minSdk = BuildConfig.minSdk
        }

        buildFeatures {
            viewBinding = true
            buildConfig = true
        }

        flavorDimensions += listOf("default", "provider")

        productFlavors {
            register("appDev") {
                dimension = "default"
                buildConfigField("boolean", "LOG_WRITE_FILE", "true")
            }
            register("appProd") {
                dimension = "default"
                buildConfigField("boolean", "LOG_WRITE_FILE", "false")
            }
            register("google") { dimension = "provider" }
            register("huawei") { dimension = "provider" }

        }

        compileOptions {
            sourceCompatibility = BuildConfig.SOURCE_COMPATIBILITY_VERSION
            targetCompatibility = BuildConfig.SOURCE_COMPATIBILITY_VERSION
            isCoreLibraryDesugaringEnabled = true
        }
    }

    configureKotlin()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = BuildConfig.SOURCE_COMPATIBILITY_VERSION
        targetCompatibility = BuildConfig.SOURCE_COMPATIBILITY_VERSION
    }

    configureKotlin()
}

private fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = BuildConfig.SOURCE_COMPATIBILITY_VERSION.toString()
        }
    }
}
