plugins {
    id("java")
}

group = "kr.chuyong"
version = "0.0.2-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

java.sourceCompatibility = JavaVersion.VERSION_17
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    compileOnly(files("libs/spigot-api-1.19.4-R0.1-SNAPSHOT.jar"))
    // https://mvnrepository.com/artifact/com.google.guava/guava
    compileOnly("com.google.guava:guava:32.1.2-jre")
    implementation(project(":bukkit-api"))
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")

}

tasks.getByName<Jar>("jar") {
    from(project(":bukkit-api").tasks.getByName("shadowJar"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
