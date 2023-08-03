package chuyong.springspigot.command

import chuyong.springspigot.command.data.CommandConfig
import chuyong.springspigot.command.data.InvokeWrapper
import java.lang.reflect.Method
import java.util.*

class SubCommandContainer(
    private val parent: SubCommandContainer?,
    private val currentKey: String,
    val commandDepth: Int,
) {
    private val childCommandMap = HashMap<String, SubCommandContainer>()
    private var invokeWrapper: InvokeWrapper? = null
    fun childCommandKeys(): Collection<String> {
        val keys: Set<String> = childCommandMap.keys
        return keys.ifEmpty { listOf(*config.suggestion) }
    }

    val isImplemented: Boolean
        get() = invokeWrapper != null

    fun getContainer(remainingArgs: Queue<String>): SubCommandContainer? {
        return if (remainingArgs.isEmpty()) {
            this
        } else {
            val nextArg = remainingArgs.poll()
            val nextCommand = childCommandMap[nextArg]
            if (nextCommand != null) {
                nextCommand.getContainer(remainingArgs)
            } else {
                //남은 args가 있는데, 더이상 뎁스가 없음 -> 추가 args인지 판별!
                val remainingItems = remainingArgs.size + 1
                //System.out.println("Remain : " + remainingItems + " wrapper " + invokeWrapper);
                if (invokeWrapper == null) return null
                if (config.minArgs >= remainingItems && config
                        .maxArgs <= remainingItems
                ) {
                    this
                } else {
                    null
                }
            }
        }
    }

    fun getTapCompleteContainer(remainingArgs: Queue<String>): SubCommandContainer {
        return if (remainingArgs.isEmpty()) {
            this
        } else {
            val nextArg = remainingArgs.poll()
            val nextCommand = childCommandMap[nextArg]
            nextCommand?.getTapCompleteContainer(remainingArgs) ?: this
        }
    }

    // "a b c"
    fun addCommand(args: Queue<String>, ano: CommandConfig?, mtd: Method?, cl: Any?): SubCommandContainer {
        return if (args.isEmpty()) {
            if (invokeWrapper != null) {
                throw RuntimeException("Duplicated command entry! Command: /" + fullKey)
            }
            invokeWrapper = InvokeWrapper(mtd!!, cl!!, ano!!)
            this
        } else {
            val nextKey = args.poll()
            childCommandMap
                .computeIfAbsent(nextKey) { e: String? -> SubCommandContainer(this, nextKey, commandDepth + 1) }
                .addCommand(args, ano, mtd, cl)
        }
    }

    val method: Method
        get() = invokeWrapper!!.method
    val config: CommandConfig
        get() = invokeWrapper!!.config
    val pathClass: Any
        get() = invokeWrapper!!.obj

    fun mapToTabbedString(): String {
        val sb = StringBuilder()
        sb.append(currentKey)
        for (commandKey in childCommandMap.values) {
            sb.append("<>".repeat(Math.max(0, commandDepth)))
            sb.append(commandKey.toString())
            sb.append("\n")
        }
        return sb.toString()
    }

    val fullKey: String
        get() = if (parent == null) currentKey else parent.fullKey + " " + currentKey

    override fun toString(): String {
        return """
            $currentKey($commandDepth) -> 
            ${mapToTabbedString()}
            """.trimIndent()
    }
}
