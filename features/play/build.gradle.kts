// The board a puzzle is played on: placing and removing pieces, what conflicts, the clock, the
// win card and the sounds. It knows what a puzzle is and never which one — a game module provides
// that, and this module is what a second puzzle reuses rather than copies.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(from = rootProject.file("gradle/view-model-coverage.gradle.kts"))

android {
    namespace = "com.mdimitrov.puzzles.play"
    resourcePrefix = "play_"

    buildFeatures {
        compose = true
    }

    testOptions {
        // A library declares no target, and Robolectric then runs at a level of its choosing
        // rather than the app's.
        targetSdk = 35

        unitTests.isIncludeAndroidResources = true

        // Robolectric rasterises for real here, and a board's bitmap does not fit in the
        // half a gigabyte a test JVM is given by default.
        unitTests.all { test -> test.maxHeapSize = "2g" }
    }
}

dependencies {
    api(project(":core:puzzletype"))
    implementation(project(":core:scope"))
    api(project(":core:solves"))
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
    implementation(libs.androidx.annotation)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.core)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.saveable)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.geometry)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text)
    implementation(libs.compose.ui.unit)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.kotlinx.coroutines.core)

    // The board is drawn and tested with a real puzzle. It does not compile against one:
    // this is a fixture, not a dependency, and the release graph has no game in it.
    testImplementation(project(":games:nqueens"))

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.annotations)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.activity)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    debugRuntimeOnly(libs.compose.ui.test.manifest)
}
