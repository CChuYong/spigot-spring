package chuyong.springspigot.util

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class YamlPropertiesFactory : PropertySourceFactory {
    @Throws(IOException::class)
    override fun createPropertySource(name: String?, resource: EncodedResource): PropertySource<*> {
        val propertiesFromYaml = loadYamlIntoProperties(resource.resource)
        val sourceName = name ?: resource.resource.filename!!
        return PropertiesPropertySource(sourceName, propertiesFromYaml!!)
    }

    companion object {
        @Throws(FileNotFoundException::class)
        fun loadYamlIntoProperties(resource: Resource?): Properties? {
            return try {
                val factory = YamlPropertiesFactoryBean()
                factory.setResources(resource)
                factory.afterPropertiesSet()
                factory.getObject()
            } catch (e: IllegalStateException) {
                // for ignoreResourceNotFound
                val cause = e.cause
                if (cause is FileNotFoundException) throw (e.cause as FileNotFoundException?)!!
                throw e
            }
        }
    }
}
