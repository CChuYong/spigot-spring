package chuyong.springspigot

import chuyong.springspigot.child.*
import chuyong.springspigot.config.ConfigurationPropertySource
import chuyong.springspigot.util.*
import chuyong.springspigot.util.MultiClassLoader
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
import org.slf4j.Logger
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.springframework.core.MethodParameter
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.support.SpringFactoriesLoader
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.util.StopWatch
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class SpringSpigotBootstrapper : JavaPlugin() {
    private val masterClassLoader = CompoundClassLoader(Thread.currentThread().contextClassLoader, classLoader)
    lateinit var parentContext: AnnotationConfigApplicationContext
    private val childContexts = mutableListOf<ConfigurableApplicationContext>()

    companion object {
        lateinit var mainContext: AnnotationConfigApplicationContext
    }

    override fun onEnable() {
        val logger = parentContext.getBean(Logger::class.java)
        logger.info("Trying wire plugins...")
        val count = parentContext.getBean(SpringSpigotPluginRegistry::class.java).wireContexts(childContexts)
        logger.info("§f§lWired ${count} beans completed...")
    }

    override fun onLoad() {
        loadSpringSpigot()
    }

    private fun loadSpringSpigot() {
        val currentContextLoader = Thread.currentThread().contextClassLoader
        val watch = StopWatch()
        watch.start()
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Initiated...")

        val globalClassLoader = CompoundClassLoader(masterClassLoader)
        val spigotSpring = SpigotSpringChildPluginData.copyFromPlugin(this)

        val pluginClasses = Arrays.stream(Bukkit.getPluginManager().plugins)
            .filter { plugin: Plugin ->
                (plugin is JavaPlugin && plugin.javaClass.isAnnotationPresent(
                    EnableSpringSpigotSupport::class.java
                ))
            }
            .toList()

        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading Spring-Spigot added plugins...")
        val pluginDataMap = mutableMapOf<String, SpigotSpringChildPluginData>()
        val setEnabledMethod = JavaPlugin::class.java.getDeclaredField("isEnabled").apply {
            isAccessible = true
        }

        val libraryClasses = mutableSetOf<URL>()
        val pluginClassNames = mutableListOf<String>()
        val pluginUrl = pluginClasses.map { plugin ->
            try {
                val data = SpigotSpringChildPluginData.copyFromPlugin(plugin as JavaPlugin)
                pluginDataMap[plugin.javaClass.name] = data
                setEnabledMethod.set(plugin, true)
                Bukkit.getConsoleSender()
                    .sendMessage("§f§l[§6SpringSpigot§f§l] Disabling plugin " + plugin.name + " To load from SpringSpigot..")
                val lib = plugin::class.java.classLoader::class.java.getDeclaredField("libraryLoader").let {
                    it.isAccessible = true
                    (it.get(plugin::class.java.classLoader) as? URLClassLoader)?.urLs ?: emptyArray()
                }
                libraryClasses.addAll(lib)
                pluginClassNames.add(plugin.javaClass.name)
              //  unloadPlugin(plugin)
                PluginUtil().unloadPlugin(plugin)

                val myLoader = PluginClassLoader(
                    parent = masterClassLoader,
                    mainContextLoader = currentContextLoader,
                    description =data.description,
                    file = data.file,
                    libraryLoader = null,
                )
                globalClassLoader.addLoader(myLoader)
                data.file.toURI().toURL()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        val multiClassLoader = MultiClassLoader(
            parent = masterClassLoader,
            mainContextLoader = currentContextLoader,
            urls = pluginUrl.toTypedArray(),
            libraryLoader = URLClassLoader(libraryClasses.toTypedArray()),
        )

    //   val crossPluginLoader = URLClassLoader(pluginUrl.toTypedArray(), masterClassLoader)
      //  globalClassLoader.addLoader(crossPluginLoader)


        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lBaking Custom ClassLoader Completed...")

        val executor =
            Executors.newSingleThreadExecutor(ThreadFactoryBuilder().setNameFormat("SpringSpigot Bootstrap").build())
        val globalResourceLoader = DefaultResourceLoader(globalClassLoader)
        CompletableFuture.runAsync({
            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading SpringBoot...")
            Thread.currentThread().contextClassLoader = globalClassLoader
            val myClazz = SpringSpigotApplication::class.java.name

            unloadPlugin(this)
            val twoClazz = Class.forName(myClazz, true, globalClassLoader)

            val applicationBuilder = SpringApplicationBuilder(
                globalResourceLoader,
                twoClazz,
            ).apply {
                bannerMode(Banner.Mode.OFF)
                initializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
                    val propertySources = it.environment.propertySources
                    propertySources.addLast(ConfigurationPropertySource(config))
                    propertySources.addLast(PropertiesPropertySource("main-yaml", getPrimaryApplicationProperties()))

                    val props = Properties()
                    props["spigot.plugin"] = name
                    propertySources.addLast(PropertiesPropertySource("spring-bukkit", props))
                    mainContext = it as AnnotationConfigApplicationContext
                })

                parentContext = run() as AnnotationConfigApplicationContext
            }
            val postInitializer = parentContext.getBean(SpringSpigotChildPostInitializer::class.java)
            postInitializer.onPostInitialize(parentContext)
            val logger = parentContext.getBean(Logger::class.java)
            logger.info("Main context initialize completed! Starting to load plugins...")

            val plugins = pluginClassNames.mapNotNull { pluginClazzName ->
                if(pluginClazzName == SpringSpigotBootstrapper::class.java.name) return@mapNotNull null
                val data = pluginDataMap[pluginClazzName]!!
                try {
                    val pluginClazz = Class.forName(pluginClazzName, true, multiClassLoader)
                    logger.info("Loading Plugin ${data.description.name}")
                    val newClazz = createMockClazz(
                        pluginClazz = pluginClazz,
                        classLoader = multiClassLoader,
                    )
                    val context = applicationBuilder.child(newClazz)
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
        val pluginLoader: JavaPluginLoader = JavaPlugin::class.java.getDeclaredField("loader").let {
            it.isAccessible = true
            it.get(plugin) as JavaPluginLoader
        }

        val loader = JavaPlugin::class.java.getDeclaredField("classLoader").let {
            it.isAccessible = true
            it.get(plugin) as URLClassLoader
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
                invoke(pluginLoader, var8.next())
            }
        }

    }

    private fun createMockClazz(pluginClazz: Class<*>, classLoader: ClassLoader): Class<*> {
        val pluginConstructor = SpringSpigotChildPlugin::class.java.getConstructor()
        val newPluginClazz = ByteBuddy()
            .subclass(SpringSpigotChildPlugin::class.java, ConstructorStrategy.Default.NO_CONSTRUCTORS)
            .name(pluginClazz.name + "\$ByteBuddy")
            .annotateType(
                AnnotationDescription
                    .Builder
                    .ofType(SpringBootApplication::class.java)
                    .defineArray("scanBasePackages", *arrayOf(pluginClazz.`package`.name))
                    .build()
            )
            .annotateType(
                AnnotationDescription
                    .Builder
                    .ofType(Primary::class.java)
                    .build()
            )
            .annotateType(
                AnnotationDescription
                    .Builder
                    .ofType(EnableAsync::class.java)
                    .build()
            )
            .annotateType(
                AnnotationDescription
                    .Builder
                    .ofType(EnableAspectJAutoProxy::class.java)
                    .build()
            )
            .defineConstructor(Visibility.PUBLIC).intercept(MethodCall.invoke(pluginConstructor))
            .method(ElementMatchers.isDeclaredBy(pluginClazz))
            .intercept(MethodDelegation.to(pluginClazz))
            .make()

        return newPluginClazz.load(classLoader).loaded
    }

}
