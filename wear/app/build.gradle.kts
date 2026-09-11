import java.util.Properties

// local.properties no se versiona: cada PC define ahí la URL por defecto de la API.
val localProperties = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiUrlPorDefecto: String = localProperties.getProperty("api.url.defecto")
    ?: error("Falta api.url.defecto en wear/local.properties, por ejemplo: api.url.defecto=http://<ip-del-servidor>:3000")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.luis.fierros"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.luis.fierros"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Punto de partida; si se cambia desde Ajustes -> Servidor, manda la guardada en el reloj.
        buildConfigField("String", "API_URL_POR_DEFECTO", "\"$apiUrlPorDefecto\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling)
    implementation(libs.core.splashscreen)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.wearable)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
}