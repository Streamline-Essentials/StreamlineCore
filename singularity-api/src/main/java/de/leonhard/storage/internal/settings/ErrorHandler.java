package de.leonhard.storage.internal.settings;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public enum ErrorHandler {
    /**
     * Makes the file to return empty values.<br>
     * Does not modify the file.
     */
    EMPTY(null),
    /**
     * Keeps the last saved data made without errors, if no saved data is found (like on init)
     * then an exception will be thrown.<br>
     * Prefer {@link #KEEP_OR_EMPTY} over this.<br>
     * Does not modify the file.
     */
    KEEP_OR_THROW("Could not initialize file!"),
    /**
     * Keeps the last saved data made without errors, if no saved data is found (like on init)
     * then empty values will be returned.<br>
     * Does not modify the file.
     */
    KEEP_OR_EMPTY(null),
    /**
     * Keeps the last saved data made without errors, if no saved data is found (like on init)
     * then the file will be cleared.<br>
     * This modifies the file.
     */
    KEEP_OR_CLEAR(null),
    /**
     * Rollbacks to the last saved data made without errors, if no saved data is found (like on init)
     * then an exception will be thrown.<br>
     * This modifies the file.
     */
    ROLLBACK("Could not initialize file!"),
    /** TODO later implementation
     * Set the default values if exists.
     */
    //SET_DEFAULT,
    /**
     * Clears the file.<br>
     * This modifies the file.
     */
    CLEAR(null);
    private final String reason;
    ErrorHandler(String reason) {
        this.reason = reason;
    }

    @Nullable
    @Contract(pure = true)
    public String getReason() {
        return reason;
    }
}
