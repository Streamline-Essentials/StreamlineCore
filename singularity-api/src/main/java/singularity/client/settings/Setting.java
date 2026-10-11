package singularity.client.settings;

import lombok.Getter;

import java.util.List;
import java.util.function.Consumer;

/**
 * One option on a client module's page in the {@code /slclient menu} menu. The menu draws a
 * control for each kind ({@link Kind}) and saves the value as text.
 *
 * @param <T> the value type
 */
@Getter
public abstract class Setting<T> {

    /** The control the menu draws. */
    public enum Kind {
        TOGGLE,
        SLIDER,
        CHOICE,
        TEXT,
        COLOR,
    }

    private final String key;
    private final String displayName;
    private final String description;
    private final T defaultValue;
    private volatile T value;
    private volatile Consumer<T> onChange;

    protected Setting(String key, String displayName, String description, T defaultValue) {
        this.key = key;
        this.displayName = displayName;
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public abstract Kind getKind();

    /** @return the value parsed from saved text, or {@code null} if it does not parse */
    protected abstract T parse(String text);

    /** @return the value as saved text */
    public String format(T value) {
        return String.valueOf(value);
    }

    /** @return the current value as saved text */
    public String formatValue() {
        return format(value);
    }

    /** Sets the value as the menu does: normalised, then reported to the listener. */
    public void set(T newValue) {
        T normalised = normalise(newValue);
        if (normalised == null) return;
        this.value = normalised;
        Consumer<T> listener = onChange;
        if (listener != null) listener.accept(normalised);
    }

    /** Sets the value from saved or typed text, keeping the current value if it does not parse. */
    public void setFromText(String text) {
        T parsed = text == null ? null : parse(text);
        if (parsed != null) set(parsed);
    }

    public void reset() {
        set(defaultValue);
    }

    /** @return {@code value} brought into range, or {@code null} to reject it */
    protected T normalise(T value) {
        return value;
    }

    /** Called with every new value, from whichever thread set it. */
    public Setting<T> onChange(Consumer<T> listener) {
        this.onChange = listener;
        return this;
    }

    /** A {@link Kind#TOGGLE}. */
    public static final class Toggle extends Setting<Boolean> {
        public Toggle(String key, String displayName, String description, boolean defaultValue) {
            super(key, displayName, description, defaultValue);
        }

        @Override
        public Kind getKind() {
            return Kind.TOGGLE;
        }

        @Override
        protected Boolean parse(String text) {
            return Boolean.parseBoolean(text.trim());
        }
    }

    /** A {@link Kind#SLIDER} over {@code min..max}, snapped to {@code step}. */
    @Getter
    public static final class Slider extends Setting<Double> {
        private final double min;
        private final double max;
        private final double step;

        public Slider(String key, String displayName, String description, double defaultValue, double min, double max, double step) {
            super(key, displayName, description, defaultValue);
            this.min = min;
            this.max = max;
            this.step = step;
        }

        @Override
        public Kind getKind() {
            return Kind.SLIDER;
        }

        @Override
        protected Double parse(String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        protected Double normalise(Double value) {
            if (value == null || value.isNaN()) return null;
            double snapped = step > 0 ? min + Math.round((value - min) / step) * step : value;
            return Math.max(min, Math.min(max, snapped));
        }
    }

    /** A {@link Kind#CHOICE} of one of {@code options}. */
    @Getter
    public static final class Choice extends Setting<String> {
        private final List<String> options;

        public Choice(String key, String displayName, String description, String defaultValue, List<String> options) {
            super(key, displayName, description, defaultValue);
            this.options = options;
        }

        @Override
        public Kind getKind() {
            return Kind.CHOICE;
        }

        @Override
        protected String parse(String text) {
            return text.trim();
        }

        @Override
        protected String normalise(String value) {
            return options.contains(value) ? value : null;
        }
    }

    /** A {@link Kind#TEXT} field, at most {@code maxLength} characters. */
    @Getter
    public static final class Text extends Setting<String> {
        private final int maxLength;

        public Text(String key, String displayName, String description, String defaultValue, int maxLength) {
            super(key, displayName, description, defaultValue);
            this.maxLength = maxLength;
        }

        @Override
        public Kind getKind() {
            return Kind.TEXT;
        }

        @Override
        protected String parse(String text) {
            return text;
        }

        @Override
        protected String normalise(String value) {
            if (value == null) return null;
            return value.length() > maxLength ? value.substring(0, maxLength) : value;
        }
    }

    /** A {@link Kind#COLOR}, as ARGB; six hex digits are taken as fully opaque. */
    public static final class Color extends Setting<Integer> {
        public Color(String key, String displayName, String description, int defaultArgb) {
            super(key, displayName, description, defaultArgb);
        }

        @Override
        public Kind getKind() {
            return Kind.COLOR;
        }

        @Override
        protected Integer parse(String text) {
            String hex = text.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.isEmpty() || hex.length() > 8) return null;
            try {
                long parsed = Long.parseLong(hex, 16);
                return hex.length() <= 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String format(Integer value) {
            return String.format("#%08X", value);
        }
    }
}
