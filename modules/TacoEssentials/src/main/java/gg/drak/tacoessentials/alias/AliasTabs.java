package gg.drak.tacoessentials.alias;

import gg.drak.tacoessentials.commands.TacoCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * Tab completion for an alias, from its custom tab lines. A line lists the candidates for each
 * argument, arguments separated by {@code -} and candidates by {@code ,}: {@code a,b-[playername]}
 * offers {@code a} or {@code b} first, then an online player's name. Besides literal words a
 * candidate may be {@code [playername]}, {@code [worlds]} or {@code [customalias]}.
 */
public final class AliasTabs {

    private AliasTabs() {}

    /** Candidates for the zero-based argument {@code arg}; filtering by what was typed happens afterwards. */
    public static List<String> complete(CustomAlias alias, int arg) {
        if (alias == null || ! alias.isTabComplete()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String line : alias.getTabCompletes()) {
            String[] perArg = line.split("-");
            if (arg >= perArg.length) continue;
            for (String token : perArg[arg].split(",")) {
                for (String value : resolve(token.trim())) if (! out.contains(value)) out.add(value);
            }
        }
        return out;
    }

    private static List<String> resolve(String token) {
        switch (token.toLowerCase(Locale.ROOT)) {
            case "":
                return Collections.emptyList();
            case "[playername]":
            case "[allplayername]":
                return TacoCommand.onlineNames();
            case "[worlds]":
                return gameplay().getWorldNames();
            case "[customalias]": {
                List<String> names = new ArrayList<>();
                AliasManager.all().forEach(a -> names.add(a.getName()));
                return names;
            }
            default:
                return Collections.singletonList(token);
        }
    }
}
