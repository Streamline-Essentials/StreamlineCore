package net.streamline.api.permissions;

import host.plas.bou.utils.UuidUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.streamline.api.SLAPI;
import singularity.data.players.CosmicPlayer;
import singularity.permissions.MetaGrabber;
import singularity.permissions.MetaKey;
import singularity.permissions.MetaValue;
import tv.quaint.objects.AtomicString;
import tv.quaint.utils.MathUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class MetaGrabberImpl implements MetaGrabber {
    public static Optional<LuckPerms> tryGetLuckPerms() {
        SLAPI.tryGetLuckPerms();
        return SLAPI.getLpOptional();
    }

    public static void withLuckPerms(Consumer<LuckPerms> consumer) {
        SLAPI.withLuckPerms(consumer);
    }

    @Override
    public Optional<MetaValue> getPrefix(CosmicPlayer player) {
        String username = player.getCurrentName();

        AtomicString prefix = new AtomicString("");
        AtomicInteger priority = new AtomicInteger(0);
        withLuckPerms(luckPerms -> {
            String uuid = UuidUtils.toUuid(username);
            User user = luckPerms.getUserManager().getUser(UUID.fromString(uuid));

            Group group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
            if (group == null) {
                ConcurrentSkipListMap<Integer, String> preWeight = new ConcurrentSkipListMap<>();

                for (PrefixNode node : user.getNodes(NodeType.PREFIX)) {
                    preWeight.put(node.getPriority(), node.getMetaValue());
                }

                prefix.set(preWeight.get(MathUtils.getCeilingInt(preWeight.keySet())));
                priority.set(MathUtils.getCeilingInt(preWeight.keySet()));

                if (prefix.get() == null) prefix.set("");
                if (priority.get() == 0) priority.set(0);
            }

            ConcurrentSkipListMap<Integer, String> preWeight = new ConcurrentSkipListMap<>();

            for (PrefixNode node : group.getNodes(NodeType.PREFIX)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }

            for (PrefixNode node : user.getNodes(NodeType.PREFIX)) {
                preWeight.put(node.getPriority(), node.getMetaValue());
            }

            prefix.set(preWeight.get(MathUtils.getCeilingInt(preWeight.keySet())));
            priority.set(MathUtils.getCeilingInt(preWeight.keySet()));

            if (prefix.get() == null) prefix.set("");
            if (priority.get() == 0) priority.set(0);
        });

        MetaValue metaValue = new MetaValue(player.getIdentifier(), MetaKey.PREFIX, prefix.get(), -1, 0);

        return Optional.of(metaValue);
    }

    @Override
    public Optional<MetaValue> getSuffix(CosmicPlayer player) {
        String username = player.getCurrentName();

        AtomicString suffix = new AtomicString("");
        AtomicInteger priority = new AtomicInteger(0);
        withLuckPerms(luckPerms -> {
            String uuid = UuidUtils.toUuid(username);
            User user = luckPerms.getUserManager().getUser(UUID.fromString(uuid));

            Group group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
            if (group == null) {
                ConcurrentSkipListMap<Integer, String> sufWeight = new ConcurrentSkipListMap<>();

                for (SuffixNode node : user.getNodes(NodeType.SUFFIX)) {
                    sufWeight.put(node.getPriority(), node.getMetaValue());
                }

                suffix.set(sufWeight.get(MathUtils.getCeilingInt(sufWeight.keySet())));
                priority.set(MathUtils.getCeilingInt(sufWeight.keySet()));

                if (suffix.get() == null) suffix.set("");
                if (priority.get() == 0) priority.set(0);
            }

            ConcurrentSkipListMap<Integer, String> sufWeight = new ConcurrentSkipListMap<>();

            for (SuffixNode node : group.getNodes(NodeType.SUFFIX)) {
                sufWeight.put(node.getPriority(), node.getMetaValue());
            }

            for (SuffixNode node : user.getNodes(NodeType.SUFFIX)) {
                sufWeight.put(node.getPriority(), node.getMetaValue());
            }

            suffix.set(sufWeight.get(MathUtils.getCeilingInt(sufWeight.keySet())));
            priority.set(MathUtils.getCeilingInt(sufWeight.keySet()));

            if (suffix.get() == null) suffix.set("");
            if (priority.get() == 0) priority.set(0);
        });

        MetaValue metaValue = new MetaValue(player.getIdentifier(), MetaKey.SUFFIX, suffix.get(), -1, 0);

        return Optional.of(metaValue);
    }

    @Override
    public void setMeta(MetaValue value) {
        // not supported
    }
}
