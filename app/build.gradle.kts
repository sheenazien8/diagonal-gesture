import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val gitVersion: List<Int> by lazy {
    try {
        val tag = providers.exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
        }.standardOutput.asText.get()
            .trim()
            .removePrefix("v")
            .removePrefix("V")
        
        if (tag.isEmpty()) listOf(0, 0, 1)
        else {
            val parts = tag.split(".")
            listOf(
                parts.getOrNull(0)?.toIntOrNull() ?: 0,
                parts.getOrNull(1)?.split("-")?.first()?.toIntOrNull() ?: 0,
                parts.getOrNull(2)?.split("-")?.first()?.toIntOrNull() ?: 0
            )
        }
    } catch (e: Exception) {
        listOf(0, 0, 1)
    }
}

val versionMajor = gitVersion[0]
val versionMinor = gitVersion[1]
val versionPatch = gitVersion[2]

android {
    namespace = "com.sheenadev.diagonalgesture"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sheenadev.diagonalgesture"
        minSdk = 24
        targetSdk = 35
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}