package chuyong.springspigot.event

import org.bukkit.Server
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.springframework.aop.support.AopUtils
import org.springframework.stereotype.Service
import org.springframework.util.ReflectionUtils
import java.lang.reflect.Method
import java.util.*
import java.util.stream.Stream

@Service
class EventService(
    private val eventExecutor: SpringEventExecutor,
    private val server: Server,
    private val plugin: Plugin,
) {
    fun registerEvents(listener: Listener) {
        getListenerMethods(listener).forEach { method: Method -> registerEvents(listener, method) }
    }

    private fun registerEvents(listener: Listener, method: Method) {
        val handler = method.getAnnotation(EventHandler::class.java)
        val eventType = method.parameters[0].type as Class<out Event?>
        // System.out.println("====================== " + method.getName() + " === " + listener.getClass().getName() + " === " + eventType.getName());
        server.pluginManager.registerEvent(
            eventType,
            listener,
            handler.priority,
            eventExecutor.create(method),
            plugin,
            handler.ignoreCancelled
        )
    }

    private fun getListenerMethods(listener: Listener): Stream<Method> {
        val target = AopUtils.getTargetClass(listener)
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(target))
            .filter { method: Method ->
                method.isAnnotationPresent(
                    EventHandler::class.java
                )
            }
            .filter { method: Method -> method.parameters.size == 1 }
            .filter { method: Method -> Event::class.java.isAssignableFrom(method.parameters[0].type) }
    }
}
