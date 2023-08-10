package chuyong.springspigot.child

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.util.function.Supplier

class SpigotSpringChildInitializer(
    private val data: SpigotSpringChildPluginData,
) : ApplicationContextInitializer<AnnotationConfigApplicationContext> {
    override fun initialize(applicationContext: AnnotationConfigApplicationContext) {
        applicationContext.registerBean(SpigotSpringChildPluginData::class.java, Supplier { data })
    }
}
