plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.compose)
}

android {
    namespace = "com.plcoding.spendlesssample.core.designsystem"
}

dependencies {
    api(libs.kotlinx.collections.immutable)
}
