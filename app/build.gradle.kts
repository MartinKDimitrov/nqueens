import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The Android application: Compose UI, navigation, and the screens. The game itself is in
// :core:domain; what stays here is what belongs to a screen rather than to the puzzle.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    jacoco
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.mdimitrov.nqueens"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mdimitrov.nqueens"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // Robolectric runs the composables in the JVM, so the unit tests need the real resources.
    testOptions {
        unitTests.isIncludeAndroidResources = true

        unitTests.all { test ->
            test.enabled = test.name.contains("Debug")
        }
    }

    // Kotlin warnings already fail the build; Android's should too, or the project holds its
    // own code to one standard and its resources and manifest to another. The exceptions are
    // the version advisories: they turn a green build red because somebody else published a
    // release, and upgrading is a decision to take deliberately, not one to be forced into by
    // a commit.
    //
    // The tests are analyzed too, so a resource-type mistake in a test fails the build like one
    // in the app. It costs about five seconds.
    lint {
        warningsAsErrors = true
        abortOnError = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
        checkTestSources = true
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:domain"))

    // Every artifact the code needs is declared, so `buildHealth` can hold the build file to
    // what is really used. Five of them — animation, coroutines, fragment, ui-geometry and the
    // AndroidX test runner — no source file imports: they are needed by what Compose, Hilt and
    // the test harness generate. The BOM keeps the versions in step.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.unit)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.navigation.common)
    implementation(libs.navigation.compose)
    implementation(libs.navigation.runtime)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectric.annotations)
    testImplementation(libs.androidx.test.junit)
    debugRuntimeOnly(libs.compose.ui.test.manifest)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.dagger)
    implementation(libs.hilt.core)
    implementation(libs.javax.inject)
    testImplementation(libs.compose.ui.geometry)
    implementation(libs.androidx.annotation)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Coverage is gated where the screens keep their decisions — the view models. The screens
// themselves are not measured at all: code Robolectric executes is invisible to JaCoCo, so a
// number for them would read as zero however many tests ran. They are checked by running them.
private val viewModelClasses =
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        include("**/*ViewModel*.class")
    }

// The exact file, not a search of the build directory: scanning it makes Gradle believe this
// task consumes the output of every other one.
private val unitTestExecutionData =
    layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")

val viewModelCoverageReport =
    tasks.register<JacocoReport>("viewModelCoverageReport") {
        dependsOn("testDebugUnitTest")
        executionData.setFrom(unitTestExecutionData)
        classDirectories.setFrom(viewModelClasses)
        sourceDirectories.setFrom(files("src/main/kotlin"))
        reports {
            xml.required.set(true)
            csv.required.set(true)
            html.required.set(true)
        }
    }

// Both of this gate's inputs are AGP output paths. If either goes missing — a plugin upgrade,
// a renamed class — JaCoCo has nothing to measure and reports success, or Gradle skips the task
// outright because `executionData` is annotated `@SkipWhenEmpty`. This runs first and refuses.
val viewModelCoverageInputs =
    tasks.register("viewModelCoverageInputs") {
        dependsOn("testDebugUnitTest")
        doLast {
            check(!viewModelClasses.isEmpty) {
                "No view model classes to measure — the coverage gate would pass without " +
                    "checking anything. Look at the class directory this task reads."
            }
            check(unitTestExecutionData.get().asFile.exists()) {
                "No unit-test coverage data at ${unitTestExecutionData.get().asFile} — the " +
                    "coverage gate would be skipped rather than fail."
            }
        }
    }

val viewModelCoverageVerification =
    tasks.register<JacocoCoverageVerification>("viewModelCoverageVerification") {
        dependsOn(viewModelCoverageReport, viewModelCoverageInputs)
        executionData.setFrom(unitTestExecutionData)
        classDirectories.setFrom(viewModelClasses)
        sourceDirectories.setFrom(files("src/main/kotlin"))

        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.85".toBigDecimal()
                }
            }
        }
    }

tasks.named("check") {
    dependsOn(viewModelCoverageInputs, viewModelCoverageVerification)
}
