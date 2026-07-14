import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "io.github.nikitat21.questhomeswitcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.nikitat21.questhomeswitcher"
        minSdk = 29
        targetSdk = 32
        versionCode = 14
        versionName = "1.1"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(keystoreProperties["storeFile"] as String?))
                storePassword = requireNotNull(keystoreProperties["storePassword"] as String?)
                keyAlias = requireNotNull(keystoreProperties["keyAlias"] as String?)
                keyPassword = requireNotNull(keystoreProperties["keyPassword"] as String?)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // This app is sideloaded on Quest's Android 12L base and is not distributed via Google Play.
        disable += "ExpiredTargetSdkVersion"
        // Android Studio can lock migrated lint jars on Windows while generating a signed APK.
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
