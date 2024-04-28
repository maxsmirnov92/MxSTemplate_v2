import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import net.maxsmr.mxstemplate.libs

class AndroidFeatureComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("mxs.template.feature.ui")
                apply("mxs.template.library.compose")
            }

            dependencies {
                add("implementation", project(":designsystem:compose"))
                add("implementation", project(":designsystem:shared_res"))
//                add("implementation", libs.findLibrary("orbit.compose").get())
            }
        }
    }
}
