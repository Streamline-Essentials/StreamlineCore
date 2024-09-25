package singularity.interfaces;

import singularity.data.console.CosmicSender;
import singularity.objects.CosmicTitle;
import org.jetbrains.annotations.Nullable;

public interface IMessenger {
    void sendMessage(@Nullable CosmicSender to, String message);

    void sendMessage(@Nullable CosmicSender to, String otherUUID, String message);

    void sendMessage(@Nullable CosmicSender to, CosmicSender other, String message);

    void sendMessageRaw(@Nullable CosmicSender to, String message);

    void sendMessageRaw(@Nullable CosmicSender to, String otherUUID, String message);

    void sendMessageRaw(@Nullable CosmicSender to, CosmicSender other, String message);

    void sendTitle(CosmicSender user, CosmicTitle title);

    String codedString(String from);

    String stripColor(String string);
}
