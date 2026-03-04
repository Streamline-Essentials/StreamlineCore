package net.streamline.api.base.commands;

import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.command.result.CommandResult;
import singularity.holders.HoldersHolder;
import singularity.utils.UUIDFetcher;
import singularity.utils.UserUtils;
import singularity.utils.UuidUtils;

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
            ctx.sendMessage("&cUsage: /sldebug <action> (more...)");
            return;
        }

        String action = ctx.getStringArg(0).toLowerCase();
        switch (action) {
            case "geyser":
                if (! ctx.isArgUsable(1)) {
                    ctx.sendMessage("&cUsage: /sldebug geyser <uuid|name|is-enabled> (more...)");
                    return;
                }

                boolean enabled = HoldersHolder.get(HoldersHolder.GEYSER_IDENTIFIER).isEnabled();

                String action2 = ctx.getStringArg(1).toLowerCase();
                switch (action2) {
                    case "is-enabled":
                        if (enabled) {
                            ctx.sendMessage("&bGeyser&7/&bFloodgate &7is found and &aenabled&8!");
                        } else {
                            ctx.sendMessage("&bGeyser&7/&bFloodgate &7is not found&8...");
                            return;
                        }
                        break;
                    case "uuid":
                        if (! enabled) {
                            ctx.sendMessage("&bGeyser&7/&bFloodgate &7is not found&8...");
                            return;
                        }

                        if (! ctx.isArgUsable(2)) {
                            ctx.sendMessage("&cUsage: /sldebug geyser uuid <uuid>");
                            return;
                        }

                        String uuid = ctx.getStringArg(2);
                        boolean isBedrockUUID = HoldersHolder.getGeyserHolder() != null && HoldersHolder.getGeyserHolder().isBedrockUUID(uuid);
                        ctx.sendMessage("&bGeyser&7/&bFloodgate &7thinks that the UUID &b" + uuid + " &7is " + (isBedrockUUID ? "&aa Bedrock UUID" : "&ca Java UUID") + "&7.");
                        break;
                    case "name":
                        if (! enabled) {
                            ctx.sendMessage("&bGeyser&7/&bFloodgate &7is not found&8...");
                            return;
                        }

                        if (! ctx.isArgUsable(2)) {
                            ctx.sendMessage("&cUsage: /sldebug geyser name <name>");
                            return;
                        }
                        String name = ctx.getStringArg(2);
                        boolean isBedrockName = HoldersHolder.getGeyserHolder() != null &&
                                HoldersHolder.getGeyserHolder().isBedrockName(name);
                        ctx.sendMessage("&bGeyser&7/&bFloodgate & 7thinks that the name &b" + name + " &7is " + (isBedrockName ? "&aa Bedrock name" : "&ca Java name") + "&7.");
                        break;
                    default:
                        if (! enabled) {
                            ctx.sendMessage("&bGeyser&7/&bFloodgate &7is not found&8...");
                            return;
                        }

                        ctx.sendMessage("&cUnknown action: " + action2);
                        return;
                }

                break;
            case "uuid":
                if (! ctx.isArgUsable(1)) {
                    ctx.sendMessage("&cUsage: /sldebug uuid <player-name>");
                    return;
                }
                
                String playerName = ctx.getStringArg(1);
                String uuid = UuidUtils.toUuid(playerName);
                if (uuid == null) {
                    ctx.sendMessage("&cCould not find a UUID for player name: " + playerName);
                } else {
                    ctx.sendMessage("&aThe UUID for player name &b" + playerName + " &ais &b" + uuid + "&a.");
                }
                break;
            case "name":
                if (! ctx.isArgUsable(1)) {
                    ctx.sendMessage("&cUsage: /sldebug name <player-uuid>");
                    return;
                }

                String playerUuid = ctx.getStringArg(1);
                String name = UuidUtils.toName(playerUuid);
                if (name == null) {
                    ctx.sendMessage("&cCould not find a player name for UUID: " + playerUuid);
                } else {
                    ctx.sendMessage("&aThe player name for UUID &b" + playerUuid + " &ais &b" + name + "&a.");
                }
                break;
            default:
                ctx.sendMessage("&cUnknown action: " + action);
                return;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> ctx) {
        if (ctx.getArgCount() <= 1) {
            return new ConcurrentSkipListSet<>() {{
                add("geyser");
                add("uuid");
                add("name");
            }};
        }

        if (ctx.getArgCount() == 2) {
            if (ctx.getStringArg(0).equalsIgnoreCase("geyser")) {
                return new ConcurrentSkipListSet<>() {{
                    add("uuid");
                    add("name");
                    add("is-enabled");
                }};
            }
            if (ctx.getStringArg(0).equalsIgnoreCase("uuid")) {
                return UserUtils.getOnlinePlayerNames();
            }
            if (ctx.getStringArg(0).equalsIgnoreCase("name")) {
                return UserUtils.getOnlinePlayerUuids();
            }
        }

        return new ConcurrentSkipListSet<>();
    }
}