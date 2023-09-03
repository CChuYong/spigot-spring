package chuyong.springspigot

import chuyong.springspigot.child.SpigotSpringChildPluginData
import chuyong.springspigot.child.SpringSpigotPluginRegistry
import chuyong.springspigot.config.BukkitConfigPropertySource
import chuyong.springspigot.util.PluginUtil
import chuyong.springspigot.util.SpringSpigotContextClassLoader
import chuyong.springspigot.util.YamlPropertiesFactory
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.springframework.boot.Banner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.util.StopWatch
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier

class SpringSpigotBootstrapper(
    val plugin: JavaPlugin,
    val selfLoader: URLClassLoader,
    val springSpigotLoader: URLClassLoader,
    val contextLoader: Any,
) : Plugin by plugin {
    object Unsafe {
        lateinit var mainContext: GenericApplicationContext
        lateinit var pluginRegistry: SpringSpigotPluginRegistry
    }

    fun start() {
        server.scheduler.runTaskLater(this, Runnable {
            loadSpringSpigot()
        }, 0L)
    }

    fun stop() {
        Unsafe.pluginRegistry.unloadPlugins()
        Unsafe.mainContext.close()
    }

    private fun loadSpringSpigot() {
        val watch = StopWatch()
        watch.start()
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Initiated...")

        val spigotSpring = SpigotSpringChildPluginData.copyFromPlugin(plugin)

        val springSpigotPlugins = Arrays
            .stream(Bukkit.getPluginManager().plugins)
            .filter { plugin: Plugin ->
                plugin.description.depend.contains(name)
            }
            .toList()

        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading Spring-Spigot added plugins...")
        val pluginDataMap = mutableMapOf<String, SpigotSpringChildPluginData>()
        pluginDataMap[this.javaClass.name] = spigotSpring
        val setEnabledMethod = JavaPlugin::class.java.getDeclaredField("isEnabled").apply {
            isAccessible = true
        }

        springSpigotPlugins.forEach { plugin ->
            try {
                val data = SpigotSpringChildPluginData.copyFromPlugin(plugin as JavaPlugin)
                pluginDataMap[plugin.javaClass.name] = data
                setEnabledMethod.set(plugin, true)
                Bukkit.getConsoleSender()
                    .sendMessage("§f§l[§6SpringSpigot§f§l] Disabling plugin " + plugin.name + " To load from SpringSpigot..")

                PluginUtil.unloadPlugin(plugin)
                data.initLoader(selfLoader, springSpigotLoader)
                if(!data.isEscalated)
                    registerClassLoader(data.classLoader!!)
                else {
                    val tmp = data.classLoader
                    data.classLoader = null
                    tmp?.close()
                }
                System.gc()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        val myClazz = SpringSpigotApplication::class.java.name

        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lBaking Custom ClassLoader Completed...")


        val executor =
            Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("SpringSpigot Bootstrap").build())
        CompletableFuture.runAsync({
            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading SpringBoot...")
            val escalatedPlugins = pluginDataMap.filter {
                it.value.isEscalated
            }

            val normalPlugins = pluginDataMap.filter {
                !it.value.isEscalated
            }

            Thread.currentThread().contextClassLoader = selfLoader

            val twoClazz = Class.forName(myClazz, true, selfLoader)

            val escalatedClazzes = escalatedPlugins.map {
                val addSourceMethod = selfLoader::class.java.getDeclaredMethod("addSource", URL::class.java)
                addSourceMethod.invoke(selfLoader, it.value.file.toURI().toURL())
                it.value.libraryUrls.forEach { url ->
                    addSourceMethod.invoke(selfLoader, url)
                }
                it.value.mainClass
            }



            val applicationBuilder = SpringApplicationBuilder(
                DefaultResourceLoader(selfLoader),
                twoClazz, *escalatedClazzes.toTypedArray()
            ).apply {
                bannerMode(Banner.Mode.OFF)
                web(WebApplicationType.NONE)
                initializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
                    val propertySources = it.environment.propertySources
                    propertySources.addLast(BukkitConfigPropertySource(config))
                    propertySources.addLast(PropertiesPropertySource("main-yaml", getPrimaryApplicationProperties()))

                    val props = Properties()
                    props["spigot.plugin"] = name
                    propertySources.addLast(PropertiesPropertySource("spring-bukkit", props))

                    it as GenericApplicationContext
                    it.registerBean(SpigotSpringChildPluginData::class.java, Supplier { spigotSpring })
                    Unsafe.mainContext = it
                })

                val application = build()
                Unsafe.mainContext = application.run() as GenericApplicationContext
            }
            val initializedPlugin = Unsafe.mainContext.getBean(Plugin::class.java)
            JavaPlugin::class.java.getDeclaredField("isEnabled").apply {
                isAccessible = true
                set(initializedPlugin, true)
            }


            Unsafe.pluginRegistry = Unsafe.mainContext.getBean(SpringSpigotPluginRegistry::class.java)
            Unsafe.pluginRegistry.onPostInitialize(Unsafe.mainContext)
            val logger = Unsafe.mainContext.getBean(Logger::class.java)
            logger.info("Main context initialize completed! Starting to load plugins...")


            val plugins = normalPlugins.mapNotNull { plugin ->
                if(plugin.value == spigotSpring) return@mapNotNull null
                Unsafe.pluginRegistry.loadPlugin(plugin.value, applicationBuilder)
            }
            logger.info("Loading ThirdParty Plugins completed! (Success: ${plugins.size}, Failed: ${normalPlugins.size - plugins.size})")

            Unsafe.pluginRegistry.wireContexts()


            watch.stop()
            logger.info("SpigotSpring Post-Initialization process finished. Elapsed Time: " + watch.totalTimeMillis + "ms")
        }, executor).get()
        executor.shutdown()
    }
    private fun getPrimaryApplicationProperties(): Properties {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        val configurationFile = FileSystemResource(File(dataFolder, "application.yml"))
        if (!configurationFile.exists()) {
            configurationFile.file.createNewFile()
        }
        return YamlPropertiesFactory.loadYamlIntoProperties(configurationFile)!!
    }

    private fun registerClassLoader(classLoader: SpringSpigotContextClassLoader) {
        val fn: java.util.function.Function<String, Class<*>?> = java.util.function.Function { name ->
            try {
                classLoader.readSelf(name, true)
            } catch (e: ClassNotFoundException) {
                null
            }
        }

        contextLoader::class.java.getDeclaredMethod("addNewLoader", java.util.function.Function::class.java)
            .apply {
                invoke(contextLoader, fn)
            }
    }

}
