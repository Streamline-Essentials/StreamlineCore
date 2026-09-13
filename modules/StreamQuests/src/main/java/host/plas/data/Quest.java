package host.plas.data;

import gg.drak.thebase.objects.Identifiable;
import host.plas.data.players.QuestPlayer;
import host.plas.data.require.Requirement;
import host.plas.data.reward.Reward;
import host.plas.events.own.QuestCompletedEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class Quest implements Identifiable {
    private String identifier;
    private String prettyName;
    private ConcurrentSkipListSet<Requirement> requirements;
    private ConcurrentSkipListSet<Reward> rewards;

    public Quest(String identifier) {
        this.identifier = identifier;
        this.prettyName = identifier;
        this.requirements = new ConcurrentSkipListSet<>();
        this.rewards = new ConcurrentSkipListSet<>();
    }

    public Quest addRequirement(Requirement requirement) {
        this.requirements.add(requirement);
        return this;
    }

    public Quest removeRequirement(String identifier) {
        this.requirements.removeIf(requirement -> requirement.getIdentifier().equals(identifier));
        return this;
    }

    public Quest removeRequirement(Requirement requirement) {
        return removeRequirement(requirement.getIdentifier());
    }

    public Quest addReward(Reward reward) {
        this.rewards.add(reward);
        return this;
    }

    public Quest removeReward(String identifier) {
        this.rewards.removeIf(reward -> reward.getIdentifier().equals(identifier));
        return this;
    }

    public Quest removeReward(Reward reward) {
        return removeReward(reward.getIdentifier());
    }

    public void checkPlayer(QuestPlayer player) {
        boolean passes = QuestManager.checkQuestRequirements(player, this);

        if (passes) {
            if (player.hasCompletedQuest(this)) return;
            rewardPlayer(player);
        }
    }

    public void rewardPlayer(QuestPlayer player) {
        QuestCompletedEvent event = new QuestCompletedEvent(this, player).fire();
        if (event.isCancelled()) return;

        event.getPlayer().completeQuest(event.getQuest());

        event.getQuest().getRewards().forEach(reward -> {
            reward.rewardPlayer(event.getPlayer());
        });
    }
}
