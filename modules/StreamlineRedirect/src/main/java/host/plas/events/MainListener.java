package host.plas.events;

import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import gg.drak.thebase.objects.AtomicString;
import host.plas.StreamlineRedirect;
import host.plas.configs.bits.ConfiguredReasonCheck;
import host.plas.managers.PlayerManager;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.events.server.KickedFromServerEvent;
import singularity.utils.UserUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainListener implements BaseEventListener {
    public MainListener() {
        StreamlineRedirect.getInstance().logInfo("MainListener loaded!");
        BaseEventHandler.bake(this, StreamlineRedirect.getInstance());
    }

    @BaseProcessor
    public void onKick(KickedFromServerEvent event) {
        CosmicSender player = event.getSender();
        if (player == null) return;

        String reason = event.getReason();
        String fromServer = event.getFromServer();


        AtomicBoolean deny = new AtomicBoolean(false);
        AtomicBoolean sayMessage = new AtomicBoolean(false);

        for (ConfiguredReasonCheck check : StreamlineRedirect.getMainConfig().getChecks()) {
            if (check.isEnabled()) {
                check.getList().forEach((item) -> {
                    String finalReason = reason;
                    String finalItem = item;
                    if (check.isCaseInsensitive()) {
                        finalReason = reason.toLowerCase();
                        finalItem = item.toLowerCase();
                    }

                    boolean action = check.isAction();

                    boolean found = false;
                    switch (check.getCheckType()) {
                        case EQUALS:
                            if (finalReason.equals(finalItem)) {
                                found = true;
                            }
                            break;
                        case CONTAINS:
                            if (finalReason.contains(finalItem)) {
                                found = true;
                            }
                            break;
                        case STARTS_WITH:
                            if (finalReason.startsWith(finalItem)) {
                                found = true;
                            }
                            break;
                        case ENDS_WITH:
                            if (finalReason.endsWith(finalItem)) {
                                found = true;
                            }
                            break;
                        case REGEX:
                            if (finalReason.matches(finalItem)) {
                                found = true;
                            }
                            break;
                        case NOT_EQUALS:
                            if (! finalReason.equals(finalItem)) {
                                found = true;
                            }
                            break;
                        case NOT_CONTAINS:
                            if (! finalReason.contains(finalItem)) {
                                found = true;
                            }
                            break;
                        case NOT_STARTS_WITH:
                            if (! finalReason.startsWith(finalItem)) {
                                found = true;
                            }
                            break;
                        case NOT_ENDS_WITH:
                            if (! finalReason.endsWith(finalItem)) {
                                found = true;
                            }
                            break;
                    }


                    if (! found) return;
                    deny.set(! action); // Action true means allow, false means deny.
                    if (! sayMessage.get()) {
                        sayMessage.set(check.isSayKickMessage());
                    }
                });
            }
        }

        if (deny.get()) {
            if (sayMessage.get()) {
                player.sendMessage(reason);
            }

            PlayerManager.removePlayer(player.getUuid());
            return;
        }

        if (! fromServer.equalsIgnoreCase("none")) {
            StreamlineRedirect.getMainConfig().getRedirects().forEach(configuredRedirect -> {
                if (configuredRedirect.getFromServers().contains(fromServer)) {
                    if (! configuredRedirect.getToServers().isEmpty()) {
                        int next = PlayerManager.tickPlayerOrGet(player.getUuid(), configuredRedirect.getIdentifier());
                        if (next >= configuredRedirect.getToServers().size()) {
                            next = 0;
                            PlayerManager.setPlayerAt(player.getUuid(), configuredRedirect.getIdentifier(), next);
                        }

                        String toServer = configuredRedirect.getToServers().get(next);
                        event.setToServer(toServer); // TODO: Add a way to check if a server is online.
                    } else {
                        PlayerManager.removePlayer(player.getUuid());
                    }
                }
            });

            return;
        }

        PlayerManager.removePlayer(player.getUuid());
    }
}
