package host.plas.discord;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import gg.drak.thebase.objects.Identifiable;
import gg.drak.thebase.storage.StorageUtils;
import host.plas.discord.data.channeling.Route;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.streamline.api.SLAPI;
import host.plas.StreamlineDiscord;
import host.plas.discord.messaging.BotMessageConfig;
import host.plas.discord.messaging.DiscordMessenger;
import singularity.configs.ModularizedConfig;
import singularity.objects.SingleSet;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;
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
        setRole(getResource().getOrSetDefault("permissions.default", getRole()));
        setAliases(new ConcurrentSkipListSet<>(getResource().getOrSetDefault("aliases", new ArrayList<>(getAliases()))));
    }

    public void saveCommand() {
        write("enabled", isEnabled());
        write("permissions.default", getRole());
        write("aliases", new ArrayList<>(getAliases()));
    }

    public void register() {
        try {
            if (!isEnabled()) return;
            DiscordHandler.registerCommand(this);
            if (StreamlineDiscord.getConfig().getBotLayout().isSlashCommandsEnabled()) {
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

    public SingleSet<MessageCreateData, BotMessageConfig> execute(MessagedString messagedString) {
        if (hasDefaultPermissions()) {
            return executeMore(messagedString);
        }
        if (defaultPermissionIsServerOwner()) {
            if (messagedString.getChannel() instanceof TextChannel) {
                TextChannel serverTextChannel = (TextChannel) messagedString.getChannel();
                if (serverTextChannel.getGuild().getOwnerId().equals(messagedString.getAuthor().getId()))
                    return executeMore(messagedString);
            }
            return DiscordMessenger.simpleMessage("Error. Please tell an administrator to contact Quaint#0001.");
        }
        if (messagedString.getChannel() instanceof TextChannel) {
            TextChannel serverTextChannel = (TextChannel) messagedString.getChannel();
            Role role = serverTextChannel.getGuild().getRoleById(getRole());
            if (role == null) return DiscordMessenger.simpleMessage("Error. Please tell an administrator to contact Quaint#0001.");
            if (! serverTextChannel.getGuild().isMember(messagedString.getAuthor())) return DiscordMessenger.simpleMessage("Error. Please tell an administrator to contact Quaint#0001.");
            Member member = serverTextChannel.getGuild().getMember(messagedString.getAuthor());
            if (member == null) return DiscordMessenger.simpleMessage("Error. Please tell an administrator to contact Quaint#0001.");
            if (member.getRoles().contains(role)) return executeMore(messagedString);
        }
        return DiscordMessenger.simpleMessage("Error. Please tell an administrator to contact Quaint#0001.");
    }

    public abstract SingleSet<MessageCreateData, BotMessageConfig> executeMore(MessagedString messagedString);

    public void ensureJsonFile(String fileName) {
        try {
            SLAPI.getInstance().getResourceAsStream(fileName).close();
        } catch (Exception e) {
            return;
        }

        StorageUtils.ensureFileFromSelf(StreamlineDiscord.getInstance().getWrapper().getPluginClassLoader(), DiscordHandler.getDiscordCommandFolder(getCommandIdentifier()),
                new File(DiscordHandler.getDiscordCommandFolder(getCommandIdentifier()), fileName), fileName);
    }

    public File getFolder() {
        return DiscordHandler.getDiscordCommandFolder(getCommandIdentifier());
    }

    public String getJsonFromFile(String fileName) {
        File[] files = DiscordHandler.getDiscordCommandFolder(getCommandIdentifier()).listFiles((dir, currentFile) -> currentFile.equals(fileName));

        if (files == null) return null;

        if (files.length == 0) {
            loadFile(fileName);
            files = DiscordHandler.getDiscordCommandFolder(getCommandIdentifier()).listFiles((dir, currentFile) -> currentFile.equals(fileName));
        }

        try {
            if (files == null || files.length == 0) {
                return null; // No file found
            }
            return new JsonParser().parse(new JsonReader(new FileReader(files[0]))).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isJsonFile(String wholeInput) {
        if (! wholeInput.startsWith("--file:")) return false;
        wholeInput = wholeInput.substring("--file:".length());
        return wholeInput.endsWith(".json");
    }

    public String getJsonFile(String wholeInput) {
        if (wholeInput.startsWith("--file:")) {
            wholeInput = wholeInput.substring("--file:".length());
        }
        if (! wholeInput.endsWith(".json")) {
            wholeInput = wholeInput + ".json";
        }
        return wholeInput;
    }

    public void loadFile(String name) {
        try {
            File file = new File(getFolder(), name);

            InputStream stream = StreamlineDiscord.class.getClassLoader().getResourceAsStream(name);
            if (stream == null) return;

            try (FileWriter writer = new FileWriter(file)) {
                Scanner scanner = new Scanner(stream);
                while (scanner.hasNextLine()) {
                    writer.write(scanner.nextLine());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
