package net.streamline.platform.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
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

/**
 * Adapts a {@link CosmicCommand} to Brigadier: {@link CommandRegistry} registers a literal
 * per label and sends its execution and suggestions here.
 */
@Getter
public class ProperCommand implements IProperCommand {

    /**
     * The mod id. Every label is also registered as {@code streamlinecore:<label>}, so a
     * command stays reachable when another mod registers the same bare name.
     */
    public static final String NAMESPACE = "streamlinecore";

    private final CosmicCommand parent;

    public ProperCommand(CosmicCommand parent) {
        this.parent = parent;
    }

    /**
     * Every label this command answers to: its base and each of its aliases, each both bare
     * and prefixed with {@code streamlinecore:}.
     */
    public List<String> getLabels() {
        List<String> bare = new ArrayList<>();
        bare.add(parent.getBase());
        if (parent.getAliases() != null) {
            for (String alias : parent.getAliases()) {
                if (alias != null && ! alias.isEmpty() && ! bare.contains(alias)) bare.add(alias);
            }
        }

        List<String> labels = new ArrayList<>(bare);
        for (String label : bare) {
            labels.add(NAMESPACE + ":" + label);
        }
        return labels;
    }

    /**
     * Brigadier nodes for this command are built by {@link CommandRegistry}, which routes
     * them here for as long as this command owns their label.
     */
    int execute(CommandContext<CommandSourceStack> ctx, String[] args) {
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
        if (UserManager.getInstance() == null) return null;
        Object src = stack.getEntity() != null ? stack.getEntity() : stack;
        return UserManager.getInstance().getOrCreateSender(src).orElse(null);
    }

    CompletableFuture<Suggestions> getSuggestions(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        CosmicSender sender = resolveCosmicSender(ctx.getSource());
        if (sender == null) return builder.buildFuture();
        try {
            String input = builder.getRemaining();
            String[] args = input.isEmpty() ? new String[]{""} : input.split(" ", -1);
            ConcurrentSkipListSet<String> completions = parent.baseTabComplete(sender, args);
            if (completions != null) {
                // Brigadier replaces only from the builder's start, so suggestions for a later
                // word are offset to where that word begins.
                SuggestionsBuilder wordBuilder = builder.createOffset(builder.getStart() + input.lastIndexOf(' ') + 1);
                MessageUtils.getCompletion(completions, args[args.length - 1]).forEach(wordBuilder::suggest);
                return wordBuilder.buildFuture();
            }
        } catch (Exception ignored) {
        }
        return builder.buildFuture();
    }

    @Override
    public void registerThis() {
        CommandRegistry.register(this);
    }

    @Override
    public void unregisterThis() {
        CommandRegistry.unregister(this);
    }
}
