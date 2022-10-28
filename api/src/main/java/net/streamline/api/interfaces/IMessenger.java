package net.streamline.api.interfaces;

import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import org.jetbrains.annotations.Nullable;

public interface IMessenger {
    void sendMessage(@Nullable StreamlineUser to, String message);

    void sendMessage(@Nullable StreamlineUser to, String otherUUID, String message);

    void sendMessage(@Nullable StreamlineUser to, StreamlineUser other, String message);

    void sendTitle(StreamlinePlayer user, StreamlineTitle title);

    String codedString(String from);

    String stripColor(String string);
}
