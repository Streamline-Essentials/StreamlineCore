package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SingleSet<K, V> {
    private K key;
    private V value;

    public SingleSet(K key, V value){
        this.key = key;
        this.value = value;
    }
}
