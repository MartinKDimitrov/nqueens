// What every screen is drawn with: the palette, the type, the measures, and the one format more
// than one feature prints. No screen and no state. Its palette does still name a queen — the
// board's piece colour is `queen` — which is the shell's word for whatever piece it draws.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mdimitrov.puzzles.ui"

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    // Everything here is meant to be used from outside, so everything says so.
    explicitApi()
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.material3)
    api(libs.compose.runtime)
    api(libs.compose.ui.graphics)
    api(libs.compose.ui.text)
    api(libs.compose.ui.unit)
    implementation(libs.compose.foundation)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}
