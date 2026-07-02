plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.folio.benchmark.macrobenchmark"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.1")
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
}
