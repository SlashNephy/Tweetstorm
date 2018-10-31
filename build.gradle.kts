import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines

val ktorVersion = "1.0.0-beta-3"

plugins {
    kotlin("jvm") version "1.3.0"
    application
}

application {
    mainClassName = "jp.nephy.tweetstorm.MainKt"
}

group = "jp.nephy"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url = "https://dl.bintray.com/kotlin/ktor")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-html-builder:$ktorVersion")
    compile("org.jetbrains.kotlinx:atomicfu:0.11.12")

    compile("jp.nephy:penicillin:3.1.0")
    compile("io.ktor:ktor-client-apache:$ktorVersion")
    compile("commons-cli:commons-cli:1.4")

    compile("io.github.microutils:kotlin-logging:1.6.10")
    compile("ch.qos.logback:logback-core:1.2.3")
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.fusesource.jansi:jansi:1.17.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = kotlinOptions.freeCompilerArgs + "-Xuse-experimental=kotlin.Experimental"
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-full"
    manifest {
        attributes["Main-Class"] = "jp.nephy.tweetstorm.MainKt"
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    from(configurations.runtime.map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
