import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal object SpendLessSdk {
    const val COMPILE = 36
    const val COMPILE_MINOR = 1
    const val MIN = 26
    const val TARGET = 36
}

internal val SpendLessJavaVersion = JavaVersion.VERSION_11

/**
 * Shared SDK / JDK setup for every Android module (application and library).
 *
 * AGP 9 compiles Kotlin itself (built-in Kotlin), so no `org.jetbrains.kotlin.android` plugin is
 * applied; Kotlin is configured through the compile tasks AGP registers.
 */
internal fun Project.configureKotlinAndroid(extension: CommonExtension) {
    extension.apply {
        compileSdk = SpendLessSdk.COMPILE
        compileSdkMinor = SpendLessSdk.COMPILE_MINOR
        defaultConfig.minSdk = SpendLessSdk.MIN
        compileOptions.sourceCompatibility = SpendLessJavaVersion
        compileOptions.targetCompatibility = SpendLessJavaVersion
    }
    configureKotlinJvmTarget()
}

internal fun Project.configureKotlinJvmTarget() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(SpendLessJavaVersion.toString()))
    }
}
