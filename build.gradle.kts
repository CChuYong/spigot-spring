import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    val kotlinVersion = "1.8.0"
    val springBootVersion = "3.1.1"
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("kapt") version kotlinVersion

    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version "1.1.2"
}

group = "kr.chuyong"
version = "0.0.1"
description = "Spring Boot Spigot Starter"
java.sourceCompatibility = JavaVersion.VERSION_17

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}


repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.hqservice.kr/repository/maven-public/")

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api("org.springframework.boot:spring-boot-starter:3.1.1")
    api("org.springframework.boot:spring-boot-starter-aop:3.1.1")
    api("org.apache.commons:commons-lang3:3.12.0")
    api("com.github.f4b6a3:ulid-creator:5.2.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.1")

    api("org.springframework.boot:spring-boot-starter-data-jpa:3.1.1")
    implementation("mysql:mysql-connector-java:8.0.28")
    implementation("com.h2database:h2:1.4.200")
    api("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-kotlin-codegen:5.0.0")

    compileOnly("io.insert-koin:koin-core:3.4.2")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("kr.hqservice:hqframework-global-core:1.0.0-SNAPSHOT")
    compileOnly("kr.hqservice:hqframework-bukkit-core:1.0.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "spigot-spring-boot"
        from(components["kotlin"])
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
