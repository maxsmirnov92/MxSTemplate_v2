import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureGatewayConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("mxs.template.feature")
            }
        }
    }
}
