package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import host.plas.StreamlineMessaging;

import java.util.concurrent.ConcurrentSkipListMap;

@Getter @Setter
public class ChatChannelConfig extends SimpleConfiguration {
    private ConcurrentSkipListMap<String, ConfiguredChatChannel> chatChannels;

    public ChatChannelConfig() {
        super("chat-channels.yml", StreamlineMessaging.getInstance().getDataFolder(), true);
    }

    @Override
    public void init() {
        chatChannels = getChatChannelsFromConfig();
        chatChannels.forEach(((s, channel) -> channel.setupCommand()));
    }

    @Override
    public void reloadResource(boolean force) {
        super.reloadResource(force);

        chatChannels = getChatChannelsFromConfig();
        chatChannels.forEach(((s, channel) -> channel.setupCommand()));
    }

    public ConcurrentSkipListMap<String, ConfiguredChatChannel> getChatChannelsFromConfig() {
        ConcurrentSkipListMap<String, ConfiguredChatChannel> r = new ConcurrentSkipListMap<>();

        getResource().singleLayerKeySet().forEach(a -> {
            try {
                ConfiguredChatChannel.Type t = ConfiguredChatChannel.Type.valueOf(getResource().getOrSetDefault(a + ".type", ConfiguredChatChannel.Type.ROOM.toString()));
                String prefix = getResource().getOrSetDefault(a + ".prefix", "");
                String accessPermission = getResource().getOrSetDefault(a + ".permissions.access", "streamline.messaging.chats." + a + ".access");
                String formattingPermission = getResource().getOrSetDefault(a + ".permissions.formatting", "streamline.messaging.chats." + a + ".formatting");
                String message = getResource().getOrSetDefault(a + ".message", "%this_message%");

                String viewBasePermission = getResource().getOrSetDefault(a + ".view.permission", "streamline.messaging.chats." + a + ".view");
                String viewTogglePermission = getResource().getOrSetDefault(a + ".view.toggle.permission", "streamline.messaging.chats." + a + ".toggle");
                ViewingInfo viewingInfo = new ViewingInfo(viewBasePermission, viewTogglePermission);
                String commandBase = getResource().getOrSetDefault(a + ".command-base", a);

                ConfiguredChatChannel channel = new ConfiguredChatChannel(a, t, prefix, accessPermission, formattingPermission, message, viewingInfo, commandBase);
                r.put(channel.getIdentifier(), channel);
            } catch (Exception e) {
                StreamlineMessaging.getInstance().logWarning("Could not load chat channel with identifier '" + a + "' due to: " + e.getMessage());
            }
        });

        r.remove("none");
        r.put("none", new ConfiguredChatChannel("none", ConfiguredChatChannel.Type.LOCAL, "-", "", "", "", new ViewingInfo("", ""), "chat-none"));

        return r;
    }

    public ConfiguredChatChannel getChatChannel(String identifier) {
        return getChatChannels().get(identifier);
    }
}
