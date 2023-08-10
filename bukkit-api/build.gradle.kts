plugins {
    val kotlinVersion = "1.8.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
}

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(project(":core-api"))
    compileOnly(files("libs/spigot-api-1.19.4-R0.1-SNAPSHOT.jar"))
    compileOnly(project(":bukkit-class-modifier"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.springframework.boot:spring-boot-starter:3.1.1")
    compileOnly("org.springframework.boot:spring-boot-starter-aop:3.1.1")
    compileOnly("io.insert-koin:koin-core:3.4.2")
    compileOnly("com.github.f4b6a3:ulid-creator:5.2.0")
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa:3.1.1")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
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
