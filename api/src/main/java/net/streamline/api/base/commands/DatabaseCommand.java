package net.streamline.api.base.commands;

import gg.drak.thebase.async.AsyncUtils;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.database.CoreDBOperator;
import singularity.database.DatabaseMigrator;
import singularity.database.DatabaseType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Manages Streamline's database.
 *
 * <p>{@code /streamlinedb migrate <from> <to> (confirm)} copies every Streamline table
 * (core and modules) from one database type to the other, both as set up in
 * {@code database-config.yml}, and switches the server to the target. Without
 * {@code confirm} it refuses to replace target tables that already hold rows.
 * {@code /streamlinedb info} shows the database in use. Registered under the alias
 * {@code sldb}.</p>
 */
public class DatabaseCommand extends CosmicCommand {

    private final String messageUsage;
    private final String messageInfo;
    private final String messageUnknownType;
    private final String messageSameType;
    private final String messageStarted;
    private final String messageProgress;
    private final String messageOccupied;
    private final String messageDone;
    private final String messageFailed;

    public DatabaseCommand() {
        super(
                "streamline-base",
                "streamlinedb",
                "streamline.command.streamlinedb.default",
                "sldb"
        );

        this.messageUsage = this.getCommandResource().getOrSetDefault("messages.usage",
                "&eUsage&8: &7/streamlinedb (info|migrate <from> <to> (confirm))&8. &7Types&8: &f%types%");
        this.messageInfo = this.getCommandResource().getOrSetDefault("messages.info",
                "&eStreamline is using &f%type%&8.");
        this.messageUnknownType = this.getCommandResource().getOrSetDefault("messages.error.unknown-type",
                "&cUnknown database type &f%type%&c. Types&8: &f%types%");
        this.messageSameType = this.getCommandResource().getOrSetDefault("messages.error.same-type",
                "&cThe source and target must be different types.");
        this.messageStarted = this.getCommandResource().getOrSetDefault("messages.migrate.started",
                "&eMigrating &f%from% &e-> &f%to%&8...");
        this.messageProgress = this.getCommandResource().getOrSetDefault("messages.migrate.progress",
                "&7%line%");
        this.messageOccupied = this.getCommandResource().getOrSetDefault("messages.migrate.occupied",
                "&cThe &f%to% &cdatabase already has data in&8: &f%tables%&c. Run &f/streamlinedb migrate %from% %to% confirm &cto replace it.");
        this.messageDone = this.getCommandResource().getOrSetDefault("messages.migrate.done",
                "&aMigrated &f%tables% &atables (&f%rows% &arows) from &f%from% &ato &f%to%&a. Streamline now uses &f%to%&a.");
        this.messageFailed = this.getCommandResource().getOrSetDefault("messages.migrate.failed",
                "&cMigration failed, still using &f%current%&8: &f%reason%");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 1) {
            context.sendMessage(usage());
            return;
        }

        String action = context.getStringArg(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "info": {
                CoreDBOperator main = GivenConfigs.getMainDatabase();
                context.sendMessage(messageInfo.replace("%type%", main == null ? "none" : main.getType().name()));
                return;
            }
            case "migrate":
                migrate(context);
                return;
            default:
                context.sendMessage(usage());
        }
    }

    private void migrate(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 3) {
            context.sendMessage(usage());
            return;
        }

        DatabaseType from = parse(context, context.getStringArg(1));
        if (from == null) return;
        DatabaseType to = parse(context, context.getStringArg(2));
        if (to == null) return;
        if (from == to) {
            context.sendMessage(messageSameType);
            return;
        }
        boolean confirm = context.getArgCount() >= 4 && context.getStringArg(3).equalsIgnoreCase("confirm");

        CosmicSender sender = context.getSender();
        context.sendMessage(replaceTypes(messageStarted, from, to));

        AsyncUtils.executeAsync(() -> {
            DatabaseMigrator.Result result = DatabaseMigrator.migrate(from, to, confirm,
                    line -> sender.sendMessage(messageProgress.replace("%line%", line)));

            if (result.isSuccess()) {
                sender.sendMessage(replaceTypes(messageDone, from, to)
                        .replace("%tables%", String.valueOf(result.getTables()))
                        .replace("%rows%", String.valueOf(result.getRows())));
            } else if (! result.getOccupied().isEmpty()) {
                sender.sendMessage(replaceTypes(messageOccupied, from, to)
                        .replace("%tables%", String.join(", ", result.getOccupied())));
            } else {
                CoreDBOperator main = GivenConfigs.getMainDatabase();
                sender.sendMessage(messageFailed
                        .replace("%current%", main == null ? "none" : main.getType().name())
                        .replace("%reason%", String.valueOf(result.getMessage())));
            }
        });
    }

    private DatabaseType parse(CommandContext<CosmicCommand> context, String name) {
        try {
            return DatabaseType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            context.sendMessage(messageUnknownType.replace("%type%", name).replace("%types%", types()));
            return null;
        }
    }

    private String replaceTypes(String message, DatabaseType from, DatabaseType to) {
        return message.replace("%from%", from.name()).replace("%to%", to.name());
    }

    private String usage() {
        return messageUsage.replace("%types%", types());
    }

    private static String types() {
        return Arrays.stream(DatabaseType.values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() <= 1) return new ConcurrentSkipListSet<>(List.of("info", "migrate"));
        if (! context.getStringArg(0).equalsIgnoreCase("migrate")) return new ConcurrentSkipListSet<>();

        if (context.getArgCount() == 2) {
            return Arrays.stream(DatabaseType.values()).map(Enum::name)
                    .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
        }
        if (context.getArgCount() == 3) {
            String from = context.getStringArg(1);
            return Arrays.stream(DatabaseType.values()).map(Enum::name)
                    .filter(name -> ! name.equalsIgnoreCase(from))
                    .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
        }
        if (context.getArgCount() == 4) return new ConcurrentSkipListSet<>(List.of("confirm"));
        return new ConcurrentSkipListSet<>();
    }
}
