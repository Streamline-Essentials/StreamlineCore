package gg.drak.tacoessentials.alias;

import gg.drak.tacoessentials.commands.Perms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A command made from other commands: running {@code /<name>} runs each of {@link #getCommands()}
 * in order. Immutable; the {@code with...} methods return changed copies.
 */
public final class CustomAlias {

    private final String name;
    private final boolean requiresPerm;
    private final boolean tabComplete;
    private final List<String> tabCompletes;
    private final List<String> commands;

    public CustomAlias(String name, boolean requiresPerm, boolean tabComplete, List<String> tabCompletes, List<String> commands) {
        this.name = name;
        this.requiresPerm = requiresPerm;
        this.tabComplete = tabComplete;
        this.tabCompletes = tabCompletes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(tabCompletes));
        this.commands = commands == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(commands));
    }

    /** A new alias: usable by everyone, tab completion on, no commands yet. */
    public static CustomAlias create(String name) {
        return new CustomAlias(name, false, true, null, null);
    }

    public String getName() {
        return name;
    }

    /** Whether running the alias needs {@link #getPermission()}. */
    public boolean isRequiresPerm() {
        return requiresPerm;
    }

    public boolean isTabComplete() {
        return tabComplete;
    }

    /**
     * Custom tab-completion lines. Each line lists the candidates per argument, arguments
     * separated by {@code -} and candidates by {@code ,}; see {@link AliasTabs}.
     */
    public List<String> getTabCompletes() {
        return tabCompletes;
    }

    /** The lines the alias runs, in order; see {@link AliasEngine} for what a line may hold. */
    public List<String> getCommands() {
        return commands;
    }

    public String getPermission() {
        return Perms.aliasUse(name);
    }

    public CustomAlias withRequiresPerm(boolean value) {
        return new CustomAlias(name, value, tabComplete, tabCompletes, commands);
    }

    public CustomAlias withTabComplete(boolean value) {
        return new CustomAlias(name, requiresPerm, value, tabCompletes, commands);
    }

    public CustomAlias withTabCompletes(List<String> value) {
        return new CustomAlias(name, requiresPerm, tabComplete, value, commands);
    }

    public CustomAlias withCommands(List<String> value) {
        return new CustomAlias(name, requiresPerm, tabComplete, tabCompletes, value);
    }
}
