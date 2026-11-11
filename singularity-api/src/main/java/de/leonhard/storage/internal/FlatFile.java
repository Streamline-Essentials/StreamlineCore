package de.leonhard.storage.internal;

import de.leonhard.storage.internal.settings.ErrorHandler;
import de.leonhard.storage.annotation.ConfigPath;
import de.leonhard.storage.internal.provider.SimplixProviders;
import de.leonhard.storage.internal.settings.DataType;
import de.leonhard.storage.internal.settings.ReloadSettings;
import de.leonhard.storage.sections.FlatFileSection;
import de.leonhard.storage.util.FileUtils;
import de.leonhard.storage.util.Valid;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import lombok.*;
import org.jetbrains.annotations.Nullable;

@ToString
@EqualsAndHashCode
public abstract class FlatFile implements DataStorage, Comparable<FlatFile> {

  @Getter
  protected final File file;
  @Getter
  protected final FileType fileType;
  @Getter
  @Setter
  protected ReloadSettings reloadSettings = ReloadSettings.INTELLIGENT;
  @Getter
  @Setter
  protected ErrorHandler errorHandler = ErrorHandler.CLEAR;
  @Getter
  protected DataType dataType = DataType.UNSORTED;
  protected FileData fileData;
  @Nullable
  @Getter
  protected Consumer<FlatFile> reloadConsumer;
  @Nullable
  @Getter
  protected Consumer<FlatFile> errorConsumer;
  protected String[] pathPrefix;
  protected final String pathSeparator;
  @Getter
  private long lastLoaded;
  private boolean errorLock = false;

  protected FlatFile(
      @NonNull final String name,
      @Nullable final String path,
      @NonNull final FileType fileType,
      @Nullable final String pathSeparator,
      @Nullable final Consumer<FlatFile> reloadConsumer,
      @Nullable final Consumer<FlatFile> errorConsumer) {
    Valid.checkBoolean(!name.isEmpty(), "Name mustn't be empty");
    this.fileType = fileType;
    this.reloadConsumer = reloadConsumer;
    this.errorConsumer = errorConsumer;
    this.pathSeparator = Objects.requireNonNullElse(pathSeparator, ".");
    if (path == null || path.isEmpty()) {
      this.file = new File(FileUtils.replaceExtensions(name) + "." + fileType.getExtension());
    } else {
      final String fixedPath = path.replace("\\", "/");
      this.file = new File(
          fixedPath
          + File.separator
          + FileUtils.replaceExtensions(name)
          + "."
          + fileType.getExtension());
    }
  }
  protected FlatFile(
          @NonNull final String name,
          @Nullable final String path,
          @NonNull final FileType fileType,
          @Nullable final String pathSeparator,
          @Nullable final Consumer<FlatFile> reloadConsumer) {
    this(name, path, fileType, pathSeparator, reloadConsumer, null);
  }

  protected FlatFile(
      @NonNull final String name,
      @Nullable final String path,
      @NonNull final FileType fileType,
      @Nullable final Consumer<FlatFile> reloadConsumer) {
    this(name, path, fileType, null, reloadConsumer);
  }

  protected FlatFile(@NonNull final File file, @NonNull final FileType fileType, @Nullable String pathSeparator) {
    this.file = file;
    this.fileType = fileType;
    this.pathSeparator = Objects.requireNonNullElse(pathSeparator, ".");
    this.reloadConsumer = null;
    this.errorConsumer = null;
    Valid.checkBoolean(
        fileType == FileType.fromExtension(file),
        "Invalid file-extension for file type: '" + fileType + "'",
        "Extension: '" + FileUtils.getExtension(file) + "'");
  }

  protected FlatFile(@NonNull final File file, @NonNull final FileType fileType) {
    this(file, fileType, null);
  }

  /**
   * This constructor should only be used to store for example YAML-LIKE data in a .db file
   *
   * <p>Therefor no validation is possible. Might be unsafe.
   */
  protected FlatFile(@NonNull final File file, @Nullable String pathSeparator) {
    this.file = file;
    this.reloadConsumer = null;
    this.errorConsumer = null;
    this.pathSeparator = pathSeparator;
    // Might be null
    this.fileType = FileType.fromFile(file);
  }

  /**
   * This constructor should only be used to store for example YAML-LIKE data in a .db file
   *
   * <p>Therefor no validation is possible. Might be unsafe.
   */
  protected FlatFile(@NonNull final File file) {
    this(file, (String) null);
  }


