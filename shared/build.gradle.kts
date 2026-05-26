plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                // Корутины
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // SQLDelight — общий runtime
                implementation("app.cash.sqldelight:runtime:2.0.1")

                // Ktor — HTTP клиент (OAuth2)
                implementation("io.ktor:ktor-client-core:2.3.9")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")

                // Сериализация
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // Дата и время
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            }
        }

        val androidMain by getting {
            dependencies {
                // SQLDelight Android драйвер
                implementation("app.cash.sqldelight:android-driver:2.0.1")

                // SQLCipher — шифрование базы данных
                implementation("net.zetetic:android-database-sqlcipher:4.5.4")
                implementation("androidx.sqlite:sqlite-ktx:2.4.0")

                // Ktor Android
                implementation("io.ktor:ktor-client-okhttp:2.3.9")

                // JavaMail для Android — IMAP/SMTP
                implementation("com.sun.mail:android-mail:1.6.7")
                implementation("com.sun.mail:android-activation:1.6.7")
            }
        }

        val jvmMain by getting {
            dependencies {
                // SQLDelight JVM драйвер
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")

                // SQLCipher JVM
                implementation("org.xerial:sqlite-jdbc:3.45.1.0")

                // Ktor JVM
                implementation("io.ktor:ktor-client-cio:2.3.9")

                // JavaMail — IMAP/SMTP для Windows через JVM bridge
                implementation("com.sun.mail:jakarta.mail:2.0.1")
            }
        }
    }
}

android {
    namespace = "cleanmail.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("CleanMailDatabase") {
            packageName.set("cleanmail.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
        }
    }
}
