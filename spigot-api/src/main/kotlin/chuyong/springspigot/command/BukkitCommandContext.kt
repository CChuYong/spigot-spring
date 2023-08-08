package chuyong.springspigot.command

import org.bukkit.command.CommandSender

class BukkitCommandContext(
    val commandSender: CommandSender,
): CommandContext() {
}
