plugins {
    id("java")
}

group = "kr.chuyong"
version = "0.0.2-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

java.sourceCompatibility = JavaVersion.VERSION_17
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    compileOnly(files("libs/spigot-api-1.19.4-R0.1-SNAPSHOT.jar"))
    // https://mvnrepository.com/artifact/com.google.guava/guava
    compileOnly("com.google.guava:guava:32.1.2-jre")
    compileOnly(project(":paper-api"))

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
