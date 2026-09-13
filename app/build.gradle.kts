import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.serialization)
    id("net.ltgt.errorprone")
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

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    errorprone("com.google.errorprone:error_prone_core:2.31.0")

}

/**
 * The ErrorProne code was vibecoded using AI - Carlos
 *
 * it is going through Androids configurations that end in AnnotationProcessor
 * All these configurations ending in AnnotationProcessor are what Andrioid uses to complile
 * We are then saying to inherit the Error Prone method
 *
 * in the second line we are saying to find every task that compiles java and then
 * enable error prone. We are also saying to disable warnings generated from source files.
 * Finally we have our if conditon which is asking if we are compiling tests
 */
configurations.matching { it.name.endsWith("AnnotationProcessor") }.configureEach { extendsFrom(configurations.errorprone.get()) }
tasks.withType<JavaCompile>().configureEach {
    options.errorprone.enabled = true
    options.errorprone.disableWarningsInGeneratedCode = true

    if (name.contains("Test", ignoreCase = true)) {
        options.errorprone.compilingTestOnlyCode = true
    }
}