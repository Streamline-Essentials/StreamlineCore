package host.plas.placeholders;

import gg.drak.thebase.utils.MatcherUtils;
import host.plas.StreamlineGroups;
import host.plas.data.GroupManager;
import host.plas.data.Party;
import host.plas.data.roles.SavableGroupRole;
import singularity.modules.ModuleUtils;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedReplaceable;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;
import singularity.data.console.CosmicSender;

import java.util.ArrayList;
import java.util.Optional;

public class GroupsExpansion extends RATExpansion {

    public GroupsExpansion() {
        super(new RATExpansionBuilder("groups"));
    }

    @Override
    public void init() {
        new IdentifiedReplaceable(this, "guild_default_level", (s) -> String.valueOf(StreamlineGroups.getConfigs().guildStartingLevel())).register();
        new IdentifiedReplaceable(this, "guild_default_xp", (s) -> String.valueOf(StreamlineGroups.getConfigs().guildStartingExperienceAmount())).register();

        new IdentifiedReplaceable(this, "loaded_parties", (s) -> String.valueOf(GroupManager.getLoadedParties().size())).register();

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("party_") + "(.*?)", 1, (s, u) -> {
            Optional<Party> optional = GroupManager.get(u);
            if (optional.isEmpty()) return s.string();
            Party party = optional.get();

            String string = startsWithParty(s.get(), party, u);
            return string == null ? s.string() : string;
        }).register();
    }

    public String startsWithParty(String params, Party party, CosmicSender CosmicSender) {
        return startsWithGroup(params, party, CosmicSender);
    }

    public String startsWithGroup(String params, Party group, CosmicSender CosmicSender) {
        if (params.startsWith("role_")) {
            SavableGroupRole role = group.getRole(CosmicSender);
            if (role == null) return null;
            if (params.equals("role_identifier")) {
                return String.valueOf(role.getIdentifier());
            }
            if (params.equals("role_name")) {
                return String.valueOf(role.getName());
            }
            if (params.equals("role_max")) {
                return String.valueOf(role.getMax());
            }
            if (params.equals("role_priority")) {
                return String.valueOf(role.getPriority());
            }
            if (params.equals("role_flags")) {
                return ModuleUtils.getListAsFormattedString(new ArrayList<>(role.getFlags()));
            }
        }
        if (params.equals("total_size")) {
            return String.valueOf(group.getAllUsers().size());
        }
        if (params.equals("size_max_current")) {
            return String.valueOf(group.getMaxSize());
        }
        if (params.equals("size_max_absolute")) {
            return String.valueOf(group.getMaxSize(group.getOwner()));
        }
        if (params.equals("leader_absolute")) {
            return ModuleUtils.getAbsolute(group.getOwner());
        }
        if (params.equals("leader_formatted")) {
            return ModuleUtils.getFormatted(group.getOwner());
        }
        if (params.equals("leader_absolute_onlined")) {
            return ModuleUtils.getOffOnAbsolute(group.getOwner());
        }
        if (params.equals("leader_formatted_onlined")) {
            return ModuleUtils.getOffOnFormatted(group.getOwner());
        }
        return null;
    }
}
