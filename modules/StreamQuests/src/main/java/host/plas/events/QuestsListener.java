package host.plas.events;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseEventPriority;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.StreamQuests;
import host.plas.data.Quest;
import host.plas.data.QuestManager;
import host.plas.data.players.QuestPlayer;
import host.plas.data.require.RequirementType;
import host.plas.events.own.QuestCompletedEvent;
import singularity.data.players.CosmicPlayer;
import singularity.events.player.CosmicCommandPreprocessEvent;
import singularity.events.player.CosmicDeathEvent;
import singularity.events.player.gameplay.PlayerBrokeBlockEvent;
import singularity.events.player.gameplay.PlayerKilledEntityEvent;
import singularity.events.player.gameplay.PlayerPlacedBlockEvent;
import singularity.events.server.CosmicChatEvent;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.modules.ModuleUtils;

public class QuestsListener implements BaseEventListener {
    public QuestsListener() {
        ModuleUtils.listen(this, StreamQuests.getInstance());
    }

    @BaseProcessor
    public void onPlayerJoin(LoginCompletedEvent event) {
        CosmicPlayer player = event.getPlayer();

        StreamQuests.getLoader().getOrCreate(player.getUuid());
    }

    @BaseProcessor
    public void onPlayerLeave(LogoutEvent event) {
        CosmicPlayer player = event.getPlayer();

        QuestPlayer p = StreamQuests.getLoader().getOrCreate(player.getUuid());

        p.save();

        StreamQuests.getLoader().unload(player.getUuid());
    }

    @BaseProcessor
    public void onPlayerChat(CosmicChatEvent event) {
        CosmicPlayer player = event.getPlayer();

        QuestPlayer p = StreamQuests.getLoader().getOrCreate(player.getUuid());

        QuestManager.fireQuestEvent(player.getUuid(), RequirementType.CHAT, event.getMessage(), 1);
    }

    // Commands never pass through chat; this fires for each one a player runs, on every backend.
    @BaseProcessor(priority = BaseEventPriority.MONITOR)
    public void onPlayerCommand(CosmicCommandPreprocessEvent event) {
        if (event.isCancelled()) return;

        String command = event.getCommandLine();
        if (! command.startsWith("/")) command = "/" + command;

        QuestManager.fireQuestEvent(event.getPlayer().getUuid(), RequirementType.RUN_COMMAND, command, 1);
    }

    // Gameplay events fire on Spigot and the mod loaders alike, each for an action that already
    // went through; their ids are namespaced, which QuestKeys maps onto the configs' Bukkit names.
    @BaseProcessor
    public void onBlockBreak(PlayerBrokeBlockEvent event) {
        QuestManager.fireQuestEvent(event.getPlayer().getUuid(), RequirementType.BREAK_BLOCK, event.getBlock().getType(), 1.0);
    }

    @BaseProcessor
    public void onBlockPlace(PlayerPlacedBlockEvent event) {
        QuestManager.fireQuestEvent(event.getPlayer().getUuid(), RequirementType.PLACE_BLOCK, event.getBlock().getType(), 1.0);
    }

    @BaseProcessor
    public void onKillEntity(PlayerKilledEntityEvent event) {
        QuestManager.fireQuestEvent(event.getPlayer().getUuid(), RequirementType.KILL_ENTITY, event.getEntityType(), 1.0);
    }

    @BaseProcessor
    public void onDeath(CosmicDeathEvent event) {
        QuestManager.fireQuestEvent(event.getPlayer().getUuid(), RequirementType.DEATHS, "", 1.0);
    }

    @BaseProcessor
    public void onQuestCompleted(QuestCompletedEvent event) {
        QuestPlayer player = event.getPlayer();
        Quest quest = event.getQuest();

        QuestManager.fireQuestEvent(player.getIdentifier(), RequirementType.QUEST_COMPLETED, quest.getIdentifier(), 1.0);
    }
}
