package net.streamline.api.base;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.logging.Logger;

public abstract class BasePlugin extends Plugin {
        String name;
        String version;
        BasePlugin instance;

        @Override
        public void onEnable() {
            name = "StreamlineAPI";
            version = "${project.version}";
        }

        abstract public void enable();
        abstract public void disable();
        abstract public void reload();

        public String getName() {return this.name;}
        public String getVersion() {return this.version;}
}
