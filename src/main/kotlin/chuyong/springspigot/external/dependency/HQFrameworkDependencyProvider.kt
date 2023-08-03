package chuyong.springspigot.external.dependency

import kr.hqservice.framework.bukkit.core.HQBukkitPlugin
import kr.hqservice.framework.global.core.component.HQComponent
import kr.hqservice.framework.global.core.component.registry.ComponentRegistry
import org.bukkit.Bukkit
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component("hq-provider")
@ConditionalOnBean(KoinDependencyProvider::class)
@ConditionalOnClass(
    ComponentRegistry::class
)
class HQFrameworkDependencyProvider(private val koinProvider: KoinDependencyProvider) : ExternalDependencyProvider {
    override fun <T : Any> get(clazz: Class<T>): T {
        return getNamed(clazz, "HQFramework")
    }

    override fun <T : Any> getNamed(clazz: Class<T>, qualifier: String): T {
        return getRegistryFromPlugin(qualifier).getComponent(clazz.kotlin as KClass<out HQComponent>) as T
    }

    private fun getRegistryFromPlugin(pluginName: String): ComponentRegistry {
        val pluginzz = Bukkit.getPluginManager().getPlugin(pluginName) as HQBukkitPlugin?
        return try {
            val m = HQBukkitPlugin::class.java.getDeclaredMethod("getComponentRegistry")
            m.isAccessible = true
            m.invoke(pluginzz) as ComponentRegistry
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw RuntimeException()
        }
    }
}
