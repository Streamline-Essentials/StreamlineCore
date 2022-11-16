package net.streamline.api.configs.given;

import net.streamline.api.SLAPI;
import tv.quaint.storage.resources.flat.FlatFileResource;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;
import tv.quaint.thebase.lib.leonhard.storage.Config;

import java.util.Arrays;
import java.util.List;

public class MainMessagesHandler extends SimpleConfiguration {
    public MainMessagesHandler() {
        super("main-messages.yml", SLAPI.getInstance().getDataFolder(), true);
    }

    @Override
    public void init() {

    }

    public enum MESSAGES {
        ;

        public enum INVALID {
            PERMISSIONS("invalid.permissions", "&cYou do not have enough permissions for this!"),

            PLAYER_SELF("invalid.player.self", "&cWe cannot find your player!"),
            PLAYER_OTHER("invalid.player.other", "&cWe cannot find that player!"),

            USER_SELF("invalid.user.self", "&cWe cannot find your user profile!"),
            USER_OTHER("invalid.user.other", "&cWe cannot find that user profile!"),

            ARGUMENTS_TOO_MANY("invalid.arguments.too.many", "&cYou specified too many arguments!"),
            ARGUMENTS_TOO_FEW("invalid.arguments.too.few", "&cYou specified too few arguments!"),
            ARGUMENTS_TYPE_DEFAULT("invalid.arguments.type.default", "&cOne of the arguments you supplied was not of a supported type!"),
            ARGUMENTS_TYPE_NUMBER("invalid.arguments.type.number", "&cOne of the arguments you supplied was supposed to be a number, but was not!"),

            WHITELIST_NOT("invalid.whitelist.not", "&cYou are not whitelisted!"),
            ;

            public final String key;

            public final String def;

            INVALID(String key) {
                this.key = key;
                this.def = "";
            }

            INVALID(String key, String def) {
                this.key = key;
                this.def = def;
            }

            public String get() {
                return MESSAGES.get(this.key, this.def);
            }
        }

        public enum DEFAULTS {
            IS_NULL("defaults.is-null", "&c&lNULL"),
            IS_TRUE("defaults.is-true", "&a&lTRUE"),
            IS_FALSE("defaults.is-false", "&c&lFALSE"),
            IS_ONLINE("defaults.is-online", "&a&lONLINE"),
            IS_OFFLINE("defaults.is-offline", "&c&lOFFLINE"),
            IS_PENDING("defaults.is-pending", "&c&lPENDING"),
            ;

            public enum PLACEHOLDERS {
                IS_NULL("defaults.placeholders.is-null", "%streamline_null%"),
                IS_TRUE("defaults.placeholders.is-true", "%streamline_true%"),
                IS_FALSE("defaults.placeholders.is-false", "%streamline_false%"),
                IS_ONLINE("defaults.placeholders.is-online", "%streamline_online%"),
                IS_OFFLINE("defaults.placeholders.is-offline", "%streamline_offline%"),
                IS_PENDING("defaults.placeholders.is-pending", "&c&lPENDING"),
                NO_PLAYER("defaults.placeholders.no-player", "&cNo Player Found"),
                LISTS_LAST("defaults.placeholders.lists.last-values", "&a%value%"),
                LISTS_BASE("defaults.placeholders.lists.base-values", "&a%value%&8, "),
                ;

                public final String key;

                public final String def;

                PLACEHOLDERS(String key) {
                    this.key = key;
                    this.def = "";
                }

                PLACEHOLDERS(String key, String def) {
                    this.key = key;
                    this.def = def;
                }

                public String get() {
                    return MESSAGES.get(this.key, this.def);
                }
            }

            public final String key;

            public final String def;

            DEFAULTS(String key) {
                this.key = key;
                this.def = "";
            }

            DEFAULTS(String key, String def) {
                this.key = key;
                this.def = def;
            }

            public String get() {
                return MESSAGES.get(this.key, this.def);
            }
        }

        public enum EXPERIENCE {
            ONCHANGE_TITLE_MAIN("experience.level.on-change.title.main", "&bLEVEL UP"),
            ONCHANGE_TITLE_SUBTITLE("experience.level.on-change.title.subtitle", "&6You are now level &a%streamline_user_level%&8!"),
            ONCHANGE_TITLE_IN("experience.level.on-change.title.in"),
            ONCHANGE_TITLE_STAY("experience.level.on-change.title.stay"),
            ONCHANGE_TITLE_OUT("experience.level.on-change.title.out"),

            ONCHANGE_CHAT("experience.level.on-change.chat"),
            ;

            public final String key;

            public final String def;

            EXPERIENCE(String key) {
                this.key = key;
                this.def = "";
            }

            EXPERIENCE(String key, String def) {
                this.key = key;
                this.def = def;
            }

            public String get() {
                return MESSAGES.get(this.key, this.def);
            }

            public int getInt() {
                return GivenConfigs.getMainMessages().getResource().getInt(this.key);
            }

            public List<String> getStringList() {
                return GivenConfigs.getMainMessages().getResource().getStringList(this.key);
            }
        }

        public static String get(String key) {
            return GivenConfigs.getMainMessages().getResource().getString(key);
        }

        public static String get(String key, String def) {
            return GivenConfigs.getMainMessages().getResource().getOrSetDefault(key, def);
        }
    }
}
