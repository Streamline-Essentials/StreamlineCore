package gg.drak.thebase.objects;

public interface Classifiable<T> {
    /**
     * Gets the class that makes this object itself.
     * @return a class distinguished by T.
     */
    Class<T> getClassifier();
}
