plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "kr.chuyong"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.5")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


tasks.getByName<Jar>("jar") {
    manifest {
        attributes["Premain-Class"] = "chuyong.springspigot.PremainAgent"
    }
}
