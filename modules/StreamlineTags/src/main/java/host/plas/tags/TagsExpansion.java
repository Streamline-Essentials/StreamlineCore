package host.plas.tags;

import host.plas.tags.data.TagStore;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;

/**
 * {@code %streamlinetags_tag%} (the active tag), {@code %streamlinetags_tag_left%} (with a space
 * before it, for after a name), {@code %streamlinetags_tag_right%} (with a space after it, for
 * before a name) and {@code %streamlinetags_tag_id%}. Each is empty when the player has no tag.
 * Values come from the online-player cache, never the database.
 */
public class TagsExpansion extends RATExpansion {
    public TagsExpansion() {
        super(new RATExpansionBuilder("streamlinetags"));
    }

    @Override
    public void init() {
        new IdentifiedUserReplaceable(this, "tag", (s, user) -> TagStore.activeDisplay(user)).register();
        new IdentifiedUserReplaceable(this, "tag_left", (s, user) -> {
            String tag = TagStore.activeDisplay(user);
            return tag.isEmpty() ? "" : " " + tag;
        }).register();
        new IdentifiedUserReplaceable(this, "tag_right", (s, user) -> {
            String tag = TagStore.activeDisplay(user);
            return tag.isEmpty() ? "" : tag + " ";
        }).register();
        new IdentifiedUserReplaceable(this, "tag_id", (s, user) ->
                user == null || user.isConsole() || TagStore.activeDisplay(user).isEmpty()
                        ? "" : TagStore.activeId(user.getUuid()).orElse("")).register();
    }
}
