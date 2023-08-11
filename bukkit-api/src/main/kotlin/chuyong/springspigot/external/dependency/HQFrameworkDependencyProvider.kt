package chuyong.springspigot.external.dependency

import org.bukkit.Bukkit
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component("hq-provider")
@ConditionalOnClass(
    name = ["kr.hqservice.framework.global.core.component.registry.ComponentRegistry"]
)
class HQFrameworkDependencyProvider : ExternalDependencyProvider {
    override fun <T : Any> get(clazz: Class<T>): T {
        return getNamed(clazz, "HQFramework")
    }

    override fun <T : Any> getNamed(clazz: Class<T>, qualifier: String): T {
        val registry = getRegistryFromPlugin(qualifier)
        registry::class.java.getMethod("getComponent", KClass::class.java).let {
            it.isAccessible = true
            return it.invoke(registry, clazz) as T
        }
    }

    private fun getRegistryFromPlugin(pluginName: String): Any {
        val pluginzz = Bukkit.getPluginManager().getPlugin(pluginName) ?: throw RuntimeException()
        return try {
            val m = pluginzz::class.java.getDeclaredMethod("getComponentRegistry")
            m.isAccessible = true
            m.invoke(pluginzz)
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw RuntimeException()
        }
    }
}
