package host.plas.ratapi;

import gg.drak.thebase.utils.MatcherUtils;
import host.plas.database.MyLoader;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.placeholders.expansions.RATExpansion;
import singularity.placeholders.replaceables.IdentifiedReplaceable;
import singularity.placeholders.replaceables.IdentifiedUserReplaceable;
import host.plas.StreamlineMessaging;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.savables.SavableChatter;

public class MessagingExpansion extends RATExpansion {
    public MessagingExpansion() {
        super(new RATExpansionBuilder("messaging"));
    }

    @Override
    public void init() {
        new IdentifiedReplaceable(this, "loaded_chatters", (s) -> String.valueOf(MyLoader.getInstance().getLoaded().size())).register();
        new IdentifiedReplaceable(this, "loaded_channels", (s) -> String.valueOf(StreamlineMessaging.getChatChannelConfig().getChatChannels().size())).register();
        new IdentifiedReplaceable(this, "default_channel", (s) -> StreamlineMessaging.getConfigs().defaultChat()).register();

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("channel_") + "(.*?)", 1, (s, u) -> {
            SavableChatter chatter = MyLoader.getInstance().getOrCreate(u.getUuid());
            String string = startsWithChannel(s.get(), chatter);
            return string == null ? s.string() : string;
        }).register();

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("friends_with_") + "(.*?)", 1, (s, u) -> {
            SavableChatter chatter = MyLoader.getInstance().getOrCreate(u.getUuid());
            String string = startsWithFriendsWith(s.get(), chatter);
            return string == null ? s.string() : string;
        }).register();

        new IdentifiedUserReplaceable(this, "friend_invites_enabled", (s, u) -> {
            SavableChatter chatter = MyLoader.getInstance().getOrCreate(u.getUuid());

            return chatter.isAcceptingFriendRequests() ?
                    MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_TRUE.get() :
                    MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_FALSE.get();
        }).register();
    }

    public String startsWithFriendsWith(String s, SavableChatter chatter) {
        String uuid = ModuleUtils.getUUIDFromName(s).orElse(null);
        if (uuid == null) return "";
        SavableChatter otherChatter = MyLoader.getInstance().getOrCreate(uuid);
        return chatter.isMyBestFriend(otherChatter) ?
                MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_TRUE.get() :
                MainMessagesHandler.MESSAGES.DEFAULTS.PLACEHOLDERS.IS_FALSE.get();
    }

    public String startsWithChannel(String s, SavableChatter chatter) {
        ConfiguredChatChannel channel = chatter.getCurrentChatChannel();
        if (channel == null) return null;
        if (s.equals("identifier")) {
            return channel.getIdentifier();
        }
        if (s.equals("type")) {
            return channel.getType().toString();
        }
        if (s.equals("permission_access")) {
            return channel.getAccessPermission();
        }
        if (s.equals("permission_formatting")) {
            return channel.getFormattingPermission();
        }
        if (s.equals("message")) {
            return channel.getMessage();
        }
        return null;
    }
}
