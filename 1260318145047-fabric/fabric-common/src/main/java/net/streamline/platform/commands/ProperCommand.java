package net.streamline.platform.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.streamline.platform.savables.UserManager;
import singularity.command.CosmicCommand;
import singularity.command.result.CommandResult;
import singularity.data.console.CosmicSender;
import singularity.interfaces.IProperCommand;
import singularity.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class ProperCommand implements IProperCommand {

    private final CosmicCommand parent;

    public ProperCommand(CosmicCommand parent) {
        this.parent = parent;
    }

    public LiteralArgumentBuilder<CommandSourceStack> buildBrigadier() {
        LiteralArgumentBuilder<CommandSourceStack> base = Commands.literal(parent.getBase())
                .executes(ctx -> execute(ctx, new String[0]));

        RequiredArgumentBuilder<CommandSourceStack, String> argsNode =
                Commands.argument("args", StringArgumentType.greedyString())
                        .suggests(this::getSuggestions)
                        .executes(ctx -> {
                            String raw = StringArgumentType.getString(ctx, "args");
                            return execute(ctx, raw.split(" "));
                        });

        base.then(argsNode);
        return base;
    }

    private int execute(CommandContext<CommandSourceStack> ctx, String[] args) {
        CosmicSender sender = resolveCosmicSender(ctx.getSource());
        if (sender == null) return 0;
        try {
            CommandResult<?> result = parent.baseRun(sender, args);
            if (result == null) return 0;
            if (result == CosmicCommand.error() || result == CosmicCommand.failure()) return 0;
            return 1;
        } catch (Exception e) {
            MessageUtils.logWarning("Error executing command '" + parent.getBase() + "': " + e.getMessage());
            return 0;
        }
    }

    private static CosmicSender resolveCosmicSender(CommandSourceStack stack) {
        // Pass the entity if it's a player, otherwise pass the stack itself (console)
        Object src = stack.getEntity() != null ? stack.getEntity() : stack;
        return UserManager.getInstance().getOrCreateSender(src).orElse(null);
    }

    private CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        CosmicSender sender = resolveCosmicSender(ctx.getSource());
        if (sender == null) return builder.buildFuture();
        try {
            String input = builder.getRemaining();
            String[] args = input.isEmpty() ? new String[]{""} : input.split(" ", -1);
            ConcurrentSkipListSet<String> completions = parent.baseTabComplete(sender, args);
            if (completions != null) {
                List<String> filtered = new ArrayList<>(MessageUtils.getCompletion(completions, args[args.length - 1]));
                filtered.forEach(builder::suggest);
            }
        } catch (Exception ignored) {
        }
        return builder.buildFuture();
    }

    @Override
    public void registerThis() {
        // Handled via PlatformListener.registerCommand()
    }

    @Override
    public void unregisterThis() {
        // Brigadier commands cannot be unregistered at runtime
    }
}
