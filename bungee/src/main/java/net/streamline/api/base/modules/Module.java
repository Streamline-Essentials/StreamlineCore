package net.streamline.api.base.modules;

import net.md_5.bungee.api.ProxyServer;
import net.streamline.api.base.BasePlugin;
import net.streamline.api.base.command.TabExecutor;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

public interface Module extends TabExecutor {
    public File getDataFolder();
    public ModuleDescriptionFile getDescription();
    /*getConfig();*/
    public InputStream getResource(String filename);
    public void saveConfig();
    public void saveDefaultConfig();
    public void saveResource(String resourcePath, boolean replace);
    public void reloadConfig();
    public ModuleLoader getModuleLoader();
    public BasePlugin getBase();
    public boolean isEnabled();
    public void onDisable();
    public void onLoad();
    public void onEnable();
    public boolean isNaggable();
    public void setNaggable(boolean canNag);
    public Logger getLogger();
    public String getName();

}
