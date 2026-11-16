plugins {
    alias(libs.plugins.convention.domain.module)
}

dependencies {
    implementation(projects.core.domain)
}
