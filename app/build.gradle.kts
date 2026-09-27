plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

import java.util.Properties

android {
    namespace = "com.legionforge.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.legionforge.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 62
        versionName = "0.9.7"

        // Token fin-grained GitHub (issues:write, repo legion-forge-ci seul). Fichier gitignore (app/github_token.properties), absent du VCS.
        val ghTokenProp = Properties().apply {
            val f = file("github_token.properties")
            if (f.exists()) load(f.inputStream())
        }
        val ghToken = ghTokenProp.getProperty("legionforge_github_token") ?: ""
        buildConfigField("String", "GITHUB_TOKEN", "\"${ghToken.replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
