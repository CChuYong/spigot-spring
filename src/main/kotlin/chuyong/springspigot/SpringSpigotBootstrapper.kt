package chuyong.springspigot

import chuyong.springspigot.child.SpigotSpringChildInitializer
import chuyong.springspigot.child.SpigotSpringChildPluginData
import chuyong.springspigot.child.SpringSpigotChildPlugin
import chuyong.springspigot.config.ConfigurationPropertySource
import chuyong.springspigot.util.CompoundClassLoader
import chuyong.springspigot.util.YamlPropertiesFactory
import com.google.common.util.concurrent.ThreadFactoryBuilder
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.util.StopWatch
import java.io.File
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Consumer

class SpringSpigotBootstrapper : JavaPlugin() {
    private val masterClassLoader = CompoundClassLoader(Thread.currentThread().contextClassLoader, classLoader)
    private val logger = LoggerFactory.getLogger(SpringSpigotBootstrapper::class.java)
    lateinit var parentContext: AnnotationConfigApplicationContext

    companion object {
        lateinit var mainContext: AnnotationConfigApplicationContext
    }

    override fun onLoad() {
        loadSpringSpigot()
    }

    private fun loadSpringSpigot() {
        val watch = StopWatch()
        watch.start()
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Initiated...")

        val classLoaders = ArrayList<ClassLoader>()
        classLoaders.add(masterClassLoader)

        val pluginClasses = Arrays.stream(Bukkit.getPluginManager().plugins)
            .filter { plugin: Plugin ->
                plugin is JavaPlugin && plugin.javaClass.isAnnotationPresent(
                    EnableSpringSpigotSupport::class.java
                )
            }
            .toList()

        val javaClassLoader = Class.forName("org.bukkit.plugin.java.PluginClassLoader")
        val f = javaClassLoader.getDeclaredField("file")
        f.isAccessible = true
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading Spring-Spigot added plugins...")
        val pluginDataMap = mutableMapOf<String, SpigotSpringChildPluginData>()
        val setEnabledMethod = JavaPlugin::class.java.getDeclaredField("isEnabled").apply {
            isAccessible = true
        }
        pluginClasses.forEach(Consumer { plugin: Plugin ->
            try {
                val file = f[plugin.javaClass.classLoader] as File
                pluginDataMap[plugin.javaClass.name] = SpigotSpringChildPluginData.copyFromPlugin(plugin as JavaPlugin)
                setEnabledMethod.set(plugin, true)
                logger.info("Disabling plugin " + plugin.name + " To load from SpringSpigot..")
                unloadPlugin(plugin)
                //Bukkit.getPluginManager().disablePlugin(plugin)
                val newLoader = URLClassLoader(arrayOf(file.toURI().toURL()), masterClassLoader)
                classLoaders.add(newLoader)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        })
        val combinedLoader = CompoundClassLoader(classLoaders)
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lBaking Custom ClassLoader Completed...")

        val executor = Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("springspigot-initializer").build())
        CompletableFuture.runAsync({
            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading SpringBoot...")
            Thread.currentThread().contextClassLoader = masterClassLoader

            SpringApplicationBuilder(DefaultResourceLoader(combinedLoader), SpringSpigotApplication::class.java).apply {
                bannerMode(Banner.Mode.OFF)
                initializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
                    val propertySources= it.environment.propertySources
                    propertySources.addLast(ConfigurationPropertySource(getConfig()))
                    propertySources.addLast(
                    PropertiesPropertySource("main-yaml", getApplicationProperties()))

                    val props = Properties()
                    props["spigot.plugin"] = getName()
                    propertySources.addLast(PropertiesPropertySource("spring-bukkit", props))
                    mainContext = it as AnnotationConfigApplicationContext
                })
                val childClasses = pluginClasses.mapNotNull {
                    val targetClazz = AopUtils.getTargetClass(it)
                    try {
                        Class.forName(targetClazz.name, true, combinedLoader)
                    } catch (ex: Exception) {
                        null
                    }
                }
                val pluginConstructor = SpringSpigotChildPlugin::class.java.getConstructor()
                val loadeablePlugins = childClasses.associate { pluginClazz ->
                    val data = pluginDataMap[pluginClazz.name]
                    val newPluginClazz = ByteBuddy()
                        .subclass(SpringSpigotChildPlugin::class.java, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                        .name(pluginClazz.name + "\$Buddy")
                        .annotateType(AnnotationDescription
                            .Builder
                            .ofType(SpringBootApplication::class.java)
                            .defineArray("scanBasePackages", *arrayOf(pluginClazz.`package`.name))
                            .build()
                        )
                        .annotateType(AnnotationDescription
                            .Builder
                            .ofType(Primary::class.java)
                            .build()
                        )
                        .annotateType(AnnotationDescription
                            .Builder
                            .ofType(EnableAsync::class.java)
                            .build()
                        )
                        .annotateType(AnnotationDescription
                            .Builder
                            .ofType(EnableAspectJAutoProxy::class.java)
                            .build()
                        )
                        .defineConstructor(Visibility.PUBLIC).intercept(MethodCall.invoke(pluginConstructor))
                        .method(ElementMatchers.isDeclaredBy(pluginClazz))
                        .intercept(MethodDelegation.to(pluginClazz))
                        .make()

                    newPluginClazz.load(combinedLoader).loaded to data
                }
                val createdClasses = loadeablePlugins.keys.toList()
               parentContext = if(createdClasses.isNotEmpty()) {
                   createdClasses.drop(1).fold(
                        child(createdClasses.first())
                            .bannerMode(Banner.Mode.OFF)
                            .initializers(SpigotSpringChildInitializer(loadeablePlugins[createdClasses.first()]!!))
                            .resourceLoader(DefaultResourceLoader(combinedLoader))
                          //  .listeners(SpringSpigotChildConstructionListener())
                    ) { acc, clazz ->
                        acc
                            .sibling(clazz)
                            .bannerMode(Banner.Mode.OFF)
                            .initializers(SpigotSpringChildInitializer(loadeablePlugins[createdClasses.first()]!!))
                            .resourceLoader(DefaultResourceLoader(combinedLoader))
                         //   .listeners(SpringSpigotChildConstructionListener())
                    }
                } else {
                    this
                }.run() as AnnotationConfigApplicationContext
            }

            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lInitialize SpringBoot Application Context Completed...")


            watch.stop()
            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Finished. Elapsed Time: " + watch.totalTimeMillis + "ms")
        }, executor).get()
        executor.shutdown();
    }

    override fun onDisable() {
        mainContext.close()
    }

    private fun getApplicationProperties(): Properties {
        val configurationFile = FileSystemResource("application.yml")
        if(!configurationFile.exists()) {
            logger.warn("application.yml not found. Creating new one...")
            configurationFile.file.createNewFile()
        }
        return YamlPropertiesFactory.loadYamlIntoProperties(configurationFile)!!
    }

    private fun unloadPlugin(plugin: JavaPlugin) {
        val pluginLoader: JavaPluginLoader = JavaPlugin::class.java.getDeclaredField("loader").let {
            it.isAccessible = true
            it.get(plugin) as JavaPluginLoader
        }

        val loader = JavaPlugin::class.java.getDeclaredField("classLoader").let {
            it.isAccessible = true
            it.get(plugin) as  URLClassLoader
        }

        JavaPluginLoader::class.java.getDeclaredField("loaders").apply {
            isAccessible = true
            (get(pluginLoader) as MutableList<*>).remove(loader)
        }

        val names = loader::class.java.getDeclaredMethod("getClasses").apply {
            isAccessible = true
        }.invoke(loader) as Collection<*>
        val var8: Iterator<*> = names.iterator()
        while (var8.hasNext()) {
            JavaPluginLoader::class.java.getDeclaredMethod("removeClass", Class::class.java).apply {
                isAccessible = true
                invoke(pluginLoader,  var8.next())
            }
        }

    }

}
