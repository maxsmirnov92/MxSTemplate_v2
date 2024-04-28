import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import net.maxsmr.mxstemplate.libs

class AndroidFeatureUiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("mxs.template.feature")
                apply("androidx.navigation.safeargs.kotlin")
                apply("org.jetbrains.kotlin.plugin.parcelize")
            }

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:database"))
                add("implementation", project(":core:utils"))
                add("implementation", project(":core:network"))
                add("implementation", project(":core:android"))
                add("implementation", project(":core:ui"))
//                add("implementation", libs.findLibrary("orbit.viewmodel").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.viewmodel.ktx").get())
            }
        }
    }
}
