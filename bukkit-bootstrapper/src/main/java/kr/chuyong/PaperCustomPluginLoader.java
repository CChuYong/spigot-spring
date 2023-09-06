package kr.chuyong;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PaperClassLoaderStorage;
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PaperCustomPluginLoader implements ConfiguredPluginClassLoader {
    private final JavaPlugin plugin;
    private final SpringSpigotContextClassLoader mainClassLoader;
    private final Map<String, Function<String, Class<?>>> childClassLoaders = new HashMap<>();
    private PluginClassLoaderGroup group;

    public PaperCustomPluginLoader(JavaPlugin plugin, SpringSpigotContextClassLoader mainClassLoader) {
        this.plugin = plugin;
        this.mainClassLoader = mainClassLoader;
        group = PaperClassLoaderStorage.instance().registerOpenGroup(this);
    }

    @Override
    public PluginMeta getConfiguration() {
        try {
            Method f = plugin.getClass().getMethod("getPluginMeta");
            return (PluginMeta) f.invoke(plugin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<?> loadClass(@NotNull String name, boolean resolve, boolean checkGlobal, boolean checkLibraries) throws ClassNotFoundException {
        try {
            mainClassLoader.readSelf(name, resolve);
        } catch (Exception exe) {

        }
        for (Function<String, Class<?>> loader : childClassLoaders.values()) {
            Class<?> foundClass = loader.apply(name);
            if (foundClass != null) return foundClass;
        }

        throw new ClassNotFoundException();
    }

    @Override
    public void init(JavaPlugin plugin) {

    }

    @Override
    public @Nullable JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public @Nullable PluginClassLoaderGroup getGroup() {
        return group;
    }

    @Override
    public void close() {
        group.remove(this);
        PaperClassLoaderStorage.instance().unregisterClassloader(this);
        try {
            mainClassLoader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addNewLoader(String pluginName, Function<String, Class<?>> classLoader) {
        childClassLoaders.put(pluginName, classLoader);
    }

    public void removeLoader(String pluginName) {
        childClassLoaders.remove(pluginName);
    }
}
