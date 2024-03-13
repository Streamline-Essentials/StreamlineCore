package net.streamline.api.data.uuid;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.data.IUuidable;
import tv.quaint.thebase.lib.re2j.Matcher;
import tv.quaint.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class UuidInfo implements IUuidable {
    private String uuid;
    private List<String> names;
    private List<String> ips;

    public UuidInfo(String uuid, List<String> names, List<String> ips) {
        this.uuid = uuid;
        this.names = names;
        this.ips = ips;
    }

    public UuidInfo(String uuid, String names, String ips) {
        this(uuid, computeNames(names), computeIps(ips));
    }

    public String computableNames() {
        StringBuilder builder = new StringBuilder();

        for (String name : names) {
            builder.append("!!!").append(name).append(":::");
        }

        return builder.toString();
    }

    public String computableIps() {
        StringBuilder builder = new StringBuilder();

        for (String ip : ips) {
            builder.append("!!!").append(ip).append(":::");
        }

        return builder.toString();
    }

    public void register() {
        UuidManager.registerUuid(this);
    }

    public void unregister() {
        UuidManager.unregisterUuid(this);
    }

    public void save() {
        SLAPI.getMainDatabase().saveUuidInfo(this);
    }

    public void addName(String name) {
        if (! names.isEmpty()) if (names.get(names.size() - 1).equals(name)) return;
//        if (names.contains(name)) return;

        names.add(name);
    }

    public void addIp(String ip) {
        if (! ips.isEmpty()) if (ips.get(ips.size() - 1).equals(ip)) return;
//        if (ips.contains(ip)) return;

        ips.add(ip);
    }

    public void removeName(String name) {
        names.removeIf(n -> n.equals(name));
    }

    public void removeIp(String ip) {
        ips.removeIf(i -> i.equals(ip));
    }

    public static List<String> computeNames(String names) {
        return computeList(names);
    }

    public static List<String> computeIps(String ips) {
        return computeList(ips);
    }

    public static List<String> computeList(String listString) {
        Matcher matcher = MatcherUtils.matcherBuilder("[!][!][!](.*?)[:][:][:]", listString);
        List<String[]> groups = MatcherUtils.getGroups(matcher, 1);

        List<String> r = new ArrayList<>();

        for (String[] group : groups) {
            String thing = group[0];
            if (! r.isEmpty()) if (r.get(r.size() - 1).equals(thing)) continue;

//            if (r.contains(thing)) continue;
            r.add(thing);
        }

        return r;
    }
}
