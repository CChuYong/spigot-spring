package kr.chuyong;

import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class SpringSpigotContextClassLoader extends URLClassLoader {
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>();
    private final PluginDescriptionFile description;
    private final Manifest manifest;
    private final URL url;
    private final URLClassLoader pluginClassLoader;
    private JarFile jar;

    public SpringSpigotContextClassLoader(File file, URL[] additional, PluginDescriptionFile description, ClassLoader parent, URLClassLoader pluginClassLoader) throws IOException {
        super(new URL[]{file.toURI().toURL()}, parent);
        Arrays.stream(additional).forEach(url -> {
            addURL(url);
        });

        this.jar = new JarFile(file);
        this.manifest = jar.getManifest();
        this.url = file.toURI().toURL();
        this.description = description;
        this.pluginClassLoader = pluginClassLoader;
    }

    public void addSource(URL url) {
        addURL(url);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true, true);
    }

    Class<?> loadClass0(String name, boolean resolve, boolean checkGlobal, boolean checkLibraries) throws ClassNotFoundException {
        try {
            Class<?> result = super.loadClass(name, resolve);

            // SPIGOT-6749: Library classes will appear in the above, but we don't want to return them to other plugins
            if (checkGlobal || result.getClassLoader() == this) {
                return result;
            }
        } catch (ClassNotFoundException ex) {
        }

        try {
            return pluginClassLoader.loadClass(name);
        } catch (ClassNotFoundException ex) {
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            String path = name.replace('.', '/').concat(".class");
            JarEntry entry = jar.getJarEntry(path);

            if (entry != null) {
                byte[] classBytes;

                try (InputStream is = jar.getInputStream(entry)) {
                    classBytes = ByteStreams.toByteArray(is);
                } catch (IOException ex) {
                    throw new ClassNotFoundException(name, ex);
                }

                classBytes = Bukkit.getServer().getUnsafe().processClass(description, path, classBytes);

                int dot = name.lastIndexOf('.');
                if (dot != -1) {
                    String pkgName = name.substring(0, dot);
                    if (getPackage(pkgName) == null) {
                        try {
                            if (manifest != null) {
                                definePackage(pkgName, manifest, url);
                            } else {
                                definePackage(pkgName, null, null, null, null, null, null, null);
                            }
                        } catch (IllegalArgumentException ex) {
                            if (getPackage(pkgName) == null) {
                                throw new IllegalStateException("Cannot find package " + pkgName);
                            }
                        }
                    }
                }

                CodeSigner[] signers = entry.getCodeSigners();
                CodeSource source = new CodeSource(url, signers);

                result = defineClass(name, classBytes, 0, classBytes.length, source);
            }

            if (result == null) {
                result = super.findClass(name);
            }

            classes.put(name, result);
        }

        return result;
    }

    Class<?> readSelf(String name, Boolean resolve) throws ClassNotFoundException {
        Class<?> c1 = findLoadedClass(name);
        if (c1 == null) {
            c1 = findClass(name);
            if (resolve) resolveClass(c1);
        }
        return c1;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jar.close();
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = super.getResources(name);
        if(name.equals("META-INF/spring.factories")) {
            //Convert enumeration to list
            List<URL> urlList = Collections.list(urls);
            urlList.removeIf(url -> !url.getPath().contains(jar.getName()));
            return Collections.enumeration(urlList);
        }
        return urls;
    }
}
