package chuyong.springspigot

import chuyong.springspigot.child.SpigotSpringChildInitializer
import chuyong.springspigot.child.SpigotSpringChildPluginData
import chuyong.springspigot.child.SpringSpigotChildPostInitializer
import chuyong.springspigot.child.SpringSpigotPluginRegistry
import chuyong.springspigot.config.BukkitConfigPropertySource
import chuyong.springspigot.util.*
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import org.bukkit.plugin.java.PluginClassLoader
import org.slf4j.Logger
import org.springframework.beans.factory.support.SimpleInstantiationStrategy
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

class SpringSpigotBootstrapper : JavaPlugin() {
    private val masterClassLoader = CompoundClassLoader(Thread.currentThread().contextClassLoader, classLoader)
    lateinit var parentContext: AnnotationConfigApplicationContext
    private val childContexts = mutableListOf<ConfigurableApplicationContext>()

    companion object {
        lateinit var mainContext: AnnotationConfigApplicationContext
    }

    override fun onLoad() {
        loadSpringSpigot()
        val logger = parentContext.getBean(Logger::class.java)
        logger.info("Trying wire plugins...")
        val count = parentContext.getBean(SpringSpigotPluginRegistry::class.java).wireContexts(childContexts)
        logger.info("§f§lWired ${count} beans completed...")
    }

    override fun onEnable() {

    }

    private fun loadSpringSpigot() {
        val currentContextLoader = Thread.currentThread().contextClassLoader
        val watch = StopWatch()
        watch.start()
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Initiated...")

        val spigotSpring = SpigotSpringChildPluginData.copyFromPlugin(this)

        val pluginClasses = Arrays.stream(Bukkit.getPluginManager().plugins)
            .filter { plugin: Plugin ->
                (plugin is JavaPlugin && plugin.javaClass.isAnnotationPresent(
                    EnableSpringSpigotSupport::class.java
                )) //|| plugin == this
            }
            .toList()

        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading Spring-Spigot added plugins...")
        val pluginDataMap = mutableMapOf<String, SpigotSpringChildPluginData>()
        pluginDataMap[this.javaClass.name] = spigotSpring
        val setEnabledMethod = JavaPlugin::class.java.getDeclaredField("isEnabled").apply {
            isAccessible = true
        }
        val escalatedClassNames = mutableSetOf<String>()

        val libraryLoaders = mutableListOf<URLClassLoader>()
        val libraryClasses = mutableSetOf<URL>()
        val pluginClassNames = mutableListOf<String>()
        val pluginUrl = pluginClasses.mapNotNull { plugin ->
            try {
                val cfg = plugin.javaClass.getAnnotation(EnableSpringSpigotSupport::class.java)!!

                val data = SpigotSpringChildPluginData.copyFromPlugin(plugin as JavaPlugin)
                pluginDataMap[plugin.javaClass.name] = data
                setEnabledMethod.set(plugin, true)
                Bukkit.getConsoleSender()
                    .sendMessage("§f§l[§6SpringSpigot§f§l] Disabling plugin " + plugin.name + " To load from SpringSpigot..")


                val lib = plugin::class.java.classLoader::class.java.getDeclaredField("libraryLoader").let {
                    it.isAccessible = true
                    val loader = (it.get(plugin::class.java.classLoader) as? URLClassLoader)
                    loader?.apply {
                        libraryLoaders.add(this)
                    }
                    loader?.urLs ?: emptyArray()
                }
                libraryClasses.addAll(lib)
                Bukkit.getPluginManager().disablePlugin(plugin)
              //  PluginUtil.unloadPlugin(plugin)
                if(!cfg.contextEscalation){
                    pluginClassNames.add(plugin.javaClass.name)
                } else {
                    Bukkit.getConsoleSender()
                        .sendMessage("§f§l[§6SpringSpigot§f§l] Experimental Context Escalation on plugin " + plugin.name + "")
                    escalatedClassNames.add(plugin.javaClass.name)
                }
                return@mapNotNull data.file.toURI().toURL()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        val list = mutableListOf<URL>()
        list.addAll(pluginUrl)
        list.addAll(libraryClasses)

        val libField = PluginClassLoader::class.java.getDeclaredField("libraryLoader").apply {
            isAccessible = true
        }
        val nonRelatedPlugins = Arrays.stream(Bukkit.getPluginManager().plugins)
            .filter { plugin: Plugin ->
                (!plugin.javaClass.isAnnotationPresent(
                    EnableSpringSpigotSupport::class.java
                )) && plugin != this
            }
            .toList()
            .mapNotNull {
                libField.get(it::class.java.classLoader) as? URLClassLoader
            }

        val customLoader = PaperPluginClassLoader(
            parent = classLoader,
           // mainContextLoader = currentContextLoader,
            urls = list.toTypedArray(),
            thirdPartyLibraryLoader = CompoundClassLoader(nonRelatedPlugins),
            tempDataMap = pluginDataMap
        )


        val multiClassLoader = CompoundClassLoader(currentContextLoader, classLoader, customLoader)

        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lBaking Custom ClassLoader Completed...")


        val executor =
            Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("SpringSpigot Bootstrap").build())
        CompletableFuture.runAsync({
            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading SpringBoot...")
            unloadPlugin(this)

            Thread.currentThread().contextClassLoader = multiClassLoader
            val myClazz = SpringSpigotApplication::class.java.name
            val twoClazz = Class.forName(myClazz, true, multiClassLoader)

            val escalatedClazzes = escalatedClassNames.map {
                Class.forName(it, true, multiClassLoader)
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

            val plugins = pluginClassNames.mapNotNull { pluginClazzName ->
                if (pluginClazzName == SpringSpigotBootstrapper::class.java.name) return@mapNotNull null
                val data = pluginDataMap[pluginClazzName]!!
                try {
                    val pluginClazz = Class.forName(pluginClazzName, true, multiClassLoader)
                    logger.info("Loading Plugin ${data.description.name}")
                    val context = applicationBuilder.child(pluginClazz)
                        .bannerMode(Banner.Mode.OFF)
                        .initializers(SpigotSpringChildInitializer(data))
                        .resourceLoader(DefaultResourceLoader(multiClassLoader))
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

    private fun unloadPlugin(plugin: JavaPlugin) {
        server.pluginManager.disablePlugin(plugin)

    }

}
