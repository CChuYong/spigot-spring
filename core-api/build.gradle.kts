plugins {
    val kotlinVersion = "1.8.0"
    `maven-publish`
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.spring") version kotlinVersion
}

group = "kr.chuyong"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter:3.1.1")
    compileOnly("io.insert-koin:koin-core:3.4.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["kotlin"])
    }
}
