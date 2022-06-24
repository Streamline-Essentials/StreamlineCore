package net.streamline.utils.objects;

public class SingleSet<K, V> {
    public K key;
    public V value;

    public SingleSet(K key, V value){
        this.key = key;
        this.value = value;
    }

    public void updateKey(K key){
        this.key = key;
    }

    public void updateValue(V value){
        this.value = value;
    }
}
