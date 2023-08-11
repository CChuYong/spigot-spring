package chuyong.springspigot.util

import chuyong.springspigot.child.SpringSpigotChildPlugin
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.SimpleCommandMap
import org.bukkit.event.Event
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.RegisteredListener
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableAsync
import java.io.IOException
import java.net.URLClassLoader
import java.util.*


class PluginUtil {
    companion object {
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
                pluginsField.isAccessible = true
                plugins = pluginsField.get(pluginManager) as MutableList<Plugin?>?
                val lookupNamesField = pluginManager::class.java.getDeclaredField("lookupNames")
                lookupNamesField.isAccessible = true
                names = lookupNamesField.get(pluginManager) as MutableMap<String?, Plugin?>?
                try {
                    val listenersField = pluginManager::class.java.getDeclaredField("listeners")
                    listenersField.isAccessible = true
                    listeners = listenersField.get(pluginManager) as Map<Event?, SortedSet<RegisteredListener?>?>?
                } catch (e: Exception) {
                    reloadlisteners = false
                }
                val commandMapField = pluginManager::class.java.getDeclaredField("commandMap")
                commandMapField.isAccessible = true
                commandMap = commandMapField.get(pluginManager) as SimpleCommandMap
                val knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
                knownCommandsField.isAccessible = true
                commands = knownCommandsField.get(commandMap) as Map<String?, Command?>?
            } catch (e: Exception) {
                e.printStackTrace()
            }
            pluginManager.disablePlugin(plugin)
            if (listeners != null && reloadlisteners) for (set in listeners.values) set?.removeIf { value -> value?.plugin === plugin }
            if (plugins != null && plugins.contains(plugin)) plugins.remove(plugin)
            if (names != null && names.containsKey(name)) names.remove(name)


            val cl: ClassLoader = plugin::class.java.classLoader
            if (cl is URLClassLoader) {
                try {
                    val pluginField = cl.javaClass.getDeclaredField("plugin")
                    pluginField.isAccessible = true
                    pluginField.set(cl, null)
                    val pluginInitField = cl.javaClass.getDeclaredField("pluginInit")
                    pluginInitField.isAccessible = true
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

        fun createMockClazz(pluginClazz: Class<*>, classLoader: ClassLoader): Class<*> {
            val pluginConstructor = SpringSpigotChildPlugin::class.java.getConstructor()
            val newPluginClazz = ByteBuddy()
                .subclass(SpringSpigotChildPlugin::class.java, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(pluginClazz.name + "\$ByteBuddy")
                .annotateType(
                    AnnotationDescription
                        .Builder
                        .ofType(SpringBootApplication::class.java)
                        .defineArray("scanBasePackages", *arrayOf(pluginClazz.`package`.name))
                        .build()
                )
                .annotateType(
                    AnnotationDescription
                        .Builder
                        .ofType(Primary::class.java)
                        .build()
                )
                .annotateType(
                    AnnotationDescription
                        .Builder
                        .ofType(EnableAsync::class.java)
                        .build()
                )
                .annotateType(
                    AnnotationDescription
                        .Builder
                        .ofType(EnableAspectJAutoProxy::class.java)
                        .build()
                )
                .defineConstructor(Visibility.PUBLIC).intercept(MethodCall.invoke(pluginConstructor))
                .method(ElementMatchers.isDeclaredBy(pluginClazz))
                .intercept(MethodDelegation.to(pluginClazz))
                .make()

            return newPluginClazz.load(classLoader).loaded
        }

        fun createEscaltedMockCLazz(pluginClazz: Class<*>, classLoader: ClassLoader): Class<*> {
            val pluginConstructor = SpringSpigotChildPlugin::class.java.getConstructor()
            val newPluginClazz = ByteBuddy()
                .subclass(SpringSpigotChildPlugin::class.java, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(pluginClazz.name + "\$ByteBuddy")
                .annotateType(
                    AnnotationDescription
                        .Builder
                        .ofType(ComponentScan::class.java)
                        .defineArray("basePackages", *arrayOf(pluginClazz.`package`.name))
                        .build()
                )
                .defineConstructor(Visibility.PUBLIC).intercept(MethodCall.invoke(pluginConstructor))
                .method(ElementMatchers.isDeclaredBy(pluginClazz))
                .intercept(MethodDelegation.to(pluginClazz))
                .make()

            return newPluginClazz.load(classLoader).loaded
        }
    }
}
