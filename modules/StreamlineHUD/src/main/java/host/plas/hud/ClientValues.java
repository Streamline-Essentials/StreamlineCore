package host.plas.hud;

import singularity.client.placeholders.ClientPlaceholders;

import java.util.Map;
import java.util.function.Supplier;

/**
 * StreamlineClient's own {@code %streamlineclient_<key>%} placeholders, such as chroma colors.
 *
 * <p>{@link ClientPlaceholders} is newer than the first cores that run client modules. On one of
 * those this class fails to load, so it is only ever called inside a {@code catch (LinkageError)},
 * and those placeholders then stay as typed.</p>
 */
final class ClientValues {

    private ClientValues() {}

    /** @return the value behind {@code key}, or {@code null} if the core has no such placeholder */
    static Placeholders.Value get(String key) {
        Supplier<String> value = ClientPlaceholders.get(key);
        return value == null ? null : info -> value.get();
    }

    static Map<String, String> descriptions() {
        return ClientPlaceholders.descriptions();
    }
}
