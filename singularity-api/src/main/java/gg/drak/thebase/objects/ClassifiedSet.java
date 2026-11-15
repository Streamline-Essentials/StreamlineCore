package gg.drak.thebase.objects;

public class ClassifiedSet<T, O extends Classifiable<T>> extends SingleSet<Class<T>, O> {
    public ClassifiedSet(O value) {
        super(value.getClassifier(), value);
    }
}
