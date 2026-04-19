plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.jskrzypczak.androidlab.core_test"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md"
            )
        }
    }
}

dependencies {
    // Zależności core-test są dostępne dla innych modułów przez api()
    // żeby nie musieć ich deklarować w każdym feature module osobno
    api(libs.junit4)
    api(libs.junit5.api)
    api(libs.junit5.params)
    api(libs.junit5.engine)
    api(libs.mockk)
    api(libs.turbine)
    api(libs.coroutines.test)
    api(libs.koin.test)
}