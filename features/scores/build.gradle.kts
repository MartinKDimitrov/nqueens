// The boards this app has seen solved: the table they are kept in, the screen that lists them,
// and the one verb every game meets the feature through. It knows nothing of any game.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(from = rootProject.file("gradle/view-model-coverage.gradle.kts"))

android {
    namespace = "com.mdimitrov.puzzles.scores"
    resourcePrefix = "scores_"

    buildFeatures {
        compose = true
    }

    testOptions {
        // A library declares no target, and Robolectric then runs at a level whose formatting
        // data differs from the one the app ships at — dates come back as their own pattern.
        // The tests run at the level the app does.
        targetSdk = 35

        unitTests.isIncludeAndroidResources = true

        // Robolectric rasterises for real here, and a wide window's bitmap does not fit in
        // the half a gigabyte a test JVM is given by default.
        unitTests.all { test -> test.maxHeapSize = "2g" }
    }
}

dependencies {
    implementation(project(":core:scope"))
    implementation(project(":core:solves"))
    implementation(project(":core:ui"))

    // `api`: the table and the DAO are in the signatures Dagger generates for this module.
    api(project(":core:database"))

    // `api`, not `implementation`: this module declares its own destination in the navigation
    // graph, so Dagger's generated factories and the navigation types are part of what it hands
    // out rather than what it keeps.
    api(libs.dagger)
    api(libs.javax.inject)
    api(libs.navigation.common)
    implementation(libs.hilt.android)
    implementation(libs.hilt.core)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.saveable)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text)
    implementation(libs.compose.ui.unit)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.annotations)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.geometry)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.activity)
    testImplementation(libs.androidx.annotation)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.sqlite)
    testImplementation(libs.room.common)
    testImplementation(libs.room.runtime)
    testImplementation(libs.room.ktx)
    kspTest(libs.room.compiler)
    debugRuntimeOnly(libs.compose.ui.test.manifest)
}
