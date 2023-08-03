package chuyong.springspigot.child

import chuyong.springspigot.child.annotation.WirePlugin
import org.slf4j.Logger
import org.springframework.aop.framework.AopProxyUtils
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty

@Component
class SpringSpigotPluginRegistry(
    private val logger: Logger,
) {
    val plugins = ConcurrentHashMap<String, SpringSpigotChildPlugin>()

    fun registerPlugin(plugin: SpringSpigotChildPlugin) {
        logger.info("Registered plugin ${plugin.name}...")
        plugins[plugin.name] = plugin
    }

    fun wireContexts(context: List<ConfigurableApplicationContext>): Int {
        var count = 0
        context.map {
            it.getBeansWithAnnotation(Component::class.java).map { (name, bean) ->
                val fields = getAnnotatedFields(bean)
                bean to fields
            }
        }.flatten().forEach { (bean, fields) ->
            fields.forEach { field->
                val inst = AopProxyUtils.getSingletonTarget(bean) ?: bean
                val annotation = field.getAnnotation(WirePlugin::class.java)
                val pluginName = if(annotation.pluginName.isNotBlank())
                    annotation.pluginName
                else if (annotation.value.isNotBlank()) annotation.value
                else throw NoSuchBeanDefinitionException("Class ${field.declaringClass.simpleName} Field ${field.name} has no plugin name.")

                val targetPlugin = plugins[pluginName] ?: throw NoSuchBeanDefinitionException("Class ${field.declaringClass.simpleName} Field ${field.name} Plugin $pluginName is not registered.")
                val targetBean = targetPlugin.context.getBean(field.type)
                if(!field.canAccess(bean)) throw NoSuchBeanDefinitionException("Class ${field.declaringClass.simpleName} Field ${field.name} is not visible.")

                field.set(inst, targetBean)
                count++
            }
        }
        return count
    }

    private fun getAnnotatedFields(obj: Any): List<Field> {
        val target = AopUtils.getTargetClass(obj)
        return target.declaredFields.filter { it.isAnnotationPresent(WirePlugin::class.java) }
    }
}
