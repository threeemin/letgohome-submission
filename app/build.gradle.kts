import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localSettings = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
fun appSetting(name: String): String =
    providers.gradleProperty(name).orNull ?: localSettings.getProperty(name).orEmpty()

fun buildStringLiteral(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.meraki.letgohome"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.meraki.letgohome"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "KAKAO_MAP_NATIVE_APP_KEY", buildStringLiteral(appSetting("KAKAO_MAP_NATIVE_APP_KEY").trim()))
        buildConfigField("String", "ROUTE_PROXY_URL", buildStringLiteral(appSetting("ROUTE_PROXY_URL").trim()))
    }

    buildTypes {
        debug {
            val proxy = appSetting("ROUTE_PROXY_URL").trim().ifBlank { "http://localhost:8765/route" }
            buildConfigField("String", "ROUTE_PROXY_URL", buildStringLiteral(proxy))
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.kakao.maps.open:android:2.15.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
