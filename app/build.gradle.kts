plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.errorprone)
    id("pmd")
}

android {
    namespace = "com.example.project1"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.project1"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures{
        compose = true
    }
}

pmd {
    toolVersion = libs.versions.pmd.get()
    isIgnoreFailures = false
    ruleSetFiles = files("$rootDir/config/pmd/kotlin-ruleset.xml")
    // Clear the default Java rulesets — they do not apply to Kotlin sources.
    ruleSets = emptyList()
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    ignoreFailures = false
}

room {
    schemaDirectory("$projectDir/schemas")
}

// The Room Gradle plugin only attaches its schema-location argument to KSP1 tasks.
// Under KSP2 (KspAATask) it attaches nothing, so Room silently stops exporting the
// schema. Set the option directly until Room ships KSP2 support.
// See https://issuetracker.google.com/issues/379159770
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // The Kotlin language module must be on PMD's tool classpath or .kt files are skipped.
    pmd("net.sourceforge.pmd:pmd-kotlin:${libs.versions.pmd.get()}")
    pmd("net.sourceforge.pmd:pmd-ant:${libs.versions.pmd.get()}")

    // Error Prone is a javac plugin. This module has 0 Java files, so compileDebugJavaWithJavac
    // is NO-SOURCE and Error Prone never executes. Wired because the assignment names it; the
    // empty result is documented in the triage report.
    // Note: error_prone_core 2.50 requires JDK 21+ (JBR 25 satisfies this), and the plugin
    // automatically forks the compiler with the --add-exports/--add-opens that JDK 16+ needs.
    errorprone(libs.errorprone.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.navigation.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register<Pmd>("pmdKotlin") {
    group = "verification"
    description = "Runs PMD's Kotlin rules over app/src/main/java (**/*.kt)."
    // `source` exposes only a getter on SourceTask; use setSource() in the Kotlin DSL.
    setSource(fileTree("src/main/java"))
    include("**/*.kt")
    exclude("**/build/**")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Gradle's pmd plugin does not expose CPD at all, so copy-paste detection is run
// directly via the PMD CLI distribution instead of a plugin-provided task.
val pmdCli: Configuration by configurations.creating

dependencies {
    pmdCli("net.sourceforge.pmd:pmd-dist:${libs.versions.pmd.get()}")
}

// CPD finds duplicated token sequences. Threshold chosen from a measured curve:
// 40 -> 10 findings, 50 -> 8, 75 -> 7, 100 -> 6, 150 -> 1. 50 catches the real screen
// duplication without flagging boilerplate.
tasks.register<JavaExec>("cpdKotlin") {
    group = "verification"
    description = "Runs CPD copy-paste detection over the Kotlin sources."
    classpath = pmdCli
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    args = listOf(
        "cpd",
        "--dir", "$projectDir/src/main/java",
        "--language", "kotlin",
        "--minimum-tokens", "50",
        "--format", "text",
    )
}

// `check` already depends on lint and testDebugUnitTest via AGP. Adding these makes
// `./gradlew check` the single command the CI workflow runs.
tasks.named("check") {
    dependsOn("detekt", "pmdKotlin", "cpdKotlin")
}
