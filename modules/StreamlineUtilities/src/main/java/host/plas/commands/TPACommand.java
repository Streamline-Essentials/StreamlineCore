package host.plas.commands;

import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.CosmicPlayer;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineUtilities;
import host.plas.essentials.EssentialsManager;
import host.plas.essentials.TPARequest;
import host.plas.essentials.configured.ConfiguredBlacklist;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class TPACommand extends UtilitiesCMDBase {
    @Getter
    private final String messageResultSentTo;
    @Getter
    private final String messageResultSentFrom;
    @Getter
    private final String messageResultAcceptedTo;
    @Getter
    private final String messageResultAcceptedFrom;
    @Getter
    private final String messageResultDeniedTo;
    @Getter
    private final String messageResultDeniedFrom;
    @Getter
    private final String messageResultNonePendingSpecific;
    @Getter
    private final String messageResultNonePendingAll;
    @Getter
    private final String messageResultBlacklistedServerTo;
    @Getter
    private final String messageResultBlacklistedWorldTo;

    public TPACommand() {
        super(StreamlineUtilities.getInstance(),
                "ptpa",
                "streamline.command.tpa.default",
                "pteleportask"
        );

        messageResultSentTo = getCommandResource().getOrSetDefault("messages.result.sent.to", "&d%streamline_parse_%this_from%:::*/*streamline_user_formatted*/*% &ewants to tpa to you&8!\n" +
                "&eType &7'&a/ptpa accept %this_from%&7' &eto accept&8!");
        messageResultSentFrom = getCommandResource().getOrSetDefault("messages.result.sent.from", "&eSent &d%streamline_parse_%this_to%:::*/*streamline_user_formatted*/*% &ea tpa request&8!");

        messageResultAcceptedTo = getCommandResource().getOrSetDefault("messages.result.accepted.to",
                "&eAccepted &d%streamline_parse_%this_from%:::*/*streamline_user_formatted*/*%&e's tpa request&8!");
        messageResultAcceptedFrom = getCommandResource().getOrSetDefault("messages.result.accepted.from",
                "&d%streamline_parse_%this_to%:::*/*streamline_user_formatted*/*% &ehas accepted your tpa request&8!");

        messageResultDeniedTo = getCommandResource().getOrSetDefault("messages.result.denied.to",
                "&eDenied &d%streamline_parse_%this_from%:::*/*streamline_user_formatted*/*%&e's tpa request&8!");
        messageResultDeniedFrom = getCommandResource().getOrSetDefault("messages.result.denied.from",
                "&d%streamline_parse_%this_to%:::*/*streamline_user_formatted*/*% &ehas denied your tpa request&8!");

        messageResultNonePendingSpecific = getCommandResource().getOrSetDefault("messages.result.none_pending.specific",
                "&eYou have no pending tpa requests from &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&8!");
        messageResultNonePendingAll = getCommandResource().getOrSetDefault("messages.result.none_pending.all",
                "&eYou have no pending tpa requests&8!");

        messageResultBlacklistedServerTo = getCommandResource().getOrSetDefault("messages.result.blocked.server.other",
                "&eYou cannot tpa to &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ebecause they are on a disallowed server&8!");

        messageResultBlacklistedWorldTo = getCommandResource().getOrSetDefault("messages.result.blocked.world.other",
                "&eYou cannot tpa to &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ebecause they are in a disallowed world&8!");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        if (strings.length < 1) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        if (! (sender instanceof CosmicPlayer)) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
            return;
        }
        CosmicPlayer senderPlayer = (CosmicPlayer) sender;

        String action = strings[0];

        switch (action) {
            case "request":
                if (strings.length < 2) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }
                String username = strings[1];
                CosmicSender other = ModuleUtils.getOrGetUserByName(username).orElse(null);
                if (other == null) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! (other instanceof CosmicPlayer)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
                    return;
                }
                CosmicPlayer otherPlayer = (CosmicPlayer) other;

                if (otherPlayer.getLocation() == null) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
                    return;
                } else {
//                    if (otherPlayer.getLocation().isNull()) {
//                        ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
//                        return;
//                    }
                }

                if (senderPlayer.getLocation() == null) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
                    return;
                } else {
//                    if (senderPlayer.getLocation().isNull()) {
//                        ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
//                        return;
//                    }
                }

                CosmicLocation senderLocation = senderPlayer.getLocation();
                CosmicLocation otherLocation = otherPlayer.getLocation();

                ConfiguredBlacklist configuredBlacklist = StreamlineUtilities.getConfigs().getTPABlacklist();

                if (configuredBlacklist != null) {
                    AtomicBoolean isServerBlacklisted = new AtomicBoolean(false);
                    configuredBlacklist.getServers().forEach(server -> {
                        if (configuredBlacklist.isAsWhitelist()) {
                            if (! server.equals(otherPlayer.getServerName())) {
                                isServerBlacklisted.set(true);
                            }
                        } else {
                            if (server.equals(otherPlayer.getServerName())) {
                                isServerBlacklisted.set(true);
                            }
                        }
                    });
                    AtomicBoolean isWorldBlacklisted = new AtomicBoolean(false);
                    configuredBlacklist.getWorlds().forEach(world -> {
                        if (configuredBlacklist.isAsWhitelist()) {
                            if (! world.equals(otherLocation.getWorld())) {
                                isWorldBlacklisted.set(true);
                            }
                        } else {
                            if (world.equals(otherLocation.getWorld())) {
                                isWorldBlacklisted.set(true);
                            }
                        }
                    });

                    if (isServerBlacklisted.get()) {
                        ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultBlacklistedServerTo, otherPlayer));
                        return;
                    }

                    if (isWorldBlacklisted.get()) {
                        ModuleUtils.sendMessage(sender, getWithOther(sender, messageResultBlacklistedWorldTo, otherPlayer));
                        return;
                    }
                }

                EssentialsManager.requestTPA(senderPlayer, otherPlayer);

                ModuleUtils.sendMessage(sender,
                        getWithOther(SenderWithOther.FROM_IS_SENDER, sender, other, messageResultSentFrom));
                ModuleUtils.sendMessage(other,
                        getWithOther(SenderWithOther.FROM_IS_SENDER, sender, other, messageResultSentTo));
                break;
            case "accept":
                String usernameAccept = "";
                if (strings.length > 1) {
                    usernameAccept = strings[1];
                } else {
                    TPARequest request = EssentialsManager.getLatestPendingTPARequest(senderPlayer, TPARequest.TransportType.SENDER_TO_RECEIVER);
                    if (request == null) {
                        ModuleUtils.sendMessage(sender, getWithOther(sender.getUuid(), messageResultNonePendingAll));
                        return;
                    }
                    usernameAccept = request.getSender().getCurrentName();
                }

                CosmicSender otherAccept = ModuleUtils.getOrGetUserByName(usernameAccept).orElse(null);
                if (otherAccept == null) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! (otherAccept instanceof CosmicPlayer)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
                    return;
                }
                CosmicPlayer otherPlayerAccept = (CosmicPlayer) otherAccept;

                TPARequest request = EssentialsManager.getTPARequest(otherPlayerAccept.getUuid(), senderPlayer.getUuid(), TPARequest.TransportType.SENDER_TO_RECEIVER);
                if (request == null) {
                    ModuleUtils.sendMessage(sender,
                            getWithOther(SenderWithOther.TO_IS_SENDER, sender, otherAccept,
                                    getWithOther(sender, messageResultNonePendingSpecific, otherAccept)));
                    return;
                }

                request.perform();

                ModuleUtils.sendMessage(sender, getWithOther(SenderWithOther.TO_IS_SENDER,
                        otherAccept, sender, messageResultAcceptedTo));
                ModuleUtils.sendMessage(otherAccept, getWithOther(SenderWithOther.TO_IS_SENDER,
                        otherAccept, sender, messageResultAcceptedFrom));
                break;
            case "deny":
                String usernameDeny = "";
                if (strings.length > 1) {
                    usernameDeny = strings[1];
                } else {
                    TPARequest requestDeny = EssentialsManager.getLatestPendingTPARequest(senderPlayer, TPARequest.TransportType.SENDER_TO_RECEIVER);
                    if (requestDeny == null) {
                        ModuleUtils.sendMessage(sender, getWithOther(sender.getUuid(), messageResultNonePendingAll));
                        return;
                    }
                    usernameDeny = requestDeny.getSender().getCurrentName();
                }

                CosmicSender otherDeny = ModuleUtils.getOrGetUserByName(usernameDeny).orElse(null);
                if (otherDeny == null) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                if (! (otherDeny instanceof CosmicPlayer)) {
                    ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_OTHER.get());
                    return;
                }
                CosmicPlayer otherPlayerDeny = (CosmicPlayer) otherDeny;

                TPARequest requestDeny = EssentialsManager.getTPARequest(otherPlayerDeny.getUuid(), senderPlayer.getUuid(), TPARequest.TransportType.SENDER_TO_RECEIVER);
                if (requestDeny == null) {
                    ModuleUtils.sendMessage(sender,
                            getWithOther(SenderWithOther.TO_IS_SENDER, otherDeny, sender,
                                    getWithOther(sender, messageResultNonePendingSpecific, otherDeny)));
                    return;
                }

                requestDeny.deny();

                ModuleUtils.sendMessage(sender, getWithOther(SenderWithOther.TO_IS_SENDER,
                        otherDeny, sender, messageResultDeniedTo));
                ModuleUtils.sendMessage(otherDeny, getWithOther(SenderWithOther.TO_IS_SENDER,
                        otherDeny, sender, messageResultDeniedFrom));
                break;
            default:
                ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length <= 1) return new ConcurrentSkipListSet<>(Arrays.asList("request", "accept", "deny"));
        if (strings.length == 2) return ModuleUtils.getOnlinePlayerNames();

        return new ConcurrentSkipListSet<>();
    }
}
