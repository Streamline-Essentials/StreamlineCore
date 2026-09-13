package host.plas.events;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.StreamQuests;
import host.plas.data.Quest;
import host.plas.data.QuestManager;
import host.plas.data.players.QuestPlayer;
import host.plas.data.require.RequirementType;
import host.plas.events.own.QuestCompletedEvent;
import singularity.data.players.CosmicPlayer;
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

        String message = event.getMessage();
        if (message.startsWith("/")) {
            QuestManager.fireQuestEvent(player.getUuid(), RequirementType.RUN_COMMAND, message, 1);
        } else {
            QuestManager.fireQuestEvent(player.getUuid(), RequirementType.CHAT, message, 1);
        }
    }

    @BaseProcessor
    public void onQuestCompleted(QuestCompletedEvent event) {
        QuestPlayer player = event.getPlayer();
        Quest quest = event.getQuest();

        QuestManager.fireQuestEvent(player.getIdentifier(), RequirementType.QUEST_COMPLETED, quest.getIdentifier(), 1.0);
    }
}
