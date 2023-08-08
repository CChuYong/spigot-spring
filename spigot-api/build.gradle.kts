import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    val kotlinVersion = "1.8.0"
    val springBootVersion = "3.1.1"
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version "1.1.2"
}

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

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "spigot-spring-${project.name}"
        from(components["kotlin"])
        artifact(tasks["kotlinSourcesJar"])
    }

    repositories {
        maven {
            val urlPath = if (!version.toString().contains("SNAPSHOT"))
                uri("https://nexus.chuyong.kr/repository/maven-releases/")
            else
                uri("https://nexus.chuyong.kr/repository/maven-snapshots/")
            name = "CChuYong"
            url = uri(urlPath)
        }
    }
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
    implementation(project(":core-api"))
    // https://mvnrepository.com/artifact/org.ow2.asm/asm
    compileOnly(files("libs/spigot-api-1.19.4-R0.1-SNAPSHOT.jar"))
    implementation("org.springframework.boot:spring-boot-starter:3.1.1")
    implementation("org.springframework.boot:spring-boot-starter-aop:3.1.1")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.1.1")
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.1.1")

    compileOnly("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("com.github.f4b6a3:ulid-creator:5.2.0")
    compileOnly("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    compileOnly("mysql:mysql-connector-java:8.0.28")
    compileOnly("com.h2database:h2:1.4.200")
    compileOnly("net.bytebuddy:byte-buddy:1.14.5")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.1")

    compileOnly("kr.hqservice:hqframework-global-core:1.0.0-SNAPSHOT")
    compileOnly("kr.hqservice:hqframework-bukkit-core:1.0.0-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

}

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<ProcessResources>("processResources") {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = Charsets.UTF_8.toString()
    filesMatching("plugin.yml") {
        expand(props)
    }
}