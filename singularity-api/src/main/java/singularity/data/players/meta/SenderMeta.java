package singularity.data.players.meta;

import lombok.Getter;
import lombok.Setter;
import singularity.configs.given.GivenConfigs;
import singularity.data.IUuidable;
import singularity.data.console.CosmicSender;
import tv.quaint.thebase.lib.re2j.Matcher;
import tv.quaint.utils.MatcherUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class SenderMeta implements IUuidable {
    private String uuid;

    private CosmicSender sender;

    private String nickname;
    private String prefix;
    private String suffix;

    private ConcurrentSkipListSet<MetaTag<?>> tags;

    public SenderMeta(CosmicSender sender) {
        this.uuid = sender.getUuid();
        this.sender = sender;
        this.nickname = GivenConfigs.getMainConfig().getDefaultMetaNickname();
        this.prefix = GivenConfigs.getMainConfig().getDefaultMetaPrefix();
        this.suffix = GivenConfigs.getMainConfig().getDefaultMetaSuffix();
        this.tags = new ConcurrentSkipListSet<>();
    }

    public ConcurrentSkipListMap<String, String> getTagsAsMap() {
        ConcurrentSkipListMap<String, String> map = new ConcurrentSkipListMap<>();

        for (MetaTag<?> tag : tags) {
            map.put(tag.getIdentifier(), tag.getSerializedValue());
        }

        return map;
    }

    public String getTagsAsString() {
        StringBuilder builder = new StringBuilder();

        for (MetaTag<?> tag : tags) {
            builder.append("!!!").append(tag.getIdentifier()).append(":::").append(tag.getSerializedValue()).append(";;;");
        }

        return builder.toString();
    }

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

    public void addTag(String tag) {
        tags.add(new MetaTag<>(tag, tag));
    }

    public void removeTag(String tag) {
        tags.removeIf(metaTag -> metaTag.getIdentifier().equals(tag));
    }
}
