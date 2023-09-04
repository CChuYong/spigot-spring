package chuyong.springspigot.child

import chuyong.springspigot.EnableEscalatedSpringSpigotSupport
import chuyong.springspigot.util.PluginUtil
import chuyong.springspigot.util.SpringSpigotContextClassLoader
import chuyong.springspigot.util.YamlPropertiesFactory
import com.google.common.base.Charsets
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.springframework.core.io.FileSystemResource
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLClassLoader
import java.util.*

data class SpigotSpringChildPluginData(
    var enabled: Boolean,
    val file: File,
    val description: PluginDescriptionFile,
    val dataFolder: File,
    private val newConfig: FileConfiguration?,
    val configFile: File,
    val libraryUrls: Array<URL>,
) {
    var classLoader: SpringSpigotContextClassLoader? = null
    lateinit var mainClass: Class<*>
    var isEscalated = false
    var actualConfig = newConfig ?: Unit.let {
        val newConfig = YamlConfiguration.loadConfiguration(configFile)
        val defConfigStream = getResource("config.yml") ?: return@let null
        newConfig.setDefaults(YamlConfiguration.loadConfiguration(InputStreamReader(defConfigStream, Charsets.UTF_8)))
        return@let newConfig
    }

    fun initLoader(parent: ClassLoader, springSpigotLoader: ClassLoader) {
        classLoader = SpringSpigotContextClassLoader(
            parent = parent,
            file = file,
            description = description,
            additional = libraryUrls,
            springSpigotLoader = springSpigotLoader,
        )
        val pluginClazz = Class.forName(description.main, true, classLoader)
        isEscalated = pluginClazz.isAnnotationPresent(EnableEscalatedSpringSpigotSupport::class.java)
        mainClass = if(isEscalated) PluginUtil.createEscaltedMockCLazz(pluginClazz, classLoader!!) else PluginUtil.createMockClazz(pluginClazz, classLoader!!)
    }

    fun getContextApplicationProperties(): Properties? {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val configurationFile = FileSystemResource(File(dataFolder, "application.yml"))
        if (!configurationFile.exists()) {
            configurationFile.file.createNewFile()
            return null
        }
        return YamlPropertiesFactory.loadYamlIntoProperties(configurationFile)!!
    }

    fun getResource(filename: String): InputStream? {
        requireNotNull(filename) { "Filename cannot be null" }

        return try {
            val url: URL = classLoader?.getResource(filename) ?: return null
            val connection = url.openConnection()
            connection.useCaches = false
            connection.getInputStream()
        } catch (ex: IOException) {
            null
        }
    }

    companion object {
        fun copyFromPlugin(plugin: JavaPlugin): SpigotSpringChildPluginData {
            val configFile = File(plugin.dataFolder, "config.yml")
            val file = JavaPlugin::class.java.getDeclaredField("file").apply {
                isAccessible = true
            }.get(plugin) as File
            return SpigotSpringChildPluginData(
                false,
                file,
                plugin.description,
                plugin.dataFolder,
                plugin.config,
                configFile,
                plugin::class.java.classLoader::class.java.getDeclaredField("libraryLoader").let {
                    it.isAccessible = true
                    val loader = (it.get(plugin::class.java.classLoader) as? URLClassLoader)
                    loader?.urLs ?: emptyArray()
                }
            )
        }
    }
}
