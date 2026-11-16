plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.convention.compose)
    alias(libs.plugins.convention.koin)
}

android {
    namespace = "com.plcoding.spendlesssample"

    defaultConfig {
        applicationId = "com.plcoding.spendlesssample"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        disable += "Instantiatable"
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.presentation)
    implementation(projects.core.designSystem)
    implementation(projects.feature.transactions.domain)
    implementation(projects.feature.transactions.data)
    implementation(projects.feature.transactions.presentation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
