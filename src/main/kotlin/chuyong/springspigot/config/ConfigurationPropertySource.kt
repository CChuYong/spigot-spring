package chuyong.springspigot.config

import org.bukkit.configuration.file.FileConfiguration
import org.springframework.core.env.PropertySource

internal class ConfigurationPropertySource(source: FileConfiguration?) :
    PropertySource<FileConfiguration>("config", source!!) {
    override fun getProperty(s: String): Any? {
        return source[s]
    }
}
