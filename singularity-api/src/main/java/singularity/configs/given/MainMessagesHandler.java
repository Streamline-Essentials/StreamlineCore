package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import singularity.Singularity;

import java.util.List;

/**
 * Configuration handler for user-facing message strings, backed by
 * {@code main-messages.yml} in the plugin's data folder.
 *
 * <p>All localised strings are accessed via the static nested enum hierarchy
 * inside {@link MESSAGES}. Each enum constant carries a YAML key and a default
 * value; {@code get()} resolves the live value (writing the default if absent)
 * through {@link GivenConfigs#getMainMessages()}.</p>
 */
public class MainMessagesHandler extends SimpleConfiguration {

    /**
     * Constructs the handler, loading (or creating) {@code main-messages.yml}
     * in the Singularity plugin's data folder as a self-contained resource.
     */
    public MainMessagesHandler() {
        super("main-messages.yml", Singularity.getInstance().getDataFolder(), true);
    }

    /**
     * No-op; message defaults are applied lazily when each enum constant's
     * {@code get()} method is first called.
     */
    @Override
    public void init() {

    }

    /**
     * Top-level namespace for all configurable message strings.
     * Contains static helper methods and nested enums grouped by category.
     */
    public enum MESSAGES {
        ;

        /**
         * Messages displayed when a player action or request is invalid
         * (e.g. missing permissions, unresolvable player, bad arguments).
         */
        public enum INVALID {
            /** Sent when the player lacks permission for a command. */
            PERMISSIONS("invalid.permissions", "&cYou do not have enough permissions for this!"),

            /** Sent when the executing player's own profile cannot be found. */
            PLAYER_SELF("invalid.player.self", "&cWe cannot find your player!"),
            /** Sent when a targeted player cannot be found. */
            PLAYER_OTHER("invalid.player.other", "&cWe cannot find that player!"),

            /** Sent when the executing player's own user profile cannot be found. */
            USER_SELF("invalid.user.self", "&cWe cannot find your user profile!"),
            /** Sent when a targeted user profile cannot be found. */
            USER_OTHER("invalid.user.other", "&cWe cannot find that user profile!"),

            /** Sent when more arguments are supplied than a command accepts. */
            ARGUMENTS_TOO_MANY("invalid.arguments.too.many", "&cYou specified too many arguments!"),
            /** Sent when fewer arguments are supplied than a command requires. */
            ARGUMENTS_TOO_FEW("invalid.arguments.too.few", "&cYou specified too few arguments!"),
            /** Sent when an argument cannot be parsed to any supported type. */
            ARGUMENTS_TYPE_DEFAULT("invalid.arguments.type.default", "&cOne of the arguments you supplied was not of a supported type!"),
            /** Sent when an argument was expected to be numeric but was not. */
            ARGUMENTS_TYPE_NUMBER("invalid.arguments.type.number", "&cOne of the arguments you supplied was supposed to be a number, but was not!"),

            /** Sent when a player is denied entry because they are not whitelisted. */
            WHITELIST_NOT("invalid.whitelist.not", "&cYou are not whitelisted!"),
            ;

            /** The YAML key used to look up this message in {@code main-messages.yml}. */
            public final String key;

            /** The fallback string used when the key is not present in the file. */
            public final String def;

            /**
             * Creates an {@code INVALID} constant with an empty default value.
             *
             * @param key the YAML key for this message
             */
            INVALID(String key) {
                this.key = key;
                this.def = "";
            }

            /**
             * Creates an {@code INVALID} constant with the given default value.
             *
             * @param key the YAML key for this message
             * @param def the fallback string when the key is absent
             */
            INVALID(String key, String def) {
                this.key = key;
                this.def = def;
            }

            /**
             * Resolves the live message value from the config, writing the
             * default if the key does not yet exist.
             *
             * @return the current message string for this constant
             */
            public String get() {
                return MESSAGES.get(this.key, this.def);
            }
        }

        /**
         * Generic display strings used to represent common boolean or status
         * values across commands and placeholders.
         */
        public enum DEFAULTS {
            /** Display string for a null value. */
            IS_NULL("defaults.is-null", "&c&lNULL"),
            /** Display string for a {@code true} boolean. */
            IS_TRUE("defaults.is-true", "&a&lTRUE"),
            /** Display string for a {@code false} boolean. */
            IS_FALSE("defaults.is-false", "&c&lFALSE"),
            /** Display string for an online status. */
            IS_ONLINE("defaults.is-online", "&a&lONLINE"),
            /** Display string for an offline status. */
            IS_OFFLINE("defaults.is-offline", "&c&lOFFLINE"),
            /** Display string for a pending/unknown status. */
            IS_PENDING("defaults.is-pending", "&c&lPENDING"),
            ;

            /**
             * Placeholder string representations used inside
             * PlaceholderAPI-style expressions.
             */
            public enum PLACEHOLDERS {
                /** Placeholder resolved when a value is null. */
                IS_NULL("defaults.placeholders.is-null", "%streamline_null%"),
                /** Placeholder resolved when a value is {@code true}. */
                IS_TRUE("defaults.placeholders.is-true", "%streamline_true%"),
                /** Placeholder resolved when a value is {@code false}. */
                IS_FALSE("defaults.placeholders.is-false", "%streamline_false%"),
                /** Placeholder resolved when a player is online. */
                IS_ONLINE("defaults.placeholders.is-online", "%streamline_online%"),
                /** Placeholder resolved when a player is offline. */
                IS_OFFLINE("defaults.placeholders.is-offline", "%streamline_offline%"),
                /** Placeholder resolved when a status is pending. */
                IS_PENDING("defaults.placeholders.is-pending", "&c&lPENDING"),
                /** Placeholder used when no player is found. */
                NO_PLAYER("defaults.placeholders.no-player", "&cNo Player Found"),
                /** Format for the last element of a list. */
                LISTS_LAST("defaults.placeholders.lists.last-values", "&a%value%"),
                /** Format for non-last elements of a list (includes trailing separator). */
                LISTS_BASE("defaults.placeholders.lists.base-values", "&a%value%&8, "),
                ;

