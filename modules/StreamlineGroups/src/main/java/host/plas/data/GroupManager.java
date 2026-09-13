package host.plas.data;

import host.plas.StreamlineGroups;
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

    public static void unload(Party party) {
        unload(party.getUuid());
    }

    public static boolean isLoaded(String uuid) {
        return get(uuid).isPresent();
    }

    public static boolean isLoaded(Party party) {
        return isLoaded(party.getUuid());
    }

    public static Optional<AbstractGroup> get(String uuid) {
        return loadedParties.stream().filter(a -> a.getUuid().equals(uuid)).findFirst();
    }

    public static Optional<AbstractGroup> get(CosmicSender player) {
        return loadedParties.stream().filter(a -> {
            if (a.getOwner().getUuid().equals(player.getUuid())) return true;
            return a.hasMember(player);
        }).findFirst();
    }

    public static Optional<Party> getParty(String uuid) {
        Optional<AbstractGroup> group = get(uuid);
        if (group.isEmpty()) return Optional.empty();
        if (! (group.get() instanceof Party)) return Optional.empty();
        return Optional.of((Party) group.get());
    }

    public static Optional<Party> getParty(CosmicSender player) {
        Optional<AbstractGroup> group = get(player);
        if (group.isEmpty()) return Optional.empty();
        if (! (group.get() instanceof Party)) return Optional.empty();
        return Optional.of((Party) group.get());
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
