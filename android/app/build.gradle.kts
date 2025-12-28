plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")

}

android {
    namespace = "com.neocalc.app"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.neocalc.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = project.findProperty("appVersion") as? String ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Read from Environment Variables (set by GitHub Secrets)
            storeFile = file("keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Cargo build moved to manual task due to plugin incompatibility
/*
cargo {
    module = "../../backend"       // Path to Rust crate
    libname = "neocalc_backend"    // Library name
    targets = listOf("arm", "arm64", "x86", "x86_64")
    apiLevel = 26
}
*/

// Helper task to generate kotlin bindings via uniffi-bindgen
// Define cargo path, falling back to standard install location if not in PATH
val cargoPath = System.getenv("CARGO_PATH") ?: "${System.getProperty("user.home")}/.cargo/bin/cargo"

// Task to build the Rust library
tasks.register<Exec>("buildRustLib") {
    workingDir = file("../../backend")
    commandLine(cargoPath, "build", "--lib")
}

// Task to generate Kotlin bindings, depends on buildRustLib
tasks.register<Exec>("generateBindings") {
    dependsOn("buildRustLib")
    workingDir = file("../../backend")
    commandLine(
        cargoPath, "run", "--bin", "uniffi-bindgen", "generate",
        "--library", "../target/debug/libneocalc_backend.so",
        "--language", "kotlin",
        "--out-dir", "${project.projectDir}/src/main/java/com/neocalc/app/core"
    )
}

// Task to build native libs using cargo-ndk
tasks.register<Exec>("buildNativeLibs") {
    workingDir = file("../../backend")
    // Use cargo-ndk to build for targets and output to jniLibs
    // Ensure cargo-ndk is installed: cargo install cargo-ndk
    commandLine(
        cargoPath, "ndk",
        "-t", "armeabi-v7a",
        "-t", "arm64-v8a",
        "-t", "x86",
        "-t", "x86_64",
        "-o", "../android/app/src/main/jniLibs",
        "build", "--release"
    )
    environment("PATH", System.getenv("PATH") + ":${System.getProperty("user.home")}/.cargo/bin")
}

// Ensure bindings are generated before preBuild
tasks.named("preBuild") {
    dependsOn("generateBindings")
    dependsOn("buildNativeLibs")
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-svg:2.5.0")
    testImplementation("junit:junit:4.13.2")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
