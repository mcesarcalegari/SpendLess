plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.koin)
}

android {
    namespace = "com.plcoding.spendlesssample.transactions.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.feature.transactions.domain)
}
