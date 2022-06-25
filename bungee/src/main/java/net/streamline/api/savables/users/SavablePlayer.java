package net.streamline.api.savables.users;

import net.md_5.bungee.api.SkinConfiguration;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.streamline.api.savables.events.LevelChangePlayerEvent;
import net.streamline.api.savables.events.XPChangePlayerEvent;
import net.streamline.base.Streamline;
import net.streamline.api.savables.UserManager;
import net.streamline.utils.MathUtils;
import net.streamline.utils.MessagingUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

public class SavablePlayer extends SavableUser {
    public float totalXP;
    public float currentXP;
    public int level;
    public int playSeconds;
    public String latestIP;
    public List<String> ipList;
    public List<String> nameList;
    public ProxiedPlayer player;

    public int defaultLevel;

    public String getLatestIP() {
        return UserManager.parsePlayerIP(this.player);
    }

    public SavablePlayer(ProxiedPlayer player) {
        super(player.getUniqueId().toString());
        this.player = player;
        setLatestIP(getLatestIP());
        setLatestName(player.getName());
    }

    public SavablePlayer(String uuid){
        super(uuid);
    }

    public SavablePlayer(UUID uuid) {
        this(uuid.toString());
    }

    @Override
    public List<String> getTagsFromConfig() {
        return Streamline.getMainConfig().playerTagsDefault();
    }

    @Override
    public void populateMoreDefaults() {
        // Ips.
        latestIP = getOrSetDefault("player.ips.latest", getLatestIP());
        ipList = getOrSetDefault("player.ips.list", new ArrayList<>());
        // Names.
        nameList = getOrSetDefault("player.names", new ArrayList<>());
        // Stats.
        level = getOrSetDefault("player.stats.level", Streamline.getMainConfig().playerStartingLevel());
        totalXP = getOrSetDefault("player.stats.experience.total", Streamline.getMainConfig().playerStartingExperienceAmount());
        currentXP = getOrSetDefault("player.stats.experience.current", Streamline.getMainConfig().playerStartingExperienceAmount());
        playSeconds = getOrSetDefault("player.stats.playtime.seconds", 0);
    }

    @Override
    public void loadMoreValues() {
        // Ips.
        latestIP = getOrSetDefault("player.ips.latest", latestIP);
        ipList = getOrSetDefault("player.ips.list", ipList);
        // Names.
        nameList = getOrSetDefault("player.names", nameList);
        // Stats.
        level = getOrSetDefault("player.stats.level", level);
        totalXP = getOrSetDefault("player.stats.experience.total", totalXP);
        currentXP = getOrSetDefault("player.stats.experience.current", currentXP);
        playSeconds = getOrSetDefault("player.stats.playtime.seconds", playSeconds);
    }

    @Override
    public void saveMore() {
        // Ips.
        set("player.ips.latest", latestIP);
        set("player.ips.list", ipList);
        // Names.
        set("player.names", nameList);
        // Stats.
        set("player.stats.level", level);
        set("player.stats.experience.total", totalXP);
        set("player.stats.experience.current", currentXP);
        set("player.stats.playtime.seconds", playSeconds);
    }

    public void addName(String name){
        if (nameList.contains(name)) return;

        nameList.add(name);
    }

    public void removeName(String name){
        if (! nameList.contains(name)) return;

        nameList.remove(name);
    }

    public void setLatestIP(String ip) {
        this.latestIP = ip;
        this.addIP(ip);
        saveAll();
    }

    public void setLatestIP(ProxiedPlayer player) {
        setLatestIP(UserManager.parsePlayerIP(player));
    }

    public void addIP(String ip){
        if (ipList.contains(ip)) return;

        ipList.add(ip);
    }

    public void addIP(ProxiedPlayer player){
        addIP(UserManager.parsePlayerIP(player));
    }

    public void removeIP(String ip){
        if (! ipList.contains(ip)) return;

        ipList.remove(ip);
    }

    public void removeIP(ProxiedPlayer player){
        removeIP(UserManager.parsePlayerIP(player));
    }

    public void addPlaySecond(int amount){
        setPlaySeconds(playSeconds + amount);
    }

    public void setPlaySeconds(int amount){
        playSeconds = amount;
    }

    public double getPlayMinutes(){
        return playSeconds / (60.0d);
    }

    public double getPlayHours(){
        return playSeconds / (60.0d * 60.0d);
    }

    public double getPlayDays(){
        if (playSeconds < 300) return 0;
        return playSeconds / (60.0d * 60.0d * 24.0d);
    }

    public String getPlaySecondsAsString(){
        return MessagingUtils.truncate(String.valueOf(this.playSeconds), 2);
    }

    public String getPlayMinutesAsString(){
        //        loadValues();
        return MessagingUtils.truncate(String.valueOf(getPlayMinutes()), 2);
    }

