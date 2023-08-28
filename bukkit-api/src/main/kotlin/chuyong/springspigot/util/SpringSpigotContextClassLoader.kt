package chuyong.springspigot.util

import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.plugin.PluginDescriptionFile
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.security.CodeSource
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import java.util.jar.Manifest

class SpringSpigotContextClassLoader(
    file: File,
    additional: Array<out URL>,
    private val description: PluginDescriptionFile,
    parent: ClassLoader,
    private val springSpigotLoader: ClassLoader,
) : URLClassLoader(arrayOf(file.toURI().toURL()), parent) {

    private val classes = ConcurrentHashMap<String, Class<*>>()
    private val jar: JarFile = JarFile(file)
    private val manifest: Manifest? = jar.manifest
    private val url: URL = file.toURI().toURL()

    init {
        additional.forEach { url ->
            addURL(url)
        }
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return loadClass0(name, resolve, true, true)
    }

    @Throws(ClassNotFoundException::class)
    fun loadClass0(
        name: String,
        resolve: Boolean,
        checkGlobal: Boolean,
        checkLibraries: Boolean,
    ): Class<*> {
        try {
            val result = super.loadClass(name, resolve)

            if (checkGlobal || result.classLoader == this) {
                return result
            }
        } catch (ex: ClassNotFoundException) {
        }

        if (checkGlobal) {
            try {
                return springSpigotLoader.loadClass(name)
            } catch (ex: ClassNotFoundException) {

            }
        }

        throw ClassNotFoundException(name)
    }

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*> {
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw ClassNotFoundException(name)
        }

        val result = classes[name]

        if (result == null) {
            val path = name.replace('.', '/').plus(".class")
            val entry = jar.getJarEntry(path)

            if (entry != null) {
                val classBytes: ByteArray
                try {
                    jar.getInputStream(entry).use { `is` ->
                        classBytes = ByteStreams.toByteArray(`is`)
                    }
                } catch (ex: IOException) {
                    throw ClassNotFoundException(name, ex)
                }

                classBytes.let {
                    Bukkit.getServer().unsafe.processClass(description, path, it)
                }

                val dot = name.lastIndexOf('.')
                if (dot != -1) {
                    val pkgName = name.substring(0, dot)
                    if (getPackage(pkgName) == null) {
                        try {
                            manifest?.let {
                                definePackage(pkgName, it, url)
                            } ?: definePackage(pkgName, null, null, null, null, null, null, null)
                        } catch (ex: IllegalArgumentException) {
                            if (getPackage(pkgName) == null) {
                                throw IllegalStateException("Cannot find package $pkgName")
                            }
                        }
                    }
                }

                val signers = entry.codeSigners
                val source = CodeSource(url, signers)

                return defineClass(name, classBytes, 0, classBytes.size, source)
            }

            return super.findClass(name)
        }

        return result
    }

    fun readSelf(name: String, resolve: Boolean): Class<*> {
        var c1 = findLoadedClass(name)
        if (c1 == null) {
            c1 = findClass(name)
            if (resolve) resolveClass(c1)
        }

        return c1
    }

    @Throws(IOException::class)
    override fun close() {
        super.close()
        jar.close()
    }
}
