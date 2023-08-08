package chuyong.springspigot.command.data

import java.lang.reflect.Method

data class InvokeWrapper(
    val method: Method,
    val obj: Any,
    val config: CommandConfig,
)
