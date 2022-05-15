package net.streamline.api.base.modules;

public abstract class BaseModule implements Module {
    @Override public final int hashCode() {return getName().hashCode();}
    @Override public final boolean equals(Object obj) {if(this==obj){return true;}if(obj==null){return false;}if(!(obj instanceof Module)){return false;}return getName().equals(((Module) obj).getName());}
    public final String getName() {return getDescription().getName();}
}
