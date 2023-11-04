package net.streamline.api.objects;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SingleSet<K, V> {
    @Setter
    private K key;
    @Setter
    private V value;

    public SingleSet(K key, V value){
        this.key = key;
        this.value = value;
    }
}
