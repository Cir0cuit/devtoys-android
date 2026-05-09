import os

modules = [
    "core/ui", "core/designsystem", "core/common",
    "tools/base", "tools/encoders", "tools/text",
    "tools/data", "tools/generators", "tools/crypto",
    "tools/developer", "tools/reference"
]

gradle_template = """plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.devtoys.android.%s"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("com.google.dagger:hilt-android:2.51.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}
"""

manifest_template = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
</manifest>
"""

for mod in modules:
    mod = mod.replace("\\", "/")
    ns_suffix = mod.replace("/", ".")
    
    mod_dir = os.path.join("C:/Users/barti/.gemini/antigravity/scratch/devtoys-android", mod)
    os.makedirs(mod_dir, exist_ok=True)
    
    with open(os.path.join(mod_dir, "build.gradle.kts"), "w", encoding="utf-8") as f:
        f.write(gradle_template % ns_suffix.lower())
        
    manifest_dir = os.path.join(mod_dir, "src", "main")
    os.makedirs(manifest_dir, exist_ok=True)
    
    with open(os.path.join(manifest_dir, "AndroidManifest.xml"), "w", encoding="utf-8") as f:
        f.write(manifest_template)

print("Done")
