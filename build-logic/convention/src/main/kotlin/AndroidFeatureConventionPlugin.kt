import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Feature presentation modules: library + Compose + Koin, plus the shared core modules and the
 * libraries every screen uses.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("convention-android-library"))
            pluginManager.apply(libs.pluginId("convention-compose"))
            pluginManager.apply(libs.pluginId("convention-koin"))

            dependencies {
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:presentation"))
                "implementation"(project(":core:design-system"))

                "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
                "implementation"(libs.library("androidx-lifecycle-viewmodel-compose"))
                "implementation"(libs.library("androidx-navigation-compose"))
                "implementation"(libs.library("kotlinx-serialization-json"))
                "implementation"(libs.library("kotlinx-collections-immutable"))
            }
        }
    }
}
