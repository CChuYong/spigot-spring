package chuyong.springspigot.config

import org.bukkit.configuration.file.FileConfiguration
import org.springframework.core.env.PropertySource

class BukkitConfigPropertySource(source: FileConfiguration?) :
    PropertySource<FileConfiguration>("config", source!!) {
    override fun getProperty(s: String): Any? {
        return source[s]
    }
}
