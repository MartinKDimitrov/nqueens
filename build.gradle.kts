// Root build: wires the quality gate that `check` runs across every module.
// Language/Android plugins are declared here (apply false) and applied per module.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    // Declared here, applied to the modules below: on the root alone it analyses nothing.
    alias(libs.plugins.dependency.analysis)
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

// Detekt static analysis on the code modules, built upon its default ruleset.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "com.autonomousapps.dependency-analysis")
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
    }
}
