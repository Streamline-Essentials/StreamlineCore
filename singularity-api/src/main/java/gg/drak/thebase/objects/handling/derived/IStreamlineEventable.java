package gg.drak.thebase.objects.handling.derived;

public interface IStreamlineEventable extends IModifierEventable {
    @Override
    public default ModifierType getModifierType() {
        return ModifierType.STREAMLINE;
    }
}
