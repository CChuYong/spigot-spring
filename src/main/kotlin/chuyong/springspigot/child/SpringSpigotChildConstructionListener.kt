package chuyong.springspigot.child

import chuyong.springspigot.SpringSpigotBootstrapper
import chuyong.springspigot.command.BukkitCommandHandler
import chuyong.springspigot.command.annotation.CommandAdvice
import chuyong.springspigot.command.annotation.CommandController
import chuyong.springspigot.event.EventService
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.SimplePluginManager
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class SpringSpigotChildConstructionListener(
    private val springSpigotPluginRegistry: SpringSpigotPluginRegistry,
): ApplicationListener<ContextRefreshedEvent> {

    companion object {
        private val initializedContext = HashSet<ApplicationContext>();
    }

    @EventListener
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if(!initializedContext.add(event.applicationContext)) return

        val parentContext = SpringSpigotBootstrapper.mainContext
        val commandHandler = parentContext.getBean(BukkitCommandHandler::class.java)


        val commandAdvices =  event.applicationContext.getBeansWithAnnotation(
            CommandAdvice::class.java
        )
        commandAdvices.forEach { (t: String?, ob: Any?) ->
            commandHandler.registerAdvices(
                ob
            )
        }
        val commandControllers = event.applicationContext.getBeansWithAnnotation(
            CommandController::class.java
        )
        commandControllers.forEach { (t: String?, ob: Any?) ->
            commandHandler.registerCommands(
                ob
            )
        }

        val beans: Collection<Listener> =  event.applicationContext.getBeansOfType(
            Listener::class.java
        ).values

        val plugin = event.applicationContext.getBean(Plugin::class.java)

        if(event.applicationContext != parentContext) {
            springSpigotPluginRegistry.registerPlugin(plugin as SpringSpigotChildPlugin)
            overwritePlugin(plugin)
            println("Registering events for plugin ${plugin.name}...")
        }

        val eventService = parentContext.getBean(EventService::class.java)
        beans.forEach{ listener->
            eventService.registerEvents(
                plugin,
                listener
            )
        }
    }

    fun overwritePlugin(plugin: Plugin){
        val pluginManager = Bukkit.getPluginManager() as SimplePluginManager
        SimplePluginManager::class.java.getDeclaredField("lookupNames").apply {
            isAccessible = true
            (get(pluginManager) as HashMap<String, Plugin>).put(plugin.name, plugin)
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
