package gg.drak.thebase.objects.handling.derived;

public interface IModEventable extends IModifierEventable {
    @Override
    public default ModifierType getModifierType() {
        return ModifierType.MOD;
    }
}
