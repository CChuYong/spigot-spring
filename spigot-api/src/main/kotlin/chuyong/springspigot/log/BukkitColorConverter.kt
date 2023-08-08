package chuyong.springspigot.log

import org.bukkit.ChatColor
import org.bukkit.Server
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.stereotype.Component
import java.lang.reflect.Method

@Component
@ConditionalOnClass(name = ["org.bukkit.craftbukkit.v1_19_R3.command.ColouredConsoleSender"])
class BukkitColorConverter(
    private val server: Server,
) : ColorConverter {
    lateinit var method: Method
    lateinit var replacements: Map<ChatColor, String>

    init {
        val clazz = Class.forName("org.bukkit.craftbukkit.v1_19_R3.command.ColouredConsoleSender")
        method = clazz.getDeclaredMethod("convertRGBColors", String::class.java)
        method.isAccessible = true

        val field = clazz.getDeclaredField("replacements")
        field.isAccessible = true
        replacements = field.get(server.consoleSender) as Map<ChatColor, String>
    }

    override fun convert(message: String) = parseMessage(method.invoke(null, message) as String)

    fun parseMessage(inputResult: String): String {
        var result = inputResult
        for (color in ChatColor.values()) {
            if (this.replacements.containsKey(color)) {
                result = result.replace(("(?i)" + color.toString()).toRegex(), replacements.get(color)!!)
            } else {
                result = result.replace(("(?i)" + color.toString()).toRegex(), "")
            }
        }
        return result + replacements.get(ChatColor.RESET)
    }
}

@Component
@ConditionalOnMissingClass("org.bukkit.craftbukkit.v1_19_R3.command.ColouredConsoleSender")
class DefaultColorConverter : ColorConverter {
    override fun convert(message: String) = message
}

interface ColorConverter {
    fun convert(message: String): String
}
