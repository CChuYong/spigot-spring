package chuyong.springspigot.command.data

import java.lang.reflect.Method

data class ExceptionHandlerWrapper(
    val method: Method,
    val obj: Any,
)
