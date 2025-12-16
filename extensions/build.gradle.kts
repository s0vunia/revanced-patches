plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "app.revanced.extension.tiktok"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    compileOnly("androidx.annotation:annotation:1.7.1")
}

// Task to create DEX file for ReVanced integration
tasks.register<Jar>("dexJar") {
    dependsOn("assembleRelease")

    archiveBaseName.set("tiktok-integrations")
    archiveExtension.set("jar")

    from(android.sourceSets["main"].java.srcDirs)

    doLast {
        // The AAR will be created, we'll use d8 to convert to DEX
        println("Extension library built. Use assembleRelease for the AAR.")
    }
}
