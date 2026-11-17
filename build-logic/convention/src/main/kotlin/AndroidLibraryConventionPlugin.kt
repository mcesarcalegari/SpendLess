import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Android library modules: same SDK/JDK setup as `:app`, JUnit 5 for local tests
 * (via `de.mannodermaus.android-junit5`) and the AndroidX instrumentation runner.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))
            pluginManager.apply("de.mannodermaus.android-junit5")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.targetSdk = SpendLessSdk.TARGET
                lint.targetSdk = SpendLessSdk.TARGET
            }

            dependencies {
                "testImplementation"(platform(libs.library("junit-bom")))
                "testImplementation"(libs.library("junit-jupiter-api"))
                "testImplementation"(libs.library("junit-jupiter-params"))
                "testRuntimeOnly"(libs.library("junit-jupiter-engine"))
                "testRuntimeOnly"(libs.library("junit-platform-launcher"))
                "testImplementation"(libs.library("assertk"))
                "testImplementation"(libs.library("turbine"))
                "testImplementation"(libs.library("kotlinx-coroutines-test"))

                "androidTestImplementation"(libs.library("androidx-test-runner"))
                "androidTestImplementation"(libs.library("androidx-test-rules"))
                "androidTestImplementation"(libs.library("androidx-test-core"))
                "androidTestImplementation"(libs.library("androidx-junit"))
            }
        }
    }
}
