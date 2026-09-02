// Root build: wires the quality gate that `check` runs across every module.
// Language/Android plugins are declared here (apply false) and applied per module.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    // Declared here, applied to the modules below: on the root alone it analyses nothing.
    alias(libs.plugins.dependency.analysis)

    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Spotless (ktlint) formats every module and the Gradle scripts themselves.
allprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint()
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }
}

// The documents live at the root, so the root formats them — inside `allprojects` every module
// would claim the same files. Nothing here reflows them: flexmark centres table headings and
// drops the indent under a list item, which reads worse in plain text than what it replaces.
// This catches the drift instead.
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    format("markdown") {
        // Named rather than swept: `**/*.md` walks every module's `build/` tree to find six files,
        // and dies on anything a parallel task removes underneath it — a gate that goes red for a
        // reason that has nothing to do with the commit. `targetExclude` filters the result, which
        // is too late.
        target("*.md", "docs/**/*.md")
        targetExclude("**/*.local.md")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// How every Android module is built. Only what actually differs between them is left in the
// module itself: its namespace, its resource prefix, whether it draws, and what its tests need.
// This is a floor, not a lock — a module that must differ can still say so and win.
val sdk = 35
val minimumSdk = 24

/**
 * Android's own warnings fail the build like Kotlin's, or the project holds its code to one
 * standard and its resources and manifest to another.
 */
fun com.android.build.api.dsl.Lint.gate() {
    warningsAsErrors = true
    abortOnError = true
    // The exceptions are the version advisories: they turn a green build red because somebody
    // else published a release, and upgrading is a decision to take deliberately rather than one
    // to be forced into by a commit.
    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
    // The tests are analyzed too, so a resource-type mistake in a test fails the build like one
    // in the app. It costs about five seconds.
    checkTestSources = true
}

subprojects {
    plugins.withId("com.android.library") {
        configure<com.android.build.gradle.LibraryExtension> {
            compileSdk = sdk
            defaultConfig { minSdk = minimumSdk }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            // A library builds both variants and the tests would run twice over the same code.
            testOptions.unitTests.all { test -> test.enabled = test.name.contains("Debug") }
            lint { gate() }
        }
    }

    // A JVM module gets no lint from either Android plugin, and one of them declares the
    // resource annotations every game module is checked against. `com.android.lint` is that
    // check without the rest of AGP.
    plugins.withId("com.android.lint") {
        configure<com.android.build.api.dsl.Lint> { gate() }
    }

    plugins.withId("com.android.application") {
        configure<com.android.build.gradle.internal.dsl.BaseAppModuleExtension> {
            compileSdk = sdk
            defaultConfig {
                minSdk = minimumSdk
                targetSdk = sdk
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            testOptions.unitTests.all { test -> test.enabled = test.name.contains("Debug") }
            lint { gate() }
        }
    }

    // Every module that composes is told which of the domain's types hold still. Said once here
    // rather than per module, because the answer is the same everywhere and a module that
    // silently missed it would draw its whole screen again on every state change.
    plugins.withId("org.jetbrains.kotlin.plugin.compose") {
        configure<org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension> {
            stabilityConfigurationFile.set(rootProject.layout.projectDirectory.file("compose-stability.conf"))
        }
    }

    // A warning left standing is a warning nobody reads, so every module fails on its own.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(true)
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

// Detekt static analysis on the code modules, built upon its default ruleset.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "com.autonomousapps.dependency-analysis")
    // `buildHealth` reports what each module was told about its dependencies and drops what it
    // was told about its own shape: `ProjectAdvice.isEmpty()` does not look at `moduleAdvice`, so
    // a module that should not be an Android module at all fails its own `projectHealth` while
    // the aggregate returns zero. The gate asks each module directly instead.
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(tasks.matching { health -> health.name == "projectHealth" })
    }
    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    }
}

dependencyAnalysis {
    issues {
        all {
            onAny { severity("fail") }
        }

        // `:core:ui` carries the theme, and Compose Material3 is Android. The advice reads
        // "could be a JVM project" because the module has no resources of its own; it cannot be.
        project(":core:ui") {
            onModuleStructure { severity("ignore") }
        }
    }
}
