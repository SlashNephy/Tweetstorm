import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "jp.nephy"
val ktorVersion = "1.2.0"

plugins {
    kotlin("jvm") version "1.3.31"
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/ktor")
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = "https://kotlin.bintray.com/kotlin-eap")
    maven(url = "https://dl.bintray.com/nephyproject/penicillin")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-html-builder:$ktorVersion")
    compile("io.ktor:ktor-client-apache:$ktorVersion")

    compile("jp.nephy:penicillin:4.2.2-eap-4")
    
    compile("commons-cli:commons-cli:1.4")

    compile("io.github.microutils:kotlin-logging:1.6.26")
    compile("ch.qos.logback:logback-core:1.2.3")
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.fusesource.jansi:jansi:1.17.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = kotlinOptions.freeCompilerArgs + "-Xuse-experimental=kotlin.Experimental"
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-full"
    manifest {
        attributes["Main-Class"] = "jp.nephy.tweetstorm.Tweetstorm"
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
