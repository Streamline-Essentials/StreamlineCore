package host.plas.placeholders;

import gg.drak.thebase.utils.MatcherUtils;
import host.plas.StreamlineGroups;
import host.plas.data.AbstractGroup;
import host.plas.data.GroupManager;
import host.plas.data.Guild;
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

        new IdentifiedReplaceable(this, "loaded_guilds", (s) ->
                String.valueOf(StreamlineGroups.getGuildLoader() == null
                        ? 0
                        : StreamlineGroups.getGuildLoader().getLoaded().size())).register();

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("party_") + "(.*?)", 1, (s, u) -> {
            Optional<Party> optional = GroupManager.getParty(u);
            if (optional.isEmpty()) return StreamlineGroups.getMessages().placeholdersPartyNotFound();

            String string = startsWithGroup(s.get(), optional.get(), u);
            return string == null ? s.string() : string;
        }).register();

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("guild_") + "(.*?)", 1, (s, u) -> {
            // The guild_default_* placeholders are server-wide config values, not
            // per-guild state, and are registered separately.
            if (s.get().startsWith("default_")) return s.string();

            Optional<Guild> optional = GroupManager.getGuild(u);
            if (optional.isEmpty()) return StreamlineGroups.getMessages().placeholdersGuildNotFound();

            String string = startsWithGroup(s.get(), optional.get(), u);
            return string == null ? s.string() : string;
        }).register();
    }

    public String startsWithParty(String params, Party party, CosmicSender CosmicSender) {
        return startsWithGroup(params, party, CosmicSender);
    }

    public String startsWithGroup(String params, AbstractGroup group, CosmicSender CosmicSender) {
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
        if (params.equals("uuid")) {
            return group.getUuid();
        }
        if (params.equals("muted")) {
            return String.valueOf(group.isMuted());
        }
        if (params.equals("public")) {
            return String.valueOf(group.isPublic());
        }

        // Everything below reads the owner, which is absent on a group loaded before its
        // owner resolves.
        CosmicSender owner = group.getOwner();
        if (owner == null) return null;

        if (params.equals("size_max_absolute")) {
            return String.valueOf(group.getMaxSize(owner));
        }
        if (params.equals("leader_absolute")) {
            return ModuleUtils.getAbsolute(owner);
        }
        if (params.equals("leader_formatted")) {
            return ModuleUtils.getFormatted(owner);
        }
        if (params.equals("leader_absolute_onlined")) {
            return ModuleUtils.getOffOnAbsolute(owner);
        }
        if (params.equals("leader_formatted_onlined")) {
            return ModuleUtils.getOffOnFormatted(owner);
        }
        return null;
    }
}
