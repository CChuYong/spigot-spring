package chuyong.springspigot.child

import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import java.util.function.Supplier

class SpigotSpringChildInitializer(
    private val data: SpigotSpringChildPluginData,
) : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(applicationContext: GenericApplicationContext) {
        applicationContext.registerBean(SpigotSpringChildPluginData::class.java, Supplier { data }, BeanDefinitionCustomizer{ ef -> ef.isPrimary = true })
    }
}
