import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// How this app opens a database, and nothing else: no table, no query, no name of its own.
// A feature brings its own tables and asks here for the connection.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.mdimitrov.nqueens.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Room opens a real SQLite, so what opens it is tested under Robolectric in the JVM.
    testOptions {
        unitTests.isIncludeAndroidResources = true

        unitTests.all { test ->
            test.enabled = test.name.contains("Debug")
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
        checkTestSources = true
    }
}

kotlin {
    // What a feature may see is stated rather than inferred.
    explicitApi()
    compilerOptions {
        allWarningsAsErrors.set(true)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // `api`, not `implementation`: Room's database type is in the signature a feature calls, and
    // Dagger's generated factories are part of what this module hands the component.
    api(libs.room.runtime)
    api(libs.dagger)
    api(libs.javax.inject)
    implementation(libs.hilt.android)
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    // The tests declare a table of their own, so the connection is exercised without a feature.
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.shadows.framework)
    testImplementation(libs.androidx.sqlite)
    testImplementation(libs.room.common)
    testImplementation(libs.room.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    kspTest(libs.room.compiler)
}
