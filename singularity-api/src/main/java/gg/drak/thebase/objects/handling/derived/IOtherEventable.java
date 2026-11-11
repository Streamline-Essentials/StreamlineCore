package gg.drak.thebase.objects.handling.derived;

public interface IOtherEventable extends IModifierEventable {
    @Override
    public default ModifierType getModifierType() {
        return ModifierType.OTHER;
    }
}
