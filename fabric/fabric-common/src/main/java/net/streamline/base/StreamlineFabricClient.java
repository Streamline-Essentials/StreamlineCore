package net.streamline.base;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.streamline.platform.commands.ClientCommand;
import net.streamline.platform.listeners.ModEvents;

/**
 * The client side of the Fabric mod: the {@code /streamlineclient} and {@code /slclient}
 * client commands, Streamline's start once the client is up, and its shutdown once the
 * client exits, since stopping an integrated server leaves it up for the next world.
 */
public class StreamlineFabricClient implements ClientModInitializer {

    /**
     * The nodes are built with Brigadier's own builders: Fabric API's helper for them is
     * {@code ClientCommandManager} up to 1.21.11 and {@code ClientCommands} from 26.1.
     */
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String label : ClientCommand.LABELS) {
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal(label)
                        .executes(ctx -> run(label, ctx, ""))
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("args", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> ClientCommand.suggest(ctx.getSource().getPlayer().getUUID(), builder))
                                .executes(ctx -> run(label, ctx, StringArgumentType.getString(ctx, "args")))));
            }
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModEvents.onClientStarted());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModEvents.onGameStopping());
    }

    private static int run(String label, CommandContext<FabricClientCommandSource> ctx, String line) {
        FabricClientCommandSource source = ctx.getSource();
        ClientCommand.run(label, source.getPlayer().getUUID(), line,
                message -> source.getClient().execute(() -> source.sendFeedback(message)));
        return 1;
    }
}
