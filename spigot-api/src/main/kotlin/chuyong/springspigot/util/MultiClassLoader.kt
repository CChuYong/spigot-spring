package chuyong.springspigot.util

import java.net.URL
import java.net.URLClassLoader

class MultiClassLoader(
    parent: ClassLoader?,
    private val mainContextLoader: ClassLoader,
    private val urls: Array<URL>,
    private val libraryUrls: Array<URL>,
) : URLClassLoader(urls, parent) {
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        try {
            return super.loadClass(name, resolve)
        } catch (ex: ClassNotFoundException) {
        }

        try {
            return mainContextLoader.loadClass(name)
        } catch (ex: ClassNotFoundException) {
        }

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

    companion object {
        init {
            registerAsParallelCapable()
        }
    }
}
