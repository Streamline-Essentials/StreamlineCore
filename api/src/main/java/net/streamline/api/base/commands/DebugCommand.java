package net.streamline.api.base.commands;

import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.holders.HoldersHolder;

import java.util.concurrent.ConcurrentSkipListSet;

public class DebugCommand extends CosmicCommand {
    public DebugCommand() {
        super(
                "streamline-base",
                "streamlinedebug",
                "streamline.command.streamlinedebug.default",
                "sldebug"
        );
    }

    @Override
    public void run(CommandContext<CosmicCommand> ctx) {
        if (! ctx.isArgUsable(0)) {
            ctx.getSender().sendMessage("&cUsage: /sldebug <action> (more...)");
            return;
        }

        String action = ctx.getStringArg(0).toLowerCase();
        switch (action) {
            case "geyser":
                if (! ctx.isArgUsable(1)) {
                    ctx.getSender().sendMessage("&cUsage: /sldebug geyser <uuid|name|is-enabled> (more...)");
                    return;
                }

                String action2 = ctx.getStringArg(1).toLowerCase();
                switch (action2) {
                    case "is-enabled":
                        boolean enabled = HoldersHolder.get(HoldersHolder.GEYSER_IDENTIFIER).isEnabled();
                        ctx.getSender().sendMessage("&bGeyser&7/&bFloodgate &7support is " + (enabled ? "&a&lenabled" : "&c&ldisabled") + "&7.");
                        break;
                    case "uuid":
                        if (! ctx.isArgUsable(2)) {
                            ctx.getSender().sendMessage("&cUsage: /sldebug geyser uuid <uuid>");
                            return;
                        }

                        String uuid = ctx.getStringArg(2);
                        boolean isBedrockUUID = HoldersHolder.getGeyserHolder() != null && HoldersHolder.getGeyserHolder().isBedrockUUID(uuid);
                        ctx.getSender().sendMessage("&bGeyser&7/&bFloodgate &7thinks that the UUID &b" + uuid + " &7is " + (isBedrockUUID ? "&aa Bedrock UUID" : "&ca Java UUID") + "&7.");
                        break;
                    case "name":
                        if (! ctx.isArgUsable(2)) {
                            ctx.getSender().sendMessage("&cUsage: /sldebug geyser name <name>");
                            return;
                        }
                        String name = ctx.getStringArg(2);
                        boolean isBedrockName = HoldersHolder.getGeyserHolder() != null &&
                                HoldersHolder.getGeyserHolder().isBedrockName(name);
                        ctx.getSender().sendMessage("&bGeyser&7/&bFloodgate & 7thinks that the name &b" + name + " &7is " + (isBedrockName ? "&aa Bedrock name" : "&ca Java name") + "&7.");
                        break;
                    default:
                        ctx.getSender().sendMessage("&cUnknown action: " + action2);
                        return;
                }

                break;
            default:
                ctx.getSender().sendMessage("&cUnknown action: " + action);
                return;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> ctx) {
        if (ctx.getArgCount() <= 1) {
            return new ConcurrentSkipListSet<>() {{
                add("geyser");
            }};
        }

        if (ctx.getArgCount() == 2 && ctx.getStringArg(0).equalsIgnoreCase("geyser")) {
            return new ConcurrentSkipListSet<>() {{
                add("uuid");
                add("name");
                add("is-enabled");
            }};
        }

        return new ConcurrentSkipListSet<>();
    }
}