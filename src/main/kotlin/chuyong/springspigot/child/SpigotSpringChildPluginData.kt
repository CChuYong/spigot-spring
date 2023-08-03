package chuyong.springspigot.child

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

data class SpigotSpringChildPluginData(
    var enabled: Boolean,
    val file: File,
    val description: PluginDescriptionFile,
    val dataFolder: File,
    var newConfig: FileConfiguration?,
    val configFile: File,
) {
    companion object {
        fun copyFromPlugin(plugin: JavaPlugin): SpigotSpringChildPluginData {
            val configFile = File(plugin.dataFolder, "config.yml")
            val file = JavaPlugin::class.java.getDeclaredField("file").apply {
                isAccessible = true
            }.get(plugin) as File
            return SpigotSpringChildPluginData(
                true,
                file,
                plugin.description,
                plugin.dataFolder,
                plugin.config,
                configFile,
            )
        }
    }
}
