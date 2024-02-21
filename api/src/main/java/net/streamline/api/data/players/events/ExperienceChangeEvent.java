package net.streamline.api.data.players.events;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.data.players.StreamPlayer;

@Getter @Setter
public class ExperienceChangeEvent extends ExperienceEvent {
    private double newExperience;
    private double oldExperience;

    public ExperienceChangeEvent(StreamPlayer player, double newExperience, double oldExperience) {
        super(player);

        this.newExperience = newExperience;
        this.oldExperience = oldExperience;
    }
}
