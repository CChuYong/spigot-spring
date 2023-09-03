package chuyong.springspigot.child

import chuyong.springspigot.SpringSpigotBootstrapper
import chuyong.springspigot.command.CommandRegistry
import chuyong.springspigot.command.annotation.CommandAdvice
import chuyong.springspigot.command.annotation.CommandController
import chuyong.springspigot.event.BukkitEventService
import io.netty.channel.nio.NioEventLoop
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.SimplePluginManager
import org.slf4j.Logger
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping

@Component
class SpringSpigotChildPostInitializer(
    private val springSpigotPluginRegistry: SpringSpigotPluginRegistry,
    private val logger: Logger,
) {
    fun onPostInitialize(applicationContext: ApplicationContext) {
        val parentContext = SpringSpigotBootstrapper.mainContext
        val commandHandler = parentContext.getBean(CommandRegistry::class.java)



        val commandAdvices = applicationContext.getBeansWithAnnotation(
            CommandAdvice::class.java
        )
        commandAdvices.forEach { (_, beanObject) ->
            commandHandler.registerAdvices(
                beanObject
            )
        }
        val commandControllers = applicationContext.getBeansWithAnnotation(
            CommandController::class.java
        )
        commandControllers.forEach { (_, beanObject) ->
            commandHandler.registerCommands(
                beanObject
            )
        }

        val beans: Collection<Listener> = applicationContext.getBeansOfType(
            Listener::class.java
        ).values

        val plugin = applicationContext.getBean(Plugin::class.java)

        if (applicationContext != parentContext) {
            logger.info("Registering events for plugin ${plugin.name}...")
            springSpigotPluginRegistry.registerPlugin(plugin as SpringSpigotChildPlugin)
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

    fun overwritePlugin(plugin: Plugin) {
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
}
