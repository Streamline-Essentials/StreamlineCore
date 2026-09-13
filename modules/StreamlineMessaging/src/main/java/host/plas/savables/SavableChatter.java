package host.plas.savables;

import host.plas.StreamlineMessaging;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.database.MyLoader;
import host.plas.timers.FriendInviteExpiry;
import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.events.server.CosmicChatEvent;
import singularity.loading.Loadable;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Getter @Setter
public class SavableChatter implements Loadable<SavableChatter> {
    private String identifier;

    public String getUuid() {
        return identifier;
    }

    @Getter
    private ConfiguredChatChannel currentChatChannel;
    @Getter @Setter
    private String replyTo;
    @Getter @Setter
    private String lastMessage;
    @Getter @Setter
    private String lastMessageSent;
    @Getter @Setter
    private String lastMessageReceived;
    @Getter @Setter
    private boolean acceptingFriendRequests;
    @Getter @Setter
    private ConcurrentHashMap<ConfiguredChatChannel, Boolean> viewing;
    @Getter @Setter
    private ConcurrentSkipListMap<Date, String> friends;
    @Getter @Setter
    private ConcurrentSkipListMap<Date, String> ignoring;
    @Getter @Setter
    private ConcurrentSkipListMap<String, FriendInviteExpiry> friendInvites;
    @Getter @Setter
    private ConcurrentSkipListMap<Date, String> bestFriends;

    @Getter @Setter
    private boolean fullyLoaded = false;

    public void setCurrentChatChannel(ConfiguredChatChannel chatChannel) {
        if (! chatChannel.getIdentifier().equals(StreamlineMessaging.getConfigs().defaultChat())) {
            if (StreamlineMessaging.getConfigs().forceDefaultAlways()) {
                ConfiguredChatChannel channel = StreamlineMessaging.getChatChannelConfig().getChatChannels().get(StreamlineMessaging.getConfigs().defaultChat());
                if (channel == null) return;
                chatChannel = channel;
            }
        }

        this.currentChatChannel = chatChannel;
    }

    public SavableChatter(String uuid) {
        this.identifier = uuid;
        
        this.currentChatChannel = StreamlineMessaging.getChatChannelConfig().getChatChannel(StreamlineMessaging.getConfigs().defaultChat());
        this.replyTo = "";
        this.lastMessage = "";
        this.lastMessageSent = "";
        this.lastMessageReceived = "";
        this.acceptingFriendRequests = true;
        this.viewing = new ConcurrentHashMap<>();
        this.friends = new ConcurrentSkipListMap<>();
        this.ignoring = new ConcurrentSkipListMap<>();
        this.friendInvites = new ConcurrentSkipListMap<>();
        this.bestFriends = new ConcurrentSkipListMap<>();
    }
    
    public void setViewed(String channel, boolean viewed) {
        ConfiguredChatChannel chatChannel = StreamlineMessaging.getChatChannelConfig().getChatChannel(channel);
        if (chatChannel == null) return;
        
        viewing.put(chatChannel, viewed);
    }
    
    public boolean isViewed(String channel) {
        ConfiguredChatChannel chatChannel = StreamlineMessaging.getChatChannelConfig().getChatChannel(channel);
        if (chatChannel == null) return false;
        
        return viewing.getOrDefault(chatChannel, false);
    }
    
    public void setFriended(long date, String uuid) {
        friends.put(new Date(date), uuid);
    }
    
    public void setFriended(String uuid) {
        friends.put(new Date(), uuid);
    }
    
    public void setIgnored(long date, String uuid) {
        ignoring.put(new Date(date), uuid);
    }
    
    public void setIgnored(String uuid) {
        ignoring.put(new Date(), uuid);
    }
    
    public void setBestFriended(long date, String uuid) {
        bestFriends.put(new Date(date), uuid);
    }
    
    public void setBestFriended(String uuid) {
        bestFriends.put(new Date(), uuid);
    }
    
    public void setInviteSent(long ticksLeft, String uuid) {
        FriendInviteExpiry expiry = new FriendInviteExpiry(this, MyLoader.getInstance().getOrCreate(uuid), ticksLeft);
    }

