plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.koin)
}

android {
    namespace = "com.plcoding.spendlesssample.core.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
