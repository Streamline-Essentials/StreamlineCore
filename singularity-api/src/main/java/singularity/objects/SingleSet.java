package singularity.objects;

import gg.drak.thebase.objects.Identifiable;
import lombok.Getter;
import lombok.Setter;

/**
 * A generic key-value pair that implements {@link Identifiable}.
 *
 * <p>The identifier is formed by concatenating the key and value
 * representations with a {@code ---} delimiter, and can be parsed
 * back into a key and value via {@link #setIdentifier(String)}.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
@Setter
@Getter
public class SingleSet<K, V> implements Identifiable {

    /** The key of this pair. */
    private K key;

    /** The value of this pair. */
    private V value;

    /**
     * Constructs a new {@code SingleSet} with the given key and value.
     *
     * @param key   the key; must not be {@code null} if used for identifier generation
     * @param value the value; must not be {@code null} if used for identifier generation
     */
    public SingleSet(K key, V value){
        this.key = key;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The identifier is {@code key.toString() + "---" + value.toString()}.
     */
    @Override
    public String getIdentifier() {
        return key.toString() + "---" + value.toString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Splits the given identifier on the first {@code ---} delimiter and
     * assigns the left part to {@link #key} and the right part to {@link #value}.
     * Both are stored as their raw {@link String} representations, requiring an
     * unchecked cast to {@code K} / {@code V} respectively.
     *
     * @param identifier the combined identifier string in the form {@code key---value}
     */
    @Override
    public void setIdentifier(String identifier) {
        String[] split = identifier.split("---", 2);
        key = (K) split[0];
        value = (V) split[1];
    }
}
