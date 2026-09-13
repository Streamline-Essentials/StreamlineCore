package host.plas.ratapi;

import gg.drak.thebase.utils.MatcherUtils;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedReplaceable;
import host.plas.StreamlineUtilities;
import host.plas.configs.obj.PermissionGroup;

public class UtilitiesExpansion extends RATExpansion {
    public UtilitiesExpansion() {
        super(new RATExpansionBuilder("utils"));
    }

    @Override
    public void init() {
        new IdentifiedReplaceable(this, MatcherUtils.makeLiteral("group_") + "(.*?)", 1, (s) -> {
            String string = startsWithGroup(s.get());
            return string == null ? s.string() : string;
        }).register();

        new IdentifiedReplaceable(this, "maintenance_mode", (s) -> StreamlineUtilities.getMaintenanceConfig().isModeEnabled()
                ? MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_TRUE.get() : MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_FALSE.get()).register();
        new IdentifiedReplaceable(this, "maintenance_message", (s) -> StreamlineUtilities.getMaintenanceConfig().getModeKickMessage()).register();

        new IdentifiedReplaceable(this, "whitelist_mode", (s) -> GivenConfigs.getWhitelistConfig().isEnabled()
                ? MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_TRUE.get() : MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_FALSE.get()).register();
        new IdentifiedReplaceable(this, "whitelist_message", (s) -> MainMessagesHandler.MESSAGES.INVALID.WHITELIST_NOT.get()).register();

        new IdentifiedReplaceable(this, MatcherUtils.makeLiteral("server_alias_") + "(.*?)", 1, (s) -> {
            String string = StreamlineUtilities.getServersConfig().getActualName(s.get());
            return string == null ? s.string() : string;
        }).register();

        new IdentifiedReplaceable(this, MatcherUtils.makeLiteral("server_pretty_name_") + "(.*?)", 1, (s) -> {
            String string = StreamlineUtilities.getServersConfig().getPrettyName(s.get());
            return string == null ? s.string() : string;
        }).register();
    }

    public String startsWithGroup(String s) {
        String identifier = s;
        if (identifier.contains("_")) identifier = identifier.substring(0, identifier.indexOf("_"));
        PermissionGroup group = StreamlineUtilities.getGroupedPermissionConfig().getPermissionGroups().get(identifier);
        if (group == null) return null;
        if (s.equals(identifier + "_name")) {
            return group.getName();
        }
        if (s.equals(identifier + "_permission")) {
            return group.getPermission();
        }
        return null;
    }
}
