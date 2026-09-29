plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun quotedBuildConfig(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val firebaseApiKey = System.getenv("FIREBASE_API_KEY") ?: ""
val firebaseDatabaseUrl = System.getenv("FIREBASE_DATABASE_URL") ?: ""

val signingStorePath = System.getenv("ANDROID_KEYSTORE_PATH") ?: ""
val signingStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""
val signingKeyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: ""
val signingKeyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: ""
val hasCiSigning = listOf(signingStorePath, signingStorePassword, signingKeyAlias, signingKeyPassword).all { it.isNotBlank() }

android {
    namespace = "com.example.mylifetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mylifetracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        buildConfigField("String", "FIREBASE_API_KEY", quotedBuildConfig(firebaseApiKey))
        buildConfigField("String", "FIREBASE_DATABASE_URL", quotedBuildConfig(firebaseDatabaseUrl))
    }

    signingConfigs {
        if (hasCiSigning) {
            create("ci") {
                storeFile = file(signingStorePath)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasCiSigning) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
