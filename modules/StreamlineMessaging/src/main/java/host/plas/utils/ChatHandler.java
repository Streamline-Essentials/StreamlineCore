package host.plas.utils;

import host.plas.StreamlineMessaging;
import host.plas.configs.ConfiguredChatChannel;

import java.util.Optional;

public class ChatHandler {
    public static Optional<ConfiguredChatChannel> getDefaultChannel() {
        String defaultChat = StreamlineMessaging.getConfigs().defaultChat();

        return Optional.ofNullable(StreamlineMessaging.getChatChannelConfig().getChatChannel(defaultChat));
    }
}
