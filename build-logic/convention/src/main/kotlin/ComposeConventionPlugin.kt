import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Compose for an Android module that already applies the application or library convention:
 * Kotlin compose compiler, `buildFeatures.compose`, the Compose BOM and the base UI artifacts.
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-compose"))

            extensions.getByType<CommonExtension>().buildFeatures.compose = true

            dependencies {
                val bom = platform(libs.library("androidx-compose-bom"))
                "implementation"(bom)
                "implementation"(libs.library("androidx-compose-ui"))
                "implementation"(libs.library("androidx-compose-ui-graphics"))
                "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
                "implementation"(libs.library("androidx-compose-material3"))
                "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
                "debugImplementation"(libs.library("androidx-compose-ui-test-manifest"))
                "androidTestImplementation"(bom)
                "androidTestImplementation"(libs.library("androidx-compose-ui-test-junit4"))
                // ui-test-junit4 pulls espresso-core 3.5.0, which calls InputManager.getInstance(),
                // removed on newer Android; the catalog version replaces it.
                "androidTestImplementation"(libs.library("androidx-espresso-core"))
            }
        }
    }
}
