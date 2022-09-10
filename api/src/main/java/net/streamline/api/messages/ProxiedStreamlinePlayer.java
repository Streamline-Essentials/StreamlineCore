package net.streamline.api.messages;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlinePlayer;

import java.util.List;

public class ProxiedStreamlinePlayer {
    @Getter @Setter
    private String latestName;
    @Getter @Setter
    private String displayName;
    @Getter @Setter
    private List<String> tagList;
    @Getter @Setter
    private double points;
    @Getter @Setter
    private String lastMessage;
    @Getter @Setter
    private boolean online;
    @Getter @Setter
    private String latestServer;
    @Getter @Setter
    private boolean bypassPermissions;
    @Getter @Setter
    private double totalXP;
    @Getter @Setter
    private double currentXP;
    @Getter @Setter
    private int level;
    @Getter @Setter
    private int playSeconds;
    @Getter @Setter
    private String latestIP;
    @Getter @Setter
    private List<String> ipList;
    @Getter @Setter
    private List<String> nameList;
    @Getter @Setter
    private int defaultLevel;
    @Getter @Setter
    private String uuid;
}