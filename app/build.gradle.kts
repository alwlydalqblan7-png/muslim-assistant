plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.taifdigital.muslimassistant"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.taifdigital.muslimassistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.0"
    }
    val suppliedKeystore = System.getenv("MUSLIM_SIGNING_STORE")
    if (!suppliedKeystore.isNullOrBlank()) {
        val existingKey = signingConfigs.create("existingKey") {
            storeFile = file(suppliedKeystore)
            storePassword = requireNotNull(System.getenv("MUSLIM_SIGNING_STORE_PASSWORD"))
            keyAlias = requireNotNull(System.getenv("MUSLIM_SIGNING_ALIAS"))
            keyPassword = requireNotNull(System.getenv("MUSLIM_SIGNING_KEY_PASSWORD"))
        }
        buildTypes.getByName("debug").signingConfig = existingKey
        buildTypes.getByName("release").signingConfig = existingKey
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.core:core:1.15.0")
}
