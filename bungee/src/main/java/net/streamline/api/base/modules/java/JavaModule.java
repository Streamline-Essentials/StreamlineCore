package net.streamline.api.base.modules.java;

import net.md_5.bungee.api.ProxyServer;
import net.streamline.api.base.modules.BaseModule;
import net.streamline.api.base.modules.ModuleDescriptionFile;
import net.streamline.api.base.modules.ModuleLoader;
import net.streamline.api.base.modules.ModuleLogger;

import java.io.File;

public class JavaModule extends BaseModule {
    private boolean isEnabled = false;
    private ModuleLoader loader = null;
    private ProxyServer server = null;
    private ModuleDescriptionFile description = null;
    private File dataFolder = null;
    private ClassLoader classLoader = null;
    private boolean naggable = true;
    /*private FileConfiguration newConfig = null;*/
    private File configFile = null;
    private ModuleLogger logger = null;

    public JavaModule() {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if(!(classLoader instanceof ModuleClassLoader)) {

        }
    }

}
