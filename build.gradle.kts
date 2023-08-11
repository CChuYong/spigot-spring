plugins {
    val kotlinVersion = "1.8.0"
    kotlin("jvm") version kotlinVersion
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

val isRelease = System.getProperty("release") != null
val baseVersion = "0.0.2"
group = "kr.chuyong"
version = "${baseVersion}${if (isRelease) "" else "-SNAPSHOT"}"
description = "Spring Boot Spigot Starter"

val parentVersion = version
val parentGroup = group

repositories {
    mavenCentral()
}

subprojects {
    if (name != "spigot-class-modifier" && name != "bukkit-bootstrapper") apply(plugin = "kotlin")
    else apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")
    this.group = parentGroup
    this.version = parentVersion

    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    tasks.getByName<Jar>("jar") {
        enabled = true
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    tasks {
        withType<Jar> {
            from(sourceSets["main"].allSource)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

    tasks {
        build {
            dependsOn(shadowJar)
        }
    }
}

task("publishAll") {
    dependsOn(subprojects.map {
        it.tasks.getByName("shadowJar")
    }, subprojects.map {
        it.tasks.getByName("publish")
    }) // 모든 모듈의 빌드 태스크 의존성 추가

    doLast {
        subprojects.forEach { module ->
            val jarTask = module.tasks.findByName("shadowJar")
            if (jarTask != null) {
                val jarFile = jarTask.outputs.files.singleFile
                val targetDir = File("$buildDir/libs")
                copy {
                    from(jarFile)
                    into(targetDir)
                }
            }
        }
    }
}

tasks.register("printVersion") {
    doLast {
        println(baseVersion)
    }
}

tasks.register("printMeta") {
    doLast {
        println(baseVersion)
    }
}
