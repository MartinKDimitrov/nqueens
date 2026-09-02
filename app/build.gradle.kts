// What is left of the application once the screens, the tables and the puzzle are modules of
// their own: the navigation between them, and the games this build was assembled with — which
// it knows only as a set it was handed.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.mdimitrov.puzzles"

    defaultConfig {
        applicationId = "com.mdimitrov.puzzles"
        versionCode = 1
        versionName = "0.1"
        // The real application, graph and all: what is under test on a device is what the app
        // draws and what it plays, and neither is worth replacing a binding for.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    // Robolectric runs the composables in the JVM, so the unit tests need the real resources.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":features:play"))
    implementation(project(":core:puzzletype"))
    implementation(project(":core:settings"))
    implementation(project(":core:solves"))
    implementation(project(":core:ui"))
    implementation(project(":features:scores"))
    implementation(project(":features:setup"))
    implementation(project(":games:nqueens"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.dagger)
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
    // The activity dresses the window and the system's bars in the palette the player chose, and
    // asks the phone what it prefers when they have not, so Compose's foundation is named here
    // rather than inherited.
    implementation(libs.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.navigation.common)
    implementation(libs.navigation.compose)
    implementation(libs.navigation.runtime)

    implementation(platform(libs.compose.bom))
    implementation(libs.hilt.android)
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.annotations)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    debugRuntimeOnly(libs.compose.ui.test.manifest)

    // On a device, and only for what a workstation cannot see: whether the platform's decoder
    // takes the sounds, and what the system draws around the app. `make check` does not run
    // these — they need hardware — and `make check-device` does. The shell's `R` class comes with
    // the tested variant's own classpath; naming the module again here would put a second copy of
    // it on the compile classpath. Nothing here replaces a binding, so the application these run
    // in is the app's own and the palette is set through the graph it already built.
    androidTestImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestRuntimeOnly(libs.androidx.test.runner)
}
