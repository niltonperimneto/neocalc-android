plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.neocalc.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
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
            isMinifyEnabled = true
            isShrinkResources = true
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

    // Build separate APKs for each ABI (smaller downloads)
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true  // Also build a universal APK
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
    workingDir = file("../../mobile_backend")
    commandLine(cargoPath, "build", "--lib")
}

// Task to generate Kotlin bindings, depends on buildRustLib
tasks.register<Exec>("generateBindings") {
    dependsOn("buildRustLib")
    workingDir = file("../../mobile_backend")
    commandLine(
        cargoPath, "run", "--bin", "uniffi-bindgen", "generate",
        "--library", "../target/debug/libneocalc_backend.so",
        "--language", "kotlin",
        "--out-dir", "${project.projectDir}/src/main/java/com/neocalc/app/core"
    )
}

// Task to build native libs using cargo-ndk
tasks.register<Exec>("buildNativeLibs") {
    workingDir = file("../../mobile_backend")
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
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // material-icons-extended is no longer managed by the Compose BOM; 1.7.8 is its final version
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-svg:3.5.0")
    testImplementation("junit:junit:4.13.2")
    implementation("net.java.dev.jna:jna:5.19.1@aar")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
