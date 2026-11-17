plugins {
    `kotlin-dsl`
}

group = "com.plcoding.spendlesssample.buildlogic"

dependencies {
    // compileOnly: the root build puts these plugins on the classpath (`apply false`), so the
    // convention plugins see exactly the versions from the catalog at runtime.
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.room.gradle.plugin)
    // Not published with a plugin marker the root build could resolve, so it ships with build-logic.
    implementation(libs.android.junit5.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.convention.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.convention.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.convention.android.feature.get().pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("compose") {
            id = libs.plugins.convention.compose.get().pluginId
            implementationClass = "ComposeConventionPlugin"
        }
        register("koin") {
            id = libs.plugins.convention.koin.get().pluginId
            implementationClass = "KoinConventionPlugin"
        }
        register("domainModule") {
            id = libs.plugins.convention.domain.module.get().pluginId
            implementationClass = "DomainModuleConventionPlugin"
        }
        register("room") {
            id = libs.plugins.convention.room.get().pluginId
            implementationClass = "RoomConventionPlugin"
        }
    }
}
