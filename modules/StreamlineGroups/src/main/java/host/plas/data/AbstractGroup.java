package host.plas.data;

import gg.drak.thebase.objects.Identified;
import host.plas.StreamlineGroups;
import host.plas.data.events.InviteCreateEvent;
import host.plas.data.flags.GroupFlag;
import host.plas.data.invites.InviteTicker;
import host.plas.data.roles.GroupRoleMap;
import host.plas.data.roles.SavableGroupRole;
import lombok.Getter;
import lombok.Setter;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.streamline.api.permissions.LuckPermsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Base type for the groups a player can belong to, such as a {@link Party} or a
 * {@link Guild}.
 *
 * <p>Groups are {@link Identified} rather than {@code Comparable<AbstractGroup>} so that
 * subclasses are free to implement {@code Loadable}, which carries its own
 * {@code Comparable<Identified>} contract.</p>
 */
@Getter @Setter
public class AbstractGroup implements Identified {
    private String uuid;
    private GroupType type;

    public String getIdentifier() {
        return getUuid();
    }

    public void setIdentifier(String identifier) {
        setUuid(identifier);
    }

    private CosmicSender owner;
    private ConcurrentSkipListSet<InviteTicker> invites = new ConcurrentSkipListSet<>();
    private boolean isMuted;
    private boolean isPublic;
    private int maxSize;
    private Date createDate;
    private GroupRoleMap groupRoleMap;
    /** Guards {@link #dispose()} so that disbanding twice is a no-op. */
    private boolean disposed = false;

    public AbstractGroup(GroupType type, String uuid, @Nullable CosmicSender owner, boolean load) {
        this.uuid = uuid;
        this.type = type;

        populateDefaults();

        updateOwner(owner);

        if (! isLoaded() && load) load();

        grabFromDatabase();
    }

    public AbstractGroup(GroupType type, String uuid, CosmicSender owner) {
        this(type, uuid, owner, true);
    }

    public AbstractGroup(GroupType type, CosmicSender owner, boolean load) {
        this(type, UUID.randomUUID().toString(), owner, load);
    }

    public AbstractGroup(GroupType type, CosmicSender owner) {
        this(type, owner, true);
    }

    public AbstractGroup(GroupType type, String uuid, boolean load) {
        this(type, uuid, null, load);
    }

    public AbstractGroup(GroupType type, String uuid) {
        this(type, uuid, true);
    }

    public String getClassedIdentifier() {
        return getClass().getSimpleName() + "-" + getUuid();
    }

    /**
     * Orders groups by their classed identifier, so that a {@link Party} and a
     * {@link Guild} sharing a uuid remain distinct entries in the sorted sets that hold
     * loaded groups.
     */
    @Override
    public int compareTo(@NotNull Identified o) {
        if (o instanceof AbstractGroup) {
            return getClassedIdentifier().compareTo(((AbstractGroup) o).getClassedIdentifier());
        }
        return getIdentifier().compareTo(o.getIdentifier());
    }

    public void grabFromDatabase() {

    }

    /**
     * Adopts the given owner, re-deriving the size cap from their permissions and placing
     * them in the highest configured role.
     */
    public void updateOwner(CosmicSender owner) {
        if (owner == null) return;

        this.owner = owner;
        this.maxSize = getMaxSize(owner);

        // With no roles configured there is no leader role to apply the owner to.
        Map.Entry<Float, SavableGroupRole> highest = groupRoleMap.getRolesOrdered().lastEntry();
        if (highest == null) {
            StreamlineGroups.getInstance().logWarning("No roles are configured; the owner of "
                    + getClassedIdentifier() + " could not be given a role.");
            return;
        }

        this.groupRoleMap.applyUser(highest.getValue(), this.owner);
    }

    public void load() {
        GroupManager.load(this);

        loadMore();
    }

    public void loadMore() {

    }

    public void unload() {
        GroupManager.unload(this);

        unloadMore();
    }

    public void unloadMore() {

    }

    public boolean isLoaded() {
        return GroupManager.isLoaded(this);
    }

    public void populateDefaults() {
        // Settings.
        isMuted = false;
        isPublic = false;
        maxSize = StreamlineGroups.getConfigs().baseMax("default");
        createDate = new Date();
        groupRoleMap = new GroupRoleMap(this);
        getGroupRoleMap().clearRoles();
        getGroupRoleMap().addAllRoles(GroupRoleMap.getDefaultRoles());

        populateDefaultsMore();
    }

    public void populateDefaultsMore() {

    }

    public ConcurrentSkipListSet<CosmicSender> parseUserListFromUUIDs(ConcurrentSkipListSet<String> uuids) {
        ConcurrentSkipListSet<CosmicSender> users = new ConcurrentSkipListSet<>();

        for (String uuid : uuids) {
            CosmicSender u = ModuleUtils.getOrCreateSender(uuid).orElse(null);
            if (u == null) continue;

            if (users.contains(u)) continue;

            users.add(u);
        }

        return users;
    }

    public ConcurrentSkipListSet<String> parseUUIDListFromUsers(ConcurrentSkipListSet<CosmicSender> users) {
        ConcurrentSkipListSet<String> uuids = new ConcurrentSkipListSet<>();

        for (CosmicSender user : users) {
            if (uuids.contains(user.getUuid())) continue;

            uuids.add(user.getUuid());
        }

        return uuids;
    }

