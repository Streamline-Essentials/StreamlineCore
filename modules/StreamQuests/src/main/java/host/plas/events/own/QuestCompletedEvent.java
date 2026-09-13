package host.plas.events.own;

import host.plas.StreamQuests;
import host.plas.data.Quest;
import host.plas.data.players.QuestPlayer;
import lombok.Getter;
import lombok.Setter;
import singularity.events.modules.ModuleEvent;

@Getter @Setter
public class QuestCompletedEvent extends ModuleEvent {
    private Quest quest;
    private QuestPlayer player;

    public QuestCompletedEvent(Quest quest, QuestPlayer player) {
        super(StreamQuests.getInstance());

        this.quest = quest;
        this.player = player;
    }
}
