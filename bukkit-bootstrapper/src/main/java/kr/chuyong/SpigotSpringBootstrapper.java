package kr.chuyong;

import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.java.PluginClassLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public class SpigotSpringBootstrapper extends JavaPlugin {
    @Override
    public void onEnable() {
        try{
            Field libraryLoader = getClassLoader().getClass().getDeclaredField("libraryLoader");
            libraryLoader.setAccessible(true);
            URLClassLoader libLoader = (URLClassLoader) libraryLoader.get(getClassLoader());


            Field fileField = PluginClassLoader.class.getDeclaredField("file");
            fileField.setAccessible(true);
            File currentPluginFile = (File) fileField.get(getClassLoader());

            File jarFile = new File("test.jar");
            SpringSpigotContextClassLoader loader = loadPlugin(jarFile, libLoader.getURLs());
            Class<?> k = Class.forName("chuyong.springspigot.SpringSpigotBootstrapper", true, loader);
            Method startMethod = k.getDeclaredMethod("start");
            Constructor cs = k.getConstructor(JavaPlugin.class, URLClassLoader.class, URLClassLoader.class, URLClassLoader.class);
            URLClassLoader contextLoader =          addOnSpigot(loader, currentPluginFile);
            Object instance = cs.newInstance(this, loader, getClassLoader(), contextLoader);
            startMethod.invoke(instance);
        }catch(Exception ex){
            ex.printStackTrace();
        }

    }

    private URLClassLoader addOnSpigot(SpringSpigotContextClassLoader mainloader, File file) {
        try{
            PluginDescriptionFile descFile = getDescription();
            File dataFolder = getDataFolder();

            Field mainField = PluginDescriptionFile.class.getDeclaredField("main");
            mainField.setAccessible(true);
            mainField.set(descFile, "kr.chuyong.MockJavaPlugin");

            ((PluginClassLoader)getClassLoader()).plugin = null;
            Field pluginField = PluginClassLoader.class.getDeclaredField("pluginInit");
            pluginField.setAccessible(true);
            pluginField.set(getClassLoader(), null);

            SpigotPluginClassLoader loader = new SpigotPluginClassLoader(
                    (JavaPluginLoader) getPluginLoader(),
                    Thread.currentThread().getContextClassLoader(),
                    descFile,
                    dataFolder,
                   file,
                    null,
                    mainloader
            );

            ((PluginClassLoader)getClassLoader()).plugin = this;

            Field loaderField = getPluginLoader().getClass().getDeclaredField("loaders");
            loaderField.setAccessible(true);
            ((List)loaderField.get(getPluginLoader())).add(loader);
            return loader;
        }catch(Exception ex){
            ex.printStackTrace();
            throw new RuntimeException();
        }

    }


    public SpringSpigotContextClassLoader loadPlugin(final File file, URL[] additional) throws InvalidPluginException, IOException{

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
        @SuppressWarnings("deprecation")
        final File oldDataFolder = new File(parentFile, description.getRawName());

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
