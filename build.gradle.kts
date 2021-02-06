import com.adarshr.gradle.testlogger.theme.ThemeType
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("multiplatform") version "1.4.30"
    id("com.github.johnrengelman.shadow") version "6.1.0"

    // For testing
    id("com.adarshr.test-logger") version "2.1.1"
    id("net.rdrei.android.buildtimetracker") version "0.11.0"
}

object ThirdpartyVersion {
    const val Ktor = "1.5.1"
    const val Penicillin = "6.0.2"
    const val JsonKt = "6.0.0"
    const val CommonsCLI = "1.4"

    // For testing
    const val JUnit = "5.7.0"

    // For logging
    const val KotlinLogging = "2.0.4"
    const val Logback = "1.2.3"
    const val jansi = "1.18"
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = "https://dl.bintray.com/starry-blue-sky/stable")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))

                implementation("blue.starry:penicillin:${ThirdpartyVersion.Penicillin}")
                implementation("blue.starry:jsonkt:${ThirdpartyVersion.JsonKt}")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        named("jvmMain") {
            dependencies {
                implementation("io.ktor:ktor-server-cio:${ThirdpartyVersion.Ktor}")
                implementation("io.ktor:ktor-html-builder:${ThirdpartyVersion.Ktor}")
                implementation("io.ktor:ktor-client-cio:${ThirdpartyVersion.Ktor}")

                implementation("commons-cli:commons-cli:${ThirdpartyVersion.CommonsCLI}")

                // For logging
                implementation("io.github.microutils:kotlin-logging:${ThirdpartyVersion.KotlinLogging}")
                implementation("ch.qos.logback:logback-core:${ThirdpartyVersion.Logback}")
                implementation("ch.qos.logback:logback-classic:${ThirdpartyVersion.Logback}")
                implementation("org.fusesource.jansi:jansi:${ThirdpartyVersion.jansi}")
            }
        }
        named("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:${ThirdpartyVersion.JUnit}")
            }
        }
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                apiVersion = "1.4"
                languageVersion = "1.4"
                verbose = true
            }
        }
    }

    sourceSets.all {
        languageSettings.progressiveMode = true
        languageSettings.apply {
            useExperimentalAnnotation("kotlin.Experimental")
        }
    }
}

/*
 * Tests
 */

buildtimetracker {
    reporters {
        register("summary") {
            options["ordered"] = "true"
            options["barstyle"] = "ascii"
            options["shortenTaskNames"] = "false"
        }
    }
}

testlogger {
    theme = ThemeType.MOCHA
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

task<JavaExec>("run") {
    dependsOn("build")

    group = "application"
    main = "blue.starry.tweetstorm.MainKt"
    classpath(configurations["jvmRuntimeClasspath"], tasks["jvmJar"])
}

// workaround for Kotlin/Multiplatform + Shadow issue
// Refer https://github.com/johnrengelman/shadow/issues/484#issuecomment-549137315.
task<ShadowJar>("shadowJar") {
    group = "shadow"
    dependsOn("jvmJar")

    manifest {
        attributes("Main-Class" to "blue.starry.tweetstorm.MainKt")
    }
    archiveClassifier.set("all")

    val jvmMain = kotlin.jvm().compilations.getByName("main")
    from(jvmMain.output)
    configurations.add(jvmMain.compileDependencyFiles as Configuration)
}
