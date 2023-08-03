package chuyong.springspigot.child

import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.PluginLoader
import org.bukkit.plugin.RegisteredListener
import org.springframework.context.support.AbstractApplicationContext
import java.io.File
import java.util.regex.Pattern

class SpigotSpringChildPluginLoader(
    val plugin: SpringSpigotChildPlugin,
): PluginLoader {
    override fun loadPlugin(file: File): Plugin {
        return plugin
    }

    override fun getPluginDescription(file: File): PluginDescriptionFile {
        return plugin.description
    }

    override fun getPluginFileFilters(): Array<Pattern> {
        return arrayOf()
    }

    override fun createRegisteredListeners(
        listener: Listener,
        plugin: Plugin
    ): MutableMap<Class<out Event>, MutableSet<RegisteredListener>> {
        return mutableMapOf()
    }

    override fun enablePlugin(plugin: Plugin) {

    }

    override fun disablePlugin(plugin: Plugin) {
        plugin.logger.info("Closing plugin ${plugin.name}...")
        if(this.plugin.context is AbstractApplicationContext) {
            (this.plugin.context as AbstractApplicationContext).close()
        }
    }
}
