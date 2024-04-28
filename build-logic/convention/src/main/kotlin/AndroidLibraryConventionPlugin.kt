import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.configureKotlinAndroid
import net.maxsmr.mxstemplate.libs

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = BuildConfig.targetSdk
                defaultConfig.vectorDrawables.useSupportLibrary = true
            }
            dependencies {
                add("coreLibraryDesugaring", libs.findLibrary("android.desugarJdkLibs").get())
//                add("implementation", project(":core:kotlinx_datetime:common"))
                add("testImplementation", kotlin("test"))
                add("testImplementation", libs.findLibrary("junit4").get())
                add("androidTestImplementation", kotlin("test"))
                add("androidTestImplementation", libs.findLibrary("androidx.test.ext.junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx.test.espresso.core").get())
            }
        }
    }
}
