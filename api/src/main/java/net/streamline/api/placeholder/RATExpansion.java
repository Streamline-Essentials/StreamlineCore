package net.streamline.api.placeholder;

import net.streamline.api.SLAPI;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.TreeMap;

public abstract class RATExpansion {
    public boolean enabled;

    public String identifier;
    public String author;
    public String version;
    public int calls;
    public TreeMap<String, Integer> userCalls;

    public RATExpansion setCallForUser(StreamlineUser user, int set) {
        this.userCalls.put(user.getUUID(), set);
        return this;
    }

    public RATExpansion addCallForUser(StreamlineUser user, int add) {
        if (! this.userCalls.containsKey(user.getUUID())) return this.setCallForUser(user, add);
        else {
            return this.setCallForUser(user, this.userCalls.get(user.getUUID()) + add);
        }
    }

    public RATExpansion addCallForUser(StreamlineUser user) {
        return addCallForUser(user, 1);
    }

    public RATExpansion removeCallForUser(StreamlineUser user, int remove) {
        if (! this.userCalls.containsKey(user.getUUID())) return this.setCallForUser(user, remove);
        else {
            return this.setCallForUser(user, this.userCalls.get(user.getUUID()) - remove);
        }
    }

    public int getUserCalls(StreamlineUser user) {
        Integer calls = userCalls.get(user.getUUID());
        return calls == null ? 0 : calls;
    }

    public RATExpansion(String identifier, String author, String version) {
        this.identifier = identifier;
        this.author = author;
        this.version = version;
        this.calls = 0;
        this.userCalls = new TreeMap<>();

        SLAPI.getInstance().getPlatform().getRATAPI().registerExpansion(this);
    }

    public String doLogic(String params) {
        if (params.equals("logic_version")) return version;
        if (params.equals("logic_author")) return author;
        if (params.equals("logic_identifier")) return identifier;
        if (params.equals("logic_class_name")) return getClass().getSimpleName();
        if (params.equals("logic_calls_previous")) return String.valueOf(this.calls);
        this.calls ++;
        if (params.equals("logic_calls_now")) return String.valueOf(this.calls);

        String logic = onLogic(params);
        return logic == null ? "" : logic;
    }

    public String doRequest(StreamlineUser user, String params) {
        String logic = doLogic(params);
        if (! logic.equals("")) return logic;

        if (params.equals("request_calls_previous")) return String.valueOf(getUserCalls(user));
        addCallForUser(user);
        if (params.equals("request_calls_now")) return String.valueOf(getUserCalls(user));

        String request = onRequest(user, params);
        return request == null ? "" : request;
    }

    public abstract String onLogic(String params);

    public abstract String onRequest(StreamlineUser user, String params);

    public RATExpansion setEnabled(boolean set) {
        this.enabled = set;
        return this;
    }
}
