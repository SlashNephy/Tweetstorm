import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines

val ktorVersion = "0.9.4"

plugins {
    kotlin("jvm") version "1.2.70"
    application
}

application {
    mainClassName = "jp.nephy.tweetstorm.MainKt"
}

group = "jp.nephy"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/ktor")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-html-builder:$ktorVersion")
    compile("org.jetbrains.kotlinx:atomicfu:0.11.3")

    compile("jp.nephy:penicillin:3.0.9")
    compile("commons-cli:commons-cli:1.4")

    compile("io.github.microutils:kotlin-logging:1.5.9")
    compile("ch.qos.logback:logback-core:1.2.3")
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.fusesource.jansi:jansi:1.17.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
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
