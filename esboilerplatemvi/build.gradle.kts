plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish") version "0.36.0"
    alias(libs.plugins.compose.compiler)
}

mavenPublishing {
    coordinates("io.github.esei1541", "esboilerplate-mvi", "0.0.1")

    pom {
        name.set("esboilerplate-mvi")
        description.set("A boilerplate library for Android MVI architecture with Jetpack Compose.")
        inceptionYear.set("2026")
        url.set("https://github.com/Esei1541/EsBoilerplate-MVI")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("Esei1541")
                name.set("Seungjin Choi")
                email.set("esei.public@gmail.com")
                url.set("https://github.com/Esei1541")
            }
        }
        scm {
            url.set("https://github.com/Esei1541/EsBoilerplate-MVI")
            connection.set("scm:git:github.com/Esei1541/EsBoilerplate-MVI.git")
            developerConnection.set("scm:git:ssh://github.com/Esei1541/EsBoilerplate-MVI.git")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}

android {
    namespace = "me.esei.esboilerplatemvi"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.navigation.compose)
}