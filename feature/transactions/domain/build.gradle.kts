plugins {
    alias(libs.plugins.convention.domain.module)
    `java-test-fixtures`
}

dependencies {
    api(projects.core.domain)
    testFixturesImplementation(projects.core.domain)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
