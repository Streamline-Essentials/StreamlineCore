package net.streamline.api.savables.users;

import net.streamline.api.configs.StorageResource;

import java.util.List;

public interface StreamlinePlayer extends StreamlineUser {
    double getTotalXP();

    int getLevel();

    int getPlaySeconds();

    List<String> getIpList();

    List<String> getNameList();

    int getDefaultLevel();

    void setCurrentXP(double currentXP);

    void setIpList(List<String> ipList);

    void setNameList(List<String> nameList);

    void setDefaultLevel(int defaultLevel);

    String getLatestIP();

    @Override
    List<String> getTagsFromConfig();

    @Override
    void populateMoreDefaults();

    @Override
    void loadMoreValues();

    @Override
    void saveMore();

    @Override
    void setLatestName(String name);

    void addName(String name);

    void removeName(String name);

    void setLatestIP(String ip);

    void addIP(String ip);

    void removeIP(String ip);

    void addPlaySecond(int amount);

    void removePlaySecond(int amount);

    void setPlaySeconds(int amount);

    double getPlayMinutes();

    double getPlayHours();

    double getPlayDays();

    String getPlaySecondsAsString();

    String getPlayMinutesAsString();

    String getPlayHoursAsString();

    String getPlayDaysAsString();

    /*
   Experience required =
   2 × current_level + 7 (for levels 0–15)
   5 × current_level – 38 (for levels 16–30)
   9 × current_level – 158 (for levels 31+)
    */


    void setLevel(int amount);

    void addLevel(int amount);

    void removeLevel(int amount);

    float getNeededXp();

    double xpUntilNextLevel();

    void addTotalXP(double amount);

    void removeTotalXP(double amount);

    void setTotalXP(double amount);

    float getCurrentLevelXP();

    double getCurrentXP();

    String getDisplayName();

    boolean isConnected();
}
