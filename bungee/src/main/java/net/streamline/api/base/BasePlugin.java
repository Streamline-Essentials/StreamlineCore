package net.streamline.api.base;

import net.md_5.bungee.api.plugin.Plugin;

public abstract class BasePlugin extends Plugin {
        static String name;
        static String version;
        BasePlugin instance;

        @Override
        public void onEnable() {
            name = "StreamlineAPI";
            version = "${project.version}";
            this.instance = this;
            this.enable();
        }
        @Override
        public void onDisable() {
            this.disable();
        }
        @Override
        public void onLoad() {
            this.load();
        }

        abstract public void enable();
        abstract public void disable();
        abstract public void load();
        abstract public void reload();

        public String getName() {return name;}
        public String getVersion() {return version;}
        public BasePlugin getInstance() {return this.instance;}
}
