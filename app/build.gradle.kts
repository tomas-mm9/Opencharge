plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gitCommitCount: String = runCatching {
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
        .standardOutput.asText.get().trim()
}.getOrDefault("1")

val gitShortSha: String = runCatching {
    providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }
        .standardOutput.asText.get().trim()
}.getOrDefault("dev")

android {
    namespace = "com.tomasmm.opencharge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tomasmm.opencharge"
        minSdk = 33
        targetSdk = 36
        versionCode = gitCommitCount.toIntOrNull() ?: 1
        versionName = "1.0-$gitShortSha"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
}
