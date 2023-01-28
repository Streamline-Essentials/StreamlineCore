package net.streamline.api.savables.users;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.messages.ProxiedStreamlinePlayer;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.events.CreatePlayerEvent;
import net.streamline.api.savables.events.LevelChangePlayerEvent;
import net.streamline.api.savables.events.XPChangePlayerEvent;
import net.streamline.api.utils.MathUtils;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import tv.quaint.thebase.lib.exp4j.tokenizer.UnknownFunctionOrVariableException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class StreamlinePlayer extends StreamlineUser {
    @Getter
    private double totalXP;
    @Setter
    private double currentXP;
    @Getter
    private int level;
    @Getter
    private int playSeconds;
    private String latestIP;
    @Getter @Setter
    private ConcurrentSkipListSet<String> ipList;
    @Getter @Setter
    private ConcurrentSkipListSet<String> nameList;
    @Getter @Setter
    private int defaultLevel;
    @Getter @Setter
    private StreamlineLocation location;

    public String getLatestIP() {
        return SLAPI.getInstance().getUserManager().parsePlayerIP(this.getUuid());
    }

    public StreamlinePlayer(ProxiedStreamlinePlayer player){
        super(player.getUuid(), UserUtils.newUserStorageResource(player.getUuid(), StreamlinePlayer.class));

        setLatestName(player.getLatestName());
        setDisplayName(player.getDisplayName());
        setTagList(player.getTagList());
        setPoints(player.getPoints());
        setLastMessage(player.getLastMessage());
        setOnline(player.isOnline());
        setLatestServer(player.getLatestServer());
        setBypassPermissions(player.isBypassPermissions());
        setTotalXP(player.getTotalXP());
        setCurrentXP(player.getCurrentXP());
        setLevel(player.getLevel());
        setPlaySeconds(player.getPlaySeconds());
        setLatestIP(player.getLatestIP());
        setIpList(player.getIpList());
        setNameList(player.getNameList());
//        setDefaultLevel(player.getDefaultLevel());
        setLocation(player.getLocation());

        if (isFirstLoad()) {
            new CreatePlayerEvent(this).fire();
            setDisplayName(UserUtils.getFormattedDefaultNickname(this));
        }
    }

    public StreamlinePlayer(String uuid){
        super(uuid, UserUtils.newUserStorageResource(uuid, StreamlinePlayer.class));
    }

    public StreamlinePlayer(UUID uuid) {
        this(uuid.toString());
    }

    @Override
    public List<String> getTagsFromConfig() {
        return GivenConfigs.getMainConfig().playerTagsDefault();
    }

    @Override
    public void populateMoreDefaults() {
        // Ips.
        latestIP = getOrSetDefault("player.ips.latest", getLatestIP());
        ipList = new ConcurrentSkipListSet<>(getStringListFromResource("player.ips.list", new ArrayList<>()));
        // Names.
        nameList = new ConcurrentSkipListSet<>(getStringListFromResource("player.names", List.of(this.getLatestName())));
        // Stats.
        level = getOrSetDefault("player.stats.level", GivenConfigs.getMainConfig().playerStartingLevel());
        totalXP = getOrSetDefault("player.stats.experience.total", GivenConfigs.getMainConfig().playerStartingExperienceAmount());
        currentXP = getOrSetDefault("player.stats.experience.current", GivenConfigs.getMainConfig().playerStartingExperienceAmount());
        playSeconds = getOrSetDefault("player.stats.playtime.seconds", 0);
        // Location.
        String locationString = getOrSetDefault("player.location", "");
        if (locationString.equals("")) {
            location = null;
        } else {
            location = new StreamlineLocation(locationString);
        }
    }

    @Override
    public void loadMoreValues() {
        // Ips.
        latestIP = getOrSetDefault("player.ips.latest", latestIP);
        ipList = new ConcurrentSkipListSet<>(getStringListFromResource("player.ips.list", ipList.stream().toList()));
        // Names.
        nameList = new ConcurrentSkipListSet<>(getStringListFromResource("player.names", nameList.stream().toList()));
        // Stats.
        level = getOrSetDefault("player.stats.level", level);
        totalXP = getOrSetDefault("player.stats.experience.total", totalXP);
        currentXP = getOrSetDefault("player.stats.experience.current", currentXP);
        playSeconds = getOrSetDefault("player.stats.playtime.seconds", playSeconds);
        // Location.
        String locationString = getOrSetDefault("player.location", location == null ? "" : location.toString());
        if (locationString.equals("")) {
            location = null;
        } else {
            location = new StreamlineLocation(locationString);
        }
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
        // Location.
        String locationString = location == null ? "" : location.toString();
        set("player.location", locationString);
    }

    @Override
    public void setLatestName(String name) {
        super.setLatestName(name);
        addName(name);
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

    public void addIP(String ip){
        if (ipList.contains(ip)) return;

        ipList.add(ip);
    }

    public void removeIP(String ip){
        if (! ipList.contains(ip)) return;

        ipList.remove(ip);
    }

    public void addPlaySecond(int amount){
        setPlaySeconds(playSeconds + amount);
    }

    public void removePlaySecond(int amount){
        setPlaySeconds(playSeconds - amount);
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
        return MessageUtils.truncate(String.valueOf(this.playSeconds), 2);
    }

    public String getPlayMinutesAsString(){
        //        loadValues();
        return MessageUtils.truncate(String.valueOf(getPlayMinutes()), 2);
    }

    public String getPlayHoursAsString(){
        //        loadValues();
        return MessageUtils.truncate(String.valueOf(getPlayHours()), 2);
    }

    public String getPlayDaysAsString(){
        //        loadValues();
        return MessageUtils.truncate(String.valueOf(getPlayDays()), 2);
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

        ModuleUtils.fireEvent(new LevelChangePlayerEvent(this, oldL));
    }

    public void addLevel(int amount) {
        setLevel(this.level + amount);
    }

    public void removeLevel(int amount) {
        setLevel(this.level - amount);
    }

    public float getNeededXp(){
        float needed = 0;

        String function = ModuleUtils.replaceAllPlayerBungee(this, GivenConfigs.getMainConfig().playerLevelingEquation())
                .replace("%default_level%", String.valueOf(GivenConfigs.getMainConfig().playerStartingLevel()));

        try {
            double result = MathUtils.eval(function);
            needed = (float) result;
        } catch (UnknownFunctionOrVariableException e) {
            MessageUtils.logSevere("Error while calculating needed xp for player " + this.getLatestName() + " due to UnknownFunctionOrVariableException! Placeholders have probably been broken...");
            needed = Float.MAX_VALUE;
        } catch (Exception e) {
            MessageUtils.logSevere("Error while calculating needed xp for player " + this.getLatestName() + "!");
            needed = Float.MAX_VALUE;
        }

        return needed;
    }

    public double xpUntilNextLevel(){
        return getNeededXp() - this.totalXP;
    }

    public void addTotalXP(double amount){
        setTotalXP(this.totalXP + amount);
    }

    public void removeTotalXP(double amount){
        setTotalXP(this.totalXP - amount);
    }

    public void setTotalXP(double amount){
        double old = this.totalXP;

        this.totalXP = amount;

        while (xpUntilNextLevel() <= 0) {
            addLevel(1);
        }

        this.currentXP = getCurrentXP();

        ModuleUtils.fireEvent(new XPChangePlayerEvent(this, old));
    }

    public float getCurrentLevelXP(){
        float needed = 0;

        String function = ModuleUtils.replaceAllPlayerBungee(this, GivenConfigs.getMainConfig().playerLevelingEquation().replace("%streamline_user_level%", String.valueOf(this.level - 1)))
                .replace("%default_level%", String.valueOf(GivenConfigs.getMainConfig().playerStartingLevel()));

        needed = (float) MathUtils.eval(function);

        return needed;
    }

    public double getCurrentXP(){
        //        loadValues();
        return this.totalXP - getCurrentLevelXP();
    }

    public boolean isConnected() {
        return isOnline();
    }
}
