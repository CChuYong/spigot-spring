package chuyong.springspigot.util

import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CompoundClassLoader : ClassLoader {
    private val classLoaders: Collection<ClassLoader>
    private val classCache = ConcurrentHashMap<String, Class<*>>()

    constructor(vararg loaders: ClassLoader) {
        classLoaders = listOf(*loaders)
    }

    constructor(loaders: Collection<ClassLoader>) {
        classLoaders = loaders
    }

    override fun getResource(name: String): URL? {
        for (loader in classLoaders) {
            val resource = loader.getResource(name)
            if (resource != null) {
                return resource
            }
        }
        return null
    }

    override fun getResourceAsStream(name: String): InputStream? {
        for (loader in classLoaders) {
            val resource = loader.getResourceAsStream(name)
            if (resource != null) {
                return resource
            }
        }
        return null
    }

    override fun getResources(name: String): Enumeration<URL> {
        val urls: MutableList<URL> = ArrayList()
        for (loader in classLoaders) {
            val resources = loader.getResources(name)
            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                if (resource != null && !urls.contains(resource)) {
                    urls.add(resource)
                }
            }
        }
        return Collections.enumeration(urls)
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String): Class<*> {
        classCache[name]?.apply {
            return this
        }
        for (loader in classLoaders) {
            try{
                return loader.loadClass(name).apply {
                    classCache[this.name] = this
                }
            }catch(e: ClassNotFoundException){
                //not here
            }

        }
        throw ClassNotFoundException()
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return loadClass(name)
    }

    override fun toString(): String {
        return String.format("CompoundClassloader %s", classLoaders)
    }
}
