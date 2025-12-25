plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.mozilla.rust-android-gradle.rust-android")
}

android {
    namespace = "com.neocalc.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neocalc.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

cargo {
    module = "../../backend"       // Path to Rust crate
    libname = "neocalc_backend"    // Library name
    targets = listOf("arm", "arm64", "x86", "x86_64")
    apiLevel = 26
}

// Helper task to generate kotlin bindings via uniffi-bindgen
// In a real setup, you might want more robust task dependencies.
// This assumes 'uniffi-bindgen' is installed or run via cargo run.

tasks.register("generateBindings") {
    doLast {
        exec {
            workingDir = file("../../backend")
            commandLine("cargo", "run", "--bin", "uniffi-bindgen", "generate", "--library", "../target/debug/libneocalc_backend.so", "--language", "kotlin", "--out-dir", "${project.projectDir}/src/main/java/com/neocalc/app/core")
        }
    }
}

// Make preBuild depend on generating bindings if you want auto-generation
// preBuild.dependsOn("generateBindings")

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
}
