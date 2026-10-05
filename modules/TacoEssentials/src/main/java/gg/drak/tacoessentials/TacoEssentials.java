package gg.drak.tacoessentials;

import gg.drak.tacoessentials.alias.AliasEditor;
import gg.drak.tacoessentials.alias.AliasManager;
import gg.drak.tacoessentials.alias.AliasPrompts;
import gg.drak.tacoessentials.commands.AdminCommands;
import gg.drak.tacoessentials.commands.HomeCommands;
import gg.drak.tacoessentials.commands.Perms;
import gg.drak.tacoessentials.commands.PositionCommands;
import gg.drak.tacoessentials.commands.WorkstationCommands;
import gg.drak.tacoessentials.commands.TeleportCommands;
import gg.drak.tacoessentials.commands.UtilityCommands;
import gg.drak.tacoessentials.data.RtpConfig;
import gg.drak.tacoessentials.data.Sessions;
import gg.drak.tacoessentials.data.TacoConfig;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.listeners.TacoListener;
import gg.drak.tacoessentials.teleport.TpaManager;
import lombok.Getter;
import org.pf4j.PluginWrapper;
import singularity.Singularity;
import singularity.command.ModuleCommand;
import singularity.modules.ModuleUtils;
import singularity.modules.SimpleModule;
import singularity.scheduler.ModuleRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Essential commands -- teleport requests, homes, warps, /back, /rtp, utilities and
 * moderation -- on any backend Streamline platform. Everything in-game goes through
 * Streamline's gameplay API, so the module itself never touches a platform's classes; data
 * lives in Streamline's main database.
 *
 * <p>Proxies have no worlds or players to act on, so there the module loads but registers
 * nothing.</p>
 */
public class TacoEssentials extends SimpleModule {

    @Getter
    private static TacoEssentials instance;

    @Getter
    private static TacoConfig config;

    @Getter
    private static RtpConfig rtpConfig;

    public TacoEssentials(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
        if (Singularity.gameplay().isEmpty()) return;

        List<ModuleCommand> commands = new ArrayList<>();
        commands.addAll(TeleportCommands.create());
        commands.addAll(HomeCommands.create());
        commands.addAll(UtilityCommands.create());
        commands.addAll(PositionCommands.create());
        commands.addAll(WorkstationCommands.create());
        commands.addAll(AdminCommands.create());
        commands.add(AliasEditor.create());
        setCommands(commands);
    }

    @Override
    public void onEnable() {
        instance = this;
        if (Singularity.gameplay().isEmpty()) {
            logWarning("This platform has no worlds to act on (a proxy?); TacoEssentials registers no commands here.");
            return;
        }

        config = new TacoConfig();
        rtpConfig = new RtpConfig();
        TacoDatabase.ensureTables();
        Perms.registerDefaults();
        ModuleUtils.listen(new TacoListener(), this);

        new ModuleRunnable(this, 20, 20) {
            @Override
            public void run() {
                TpaManager.expire();
            }
        };
    }

    /**
     * Aliases load after {@code super.start()} has registered this module's own commands, so
     * an alias stored under one of their names is skipped rather than taking its label.
     */
    @Override
    public void start() {
        boolean wasEnabled = isEnabled();
        super.start();
        if (! wasEnabled && Singularity.gameplay().isPresent()) AliasManager.load();
    }

    @Override
    public void onDisable() {
        TpaManager.clear();
        Sessions.clear();
        AliasManager.unloadAll();
        AliasPrompts.clear();
    }
}
