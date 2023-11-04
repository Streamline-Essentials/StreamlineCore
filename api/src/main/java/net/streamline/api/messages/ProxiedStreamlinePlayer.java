package net.streamline.api.messages;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlineLocation;

import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class ProxiedStreamlinePlayer {
    @Setter
    private String latestName;
    @Setter
    private String displayName;
    @Setter
    private ConcurrentSkipListSet<String> tagList;
    @Setter
    private double points;
    @Setter
    private String lastMessage;
    @Setter
    private boolean online;
    @Setter
    private String latestServer;
    @Setter
    private boolean bypassPermissions;
    @Setter
    private double totalXP;
    @Setter
    private double currentXP;
    @Setter
    private int level;
    @Setter
    private int playSeconds;
    @Setter
    private String latestIP;
    @Setter
    private ConcurrentSkipListSet<String> ipList;
    @Setter
    private ConcurrentSkipListSet<String> nameList;
    @Setter
    private int defaultLevel;
    @Setter
    private String uuid;
    @Setter
    private StreamlineLocation location;
}
