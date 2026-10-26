plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace = "vn.loctm.ltmcampus"; compileSdk = 35
    defaultConfig { applicationId = "vn.loctm.ltmcampus"; minSdk = 26; targetSdk = 35; versionCode = 3; versionName = "0.3.0" }
    val releaseStore = System.getenv("CAMPUS_KEYSTORE")
    if (!releaseStore.isNullOrBlank()) {
        signingConfigs.create("campusRelease") {
            storeFile = file(releaseStore)
            storePassword = System.getenv("CAMPUS_STORE_PASSWORD")
            keyAlias = "campus"
            keyPassword = System.getenv("CAMPUS_KEY_PASSWORD")
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("campusRelease")
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.json:json:20240303")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material-icons-core")
}
