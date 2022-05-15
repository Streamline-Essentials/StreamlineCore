package net.streamline.api.base.modules;

public abstract class BaseModule {
    String identifier;
    boolean enabled;
    String path;

    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void onLoad();
    public abstract void onReload();


    public boolean isEnabled() {return enabled;}
    public String getIdentifier() {return identifier;}
    public String getPath() {return path;}

}
