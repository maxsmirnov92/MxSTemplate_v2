import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.configureKotlinAndroid
import net.maxsmr.mxstemplate.libs

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = BuildConfig.targetSdk
            }
            dependencies {
                // Timber подключается в целевой app, для реализация BaseLogger
                add("implementation", libs.findLibrary("timber").get())
                add("coreLibraryDesugaring", libs.findLibrary("android.desugarJdkLibs").get())
            }
        }
    }

}