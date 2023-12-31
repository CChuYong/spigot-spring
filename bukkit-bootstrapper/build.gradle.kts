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
    compileOnly(files("libs/spigot-api-1.19.4-R0.1-SNAPSHOT.jar"))
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

tasks.getByName<ProcessResources>("processResources") {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = Charsets.UTF_8.toString()
    filesMatching("plugin.yml") {
        expand(props)
    }
}