    public void addMember(CosmicSender user) {
        groupRoleMap.addUser(user);
        remFromInvites(user);
    }

    public void removeMember(CosmicSender user) {
        groupRoleMap.removeUserAll(user);
        remFromInvites(user);
    }

    public CosmicSender getMember(String uuid) {
        return ModuleUtils.getOrCreateSender(uuid).orElse(null);
    }

    public ConcurrentSkipListSet<CosmicSender> getAllUsers() {
        return groupRoleMap.getAllUsers();
    }

    public boolean hasInvite(CosmicSender user) {
        return getInviteTicker(user) != null;
    }

    public boolean hasMember(CosmicSender stat){
        return groupRoleMap.hasUser(stat);
    }

    public int getSize(){
        return groupRoleMap.size();
    }

    public InviteTicker getInviteTicker(CosmicSender invited) {
        if (invited == null) return null;

        for (InviteTicker inviteTicker : invites) {
            CosmicSender other = inviteTicker.getInvited();
            if (other != null && other.getUuid().equals(invited.getUuid())) return inviteTicker;
        }

        return null;
    }

    public void remFromInvites(InviteTicker ticker){
        invites.remove(ticker);
    }

    /** Cancels and drops the pending invite for the given user, if there is one. */
    public void remFromInvites(CosmicSender user){
        InviteTicker ticker = getInviteTicker(user);
        if (ticker == null) return;

        ticker.cancel();
        invites.remove(ticker);
    }

    /** Drops the user's invite <em>and</em> removes them from every role. */
    public void remFromInvitesCompletely(CosmicSender user){
        remFromInvites(user);
        groupRoleMap.removeUserAll(user);
    }

    public ConcurrentSkipListSet<CosmicSender> getInvitesAsUsers() {
        ConcurrentSkipListSet<CosmicSender> users = new ConcurrentSkipListSet<>();

        invites.forEach(a -> users.add(a.getInvited()));

        return users;
    }

    public void addInvite(CosmicSender inviter, CosmicSender to) {
        if (hasInvite(to)) return;
        invites.add(new InviteTicker(this, to, inviter));
        ModuleUtils.fireEvent(new InviteCreateEvent<>(this, to, inviter));
    }

    public void setMuted(boolean bool) {
        isMuted = bool;
    }

    public void toggleMute(){
        setMuted(! isMuted);
    }

    public void setPublic(boolean bool){
        isPublic = bool;
    }

    public void togglePublic() {
        setPublic(! isPublic);
    }

    public SavableGroupRole getRole(CosmicSender member){
        return groupRoleMap.getRoleOf(member);
    }

    public boolean userHasFlag(CosmicSender user, GroupFlag flag) {
        return groupRoleMap.userHas(user, flag);
    }

    /**
     * Sets the size cap, clamped to what the owner's permissions actually allow.
     *
     * <p>The ceiling is derived from the owner rather than from the group's own uuid,
     * which is not a sender identifier.</p>
     */
    public void setMaxSize(int size){
        CosmicSender user = getOwner();
        if (user == null) {
            // No owner resolved yet (a group loaded before its owner). Take the value as
            // given; updateOwner re-derives the ceiling once an owner arrives.
            this.maxSize = size;
            return;
        }

        if (size <= getMaxSize(user))
            this.maxSize = size;
    }

    public int getMaxSize(CosmicSender leader){
        if (! (leader instanceof CosmicPlayer)) {
            return StreamlineGroups.getConfigs().baseMax("default");
        }

        try {
            if (LuckPermsHandler.hasLuckPerms()) {
                User user = LuckPermsProvider.get().getUserManager().getUser(leader.getCurrentName());
                if (user == null) {
                    StreamlineGroups.getInstance().logInfo("Could not get LuckPerms user with name '" + leader.getCurrentName() + "'.");
                    return StreamlineGroups.getConfigs().baseMax("default");
                }
                String group = user.getPrimaryGroup();

                return StreamlineGroups.getConfigs().baseMax(group);
            } else {
                return StreamlineGroups.getConfigs().baseMax("default");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return StreamlineGroups.getConfigs().baseMax("default");
        }
    }

    public void setMemberLevel(CosmicSender user, SavableGroupRole role) {
        groupRoleMap.applyUser(role, user);
    }

    public void promoteUser(CosmicSender user) {
        groupRoleMap.promote(user);
    }

    public void demoteUser(CosmicSender user) {
        groupRoleMap.demote(user);
    }

    /**
     * Tears the group down: cancels every outstanding invite, drops all members and
     * removes it from the loaded set.
     *
     * <p>Safe to call more than once -- a group that has already been disposed of simply
     * returns.</p>
     */
    public void disband() {
        if (disposed) return;

        try {
            unload();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            dispose();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Releases the group's state. Collections are cleared rather than nulled so that a
     * stale reference to a disbanded group cannot trigger a {@link NullPointerException}.
     */
    public void dispose() {
        if (disposed) return;
        disposed = true;

        try {
            for (InviteTicker ticker : invites) {
                try {
                    ticker.cancel();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            invites.clear();

            groupRoleMap.clearRoles();
            owner = null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}