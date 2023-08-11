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
    // maven("https://maven.hqservice.kr/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core-api"))
    compileOnly(project(":spigot-class-modifier"))
    // https://mvnrepository.com/artifact/org.ow2.asm/asm
    compileOnly(files("libs/spigot-api-1.19.4-R0.1-SNAPSHOT.jar"))

    implementation("org.springframework.boot:spring-boot-starter:3.1.1")
    implementation("org.springframework.boot:spring-boot-starter-aop:3.1.1")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.1.1")
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.1.1")

    compileOnly("org.apache.commons:commons-lang3:3.12.0")
    compileOnly("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    compileOnly("mysql:mysql-connector-java:8.0.28")
    compileOnly("com.h2database:h2:1.4.200")
    compileOnly("net.bytebuddy:byte-buddy:1.14.5")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect")
    compileOnly("com.github.f4b6a3:ulid-creator:5.2.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.1")


    //compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
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
