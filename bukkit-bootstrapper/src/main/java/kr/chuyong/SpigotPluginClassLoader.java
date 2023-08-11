package kr.chuyong;

import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.java.PluginClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SpigotPluginClassLoader extends PluginClassLoader {
    private final SpringSpigotContextClassLoader mainClassLoader;
    private final List<Function<String, Class<?>> > childClassLoaders = new ArrayList<>();
    private boolean isInit = false;
    public SpigotPluginClassLoader(JavaPluginLoader loader, ClassLoader parent, PluginDescriptionFile description, File dataFolder, File file, ClassLoader libraryLoader, SpringSpigotContextClassLoader mainLoader) throws IOException, InvalidPluginException, MalformedURLException {
        super(loader, parent, description, dataFolder, file, libraryLoader);
        this.mainClassLoader = mainLoader;
        isInit = true;
    }

    @Override
    public Class<?> loadClass0(String name, boolean resolve, boolean checkGlobal, boolean checkLibraries) throws ClassNotFoundException {
        if(!isInit){
            return super.loadClass0(name, resolve, checkGlobal, checkLibraries);
        }
        try{
            mainClassLoader.readSelf(name, resolve);
        }catch( Exception exe ) {

        }
        for(Function<String, Class<?>> loader : childClassLoaders) {
            Class<?> foundClass = loader.apply(name);
            if(foundClass != null) return foundClass;
        }

        throw new ClassNotFoundException();
    }

    public void addNewLoader(Function<String, Class<?>> classLoader) {
        childClassLoaders.add(classLoader);
    }
}
