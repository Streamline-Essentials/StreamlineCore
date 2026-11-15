package net.streamline.api.base.ratapi;

import net.streamline.api.base.module.BaseModule;
import net.streamline.api.permissions.Permissions;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;

/**
 * RAT placeholder expansion that exposes permission-plugin data for every
 * {@link singularity.data.console.CosmicSender}, read through {@link Permissions}.
 *
 * <p>The expansion is registered under the {@code luckperms} namespace and
 * provides the following placeholders (all prefixed with
 * {@code %streamline_luckperms_}):
 * <ul>
 *   <li>{@code prefix} — the permission plugin's prefix, ignoring Streamline's own.</li>
 *   <li>{@code suffix} — the permission plugin's suffix, ignoring Streamline's own.</li>
 *   <li>{@code primary_group} — the player's primary group name.</li>
 *   <li>{@code highest_group} — the first inherited group in the contextual
 *       inheritance chain.</li>
 *   <li>{@code meta_<key>} — a specific meta value from the player's nodes or
 *       their primary group's nodes.</li>
 * </ul>
 * Placeholders that cannot be resolved — for the console, a player the permission
 * plugin has not loaded, or a server without a permission plugin — are left as-is.
 */
public class LuckpermsExpansion extends RATExpansion {

    /**
     * Constructs the expansion and logs its registration to the base module's
     * logger.
     */
    public LuckpermsExpansion() {
        super(new RATExpansionBuilder("luckperms"));
        BaseModule.getInstance().logInfo(getClass().getSimpleName() + " is registered!");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Registers the {@code prefix}, {@code suffix}, {@code primary_group},
     * {@code highest_group}, and regex-based {@code meta_<key>}
     * {@link IdentifiedUserReplaceable} instances.</p>
     */
    @Override
    public void init() {
        new IdentifiedUserReplaceable(this, "prefix", (s, user) -> Permissions.getPrefix(user.getUuid()).getValue()).register();
        new IdentifiedUserReplaceable(this, "suffix", (s, user) -> Permissions.getSuffix(user.getUuid()).getValue()).register();

        new IdentifiedUserReplaceable(this, "primary_group", (s, user) ->
                Permissions.getPrimaryGroup(user.getUuid()).orElse(s.string())).register();

        new IdentifiedUserReplaceable(this, "highest_group", (s, user) ->
                Permissions.getHighestGroup(user.getUuid()).orElse(s.string())).register();

        new IdentifiedUserReplaceable(this, "[m][e][t][a][_]" + "(.*?)", 1, (s, user) ->
                Permissions.getMeta(user.getUuid(), s.get()).orElse(s.string())).register();
    }
}
