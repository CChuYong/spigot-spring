package chuyong.springspigot

import chuyong.springspigot.child.SpigotSpringChildInitializer
import chuyong.springspigot.child.SpigotSpringChildPluginData
import chuyong.springspigot.child.SpringSpigotChildPostInitializer
import chuyong.springspigot.child.SpringSpigotPluginRegistry
import chuyong.springspigot.config.BukkitConfigPropertySource
import chuyong.springspigot.util.PluginUtil
import chuyong.springspigot.util.SpringSpigotContextClassLoader
import chuyong.springspigot.util.YamlPropertiesFactory
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.PluginClassLoader
import org.slf4j.Logger
import org.springframework.boot.Banner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
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
    lateinit var parentContext: AnnotationConfigApplicationContext
    private val childContexts = mutableListOf<ConfigurableApplicationContext>()

    companion object {
        lateinit var mainContext: AnnotationConfigApplicationContext
    }

    fun start() {
        server.scheduler.runTaskLater(this, Runnable {
            loadSpringSpigot()
            val logger = parentContext.getBean(Logger::class.java)
            logger.info("Trying wire plugins...")
            val count = parentContext.getBean(SpringSpigotPluginRegistry::class.java).wireContexts(childContexts)
            logger.info("§f§lWired ${count} beans completed...")
        }, 0L)
    }

    fun stop() {
        onDisable()
    }

    private fun loadSpringSpigot() {
        val watch = StopWatch()
        watch.start()
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Initiated...")

        val spigotSpring = SpigotSpringChildPluginData.copyFromPlugin(plugin)

        val springSpigotPlugins = Arrays.stream(Bukkit.getPluginManager().plugins)
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
        val multiClassLoader = selfLoader

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

            Thread.currentThread().contextClassLoader = multiClassLoader

            val twoClazz = Class.forName(myClazz, true, multiClassLoader)

            val escalatedClazzes = escalatedPlugins.map {
                val addSourceMethod = multiClassLoader::class.java.getDeclaredMethod("addSource", URL::class.java)
                addSourceMethod.invoke(multiClassLoader, it.value.file.toURI().toURL())
                it.value.libraryUrls.forEach { url ->
                    addSourceMethod.invoke(multiClassLoader, url)
                }
                it.value.mainClass
            }



            val applicationBuilder = SpringApplicationBuilder(
                DefaultResourceLoader(multiClassLoader),
                twoClazz, *escalatedClazzes.toTypedArray()
            ).apply {
                bannerMode(Banner.Mode.OFF)
                initializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
                    val propertySources = it.environment.propertySources
                    propertySources.addLast(BukkitConfigPropertySource(config))
                    propertySources.addLast(PropertiesPropertySource("main-yaml", getPrimaryApplicationProperties()))

                    val props = Properties()
                    props["spigot.plugin"] = name
                    propertySources.addLast(PropertiesPropertySource("spring-bukkit", props))

                    it as AnnotationConfigApplicationContext
                    it.registerBean(SpigotSpringChildPluginData::class.java, Supplier { spigotSpring })
                    mainContext = it
                })

                parentContext = run() as AnnotationConfigApplicationContext
            }
            val initializedPlugin = parentContext.getBean(Plugin::class.java)
            JavaPlugin::class.java.getDeclaredField("isEnabled").apply {
                isAccessible = true
                set(initializedPlugin, true)
            }


            val postInitializer = parentContext.getBean(SpringSpigotChildPostInitializer::class.java)
            postInitializer.onPostInitialize(parentContext)
            val logger = parentContext.getBean(Logger::class.java)
            logger.info("Main context initialize completed! Starting to load plugins...")

            val plugins = normalPlugins.mapNotNull { plugin ->
                if(plugin.value == spigotSpring) return@mapNotNull null
                val data = plugin.value
                try {
                    logger.info("Loading Plugin ${data.description.name}")
                    val context = applicationBuilder.child(data.mainClass)
                        .bannerMode(Banner.Mode.OFF)
                        .initializers(SpigotSpringChildInitializer(data))
                        .resourceLoader(DefaultResourceLoader(data.classLoader))
                        .initializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
                            data.getContextApplicationProperties()?.apply {
                                logger.info("Found application.yml for plugin ${data.description.name}. Overriding default properties...")
                                val propertySources = it.environment.propertySources
                                propertySources.addLast(
                                    PropertiesPropertySource(
                                        "plugin-yaml",
                                        this,
                                    )
                                )
                            }
                        })
                        .run()
                    postInitializer.onPostInitialize(context)
                    logger.info("Loaded Plugin ${data.description.name}")
                    data to context
                } catch (e: Throwable) {
                    logger.info("Error while loading Plugin ${data.description.name}")
                    e.printStackTrace()
                    return@mapNotNull null
                }
            }

            logger.info("Loading ThirdParty Plugins completed!")
            childContexts.addAll(plugins.map { it.second })

            watch.stop()
            logger.info("SpigotSpring Post-Initialization process finished. Elapsed Time: " + watch.totalTimeMillis + "ms")
        }, executor).get()
        executor.shutdown()
    }

    override fun onDisable() {
        childContexts.forEach {
            it.close()
        }
        parentContext.close()
    }

    private fun getPrimaryApplicationProperties(): Properties {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        val configurationFile = FileSystemResource(File(dataFolder, "application.yml"))
        if (!configurationFile.exists()) {
            configurationFile.file.createNewFile()
        }
        return YamlPropertiesFactory.loadYamlIntoProperties(configurationFile)!!
    }

    fun registerClassLoader(classLoader: SpringSpigotContextClassLoader) {
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
