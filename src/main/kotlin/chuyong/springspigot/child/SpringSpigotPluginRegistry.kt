package chuyong.springspigot.child

import org.slf4j.Logger
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SpringSpigotPluginRegistry(
    private val logger: Logger,
) {
    val plugins = ConcurrentHashMap<String, SpringSpigotChildPlugin>()

    fun registerPlugin(plugin: SpringSpigotChildPlugin) {
        logger.info("Registered plugin ${plugin.name}...")
        plugins[plugin.name] = plugin
    }
}