    public static String getDefaultChannelsViewing() {
        StringBuilder builder = new StringBuilder();
        StreamlineMessaging.getChatChannelConfig().getChatChannels().forEach((s, configuredChatChannel) -> {
            if (configuredChatChannel.getIdentifier().equals(StreamlineMessaging.getConfigs().defaultChat())) {
                builder.append(s).append(",").append(true).append(";"); // default chat channel is always true
                return;
            }
            builder.append(s).append(",").append(false).append(";");
        });
        return builder.toString();
    }

    public static String getCurrentChannelsViewing(ConcurrentHashMap<ConfiguredChatChannel, Boolean> viewing) {
        StringBuilder builder = new StringBuilder();
        viewing.forEach((configuredChatChannel, aBoolean) -> {
            builder.append(configuredChatChannel.getIdentifier()).append(",").append(aBoolean).append(";");
        });
        return builder.toString();
    }

    public static ConcurrentSkipListMap<Date, String> getFriendsFromList(List<String> strings) {
        ConcurrentSkipListMap<Date, String> map = new ConcurrentSkipListMap<>();

        strings.forEach(s -> {
            try {
                String[] split = s.split(",", 2);
                if (split.length != 2) return;
                map.put(new Date(Long.parseLong(split[0])), split[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return map;
    }

    public static ConcurrentSkipListMap<Date, String> getBestFriendsFromList(Map<String, String> strings) {
        ConcurrentSkipListMap<Date, String> map = new ConcurrentSkipListMap<>();

        strings.forEach((s, s2) -> {
            try {
                map.put(new Date(Long.parseLong(s)), s2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return map;
    }

    public static List<String> friendsToList(ConcurrentSkipListMap<Date, String> map) {
        List<String> list = new ArrayList<>();

        map.forEach((date, s) -> {
            list.add(date.getTime() + "," + s);
        });

        return list;
    }

    public static List<String> bestFriendsToList(ConcurrentSkipListMap<Date, String> map) {
        List<String> list = new ArrayList<>();

        map.forEach((date, s) -> {
            list.add(date.getTime() + "," + s);
        });

        return list;
    }

    public void save() {
        StreamlineMessaging.getKeeper().save(this);
    }

    @Override
    public SavableChatter augment(CompletableFuture<Optional<SavableChatter>> completableFuture, boolean isGet) {
        fullyLoaded = false;

        completableFuture.whenComplete((optional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                fullyLoaded = true;
                return;
            }

            if (optional.isPresent()) {
                SavableChatter user = optional.get();

                this.currentChatChannel = user.currentChatChannel;
                this.replyTo = user.replyTo;
                this.lastMessage = user.lastMessage;
                this.lastMessageSent = user.lastMessageSent;
                this.lastMessageReceived = user.lastMessageReceived;
                this.acceptingFriendRequests = user.acceptingFriendRequests;
                this.viewing.putAll(user.viewing);
                this.friends.putAll(user.friends);
                this.ignoring.putAll(user.ignoring);
                this.friendInvites.putAll(user.friendInvites);
                this.bestFriends.putAll(user.bestFriends);
            } else {
                if (! isGet) {
                    save();
                }
            }

            fullyLoaded = true;
        });

        return this;
    }

    @Override
    public boolean isLoaded() {
        return MyLoader.getInstance().isLoaded(this);
    }

    @Override
    public void load() {
        MyLoader.getInstance().load(this);
    }

    @Override
    public void unload() {
        MyLoader.getInstance().unload(this);
    }

    @Override
    public void save(boolean async) {
        StreamlineMessaging.getKeeper().save(this, async);
    }

    @Override
    public void saveAndUnload(boolean async) {
        save(async);
        unload();
    }

    public CosmicSender replyToAsUser() {
        return UserUtils.getOrCreateSender(getReplyTo()).orElse(null);
    }

    public CosmicSender asUser() {
        return UserUtils.getOrCreateSender(this.getUuid()).orElse(null);
    }

    public void onReply(CosmicSender recipient, String message) {
        SavableChatter other = MyLoader.getInstance().getOrCreate(recipient.getUuid());
        if (StreamlineMessaging.getConfigs().messagingReplyUpdateSender()) setReplyTo(recipient.getUuid());
        if (StreamlineMessaging.getConfigs().messagingReplyUpdateRecipient()) other.setReplyTo(this.getUuid());
        setLastMessageSent(message);
        other.setLastMessageReceived(message);
    }

    public void onReply(CosmicSender recipient, String message, String senderFormat, String recipientFormat) {
        if (this.getUuid().equals(recipient.getUuid())) {
            ModuleUtils.sendMessage(asUser(), StreamlineMessaging.getMessages().errorsMessagingSelf());
            return;
        }

        ModuleUtils.sendMessage(asUser(), senderFormat
                .replace("%this_other%", recipient.getCurrentName())
                .replace("%this_message%", message)
        );
        ModuleUtils.sendMessage(recipient, asUser(), recipientFormat
                .replace("%this_other%", recipient.getCurrentName())
                .replace("%this_message%", message)
        );

        onReply(recipient, message);
    }

    public void onMessage(CosmicSender recipient, String message) {
        SavableChatter other = MyLoader.getInstance().getOrCreate(recipient.getUuid());
        if (StreamlineMessaging.getConfigs().messagingMessageUpdateSender()) setReplyTo(recipient.getUuid());
        if (StreamlineMessaging.getConfigs().messagingMessageUpdateRecipient()) other.setReplyTo(this.getUuid());
        setLastMessageSent(message);
        other.setLastMessageReceived(message);
    }

    public void onMessage(CosmicSender recipient, String message, String senderFormat, String recipientFormat) {
        if (this.getUuid().equals(recipient.getUuid())) {
            ModuleUtils.sendMessage(asUser(), StreamlineMessaging.getMessages().errorsMessagingSelf());
            return;
        }

        ModuleUtils.sendMessage(asUser(), senderFormat
                .replace("%this_other%", recipient.getCurrentName())
                .replace("%this_message%", message)
        );
        ModuleUtils.sendMessage(recipient, asUser(), recipientFormat
                .replace("%this_other%", recipient.getCurrentName())
                .replace("%this_message%", message)
        );

        onMessage(recipient, message);
    }

    public boolean onChannelMessage(CosmicChatEvent event) {
        if (getCurrentChatChannel() == null) {
            ModuleUtils.sendMessage(asUser(), StreamlineMessaging.getMessages().errorsChannelIsNull());
            return false;
        }

        if (ModuleUtils.isCommand(event.getMessage())) {
//            ModuleUtils.runAs(event.getSender(), event.getMessage());
            return false;
        }
        if (getCurrentChatChannel().getIdentifier().equals("none")) {
//            ModuleUtils.chatAs(event.getSender(), event.getMessage());
            return false;
        }

        if (! getCurrentChatChannel().getFormattingPermission().equals("") && ! ModuleUtils.hasPermission(asUser(), getCurrentChatChannel().getFormattingPermission())) {
            event.setMessage(ModuleUtils.stripColor(event.getMessage()));
        }

        if (! getCurrentChatChannel().getAccessPermission().equals("") && ! ModuleUtils.hasPermission(asUser(), getCurrentChatChannel().getAccessPermission())) {
            ModuleUtils.sendMessage(asUser(), StreamlineMessaging.getMessages().errorsChannelNoAccess());
            return false;
        }

        getCurrentChatChannel().sendMessageAs(asUser(), event.getMessage());
        return true;
    }

    public void setViewingEnabled(ConfiguredChatChannel channel, boolean isEnabled) {
        viewing.put(channel, isEnabled);
    }

    public boolean isViewing(ConfiguredChatChannel channel) {
        getViewing().putIfAbsent(channel, true);
        return true;
    }

    public boolean hasViewingPermission(ConfiguredChatChannel channel) {
        CosmicSender sender = asUser();
        if (sender == null) return false;

        return sender.hasPermission(channel.getViewingInfo().getPermission());
    }

    public boolean canMessageFrom(ConfiguredChatChannel channel) {
        return hasViewingPermission(channel) && isViewing(channel);
    }

    public boolean canToggleViewing(ConfiguredChatChannel channel) {
        CosmicSender sender = asUser();
        if (sender == null) return false;

        return sender.hasPermission(channel.getViewingInfo().getTogglePermission());
    }

    public void addFriend(SavableChatter chatter) {
        this.removeInviteTo(chatter);
        getFriends().put(new Date(), chatter.getUuid());
        if (StreamlineMessaging.getMessages().friendsAddSend()) ModuleUtils.sendMessage(this.getUuid(), StreamlineMessaging.getMessages().friendsAddMessage().replace("%this_other%", chatter.getUuid()));
        StreamlineMessaging.getInstance().logInfo("%streamline_parse_" + this.getUuid() + ":::*/*streamline_user_formatted*/*% just added %streamline_parse_" + getUuid() + ":::*/*streamline_user_formatted*/*% as a friend!");
    }

    public void addFriend(String uuid) {
        this.addFriend(MyLoader.getInstance().getOrCreate(uuid));
    }

    public void addFriendOther(String uuid) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(uuid);
        chatter.addFriend(this);
    }

    public void removeFriend(SavableChatter chatter) {
        this.removeInviteTo(chatter);
        getFriends().remove(getFriendedAt(chatter.getUuid()));
        if (StreamlineMessaging.getMessages().friendsRemoveSend()) ModuleUtils.sendMessage(this.getUuid(), StreamlineMessaging.getMessages().friendsRemoveMessage().replace("%this_other%", chatter.getUuid()));
        StreamlineMessaging.getInstance().logInfo("%streamline_parse_" + this.getUuid() + ":::*/*streamline_user_formatted*/*% just removed %streamline_parse_" + getUuid() + ":::*/*streamline_user_formatted*/*% as a friend!");
    }

    public void removeFriend(String uuid) {
        this.removeFriend(MyLoader.getInstance().getOrCreate(uuid));
    }

    public void removeFriendOther(String uuid) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(uuid);
        chatter.removeFriend(this);
    }

    public Date getFriendedAt(String uuid) {
        for (Date date : getFriends().keySet()) {
            if (getFriends().get(date).equals(uuid)) return date;
        }

        return null;
    }

    public Date getMeFriendedAtOf(String uuid) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(uuid);
        return chatter.getFriendedAt(this.getUuid());
    }

    public boolean isMyFriend(SavableChatter chatter) {
        return this.isMyFriend(chatter.getUuid());
    }

    public boolean isMyFriend(String uuid) {
        return getFriends().containsValue(uuid);
    }

    public boolean isMeFriendOf(String uuid) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(uuid);
        return chatter.isMyFriend(this.getUuid());
    }

    public ConcurrentSkipListMap<Integer, String> getFriendsListPaged() {
        double times = Math.ceil(getFriends().size() / ((double) StreamlineMessaging.getMessages().friendsListMaxPerPage()));

        ConcurrentSkipListMap<Integer, String> r = new ConcurrentSkipListMap<>();

        for (int i = 0; i < times; i ++) {
            r.put(i, getFriendsListFrom(i * 5));
        }

        return r;
    }

    public String getFriendsListFrom(int start) {
        StringBuilder builder = new StringBuilder();

        for (int i = start; i < getFriends().size(); i ++) {
            if (i >= start + StreamlineMessaging.getMessages().friendsListMaxPerPage()) continue;
            String uuid = new ArrayList<>(getFriends().values()).get(i);
            CosmicSender user = asUser();
            if (user == null) return "";
            if (i == start + StreamlineMessaging.getMessages().friendsListMaxPerPage() - 1) {
                builder.append(ModuleUtils.replaceAllPlayerBungee(user, StreamlineMessaging.getMessages().friendsListEntryLast()));
            } else {
                builder.append(ModuleUtils.replaceAllPlayerBungee(user, StreamlineMessaging.getMessages().friendsListEntryNotLast()));
            }
        }

        return builder.toString();
    }

    public boolean isAlreadyFriendInvited(SavableChatter chatter) {
        return isAlreadyFriendInvited(chatter.getUuid());
    }

    public boolean isAlreadyFriendInvited(String uuid) {
        return getFriendInvites().containsKey(uuid);
    }

    public void addInviteTo(SavableChatter chatter) {
        getFriendInvites().put(chatter.getUuid(), new FriendInviteExpiry(this, chatter, StreamlineMessaging.getConfigs().friendInviteTime()));
    }

    public void removeInviteTo(SavableChatter chatter) {
        if (isAlreadyFriendInvited(chatter)) {
            FriendInviteExpiry prev = getFriendInvites().remove(chatter.getUuid());
            prev.cancel();
        }
    }

    public void handleInviteExpiryEnd(FriendInviteExpiry expiry) {
        removeInviteTo(expiry.getInvited());
        ModuleUtils.sendMessage(asUser(), StreamlineMessaging.getMessages().friendInviteTimeoutSender()
                .replace("%this_other%", expiry.getInvited().asUser().getCurrentName())
        );
        ModuleUtils.sendMessage(expiry.getInvited().asUser(), StreamlineMessaging.getMessages().friendInviteTimeoutInvited()
                .replace("%this_other%", asUser().getCurrentName())
        );

        save();
    }

    public void addBestFriend(SavableChatter chatter) {
        this.removeInviteTo(chatter);
        getBestFriends().put(new Date(), chatter.getUuid());
        if (StreamlineMessaging.getMessages().bestFriendsAddSend()) ModuleUtils.sendMessage(this.getUuid(), StreamlineMessaging.getMessages().bestFriendsAddMessage().replace("%this_other%", chatter.getUuid()));
        StreamlineMessaging.getInstance().logInfo("%streamline_parse_" + this.getUuid() + ":::*/*streamline_user_formatted*/*% just added %streamline_parse_" + getUuid() + ":::*/*streamline_user_formatted*/*% as a best friend!");
    }

    public void addBestFriend(String uuid) {
        this.addBestFriend(MyLoader.getInstance().getOrCreate(uuid));
    }

    public void addBestFriendOther(String uuid) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(uuid);
        chatter.addBestFriend(this);
    }

    public void removeBestFriend(SavableChatter chatter) {
        this.removeInviteTo(chatter);
        getBestFriends().remove(getBestFriendedAt(chatter.getUuid()));
        if (StreamlineMessaging.getMessages().bestFriendsRemoveSend()) ModuleUtils.sendMessage(this.getUuid(), StreamlineMessaging.getMessages().bestFriendsRemoveMessage().replace("%this_other%", chatter.getUuid()));
        StreamlineMessaging.getInstance().logInfo("%streamline_parse_" + this.getUuid() + ":::*/*streamline_user_formatted*/*% just removed %streamline_parse_" + getUuid() + ":::*/*streamline_user_formatted*/*% as a best friend!");
    }

    public void removeBestFriend(String uuid) {
        this.removeBestFriend(MyLoader.getInstance().getOrCreate(uuid));
    }

    public void removeBestFriendOther(String uuid) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(uuid);
        chatter.removeBestFriend(this);
    }

    public Date getBestFriendedAt(String uuid) {
        for (Date date : getBestFriends().keySet()) {
            if (getBestFriends().get(date).equals(uuid)) return date;
        }

        return null;
    }

    public Date getMeBestFriendedAtOf(String uuid) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(uuid);
        return chatter.getBestFriendedAt(this.getUuid());
    }

    public boolean isMyBestFriend(SavableChatter chatter) {
        return this.isMyBestFriend(chatter.getUuid());
    }

    public boolean isMyBestFriend(String uuid) {
        return getBestFriends().containsValue(uuid);
    }

    public boolean isMeBestFriendOf(String uuid) {
        SavableChatter chatter = MyLoader.getInstance().getOrCreate(uuid);
        return chatter.isMyBestFriend(this.getUuid());
    }

    public ConcurrentSkipListMap<Integer, String> getBestFriendsListPaged() {
        double times = Math.ceil(getBestFriends().size() / ((double) StreamlineMessaging.getMessages().bestFriendsListMaxPerPage()));

        ConcurrentSkipListMap<Integer, String> r = new ConcurrentSkipListMap<>();

        for (int i = 0; i < times; i ++) {
            r.put(i, getBestFriendsListFrom(i * 5));
        }

        return r;
    }

    public String getBestFriendsListFrom(int start) {
        StringBuilder builder = new StringBuilder();

        for (int i = start; i < getBestFriends().size(); i ++) {
            if (i >= start + StreamlineMessaging.getMessages().bestFriendsListMaxPerPage()) continue;
            String uuid = new ArrayList<>(getBestFriends().values()).get(i);
            CosmicSender user = asUser();
            if (user == null) return "";
            if (i == start + StreamlineMessaging.getMessages().bestFriendsListMaxPerPage() - 1) {
                builder.append(ModuleUtils.replaceAllPlayerBungee(user, StreamlineMessaging.getMessages().bestFriendsListEntryLast()));
            } else {
                builder.append(ModuleUtils.replaceAllPlayerBungee(user, StreamlineMessaging.getMessages().bestFriendsListEntryNotLast()));
            }
        }

        return builder.toString();
    }

    public void unregister() {
        getFriendInvites().forEach((s, friendInviteExpiry) -> friendInviteExpiry.cancel());

        MyLoader.getInstance().unload(this);
    }

    public void register() {
        MyLoader.getInstance().load(this);
    }
}
