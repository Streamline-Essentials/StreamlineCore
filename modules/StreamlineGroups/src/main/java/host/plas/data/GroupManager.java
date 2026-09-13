package host.plas.data;

import host.plas.StreamlineGroups;
import host.plas.data.events.CreateGroupEvent;
import host.plas.data.events.GroupChatEvent;
import host.plas.data.flags.GroupFlag;
import host.plas.data.parties.CreatePartyEvent;
import host.plas.data.parties.PartyChatEvent;
import lombok.Getter;
import lombok.Setter;
import singularity.modules.ModuleUtils;
import singularity.data.players.CosmicPlayer;
import singularity.data.console.CosmicSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class GroupManager {
    @Getter @Setter
    private static ConcurrentSkipListSet<AbstractGroup> loadedParties = new ConcurrentSkipListSet<>();

    public static void load(AbstractGroup party) {
        loadedParties.add(party);
    }

    public static void unload(String uuid) {
        loadedParties.removeIf(a -> a.getUuid().equals(uuid));
    }

    /**
     * Unloads exactly this group, matched on its classed identifier so that a
     * {@link Party} and a {@link Guild} sharing a uuid are not both dropped.
     */
    public static void unload(AbstractGroup group) {
        loadedParties.removeIf(a -> a.getClassedIdentifier().equals(group.getClassedIdentifier()));
    }

    public static boolean isLoaded(String uuid) {
        return get(uuid).isPresent();
    }

    /**
     * Returns whether this exact group is loaded, matched on its classed identifier so
     * that a loaded {@link Party} does not mask an unloaded {@link Guild} of the same uuid.
     */
    public static boolean isLoaded(AbstractGroup group) {
        return loadedParties.stream().anyMatch(a -> a.getClassedIdentifier().equals(group.getClassedIdentifier()));
    }

    public static Optional<AbstractGroup> get(String uuid) {
        return loadedParties.stream().filter(a -> a.getUuid().equals(uuid)).findFirst();
    }

    /**
     * Returns the group the given sender owns or belongs to.
     *
     * <p>A group may be loaded before its owner is resolved (see {@code GuildKeeper}), so
     * a null owner is treated as "not this sender's group" rather than an error.</p>
     */
    public static Optional<AbstractGroup> get(CosmicSender player) {
        if (player == null) return Optional.empty();

        return loadedParties.stream().filter(a -> {
            CosmicSender owner = a.getOwner();
            if (owner != null && owner.getUuid().equals(player.getUuid())) return true;
            return a.hasMember(player);
        }).findFirst();
    }

    /**
     * Returns the loaded group with the given uuid when it is of the requested type.
     *
     * @param uuid  the group's uuid
     * @param clazz the group type to narrow to
     * @param <T>   the group type
     * @return the group, or empty if none is loaded or it is a different type
     */
    public static <T extends AbstractGroup> Optional<T> get(String uuid, Class<T> clazz) {
        return loadedParties.stream()
                .filter(clazz::isInstance)
                .filter(a -> a.getUuid().equals(uuid))
                .map(clazz::cast)
                .findFirst();
    }

    /**
     * Returns the loaded group the sender belongs to when it is of the requested type.
     *
     * @param player the sender whose group to find
     * @param clazz  the group type to narrow to
     * @param <T>    the group type
     * @return the group, or empty if none is loaded or it is a different type
     */
    public static <T extends AbstractGroup> Optional<T> get(CosmicSender player, Class<T> clazz) {
        if (player == null) return Optional.empty();

        // Narrow by type before picking, so that a sender who is in both a party and a
        // guild still resolves to the requested one.
        return loadedParties.stream()
                .filter(clazz::isInstance)
                .filter(a -> {
                    CosmicSender owner = a.getOwner();
                    if (owner != null && owner.getUuid().equals(player.getUuid())) return true;
                    return a.hasMember(player);
                })
                .map(clazz::cast)
                .findFirst();
    }

    public static Optional<Party> getParty(String uuid) {
        return get(uuid, Party.class);
    }

    public static Optional<Party> getParty(CosmicSender player) {
        return get(player, Party.class);
    }

    public static Optional<Guild> getGuild(String uuid) {
        return get(uuid, Guild.class);
    }

    public static Optional<Guild> getGuild(CosmicSender player) {
        return get(player, Guild.class);
    }

    public static boolean hasParty(CosmicSender player) {
        return getParty(player).isPresent();
    }

    public static Party createNewParty(CosmicSender owner) {
        return new Party(owner);
    }

    public static Party getOrGetParty(CosmicSender owner) {
        Optional<Party> party = getParty(owner);
        if (party.isPresent()) return party.get();

        Party newParty = createNewParty(owner);

        load(newParty);

        return newParty;
    }

    public static Party createParty(CosmicSender sender, CosmicSender leader) {
        if (hasParty(leader)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseAlreadyExists());
            return getParty(leader).get(); // This should never be null
        }

        Party party = getOrGetParty(leader);

        ModuleUtils.sendMessage(leader, StreamlineGroups.getMessages().partiesCreate());
        if (sender != leader) ModuleUtils.sendMessage(sender, leader, StreamlineGroups.getMessages().partiesCreate());

        ModuleUtils.fireEvent(new CreatePartyEvent(party, leader));

        return party;
    }

    public static void invitePlayerParty(CosmicSender sender, CosmicSender other, CosmicSender toInvite) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        if (get(toInvite).isPresent()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseAlreadyInOther());
            return;
        }

        party.addInvite(sender, toInvite);

        ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().partiesSendInviteSender()
                .replace("%this_other%", other.getCurrentName())
                .replace("%this_target%", toInvite.getCurrentName())
                .replace("%this_sender%", sender.getCurrentName())
                .replace("%this_owner%", party.getOwner().getCurrentName())
        );
        ModuleUtils.sendMessage(toInvite, StreamlineGroups.getMessages().partiesSendInviteOther()
                .replace("%this_other%", other.getCurrentName())
                .replace("%this_target%", toInvite.getCurrentName())
                .replace("%this_sender%", sender.getCurrentName())
                .replace("%this_owner%", party.getOwner().getCurrentName())
        );
        party.getAllUsers().forEach(a -> {
            if (a.equals(sender)) return;
            ModuleUtils.sendMessage(a, StreamlineGroups.getMessages().partiesSendInviteMembers()
                    .replace("%this_other%", other.getCurrentName())
                    .replace("%this_target%", toInvite.getCurrentName())
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_owner%", party.getOwner().getCurrentName())
            );
        });
    }

    public static void acceptInviteParty(CosmicSender sender, CosmicSender other, CosmicSender invited) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        if (getParty(invited).isPresent()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseAlreadyInSelf());
            return;
        }

        if (! party.hasInvite(invited)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInvited());
            return;
        }

        party.remFromInvites(invited);
        party.addMember(invited);

        ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().partiesAcceptSender()
                .replace("%this_other%", other.getCurrentName())
                .replace("%this_target%", invited.getCurrentName())
                .replace("%this_sender%", sender.getCurrentName())
                .replace("%this_owner%", party.getOwner().getCurrentName())
        );
        ModuleUtils.sendMessage(other, StreamlineGroups.getMessages().partiesAcceptOther()
                .replace("%this_other%", other.getCurrentName())
                .replace("%this_target%", invited.getCurrentName())
                .replace("%this_sender%", sender.getCurrentName())
                .replace("%this_owner%", party.getOwner().getCurrentName())
        );
        party.getAllUsers().forEach(a -> {
            if (a.equals(sender)) return;
            ModuleUtils.sendMessage(a, StreamlineGroups.getMessages().partiesAcceptMembers()
                    .replace("%this_other%", other.getCurrentName())
                    .replace("%this_target%", invited.getCurrentName())
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_owner%", party.getOwner().getCurrentName())
            );
        });
    }

    public static void denyInviteParty(CosmicSender sender, CosmicSender other, CosmicSender invited) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        if (getParty(invited).isPresent()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseAlreadyInSelf());
            return;
        }

        if (! party.hasInvite(invited)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInvited());
            return;
        }

        party.remFromInvites(invited);

        ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().partiesDenySender()
                .replace("%this_other%", other.getCurrentName())
                .replace("%this_target%", invited.getCurrentName())
                .replace("%this_sender%", sender.getCurrentName())
                .replace("%this_owner%", party.getOwner().getCurrentName())
        );
        ModuleUtils.sendMessage(invited, StreamlineGroups.getMessages().partiesDenyOther()
                .replace("%this_other%", other.getCurrentName())
                .replace("%this_target%", invited.getCurrentName())
                .replace("%this_sender%", sender.getCurrentName())
                .replace("%this_owner%", party.getOwner().getCurrentName())
        );
        party.getAllUsers().forEach(a -> {
            if (a.equals(sender)) return;
            ModuleUtils.sendMessage(a, StreamlineGroups.getMessages().partiesDenyMembers()
                    .replace("%this_other%", other.getCurrentName())
                    .replace("%this_target%", invited.getCurrentName())
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_owner%", party.getOwner().getCurrentName())
            );
        });
    }

    public static void listParty(CosmicSender sender, CosmicSender other) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        StringBuilder forRoles = new StringBuilder();

        party.getGroupRoleMap().getRolesOrdered().descendingMap().values().forEach(a -> {
            List<String> formattedNames = new ArrayList<>();
            party.getGroupRoleMap().getUsersOf(a).forEach(act -> formattedNames.add(ModuleUtils.getFormatted(act)));
            forRoles.append(StreamlineGroups.getMessages().partiesListRole()
                    .replace("%this_role_identifier%", a.getIdentifier())
                    .replace("%this_role_name%", a.getName())
                    .replace("%this_role_max%", String.valueOf(a.getMax()))
                    .replace("%this_role_priority%", String.valueOf(a.getPriority()))
                    .replace("%this_role_flags%", ModuleUtils.getListAsFormattedString(new ArrayList<>(a.getFlags())))
                    .replace("%this_role_members%", ModuleUtils.getListAsFormattedString(formattedNames))
            );
        });

        ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().partiesListMain()
                .replace("%this_for_roles%", forRoles)
        );
    }

    public static void disbandParty(CosmicSender sender, CosmicSender other) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        for (CosmicSender user : party.getAllUsers()) {
            if (user.equals(sender)) {
                ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesDisbandSender()
                        .replace("%this_sender%", sender.getCurrentName())
                        .replace("%this_owner%", party.getOwner().getCurrentName())
                );
                continue;
            }
            if (user.equals(party.getOwner())) {
                ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesDisbandLeader()
                        .replace("%this_sender%", sender.getCurrentName())
                        .replace("%this_owner%", party.getOwner().getCurrentName())
                );
                continue;
            }
            ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesDisbandMembers()
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_owner%", party.getOwner().getCurrentName())
            );
        }

        party.disband();
    }

    public static void promoteParty(CosmicSender sender, CosmicSender other, CosmicSender promote) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        if (! party.hasMember(promote)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
            return;
        }

        if (promote.equals(sender)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseCannotPromoteSelf());
            return;
        }

        if (party.getRole(promote).hasFlag(GroupFlag.LEADER)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseCannotPromoteLeader());
            return;
        }

        if (party.hasMember(sender)) {
            if (! party.userHasFlag(sender, GroupFlag.PROMOTE)) {
                ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorWithoutFlag(GroupFlag.PROMOTE));
                return;
            }
            if (party.getRole(promote).equals(party.getRole(sender))) {
                ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseCannotPromoteSame());
                return;
            }
        }

        for (CosmicSender user : party.getAllUsers()) {
            if (user.equals(sender)) {
                ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesPromoteSender()
                        .replace("%this_other%", other.getCurrentName())
                        .replace("%this_target%", promote.getCurrentName())
                        .replace("%this_sender%", sender.getCurrentName())
                        .replace("%this_owner%", party.getOwner().getCurrentName())
                );
                continue;
            }
            if (user.equals(promote)) {
                ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesPromoteOther()
                        .replace("%this_other%", other.getCurrentName())
                        .replace("%this_target%", promote.getCurrentName())
                        .replace("%this_sender%", sender.getCurrentName())
                        .replace("%this_owner%", party.getOwner().getCurrentName())
                );
                continue;
            }
            ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesPromoteMembers()
                    .replace("%this_other%", other.getCurrentName())
                    .replace("%this_target%", promote.getCurrentName())
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_owner%", party.getOwner().getCurrentName())
            );
        }

        party.promoteUser(promote);
    }

    public static void demoteParty(CosmicSender sender, CosmicSender other, CosmicSender demote) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        if (! party.hasMember(demote)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
            return;
        }

        if (demote.equals(sender)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseCannotDemoteSelf());
            return;
        }

        if (party.getRole(demote).hasFlag(GroupFlag.LEADER)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseCannotDemoteLeader());
            return;
        }

        if (party.hasMember(sender)) {
            if (! party.userHasFlag(sender, GroupFlag.DEMOTE)) {
                ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorWithoutFlag(GroupFlag.DEMOTE));
                return;
            }
            if (party.getRole(demote).equals(party.getRole(sender))) {
                ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseCannotDemoteSame());
                return;
            }
        }

        for (CosmicSender user : party.getAllUsers()) {
            if (user.equals(sender)) {
                ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesDemoteSender()
                        .replace("%this_other%", other.getCurrentName())
                        .replace("%this_target%", demote.getCurrentName())
                        .replace("%this_sender%", sender.getCurrentName())
                        .replace("%this_owner%", party.getOwner().getCurrentName())
                );
                continue;
            }
            if (user.equals(demote)) {
                ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesDemoteOther()
                        .replace("%this_other%", other.getCurrentName())
                        .replace("%this_target%", demote.getCurrentName())
                        .replace("%this_sender%", sender.getCurrentName())
                        .replace("%this_owner%", party.getOwner().getCurrentName())
                );
                continue;
            }
            ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesDemoteMembers()
                    .replace("%this_other%", other.getCurrentName())
                    .replace("%this_target%", demote.getCurrentName())
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_owner%", party.getOwner().getCurrentName())
            );
        }

        party.demoteUser(demote);
    }

    public static void leaveParty(CosmicSender sender, CosmicSender other) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        if (! party.hasMember(other)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
            return;
        }

        for (CosmicSender user : party.getAllUsers()) {
            if (user.equals(sender)) {
                ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesLeaveSender()
                        .replace("%this_sender%", sender.getCurrentName())
                        .replace("%this_other%", other.getCurrentName())
                        .replace("%this_owner%", party.getOwner().getCurrentName())
                );
                continue;
            }
            if (user.equals(other)) {
                ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesLeaveOther()
                        .replace("%this_sender%", sender.getCurrentName())
                        .replace("%this_other%", other.getCurrentName())
                        .replace("%this_owner%", party.getOwner().getCurrentName())
                );
                continue;
            }
            ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesLeaveMembers()
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_other%", other.getCurrentName())
                    .replace("%this_owner%", party.getOwner().getCurrentName())
            );
        }

        party.removeMember(other);
    }

    public static void chatParty(CosmicSender sender, CosmicSender other, String message) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        for (CosmicSender user : party.getAllUsers()) {
            ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().partiesChat()
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_message%", message)
            );
        }

        ModuleUtils.fireEvent(new PartyChatEvent(party, sender, message));
    }

    public static boolean hasGuild(CosmicSender player) {
        return getGuild(player).isPresent();
    }

    /**
     * Returns the sender's guild, pulling it into memory from storage when the sender has
     * one stored but it is not currently loaded.
     */
    public static Optional<Guild> getOrLoadGuild(CosmicSender player) {
        Optional<Guild> loaded = getGuild(player);
        if (loaded.isPresent()) return loaded;

        if (StreamlineGroups.getGuildLoader() == null) return Optional.empty();

        return StreamlineGroups.getGuildLoader().getLoaded().stream()
                .filter(a -> a.hasMember(player))
                .findFirst();
    }

    public static Guild createGuild(CosmicSender sender, CosmicSender leader) {
        Optional<Guild> existing = getGuild(leader);
        if (existing.isPresent()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseAlreadyExists());
            return existing.get();
        }

        Guild guild = new Guild(leader);
        StreamlineGroups.getGuildLoader().load(guild);
        guild.save();

        ModuleUtils.sendMessage(leader, StreamlineGroups.getMessages().guildsCreate());
        if (sender != leader) ModuleUtils.sendMessage(sender, leader, StreamlineGroups.getMessages().guildsCreate());

        ModuleUtils.fireEvent(new CreateGroupEvent<>(guild, leader));

        return guild;
    }

    public static void invitePlayerGuild(CosmicSender sender, CosmicSender other, CosmicSender toInvite) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        if (getGuild(toInvite).isPresent()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseAlreadyInOther());
            return;
        }

        if (guild.hasMember(sender) && ! guild.userHasFlag(sender, GroupFlag.INVITE)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorWithoutFlag(GroupFlag.INVITE));
            return;
        }

        guild.addInvite(sender, toInvite);

        ModuleUtils.sendMessage(sender, replaceGroup(StreamlineGroups.getMessages().guildsSendInviteSender(), guild, sender, other, toInvite));
        ModuleUtils.sendMessage(toInvite, replaceGroup(StreamlineGroups.getMessages().guildsSendInviteOther(), guild, sender, other, toInvite));
        guild.getAllUsers().forEach(a -> {
            if (a.equals(sender)) return;
            ModuleUtils.sendMessage(a, replaceGroup(StreamlineGroups.getMessages().guildsSendInviteMembers(), guild, sender, other, toInvite));
        });
    }

    public static void acceptInviteGuild(CosmicSender sender, CosmicSender other, CosmicSender invited) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        if (getGuild(invited).isPresent()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseAlreadyInSelf());
            return;
        }

        if (! guild.hasInvite(invited)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInvited());
            return;
        }

        if (guild.getSize() >= guild.getMaxSize()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseFullGroup());
            return;
        }

        guild.addMember(invited);
        guild.save();

        ModuleUtils.sendMessage(sender, replaceGroup(StreamlineGroups.getMessages().guildsAcceptSender(), guild, sender, other, invited));
        ModuleUtils.sendMessage(other, replaceGroup(StreamlineGroups.getMessages().guildsAcceptOther(), guild, sender, other, invited));
        guild.getAllUsers().forEach(a -> {
            if (a.equals(sender)) return;
            ModuleUtils.sendMessage(a, replaceGroup(StreamlineGroups.getMessages().guildsAcceptMembers(), guild, sender, other, invited));
        });
    }

    public static void denyInviteGuild(CosmicSender sender, CosmicSender other, CosmicSender invited) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        if (! guild.hasInvite(invited)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInvited());
            return;
        }

        guild.remFromInvites(invited);

        ModuleUtils.sendMessage(sender, replaceGroup(StreamlineGroups.getMessages().guildsDenySender(), guild, sender, other, invited));
        ModuleUtils.sendMessage(invited, replaceGroup(StreamlineGroups.getMessages().guildsDenyOther(), guild, sender, other, invited));
        guild.getAllUsers().forEach(a -> {
            if (a.equals(sender)) return;
            ModuleUtils.sendMessage(a, replaceGroup(StreamlineGroups.getMessages().guildsDenyMembers(), guild, sender, other, invited));
        });
    }

    public static void listGuild(CosmicSender sender, CosmicSender other) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        StringBuilder forRoles = new StringBuilder();

        guild.getGroupRoleMap().getRolesOrdered().descendingMap().values().forEach(a -> {
            List<String> formattedNames = new ArrayList<>();
            guild.getGroupRoleMap().getUsersOf(a).forEach(act -> formattedNames.add(ModuleUtils.getFormatted(act)));
            forRoles.append(StreamlineGroups.getMessages().guildsListRole()
                    .replace("%this_role_identifier%", a.getIdentifier())
                    .replace("%this_role_name%", a.getName())
                    .replace("%this_role_max%", String.valueOf(a.getMax()))
                    .replace("%this_role_priority%", String.valueOf(a.getPriority()))
                    .replace("%this_role_flags%", ModuleUtils.getListAsFormattedString(new ArrayList<>(a.getFlags())))
                    .replace("%this_role_members%", ModuleUtils.getListAsFormattedString(formattedNames))
            );
        });

        ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().guildsListMain()
                .replace("%this_for_roles%", forRoles)
        );
    }

    /**
     * Dissolves the guild and removes it from storage. Unlike leaving, this is permanent.
     */
    public static void disbandGuild(CosmicSender sender, CosmicSender other) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        if (guild.hasMember(sender) && ! guild.userHasFlag(sender, GroupFlag.DISBAND)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorWithoutFlag(GroupFlag.DISBAND));
            return;
        }

        for (CosmicSender user : guild.getAllUsers()) {
            if (user.equals(sender)) {
                ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsDisbandSender(), guild, sender, other, user));
                continue;
            }
            if (user.equals(guild.getOwner())) {
                ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsDisbandLeader(), guild, sender, other, user));
                continue;
            }
            ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsDisbandMembers(), guild, sender, other, user));
        }

        String uuid = guild.getUuid();

        if (StreamlineGroups.getGuildLoader() != null) StreamlineGroups.getGuildLoader().getLoaded().remove(guild);
        guild.disband();

        if (StreamlineGroups.getGuildKeeper() != null) StreamlineGroups.getGuildKeeper().delete(uuid);
    }

    public static void promoteGuild(CosmicSender sender, CosmicSender other, CosmicSender promote) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        if (! canChangeRank(sender, guild, promote, true)) return;

        for (CosmicSender user : guild.getAllUsers()) {
            if (user.equals(sender)) {
                ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsPromoteSender(), guild, sender, other, promote));
                continue;
            }
            if (user.equals(promote)) {
                ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsPromoteOther(), guild, sender, other, promote));
                continue;
            }
            ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsPromoteMembers(), guild, sender, other, promote));
        }

        guild.promoteUser(promote);
        guild.save();
    }

    public static void demoteGuild(CosmicSender sender, CosmicSender other, CosmicSender demote) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        if (! canChangeRank(sender, guild, demote, false)) return;

        for (CosmicSender user : guild.getAllUsers()) {
            if (user.equals(sender)) {
                ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsDemoteSender(), guild, sender, other, demote));
                continue;
            }
            if (user.equals(demote)) {
                ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsDemoteOther(), guild, sender, other, demote));
                continue;
            }
            ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsDemoteMembers(), guild, sender, other, demote));
        }

        guild.demoteUser(demote);
        guild.save();
    }

    public static void leaveGuild(CosmicSender sender, CosmicSender other) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        if (! guild.hasMember(other)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
            return;
        }

        // The owner leaving would strand the guild with no leader, so it is disbanded.
        if (guild.getOwner() != null && guild.getOwner().getUuid().equals(other.getUuid())) {
            disbandGuild(sender, other);
            return;
        }

        for (CosmicSender user : guild.getAllUsers()) {
            if (user.equals(sender)) {
                ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsLeaveSender(), guild, sender, other, other));
                continue;
            }
            if (user.equals(other)) {
                ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsLeaveOther(), guild, sender, other, other));
                continue;
            }
            ModuleUtils.sendMessage(user, replaceGroup(StreamlineGroups.getMessages().guildsLeaveMembers(), guild, sender, other, other));
        }

        guild.removeMember(other);
        guild.save();
    }

    public static void chatGuild(CosmicSender sender, CosmicSender other, String message) {
        Optional<Guild> optional = getGuild(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Guild guild = optional.get();

        if (guild.isMuted() && guild.hasMember(sender) && ! guild.userHasFlag(sender, GroupFlag.MUTE)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseMutedGroup());
            return;
        }

        for (CosmicSender user : guild.getAllUsers()) {
            ModuleUtils.sendMessage(user, StreamlineGroups.getMessages().guildsChat()
                    .replace("%this_sender%", sender.getCurrentName())
                    .replace("%this_message%", message)
            );
        }

        ModuleUtils.fireEvent(new GroupChatEvent<>(guild, sender, message));
    }

    /**
     * Shared promote/demote guard: the target must be a member, must not be the sender,
     * must not be a leader, and must not hold the sender's own rank.
     *
     * @param promoting {@code true} for a promotion, {@code false} for a demotion
     * @return whether the change may go ahead
     */
    private static boolean canChangeRank(CosmicSender sender, AbstractGroup group, CosmicSender target, boolean promoting) {
        if (! group.hasMember(target)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
            return false;
        }

        if (target.equals(sender)) {
            ModuleUtils.sendMessage(sender, promoting
                    ? StreamlineGroups.getMessages().errorsBaseCannotPromoteSelf()
                    : StreamlineGroups.getMessages().errorsBaseCannotDemoteSelf());
            return false;
        }

        if (group.getRole(target) != null && group.getRole(target).hasFlag(GroupFlag.LEADER)) {
            ModuleUtils.sendMessage(sender, promoting
                    ? StreamlineGroups.getMessages().errorsBaseCannotPromoteLeader()
                    : StreamlineGroups.getMessages().errorsBaseCannotDemoteLeader());
            return false;
        }

        if (group.hasMember(sender)) {
            GroupFlag needed = promoting ? GroupFlag.PROMOTE : GroupFlag.DEMOTE;
            if (! group.userHasFlag(sender, needed)) {
                ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorWithoutFlag(needed));
                return false;
            }
            if (group.getRole(target) != null && group.getRole(target).equals(group.getRole(sender))) {
                ModuleUtils.sendMessage(sender, promoting
                        ? StreamlineGroups.getMessages().errorsBaseCannotPromoteSame()
                        : StreamlineGroups.getMessages().errorsBaseCannotDemoteSame());
                return false;
            }
        }

        return true;
    }

    /**
     * Fills in the {@code %this_*%} tokens every group message shares.
     */
    private static String replaceGroup(String message, AbstractGroup group, CosmicSender sender,
                                       CosmicSender other, CosmicSender target) {
        String ownerName = group.getOwner() == null ? "" : group.getOwner().getCurrentName();

        return message
                .replace("%this_other%", other == null ? "" : other.getCurrentName())
                .replace("%this_target%", target == null ? "" : target.getCurrentName())
                .replace("%this_sender%", sender == null ? "" : sender.getCurrentName())
                .replace("%this_owner%", ownerName);
    }

    public static void warp(CosmicSender sender, CosmicSender other) {
        Optional<Party> optional = getParty(other);
        if (optional.isEmpty()) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotExists());
            return;
        }
        Party party = optional.get();

        if (! party.hasMember(other)) {
            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotInOther());
            return;
        }

        if (party.hasMember(sender)) {
            if (! party.userHasFlag(sender, GroupFlag.WARP)) {
                ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorWithoutFlag(GroupFlag.WARP));
                return;
            }
        }

        if (! (other instanceof CosmicPlayer)) {
//            ModuleUtils.sendMessage(sender, StreamlineGroups.getMessages().errorsBaseNotPlayer());
            return;
        }

        party.getAllUsers().forEach(u -> {
            if (u instanceof CosmicPlayer) {
                CosmicPlayer p = (CosmicPlayer) u;

                ModuleUtils.teleport(p, ((CosmicPlayer) other).getLocation());
            }
        });
    }
}
