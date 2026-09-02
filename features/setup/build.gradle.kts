// Choosing what to play: which puzzle, and on how big a board. It knows what a puzzle is and
// never which one, and it never starts the game itself — it answers with a choice and the app
// decides where that leads.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(from = rootProject.file("gradle/view-model-coverage.gradle.kts"))

android {
    namespace = "com.mdimitrov.puzzles.setup"
    resourcePrefix = "setup_"

    buildFeatures {
        compose = true
    }

    testOptions {
        // A library declares no target, and Robolectric then runs at a level of its choosing
        // rather than the app's.
        targetSdk = 35

        unitTests.isIncludeAndroidResources = true

        // Robolectric rasterises for real here, and a screen's bitmap does not fit in the
        // half a gigabyte a test JVM is given by default.
        unitTests.all { test -> test.maxHeapSize = "2g" }
    }
}

dependencies {
    api(project(":core:puzzletype"))
    // `api`: the palette the player chose is on this module's own state.
    api(project(":core:settings"))
    implementation(project(":core:scope"))
    implementation(project(":core:boardlogic"))
    implementation(project(":core:ui"))

    api(libs.dagger)
    api(libs.javax.inject)
    api(libs.navigation.common)
    implementation(libs.hilt.android)
    compileOnly(libs.hilt.core)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text)
    implementation(libs.compose.ui.unit)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)

    // The screen is drawn and tested with real puzzles. It does not compile against one:
    // these are fixtures, not dependencies, and the release graph has no game in it.
    testImplementation(project(":games:nqueens"))

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.annotations)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    debugRuntimeOnly(libs.compose.ui.test.manifest)
}
