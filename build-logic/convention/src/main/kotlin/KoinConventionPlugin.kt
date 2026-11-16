import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Koin for Android modules: the Koin BOM plus `koin-android` and `koin-androidx-compose`. */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                "implementation"(platform(libs.library("koin-bom")))
                "implementation"(libs.findBundle("koin-android").get())
            }
        }
    }
}
