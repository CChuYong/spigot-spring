package chuyong.springspigot

import org.bukkit.event.Listener
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import java.util.*

internal class ScopePostProcessor : BeanFactoryPostProcessor {
    @Throws(BeansException::class)
    override fun postProcessBeanFactory(factory: ConfigurableListableBeanFactory) {
        Arrays.stream(factory.beanDefinitionNames).forEach { beanName: String? ->
            val beanDef = factory.getBeanDefinition(
                beanName!!
            )
            val beanType = factory.getType(beanName)
            if (beanType != null && beanType.isAssignableFrom(Listener::class.java)) {
                beanDef.scope = ConfigurableBeanFactory.SCOPE_SINGLETON
            }
        }
    }
}
