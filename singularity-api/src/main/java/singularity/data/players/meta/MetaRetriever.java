package singularity.data.players.meta;

import java.util.function.Function;

/**
 * A specialised {@link Function} that deserializes a raw string value into an
 * object of type {@code O}.
 *
 * <p>Implementations are supplied to {@link MetaTag} so that the tag's
 * serialized string can be converted back to its original type on demand.</p>
 *
 * @param <O> the deserialized object type produced by this retriever
 */
public interface MetaRetriever<O> extends Function<String, O> {
}
