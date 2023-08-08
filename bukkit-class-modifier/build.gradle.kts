plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

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

publishing {
    publications.create<MavenPublication>(project.name) {
        artifactId = "spigot-spring-${project.name}"
        from(components["java"])
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
