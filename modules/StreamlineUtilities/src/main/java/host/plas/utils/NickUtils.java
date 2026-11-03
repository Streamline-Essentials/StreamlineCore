package host.plas.utils;

import host.plas.accessors.SpigotAccessor;
import host.plas.events.NicknameUpdateEvent;
import org.jetbrains.annotations.Nullable;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.meta.SenderMeta;
import singularity.modules.ModuleUtils;

public class NickUtils {
    /**
     * Fires a {@link NicknameUpdateEvent} and, unless a listener cancels it, stores the
     * resulting nickname. An empty nickname clears it, so the player shows their own name.
     *
     * @return the nickname that was stored (possibly changed by a listener), or {@code null}
     *         when the change was cancelled
     */
    @Nullable
    public static String handleNickChange(CosmicSender user, String newName, String oldName) {
        NicknameUpdateEvent updateEvent = new NicknameUpdateEvent(user, newName, oldName);
        ModuleUtils.fireEvent(updateEvent);
        if (updateEvent.isCancelled()) return null;

        String changeTo = updateEvent.getChangeTo() == null ? "" : updateEvent.getChangeTo();

        SenderMeta meta = user.getMeta();
        meta.setNickname(changeTo);

        if (user instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) user;
            SpigotAccessor.updateCustomName(player, user.getDisplayName());
        }

        return changeTo;
    }

    /**
     * The name a player currently goes by: their nickname, or their username when none is set.
     */
    public static String currentNickname(CosmicSender user) {
        String nick = user.getMeta().getNickname();
        if (nick == null || nick.isBlank()) return user.getCurrentName();
        return nick;
    }
}
