package net.streamline.api.configs.given;

import de.leonhard.storage.Config;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.FlatFileResource;

import java.util.List;

public class MainMessagesHandler extends FlatFileResource<Config> {
    public MainMessagesHandler() {
        super(Config.class, "main-messages.yml", SLAPI.getInstance().getDataFolder(), true);
    }

    public enum MESSAGES {
        ;

        public enum INVALID {
            PERMISSIONS("invalid.permissions"),
            PLAYER_SELF("invalid.player.self"),
            PLAYER_OTHER("invalid.player.other"),
            USER_SELF("invalid.user.self"),
            USER_OTHER("invalid.user.other"),
            ARGUMENTS_TOO_MANY("invalid.arguments.too.many"),
            ARGUMENTS_TOO_FEW("invalid.arguments.too.few"),
            ARGUMENTS_TYPE_DEFAULT("invalid.arguments.type.default"),
            ARGUMENTS_TYPE_NUMBER("invalid.arguments.type.number"),
            ;

            public final String key;

            INVALID(String key) {
                this.key = key;
            }

            public String get() {
                return MESSAGES.get(this.key);
            }
        }

        public enum DEFAULTS {
            IS_NULL("defaults.is-null"),
            IS_TRUE("defaults.is-true"),
            IS_FALSE("defaults.is-false"),
            IS_ONLINE("defaults.is-online"),
            IS_OFFLINE("defaults.is-offline"),
            ;

            public enum PLACEHOLDERS {
                IS_NULL("defaults.placeholders.is-null"),
                IS_TRUE("defaults.placeholders.is-true"),
                IS_FALSE("defaults.placeholders.is-false"),
                IS_ONLINE("defaults.placeholders.is-online"),
                IS_OFFLINE("defaults.placeholders.is-offline"),
                NO_PLAYER("defaults.placeholders.no-player"),
                LISTS_LAST("defaults.placeholders.lists.last-values"),
                LISTS_BASE("defaults.placeholders.lists.base-values"),
                ;

                public final String key;

                PLACEHOLDERS(String key) {
                    this.key = key;
                }

                public String get() {
                    return MESSAGES.get(this.key);
                }
            }

            public final String key;

            DEFAULTS(String key) {
                this.key = key;
            }

            public String get() {
                return MESSAGES.get(this.key);
            }
        }

        public enum EXPERIENCE {
            ONCHANGE_TITLE_MAIN("experience.level.on-change.title.main"),
            ONCHANGE_TITLE_SUBTITLE("experience.level.on-change.title.subtitle"),
            ONCHANGE_TITLE_IN("experience.level.on-change.title.in"),
            ONCHANGE_TITLE_STAY("experience.level.on-change.title.stay"),
            ONCHANGE_TITLE_OUT("experience.level.on-change.title.out"),

            ONCHANGE_CHAT("experience.level.on-change.chat"),
            ;

            public final String key;

            EXPERIENCE(String key) {
                this.key = key;
            }

            public String get() {
                return MESSAGES.get(this.key);
            }

            public int getInt() {
                return GivenConfigs.getMainMessages().resource.getInt(this.key);
            }

            public List<String> getStringList() {
                return GivenConfigs.getMainMessages().resource.getStringList(this.key);
            }
        }

        public static String get(String key) {
            return GivenConfigs.getMainMessages().resource.getString(key);
        }
    }
}
