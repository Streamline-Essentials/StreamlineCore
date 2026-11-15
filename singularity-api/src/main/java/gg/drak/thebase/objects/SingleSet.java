package gg.drak.thebase.objects;

import lombok.Getter;
import lombok.Setter;

public class SingleSet<K, V> {
    @Getter @Setter
    private K key;
    @Getter @Setter
    private V value;

    public SingleSet(K key, V value){
        this.key = key;
        this.value = value;
    }
}
