package gg.drak.thebase.objects.handling.derived;

public interface IPluginEventable extends IModifierEventable {
    @Override
    public default ModifierType getModifierType() {
        return ModifierType.PLUGIN;
    }
}
