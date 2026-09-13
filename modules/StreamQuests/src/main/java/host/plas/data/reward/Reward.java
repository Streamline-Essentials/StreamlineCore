package host.plas.data.reward;

import gg.drak.thebase.objects.Identifiable;
import host.plas.data.QuestManager;
import host.plas.data.players.QuestPlayer;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Setter
public class Reward implements Identifiable {
    @Getter
    private String identifier;
    private Double points;
    private List<String> commands;

    public Reward(String identifier, @Nullable Double points, @Nullable List<String> commands) {
        this.identifier = identifier;
        this.points = points;
        this.commands = commands;
    }

    public Optional<Double> getPoints() {
        return Optional.ofNullable(points);
    }

    public Optional<List<String>> getCommands() {
        return Optional.ofNullable(commands);
    }

    public boolean hasPointsReward() {
        return getPoints().isPresent();
    }

    public boolean hasCommandReward() {
        return getCommands().isPresent();
    }

    public boolean hasCommands() {
        return hasCommandReward() && ! commands.isEmpty();
    }

    public void rewardPlayer(QuestPlayer player) {
        try {
            getCommands().ifPresent(commands -> {
                QuestManager.runCommands(player, commands);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getPoints().ifPresent(player::addPoints);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
