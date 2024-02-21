package net.streamline.api.interfaces;

import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.objects.StreamlineTitle;
import org.jetbrains.annotations.Nullable;

public interface IMessenger {
    void sendMessage(@Nullable StreamSender to, String message);

    void sendMessage(@Nullable StreamSender to, String otherUUID, String message);

    void sendMessage(@Nullable StreamSender to, StreamSender other, String message);

    void sendMessageRaw(@Nullable StreamSender to, String message);

    void sendMessageRaw(@Nullable StreamSender to, String otherUUID, String message);

    void sendMessageRaw(@Nullable StreamSender to, StreamSender other, String message);

    void sendTitle(StreamSender user, StreamlineTitle title);

    String codedString(String from);

    String stripColor(String string);
}
