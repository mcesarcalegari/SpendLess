import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Pure Kotlin/JVM domain modules: no Android, JVM toolchain 11, coroutines, JUnit 5 tests. */
class DomainModuleConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-jvm"))

            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(SpendLessJavaVersion.majorVersion.toInt())
            }

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }

            dependencies {
                "implementation"(libs.library("kotlinx-coroutines-core"))

                "testImplementation"(platform(libs.library("junit-bom")))
                "testImplementation"(libs.library("junit-jupiter-api"))
                "testImplementation"(libs.library("junit-jupiter-params"))
                "testRuntimeOnly"(libs.library("junit-jupiter-engine"))
                "testRuntimeOnly"(libs.library("junit-platform-launcher"))
                "testImplementation"(libs.library("assertk"))
                "testImplementation"(libs.library("turbine"))
                "testImplementation"(libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
