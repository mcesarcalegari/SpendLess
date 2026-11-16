plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.room)
    alias(libs.plugins.convention.koin)
}

android {
    namespace = "com.plcoding.spendlesssample.core.database"
}

dependencies {
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)
}