    public String getPlayHoursAsString(){
        //        loadValues();
        return MessagingUtils.truncate(String.valueOf(getPlayHours()), 2);
    }

    public String getPlayDaysAsString(){
        //        loadValues();
        return MessagingUtils.truncate(String.valueOf(getPlayDays()), 2);
    }

    /*
   Experience required =
   2 × current_level + 7 (for levels 0–15)
   5 × current_level – 38 (for levels 16–30)
   9 × current_level – 158 (for levels 31+)
    */


    public void setLevel(int amount) {
        int oldL = this.level;

        this.level = amount;

        Streamline.getInstance().getProxy().getPluginManager().callEvent(new LevelChangePlayerEvent(this, oldL));
    }

    public void addLevel(int amount) {
        setLevel(this.level + amount);
    }

    public void removeLevel(int amount) {
        setLevel(this.level - amount);
    }

    public float getNeededXp(){
        float needed = 0;

        String function = MessagingUtils.replaceAllPlayerBungee(this, Streamline.getMainConfig().playerLevelingEquation())
                        .replace("%default_level%", String.valueOf(Streamline.getMainConfig().playerStartingLevel()));

        needed = (float) MathUtils.eval(function);

        return needed;
    }

    public float xpUntilNextLevel(){
        return getNeededXp() - this.totalXP;
    }

    public void addTotalXP(float amount){
        setTotalXP(this.totalXP + amount);
    }

    public void removeTotalXP(float amount){
        setTotalXP(this.totalXP - amount);
    }

    public void setTotalXP(float amount){
        float old = this.totalXP;

        this.totalXP = amount;

        while (xpUntilNextLevel() <= 0) {
            addLevel(1);
        }

        this.currentXP = getCurrentXP();

        Streamline.getInstance().getProxy().getPluginManager().callEvent(new XPChangePlayerEvent(this, old));
    }

    public float getCurrentLevelXP(){
        float needed = 0;

        String function = MessagingUtils.replaceAllPlayerBungee(this, Streamline.getMainConfig().playerLevelingEquation().replace("%streamline_user_level%", String.valueOf(this.level - 1)))
                .replace("%default_level%", String.valueOf(Streamline.getMainConfig().playerStartingLevel()));

        needed = (float) MathUtils.eval(function);

        return needed;
    }

    public float getCurrentXP(){
        //        loadValues();
        return this.totalXP - getCurrentLevelXP();
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void connect(ServerInfo target) {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).connect(target);
        }
    }

    public void connect(Server target) {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).connect(target.getInfo());
        }
    }

    
    public Server getServer() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getServer();
        }
        return null;
    }


    public long getPing() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getPing();
        }
        return -1;
    }
    
    public void chat(String message) {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).chat(message);
        }
    }
    
    public String getUUID() {
        return uuid;
    }

    public UUID getUniqueId() {
        return UUID.fromString(uuid);
    }

    
    public Locale getLocale() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getLocale();
        }
        return null;
    }

    
    public byte getViewDistance() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getViewDistance();
        }
        return -1;
    }

    
    public ProxiedPlayer.ChatMode getChatMode() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getChatMode();
        }
        return null;
    }

    
    public boolean hasChatColors() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).hasChatColors();
        }
        return false;
    }

    
    public SkinConfiguration getSkinParts() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getSkinParts();
        }
        return null;
    }

    
    public ProxiedPlayer.MainHand getMainHand() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getMainHand();
        }
        return null;
    }

    
    public void setTabHeader(BaseComponent header, BaseComponent footer) {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).setTabHeader(header, footer);
        }
    }

    public void resetTabHeader() {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).resetTabHeader();
        }
    }

    public void sendTitle(Title title) {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).sendTitle(title);
        }
    }
    
    public String getName() {
        return latestName;
    }

    public void sendMessage(BaseComponent message) {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).sendMessage(message);
        }
    }
    public void sendMessage(String message) {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).sendMessage(MessagingUtils.codedText(message));
        }
    }
    
    public boolean hasPermission(String permission) {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).hasPermission(permission);
        }
        return false;
    }

    @Deprecated
    public InetSocketAddress getAddress() {
        if (online) {
            return Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getAddress();
        }
        return InetSocketAddress.createUnresolved(latestIP, new Random().nextInt(26666));
    }

    
    public SocketAddress getSocketAddress() {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).getSocketAddress();
        }
        return InetSocketAddress.createUnresolved(latestIP, new Random().nextInt(26666));
    }

    @Deprecated
    public void disconnect(String reason) {
        if (online) {
            Objects.requireNonNull(Streamline.getInstance().getPlayer(this.uuid)).disconnect(MessagingUtils.codedText(reason));
        }
    }
    
    public boolean isConnected() {
        return online;
    }
}