  public void setPathPrefix(String pathPrefix) {
    setPathPrefix(splitPath(pathPrefix));
  }

  @SuppressWarnings("LombokSetterMayBeUsed")
  public void setPathPrefix(String[] pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  public String[] getPathPrefixArray() {
    return pathPrefix;
  }

  public String getPathPrefix() {
    return createPath(pathPrefix);
  }

  protected boolean shouldGetEmpty() {
    return errorLock && (errorHandler == ErrorHandler.EMPTY || (errorHandler == ErrorHandler.KEEP_OR_EMPTY && fileData == null));
  }

  protected boolean shouldSetEmpty() {
    return errorLock && (errorHandler == ErrorHandler.EMPTY || errorHandler == ErrorHandler.KEEP_OR_EMPTY);
  }

  // ----------------------------------------------------------------------------------------------------
  //  Creating our file
  // ----------------------------------------------------------------------------------------------------

  /**
   * Creates an empty .yml or .json file.
   *
   * @return true if file was created.
   */
  protected final boolean create() {
    return createFile(this.file);
  }

  private synchronized boolean createFile(final File file) {
    if (file.exists()) {
      this.lastLoaded = System.currentTimeMillis();
      return false;
    } else {
      FileUtils.getAndMake(file);
      this.lastLoaded = System.currentTimeMillis();
      return true;
    }
  }

  // ----------------------------------------------------------------------------------------------------
  // Abstract methods (Reading & Writing)
  // ----------------------------------------------------------------------------------------------------

  /**
   * Forces Re-read/load the content of our flat file Should be used to put the data from the file
   * to our FileData
   */
  protected abstract Map<String, Object> readToMap() throws IOException;

  /**
   * Write our data to file
   *
   * @param data Our data
   */
  protected abstract void write(final FileData data) throws IOException;

  protected void handleReloadException(final IOException ioException) {
    final String fileName = this.fileType == null
        ? "File"
        : this.fileType.name().toLowerCase(); // fileType might be null
    SimplixProviders.logger().sendError("Exception reloading " + fileName + " '" + getName() + "'");
    SimplixProviders.logger().sendError("In '" + FileUtils.getParentDirPath(this.file) + "'");
    SimplixProviders.logger().printStackTrace(ioException);
  }
  // ----------------------------------------------------------------------------------------------------
  // Overridden methods from DataStorage
  // ---------------------------------------------------------------------------------------------------->

  @Override
  public synchronized void setRaw(final String[] key, final Object value) {
    reloadIfNeeded();
    if (shouldSetEmpty()) {
      SimplixProviders.logger().sendWarning("Tried to set value to path '" + createPath(key) + "' but is lock by an error!");
      return;
    }
    final String[] finalKey = (this.pathPrefix == null) ? key : concatenatePath(this.pathPrefix, key);
    this.fileData.insert(finalKey, value);
    write();
  }

  @Nullable
  @Override
  public final Object get(final String[] key) {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return null;
    }
    final String[] finalKey = (this.pathPrefix == null) ? key : concatenatePath(this.pathPrefix, key);
    return getFileData().get(finalKey);
  }

  @Override
  public String pathSeparator() {
    return this.pathSeparator;
  }

