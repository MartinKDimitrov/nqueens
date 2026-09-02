// One puzzle: the rules queens threaten along, the words the screens say about them, and the
// glyph. It depends on the contract and on nothing that draws it, so a second puzzle is a copy
// of this module with its rules and its words changed.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mdimitrov.puzzles.nqueens"
    resourcePrefix = "nqueens_"

    // Its words are resources, and whether they take the arguments the screens pass them can
    // only be answered by resolving them. That needs a real resource table.
    testOptions {
        targetSdk = 35

        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(project(":core:puzzletype"))
    api(project(":core:boardlogic"))

    // The module contributes one binding and nothing else, so it needs Hilt's annotations and
    // its compiler — not the Gradle plugin, which is for the components that aggregate them.
    api(libs.dagger)
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows.framework)
}
