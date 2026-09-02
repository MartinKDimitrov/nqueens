// Every table this app stores, and the one database they live in. Room needs the entities of a
// database declared together, so they are here rather than in the features that read them; what
// a feature is given is the accessor for its own table.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.mdimitrov.puzzles.database"

    // Room opens a real SQLite, so the tables and what opens them are tested under Robolectric.
    testOptions {
        // A library declares no target, and Robolectric then runs at a level of its choosing
        // rather than the app's. Pinning it keeps a module's tests honest about the platform
        // the code actually meets.
        targetSdk = 35

        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    // What a feature may see is stated rather than inferred: its table, its DAO, and nothing else.
    explicitApi()
}

// Room writes Kotlin rather than Java, so the database and its modules stay `internal`:
// generated Java cannot call the mangled names Kotlin gives them.
ksp {
    arg("room.generateKotlin", "true")
    // The schema is checked in, so the first migration has something to be written against.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // `api`, not `implementation`: a DAO's queries answer with Room's types and Kotlin's flows,
    // both of which are in the signatures a feature calls.
    api(libs.room.runtime)
    implementation(libs.room.common)
    api(libs.kotlinx.coroutines.core)
    api(libs.dagger)
    api(libs.javax.inject)
    implementation(libs.androidx.sqlite)
    implementation(libs.room.ktx)
    implementation(libs.hilt.android)
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(libs.kotlinx.coroutines.test)
}