  /**
   * Checks whether a key exists in the file
   *
   * @param key Key to check
   * @return Returned value
   */
  @Override
  public final boolean contains(final String[] key) {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return false;
    }
    final String[] finalKey = (this.pathPrefix == null) ? key : concatenatePath(this.pathPrefix, key);
    return this.fileData.containsKey(finalKey);
  }

  @Override
  public final Set<String> singleLayerKeySet() {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return Collections.emptySet();
    }
    return this.fileData.singleLayerKeySet();
  }

  @Override
  public final Set<String> singleLayerKeySet(final String[] key) {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return Collections.emptySet();
    }
    return this.fileData.singleLayerKeySet(key);
  }

  @Override
  public final Set<String> keySet() {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return Collections.emptySet();
    }
    return this.fileData.keySet();
  }

  @Override
  public final Set<String> keySet(final String key) {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return Collections.emptySet();
    }
    return this.fileData.keySet(key);
  }

  @Override
  public final Set<String> keySet(final String[] key) {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return Collections.emptySet();
    }
    return this.fileData.keySet(key);
  }

  @Override
  public final synchronized void remove(final String[] key) {
    reloadIfNeeded();
    if (shouldSetEmpty()) {
      SimplixProviders.logger().sendWarning("Tried to remove value from path '" + createPath(key) + "' but is lock by an error!");
      return;
    }
    this.fileData.remove(key);
    write();
  }

  // ----------------------------------------------------------------------------------------------------
  // More advanced & FlatFile specific operations to add data.
  // ----------------------------------------------------------------------------------------------------

  /**
   * Method to insert the data of a map to our FlatFile
   *
   * @param map Map to insert.
   */
  public final void putAll(final Map<String, Object> map) {
    if (shouldSetEmpty()) {
      SimplixProviders.logger().sendWarning("Tried to set values but is lock by an error!");
      return;
    }
    this.fileData.putAll(map);
    write();
  }
  public final void putAllRaw(final Map<String[], Object> map) {
    if (shouldSetEmpty()) {
      SimplixProviders.logger().sendWarning("Tried to set values but is lock by an error!");
      return;
    }
    this.fileData.putAllRaw(map);
    write();
  }

  /**
   * @return The data of our file as a {@link Map} object.
   */
  public final Map<String, Object> getData() {
    if (shouldGetEmpty()) {
      return Collections.emptyMap();
    }
    return getFileData().toMap();
  }

  // For performance separated from get(String key)
  public final List<Object> getAll(final String... keys) {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return Collections.emptyList();
    }
    final List<Object> result = new ArrayList<>();

    for (final String key : keys) {
      result.add(get(key));
    }

    return result;
  }

  public final List<Object> getAllRaw(final String[]... keys) {
    reloadIfNeeded();
    if (shouldGetEmpty()) {
      return Collections.emptyList();
    }
    final List<Object> result = new ArrayList<>();

    for (final String[] key : keys) {
      result.add(get(key));
    }

    return result;
  }

  public void removeAll(final String... keys) {
    if (shouldSetEmpty()) {
      SimplixProviders.logger().sendWarning("Tried to remove values but is lock by an error!");
      return;
    }
    for (final String key : keys) {
      this.fileData.remove(key);
    }
    write();
  }

  public void removeAllRaw(final String[]... keys) {
    if (shouldSetEmpty()) {
      SimplixProviders.logger().sendWarning("Tried to remove values but is lock by an error!");
      return;
    }
    for (final String[] key : keys) {
      this.fileData.remove(key);
    }
    write();
  }

  // ----------------------------------------------------------------------------------------------------
  // Pretty nice utility methods for FlatFile's
  // ----------------------------------------------------------------------------------------------------

  public final void addDefaultsFromMap(@NonNull final Map<String, Object> mapWithDefaults) {
    addDefaultsFromFileData(new FileData(mapWithDefaults, this.dataType));
  }

  public final void addDefaultsFromFileData(@NonNull final FileData newData) {
    reloadIfNeeded();
    if (shouldSetEmpty()) {
      SimplixProviders.logger().sendWarning("Tried to set values but is lock by an error!");
      return;
    }

    // Creating & setting defaults
    for (final String key : newData.keySet()) {
      if (!this.fileData.containsKey(key)) {
        this.fileData.insert(key, newData.get(key));
      }
    }

    write();
  }

  public final void addDefaultsFromFlatFile(@NonNull final FlatFile flatFile) {
    addDefaultsFromFileData(flatFile.getFileData());
  }

  public final String getName() {
    return this.file.getName();
  }

  public final String getFilePath() {
    return this.file.getAbsolutePath();
  }

  public synchronized void replace(
      final CharSequence target,
      final CharSequence replacement) throws IOException {
    if (shouldSetEmpty()) {
      SimplixProviders.logger().sendWarning("Tried to replace values but is lock by an error!");
      return;
    }
    final List<String> lines = Files.readAllLines(this.file.toPath());
    final List<String> result = new ArrayList<>();
    for (final String line : lines) {
      result.add(line.replace(target, replacement));
    }
    Files.write(this.file.toPath(), result);
  }

  public void write() {
    try {
      write(this.fileData);
    } catch (final IOException ex) {
      SimplixProviders.logger().sendError("Exception writing to file '" + getName() + "'");
      SimplixProviders.logger().sendError("In '" + FileUtils.getParentDirPath(this.file) + "'");
      SimplixProviders.logger().printStackTrace(ex);
    }
    this.lastLoaded = System.currentTimeMillis();
  }

  public final boolean hasChanged() {
    return FileUtils.hasChanged(this.file, this.lastLoaded);
  }

  public final void forceReload() {
    boolean shouldThrow = false;
    Map<String, Object> out = new HashMap<>();
    try {
      out = readToMap();
      errorLock = false;
    } catch (final IOException ex) {
      handleReloadException(ex);
      errorLock = true;
    } finally {
      final boolean ex = errorLock;
      boolean shouldModify = true;

      if (errorLock) {
        switch (errorHandler) {
          case KEEP_OR_EMPTY:
          case EMPTY:
            shouldModify = false; // disallows modify file data
            break;
          case KEEP_OR_CLEAR:
            if (fileData == null) {
              errorLock = false; // restores to clear
            } else {
              shouldModify = false; // disallows modify file data
            }
            break;
          case KEEP_OR_THROW:
            shouldModify = false; // disallows modifications
            if (fileData == null) shouldThrow = true; // throws if no data exists
            break;
          case ROLLBACK:
            if (fileData == null) {
              shouldThrow = true; // throws if no data exists
            } else {
              out = this.fileData.toMap(); // returns old config
              errorLock = false;
            }
            break;
          case CLEAR:
            errorLock = false; // restores to clear
            break;
        }
      }

      if (shouldModify) {
        if (this.fileData == null) {
          this.fileData = new FileData(out, this.dataType, pathSeparator());
        } else {
          this.fileData.loadData(out);
        }
      }

      this.lastLoaded = System.currentTimeMillis();
      if (ex && this.errorConsumer != null) {
        this.errorConsumer.accept(this);
      }
      if (shouldModify && this.reloadConsumer != null) {
        this.reloadConsumer.accept(this);
      }
    }
    if (shouldThrow) {
      throw new IllegalStateException("Exception caused because of the error handler requirements: "
              + this.errorHandler.name() + " (" + this.errorHandler.getReason() + ")");
    }
  }

  public final void clear() {
    this.fileData.clear();
    write();
  }

  public final void clearPathPrefix() {
    this.pathPrefix = null;
  }

  // ----------------------------------------------------------------------------------------------------
  // Internal stuff
  // ----------------------------------------------------------------------------------------------------

  protected final void reloadIfNeeded() {
    if (shouldReload()) {
      forceReload();
    }
  }

  // Should the file be re-read before the next get() operation?
  // Can be used as utility method for implementations of FlatFile
  protected boolean shouldReload() {
      switch (this.reloadSettings) {
        case AUTOMATICALLY:
          return true;
        case INTELLIGENT:
          return FileUtils.hasChanged(this.file, this.lastLoaded);
        default:
          return false;
      }
  }

  // ----------------------------------------------------------------------------------------------------
  // Misc
  // ----------------------------------------------------------------------------------------------------

  public final FileData getFileData() {
    Valid.notNull(this.fileData, "FileData mustn't be null");
    return this.fileData;
  }

  public final FlatFileSection getSection(final String[] pathPrefix) {
    return new FlatFileSection(this, pathPrefix);
  }

  public final FlatFileSection getSection(final String pathPrefix) {
    return getSection(splitPath(pathPrefix));
  }

  @Override
  public final int compareTo(@NonNull final FlatFile flatFile) {
    return this.file.compareTo(flatFile.file);
  }

  public void annotateClass(Object classInstance, String section) {
    this.annotateClass(classInstance, (s, field) -> section + "." + s);
  }

  public void annotateClass(Object classInstance) {
    this.annotateClass(classInstance, ((s, field) -> s));
  }

  public void annotateClass(Object classInstance, BiFunction<String, Field, String> elementSelector) {
    Class<?> clazz = classInstance.getClass();
    try {
      for (Field field : clazz.getFields()) {
        ConfigPath configPath = field.getAnnotation(ConfigPath.class);
        if(configPath != null) {
          field.setAccessible(true);
          field.set(classInstance, this.get(elementSelector.apply(configPath.value(), field), field.getType()));
        }
      }
    }catch (IllegalAccessException e) {
      throw SimplixProviders.exceptionHandler().create(e.getCause(), "Unable to set the value of fields in " + clazz.getName());
    }
  }
}
