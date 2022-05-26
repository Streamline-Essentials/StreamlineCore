package net.streamline.api.base.modules;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;

public abstract class BaseModule implements Module {
    @Override public final int hashCode() {return getName().hashCode();}
    @Override public final boolean equals(Object obj) {if(this==obj){return true;}if(obj==null){return false;}if(!(obj instanceof Module)){return false;}return getName().equals(((Module) obj).getName());}
    public final String getName() {return getDescription().getName();}

    public abstract ModuleLoader getPluginLoader();

    public abstract boolean onCommand(CommandSender sender, Command command, String label, String[] args);

    public abstract List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args);
}
