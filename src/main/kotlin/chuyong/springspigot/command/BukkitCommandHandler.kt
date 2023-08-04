package chuyong.springspigot.command

import chuyong.springspigot.command.annotation.CommandExceptionHandler
import chuyong.springspigot.command.annotation.CommandMapping
import chuyong.springspigot.command.data.CommandConfig
import chuyong.springspigot.command.data.ExceptionHandlerWrapper
import chuyong.springspigot.command.data.SuperCommandConfig
import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.slf4j.Logger
import org.springframework.aop.support.AopUtils
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method

@Component
class BukkitCommandHandler(
    private val exceptionHandler: BaseCommandExceptionHandler,
    private val logger: Logger,
) {
    private val mainCMD = HashMap<String, BukkitCommandImpl>()

    fun registerAdvices(beanObject: Any) {
        val commandClazz = AopUtils.getTargetClass(beanObject)
        for (method in ReflectionUtils.getAllDeclaredMethods(commandClazz)) {
            if (method.isAnnotationPresent(CommandExceptionHandler::class.java)) {
                val annotation = method.getAnnotation(
                    CommandExceptionHandler::class.java
                )
                for (throwableClazz in annotation.value) {
                    exceptionHandler.registerExceptionHandler(
                        throwableClazz = throwableClazz.java,
                        handler = ExceptionHandlerWrapper(method, beanObject)
                    )
                }
            }
        }

    }

    fun registerCommands(beanObject: Any) {
        val commandClazz = AopUtils.getTargetClass(beanObject)
        var baseConfig: CommandMapping? = null
        if (commandClazz.isAnnotationPresent(CommandMapping::class.java)) {
            baseConfig = commandClazz.getAnnotation(CommandMapping::class.java)
            registerParentCommand(baseConfig)
        }
        for (method in ReflectionUtils.getAllDeclaredMethods(commandClazz)) {
            val commandMapping = method.getAnnotation(CommandMapping::class.java)
            if (commandMapping != null) {
                if (baseConfig == null) {
                    //parent가 없을때
                    registerChildCommands(
                        commandMapping.value,
                        arrayOf(commandMapping.child),
                        CommandConfig.fromAnnotation(commandMapping),
                        method,
                        beanObject
                    )
                } else {
                    //class parent가 있을때
                    registerChildCommands(
                        baseConfig.value,
                        arrayOf(baseConfig.child, commandMapping.value, commandMapping.child),
                        CommandConfig.fromAnnotation(commandMapping),
                        method,
                        beanObject
                    )
                }
            }
        }

    }

    private fun registerParentCommand(ano: CommandMapping): BukkitCommandImpl {
        if (ano.value == "" && ano.child == "") throw RuntimeException("Cannot Register non-named class commands")
        return if (mainCMD.containsKey(ano.value)) mainCMD[ano.value]!!
        else {
            val bukkitCommandMap = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            bukkitCommandMap.isAccessible = true
            val commandMap = bukkitCommandMap[Bukkit.getServer()] as CommandMap
            val command = BukkitCommandImpl(ano.value, SuperCommandConfig.fromAnnotation(ano))
            commandMap.register(ano.value, command)
            mainCMD[ano.value] = command
            if (command.aliases.size < ano.aliases.size) {
                command.aliases = listOf(*ano.aliases)
            }
            command
        }
    }

    private fun registerChildCommands(
        parentKey: String,
        childKey: Array<String>,
        ano: CommandConfig,
        method: Method,
        beanObject: Any,
    ): BukkitCommandImpl {
        val bukkitCommandMap = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        bukkitCommandMap.isAccessible = true
        val commandMap = bukkitCommandMap[Bukkit.getServer()] as CommandMap
        var bukkitCommand = mainCMD[parentKey]
        if (bukkitCommand == null) {
            bukkitCommand = BukkitCommandImpl(parentKey)
            commandMap.register(parentKey, bukkitCommand)
            mainCMD[parentKey] = bukkitCommand
        }
        val container = bukkitCommand.addCommand(childKey, ano, method, beanObject)
        logger.info("§a/${container.fullKey} §f§lCommand Successfully Initialized")
        return bukkitCommand
    }
}
