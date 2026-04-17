package singularity.data.players.meta;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.objects.Identifiable;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A key-value metadata entry that can be attached to a {@link SenderMeta}.
 *
 * <p>Each tag stores its value in serialized (string) form and carries a
 * {@link MetaRetriever} capable of deserializing that string back to the
 * original typed object. Convenience constructors are provided for the most
 * common primitive types as well as for typed {@link List} values.</p>
 *
 * @param <O> the type of the deserialized value produced by the retriever
 */
@Getter @Setter
public class MetaTag<O> implements Identifiable {

    /**
     * The unique key that identifies this tag within a {@link SenderMeta}.
     */
    private String identifier;

    /**
     * The string representation of this tag's value, used for storage and transfer.
     */
    private String serializedValue;

    /**
     * The function responsible for converting {@link #serializedValue} back to type {@code O}.
     */
    private MetaRetriever<O> objectReturner;

    /**
     * Constructs a {@code MetaTag} with full control over serialization and deserialization.
     *
     * @param identifier     the unique key for this tag
     * @param valueGetter    a supplier whose result is stored as the serialized value
     * @param objectReturner a function that converts the serialized string back to {@code O}
     */
    public MetaTag(String identifier, Supplier<String> valueGetter, MetaRetriever<O> objectReturner) {
        this.identifier = identifier;
        this.serializedValue = valueGetter.get();
        this.objectReturner = objectReturner;
    }

    /**
     * Constructs a {@code MetaTag} whose value is a plain {@link String}.
     * The retriever simply returns the string as-is.
     *
     * @param identifier the unique key for this tag
     * @param value      the string value to store
     */
    public MetaTag(String identifier, String value) {
        this(identifier, () -> value, (s) -> (O) s);
    }

    /**
     * Constructs a {@code MetaTag} whose value is a {@code boolean}.
     *
     * @param identifier the unique key for this tag
     * @param value      the boolean value to store
     */
    public MetaTag(String identifier, boolean value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Boolean.valueOf(s));
    }

    /**
     * Constructs a {@code MetaTag} whose value is an {@code int}.
     *
     * @param identifier the unique key for this tag
     * @param value      the integer value to store
     */
    public MetaTag(String identifier, int value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Integer.valueOf(s));
    }

    /**
     * Constructs a {@code MetaTag} whose value is a {@code long}.
     *
     * @param identifier the unique key for this tag
     * @param value      the long value to store
     */
    public MetaTag(String identifier, long value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Long.valueOf(s));
    }

    /**
     * Constructs a {@code MetaTag} whose value is a {@code double}.
     *
     * @param identifier the unique key for this tag
     * @param value      the double value to store
     */
    public MetaTag(String identifier, double value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Double.valueOf(s));
    }

    /**
     * Constructs a {@code MetaTag} whose value is a {@code float}.
     *
     * @param identifier the unique key for this tag
     * @param value      the float value to store
     */
    public MetaTag(String identifier, float value) {
        this(identifier, () -> String.valueOf(value), (s) -> (O) Float.valueOf(s));
    }

    /**
     * Constructs a {@code MetaTag} whose value is a {@link List} of typed elements.
     *
     * <p>Each element is serialized to a delimited token of the form
     * {@code !!!<element>;;} and concatenated into a single string. When
     * the retriever is invoked the tokens are parsed back via regex and each
     * element is individually deserialized using the supplied {@code objectReturner}.</p>
     *
     * @param <T>            the element type of the list
     * @param identifier     the unique key for this tag
     * @param value          the list of elements to store
     * @param objectReturner a retriever that deserializes each individual list element
     */
    public <T> MetaTag(String identifier, List<T> value, MetaRetriever<T> objectReturner) {
        this(identifier, () -> {
            StringBuilder builder = new StringBuilder();
            for (T t : value) {
                builder.append("!!!").append(t).append(";;");
            }
            return builder.toString();
        }, (s) -> {
            List<T> r = new ArrayList<>();

            Matcher matcher = MatcherUtils.matcherBuilder("(!!!)(.*?)(;;)", s);
            List<String[]> matches = MatcherUtils.getGroups(matcher, 3);
            for (String[] match : matches) {
                try {
                    T t = objectReturner.apply(match[1]);
                    r.add(t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return (O) r;
        });
    }
}
