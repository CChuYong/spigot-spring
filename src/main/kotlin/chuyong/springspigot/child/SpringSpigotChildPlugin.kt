package chuyong.springspigot.child

import com.google.common.base.Charsets
import jakarta.annotation.PostConstruct
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.PluginLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import java.io.*
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger

open class SpringSpigotChildPlugin: Plugin {
    @Autowired
    lateinit var data: SpigotSpringChildPluginData

    @Autowired
    lateinit var context: ApplicationContext

    val pluginLoader = SpigotSpringChildPluginLoader(this)

    @PostConstruct
    fun onPluginLoad() {

    }

    override fun onTabComplete(
        p0: CommandSender,
        p1: Command,
        p2: String,
        p3: Array<out String>,
    ): MutableList<String>? {
        return mutableListOf()
    }

    override fun onCommand(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): Boolean {
        return false
    }

    override fun getDataFolder(): File {
        return data.dataFolder
    }

    override fun getDescription(): PluginDescriptionFile {
        return data.description
    }

    @Bean
    override fun getConfig(): FileConfiguration {
        if (data.newConfig == null) {
            reloadConfig()
        }
        return data.newConfig!!
    }

    override fun getResource(filename: String): InputStream? {
        requireNotNull(filename) { "Filename cannot be null" }

        return try {
            val url: URL = javaClass.classLoader.getResource(filename) ?: return null
            val connection = url.openConnection()
            connection.useCaches = false
            connection.getInputStream()
        } catch (ex: IOException) {
            null
        }
    }

    override fun saveConfig() {
        try {
            config.save(data.configFile)
        } catch (ex: IOException) {
            logger.log(Level.SEVERE, "Could not save config to ${data.configFile}", ex)
        }
    }

    override fun saveDefaultConfig() {
        if (!data.configFile.exists()) {
            saveResource("config.yml", false)
        }
    }

    override fun saveResource(tempResourcePath: String, replace: Boolean) {
        require(tempResourcePath != "") { "ResourcePath cannot be null or empty" }

        val resourcePath = tempResourcePath.replace('\\', '/')
        val `in` = getResource(resourcePath)
            ?: throw IllegalArgumentException("The embedded resource '$resourcePath' cannot be found in $${data.file}")

        val outFile = File(dataFolder, resourcePath)
        val lastIndex: Int = resourcePath.lastIndexOf('/')
        val outDir = File(dataFolder, resourcePath.substring(0, if (lastIndex >= 0) lastIndex else 0))

        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        try {
            if (!outFile.exists() || replace) {
                val out: OutputStream = FileOutputStream(outFile)
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
                out.close()
                `in`.close()
            } else {
                logger.log(
                    Level.WARNING,
                    "Could not save " + outFile.name + " to " + outFile + " because " + outFile.name + " already exists."
                )
            }
        } catch (ex: IOException) {
            logger.log(Level.SEVERE, "Could not save " + outFile.name + " to " + outFile, ex)
        }
    }

    override fun reloadConfig() {
        data.newConfig = YamlConfiguration.loadConfiguration(data.configFile)

        val defConfigStream = getResource("config.yml") ?: return

        data.newConfig!!.setDefaults(YamlConfiguration.loadConfiguration(InputStreamReader(defConfigStream, Charsets.UTF_8)))
    }

    override fun getPluginLoader(): PluginLoader {
        return pluginLoader
    }

    override fun getServer(): Server {
        return context.getBean(Server::class.java)
    }

    override fun isEnabled(): Boolean {
        return data.enabled
    }

    override fun onDisable() {

    }

    override fun onLoad() {

    }

    override fun onEnable() {

    }

    override fun isNaggable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setNaggable(p0: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getDefaultWorldGenerator(p0: String, p1: String?): ChunkGenerator? {
        TODO("Not yet implemented")
    }

    override fun getDefaultBiomeProvider(p0: String, p1: String?): BiomeProvider? {
        TODO("Not yet implemented")
    }

    @Bean
    override fun getLogger(): Logger {
        return Bukkit.getLogger()
    }

    override fun getName(): String {
        return data.description.name
    }
}
