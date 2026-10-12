plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "com.hdlee73.dailyhabit"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Database: SQLite JDBC
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // HTML Parser for Gospel scraping
    implementation("org.jsoup:jsoup:1.18.3")

    // JSON handling
    implementation("org.json:json:20240303")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.hdlee73.dailyhabit.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "DailyHabit"
            packageVersion = "1.0.0"
            description = "DailyHabit - Habit, Todo & Life Manager for Windows"
            vendor = "hdlee73"
        }
    }
}
