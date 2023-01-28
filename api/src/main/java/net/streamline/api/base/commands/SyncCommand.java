package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.CommandHandler;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import tv.quaint.storage.resources.cache.CachedResource;
import tv.quaint.storage.resources.cache.CachedResourceUtils;
import tv.quaint.storage.resources.databases.singled.DatabaseSingle;
import tv.quaint.utils.MathUtils;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

public class SyncCommand extends StreamlineCommand {
    private final String messageResult;
    private final String messageErrorNot;

    public SyncCommand() {
        super(
                "streamline-base",
                "streamlinesync",
                "streamline.command.streamlinesync.default",
                "slsync"
        );

        this.messageResult = this.getCommandResource().getOrSetDefault("messages.result",
                "&eReloaded Streamline and modules&8!");
        this.messageErrorNot = this.getCommandResource().getOrSetDefault("messages.error.not-syncable",
                "&cSave type is not a database!");
    }

    @Override
    public void run(StreamlineUser sender, String[] args) {
        if (SLAPI.getMainDatabase() == null) {
            ModuleUtils.sendMessage(sender, this.messageErrorNot);
            return;
        }

        UserUtils.syncAllUsers();
        ModuleUtils.sendMessage(sender, this.messageResult);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(StreamlineUser sender, String[] args) {
        return new ConcurrentSkipListSet<>();
    }
}