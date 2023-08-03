package chuyong.springspigot.command

import chuyong.springspigot.command.annotation.CommandMapping
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.bukkit.command.CommandSender
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.stereotype.Component
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Aspect
@Component
class CommandExceptionHandler(
    private val bukkitCommandHandler: BukkitCommandHandler,
) : AsyncUncaughtExceptionHandler {
    @Pointcut("@annotation(commandMapping)")
    fun commandMappingPointcut(commandMapping: CommandMapping?) {
    }

    @AfterThrowing(pointcut = "commandMappingPointcut(commandMapping)", throwing = "ex")
    fun handleCommandMappingException(joinPoint: JoinPoint?, commandMapping: CommandMapping?, ex: Exception) {
        handleCommandMappingException(ex)
    }

    override fun handleUncaughtException(ex: Throwable, method: Method, vararg params: Any) {
        //Async로 발생한거긴 한데;; 씹어야해 ㅠㅠ
        ex.printStackTrace()
    }

    fun handleCommandMappingException(ex: Throwable) {
        for ((key, value) in bukkitCommandHandler.exceptionHandlers) {
            if (key.isInstance(ex)) {
                try {
                    val paramContainer = HashMap<Class<*>, Any>()
                    CommandContext.currentContext?.apply {
                        paramContainer[CommandContext::class.java] = this
                        paramContainer[CommandSender::class.java] = this.sender
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
        var pos = 0
        for (type in method.parameterTypes) {
            var obj = paramContainer[type]
            if (obj == null) {
                obj = paramContainer[type.superclass]
                if (obj == null) throw RuntimeException("Unknown Exception Handler parameter type: " + type.name)
            }
            arr[pos++] = obj
        }
        paramContainer.clear()
        return arr
    }
}
