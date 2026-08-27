import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure-Kotlin domain: no Android on the classpath, so it stays fast and unit-testable.
plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
}

kotlin {
    // Public API of the domain must be explicit — it is the contract the app depends on.
    explicitApi()
    compilerOptions {
        allWarningsAsErrors.set(true)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    finalizedBy(tasks.jacocoTestReport)
}

// Coverage floor for the domain: the logic lives here, so it is gated hardest.
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit { minimum = "0.90".toBigDecimal() }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
