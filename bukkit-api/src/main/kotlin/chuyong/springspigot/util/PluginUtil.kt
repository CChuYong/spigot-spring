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
import org.bukkit.command.PluginCommand
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
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.util.*


class PluginUtil {
    companion object {
        fun unloadPluginPaper(plugin: Plugin) {
            Bukkit.getPluginManager().disablePlugin(plugin)
            val plugins: MutableList<Plugin>
            val lookupNames: MutableMap<String, Plugin>
            val commandMap: SimpleCommandMap
            val knownCommands: MutableMap<String, Command>
            val listeners: Map<Event?, SortedSet<RegisteredListener>>?
            var pluginContainer: Any = Bukkit.getPluginManager()
            try {
                val paperPluginManager: Field = Bukkit.getServer().javaClass.getDeclaredField("paperPluginManager")
                paperPluginManager.isAccessible = true
                pluginContainer = paperPluginManager.get(Bukkit.getServer())
                val instanceManager: Field = pluginContainer.javaClass.getDeclaredField("instanceManager")
                instanceManager.isAccessible = true
                pluginContainer = instanceManager.get(pluginContainer)
            } catch (ignored: Throwable) {
            }
            plugins = try { //Get plugins list
                val f: Field = pluginContainer.javaClass.getDeclaredField("plugins")
                f.isAccessible = true
                f.get(pluginContainer) as MutableList<Plugin>
            } catch (e: Throwable) {
                e.printStackTrace()
                throw IllegalStateException()
            }
            lookupNames = try { //Get lookup names
                val f: Field = pluginContainer.javaClass.getDeclaredField("lookupNames")
                f.isAccessible = true
                f.get(pluginContainer) as MutableMap<String, Plugin>
            } catch (e: Throwable) {
                e.printStackTrace()
                throw IllegalStateException()
            }
            commandMap = try { //Get command map
                val f: Field = pluginContainer.javaClass.getDeclaredField("commandMap")
                f.isAccessible = true
                f.get(pluginContainer) as SimpleCommandMap
            } catch (e: Throwable) {
                e.printStackTrace()
                throw IllegalStateException()
            }
            knownCommands = try { //Get known commands
                val f: Field = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
                f.isAccessible = true
                f.get(commandMap) as MutableMap<String, Command>
            } catch (e: Throwable) {
                e.printStackTrace()
                throw IllegalStateException()
            }
            listeners = try {
                val f: Field = pluginContainer.javaClass.getDeclaredField("listeners")
                f.isAccessible = true
                f.get(pluginContainer) as Map<Event?, SortedSet<RegisteredListener>>?
            } catch (e: Throwable) {
                null
            }
            plugins.remove(plugin)
            lookupNames.remove(plugin.name)
            lookupNames.remove(plugin.name.lowercase(Locale.getDefault()))
            run {
                //Remove plugin commands
                val iterator: MutableIterator<Map.Entry<String, Command>> =
                    knownCommands.entries.iterator()
                while (iterator.hasNext()) {
                    val (_, value) = iterator.next()
                    if (value is PluginCommand) {
                        val command: PluginCommand = value as PluginCommand
                        if (command.getPlugin().equals(plugin)) iterator.remove()
                    }
                }
            }
            if (listeners != null) {
                for (registeredListeners in listeners.values) {
                    registeredListeners.removeIf { registeredListener: RegisteredListener -> registeredListener.plugin == plugin }
                }
            }
            try {
                val entryPointHandler = Class.forName("io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler")
                val instance = entryPointHandler.getDeclaredField("INSTANCE")[null]
                val storage = instance.javaClass.getMethod("getStorage").invoke(instance) as Map<*, *>
                for (providerStorage in storage.values) {
                    val providers = providerStorage!!.javaClass.getMethod("getRegisteredProviders")
                        .invoke(providerStorage) as MutableIterable<*>
                    val it = providers.iterator()
                    while (it.hasNext()) {
                        val provider = it.next()!!
                        val meta = provider.javaClass.getMethod("getMeta").invoke(provider)
                        val metaName = meta.javaClass.getMethod("getName").invoke(meta) as String
                        if (metaName == plugin.name) it.remove()
                    }
                }
            } catch (ignored: Throwable) {
            }
            if (plugin.javaClass.classLoader is URLClassLoader) {
                val classLoader = plugin.javaClass.classLoader as URLClassLoader
                try {
                    classLoader.close()
                } catch (t: Throwable) {
                    throw IllegalStateException()
                }
            }
            try { //Synchronize the commands between client/server on newer versions
                val syncCommands: Method = Bukkit.getServer().javaClass.getDeclaredMethod("syncCommands")
                syncCommands.isAccessible = true
                syncCommands.invoke(Bukkit.getServer())
            } catch (ignored: Throwable) {
            }
            System.gc() //Hopefully remove all leftover plugin classes and references
        }

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
