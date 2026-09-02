// What the player has chosen and the app must remember between runs. One preference today — which
// palette to draw in — and the file it lives in, which is not a database and has no table.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.mdimitrov.puzzles.settings"

    testOptions {
        // A preference file is a real file, so what reads and writes it is tested under
        // Robolectric at the level the app ships at.
        targetSdk = 35
    }
}

kotlin {
    // What a screen may see is stated rather than inferred: the choice and the two verbs.
    explicitApi()
}

dependencies {
    // `api` only for what a screen names: the flow of the choice. The store itself is this
    // module's own business — `implementation`, so no consumer can reach past `Themes` to it.
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences.core)
    api(libs.dagger)
    api(libs.javax.inject)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(libs.kotlinx.coroutines.test)
}
