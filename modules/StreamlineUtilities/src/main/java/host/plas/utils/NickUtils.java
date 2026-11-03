package host.plas.utils;

import host.plas.accessors.SpigotAccessor;
import host.plas.events.NicknameUpdateEvent;
import org.jetbrains.annotations.Nullable;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.meta.SenderMeta;
import singularity.modules.ModuleUtils;

public class NickUtils {
    /**
     * A prefix / suffix that shows nothing. The core uses a player's own (Streamline) prefix
     * and suffix whenever they are non-empty and falls back to the permission plugin's
     * otherwise, so a lone reset code hides the LuckPerms prefix and suffix.
     */
    public static final String PLAIN = "&r";

    /**
     * Fires a {@link NicknameUpdateEvent} and, unless a listener cancels it, stores the
     * resulting nickname. An empty nickname removes it, so the player shows their own name.
     *
     * @param plain {@code true} to show the nickname without any prefix or suffix;
     *              {@code false} to show the usual prefix and suffix around it
     * @return the nickname that was stored (possibly changed by a listener), or {@code null}
     *         when the change was cancelled
     */
    @Nullable
    public static String handleNickChange(CosmicSender user, String newName, String oldName, boolean plain) {
        NicknameUpdateEvent updateEvent = new NicknameUpdateEvent(user, newName, oldName);
        ModuleUtils.fireEvent(updateEvent);
        if (updateEvent.isCancelled()) return null;

        String changeTo = updateEvent.getChangeTo() == null ? "" : updateEvent.getChangeTo();

        SenderMeta meta = user.getMeta();
        meta.setNickname(changeTo);
        meta.setPrefix(plain ? PLAIN : GivenConfigs.getMainConfig().getDefaultMetaPrefix());
        meta.setSuffix(plain ? PLAIN : GivenConfigs.getMainConfig().getDefaultMetaSuffix());

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
