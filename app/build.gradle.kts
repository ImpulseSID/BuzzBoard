import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

android {
    namespace = "com.impulse.buzzboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.impulse.buzzboard"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val newsApiKey: String? = localProperties.getProperty("NEWS_API_KEY")
        val newsDataApiKey: String? = localProperties.getProperty("NEWS_DATA_API_KEY")

        buildConfigField("String", "NEWS_API_KEY", "\"${newsApiKey ?: ""}\"")
        buildConfigField("String", "NEWS_DATA_API_KEY", "\"${newsDataApiKey ?: ""}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit for networking
    implementation(libs.retrofit)
    // Gson converter for Retrofit
    implementation(libs.retrofit.gson)
    // Glide for image loading
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    // RecyclerView for displaying lists
    implementation(libs.androidx.recyclerview)
}