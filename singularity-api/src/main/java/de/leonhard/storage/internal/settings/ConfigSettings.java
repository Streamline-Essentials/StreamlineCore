package de.leonhard.storage.internal.settings;

public enum ConfigSettings {
  /**
   * Preserves the comments from the file.
   */
  PRESERVE_COMMENTS,
  /**
   * Preserves the comments if the file does not exist otherwise is skipped.
   */
  FIRST_TIME,
  /**
   * Skips the comments from the file.
   */
  SKIP_COMMENTS;

  public boolean isSorted() {
    return this == PRESERVE_COMMENTS || this == FIRST_TIME;
  }

  public boolean isUnsorted() {
    return this == SKIP_COMMENTS;
  }
}
