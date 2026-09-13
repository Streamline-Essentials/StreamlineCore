package singularity.data.uuid;

import gg.drak.thebase.lib.re2j.Matcher;
import gg.drak.thebase.utils.MatcherUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.IUuidable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the historical usernames and IP addresses associated with a single
 * player UUID.
 *
 * <p>Names and IPs are stored as ordered lists so that the most recently seen
 * value is always at the tail. The class also supports a compact serialised
 * form used when persisting to the database: each entry is delimited by the
 * prefix {@code !!!} and the suffix {@code :::}, for example
 * {@code !!!Steve:::!!!Alex:::}.</p>
 *
 * <p>Instances are registered with and managed by the {@link UuidManager}.</p>
 */
@Getter @Setter
public class UuidInfo implements IUuidable {

    /** The player's unique identifier (UUID string). */
    private String uuid;

    /**
     * The ordered list of usernames that have been associated with this UUID,
     * most recent last.
     */
    private List<String> names;

    /**
     * The ordered list of IP addresses that have been associated with this UUID,
     * most recent last.
     */
    private List<String> ips;

    /**
     * Creates a {@code UuidInfo} with pre-computed name and IP lists.
     *
     * @param uuid  the player UUID string
     * @param names the ordered list of known usernames
     * @param ips   the ordered list of known IP addresses
     */
    public UuidInfo(String uuid, List<String> names, List<String> ips) {
        this.uuid = uuid;
        this.names = names;
        this.ips = ips;
    }

    /**
     * Creates a {@code UuidInfo} by deserialising the compact database format for
     * names and IPs. Each argument may be a single plain value or the delimited
     * {@code !!!entry:::} format produced by {@link #computableNames()} and
     * {@link #computableIps()}.
     *
     * @param uuid  the player UUID string
     * @param names the serialised names string
     * @param ips   the serialised IPs string
     */
    public UuidInfo(String uuid, String names, String ips) {
        this(uuid, computeNames(names), computeIps(ips));
    }

    /**
     * Serialises the name list into the compact database format where each entry is
     * wrapped with {@code !!!} and {@code :::} delimiters.
     *
     * @return the serialised names string, e.g. {@code !!!Steve:::!!!Alex:::}
     */
    public String computableNames() {
        StringBuilder builder = new StringBuilder();

        for (String name : names) {
            builder.append("!!!").append(name).append(":::");
        }

        return builder.toString();
    }

    /**
     * Serialises the IP list into the compact database format where each entry is
     * wrapped with {@code !!!} and {@code :::} delimiters.
     *
     * @return the serialised IPs string
     */
    public String computableIps() {
        StringBuilder builder = new StringBuilder();

        for (String ip : ips) {
            builder.append("!!!").append(ip).append(":::");
        }

        return builder.toString();
    }

    /**
     * Registers this instance with the {@link UuidManager} so it can be looked up
     * in memory.
     */
    public void register() {
        UuidManager.registerUuid(this);
    }

    /**
     * Removes this instance from the {@link UuidManager}.
     */
    public void unregister() {
        UuidManager.unregisterUuid(this);
    }

    /**
     * Persists this instance to the main database via
     * {@link singularity.database.CoreDBOperator#saveUuidInfo(UuidInfo)}.
     */
    public void save() {
        Singularity.getMainDatabase().saveUuidInfo(this);
    }

    /**
     * Appends a username to the name history if it differs from the most recently
     * recorded name.
     *
     * @param name the username to add
     */
    public void addName(String name) {
        if (! names.isEmpty()) if (names.get(names.size() - 1).equals(name)) return;
//        if (names.contains(name)) return;

        names.add(name);
    }

    /**
     * Appends an IP address to the IP history if it differs from the most recently
     * recorded address.
     *
     * @param ip the IP address to add
     */
    public void addIp(String ip) {
        if (! ips.isEmpty()) if (ips.get(ips.size() - 1).equals(ip)) return;
//        if (ips.contains(ip)) return;

        ips.add(ip);
    }

    /**
     * Removes all occurrences of the given username from the name history.
     *
     * @param name the username to remove
     */
    public void removeName(String name) {
        names.removeIf(n -> n.equals(name));
    }

    /**
     * Removes all occurrences of the given IP address from the IP history.
     *
     * @param ip the IP address to remove
     */
    public void removeIp(String ip) {
        ips.removeIf(i -> i.equals(ip));
    }

    /**
     * Returns a copy of the name list with every entry converted to lower-case,
     * suitable for case-insensitive look-ups.
     *
     * @return a new list containing lower-case versions of all known usernames
     */
    public List<String> getNamesCaseInsensitive() {
        List<String> r = new ArrayList<>();

        for (String name : names) {
            r.add(name.toLowerCase());
        }

        return r;
    }

    /**
     * Deserialises a serialised names string into a list of username strings.
     *
     * @param names the serialised names value (either a plain string or the
     *              {@code !!!entry:::} delimited format)
     * @return the ordered list of username strings
     */
    public static List<String> computeNames(String names) {
        return computeList(names);
    }

    /**
     * Deserialises a serialised IPs string into a list of IP address strings.
     *
     * @param ips the serialised IPs value (either a plain string or the
     *            {@code !!!entry:::} delimited format)
     * @return the ordered list of IP address strings
     */
    public static List<String> computeIps(String ips) {
        return computeList(ips);
    }

    /**
     * Deserialises a string that was produced by the compact {@code !!!entry:::}
     * format back into an ordered list of individual values.
     *
     * <p>If {@code listString} is not in the computable format (i.e.
     * {@link #isComputable} returns {@code false}), the raw string is returned as a
     * single-element list.</p>
     *
     * @param listString the raw or serialised string to parse
     * @return the ordered list of decoded values
     */
    public static List<String> computeList(String listString) {
        if (! isComputable(listString)) return new ArrayList<>(List.of(listString));

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

    /**
     * Returns whether the given string uses the compact {@code !!!entry:::} delimited
     * format expected by {@link #computeList(String)}.
     *
     * @param listString the string to inspect
     * @return {@code true} if the string starts with {@code !!!} and ends with
     *         {@code :::}; {@code false} otherwise
     */
    public static boolean isComputable(String listString) {
        return listString.startsWith("!!!") && listString.endsWith(":::");
    }
}
