// What a game says about a board it has finished, and nothing else. No table, no screen, no
// Android: the two features that meet here depend on this and never on each other.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
