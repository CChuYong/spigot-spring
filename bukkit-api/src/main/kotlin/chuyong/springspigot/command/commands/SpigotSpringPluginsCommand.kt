package chuyong.springspigot.command.commands

import chuyong.springspigot.child.SpringSpigotPluginRegistry
import chuyong.springspigot.command.annotation.CommandController
import chuyong.springspigot.command.annotation.CommandMapping
import org.bukkit.command.CommandSender

@CommandController
@CommandMapping("ss", child = "plugins", console = true, op = true)
class SpigotSpringPluginsCommand(
    private val pluginRegistry: SpringSpigotPluginRegistry,
) {
    @CommandMapping("list", console = true)
    fun onPluginListCommand(sender: CommandSender) {
        val plugins = pluginRegistry.getPluginMetas()
        plugins.joinToString("§7, ") {
            if (it.enabled) {
                "§a${it.description.name}"
            } else {
                "§c${it.description.name}"
            }
        }.let {
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugins: $it")
        }
    }

    @CommandMapping("unload", minArgs = 1, maxArgs = 1, console = true)
    fun unloadPlugin(sender: CommandSender, args: Array<String>) {
        val pluginMeta = pluginRegistry.getPluginMeta(args[0])
        if (pluginMeta == null) {
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin not found.")
            return
        }

        try {
            val plugin = pluginRegistry.getPlugin(args[0])
            if (plugin != null) {
                pluginRegistry.unloadPlugin(plugin)
            } else {
                pluginRegistry.unloadPluginData(pluginMeta)
            }

            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin ${pluginMeta.description.name} successfully unloaded.")
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendMessage("§7[§bSpringSpigot§7] §fAn error occured while unloading plugin.")
        }

    }

    @CommandMapping("disable", minArgs = 1, maxArgs = 1, console = true)
    fun disablePlugin(sender: CommandSender, args: Array<String>) {
        val pluginMeta = pluginRegistry.getPluginMeta(args[0])
        if (pluginMeta == null) {
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin not found.")
            return
        }

        if (!pluginMeta.enabled) {
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin already disabled.")
            return
        }

        try {
            val plugin = pluginRegistry.getPlugin(args[0])!!
            pluginRegistry.disablePlugin(plugin)
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin ${plugin.name} successfully disabled.")
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendMessage("§7[§bSpringSpigot§7] §fAn error occured while disabling plugin.")
        }

    }

    @CommandMapping("enable", console = true, minArgs = 1, maxArgs = 1)
    fun enablePlugin(sender: CommandSender, args: Array<String>) {
        val pluginMeta = pluginRegistry.getPluginMeta(args[0])
        if (pluginMeta == null) {
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin not found.")
            return
        }

        if (pluginMeta.enabled) {
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin already enabled.")
            return
        }

        try {
            pluginRegistry.enablePlugin(pluginMeta)
            pluginRegistry.wireContexts()
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin ${pluginMeta.description.name} successfully enabled.")
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendMessage("§7[§bSpringSpigot§7] §fAn error occured while enabling plugin.")
        }

    }

    @CommandMapping("reload", console = true, minArgs = 1, maxArgs = 1)
    fun reloadPlugin(sender: CommandSender, args: Array<String>) {
        val pluginMeta = pluginRegistry.getPluginMeta(args[0])
        if (pluginMeta == null) {
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin not found.")
            return
        }

        if (!pluginMeta.enabled) {
            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin not enabled.")
            return
        }

        try {
            val plugin = pluginRegistry.getPlugin(args[0])
            if (plugin != null) {
                pluginRegistry.unloadPlugin(plugin)
            } else {
                pluginRegistry.unloadPluginData(pluginMeta)
            }
            System.gc()

            val newMeta = pluginMeta.rebuildNew()
            pluginRegistry.loadPlugin(newMeta)

            pluginRegistry.wireContexts()


            sender.sendMessage("§7[§bSpringSpigot§7] §fPlugin ${pluginMeta.description.name} successfully reloaded.")
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendMessage("§7[§bSpringSpigot§7] §fAn error occured while enabling plugin.")
        }

    }
}
