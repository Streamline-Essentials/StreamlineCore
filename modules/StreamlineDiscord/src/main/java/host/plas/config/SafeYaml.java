package host.plas.config;

import gg.drak.thebase.lib.leonhard.storage.Yaml;
import gg.drak.thebase.lib.leonhard.storage.internal.settings.ConfigSettings;
import host.plas.StreamlineDiscord;
import singularity.configs.ModularizedConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Writes to the module's YAML files without tripping the YAML library's comment keeper.
 *
 * <p>The library keeps comments by matching every line it writes to a key. A blank line in the written file
 * has no key, and matching it throws ({@code StringIndexOutOfBoundsException} in {@code YamlParser}). A text
 * holding a line break is written as a quoted string spread over several lines, with a blank line for every
 * break, so one multi-line message makes every later write of that file fail. Multi-line texts are therefore
 * stored as a list of lines, which reads back the same through {@link Messages#getText(String)}; a write that
 * still fails, because the file already held a multi-line text, is repeated without keeping comments.</p>
 */
public final class SafeYaml {
    private SafeYaml() {
    }

    /** {@code value} in a form that writes no blank lines: a text with line breaks becomes its list of lines. */
    public static Object storable(Object value) {
        if (value instanceof String && ((String) value).indexOf('\n') >= 0) {
            return new ArrayList<>(Arrays.asList(((String) value).replace("\r", "").split("\n", -1)));
        }
        return value;
    }

    /** Writes {@code value} under {@code key}, stored by {@link #storable(Object)}. */
    public static void write(ModularizedConfig config, String key, Object value) {
        Object stored = storable(value);
        withFallback(config, () -> config.write(key, stored));
    }

    /** Removes {@code key}. */
    public static void remove(ModularizedConfig config, String key) {
        withFallback(config, () -> config.getResource().remove(key));
    }

    /**
     * Rewrites every multi-line text in the file as a list of lines, so later writes keep the file's comments.
     * Returns how many texts were rewritten.
     */
    public static int normalize(ModularizedConfig config) {
        List<String> multiLine = new ArrayList<>();
        try {
            Map<String, Object> flat = config.getResource().getFileData().toMap();
            collect("", flat, multiLine);
        } catch (Throwable t) {
            StreamlineDiscord.getInstance().logWarning("Could not read " + config.getResource().getName() + " to check its texts: " + t);
            return 0;
        }

        for (String key : multiLine) {
            Object value = config.getResource().get(key);
            write(config, key, value);
        }
        return multiLine.size();
    }

    @SuppressWarnings("unchecked")
    private static void collect(String prefix, Map<String, Object> map, List<String> into) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                collect(key, (Map<String, Object>) value, into);
            } else if (value instanceof String && ((String) value).indexOf('\n') >= 0) {
                into.add(key);
            }
        }
    }

    private static void withFallback(ModularizedConfig config, Runnable action) {
        try {
            action.run();
            return;
        } catch (RuntimeException e) {
            if (! (config.getResource() instanceof Yaml)) throw e;
        }

        Yaml yaml = config.getResource();
        ConfigSettings before = yaml.getConfigSettings();
        StreamlineDiscord.getInstance().logWarning("Could not keep the comments in " + yaml.getName()
                + " while saving it, because a text in it spans several lines. Saving it without them.");
        try {
            yaml.setConfigSettings(ConfigSettings.SKIP_COMMENTS);
            action.run();
        } finally {
            yaml.setConfigSettings(before);
        }
    }
}
