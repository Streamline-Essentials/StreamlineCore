package gg.drak.tacoessentials.alias;

import gg.drak.tacoessentials.commands.Msg;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The alias editor's "type it in chat" prompts: while a player has one open, their next chat
 * message goes to the prompt instead of to chat. Typing {@code cancel} closes it.
 */
public final class AliasPrompts {

    private static final Map<String, Consumer<String>> PENDING = new ConcurrentHashMap<>();

    private AliasPrompts() {}

    /** @return whether the prompt opened; a player has at most one open at a time */
    public static boolean open(CosmicPlayer player, Consumer<String> onAnswer) {
        if (PENDING.putIfAbsent(player.getUuid(), onAnswer) != null) {
            player.sendMessage(Msg.error("You already have an alias editor prompt open. Type &6cancel &cor answer it first."));
            return false;
        }
        return true;
    }

    /**
     * Hands {@code message} to the player's open prompt, on the server's main thread.
     *
     * @return whether a prompt took the message, in which case it must not reach chat
     */
    public static boolean answer(CosmicPlayer player, String message) {
        Consumer<String> prompt = PENDING.remove(player.getUuid());
        if (prompt == null) return false;
        String text = message.trim();
        Singularity.getInstance().getPlatform().runOnMainThread(() -> {
            if (text.equalsIgnoreCase("cancel")) {
                player.sendMessage(Msg.error("Cancelled."));
                return;
            }
            prompt.accept(text);
        });
        return true;
    }

    public static void drop(String uuid) {
        PENDING.remove(uuid);
    }

    public static void clear() {
        PENDING.clear();
    }
}
