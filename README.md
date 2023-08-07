# Spigot Spring
A Minecraft Plugin integrates Spigot with SpringBoot with multiple plugins.  
This plugin based on [mcspring-boot](https://github.com/Alan-Gomes/mcspring-boot).

## Getting Started
First, add dependency to your gradle files. Following example is Gradle Kotlin DSL (.kts)

```groovy
repositories {
      maven("https://nexus.chuyong.kr/repository/maven-snapshots/")
}

dependencies {
    implementation("kr.chuyong:spigot-spring:0.0.1-SNAPSHOT")
}
```

Then Simply put `@EnableSpringSpigotSupport` to your JavaPlugin class. After that, you are ready to go. 
```kotlin
@EnableSpringSpigotSupport
class MySpecialPlugin : JavaPlugin() {
    //Body Not need..
}
```


## Features
- [x] Supports multiple plugins using spigot-spring
- [x] Supports SpringBoot 3.0+
- [x] Supports AOP Based spring libraries (JPA TX, Scheduling...)
- [x] Integrated Spring Scheduler with Minecraft Bukkit Scheduler
- [x] Supports `Annotated Command Controller` just like Spring's RestController
- [x] Includes default JPA ItemStack Types (Storing ItemStack to DataBase)

## Known issues
### My plugin's external library dependency collision with SpringSpigot!
SpigotSpring Plugin uses Bukkit's PluginClassLoader while bootstrapping, so cannot override bukkit's default dependencies (ex: latest netty, guava.. etc)
### Cannot use external SpringBoot starter libraries!
All Bukkit's plugin has unique `PluginClassLoader`, So if individual plugin loads springboot related classes, it will cause classloader exception. (SpringBoot's A class can loaded both SpigotSpring and your plugin, but it treated as different class)
