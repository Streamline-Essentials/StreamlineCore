package host.plas.listeners;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseEventPriority;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.database.MyLoader;
import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LoginReceivedEvent;
import singularity.events.server.LogoutEvent;
import singularity.events.server.CosmicChatEvent;
import singularity.messages.events.ProxiedMessageEvent;
import singularity.modules.ModuleUtils;
import host.plas.StreamlineUtilities;
import host.plas.accessors.SpigotAccessor;
import host.plas.essentials.users.UtilitiesUser;

public class MainListener implements BaseEventListener {
    @BaseProcessor
    public void onChat(CosmicChatEvent chatEvent) {
        if (StreamlineUtilities.getConfigs().chatModifyEnabled()) {
            if (! ModuleUtils.hasPermission(chatEvent.getSender(), StreamlineUtilities.getConfigs().chatModifyPermission())) return;

            String message = ModuleUtils.replaceAllPlayerBungee(chatEvent.getSender(), chatEvent.getMessage());
            chatEvent.setMessage(message);
        }
    }

    @BaseProcessor
    public void onPreJoin(LoginReceivedEvent event) {
        if (event.isCancelled()) return;

        CosmicSender player = event.getSender();
        if (player == null) return;

        if (StreamlineUtilities.getMaintenanceConfig().isModeEnabled()) {
            if (! StreamlineUtilities.getMaintenanceConfig().containsAllowed(player.getUuid())) {
                LoginReceivedEvent.ConnectionResult result = new LoginReceivedEvent.ConnectionResult();
                result.setCancelled(true);
                result.setDisconnectMessage(ModuleUtils.replaceAllPlayerBungee(player, StreamlineUtilities.getMaintenanceConfig().getModeKickMessage()));

                event.setCancelled(true);
                event.setResult(result);
            }
        }
    }

    @BaseProcessor
    public void onFullyJoin(LoginCompletedEvent event) {
        if (event.isCancelled()) return;

        CosmicSender player = event.getSender();
        if (player == null) return;
        if (! (player instanceof CosmicPlayer)) return;
        CosmicPlayer streamPlayer = (CosmicPlayer) player;

        if (StreamlineUtilities.getMaintenanceConfig().isModeEnabled()) {
            if (! StreamlineUtilities.getMaintenanceConfig().containsAllowed(player.getUuid())) {
                ModuleUtils.kick(streamPlayer, ModuleUtils.replaceAllPlayerBungee(player, StreamlineUtilities.getMaintenanceConfig().getModeKickMessage()));

                event.setCancelled(true);

                return;
            }
        }

        UtilitiesUser user = MyLoader.getInstance().getOrCreate(event.getSender().getUuid());
        if (user == null) return;
        if (SLAPI.isProxy()) {
            if (StreamlineUtilities.getConfigs().lastServerEnabled()) {
                if (StreamlineUtilities.getConfigs().lastServerPermissionRequired()) {
                    if (ModuleUtils.hasPermission(event.getSender(), StreamlineUtilities.getConfigs().lastServerPermissionValue())) user.goToLastServer();
                } else user.goToLastServer();
            }
        }

        if (StreamlineUtilities.getConfigs().isNicknamesEnabled()) {
            SpigotAccessor.updateCustomName(streamPlayer, event.getSender().getDisplayName());
        }
    }

    @BaseProcessor
    public void onLeave(LogoutEvent event) {
        UtilitiesUser user = MyLoader.getInstance().getOrCreate(event.getSender().getUuid());
        if (user == null) return;
        user.setLastServer(event.getSender().getServerName());
        user.save();
    }

    @BaseProcessor(priority = BaseEventPriority.LOWEST)
    public void onProxiedMessage(ProxiedMessageEvent event) {
//        if (event.getMessage().getSubChannel().equals(SavablePlayerMessageBuilder.getSubChannel())) {
//            SpigotAccessor.updateTabCMI();
//        }
    }
}
