package chuyong.springspigot.command

import chuyong.springspigot.command.CommandContext.Companion.clearContext
import chuyong.springspigot.command.CommandContext.Companion.currentContext
import chuyong.springspigot.command.data.CommandConfig
import chuyong.springspigot.command.data.SuperCommandConfig
import org.bukkit.command.CommandSender
import org.bukkit.command.defaults.BukkitCommand
import org.bukkit.entity.Player
import java.lang.reflect.Method
import java.util.*

class BukkitCommandImpl(baseLabel: String?) : BukkitCommand(baseLabel!!) {
    val primaryContainer = SubCommandContainer(null, label, 0)
    var baseConfig: SuperCommandConfig? = null

    init {
        description = ""
        usageMessage = ""
        permission = ""
        aliases = ArrayList()
    }

    constructor(baseLabel: String?, baseConfig: SuperCommandConfig?) : this(baseLabel) {
        this.baseConfig = baseConfig
    }

    private fun getContainer(args: Array<String>): SubCommandContainer? {
        return primaryContainer.getContainer(LinkedList(listOf(*args)))
    }

    private fun getTapCompleteContainer(args: Array<String>): SubCommandContainer {
        return primaryContainer.getTapCompleteContainer(LinkedList(listOf(*args)))
    }

    fun addCommand(subcommand: Array<String>, ano: CommandConfig?, mtd: Method?, cl: Any?): SubCommandContainer {
        val commandList = LinkedList(Arrays.asList(*subcommand))
        commandList.removeIf { element: String -> element == "" }
        return primaryContainer.addCommand(commandList, ano, mtd, cl)
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
        val sc = getContainer(args)
        if (sc == null || !sc.isImplemented) {
            // System.out.println("UNKNOWN COMMAND");
            return false
        }
        val copiedArray = arrayOfNulls<String>(args.size - sc.commandDepth)
        System.arraycopy(args, sc.commandDepth, copiedArray, 0, copiedArray.size)
        val config = sc.config
        if (!(copiedArray.size >= config.minArgs && copiedArray.size <= config.maxArgs)) {
            sender.sendMessage(getPrefix(config) + config.usage)
            return false
        }
        if (checkPermValid(sender, sc.config)) {
            executeMethod(sc, sender, copiedArray)
        }
        return false
    }

    @Throws(IllegalArgumentException::class)
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): List<String> {
        val sc = getTapCompleteContainer(args)
        val keys = sc.childCommandKeys()
        if (keys.isEmpty() && sc.config.defaultSuggestion) {
            return super.tabComplete(sender, alias, args)
        }
        if (args.size != 0) {
            val lastArgs = args[args.size - 1]
            return keys.stream().filter { key: String -> key.startsWith(lastArgs) }.toList()
        }
        return keys.stream().toList()
    }

    private fun checkPermValid(sender: CommandSender, commandConfig: CommandConfig): Boolean {
        if (opCondition(commandConfig) && !sender.isOp) {
            sender.sendMessage(getPrefix(commandConfig) + noPermMessage(commandConfig))
            return false
        }
        if (!consoleCondition(commandConfig) && sender !is Player) {
            sender.sendMessage(getPrefix(commandConfig) + noConsoleMessage(commandConfig))
            return false
        }
        if (commandConfig.perm != "" && !sender.hasPermission(commandConfig.perm)) {
            sender.sendMessage(getPrefix(commandConfig) + noPermMessage(commandConfig))
            return false
        }
        return true
    }

    private fun executeMethod(sc: SubCommandContainer, sender: CommandSender, args: Array<String?>) {
        var uuid: UUID? = null
        if (sender is Player) uuid = sender.uniqueId
        val uk = uuid
        val target = paramBuilder(sc.method, getParamContainer(sender, args, name), uuid)
        try {
            currentContext = CommandContext(sender)
            sc.method.invoke(sc.pathClass, *target)
        } catch (e: Exception) {
            //Must handled front
            e.printStackTrace()
        } finally {
            clearContext()
        }

        //  ReflectionUtils.invokeMethod(sc.getMethod(), sc.getPathClass(), target);
    }

    private fun getParamContainer(sender: CommandSender, args: Array<String?>, label: String): HashMap<Class<*>, Any> {
        val map = HashMap<Class<*>, Any>()
        map[CommandSender::class.java] = sender
        map[Array<String>::class.java] = args
        map[String::class.java] = label
        if (sender is Player) {
            map[Player::class.java] = sender
        }
        return map
    }

    private fun paramBuilder(method: Method, paramContainer: HashMap<Class<*>, Any>, player: UUID?): Array<Any?> {
        val arr = arrayOfNulls<Any>(method.parameterCount)
        var pos = 0
        for (type in method.parameterTypes) {
            val obj = paramContainer[type]
            arr[pos++] = obj
        }
        paramContainer.clear()
        return arr
    }

    private fun getPrefix(config: CommandConfig): String {
        return if (baseConfig != null && baseConfig!!.prefix != "") baseConfig!!.prefix else config.prefix
    }

    private fun opCondition(config: CommandConfig): Boolean {
        return baseConfig != null && !baseConfig!!.op || config.op
    }

    private fun consoleCondition(config: CommandConfig): Boolean {
        return baseConfig != null && !baseConfig!!.console || config.console
    }

    private fun noPermMessage(config: CommandConfig): String {
        return if (baseConfig != null && baseConfig!!.noPermMessage != "") baseConfig!!.noPermMessage else config.perm
    }

    private fun noConsoleMessage(config: CommandConfig): String {
        return if (baseConfig != null && baseConfig!!.noConsoleMessage != "") baseConfig!!.noConsoleMessage else config.noConsoleMessage
    }
}
