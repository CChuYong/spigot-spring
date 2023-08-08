package chuyong.springspigot.command.data

import chuyong.springspigot.command.annotation.CommandMapping

data class SuperCommandConfig(
    val args: String,
    val usage: String,
    val prefix: String,
    val perm: String,
    val errorMessage: String,
    val noPermMessage: String,
    val noConsoleMessage: String,
    val op: Boolean,
    val console: Boolean,
) {
    companion object {
        fun fromAnnotation(mapping: CommandMapping) = SuperCommandConfig(
            mapping.child,
            mapping.usage,
            mapping.prefix,
            mapping.perm,
            mapping.error,
            mapping.noPerm,
            mapping.noConsole,
            mapping.op,
            mapping.console
        )
    }
}
