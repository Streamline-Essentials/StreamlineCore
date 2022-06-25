package net.streamline.api.command;

import net.streamline.api.BasePlugin;
import net.streamline.api.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ICommandSender extends Permissible {

    /**
     * Sends this sender a message
     *
     * @param message Message to be displayed
     */
    public void sendMessage(@NotNull String message);

    /**
     * Sends this sender multiple messages
     *
     * @param messages An array of messages to be displayed
     */
    public void sendMessage(@NotNull String... messages);

    /**
     * Sends this sender a message
     *
     * @param message Message to be displayed
     * @param sender The sender of this message
     */
    public void sendMessage(@Nullable UUID sender, @NotNull String message);

    /**
     * Sends this sender multiple messages
     *
     * @param messages An array of messages to be displayed
     * @param sender The sender of this message
     */
    public void sendMessage(@Nullable UUID sender, @NotNull String... messages);

    /**
     * Returns the server instance that this command is running on
     *
     * @return Server instance
     */
    @NotNull
    public BasePlugin getBase();

    /**
     * Gets the name of this command sender
     *
     * @return Name of the sender
     */
    @NotNull
    public String getName();

    /**
     * Gets the name of this command sender
     *
     * @return UUID of the sender
     */
    @NotNull
    public String getUUID();
}
