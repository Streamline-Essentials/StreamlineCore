package host.plas.data.roles;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.utils.MatcherUtils;
import host.plas.data.AbstractGroup;
import host.plas.data.Party;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineGroups;
import host.plas.data.flags.GroupFlag;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class GroupRoleMap {
    public AbstractGroup group;
    public ConcurrentSkipListMap<SavableGroupRole, ConcurrentSkipListSet<CosmicSender>> roles = new ConcurrentSkipListMap<>();

    public GroupRoleMap(AbstractGroup group) {
        this.group = group;
    }

    public void addRole(SavableGroupRole role, ConcurrentSkipListSet<CosmicSender> users) {
        roles.put(role, users);
    }

    public void addRole(SavableGroupRole role) {
        addRole(role, new ConcurrentSkipListSet<>());
    }

    public ConcurrentSkipListSet<CosmicSender> getUsersOf(SavableGroupRole role) {
        ConcurrentSkipListSet<CosmicSender> users = roles.get(role);
        if (users == null) return new ConcurrentSkipListSet<>();
        return users;
    }

    public void applyUser(SavableGroupRole role, CosmicSender user) {
        removeUserAll(user);
        ConcurrentSkipListSet<CosmicSender> users = getUsersOf(role);
        users.add(user);
    }

    public int size() {
        int r = 0;
        for (SavableGroupRole role : getRoles()) {
            r += getUsersOf(role).size();
        }
        return r;
    }

    public ConcurrentSkipListSet<CosmicSender> getAllUsers() {
        ConcurrentSkipListSet<CosmicSender> r = new ConcurrentSkipListSet<>();

        getRoles().forEach(role -> r.addAll(getUsersOf(role)));

        return r;
    }

    public boolean hasUser(CosmicSender user) {
        for (SavableGroupRole role : getRoles()) {
            if (roleHasUser(role, user)) return true;
        }
        return false;
    }

    public boolean roleHasUser(SavableGroupRole role, CosmicSender user) {
        AtomicBoolean r = new AtomicBoolean(false);

        getUsersOf(role).forEach(a -> {
            if (a.getUuid().equals(user.getUuid())) r.set(true);
        });

        return r.get();
    }

    public void clearRoles() {
        roles.clear();
    }

    public void addAllRoles(ConcurrentSkipListSet<SavableGroupRole> roles) {
        roles.forEach(this::addRole);
    }

    public void removeUserAll(CosmicSender user) {
        removeUserAll(user.getUuid());
    }

    public void removeUserAll(String uuid) {
        roles.forEach((role, users) -> users.removeIf(a -> a.getUuid().equals(uuid)));
    }

    public ConcurrentSkipListSet<SavableGroupRole> getRoles() {
        return new ConcurrentSkipListSet<>(roles.keySet());
    }

    public ConcurrentSkipListMap<Float, SavableGroupRole> getRolesOrdered() {
        return getRolesOrdered(getRoles());
    }

    public static ConcurrentSkipListMap<Float, SavableGroupRole> getRolesOrdered(ConcurrentSkipListSet<SavableGroupRole> roles) {
        ConcurrentSkipListMap<Float, SavableGroupRole> r = new ConcurrentSkipListMap<>();

        roles.forEach(role -> {
            float num = role.getPriority();
            while (r.containsKey(num)) {
                num += 0.001F;
            }
            r.put(num, role);
        });

        return r;
    }

    public Float getPriorityOfRole(SavableGroupRole role) {
        for (Float f : getRolesOrdered().keySet()) {
            if (getRolesOrdered().get(f).equals(role)) return f;
        }
        return null;
    }

    public SavableGroupRole getRoleOf(CosmicSender user) {
        for (SavableGroupRole role : getRoles()) {
            if (roleHasUser(role, user)) return role;
        }
        return null;
    }

    public SavableGroupRole getNextRoleOf(CosmicSender user) {
        SavableGroupRole current = getRoleOf(user);
        if (current.equals(getRolesOrdered().lastEntry().getValue())) return null;

        return getRolesOrdered().higherEntry(getPriorityOfRole(current)).getValue();
    }

    public SavableGroupRole getPreviousRoleOf(CosmicSender user) {
        SavableGroupRole current = getRoleOf(user);
        if (current.equals(getRolesOrdered().firstEntry().getValue())) return null;

        return getRolesOrdered().lowerEntry(getPriorityOfRole(current)).getValue();
    }

    public void addUser(CosmicSender user) {
        promote(user);
    }

    public void promote(CosmicSender user) {
//        if (getRoles().isEmpty()) {
//            SavableGroupRole role =
//        }

        if (getRoleOf(user) == null) {
            applyUser(getRolesOrdered().firstEntry().getValue(), user);
            return;
        }

        if (getNextRoleOf(user) == null) return;

//        SavableGroupRole oldRole = getRoleOf(user);
        SavableGroupRole newRole = getNextRoleOf(user);

        removeUserAll(user);
        applyUser(newRole, user);
    }

    public void demote(CosmicSender user) {
        if (getRoleOf(user) == null) applyUser(getRolesOrdered().firstEntry().getValue(), user);

//        SavableGroupRole oldRole = getRoleOf(user);
        SavableGroupRole newRole = getPreviousRoleOf(user);

        removeUserAll(user);
        applyUser(newRole, user);
    }

    public boolean userHas(CosmicSender user, GroupFlag flag) {
        return getRoleOf(user).hasFlag(flag);
    }

    public SavableGroupRole getHigherRole(SavableGroupRole role) {
        if (! getRoles().contains(role)) return null;
        Map.Entry<Float, SavableGroupRole> entry = getRolesOrdered().higherEntry(getPriorityOfRole(role));
        if (entry == null) return null;
        return entry.getValue();
    }

    public SavableGroupRole getLowerRole(SavableGroupRole role) {
        if (! getRoles().contains(role)) return null;
        Map.Entry<Float, SavableGroupRole> entry = getRolesOrdered().lowerEntry(getPriorityOfRole(role));
        if (entry == null) return null;
        return entry.getValue();
    }

    public ConcurrentSkipListSet<SavableGroupRole> rolesAbove(SavableGroupRole role) {
        ConcurrentSkipListSet<SavableGroupRole> r = new ConcurrentSkipListSet<>();
        if (! getRoles().contains(role)) return r;

        SavableGroupRole toCheck = getHigherRole(role);
        while (toCheck != null) {
            r.add(toCheck);
            toCheck = getHigherRole(toCheck);
        }

        return r;
    }

    public ConcurrentSkipListSet<SavableGroupRole> rolesBelow(SavableGroupRole role) {
        ConcurrentSkipListSet<SavableGroupRole> r = new ConcurrentSkipListSet<>();
        if (! getRoles().contains(role)) return r;

        SavableGroupRole toCheck = getLowerRole(role);
        while (toCheck != null) {
            r.add(toCheck);
            toCheck = getLowerRole(toCheck);
        }

        return r;
    }

    public String toString() {
        return toString(getRolesOrdered());
    }

    public static String toString(ConcurrentSkipListMap<Float, SavableGroupRole> roles) {
        StringBuilder builder = new StringBuilder();

        int i = 0;
        for (SavableGroupRole role : roles.values()) {
            StringBuilder flags = new StringBuilder();
            for (String flag : role.getFlags()) {
                flags.append(flag).append("..");
            }

            builder.append("[").append(i).append(":").append("identifier=").append(role.getIdentifier()).append(",")
                    .append("name=").append(role.getName()).append(",").append("priority=").append(role.getPriority())
                    .append(",").append("max").append(role.getMax()).append("flags=").append(flags).append("]");

            i ++;
        }

        return builder.toString();
    }

    public static GroupRoleMap fromString(Party group, String string) {
        GroupRoleMap map = new GroupRoleMap(group);

        Matcher matcher = MatcherUtils.matcherBuilder("\\[(\\d+):(identifier=(.*),name=(.*),priority=(.*),max=(.*),flags=(.*))\\]", string);
        List<String[]> matches = MatcherUtils.getGroups(matcher, 7);

        for (String[] match : matches) {
            try {
                String identifier = match[2];
                String name = match[3];
                int priority = Integer.parseInt(match[4]);
                int max = Integer.parseInt(match[5]);
                String[] flags = match[6].split("\\.\\.");
                ConcurrentSkipListSet<String> groupFlags = new ConcurrentSkipListSet<>(Arrays.asList(flags));

                map.addRole(new SavableGroupRole(identifier, priority, name, max, groupFlags));
            } catch (Exception e) {
                StreamlineGroups.getInstance().logWarning("Failed to parse role from string: " + string);
                e.printStackTrace();
            }
        }

        return map;
    }

    public static String getDefaultRoleString() {
        try {
            ConcurrentSkipListSet<SavableGroupRole> roles = new ConcurrentSkipListSet<>();
            StreamlineGroups.getDefaultRoles().getDefaultRoles().forEach(a -> {
                roles.add(new SavableGroupRole(a.getIdentifier(), a.getPriority(), a.getName(), a.getMax(), a.getFlags()));
            });

            return toString(getRolesOrdered(roles));
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    public static ConcurrentSkipListSet<SavableGroupRole> getDefaultRoles() {
        ConcurrentSkipListSet<SavableGroupRole> roles = new ConcurrentSkipListSet<>();
        StreamlineGroups.getDefaultRoles().getDefaultRoles().forEach(a -> {
            try {
                roles.add(new SavableGroupRole(a.getIdentifier(), a.getPriority(), a.getName(), a.getMax(), a.getFlags()));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });

        return roles;
    }
}
