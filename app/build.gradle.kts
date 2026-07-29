import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

android {
    namespace = "tw.taipei.veges"
    compileSdk = 36

    defaultConfig {
        applicationId = "tw.taipei.veges"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        androidResources.localeFilters += "zh-rTW"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
}

kapt { correctErrorTypes = true }

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":design-system"))
    implementation(project(":home"))
    implementation(project(":catalog"))
    implementation(project(":detail"))
    implementation(project(":alerts"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.material)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(project(":domain"))
    androidTestImplementation(platform(libs.compose.bom))
}

tasks.register("validateReleaseArtifact") {
    group = "verification"
    description = "Builds and validates the non-empty release APK artifact."
    dependsOn("assembleRelease")
    notCompatibleWithConfigurationCache("The task inspects the generated APK directly at execution time.")

    doLast {
        val artifacts = layout.buildDirectory.dir("outputs/apk/release").get().asFile
            .listFiles { file -> file.extension == "apk" }
            .orEmpty()
        check(artifacts.size == 1 && artifacts.single().length() > 0) {
            "Expected exactly one non-empty release APK in ${artifacts.firstOrNull()?.parent}"
        }
    }
}
