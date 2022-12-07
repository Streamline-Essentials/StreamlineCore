package net.streamline.api.savables.events;

import net.streamline.api.savables.users.StreamlinePlayer;

public class CreatePlayerEvent extends CreateSavableResourceEvent<StreamlinePlayer> {
    public CreatePlayerEvent(StreamlinePlayer resource) {
        super(resource);
    }
}
