package gg.drak.thebase.storage.datastores;

public interface ISimpleDatastore<T> {
    public void init(T object);

    public void save(T object);

    public void delete(String key);

    public boolean exists(String key);

    public T get(String key);
}
