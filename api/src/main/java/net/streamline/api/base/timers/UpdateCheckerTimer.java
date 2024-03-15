package net.streamline.api.base.timers;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.upkeep.UpkeepData;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UpdateCheckerTimer extends BaseRunnable {
    boolean defer;

    public UpdateCheckerTimer() {
        super(0, 20 * 10);

        defer = false;
    }

    @Override
    public void run() {
        if (defer) return;

        CompletableFuture.runAsync(() -> {
            defer = true;
            UserUtils.getLoadedPlayers().forEach((uuid, player) -> {
                Optional<UpkeepData> optional = SLAPI.getMainDatabase().pullUpkeep(player.getUuid()).join();
                if (optional.isEmpty()) return;
                UpkeepData upkeepData = optional.get();

                if (! upkeepData.getServerUuid().equals(getServerUuid())) {
                    player.reload();
                }
            });

            defer = false;
        });
    }

    public static String getServerUuid() {
        return SLAPI.getServerUuid();
    }
}
