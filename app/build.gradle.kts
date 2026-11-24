import com.google.protobuf.gradle.id

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
}

android {
    namespace = "com.proyectos.tvremote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.proyectos.tvremote"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/**"
            excludes += "/META-INF/*.RSA"
            excludes += "/META-INF/*.SF"
            excludes += "/META-INF/*.DSA"
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.activity:activity-ktx:1.9.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.protobuf:protobuf-javalite:3.25.3")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // X.509 self-signed cert generation for Android TV Remote v2 pairing (no stock Android API for this)
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    // Pure Kotlin/JVM ADB client (no PC, no root) - control for generic TV boxes that only
    // expose wireless debugging, not SSAP or Android TV Remote v2. Published as dev.mobile:dadb
    // (renamed from com.malinskiy:dadb upstream).
    implementation("dev.mobile:dadb:1.2.9") {
        // Pulls in a GraalVM native-image JUnit5 test harness as a transitive runtime dep -
        // irrelevant on Android and its jars collide on META-INF/LICENSE.md etc.
        exclude(group = "org.graalvm.buildtools", module = "junit-platform-native")
    }

    debugImplementation("androidx.compose.ui:ui-tooling")
}
