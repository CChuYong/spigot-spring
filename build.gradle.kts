
plugins {
    val kotlinVersion = "1.8.0"
    val springBootVersion = "3.1.1"
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val isRelease = false
val baseVersion = "0.0.1"
group = "kr.chuyong"
version = "${baseVersion}${if(isRelease) "" else "-SNAPSHOT"}"
description = "Spring Boot Spigot Starter"

dependencies {

}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "spigot-spring"
    }

    repositories {
        maven {
            val urlPath = if(isRelease)
                uri("https://nexus.chuyong.kr/repository/maven-releases/")
            else
                uri("https://nexus.chuyong.kr/repository/maven-snapshots/")
            name = "CChuYong"
            url = uri(urlPath)
        }
    }
}