                /** The YAML key used to look up this placeholder string. */
                public final String key;

                /** The fallback string used when the key is not present in the file. */
                public final String def;

                /**
                 * Creates a {@code PLACEHOLDERS} constant with an empty default value.
                 *
                 * @param key the YAML key for this placeholder string
                 */
                PLACEHOLDERS(String key) {
                    this.key = key;
                    this.def = "";
                }

                /**
                 * Creates a {@code PLACEHOLDERS} constant with the given default value.
                 *
                 * @param key the YAML key for this placeholder string
                 * @param def the fallback string when the key is absent
                 */
                PLACEHOLDERS(String key, String def) {
                    this.key = key;
                    this.def = def;
                }

                /**
                 * Resolves the live placeholder string from the config, writing the
                 * default if the key does not yet exist.
                 *
                 * @return the current placeholder string for this constant
                 */
                public String get() {
                    return MESSAGES.get(this.key, this.def);
                }
            }

            /** The YAML key used to look up this display string. */
            public final String key;

            /** The fallback string used when the key is not present in the file. */
            public final String def;

            /**
             * Creates a {@code DEFAULTS} constant with an empty default value.
             *
             * @param key the YAML key for this display string
             */
            DEFAULTS(String key) {
                this.key = key;
                this.def = "";
            }

            /**
             * Creates a {@code DEFAULTS} constant with the given default value.
             *
             * @param key the YAML key for this display string
             * @param def the fallback string when the key is absent
             */
            DEFAULTS(String key, String def) {
                this.key = key;
                this.def = def;
            }

            /**
             * Resolves the live display string from the config, writing the
             * default if the key does not yet exist.
             *
             * @return the current display string for this constant
             */
            public String get() {
                return MESSAGES.get(this.key, this.def);
            }
        }

        /**
         * Messages and title/subtitle strings shown to players on experience
         * and level-change events.
         */
        public enum EXPERIENCE {
            /** Main title text shown when the player levels up. */
            ONCHANGE_TITLE_MAIN("experience.level.on-change.title.main", "&bLEVEL UP"),
            /** Subtitle text shown when the player levels up. */
            ONCHANGE_TITLE_SUBTITLE("experience.level.on-change.title.subtitle", "&6You are now level &a%streamline_user_level%&8!"),
            /** Fade-in duration (ticks) for the level-up title. */
            ONCHANGE_TITLE_IN("experience.level.on-change.title.in"),
            /** Stay duration (ticks) for the level-up title. */
            ONCHANGE_TITLE_STAY("experience.level.on-change.title.stay"),
            /** Fade-out duration (ticks) for the level-up title. */
            ONCHANGE_TITLE_OUT("experience.level.on-change.title.out"),

            /** Chat message sent to the player when they level up. */
            ONCHANGE_CHAT("experience.level.on-change.chat"),
            ;

            /** The YAML key used to look up this experience message. */
            public final String key;

            /** The fallback string used when the key is not present in the file. */
            public final String def;

            /**
             * Creates an {@code EXPERIENCE} constant with an empty default value.
             *
             * @param key the YAML key for this message
             */
            EXPERIENCE(String key) {
                this.key = key;
                this.def = "";
            }

            /**
             * Creates an {@code EXPERIENCE} constant with the given default value.
             *
             * @param key the YAML key for this message
             * @param def the fallback string when the key is absent
             */
            EXPERIENCE(String key, String def) {
                this.key = key;
                this.def = def;
            }

            /**
             * Resolves the live string value from the config, writing the
             * default if the key does not yet exist.
             *
             * @return the current message string for this constant
             */
            public String get() {
                return MESSAGES.get(this.key, this.def);
            }

            /**
             * Reads the value at this constant's key as an integer. Intended for
             * title timing constants (fade-in, stay, fade-out) stored as tick counts.
             *
             * @return the integer value, or {@code 0} if the key is absent or non-numeric
             */
            public int getInt() {
                return GivenConfigs.getMainMessages().getResource().getInt(this.key);
            }

            /**
             * Reads the value at this constant's key as a list of strings.
             * Useful for multi-line chat messages.
             *
             * @return the string list value, or an empty list if the key is absent
             */
            public List<String> getStringList() {
                return GivenConfigs.getMainMessages().getResource().getStringList(this.key);
            }
        }

        /**
         * Retrieves the raw string value stored at the given key in
         * {@code main-messages.yml}, returning {@code null} if absent.
         *
         * @param key the YAML path to look up
         * @return the stored string, or {@code null} if the key does not exist
         */
        public static String get(String key) {
            return GivenConfigs.getMainMessages().getResource().getString(key);
        }

        /**
         * Retrieves the string value stored at the given key, writing and
         * returning the default if the key is not yet present.
         *
         * @param key the YAML path to look up
         * @param def the value to write and return if the key is absent
         * @return the current or newly written string value
         */
        public static String get(String key, String def) {
            return GivenConfigs.getMainMessages().getResource().getOrSetDefault(key, def);
        }
    }
}
