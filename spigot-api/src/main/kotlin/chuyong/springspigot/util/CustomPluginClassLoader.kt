package chuyong.springspigot.util

import chuyong.springspigot.child.SpigotSpringChildPluginData
import org.bukkit.plugin.java.JavaPluginLoader
import org.bukkit.plugin.java.PluginClassLoader

class CustomPluginClassLoader(
    parented: ClassLoader,
    loader: JavaPluginLoader,
    val data: SpigotSpringChildPluginData,
    customClassLoader: MultiClassLoader,
) : PluginClassLoader(
    loader,
    parented,
    data.description.apply {
        val pdf = this
        this::class.java.getDeclaredField("main").apply {
            isAccessible = true
            set(pdf, "chuyong.springspigot.MockMain")
        }
    },
    data.dataFolder,
    data.file,
    null,
) {
    val myClazzLoader: MultiClassLoader

    init {
        myClazzLoader = customClassLoader
    }

    override fun loadClass0(name: String, resolve: Boolean, checkGlobal: Boolean, checkLibraries: Boolean): Class<*> {
        try {
            return myClazzLoader.readSelf(name, resolve)
        } catch (e: Exception) {
        }

        try {
            return super.loadClass0(name, resolve, checkGlobal, checkLibraries)
        } catch (e: ClassNotFoundException) {
        }
        throw ClassNotFoundException("Cannot find ${name}")
    }


    companion object {
        init {
            ClassLoader.registerAsParallelCapable()
        }
    }


}
