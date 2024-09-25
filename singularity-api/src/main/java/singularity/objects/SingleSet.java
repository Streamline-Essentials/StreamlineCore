package singularity.objects;

import lombok.Getter;
import lombok.Setter;
import tv.quaint.objects.Identifiable;

@Setter
@Getter
public class SingleSet<K, V> implements Identifiable {
    private K key;
    private V value;

    public SingleSet(K key, V value){
        this.key = key;
        this.value = value;
    }

    @Override
    public String getIdentifier() {
        return key.toString() + "---" + value.toString();
    }

    @Override
    public void setIdentifier(String identifier) {
        String[] split = identifier.split("---", 2);
        key = (K) split[0];
        value = (V) split[1];
    }
}
