plugins {
    alias(libs.plugins.convention.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.plcoding.spendlesssample.transactions.presentation"
}

dependencies {
    implementation(projects.feature.transactions.domain)
}

dependencies {
    testImplementation(testFixtures(projects.core.domain))
    androidTestImplementation(testFixtures(projects.core.domain))
}
