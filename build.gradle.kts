import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

/** 버전은 VERSION 파일 하나로 관리 (안드로이드 앱과 같은 번호) */
val appVersion: String = System.getenv("VERSION_NAME") ?: file("VERSION").readText().trim()

/** 판본: -Pedition=catholic (천주교인용: 복음·기도) / general (일반용: 오늘의 명언, 기본값) */
val edition: String = (findProperty("edition") as String?) ?: "general"
val catholic = edition == "catholic"

/** Windows ARM64 PC(Surface Pro X·Snapdragon 노트북 등)에서 빌드하면 ARM64용 설치 파일이 만들어진다.
 *  Compose 1.7.3의 currentOs에는 Windows ARM64가 없어서, 공통 모듈 + 같은 skiko 버전의 ARM64 런타임을 직접 넣는다. */
val windowsArm64 = System.getProperty("os.name").startsWith("Windows") &&
    System.getProperty("os.arch").lowercase().let { it == "aarch64" || it == "arm64" }

group = "com.hdlee73.dailyhabit"
version = appVersion

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Compose Desktop
    if (windowsArm64) {
        implementation(compose.desktop.common)
        implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-arm64:0.8.18")
    } else {
        implementation(compose.desktop.currentOs)
    }
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

    // 휴대폰과 같은 와이파이 동기화: PC 찾기(mDNS)와 짝 맺기 QR 코드
    implementation("org.jmdns:jmdns:3.5.9")
    implementation("org.slf4j:slf4j-nop:2.0.16")
    implementation("com.google.zxing:core:3.5.3")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

// 판본·버전을 코드에서 쓰도록 BuildConfig 생성 (안드로이드의 BuildConfig와 같은 이름)
val generateBuildConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/buildconfig")
    val build = System.getenv("GITHUB_RUN_NUMBER") ?: "0"
    val appName = if (catholic) "DailyHabit 천주교인용" else "DailyHabit"
    inputs.property("version", appVersion)
    inputs.property("edition", edition)
    inputs.property("build", build)
    outputs.dir(outDir)
    doLast {
        val f = outDir.get().file("com/hdlee73/dailyhabit/BuildConfig.kt").asFile
        f.parentFile.mkdirs()
        f.writeText(
            """
            |package com.hdlee73.dailyhabit
            |
            |object BuildConfig {
            |    const val VERSION_NAME = "$appVersion"
            |    const val VERSION_CODE = $build
            |    const val CATHOLIC = $catholic
            |    const val APP_NAME = "$appName"
            |}
            |""".trimMargin(),
            Charsets.UTF_8,
        )
    }
}

kotlin {
    sourceSets.main { kotlin.srcDir(generateBuildConfig) }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("dailyhabit.dataDir", layout.buildDirectory.dir("test-data").get().asFile.absolutePath)
    // -Pscreens=<폴더> 이면 각 화면을 PNG로 그려 저장 (CI에서 디자인 확인용)
    (findProperty("screens") as String?)?.let {
        systemProperty("dailyhabit.screensDir", it)
        outputs.upToDateWhen { false }
    }
}

compose.desktop {
    application {
        mainClass = "com.hdlee73.dailyhabit.MainKt"
        jvmArgs += listOf("-Dfile.encoding=UTF-8")

        nativeDistributions {
            // .msi (자동 업데이트에 쓰는 설치 파일) + .exe (같은 설치 프로그램의 실행 파일판, 같은 업그레이드 코드)
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            // 판본마다 따로 설치되는 별개의 앱 (안드로이드와 같음)
            packageName = if (catholic) "DailyHabit-Catholic" else "DailyHabit"
            packageVersion = appVersion.substringBefore('-')
            description = if (catholic) "DailyHabit Catholic" else "DailyHabit"
            vendor = "hdlee73"
            copyright = "(c) 2026 Lee Hyeondeok (hdlee73@gmail.com)"
            modules("java.sql", "java.naming", "jdk.crypto.ec", "jdk.unsupported", "jdk.httpserver")

            windows {
                iconFile.set(project.file("icons/dailyhabit.ico"))
                menuGroup = "DailyHabit"
                shortcut = true
                menu = true
                dirChooser = true
                // 업그레이드 코드를 고정해 두어야 새 버전 설치 파일이 기존 설치본을 덮어 업데이트한다.
                // 천주교인용은 v1.0.0(이름 DailyHabit, 기본 천주교인용 모드)이 쓰던 코드를 이어받는다.
                upgradeUuid = if (catholic) "52aba140-d917-3af2-8940-366602e975d5" else "fde59af2-6401-4c88-b25d-95c5cb3fbdd8"
            }
        }
    }
}
