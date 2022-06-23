package net.streamline.api.base.command;

import net.md_5.bungee.api.ProxyServer;
import net.streamline.api.base.BasePlugin;

public interface CommandExecutor {
    /**
     * Executes the given command, returning its success
     *
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return true if a valid command, otherwise false
     */
    public boolean onCommand(CommandExecutor sender, Command command, String label, String[] args);

    /**
     * Sends this sender a message
     *
     * @param message Message to be displayed
     */
    public void sendMessage(String message);

    /**
     * Sends this sender multiple messages
     *
     * @param messages An array of messages to be displayed
     */
    public void sendMessage(String[] messages);

    /**
     * Returns the server instance that this command is running on
     *
     * @return Server instance
     */
    public BasePlugin getBase();

    /**
     * Gets the name of this command sender
     *
     * @return Name of the sender
     */
    public String getName();

    /**
     * Gets the uuid of this command sender
     *
     * @return UUID of the sender
     */
    public String getUUID();

    /**
     * Checks if the sender has the given permission
     *
     * @param permission The permission to check
     * @return If the sender has the given permission
     */
    public boolean hasPermission(String permission);

    /**
     * Checks if the sender is online
     *
     * @return whether the sender is online or not
     */
    public boolean isOnline();
}
