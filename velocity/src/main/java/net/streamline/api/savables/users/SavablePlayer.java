package net.streamline.api.savables.users;

import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.events.LevelChangePlayerEvent;
import net.streamline.api.savables.events.XPChangePlayerEvent;
import net.streamline.base.Streamline;
import net.streamline.utils.MathUtils;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SavablePlayer extends SavableUser {
    public float totalXP;
    public float currentXP;
    public int level;
    public int playSeconds;
    public String latestIP;
    public List<String> ipList;
    public List<String> nameList;

    public int defaultLevel;

    public String getLatestIP() {
        return UserManager.parsePlayerIP(Streamline.getPlayer(this.uuid));
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
        nameList = getOrSetDefault("player.names", List.of(this.latestName));
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

        ModuleUtils.fireEvent(new XPChangePlayerEvent(this, old));
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

    public boolean isConnected() {
        return online;
    }
}
