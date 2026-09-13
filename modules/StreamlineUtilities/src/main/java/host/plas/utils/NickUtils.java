package host.plas.utils;

import host.plas.accessors.SpigotAccessor;
import host.plas.events.NicknameUpdateEvent;
import org.jetbrains.annotations.Nullable;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.meta.SenderMeta;
import singularity.modules.ModuleUtils;

import java.util.function.BiConsumer;

public class NickUtils {
    public static void handleNickChange(CosmicSender user, String newName, String oldName, @Nullable BiConsumer<String, String> ifCancelled) {
        NicknameUpdateEvent updateEvent = new NicknameUpdateEvent(user, newName, oldName);
        ModuleUtils.fireEvent(updateEvent);
        if (updateEvent.isCancelled()) {
            if (ifCancelled == null) return;
            ifCancelled.accept(newName, oldName);
            return;
        }

        SenderMeta meta = user.getMeta();
        meta.setNickname(updateEvent.getChangeTo());

        if (user instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) user;
            SpigotAccessor.updateCustomName(player, updateEvent.getChangeTo());
        }
    }
}
