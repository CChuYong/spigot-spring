package chuyong.springspigot.util

import java.io.InputStream
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

class MultiClassLoader(
    parent: ClassLoader?,
    private val mainContextLoader: ClassLoader,
    private val urls: Array<URL>,
    private val libraryUrls: Array<URL>,

    ) :
    URLClassLoader(urls, parent) {
    private val libraryLoader: ClassLoader = URLClassLoader(libraryUrls, parent)
    private val classes: MutableMap<String, Class<*>?> = ConcurrentHashMap()

    override fun getResourceAsStream(name: String?): InputStream? {
        // println(name)
        val currentLoaderBias = super.getResourceAsStream(name)
        return currentLoaderBias ?: libraryLoader.getResourceAsStream(name)
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // println("LOAD ${name}")
        return loadClass0(name, resolve, true, true)
    }

    @Throws(ClassNotFoundException::class)
    fun loadClass0(name: String, resolve: Boolean, checkGlobal: Boolean, checkLibraries: Boolean): Class<*> {
        try {
            //     println("LOADCLASS ${name}")
            return super.loadClass(name, resolve)

        } catch (ex: ClassNotFoundException) {
        }
        if (checkLibraries && libraryLoader != null) {
            //   println("LOADCLASS-LIB ${name}")
            try {
                return libraryLoader.loadClass(name)
            } catch (ex: ClassNotFoundException) {
            }
        }
        try {
            // println("CTX-LIB ${name}")
            return mainContextLoader.loadClass(name)
        } catch (ex: ClassNotFoundException) {
        }


//        if (checkGlobal) {
//            // This ignores the libraries of other plugins, unless they are transitive dependencies.
//            val result = loader.getClassByName(name, resolve, description)
//            if (result != null) {
//                return result
//            }
//        }
        throw ClassNotFoundException(name)
    }

    fun readSelf(name: String, resolve: Boolean): Class<*> {
        var c1 = findLoadedClass(name)
        if (c1 == null) {
            c1 = findClass(name)
            if (resolve) resolveClass(c1)
        }

        return c1
    }

    fun getClasses(): Collection<Class<*>?> {
        return classes.values
    }

    companion object {
        init {
            registerAsParallelCapable()
        }
    }
}
