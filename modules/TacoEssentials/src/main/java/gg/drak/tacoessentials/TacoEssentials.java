package gg.drak.tacoessentials;

import gg.drak.tacoessentials.commands.AdminCommands;
import gg.drak.tacoessentials.commands.HomeCommands;
import gg.drak.tacoessentials.commands.Perms;
import gg.drak.tacoessentials.commands.TeleportCommands;
import gg.drak.tacoessentials.commands.UtilityCommands;
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
        commands.addAll(AdminCommands.create());
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

    @Override
    public void onDisable() {
        TpaManager.clear();
        Sessions.clear();
    }
}
