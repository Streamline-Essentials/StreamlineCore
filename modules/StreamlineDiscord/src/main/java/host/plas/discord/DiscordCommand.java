package host.plas.discord;

import gg.drak.thebase.objects.Identifiable;
import host.plas.discord.data.channeling.Route;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import host.plas.StreamlineDiscord;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.BotMessages;
import singularity.configs.ModularizedConfig;
import singularity.objects.SingleSet;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Setter
@Getter
public abstract class DiscordCommand extends ModularizedConfig {

    private boolean enabled;
    private String commandIdentifier;
    private ConcurrentSkipListSet<String> aliases;
    private long role;
    private String description = "No description provided.";
    private boolean replyEphemeral = false;
    private long slashCommandSnowflake = -1L;
    /** Whether this command is offered as a Discord slash command; the prefix form does not depend on it. */
    private boolean slashEnabled = true;

    public DiscordCommand(String commandIdentifier, String... aliases) {
        this(commandIdentifier, 0L, aliases);
    }

    public DiscordCommand(String commandIdentifier, long role, String... aliases) {
        this(commandIdentifier, new ConcurrentSkipListSet<>(Arrays.stream(aliases).collect(Collectors.toList())), role);
    }

    public DiscordCommand(String commandIdentifier, ConcurrentSkipListSet<String> aliases, long role) {
        super(StreamlineDiscord.getInstance(), commandIdentifier + ".yml",
                DiscordHandler.getDiscordCommandFolder(commandIdentifier), false);
        setCommandIdentifier(commandIdentifier);
        setAliases(aliases);
        setRole(role);

        loadCommand(true);

        register();
    }

    public void loadCommand() {
        loadCommand(false);
    }

    public void loadCommand(boolean force) {
        reloadResource(force);

        setEnabled(getResource().getOrSetDefault("enabled", true));
        setSlashEnabled(getResource().getOrSetDefault("slash-enabled", true));
        setRole(getResource().getOrSetDefault("permissions.default", getRole()));
        setAliases(new ConcurrentSkipListSet<>(getResource().getOrSetDefault("aliases", new ArrayList<>(getAliases()))));
    }

    public void saveCommand() {
        write("enabled", isEnabled());
        write("slash-enabled", isSlashEnabled());
        write("permissions.default", getRole());
        write("aliases", new ArrayList<>(getAliases()));
    }

    public void register() {
        try {
            if (!isEnabled()) return;
            DiscordHandler.registerCommand(this);
            if (isSlashEnabled() && StreamlineDiscord.getConfig().getBotLayout().isSlashCommandsEnabled()) {
                DiscordHandler.registerSlashCommand(this);
            }
        } catch (Exception e) {
            StreamlineDiscord.getInstance().logWarning("Error registering command: " + getCommandIdentifier() + " - " + e.getMessage());
            StreamlineDiscord.getInstance().logWarning(e.getStackTrace());
        }
    }

    public void unregister() {
        DiscordHandler.unregisterCommand(this.getCommandIdentifier());
        if (StreamlineDiscord.getConfig().getBotLayout().isSlashCommandsEnabled()) {
            DiscordHandler.unregisterSlashCommand(this);
        }
    }

    public abstract CommandCreateAction setupOptionData(CommandCreateAction action);

    public boolean isRegistered() {
        return DiscordHandler.isRegistered(this.getCommandIdentifier());
    }

    public boolean hasDefaultPermissions() {
        return getRole() == 0L;
    }

    public boolean defaultPermissionIsServerOwner() {
        return getRole() == -1L;
    }

    public static final String CONTACT_SUFFIX = "Please tell an administrator to contact @drakdv on Discord.";

    public SingleSet<MessageCreateData, BotMessageConfig> execute(MessagedString messagedString) {
        if (hasDefaultPermissions()) {
            return executeMore(messagedString);
        }

        // Any guild channel carries a guild: text, announcement, thread, voice and stage chats alike.
        if (! (messagedString.getChannel() instanceof GuildChannel)) {
            return respond("commands.errors.server-only", messagedString);
        }
        Guild guild = ((GuildChannel) messagedString.getChannel()).getGuild();

        if (guild.getOwnerIdLong() == messagedString.getAuthor().getIdLong()) return executeMore(messagedString);

        Member member = guild.getMember(messagedString.getAuthor());
        if (member == null) {
            // The member cache holds everyone only with the GUILD_MEMBERS intent; otherwise ask Discord.
            try {
                member = guild.retrieveMember(messagedString.getAuthor()).complete();
            } catch (Exception e) {
                return respond("commands.errors.member-lookup-failed", messagedString);
            }
        }

        List<Long> superAdminRoles = StreamlineDiscord.getConfig().getSuperAdminRoles();
        if (member.getRoles().stream().anyMatch(r -> superAdminRoles.contains(r.getIdLong()))) return executeMore(messagedString);

        if (defaultPermissionIsServerOwner()) {
            return respond("commands.errors.owner-only", messagedString);
        }

        Role role = guild.getRoleById(getRole());
        if (role == null) {
            return respond("commands.errors.role-missing", messagedString, "this_role_id", String.valueOf(getRole()));
        }

        if (member.getRoles().contains(role)) return executeMore(messagedString);
        return respond("commands.errors.no-role", messagedString, "this_role_name", role.getName(), "this_role_id", role.getId());
    }

    public abstract SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString);

    /**
     * The bot message {@code key} (see {@link BotMessages}) as this command's reply, with
     * {@code %this_command%}, {@code %this_command_label%} and {@code %this_channel_id%} filled in, plus
     * {@code extra} as alternating placeholder names and values. Null when the owner disabled the message.
     */
    public SingleSet<MessageCreateData, BotMessageConfig> respond(String key, MessagedString messagedString, String... extra) {
        Map<String, String> placeholders = BotMessages.placeholders(extra);
        placeholders.put("this_command", getCommandIdentifier());
        if (messagedString != null) {
            placeholders.put("this_command_label", messagedString.getBase());
            if (messagedString.getChannel() != null) placeholders.put("this_channel_id", messagedString.getChannel().getId());
        }

        return BotMessages.build(key, UserUtils.getConsole(), placeholders);
    }

    public String specialConcat(Collection<String> collection) {
        String before = String.join("`\n`", collection);
        if (before.isBlank()) return before;
        return "`" + before + "`";
    }

    public <I extends Identifiable> String specialConcatIdentified(Collection<I> collection) {
        String before = collection.stream().map(Identifiable::getIdentifier).collect(Collectors.joining("`\n`"));
        if (before.isBlank()) return before;
        return "`" + before + "`";
    }

    public <I extends Enum<I>> String specialConcatEnum(Collection<I> collection) {
        String before = collection.stream().map(Enum::name).collect(Collectors.joining("`\n`"));
        if (before.isBlank()) return before;
        return "`" + before + "`";
    }
}
