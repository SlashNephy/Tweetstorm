import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines

val ktorVersion = "0.9.4-alpha-2"

plugins {
    kotlin("jvm") version "1.2.60"
}

group = "jp.nephy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "http://kotlin.bintray.com/ktor")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    compile("io.ktor:ktor-server-core:$ktorVersion")
    compile("io.ktor:ktor-server-netty:$ktorVersion")

    compile("jp.nephy:penicillin:2.0.4")
    compile("jp.nephy:jsonkt:1.5")

    compile("io.github.microutils:kotlin-logging:1.4.9")
    compile("ch.qos.logback:logback-core:1.2.3")
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.fusesource.jansi:jansi:1.17.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}
