package host.plas.discord;

import gg.drak.thebase.async.AsyncUtils;
import gg.drak.thebase.objects.AtomicString;
import host.plas.StreamlineDiscord;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import host.plas.events.streamline.bot.BotReadyEvent;
import host.plas.events.streamline.bot.posting.DiscordMessageEvent;
import singularity.modules.ModuleUtils;
import singularity.scheduler.TaskManager;

import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordListener extends ListenerAdapter {
    private static final AtomicBoolean warnedNoMessageContent = new AtomicBoolean(false);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String text = describe(event.getMessage());

        if (text.isEmpty()) {
            // Without MESSAGE_CONTENT, Discord delivers other users' guild messages with empty
            // text, attachments and embeds; relaying that would post a blank line in-game.
            if (! event.getAuthor().isBot()
                    && ! event.getJDA().getGatewayIntents().contains(GatewayIntent.MESSAGE_CONTENT)
                    && warnedNoMessageContent.compareAndSet(false, true)) {
                StreamlineDiscord.getInstance().logWarning(
                        "&cDiscord messages arrive without their text, so they are not relayed.&r%newline%" +
                        "&eEnable '&bMessage Content Intent&e' for the bot at &bhttps://discord.com/developers/applications&e " +
                        "(Bot tab), then restart the bot.");
            }
            return;
        }

        ModuleUtils.fireEvent(new DiscordMessageEvent(new MessagedString(event.getAuthor(), event.getChannel(), text)));
    }

    /**
     * The message's text, followed by a short tag for each attachment and sticker so a
     * picture-only message still shows up in-game. Empty when there is nothing to show.
     */
    private static String describe(Message message) {
        StringBuilder sb = new StringBuilder(message.getContentRaw().trim());

        for (Message.Attachment attachment : message.getAttachments()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(attachment.isImage() ? "[image]" : attachment.isVideo() ? "[video]" : "[file: " + attachment.getFileName() + "]");
        }
        for (StickerItem sticker : message.getStickers()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append("[sticker: ").append(sticker.getName()).append(']');
        }

        return sb.toString();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        AsyncUtils.runAsync(() -> {
            new BotReadyEvent(DiscordHandler.getUser(event.getJDA().getSelfUser().getIdLong())).fire();
        }, 40L); // Run after 2 seconds to ensure the bot is fully ready
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        DiscordCommand command = DiscordHandler.getSlashCommand(event.getCommandIdLong());
        if (command == null) {
            StreamlineDiscord.getInstance().logWarning("No command found for command id: " + event.getCommandIdLong());
            return;
        } else {
            StreamlineDiscord.getInstance().logDebug("Command found for command id: " + event.getCommandIdLong());
        }

        AtomicString message = new AtomicString(command.getCommandIdentifier());

        event.getOptions().forEach(option -> {
            String current = message.get();
            message.set(current + " " + option.getAsString());
        });

        MessagedString messagedString = new MessagedString(event.getUser(), event.getChannel(), message.get());
        ReplyCallbackAction action = event.reply(command.execute(messagedString).getKey());
        if (command.isReplyEphemeral()) action = action.setEphemeral(true);
        action.queue();
    }
}
