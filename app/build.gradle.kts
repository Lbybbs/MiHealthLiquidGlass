plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mihealth.liquidglass"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.mihealth.liquidglass"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use debug signing so the module is directly installable.
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

    // Merge the libxposed metadata (java_init.list / module.prop / scope.list)
    // into the final APK as-is.
    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }
}

dependencies {
    // Provided by the framework at runtime; required to compile the module.
    compileOnly("io.github.libxposed:api:102.0.0")

    // Runtime support used by the module (prefs / config).
    implementation("io.github.libxposed:service:102.0.0")

    // Liquid-glass renderer for the Android View system.
    implementation("com.github.QWEA0:liquidglass:v2.0.2")
}
