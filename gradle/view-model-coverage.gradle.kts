import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

// Coverage is gated where the screens keep their decisions — the view models. The screens
// themselves are not measured at all: code Robolectric executes is invisible to JaCoCo, so a
// number for them would read as zero however many tests ran. They are checked by running them.
//
// This is applied by every module that holds a view model, so the floor is one rule rather than
// one rule per module drifting from the others.
apply(plugin = "jacoco")

val viewModelClasses =
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        include("**/*ViewModel*.class")
    }

// The exact file, not a search of the build directory: scanning it makes Gradle believe this
// task consumes the output of every other one.
val unitTestExecutionData = layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")

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
                "No view model classes to measure in ${project.path} — the coverage gate would " +
                    "pass without checking anything. Look at the class directory this task reads."
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
