package net.streamline.platform.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.streamline.client.compat.LegacyCodes;
import singularity.client.ClientBridge;
import singularity.command.CommandHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * {@code /streamlineclient (menu|hud|<command> (args ...))} and its alias {@code /slclient} on
 * the 1.8.9 client: {@code menu} opens the client module menu and {@code hud} the HUD editor.
 * Other Streamline commands run on a server, which a 1.8.9 client never hosts, so for those
 * the command explains that instead.
 */
public final class ClientCommand extends CommandBase {

    public static final String LABEL = "streamlineclient";
    public static final String ALIAS = "slclient";
    public static final String MENU = "menu";
    public static final String HUD = "hud";

    private static final String USAGE = "&eUsage: &f/%s (menu|hud|<command> (args ...))"
            + "\n&7menu &f- client module settings  &7hud &f- move and scale HUD elements";
    private static final String NO_SERVER = "&cStreamline commands run on a server, and this "
            + "Minecraft 1.8.9 client does not host one. Only &fmenu &cand &fhud &cwork here.";

    @Override
    public String getCommandName() {
        return LABEL;
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList(ALIAS);
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + LABEL + " (menu|hud|<command> (args ...))";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || args[0].isEmpty()) {
            for (String line : String.format(USAGE, LABEL).split("\n")) reply(sender, line);
            return;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        if (name.equals(MENU)) {
            ClientBridge.openMenu();
        } else if (name.equals(HUD)) {
            ClientBridge.openHudEditor();
        } else {
            reply(sender, NO_SERVER);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length != 1) return null;
        List<String> options = new ArrayList<>();
        options.add(MENU);
        options.add(HUD);
        options.addAll(CommandHandler.getAllAliases());
        return getListOfStringsMatchingLastWord(args, options);
    }

    private static void reply(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(LegacyCodes.convert(message)));
    }
}
