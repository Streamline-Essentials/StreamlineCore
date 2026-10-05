package net.streamline.platform.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.streamline.platform.text.LegacyText;
import singularity.gui.CosmicItem;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import singularity.objects.ClickableMessage;
import java.net.URI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.TeleportTransition;
import singularity.objects.PingedResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Minecraft API calls whose shape differs between game versions, for 1.21.11 onward.
 * The same class for older versions lives in {@code mod-common/compat/mc1201} and
 * {@code mod-common/compat/mc1211}.
 */
public final class McCompat {

    private McCompat() {}

    /**
     * The player's latency in milliseconds, as tracked by their connection.
     */
    public static int getPing(ServerPlayer player) {
        return player.connection.latency();
    }

    /** Lowest block Y in the level. */
    public static int minY(ServerLevel level) {
        return level.getMinY();
    }

    /** Highest block Y in the level, inclusive. */
    public static int maxY(ServerLevel level) {
        return level.getMaxY();
    }

    /** The level's dimension id, such as {@code minecraft:overworld}. */
    public static String dimensionId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    /** The biome's id, such as {@code minecraft:plains}, or an empty string for an unregistered biome. */
    public static String biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.identifier().toString()).orElse("");
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        player.teleport(new TeleportTransition(level, new Vec3(x, y, z), Vec3.ZERO, yaw, pitch,
                Set.<Relative>of(), TeleportTransition.DO_NOTHING));
    }

    /** Where new players spawn, with the facing they spawn with. */
    public static SpawnPoint worldSpawn(MinecraftServer server) {
        LevelData.RespawnData data = server.getRespawnData();
        ServerLevel level = server.getLevel(data.dimension());
        if (level == null) level = server.overworld();
        return new SpawnPoint(level, data.pos(), data.yaw(), data.pitch());
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    /** Operator level 2 (gamemaster) or higher. */
    public static boolean isOperator(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** The address the client says it connected to, as sent in its handshake. */
    public static String handshakeHost(ClientIntentionPacket packet) {
        return packet.hostName();
    }

    /** The players listed when hovering the player count in the server list. */
    public static List<PingedResponse.PlayerInfo> readSample(ServerStatus.Players players) {
        List<PingedResponse.PlayerInfo> sample = new ArrayList<>();
        for (NameAndId entry : players.sample()) {
            sample.add(new PingedResponse.PlayerInfo(entry.name(), String.valueOf(entry.id())));
        }
        return sample;
    }

    public static ServerStatus.Players statusPlayers(int max, int online, List<PingedResponse.PlayerInfo> sample) {
        List<NameAndId> entries = new ArrayList<>();
        for (PingedResponse.PlayerInfo info : sample) {
            entries.add(new NameAndId(info.getUniqueId(), info.getName()));
        }
        return new ServerStatus.Players(max, online, entries);
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

    /** A click event for a {@link ClickableMessage} segment, or {@code null} when it has none or its URL is malformed. */
    public static ClickEvent clickEvent(ClickableMessage.ClickAction action, String value) {
        if (action == null || value == null) return null;
        switch (action) {
            case RUN_COMMAND:
                return new ClickEvent.RunCommand(value);
            case SUGGEST_COMMAND:
                return new ClickEvent.SuggestCommand(value);
            case OPEN_URL:
                try {
                    return new ClickEvent.OpenUrl(URI.create(value));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    public static HoverEvent showText(Component text) {
        return new HoverEvent.ShowText(text);
    }

    /** The stack a GUI shows for {@code item}, built from data components. */
    public static ItemStack guiItem(CosmicItem item) {
        if (item == null || item.isAir()) return ItemStack.EMPTY;

        Identifier id = Identifier.tryParse(item.getMaterialKey());
        Item type = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (type == null || type == Items.AIR) type = Items.BARRIER;

        ItemStack stack = new ItemStack(type, item.getAmount());
        if (item.getName() != null) stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(item.getName(), LegacyText.ITEM_NAME));

        if (! item.getLore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : item.getLore()) lore.add(LegacyText.parse(line, LegacyText.ITEM_LORE));
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }

        if (item.isGlowing()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        if (item.isHideExtras()) {
            stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT
                    .withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)
                    .withHidden(DataComponents.ENCHANTMENTS, true)
                    .withHidden(DataComponents.UNBREAKABLE, true));
        }

        if (item.getCustomModelData() > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    List.of((float) item.getCustomModelData()), List.of(), List.of(), List.of()));
        }

        if (item.getSkullOwner() != null && type == Items.PLAYER_HEAD) {
            java.util.UUID uuid = ownerUuid(item.getSkullOwner());
            stack.set(DataComponents.PROFILE, uuid != null
                    ? ResolvableProfile.createUnresolved(uuid)
                    : ResolvableProfile.createUnresolved(item.getSkullOwner()));
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
