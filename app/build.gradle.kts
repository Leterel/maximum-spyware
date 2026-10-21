plugins {
    id("com.android.application")
}
val adsTest = providers.gradleProperty("adsTest").map { it.toBooleanStrict() }.getOrElse(false)

android {
    namespace = "com.maximumspyware.parody"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.maximumspyware.parody"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.2.2"
        if (adsTest) { applicationIdSuffix = ".adstest"; versionNameSuffix = "-adstest" }

    }
    sourceSets.getByName("main").java.srcDir(if (adsTest) "src/adsTest/java" else "src/offline/java")
    if (adsTest) {
        sourceSets.getByName("debug").manifest.srcFile("src/adsTest/AndroidManifest.xml")
        sourceSets.getByName("release").manifest.srcFile("src/adsTest/AndroidManifest.xml")
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
    if (adsTest) {
        implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0")
        implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    }
}
