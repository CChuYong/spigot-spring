package chuyong.springspigot.util

import chuyong.springspigot.child.SpigotSpringChildPluginData
import io.papermc.paper.plugin.configuration.PluginMeta
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.logging.Logger

class PaperPluginClassLoader(
    urls: Array<URL>,
    parent: ClassLoader?,
    val thirdPartyLibraryLoader: CompoundClassLoader,
    val tempDataMap: Map<String, SpigotSpringChildPluginData>,
): URLClassLoader(urls, parent), ConfiguredPluginClassLoader {
    override fun close() {
        this.close()
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
//        try{
//            return thirdPartyLibraryLoader.loadClassSafe(name)
//        }catch(ex: ClassNotFoundException) {
//
//        }

        try{
            return thirdPartyLibraryLoader.loadClass(name)
        }catch(ex: ClassNotFoundException) {

        }

        try {
            return super.loadClass(name, resolve)
        } catch (ex: ClassNotFoundException) {
        }



        throw ClassNotFoundException(name)
    }


    override fun getConfiguration(): PluginMeta {
        TODO("Not yet implemented")
    }

    override fun loadClass(name: String, resolve: Boolean, checkGlobal: Boolean, checkLibraries: Boolean): Class<*> {
        TODO("Not yet implemented")
    }

    override fun init(plugin: JavaPlugin) {
        val data = tempDataMap.filter { Class.forName(it.key, true, this).isAssignableFrom(plugin::class.java) }.firstNotNullOf { it.value }
      //  val data = tempDataMap[plugin.name]!!
        JavaPlugin::class.java.getDeclaredMethod("init", Server::class.java, PluginDescriptionFile::class.java, File::class.java, File::class.java, ClassLoader::class.java, PluginMeta::class.java, Logger::class.java).apply {
            isAccessible = true
            invoke(plugin, Bukkit.getServer(), data.description, data.dataFolder, data.file, this@PaperPluginClassLoader, data.meta, Bukkit.getLogger())
        }
    }

    override fun getPlugin(): JavaPlugin? {
        TODO("Not yet implemented")
    }

    override fun getGroup(): PluginClassLoaderGroup? {
        TODO("Not yet implemented")
    }
}
