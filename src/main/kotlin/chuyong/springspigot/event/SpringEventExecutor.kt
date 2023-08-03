package chuyong.springspigot.event

import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import org.springframework.aop.support.AopUtils
import org.springframework.stereotype.Component
import java.lang.reflect.Method

@Component
class SpringEventExecutor {
    fun create(method: Method): EventExecutor {
        val eventType = method.parameters[0].type
        return EventExecutor { listener: Listener, event: Event ->
            if (!eventType.isInstance(event)) return@EventExecutor
            triggerEvent(method, listener, event)
        }
    }

    private fun triggerEvent(method: Method, listener: Listener, event: Event) {
        AopUtils.invokeJoinpointUsingReflection(listener, method, arrayOf<Any>(event))
    }
}
