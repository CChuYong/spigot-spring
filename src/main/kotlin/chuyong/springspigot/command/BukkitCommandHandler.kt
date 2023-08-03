package chuyong.springspigot.command

import chuyong.springspigot.command.annotation.CommandExceptionHandler
import chuyong.springspigot.command.annotation.CommandMapping
import chuyong.springspigot.command.data.CommandConfig
import chuyong.springspigot.command.data.ExceptionHandlerWrapper
import chuyong.springspigot.command.data.SuperCommandConfig
import jakarta.annotation.PostConstruct
import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.springframework.aop.support.AopUtils
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method

@Component
class BukkitCommandHandler {
    private val mainCMD = HashMap<String, BukkitCommandImpl>()
    var exceptionHandlers = HashMap<Class<out Throwable>, ExceptionHandlerWrapper>()

    fun registerAdvices(obj: Any?) {
        try {
            val commandClazz = AopUtils.getTargetClass(obj!!)
            for (mt in ReflectionUtils.getAllDeclaredMethods(commandClazz)) {
                if (mt.isAnnotationPresent(CommandExceptionHandler::class.java)) {
                    val annotation = mt.getAnnotation(
                        CommandExceptionHandler::class.java
                    )
                    for (aClass in annotation.value) {
                        exceptionHandlers[aClass.java] = ExceptionHandlerWrapper(mt, obj)
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun registerCommands(cls: Any) {
        try {
            val commandClazz = AopUtils.getTargetClass(cls)
            var baseConfig: CommandMapping? = null
            if (commandClazz.isAnnotationPresent(CommandMapping::class.java)) {
                baseConfig = commandClazz.getAnnotation(CommandMapping::class.java)
                registerParentCommand(baseConfig)
            }
            for (mt in ReflectionUtils.getAllDeclaredMethods(commandClazz)) {
                val at = mt.getAnnotation(CommandMapping::class.java)
                if (at != null) {
                    if (baseConfig == null) {
                        //parent가 없을때
                        registerChildCommands(at.value, arrayOf(at.child), CommandConfig.fromAnnotation(at), mt, cls)
                    } else {
                        //class parent가 있을때
                        registerChildCommands(
                            baseConfig.value,
                            arrayOf(baseConfig.child, at.value, at.child),
                            CommandConfig.fromAnnotation(at),
                            mt,
                            cls
                        )
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun registerParentCommand(ano: CommandMapping): BukkitCommandImpl? {
        if (ano.value == "" && ano.child == "") throw RuntimeException("Cannot Register non-named class commands")
        return if (mainCMD.containsKey(ano.value)) mainCMD[ano.value] else try {
            val bukkitCommandMap = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            bukkitCommandMap.isAccessible = true
            val commandMap = bukkitCommandMap[Bukkit.getServer()] as CommandMap
            val a = BukkitCommandImpl(ano.value, SuperCommandConfig.fromAnnotation(ano))
            commandMap.register(ano.value, a)
            mainCMD[ano.value] = a
            if (a.aliases.size < ano.aliases.size) {
                a.aliases = listOf(*ano.aliases)
            }
            a
        } catch (ex: Exception) {
            throw RuntimeException()
        }
    }

    private fun registerChildCommands(
        parentKey: String,
        childKey: Array<String>,
        ano: CommandConfig,
        mtd: Method,
        cl: Any,
    ): BukkitCommandImpl {
        return try {
            val bukkitCommandMap = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            bukkitCommandMap.isAccessible = true
            val commandMap = bukkitCommandMap[Bukkit.getServer()] as CommandMap
            var bukkitCommand = mainCMD[parentKey]
            if (bukkitCommand == null) {
                bukkitCommand = BukkitCommandImpl(parentKey)
                commandMap.register(parentKey, bukkitCommand)
                mainCMD[parentKey] = bukkitCommand
            }
            val container = bukkitCommand.addCommand(childKey, ano, mtd, cl)
            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §a/" + container.fullKey + " §f§lCommand Successfully Initialized")
            bukkitCommand
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException()
        }
    }
}
