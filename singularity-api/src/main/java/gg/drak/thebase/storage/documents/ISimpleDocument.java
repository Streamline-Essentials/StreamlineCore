package gg.drak.thebase.storage.documents;

public interface ISimpleDocument {
    public void init();

    public void save();

    public void delete();

    public boolean exists();
}
