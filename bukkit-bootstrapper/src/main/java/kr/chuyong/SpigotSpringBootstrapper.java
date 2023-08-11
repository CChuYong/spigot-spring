package kr.chuyong;

import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.java.PluginClassLoader;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SpigotSpringBootstrapper extends JavaPlugin {
    String fileName = "bukkit-api-0.0.2-SNAPSHOT-all.jar";

    @Override
    public void onEnable() {
        try {
            Field libraryLoader = getClassLoader().getClass().getDeclaredField("libraryLoader");
            libraryLoader.setAccessible(true);
            URLClassLoader libLoader = (URLClassLoader) libraryLoader.get(getClassLoader());


            Field fileField = PluginClassLoader.class.getDeclaredField("file");
            fileField.setAccessible(true);
            File currentPluginFile = (File) fileField.get(getClassLoader());

            File libsPath = new File("libs");
            if (!libsPath.exists())
                libsPath.mkdirs();

            File jarPath = new File(libsPath, fileName);
            if (!jarPath.exists()) {
                InputStream ins = getResource(fileName);
                try (FileOutputStream fos = new FileOutputStream(jarPath)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = ins.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                }
                getLogger().info("Saving " + fileName + " to " + jarPath.getAbsolutePath());
            }

            SpringSpigotContextClassLoader loader = loadPlugin(jarPath, libLoader.getURLs());
            Class<?> k = Class.forName("chuyong.springspigot.SpringSpigotBootstrapper", true, loader);
            Method startMethod = k.getDeclaredMethod("start");
            Constructor cs = k.getConstructor(JavaPlugin.class, URLClassLoader.class, URLClassLoader.class, Object.class);
            Object contextLoader = isPaperAvailable() ? addOnPaper(loader) : addOnSpigot(loader, currentPluginFile);

            Object instance = cs.newInstance(this, loader, getClassLoader(), contextLoader);
            startMethod.invoke(instance);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private Object addOnPaper(SpringSpigotContextClassLoader mainLoader) {
        try {
            Class<?> clazz = Class.forName("kr.chuyong.PaperCustomPluginLoader");
            Constructor<?> constructor = clazz.getConstructor(JavaPlugin.class, SpringSpigotContextClassLoader.class);
            return constructor.newInstance(
                    this,
                    mainLoader
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    private Object addOnSpigot(SpringSpigotContextClassLoader mainloader, File file) {
        try {
            PluginDescriptionFile descFile = getDescription();
            File dataFolder = getDataFolder();

            Field mainField = PluginDescriptionFile.class.getDeclaredField("main");
            mainField.setAccessible(true);
            mainField.set(descFile, "kr.chuyong.MockJavaPlugin");

            ((PluginClassLoader) getClassLoader()).plugin = null;
            Field pluginField = PluginClassLoader.class.getDeclaredField("pluginInit");
            pluginField.setAccessible(true);
            pluginField.set(getClassLoader(), null);

            Class<?> clazz = Class.forName("kr.chuyong.SpigotPluginClassLoader");
            Constructor<?> constructor = clazz.getConstructor(
                    JavaPluginLoader.class,
                    ClassLoader.class,
                    PluginDescriptionFile.class,
                    File.class,
                    File.class,
                    ClassLoader.class,
                    SpringSpigotContextClassLoader.class
            );
            Object res = constructor.newInstance(
                    (JavaPluginLoader) getPluginLoader(),
                    Thread.currentThread().getContextClassLoader(),
                    descFile,
                    dataFolder,
                    file,
                    null,
                    mainloader
            );


            ((PluginClassLoader) getClassLoader()).plugin = this;

            Field loaderField = getPluginLoader().getClass().getDeclaredField("loaders");
            loaderField.setAccessible(true);
            ((List) loaderField.get(getPluginLoader())).add(res);
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }

    }

    private boolean isPaperAvailable() {
        try {
            Class.forName("io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }


    public SpringSpigotContextClassLoader loadPlugin(final File file, URL[] additional) throws InvalidPluginException, IOException {

        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(file.getPath() + " does not exist"));
        }

        final PluginDescriptionFile description;
        try {
            description = getPluginDescription(file);
        } catch (InvalidDescriptionException ex) {
            throw new InvalidPluginException(ex);
        }

        final File parentFile = file.getParentFile();
        final File dataFolder = new File(parentFile, description.getName());
        @SuppressWarnings("deprecation") final File oldDataFolder = new File(parentFile, description.getRawName());

        if (dataFolder.equals(oldDataFolder)) {
        } else if (dataFolder.isDirectory() && oldDataFolder.isDirectory()) {
        } else if (oldDataFolder.isDirectory() && !dataFolder.exists()) {
            if (!oldDataFolder.renameTo(dataFolder)) {
                throw new InvalidPluginException("Unable to rename old data folder: `" + oldDataFolder + "' to: `" + dataFolder + "'");
            }
        }


        return new SpringSpigotContextClassLoader(file, additional, description, getClassLoader().getParent(), (URLClassLoader) getClassLoader());
    }

    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {

        JarFile jar = null;
        InputStream stream = null;

        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry("plugin.yml");

            if (entry == null) {
                throw new InvalidDescriptionException(new FileNotFoundException("Jar does not contain plugin.yml"));
            }

            stream = jar.getInputStream(entry);

            return new PluginDescriptionFile(stream);

        } catch (IOException ex) {
            throw new InvalidDescriptionException(ex);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
