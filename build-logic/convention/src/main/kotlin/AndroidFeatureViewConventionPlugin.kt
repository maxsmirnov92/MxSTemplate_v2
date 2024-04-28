import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureViewConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("mxs.template.feature.ui")
            }

            dependencies {
                add("implementation", project(":designsystem:view"))
                add("implementation", project(":designsystem:shared_res"))
            }
        }
    }
}
