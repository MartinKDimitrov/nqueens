// What one puzzle is, and nothing about any particular one. A game module provides one of these;
// the screens that draw it depend on this and never on a game.
//
// A JVM module, not an Android one: a `Puzzle` names its words and its glyph by resource id, and
// an id is an `Int`. `androidx.annotation` marks which ints they are and is itself a plain jar,
// so nothing here needs a manifest, a resource merge or an Android classpath.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // Not an Android module, but the one that declares `@StringRes`, `@DrawableRes` and
    // `@PluralsRes`. This runs Android's own checks over it without the rest of AGP.
    alias(libs.plugins.android.lint)
}

kotlin {
    // The contract a game module implements says exactly what it offers.
    explicitApi()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(":core:boardlogic"))
    api(libs.androidx.annotation)
    api(libs.javax.inject)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}
