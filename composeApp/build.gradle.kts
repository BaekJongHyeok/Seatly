import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "kr.jiyeok.seatly.composeApp"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

kotlin {
    androidTarget()
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "seatly"
        browser {
            commonWebpackConfig {
                outputFileName = "seatly.js"
            }
        }
        binaries.executable()
    }
    
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // Common compiler options
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            
            // Navigation
            implementation(libs.navigation.compose)
            
            // Koin DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            
            // ViewModel
            implementation(libs.lifecycle.viewmodel.compose)
            
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            
            // Networking (Ktor for multiplatform)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            
            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings)
            implementation(libs.kotlinx.datetime)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.google.maps)
            implementation(libs.maps.compose)
        }
        
        wasmJsMain.dependencies {
            // Ktor client for JS/Wasm
        }
    }
}
