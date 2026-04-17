package singularity.data.players.meta;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.configs.given.GivenConfigs;
import singularity.data.IUuidable;
import singularity.data.console.CosmicSender;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Holds the display-oriented metadata for a {@link CosmicSender}: nickname,
 * chat prefix, chat suffix, and an arbitrary set of {@link MetaTag} entries.
 *
 * <p>Default values for nickname, prefix, and suffix are read from the main
 * configuration at construction time. Tags are stored in a thread-safe
 * {@link ConcurrentSkipListSet} and can be serialized to/from a delimited
 * string for persistence.</p>
 */
@Getter @Setter
public class SenderMeta implements IUuidable {

    /**
     * The UUID of the sender this metadata belongs to.
     */
    private String uuid;

    /**
     * The sender whose metadata this object represents.
     */
    private CosmicSender sender;

    /**
     * The sender's display nickname.
     */
    private String nickname;

    /**
     * The chat prefix prepended to the sender's display name.
     */
    private String prefix;

    /**
     * The chat suffix appended to the sender's display name.
     */
    private String suffix;

    /**
     * The collection of arbitrary key-value tags attached to this sender.
     */
    private ConcurrentSkipListSet<MetaTag<?>> tags;

    /**
     * Constructs a {@code SenderMeta} for the given sender, initializing
     * nickname, prefix, and suffix from the main configuration defaults.
     *
     * @param sender the sender whose metadata this object represents
     */
    public SenderMeta(CosmicSender sender) {
        this.uuid = sender.getUuid();
        this.sender = sender;
        this.nickname = GivenConfigs.getMainConfig().getDefaultMetaNickname();
        this.prefix = GivenConfigs.getMainConfig().getDefaultMetaPrefix();
        this.suffix = GivenConfigs.getMainConfig().getDefaultMetaSuffix();
        this.tags = new ConcurrentSkipListSet<>();
    }

    /**
     * Returns all tags as a sorted map from identifier to serialized value.
     *
     * @return a {@link ConcurrentSkipListMap} of tag identifier to serialized value
     */
    public ConcurrentSkipListMap<String, String> getTagsAsMap() {
        ConcurrentSkipListMap<String, String> map = new ConcurrentSkipListMap<>();

        for (MetaTag<?> tag : tags) {
            map.put(tag.getIdentifier(), tag.getSerializedValue());
        }

        return map;
    }

    /**
     * Serializes all tags to a single delimited string of the form
     * {@code !!!<identifier>:::<serializedValue>;;;} per tag.
     *
     * @return the serialized tags string
     */
    public String getTagsAsString() {
        StringBuilder builder = new StringBuilder();

        for (MetaTag<?> tag : tags) {
            builder.append("!!!").append(tag.getIdentifier()).append(":::").append(tag.getSerializedValue()).append(";;;");
        }

        return builder.toString();
    }

    /**
     * Parses a serialized tags string (as produced by {@link #getTagsAsString()})
     * and adds the resulting {@link MetaTag} entries to this metadata's tag set.
     *
     * @param string the serialized tags string to parse
     */
    public void setTagsFromString(String string) {
        Matcher matcher = MatcherUtils.matcherBuilder("(!!!)(.*?)(:::)(.*?)(;;;)", string);
        List<String[]> matches = MatcherUtils.getGroups(matcher, 5);

        for (String[] match : matches) {
            String identifier = match[1];
            String serializedValue = match[3];

            MetaTag<?> tag = new MetaTag<>(identifier, serializedValue);
            tags.add(tag);
        }
    }

    /**
     * Builds the sender's fully decorated display name by prepending the prefix
     * and appending the suffix to the nickname.
     *
     * <p>Returns an empty string if the nickname is {@code null}, empty, or blank.</p>
     *
     * @return the fully decorated display name, or an empty string if no nickname is set
     */
    public String getFull() {
        String full = "";
        if (getNickname() != null && ! getNickname().isEmpty() && ! getNickname().isBlank()) {
            full = getNickname();
            if (getPrefix() != null && ! getPrefix().isEmpty() && ! getPrefix().isBlank()) {
                full = getPrefix() + full;
            }
            if (getSuffix() != null && ! getSuffix().isEmpty() && ! getSuffix().isBlank()) {
                full = full + getSuffix();
            }
        }

        return full;
    }

    /**
     * Adds a plain-string tag with the given value as both identifier and serialized value.
     *
     * @param tag the tag identifier and value to add
     */
    public void addTag(String tag) {
        tags.add(new MetaTag<>(tag, tag));
    }

    /**
     * Removes the tag with the given identifier from this metadata's tag set.
     *
     * @param tag the identifier of the tag to remove
     */
    public void removeTag(String tag) {
        tags.removeIf(metaTag -> metaTag.getIdentifier().equals(tag));
    }
}
