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

### SpigotSpring throws `java.lang.IllegalAccessError` while loading
Paper API provides custom implementation for PluginClassLoaders.
However, Spigot API restricts inheritance of PluginClassLoader.  
Thus, we created small java-agent which modify PluginClassLoader's Access Modifier to public.  
If your server uses Spigot, you should download `spigot-class-modifier` and start bukkit with flag `-javaagent:spigot-class-modifier.jar`.
