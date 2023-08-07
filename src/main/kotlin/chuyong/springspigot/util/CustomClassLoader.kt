package chuyong.springspigot.util

import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.plugin.PluginDescriptionFile
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.security.CodeSource
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import java.util.jar.Manifest

internal class PluginClassLoader(
    parent: ClassLoader?,
    private val mainContextLoader: ClassLoader,
    private val description: PluginDescriptionFile,
    private val file: File,
    private val libraryLoader: ClassLoader?,
) :
    URLClassLoader(arrayOf(file.toURI().toURL()), parent) {
    private val classes: MutableMap<String, Class<*>?> = ConcurrentHashMap()
    private val jar: JarFile = JarFile(file)
    private val manifest: Manifest = jar.manifest
    private val url: URL = file.toURI().toURL()

    init {
        try {
            val jarClass: Class<*>
            jarClass = try {
                Class.forName(description.main, true, this)
            } catch (ex: ClassNotFoundException) {
                throw RuntimeException("Cannot find main class `" + description.main + "'", ex)
            }
        } catch (ex: IllegalAccessException) {
            throw RuntimeException("No public constructor", ex)
        } catch (ex: InstantiationException) {
            throw RuntimeException("Abnormal plugin type", ex)
        }
    }

    override fun getResource(name: String): URL {
        return findResource(name)
    }

    @Throws(IOException::class)
    override fun getResources(name: String): Enumeration<URL> {
        return findResources(name)
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // println("LOAD ${name}")
        return loadClass0(name, resolve, true, true)
    }

    @Throws(ClassNotFoundException::class)
    fun loadClass0(name: String, resolve: Boolean, checkGlobal: Boolean, checkLibraries: Boolean): Class<*> {
        try {
            val result = super.loadClass(name, resolve)

            // SPIGOT-6749: Library classes will appear in the above, but we don't want to return them to other plugins
            if (checkGlobal || result.classLoader === this) {
                return result
            }
        } catch (ex: ClassNotFoundException) {
        }
        if (checkLibraries && libraryLoader != null) {
            try {
                return libraryLoader.loadClass(name)
            } catch (ex: ClassNotFoundException) {
            }
        }
        try {
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

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*>? {
        //println("FIND ${name}")
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw ClassNotFoundException(name)
        }
        var result = classes[name]
        if (result == null) {
            val path = name.replace('.', '/') + ".class"
            val entry = jar.getJarEntry(path)
            if (entry != null) {
                var classBytes: ByteArray
                try {
                    jar.getInputStream(entry).use { `is` ->
                        classBytes = ByteStreams.toByteArray(`is`)
                    }
                } catch (ex: IOException) {
                    throw ClassNotFoundException(name, ex)
                }
                classBytes = Bukkit.getServer().unsafe.processClass(description, path, classBytes)
                val dot = name.lastIndexOf('.')
                if (dot != -1) {
                    val pkgName = name.substring(0, dot)
                    if (getPackage(pkgName) == null) {
                        try {
                            definePackage(pkgName, manifest, url) ?: definePackage(
                                pkgName,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                            )
                            println("Defined ${pkgName} with ${manifest}")
                        } catch (ex: IllegalArgumentException) {
                            checkNotNull(getPackage(pkgName)) { "Cannot find package $pkgName" }
                        }
                    }
                }
                val signers = entry.codeSigners
                val source = CodeSource(url, signers)
                try {
                    result = defineClass(name, classBytes, 0, classBytes.size, source)
                } catch (e: NoClassDefFoundError) {

                }
            }
            if (result == null) {
                result = super.findClass(name)
            }
            //  loader.setClass(name, result!!)
            classes[name] = result
        }
        return result
    }

    @Throws(IOException::class)
    override fun close() {
        try {
            super.close()
        } finally {
            jar.close()
        }
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
