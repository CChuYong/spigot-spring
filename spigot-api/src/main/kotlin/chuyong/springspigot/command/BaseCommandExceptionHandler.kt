package chuyong.springspigot.command

import chuyong.springspigot.command.annotation.CommandMapping
import chuyong.springspigot.command.data.ExceptionHandlerWrapper
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.bukkit.command.CommandSender
import org.springframework.stereotype.Component
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Aspect
@Component
class BaseCommandExceptionHandler : CommandExceptionHandler {
    var exceptionHandlers = HashMap<Class<out Throwable>, ExceptionHandlerWrapper>()

    @Pointcut("@annotation(commandMapping)")
    fun commandMappingPointcut(commandMapping: CommandMapping?) {
    }

    fun registerExceptionHandler(throwableClazz: Class<out Throwable>, handler: ExceptionHandlerWrapper) {
        exceptionHandlers[throwableClazz] = handler
    }

    @AfterThrowing(pointcut = "commandMappingPointcut(commandMapping)", throwing = "ex")
    fun handleCommandMappingException(joinPoint: JoinPoint?, commandMapping: CommandMapping?, ex: Exception) {
        handleCommandMappingException(ex)
    }

    override fun handleCommandMappingException(ex: Throwable) {
        for ((key, value) in exceptionHandlers) {
            if (key.isInstance(ex)) {
                try {
                    val paramContainer = HashMap<Class<*>, Any>()
                    CommandContext.currentContext?.apply {
                        this as BukkitCommandContext
                        paramContainer[CommandContext::class.java] = this
                        paramContainer[CommandSender::class.java] = this.commandSender
                    }
                    paramContainer[key] = ex
                    paramContainer[Throwable::class.java] = ex
                    val builtParam = paramBuilder(value.method, paramContainer)
                    value.method.invoke(value.obj, *builtParam)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                } catch (e: InvocationTargetException) {
                    throw RuntimeException(e)
                }
                return
            }
        }
        System.err.println("Uncaught Exception: " + ex.javaClass.simpleName)
        ex.printStackTrace()
    }

    private fun paramBuilder(method: Method, paramContainer: HashMap<Class<*>, Any>): Array<Any?> {
        val arr = arrayOfNulls<Any>(method.parameterCount)
        for ((pos, type) in method.parameterTypes.withIndex()) {
            var obj = paramContainer[type]
            if (obj == null) {
                obj = paramContainer[type.superclass]
                if (obj == null) {
                    throw RuntimeException("Unknown Exception Handler parameter type: " + type.name)
                }
            }
            arr[pos] = obj
        }
        paramContainer.clear()
        return arr
    }
}
