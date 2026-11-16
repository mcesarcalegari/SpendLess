import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room with KSP; the exported schema lives in `<module>/schemas`. */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            pluginManager.apply(libs.pluginId("room"))

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "implementation"(libs.library("room-runtime"))
                "implementation"(libs.library("room-ktx"))
                "ksp"(libs.library("room-compiler"))
                "androidTestImplementation"(libs.library("room-testing"))
            }
        }
    }
}
