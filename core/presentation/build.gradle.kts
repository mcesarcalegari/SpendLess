plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.compose)
}

android {
    namespace = "com.plcoding.spendlesssample.core.presentation"
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
