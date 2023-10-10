package chuyong.springspigot.child

import chuyong.springspigot.SpringSpigotBootstrapper
import chuyong.springspigot.child.annotation.WirePlugin
import chuyong.springspigot.command.CommandRegistry
import chuyong.springspigot.command.annotation.CommandAdvice
import chuyong.springspigot.command.annotation.CommandController
import chuyong.springspigot.event.BukkitEventService
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.SimplePluginManager
import org.slf4j.Logger
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.Banner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

@Component
class SpringSpigotPluginRegistry(
    private val logger: Logger,
    private val parentBuilder: SpringApplicationBuilder,
    private val commandRegistry: CommandRegistry,
) {
    private val plugins = ConcurrentHashMap<String, SpringSpigotChildPlugin>()
    private val pluginMetas = ConcurrentHashMap<String, SpigotSpringChildPluginData>()

    fun getPlugin(plugin: String): SpringSpigotChildPlugin? {
        return plugins[plugin]
    }

    fun getPlugins(): List<SpringSpigotChildPlugin> {
        return plugins.values.toList()
    }

    fun getPluginMetas(): List<SpigotSpringChildPluginData> {
        return pluginMetas.values.toList()
    }

    fun getPluginMeta(pluginName: String): SpigotSpringChildPluginData? = pluginMetas[pluginName]

    fun loadPlugin(pluginData: SpigotSpringChildPluginData): ConfigurableApplicationContext? {
        try {
            logger.info("Loading Plugin ${pluginData.description.name}")
            pluginMetas[pluginData.description.name] = pluginData
            return enablePlugin(pluginData)
        } catch (e: Throwable) {
            logger.info("Error while loading Plugin ${pluginData.description.name}")
            e.printStackTrace()
            return null
        }
    }

    fun enablePlugin(pluginData: SpigotSpringChildPluginData): ConfigurableApplicationContext? {
        val context = parentBuilder.child(pluginData.mainClass)
            .bannerMode(Banner.Mode.OFF)
            .web(WebApplicationType.NONE)
            .initializers(SpigotSpringChildInitializer(pluginData))
            .resourceLoader(DefaultResourceLoader(pluginData.classLoader))
            .initializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
                pluginData.getContextApplicationProperties()?.apply {
                    logger.info("Found application.yml for plugin ${pluginData.description.name}. Overriding default properties...")
                    val propertySources = it.environment.propertySources
                    propertySources.addLast(
                        PropertiesPropertySource(
                            "plugin-yaml",
                            this,
                        )
                    )
                }
            })
            .listeners(ApplicationListener<ContextRefreshedEvent> { event ->

            })
            .run()
        onPostInitialize(context)
        logger.info("Loaded Plugin ${pluginData.description.name}")

        return context
    }

    fun disablePlugin(plugin: SpringSpigotChildPlugin) {
        plugin.data.enabled = false
        plugins.remove(plugin.name)

        commandRegistry.unregisterCommands(plugin.name)

        val context = plugin.context
        if (context is ConfigurableApplicationContext) {
            context.close()
        }
    }

    fun unloadPlugin(plugin: SpringSpigotChildPlugin) {
        pluginMetas.remove(plugin.name)
        SpringSpigotBootstrapper.Unsafe.unRegisterClassLoader(plugin.data)
        disablePlugin(plugin)
        plugin.data.close()
    }

    fun unloadPluginData(pluginData: SpigotSpringChildPluginData) {
        pluginMetas.remove(pluginData.description.name)
        SpringSpigotBootstrapper.Unsafe.unRegisterClassLoader(pluginData)
        pluginData.close()
    }

    fun unloadPlugins() {
        logger.info("Unloading all plugins...")
        plugins.values.forEach { plugin ->
            logger.info("Unloading plugin ${plugin.name}...")
            val context = plugin.context
            if (context is ConfigurableApplicationContext) {
                context.close()
            }
        }
        logger.info("Unloading Third-Party plugins completed!")
    }

    private fun registerPlugin(plugin: SpringSpigotChildPlugin) {
        logger.info("Registered plugin ${plugin.name}...")
        plugins[plugin.name] = plugin
    }

    fun wireContexts(): Int {
        logger.info("Trying wire plugins...")
        var count = 0
        plugins.map {
            it.value.context.getBeansWithAnnotation(Component::class.java).map { (name, bean) ->
                val fields = getAnnotatedFields(bean)
                bean to fields
            }
        }.flatten().forEach { (bean, fields) ->
            fields.forEach { field ->
                val inst = AopProxyUtils.getSingletonTarget(bean) ?: bean
                val annotation = field.getAnnotation(WirePlugin::class.java)
                val pluginName = if (annotation.pluginName.isNotBlank())
                    annotation.pluginName
                else if (annotation.value.isNotBlank()) annotation.value
                else throw NoSuchBeanDefinitionException("Class ${field.declaringClass.simpleName} Field ${field.name} has no plugin name.")

                val targetPlugin = plugins[pluginName]
                    ?: throw NoSuchBeanDefinitionException("Class ${field.declaringClass.simpleName} Field ${field.name} Plugin $pluginName is not registered.")
                val targetBean = targetPlugin.context.getBean(field.type)
                if (!field.canAccess(bean)) throw NoSuchBeanDefinitionException("Class ${field.declaringClass.simpleName} Field ${field.name} is not visible.")

                field.set(inst, targetBean)
                count++
            }
        }
        logger.info("§f§lWired $count beans completed...")
        return count
    }

    fun onPostInitialize(applicationContext: ApplicationContext) {
        logger.info("Post-Initializing plugin...")
        val parentContext = SpringSpigotBootstrapper.Unsafe.mainContext
        val plugin = applicationContext.getBean(Plugin::class.java)

        val commandAdvices = applicationContext.getBeansWithAnnotation(
            CommandAdvice::class.java
        )
        commandAdvices.forEach { (_, beanObject) ->
            commandRegistry.registerAdvices(
                beanObject
            )
        }
        val commandControllers = applicationContext.getBeansWithAnnotation(
            CommandController::class.java
        )
        commandControllers.forEach { (_, beanObject) ->
            commandRegistry.registerCommands(
                beanObject,
                pluginName = plugin.name,
            )
        }

        val beans: Collection<Listener> = applicationContext.getBeansOfType(
            Listener::class.java
        ).values



        if (applicationContext != parentContext && plugin is SpringSpigotChildPlugin) {
            logger.info("Registering events for plugin ${plugin.name}...")
            plugin.data.enabled = true
            registerPlugin(plugin)
            overwritePlugin(plugin)
        }


        val eventService = parentContext.getBean(BukkitEventService::class.java)
        beans.forEach { listener ->
            eventService.registerEvents(
                plugin,
                listener
            )
        }
    }

    private fun overwritePlugin(plugin: Plugin) {
        val pluginManager = Bukkit.getPluginManager() as SimplePluginManager
        SimplePluginManager::class.java.getDeclaredField("lookupNames").apply {
            isAccessible = true
            (get(pluginManager) as HashMap<String, Plugin>)[plugin.name] = plugin
        }
        SimplePluginManager::class.java.getDeclaredField("plugins").apply {
            isAccessible = true
            (get(pluginManager) as ArrayList<Plugin>).apply {
                removeIf { it.name == plugin.name }
                add(plugin)
            }
        }
    }

    private fun getAnnotatedFields(obj: Any): List<Field> {
        val target = AopUtils.getTargetClass(obj)
        return target.declaredFields.filter { it.isAnnotationPresent(WirePlugin::class.java) }
    }
}
