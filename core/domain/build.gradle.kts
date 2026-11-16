plugins {
    alias(libs.plugins.convention.domain.module)
    `java-test-fixtures`
}

dependencies {
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
