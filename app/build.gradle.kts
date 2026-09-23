plugins {
    id("com.android.application")
}

android {
    namespace = "com.maximumspyware.parody"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.maximumspyware.parody"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1"

    }

    signingConfigs {
        getByName("debug") {
            // Keep Gradle's generated keystore out of the protected user home.
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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

}

dependencies {
}
