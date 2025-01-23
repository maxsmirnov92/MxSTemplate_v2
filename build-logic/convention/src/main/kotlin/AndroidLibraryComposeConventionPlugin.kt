import com.android.build.gradle.LibraryExtension
import net.maxsmr.mxstemplate.configureAndroidCompose
import net.maxsmr.mxstemplate.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("mxs.template.library")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            val extension = extensions.getByType<LibraryExtension>()
            configureAndroidCompose(extension)

            dependencies {
                val bom = libs.findLibrary("androidx.compose.bom").get()
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))

                // Material Design
                add("implementation", libs.findLibrary("androidx.compose.material3").get())
                add("implementation", libs.findLibrary("androidx.compose.material.icons.extended").get())
//                add("implementation", libs.findLibrary("accompanist.navigation.material").get())

                add("implementation", libs.findLibrary("androidx.constraintlayout.compose").get())
                add("implementation", libs.findLibrary("androidx.activity.compose").get())
                add("implementation", libs.findLibrary("androidx.compose.runtime.livedata").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.runtime.compose").get())
                add("implementation", libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())
                add("implementation", libs.findLibrary("androidx.navigation.compose").get())
                add("implementation", libs.findLibrary("hilt.navigation.compose").get())
                add("implementation", libs.findLibrary("glide.compose").get())
//                add("implementation", libs.findLibrary("orbit.compose").get())

                // Android Studio Preview support
                add("implementation", libs.findLibrary("androidx.compose.ui.tooling.preview").get())
                add("debugImplementation", libs.findLibrary("androidx.compose.ui.tooling").get())
            }
        }
    }

}
