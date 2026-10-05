package net.streamline.platform.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.streamline.platform.text.LegacyText;
import singularity.gui.CosmicItem;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import singularity.objects.ClickableMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import singularity.objects.PingedResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft API calls whose shape differs between game versions, for 1.20.1.
 * The same class for newer versions lives in {@code mod-common/compat/mc1211} and
 * {@code mod-common/compat/current}.
 */
public final class McCompat {

    private McCompat() {}

    /**
     * The player's latency in milliseconds; on 1.20.1 this is a field on the player.
     */
    public static int getPing(ServerPlayer player) {
        return player.latency;
    }

    /** Lowest block Y in the level. */
    public static int minY(ServerLevel level) {
        return level.getMinBuildHeight();
    }

    /** Highest block Y in the level, inclusive; 1.20.1 reports the exclusive bound. */
    public static int maxY(ServerLevel level) {
        return level.getMaxBuildHeight() - 1;
    }

    /** The level's dimension id, such as {@code minecraft:overworld}. */
    public static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    /** The biome's id, such as {@code minecraft:plains}, or an empty string for an unregistered biome. */
    public static String biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        player.teleportTo(level, x, y, z, yaw, pitch);
    }

    /** Where new players spawn, with the facing they spawn with. */
    public static SpawnPoint worldSpawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return new SpawnPoint(level, level.getSharedSpawnPos(), level.getSharedSpawnAngle(), 0F);
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return EnchantmentHelper.hasBindingCurse(stack);
    }

    /** Operator level 2 (gamemaster) or higher. */
    public static boolean isOperator(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    /** The address the client says it connected to, as sent in its handshake. */
    public static String handshakeHost(ClientIntentionPacket packet) {
        return packet.getHostName();
    }

    /** The players listed when hovering the player count in the server list. */
    public static List<PingedResponse.PlayerInfo> readSample(ServerStatus.Players players) {
        List<PingedResponse.PlayerInfo> sample = new ArrayList<>();
        for (GameProfile profile : players.sample()) {
            sample.add(new PingedResponse.PlayerInfo(profile.getName(), String.valueOf(profile.getId())));
        }
        return sample;
    }

    public static ServerStatus.Players statusPlayers(int max, int online, List<PingedResponse.PlayerInfo> sample) {
        List<GameProfile> profiles = new ArrayList<>();
        for (PingedResponse.PlayerInfo info : sample) {
            profiles.add(new GameProfile(info.getUniqueId(), info.getName()));
        }
        return new ServerStatus.Players(max, online, profiles);
    }


    /** A spawn position and the facing that goes with it. */
    public static final class SpawnPoint {
        public final ServerLevel level;
        public final BlockPos pos;
        public final float yaw;
        public final float pitch;

        public SpawnPoint(ServerLevel level, BlockPos pos, float yaw, float pitch) {
            this.level = level;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    /** A click event for a {@link ClickableMessage} segment, or {@code null} when it has none. */
    public static ClickEvent clickEvent(ClickableMessage.ClickAction action, String value) {
        if (action == null || value == null) return null;
        switch (action) {
            case RUN_COMMAND:
                return new ClickEvent(ClickEvent.Action.RUN_COMMAND, value);
            case SUGGEST_COMMAND:
                return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value);
            case OPEN_URL:
                return new ClickEvent(ClickEvent.Action.OPEN_URL, value);
            default:
                return null;
        }
    }

    public static HoverEvent showText(Component text) {
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
    }

    /**
     * The stack a GUI shows for {@code item}. 1.20.1 keeps names, lore, skull owners and model
     * data in NBT, and glints through a hidden enchantment.
     */
    public static ItemStack guiItem(CosmicItem item) {
        if (item == null || item.isAir()) return ItemStack.EMPTY;

        ResourceLocation id = ResourceLocation.tryParse(item.getMaterialKey());
        Item type = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (type == null || type == Items.AIR) type = Items.BARRIER;

        ItemStack stack = new ItemStack(type, item.getAmount());
        if (item.getName() != null) stack.setHoverName(LegacyText.parse(item.getName(), LegacyText.ITEM_NAME));

        if (! item.getLore().isEmpty()) {
            ListTag lore = new ListTag();
            for (String line : item.getLore()) {
                lore.add(StringTag.valueOf(Component.Serializer.toJson(LegacyText.parse(line, LegacyText.ITEM_LORE))));
            }
            stack.getOrCreateTagElement("display").put("Lore", lore);
        }

        if (item.isGlowing()) {
            stack.enchant(Enchantments.UNBREAKING, 1);
            stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);
        }

        if (item.isHideExtras()) {
            stack.hideTooltipPart(ItemStack.TooltipPart.MODIFIERS);
            stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);
            stack.hideTooltipPart(ItemStack.TooltipPart.UNBREAKABLE);
            stack.hideTooltipPart(ItemStack.TooltipPart.ADDITIONAL);
        }

        if (item.getCustomModelData() > 0) stack.getOrCreateTag().putInt("CustomModelData", item.getCustomModelData());

        if (item.getSkullOwner() != null && type == Items.PLAYER_HEAD) {
            java.util.UUID uuid = ownerUuid(item.getSkullOwner());
            if (uuid != null) {
                stack.getOrCreateTag().put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), new GameProfile(uuid, null)));
            } else {
                stack.getOrCreateTag().putString("SkullOwner", item.getSkullOwner());
            }
        }

        return stack;
    }
    /** A player's UUID, or {@code null} when {@code owner} is a name. */
    private static java.util.UUID ownerUuid(String owner) {
        try {
            return java.util.UUID.fromString(owner);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
