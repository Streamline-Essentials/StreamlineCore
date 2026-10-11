package net.streamline.platform;

import net.streamline.api.SLAPI;
import singularity.data.console.CosmicSender;
import singularity.interfaces.IMessenger;
import singularity.objects.CosmicTitle;
import singularity.utils.MessageUtils;

import java.util.regex.Pattern;

/**
 * Messages on a 1.8.9 client. With no server there are no players to message, so whatever
 * Streamline sends goes to the game's log.
 */
public class Messenger implements IMessenger {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}|\\{#[0-9A-F]{6}}|#[0-9A-F]{6}|§[0-9A-FK-ORX]|&[0-9A-FK-ORX]|<[^>]+>");

    private static Messenger instance;

    public Messenger() {
        instance = this;
    }

    public static Messenger getInstance() {
        return instance;
    }

    private static String resolve(CosmicSender context, String message) {
        return SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(context, message) : message;
    }

    private static String resolve(String otherUUID, String message) {
        return SLAPI.isReady() ? MessageUtils.replaceAllPlayerBungee(otherUUID, message) : message;
    }

    private void deliver(String message) {
        BasePlugin.log(stripColor(message));
    }

    @Override
    public void sendMessage(CosmicSender to, String message) {
        if (to == null) return;
        deliver(resolve(to, message));
    }

    @Override
    public void sendMessage(CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        deliver(resolve(otherUUID, message));
    }

    @Override
    public void sendMessage(CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        deliver(resolve(other, message));
    }

    @Override
    public void sendMessageRaw(CosmicSender to, String message) {
        if (to == null) return;
        BasePlugin.log(resolve(to, message));
    }

    @Override
    public void sendMessageRaw(CosmicSender to, String otherUUID, String message) {
        if (to == null) return;
        BasePlugin.log(resolve(otherUUID, message));
    }

    @Override
    public void sendMessageRaw(CosmicSender to, CosmicSender other, String message) {
        if (to == null || other == null) return;
        BasePlugin.log(resolve(other, message));
    }

    /** Titles go to players, and a 1.8.9 client has none. */
    @Override
    public void sendTitle(CosmicSender player, CosmicTitle title) {
    }

    @Override
    public String codedString(String from) {
        return from.replace("&", "§");
    }

    @Override
    public String stripColor(String string) {
        return COLOR_PATTERN.matcher(string).replaceAll("");
    }
}
