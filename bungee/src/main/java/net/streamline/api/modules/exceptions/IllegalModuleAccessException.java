package net.streamline.api.modules.exceptions;

/**
 * Thrown when a plugin attempts to interact with the server when it is not
 * enabled
 */
@SuppressWarnings("serial")
public class IllegalModuleAccessException extends RuntimeException {

    /**
     * Creates a new instance of <code>IllegalPluginAccessException</code>
     * without detail message.
     */
    public IllegalModuleAccessException() {}

    /**
     * Constructs an instance of <code>IllegalPluginAccessException</code>
     * with the specified detail message.
     *
     * @param msg the detail message.
     */
    public IllegalModuleAccessException(String msg) {
        super(msg);
    }
}
