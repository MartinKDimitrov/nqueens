// Where work goes when it has to outlive the screen that started it. One qualifier and the scope
// behind it: three features need the same one, and a feature cannot reach another feature.
//
// A JVM module: a coroutine scope and a Dagger qualifier are not Android, and the component that
// aggregates this module is built in the app.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.lint)
}

kotlin {
    // What a feature may see is stated rather than inferred: the qualifier, and nothing else.
    explicitApi()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // `api`: the scope and its qualifier are in the signatures of the constructors Dagger
    // generates for the view models that ask for it.
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
    api(libs.dagger)
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}
