package net.streamline.api.base.ratapi;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.placeholders.expansions.RATExpansion;
import net.streamline.api.placeholders.replaceables.IdentifiedReplaceable;
import net.streamline.api.placeholders.replaceables.IdentifiedUserReplaceable;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;
import tv.quaint.objects.AtomicString;
import tv.quaint.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class LuckpermsExpansion extends RATExpansion {
    public LuckpermsExpansion() {
        super(new RATExpansionBuilder("luckperms"));
        BaseModule.getInstance().logInfo(getClass().getSimpleName() + " is registered!");
    }

    @Override
    public void init() {
        new IdentifiedUserReplaceable(this, "prefix", (s, user) -> UserUtils.getLuckPermsPrefix(user.getName())).register();
        new IdentifiedUserReplaceable(this, "suffix", (s, user) -> UserUtils.getLuckPermsSuffix(user.getName())).register();

        new IdentifiedUserReplaceable(this, "primary_group", (s, user) -> {
            try {
                LuckPerms api = LuckPermsProvider.get();
                User u = api.getUserManager().getUser(UUID.fromString(user.getUuid()));
                if (u == null) return s.string();

                return u.getPrimaryGroup();
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();

        new IdentifiedUserReplaceable(this, "highest_group", (s, user) -> {
            try {
                LuckPerms api = LuckPermsProvider.get();
                User u = api.getUserManager().getUser(UUID.fromString(user.getUuid()));
                if (u == null) return s.string();

                Optional<Group> group = u.getInheritedGroups(QueryOptions.builder(QueryMode.CONTEXTUAL).build()).stream().findFirst();

                return group.map(Group::getName).orElse(s.string());
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();

        new IdentifiedUserReplaceable(this, MatcherUtils.makeLiteral("meta_") + "(.*?)", 1, (s, user) -> {
            try {
                String params = s.get();

                LuckPerms api = LuckPermsProvider.get();
                User u = api.getUserManager().getUser(UUID.fromString(user.getUuid()));
                if (u == null) return s.string();

                AtomicString s1 = new AtomicString("");
                u.getNodes(NodeType.META).forEach(node -> {
                            if (node.getKey().equals(params)) {
                                s1.set(node.getMetaValue());
                            }
                        }
                );

                if (! s1.get().isEmpty() || !s1.get().isBlank()) {
                    return s1.get();
                }

                Group group = api.getGroupManager().getGroup(u.getPrimaryGroup());
                if (group == null) return s.string();

                group.getNodes(NodeType.META).forEach(node -> {
                            if (node.getKey().equals(params)) {
                                s1.set(node.getMetaValue());
                            }
                        }
                );

                if (! s1.get().isEmpty() || !s1.get().isBlank()) {
                    return s1.get();
                }

                return s.string();
            } catch (Exception e) {
                e.printStackTrace();
                return s.string();
            }
        }).register();
    }
}
