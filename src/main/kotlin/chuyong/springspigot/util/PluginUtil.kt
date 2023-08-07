package chuyong.springspigot.util

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.SimpleCommandMap
import org.bukkit.event.Event
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.RegisteredListener
import java.io.IOException
import java.net.URLClassLoader
import java.util.*


class PluginUtil {
    fun unloadPlugin(plugin: Plugin) {
        val name: String = plugin.name

        val pluginManager = Bukkit.getPluginManager()
        var commandMap: SimpleCommandMap? = null
        var plugins: MutableList<Plugin?>? = null
        var names: MutableMap<String?, Plugin?>? = null
        var commands: Map<String?, Command?>? = null
        var listeners: Map<Event?, SortedSet<RegisteredListener?>?>? = null
        var reloadlisteners = true
        pluginManager.disablePlugin(plugin)
        try {
            val pluginsField = pluginManager::class.java.getDeclaredField("plugins")
            pluginsField.setAccessible(true)
            plugins = pluginsField.get(pluginManager) as MutableList<Plugin?>?
            val lookupNamesField = pluginManager::class.java.getDeclaredField("lookupNames")
            lookupNamesField.setAccessible(true)
            names = lookupNamesField.get(pluginManager) as MutableMap<String?, Plugin?>?
            try {
                val listenersField = pluginManager::class.java.getDeclaredField("listeners")
                listenersField.setAccessible(true)
                listeners = listenersField.get(pluginManager) as Map<Event?, SortedSet<RegisteredListener?>?>?
            } catch (e: Exception) {
                reloadlisteners = false
            }
            val commandMapField = pluginManager::class.java.getDeclaredField("commandMap")
            commandMapField.setAccessible(true)
            commandMap = commandMapField.get(pluginManager) as SimpleCommandMap
            val knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
            knownCommandsField.setAccessible(true)
            commands = knownCommandsField.get(commandMap) as Map<String?, Command?>?
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pluginManager.disablePlugin(plugin)
        if (listeners != null && reloadlisteners) for (set in listeners.values) set?.removeIf { value -> value?.plugin === plugin }
        if (plugins != null && plugins.contains(plugin)) plugins.remove(plugin)
        if (names != null && names.containsKey(name)) names.remove(name)


        val cl: ClassLoader = plugin::class.java.getClassLoader()
        if (cl is URLClassLoader) {
            try {
                val pluginField = cl.javaClass.getDeclaredField("plugin")
                pluginField.setAccessible(true)
                pluginField.set(cl, null)
                val pluginInitField = cl.javaClass.getDeclaredField("pluginInit")
                pluginInitField.setAccessible(true)
                pluginInitField.set(cl, null)
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
            try {
                cl.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }

        System.gc()
    }
}
