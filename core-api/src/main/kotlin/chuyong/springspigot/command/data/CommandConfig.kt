package chuyong.springspigot.command.data

import chuyong.springspigot.command.annotation.CommandMapping

data class CommandConfig(
    val usage: String,
    val prefix: String,
    val perm: String,
    val errorMessage: String,
    val noConsoleMessage: String,
    val suggestion: Array<String>,
    val minArgs: Int,
    val maxArgs: Int,
    val op: Boolean,
    val console: Boolean,
    val defaultSuggestion: Boolean,

    ) {
    companion object {
        fun fromAnnotation(ano: CommandMapping) = CommandConfig(
            ano.usage,
            ano.prefix,
            ano.perm,
            ano.error,
            ano.noConsole,
            ano.suggestion,
            ano.minArgs,
            ano.maxArgs,
            ano.op,
            ano.console,
            ano.defaultSuggestion
        )
    }
}
