package de.leonhard.storage.internal;

import de.leonhard.storage.internal.exceptions.SimplixValidationException;
import de.leonhard.storage.internal.provider.SimplixProviders;
import de.leonhard.storage.internal.serialize.SimplixSerializableLike;
import de.leonhard.storage.internal.serialize.SimplixSerializerManager;
import de.leonhard.storage.util.ClassWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface DataStorage {

  /**
   * Basic method to receive data from your data-structure
   *
   * @param key Key to search data for
   * @return Object in data-structure. Null if nothing was found!
   */
  @Nullable
  Object get(final String[] key);

  /**
   * Checks whether a key exists in the data-structure
   *
   * @param key Key to check
   * @return Returned value.
   */
  boolean contains(final String[] key);

  /**
   * @return The separator for this file.
   */
  String pathSeparator();

  /**
   * Set an object to your data-structure, if the object is instance of a {@link SimplixSerializableLike} then
   * it will be deserialized as their method calls.
   *
   * @param key   The key your value should be associated with
   * @param value The value you want to set in your data-structure.
   */
  default void set(final String[] key, final Object value) {
    set(key, value, true);
  }

  /**
   * Set an object to your data-structure, if parse is set true, the object will be auto deserialized, and their
   * children if applicable, otherwise if the object is instance of a {@link SimplixSerializableLike} then
   * it will be deserialized as their method calls.
   *
   * @param key   The key your value should be associated with
   * @param value The value you want to set in your data-structure.
   * @param sub   If applicable, true if all the content should be parsed, false otherwise to only parse the first layer, null to ignore contents.
   * @see SimplixSerializerManager#resolveSingle(Object)
   * @see SimplixSerializerManager#resolveAll(Object)
   * @see SimplixSerializableLike#serialized()
   */
  default void set(final String[] key, final Object value, @Nullable Boolean sub) {
    if (sub != null) {
      if (sub) setRaw(key, SimplixSerializerManager.resolveAll(value));
      else setRaw(key, SimplixSerializerManager.resolveSingle(value));
    } else if (value instanceof SimplixSerializableLike) {
      SimplixSerializableLike s = (SimplixSerializableLike) value;
      setRaw(key, s.serialized());
    } else {
      setRaw(key, value);
    }
  }

  /**
   * Set an object to your data-structure
   *
   * @param key   The key your value should be associated with
   * @param value The value you want to set in your data-structure.
   */
  void setRaw(final String[] key, final Object value);

  Set<String> singleLayerKeySet();

  Set<String> keySet();

  Set<String> keySet(final String[] key);

  Set<String> singleLayerKeySet(final String[] key);

  void remove(final String[] key);

  /**
   * Basic method to receive data from your data-structure
   *
   * @param key Key to search data for
   * @return Object in data-structure. Null if nothing was found!
   */
  @Nullable
  default Object get(final String key) {
    return get(splitPath(key));
  }

  /**
   * Checks whether a key exists in the data-structure
   *
   * @param key Key to check
   * @return Returned value.
   */
  default boolean contains(final String key) {
    return contains(splitPath(key));
  }


  // ----------------------------------------------------------------------------------------------------
  // Path Specific Methods
  // ----------------------------------------------------------------------------------------------------

  /**
   * @return A string array split by the separator of this file.
   */
  default String[] splitPath(String path) {
    return path.split(Pattern.quote(pathSeparator()));
  }

  /**
   * Creates a path for this file with the path separator defined for this file.
   */
  default String createPath(@NotNull final String first, @NotNull final String... other) {
    if (other.length == 0) return first;
    StringBuilder sb = new StringBuilder(first);
    for (String o : other) {
      sb.append(pathSeparator()).append(o);
    }
    return sb.toString();
  }

  /**
   * Creates a path for this file with the path separator defined for this file.
   */
  default String createPath(String[] path) {
    StringBuilder sb = null;
    for (String o : path) {
      if (sb == null) {
        sb = new StringBuilder(o);
      } else
        sb.append(pathSeparator()).append(o);
    }
    return sb == null ? "" : sb.toString();
  }

  /**
   * @return A string array split by the separator of this file.
   */
  default String[] concatenatePath(String[] first, String[] second) {
    return Stream.concat(Arrays.stream(first), Arrays.stream(second)).toArray(String[]::new);
  }

  /**
   * Set an object to your data-structure
   *
   * @param key   The key your value should be associated with
   * @param value The value you want to set in your data-structure.
   */
  default void set(final String key, final Object value) {
    set(splitPath(key), value);
  }

  default Set<String> singleLayerKeySet(final String key) {
    return singleLayerKeySet(splitPath(key));
  }

  default Set<String> keySet(final String key) {
    return keySet(splitPath(key));
  }

  default void remove(final String key) {
    remove(splitPath(key));
  }

  // ----------------------------------------------------------------------------------------------------
  //
  // Default-Implementations
  //
  // ----------------------------------------------------------------------------------------------------

  /**
   * Get a value or a default one
   *
   * @param key Path to value in data-structure
   * @param def Default value and type of it
   * @see #getOrDefault(String[], Object)
   * @see #getRaw(String[], Object)
   * @see #getOrThrow(String[], Object)
   */
  default <T> T get(final String[] key, final T def) {
    return getOrDefault(key, def);
  }

  /**
   * Get a value or a default one
   *
   * @param key Path to value in data-structure
   * @param def Default value and type of it
   * @see #getOrDefault(String, Object)
   * @see #getRaw(String, Object)
   * @see #getOrThrow(String, Object)
   */
  default <T> T get(final String key, final T def) {
    return getOrDefault(key, def);
  }

  /**
   * Get a value or throws an exception if it does not exist or another error occurs.
   *
   * @param key Path to value in data-structure
   * @param def Default value and type of it
   * @throws SimplixValidationException if the returned value can not be found, cast or another error occurs
   * @see #getOrDefault(String[], Object)
   * @see #getRaw(String[], Object)
   */
  @NotNull
  default <T> T getOrThrow(final String[] key, final T def) {
    try {
      final Object raw = Objects.requireNonNull(get(key));
      return ClassWrapper.getFromDef(raw, def);
    } catch (ClassCastException e) {
      throw new SimplixValidationException(e, "Cannot cast the value to the given type for key '" + createPath(key) + "'");
    } catch (NullPointerException e) {
      throw new SimplixValidationException(e, "Cannot found a value for key '" + createPath(key) + "'");
    }catch (Exception e) {
      throw new SimplixValidationException(e, "An error occurred while getting the key '" + createPath(key) + "'");
    }
  }

  /**
   * Get a value or throws an exception if it does not exist or another error occurs.
   *
   * @param key Path to value in data-structure
   * @param def Default value and type of it
   * @throws SimplixValidationException if the returned value can not be found, cast or another error occurs
   * @see #getOrDefault(String[], Object)
   * @see #getRaw(String[], Object)
   */
  @NotNull
  default <T> T getOrThrow(final String key, final T def) {
    return getOrThrow(splitPath(key), def);
  }

  /**
   * Get a value or throws an exception if it does not exist or another error occurs.
   *
   * @param key Path to value in data-structure
   * @param type Type of it
   * @throws SimplixValidationException if the returned value can not be found, cast or another error occurs
   * @see #getOrDefault(String[], Object)
   * @see #getRaw(String[], Object)
   */
  @NotNull
  default <T> T getOrThrow(final String[] key, final Class<T> type) {
    try {
      final Object raw = Objects.requireNonNull(get(key));
      return ClassWrapper.getFromDef(raw, type);
    } catch (ClassCastException e) {
      throw new SimplixValidationException(e, "Cannot cast the value to the given type for key '" + createPath(key) + "'");
    } catch (NullPointerException e) {
      throw new SimplixValidationException(e, "Cannot found a value for key '" + createPath(key) + "'");
    }catch (Exception e) {
      throw new SimplixValidationException(e, "An error occurred while getting the key '" + createPath(key) + "'");
    }
  }

  /**
   * Get a value or throws an exception if it does not exist or another error occurs.
   *
   * @param key Path to value in data-structure
   * @param type Type of it
   * @throws SimplixValidationException if the returned value can not be found, cast or another error occurs
   * @see #getOrDefault(String[], Object)
   * @see #getRaw(String[], Object)
   */
  @NotNull
  default <T> T getOrThrow(final String key, final Class<T> type) {
    return getOrThrow(splitPath(key), type);
  }

  /**
   * Get a value or null if no present
   *
   * @param key  Path to value in data-structure
   * @param type Type of it
   * @see #get(String[], Object)
   * @see #getOrDefault(String[], Object)
   */
  @Nullable
  default <T> T getRaw(final String[] key, final T type) {
    final Object raw = get(key);
    return raw == null ? null : ClassWrapper.getFromDef(raw, type);
  }

  /**
   * Get a value or null if no present
   *
   * @param key  Path to value in data-structure
   * @param type Type of it
   * @see #get(String, Object)
   * @see #getOrDefault(String, Object)
   */
  @Nullable
  default <T> T getRaw(final String key, final T type) {
    return getRaw(splitPath(key), type);
  }

  /**
   * Get a value or null if no present
   *
   * @param key Path to value in data-structure
   * @param type Type of it
   * @see #get(String[], Object)
   * @see #getOrDefault(String[], Object)
   * @see #getRawOrThrow(String[], Class)
   */
  @Nullable
  default <T> T getRaw(final String[] key, final Class<T> type) {
    final Object raw = get(key);
    return raw == null ? null : ClassWrapper.getFromDef(raw, type);
  }

  /**
   * Get a value or null if no present
   *
   * @param key Path to value in data-structure
   * @param type Type of it
   * @see #get(String, Object)
   * @see #getOrDefault(String, Object)
   * @see #getRawOrThrow(String, Class)
   */
  @Nullable
  default <T> T getRaw(final String key, final Class<T> type) {
    return getRaw(splitPath(key), type);
  }

  /**
   * Get a value or null if no present, generates an exception if an error occurs
   *
   * @param key Path to value in data-structure
   * @param type Type of it
   * @throws SimplixValidationException if the returned value can not be cast or another error occurs
   * @see #get(String[], Object)
   * @see #getOrDefault(String[], Object)
   * @see #getRawOrThrow(String[], Object)
   */
  @Nullable
  default <T> T getRawOrThrow(final String[] key, final T type) {
    try {
      return getRaw(key, type);
    } catch (ClassCastException e) {
      throw new SimplixValidationException(e, "Cannot cast the value to the given type for key '" + createPath(key) + "'");
    } catch (Exception e) {
      throw new SimplixValidationException(e, "An error occurred while getting the key '" + createPath(key) + "'");
    }
  }

  /**
   * Get a value or null if no present, generates an exception if an error occurs
   *
   * @param key Path to value in data-structure
   * @param type Type of it
   * @throws SimplixValidationException if the returned value can not be cast or another error occurs
   * @see #get(String, Object)
   * @see #getOrDefault(String, Object)
   * @see #getRawOrThrow(String, Object)
   */
  @Nullable
  default <T> T getRawOrThrow(final String key, final T type) {
    return getRawOrThrow(splitPath(key), type);
  }

  /**
   * Get a value or null if no present, generates an exception if an error occurs
   *
   * @param key Path to value in data-structure
   * @param type Type of it
   * @throws SimplixValidationException if the returned value can not be cast or another error occurs
   * @see #get(String[], Object)
   * @see #getOrDefault(String[], Object)
   */
  @Nullable
  default <T> T getRawOrThrow(final String[] key, final Class<T> type) {
    try {
      return getRaw(key, type);
    } catch (ClassCastException e) {
      throw new SimplixValidationException(e, "Cannot cast the value to the given type for key '" + createPath(key) + "'");
    } catch (Exception e) {
      throw new SimplixValidationException(e, "An error occurred while getting the key '" + createPath(key) + "'");
    }
  }

  /**
   * Get a value or null if no present, generates an exception if an error occurs
   *
   * @param key Path to value in data-structure
   * @param type Type of it
   * @throws SimplixValidationException if the returned value can not be cast or another error occurs
   * @see #get(String, Object)
   * @see #getOrDefault(String, Object)
   */
  @Nullable
  default <T> T getRawOrThrow(final String key, final Class<T> type) {
    return getRawOrThrow(splitPath(key), type);
  }

  /**
   * Method to get a value of a predefined type from our data structure will return {@link
   * Optional#empty()} if the value wasn't found.
   *
   * @param key  Key to search the value for
   */
  default Optional<Object> findRaw(final String[] key) {
    return Optional.ofNullable(get(key));
  }

  /**
   * Method to get a value of a predefined type from our data structure will return {@link
   * Optional#empty()} if the value wasn't found.
   *
   * @param key  Key to search the value for
   * @see #getOrThrow(String, Object)
   */
  default Optional<Object> findRaw(final String key) {
    return findRaw(splitPath(key));
  }

  /**
   * Method to get a value of a predefined type from our data structure will return {@link
   * Optional#empty()} if the value wasn't found.
   *
   * @param key  Key to search the value for
   * @param type Type of the value
   * @see #getOrThrow(String[], Object)
   */
  default <T> Optional<T> find(final String[] key, final Class<T> type) {
    return Optional.ofNullable(getRaw(key, type));
  }

  /**
   * Method to get a value of a predefined type from our data structure will return {@link
   * Optional#empty()} if the value wasn't found.
   *
   * @param key  Key to search the value for
   * @param type Type of the value
   * @see #getOrThrow(String, Object)
   */
  default <T> Optional<T> find(final String key, final Class<T> type) {
    return find(splitPath(key), type);
  }

  /**
   * Method to get a value of a predefined type from our data structure will return {@link
   * Optional#empty()} if the value wasn't found.<br>
   * The type is only used to get the class.
   *
   * @param key  Key to search the value for
   * @param type Type of the value
   * @see #getOrThrow(String[], Object)
   */
  default <T> Optional<T> find(final String[] key, final T type) {
    return Optional.ofNullable(getRaw(key, type));
  }

  /**
   * Method to get a value of a predefined type from our data structure will return {@link
   * Optional#empty()} if the value wasn't found.<br>
   * The type is only used to get the class.
   *
   * @param key  Key to search the value for
   * @param type Type of the value
   * @see #getOrThrow(String, Object)
   */
  default <T> Optional<T> find(final String key, final T type) {
    return find(splitPath(key), type);
  }

  // ----------------------------------------------------------------------------------------------------
  // Predefined getter for Strings and primitive types from data-structure
  // ----------------------------------------------------------------------------------------------------

  /**
   * Get a String from a data-structure
   *
   * @param key Path to String in data-structure
   * @return Returns the value
   */
  @Nullable
  default String getRawString(final String[] key) {
    return getRaw(key, "");
  }

  /**
   * Get a String from a data-structure
   *
   * @param key Path to String in data-structure
   * @return Returns the value
   */
  @Nullable
  default String getRawString(final String key) {
    return getRaw(key, "");
  }

  /**
   * Get a String from a data-structure
   *
   * @param key Path to String in data-structure
   * @return Returns the value
   */
  @NotNull
  default String getString(final String[] key) {
    return getOrDefault(key, "");
  }

  /**
   * Get a String from a data-structure
   *
   * @param key Path to String in data-structure
   * @return Returns the value
   */
  default String getString(final String key) {
    return getOrDefault(key, "");
  }

  /**
   * Gets a long from a data-structure by key
   *
   * @param key Path to long in data-structure
   * @return String from data-structure
   */
  default long getLong(final String key) {
    return getOrDefault(key, 0L);
  }

  /**
   * Gets an int from a data-structure
   *
   * @param key Path to int in data-structure
   * @return Int from data-structure
   */
  default int getInt(final String key) {
    return getOrDefault(key, 0);
  }

  /**
   * Get a byte from a data-structure
   *
   * @param key Path to byte in data-structure
   * @return Byte from data-structure
   */
  default byte getByte(final String key) {
    return getOrDefault(key, (byte) 0);
  }

  /**
   * Get a boolean from a data-structure
   *
   * @param key Path to boolean in data-structure
   * @return Boolean from data-structure
   */
  default boolean getBoolean(final String key) {
    return getOrDefault(key, false);
  }

  /**
   * Get a float from a data-structure
   *
   * @param key Path to float in data-structure
   * @return Float from data-structure
   */
  default float getFloat(final String key) {
    return getOrDefault(key, 0F);
  }

  /**
   * Get a double from a data-structure
   *
   * @param key Path to double in the data-structure
   * @return Double from data-structure
   */
  default double getDouble(final String key) {
    return getOrDefault(key, 0D);
  }

  // ----------------------------------------------------------------------------------------------------
  // Getting Lists and non-ClassWrapper types from data-structure
  // ----------------------------------------------------------------------------------------------------

  /**
   * Get a List from a data-structure
   *
   * @param key Path to List in data structure.
   */
  @NotNull
  default List<?> getList(final String key) {
    return getOrDefault(key, new ArrayList<>());
  }
  /**
   * Get a List from a data-structure
   *
   * @param key Path to List in data structure.
   */
  @NotNull
  default List<?> getList(final String[] key) {
    return getOrDefault(key, new ArrayList<>());
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   * @param def  The default value if it does not exist.
   */
  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> List<T> getList(final String[] key, final T type, @Nullable List<T> def) {
    final Object raw = get(key);
    if (raw instanceof List) {
      return ClassWrapper.getListFromType((List) raw, type);
    }
    return def;
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   * @param def  The default value if it does not exist.
   */
  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> List<T> getList(final String key, final T type, @Nullable List<T> def) {
    return getList(splitPath(key), type, def);
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   */
  @NotNull
  default <T> List<T> getList(final String[] key, final T type) {
    return getList(key, type, new ArrayList<>());
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   */
  @NotNull
  default <T> List<T> getList(final String key, final T type) {
    return getList(splitPath(key), type);
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   * @param def  The default value if it does not exist.
   */
  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> List<T> getList(final String[] key, final Class<T> type, @Nullable List<T> def) {
    final Object raw = get(key);
    if (raw instanceof List) {
      return ClassWrapper.getListFromType((List) raw, type);
    }
    return def;
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   * @param def  The default value if it does not exist.
   */
  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> List<T> getList(final String key, final Class<T> type, @Nullable List<T> def) {
    return getList(splitPath(key), type, def);
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   */
  @NotNull
  default <T> List<T> getList(final String[] key, final Class<T> type) {
    return getList(key, type, new ArrayList<T>());
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   */
  @NotNull
  default <T> List<T> getList(final String key, final Class<T> type) {
    return getList(splitPath(key), type);
  }

  /**
   * Get a List from a data-structure, casting the content to the type, filtering out values that do not match.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   * @param def  The default value if it does not exist.
   */
  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> List<T> getListFiltered(final String[] key, final T type, @Nullable List<T> def) {
    final Object raw = get(key);
    if (raw instanceof List) {
      return ClassWrapper.getListFromTypeFilter((List) raw, type);
    }
    return def;
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   */
  default <T> List<T> getListFiltered(final String[] key, final T type) {
    return getListFiltered(key, type, new ArrayList<>());
  }

  /**
   * Get a List from a data-structure, casting the content to the type, filtering out values that do not match.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   * @param def  The default value if it does not exist.
   */
  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> List<T> getListFiltered(final String[] key, final Class<T> type, @Nullable List<T> def) {
    final Object raw = get(key);
    if (raw instanceof List) {
      return ClassWrapper.getListFromTypeFilter((List) raw, type);
    }
    return def;
  }

  /**
   * Get a List from a data-structure, casting the content to the type.
   *
   * @param key  Path to List in data structure.
   * @param type Type of the content.
   */
  default <T> List<T> getListFiltered(final String[] key, final Class<T> type) {
    return getListFiltered(key, type, new ArrayList<T>());
  }

  /**
   * Get a List from a data-structure and use a mapper for their content.
   *
   * @param key    Path to List in data structure.
   * @param mapper Mapper to parse the list content.
   */
  @NotNull
  default <T> List<T> getMappedList(final String key, final Function<Object, T> mapper) {
    return getMappedList(splitPath(key), mapper);
  }

  /**
   * Get a List from a data-structure and use a mapper for their content.
   *
   * @param key    Path to List in data structure.
   * @param mapper Mapper to parse the list content.
   */
  @NotNull
  default <T> List<T> getMappedList(final String[] key, final Function<Object, T> mapper) {
    final Object raw = get(key);
    return raw == null ? new ArrayList<>() : ClassWrapper.getFromDef(raw, new ArrayList<>()).stream().map(mapper).collect(Collectors.toList());
  }

  /**
   * Get a List from a data-structure and use a mapper for their content.<br>
   * Also filters the null results.
   *
   * @param key    Path to List in data structure.
   * @param mapper Mapper to parse the list content.
   */
  @NotNull
  default <T> List<T> getMappedListFiltered(final String key, final Function<Object, T> mapper) {
    return getMappedListFiltered(splitPath(key), mapper);
  }

  /**
   * Get a List from a data-structure and use a mapper for their content.<br>
   * Also filters the null results.
   *
   * @param key    Path to List in data structure.
   * @param mapper Mapper to parse the list content.
   */
  @NotNull
  default <T> List<T> getMappedListFiltered(final String[] key, final Function<Object, T> mapper) {
    final Object raw = get(key);
    return raw == null ? new ArrayList<>() : ClassWrapper.getFromDef(raw, new ArrayList<>()).stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * Get a List from a data-structure and use a mapper for simple values.<br>
   * This is for simple values, for complex implementations use a custom serializer instead.
   *
   * @param key    Path to List in data structure.
   * @param mapper Mapper to parse the list content.
   */
  @NotNull
  default <T> List<T> getStringMappedList(final String key, final Function<String, T> mapper) {
    return getStringMappedList(splitPath(key), mapper);
  }

  /**
   * Get a List from a data-structure and use a mapper for simple values.<br>
   * This is for simple values, for complex implementations use a custom serializer instead.
   *
   * @param key    Path to List in data structure.
   * @param mapper Mapper to parse the list content.
   */
  @NotNull
  default <T> List<T> getStringMappedList(final String[] key, final Function<String, T> mapper) {
    final Object raw = get(key);
    return raw == null ? new ArrayList<>() : ClassWrapper.getFromDef(raw, new ArrayList<String>()).stream().map(mapper).collect(Collectors.toList());
  }

  /**
   * Get a List from a data-structure and use a mapper for simple values.<br>
   * Also filters the null results.<br>
   * This is for simple values, for complex implementations use a custom serializer instead.
   *
   * @param key    Path to List in data structure.
   * @param mapper Mapper to parse the list content.
   */
  @NotNull
  default <T> List<T> getStringMappedListFiltered(final String key, final Function<String, T> mapper) {
    return getStringMappedListFiltered(splitPath(key), mapper);
  }

  /**
   * Get a List from a data-structure and use a mapper for simple values.<br>
   * Also filters the null results.<br>
   * This is for simple values, for complex implementations use a custom serializer instead.
   *
   * @param key    Path to List in data structure.
   * @param mapper Mapper to parse the list content.
   */
  @NotNull
  default <T> List<T> getStringMappedListFiltered(final String[] key, final Function<String, T> mapper) {
    final Object raw = get(key);
    return raw == null ? new ArrayList<>() : ClassWrapper.getFromDef(raw, new ArrayList<String>()).stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * Attempts to get a List of the given type
   * @param key Path to List in data structure.
   */
  @NotNull
  default <T> List<T> getListParameterized(final String key) {
    return getOrSetDefault(key, new ArrayList<>());
  }

  @NotNull
  default List<String> getStringList(final String key) {
    return getOrDefault(key, new ArrayList<>());
  }

  @NotNull
  default List<String> getStringList(final String[] key) {
    return getOrDefault(key, new ArrayList<>());
  }

  @NotNull
  default List<Integer> getIntegerList(final String key) {
    return getOrDefault(key, new ArrayList<String>()).stream().map(Integer::parseInt).collect(Collectors.toList());
  }

  @NotNull
  default List<Byte> getByteList(final String key) {
    return getOrDefault(key, new ArrayList<String>()).stream().map(Byte::parseByte).collect(Collectors.toList());
  }

  @NotNull
  default List<Long> getLongList(final String key) {
    return getOrDefault(key, new ArrayList<String>()).stream().map(Long::parseLong).collect(Collectors.toList());
  }

  @NotNull
  default Map<?, ?> getMap(final String[] key) {
    return getOrDefault(key, new HashMap<>());
  }

  @NotNull
  default Map<?, ?> getMap(final String key) {
    return getMap(splitPath(key));
  }

  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> Map<String, T> getMap(final String[] key, Function<Object, T> mapper, @Nullable Map<String, T> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromType((Map<String, ?>) raw, mapper);
    }
    return def;
  }

  /**
   * Get a Map from a data-structure, casting the content to the type.
   *
   * @param key  Path to Map in data structure.
   * @param type Type of the values.
   * @param def  The default value to use if it does not exist.
   */
  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> Map<String, T> getMap(final String[] key, T type, @Nullable Map<String, T> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromType((Map<String, ?>) raw, type);
    }
    return def;
  }

  /**
   * Get a Map from a data-structure, casting the content to the type.
   *
   * @param key  Path to Map in data structure.
   * @param type Class of the values.
   * @param def  The default value to use if it does not exist.
   */
  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> Map<String, T> getMap(final String[] key, Class<T> type, @Nullable Map<String, T> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromType((Map<String, ?>) raw, type);
    }
    return def;
  }

  /**
   * Get a Map from a data-structure, casting the content to the type, using a custom mapper for the key values.
   * @param key       Path to Map in data structure.
   * @param keyType   Mapper for the key values
   * @param valueType Type of the content.
   * @param def       The default value if it does not exist.
   */
  @Nullable
  @Contract("_, _, _, !null -> !null")
  default <K, V> Map<K, V> getMap(final String[] key, K keyType, V valueType, @Nullable Map<K, V> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromType((Map<String, ?>) raw, keyType, valueType);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, _, !null -> !null")
  default <K, V> Map<K, V> getMap(final String[] key, Class<K> keyType, Class<V> valueType, @Nullable Map<K, V> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromType((Map<String, ?>) raw, keyType, valueType);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, _, !null -> !null")
  default <K, V> Map<K, V> getMap(final String[] key, Function<Object, K> keyType, Function<Object, V> valueType, @Nullable Map<K, V> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeMapper((Map<String, ?>) raw, keyType, valueType);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, _, !null -> !null")
  default <K, V> Map<K, V> getMapStr(final String[] key, Function<String, K> keyType, Function<Object, V> valueType, @Nullable Map<K, V> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeMapperStr((Map<String, ?>) raw, keyType, valueType);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> Map<String, T> getMapFiltered(final String[] key, T type, @Nullable Map<String, T> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeFilter((Map<String, ?>) raw, type);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> Map<String, T> getMapFiltered(final String[] key, Class<T> type, @Nullable Map<String, T> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeFilter((Map<String, ?>) raw, type);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, _, !null -> !null")
  default <K, V> Map<K, V> getMapFiltered(final String[] key, K keyType, V valueType, @Nullable Map<K, V> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeFilter((Map<String, ?>) raw, keyType, valueType);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, _, !null -> !null")
  default <K, V> Map<K, V> getMapFiltered(final String[] key, Class<K> keyType, Class<V> valueType, @Nullable Map<K, V> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeFilter((Map<String, ?>) raw, keyType, valueType);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, !null -> !null")
  default <T> Map<String, T> getMapFiltered(final String[] key, Function<Object, T> type, @Nullable Map<String, T> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeFilter((Map<String, ?>) raw, type);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, _, !null -> !null")
  default <K, V> Map<K, V> getMapFiltered(final String[] key, Function<Object, K> keyType, Function<Object, V> valueType, @Nullable Map<K, V> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeFilterMapper((Map<String, ?>) raw, keyType, valueType);
    }
    return def;
  }

  @Nullable
  @Contract("_, _, _, !null -> !null")
  default <K, V> Map<K, V> getMapFilteredStr(final String[] key, Function<String, K> keyType, Function<Object, V> valueType, @Nullable Map<K, V> def) {
    final Object raw = get(key);
    if (raw instanceof Map) {
      return ClassWrapper.getMapFromTypeFilterMapperStr((Map<String, ?>) raw, keyType, valueType);
    }
    return def;
  }

  /**
   * Attempts to get a map of the given type
   * @param key Path to the Map in the data-structure
   */
  @NotNull
  default <K, V> Map<K, V> getMapParameterized(final String key) {
    return getOrSetDefault(key, new HashMap<>());
  }

  // ----------------------------------------------------------------------------------------------------
  // Enum Specific Methods
  // ----------------------------------------------------------------------------------------------------

  /**
   * Enum list getting the value
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumList(
          final String[] key,
          final Class<E> type) {
    return getStringMappedList(key, (s) -> Enum.valueOf(type, s));
  }

  /**
   * Enum list getting the value
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumList(
          final String key,
          final Class<E> type) {
    return getStringMappedList(key, s -> Enum.valueOf(type, s));
  }

  /**
   * Enum list mapping the base value
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumList(
          final String[] key,
          final Class<E> type,
          final Function<String, String> mapper) {
    return getStringMappedList(key, s -> Enum.valueOf(type, mapper.apply(s)));
  }

  /**
   * Enum list mapping the base value
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumList(
          final String key,
          final Class<E> type,
          final Function<String, String> mapper) {
    return getStringMappedList(key, s -> Enum.valueOf(type, mapper.apply(s)));
  }

  /**
   * Enum list mapping directly to the value
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumList(
          final String[] key,
          final Function<String, E> mapper) {
    return getStringMappedList(key, mapper);
  }

  /**
   * Enum list mapping directly to the value
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumList(
          final String key,
          final Function<String, E> mapper) {
    return getStringMappedList(key, mapper);
  }

  /**
   * Enum list getting the value<br>
   * Filters incorrect values
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumListFiltered(
          final String[] key,
          final Class<E> type) {
    return getStringMappedListFiltered(key, s -> Enum.valueOf(type, s));
  }

  /**
   * Enum list getting the value<br>
   * Filters incorrect values
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumListFiltered(
          final String key,
          final Class<E> type) {
    return getStringMappedListFiltered(key, s -> Enum.valueOf(type, s));
  }

  /**
   * Enum list mapping the base value<br>
   * Filters incorrect values
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumListFiltered(
          final String[] key,
          final Class<E> type,
          final Function<String, String> mapper) {
    return getStringMappedListFiltered(key, s -> Enum.valueOf(type, mapper.apply(s)));
  }

  /**
   * Enum list mapping the base value<br>
   * Filters incorrect values
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumListFiltered(
          final String key,
          final Class<E> type,
          final Function<String, String> mapper) {
    return getStringMappedListFiltered(key, s -> Enum.valueOf(type, mapper.apply(s)));
  }

  /**
   * Enum list mapping directly to the value<br>
   * Filters incorrect values
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumListFiltered(
          final String[] key,
          final Function<String, E> mapper) {
    return getStringMappedListFiltered(key, mapper);
  }


  /**
   * Enum list mapping directly to the value<br>
   * Filters incorrect values
   */
  @NotNull
  default <E extends Enum<E>> List<E> getEnumListFiltered(
          final String key,
          final Function<String, E> mapper) {
    return getStringMappedListFiltered(key, mapper);
  }

  /**
   * Internal Method for enums
   */
  private String stringForEnum(String[] key) {
    String value;
    try {
      value = getRaw(key, String.class);
    } catch (ClassCastException e) {
      throw new SimplixValidationException(e, "No usable Enum-Value found for '" + createPath(key) + "'.");
    } catch (Exception e) {
      throw new SimplixValidationException(e, "An error occurred while getting the key '" + createPath(key) + "'");
    }
    if (value == null)
      throw new SimplixValidationException("Null Enum-Value was found for '" + createPath(key) + "'.");
    return value;
  }

  /**
   * Serialize an Enum from entry in the data-structure
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param <E>      EnumType
   * @return Serialized
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   */
  @NotNull
  default <E extends Enum<E>> E getEnum(
          final String[] key,
          final Class<E> enumType) {
    final String value = stringForEnum(key);
    return Enum.valueOf(enumType, value);
  }

  /**
   * Serialize an Enum from entry in the data-structure
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param <E>      EnumType
   * @return Serialized
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   */
  @NotNull
  default <E extends Enum<E>> E getEnum(
          final String key,
          final Class<E> enumType) {
    return getEnum(splitPath(key), enumType);
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses an extra mapper to skip simple scenarios. (like capitalization)
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param mapper   Mapper for some simple scenarios
   * @param <E>      EnumType
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   * @return Serialized Enum
   */
  @NotNull
  default <E extends Enum<E>> E getEnum(
          final String[] key,
          final Class<E> enumType,
          final Function<String, String> mapper) {
    final String value = stringForEnum(key);
    return Enum.valueOf(enumType, mapper.apply(value));
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses an extra mapper to skip simple scenarios. (like capitalization)
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param mapper   Mapper for some simple scenarios
   * @param <E>      EnumType
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   * @return Serialized Enum
   */
  @NotNull
  default <E extends Enum<E>> E getEnum(
          final String key,
          final Class<E> enumType,
          final Function<String, String> mapper) {
    return getEnum(splitPath(key), enumType, mapper);
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses a mapper to get the enum.
   *
   * @param key         Path to Enum
   * @param mapper Transformer to enum
   * @param <E>         EnumType
   * @throws SimplixValidationException if an internal error occurs
   * @return Serialized Enum
   */
  @NotNull
  default <E extends Enum<E>> E getEnum(
          final String[] key,
          final Function<String, E> mapper) {
    final String value = stringForEnum(key);
    return mapper.apply(value);
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses a mapper to get the enum.
   *
   * @param key         Path to Enum
   * @param mapper Transformer to enum
   * @param <E>         EnumType
   * @throws SimplixValidationException if an internal error occurs
   * @return Serialized Enum
   */
  @NotNull
  default <E extends Enum<E>> E getEnum(
          final String key,
          final Function<String, E> mapper) {
    return getEnum(splitPath(key), mapper);
  }

  /**
   * Serialize an Enum from entry in the data-structure
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param <E>      EnumType
   * @return Serialized
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   */
  @Nullable
  default <E extends Enum<E>> E getRawEnum(
          final String[] key,
          final Class<E> enumType) {
    if (!contains(key)) return null;
    final String value = stringForEnum(key);
    return Enum.valueOf(enumType, value);
  }

  /**
   * Serialize an Enum from entry in the data-structure
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param <E>      EnumType
   * @return Serialized
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   */
  @Nullable
  default <E extends Enum<E>> E getRawEnum(
          final String key,
          final Class<E> enumType) {
    return getRaw(splitPath(key), enumType);
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses an extra mapper to skip simple scenarios. (like capitalization)
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param mapper   Mapper for some simple scenarios
   * @param <E>      EnumType
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   * @return Serialized Enum
   */
  @Nullable
  default <E extends Enum<E>> E getRawEnum(
          final String[] key,
          final Class<E> enumType,
          final Function<String, String> mapper) {
    if (!contains(key)) return null;
    final String value = stringForEnum(key);
    return Enum.valueOf(enumType, mapper.apply(value));
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses an extra mapper to skip simple scenarios. (like capitalization)
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param mapper   Mapper for some simple scenarios
   * @param <E>      EnumType
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   * @return Serialized Enum
   */
  @Nullable
  default <E extends Enum<E>> E getRawEnum(
          final String key,
          final Class<E> enumType,
          final Function<String, String> mapper) {
    return getRawEnum(splitPath(key), enumType, mapper);
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses a mapper to get the enum.
   *
   * @param key         Path to Enum
   * @param mapper Transformer to enum
   * @param <E>         EnumType
   * @throws SimplixValidationException if an internal error occurs
   * @return Serialized Enum
   */
  @Nullable
  default <E extends Enum<E>> E getRawEnum(
          final String[] key,
          final Function<String, E> mapper) {
    if (!contains(key)) return null;
    final String value = stringForEnum(key);
    return mapper.apply(value);
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses a mapper to get the enum.
   *
   * @param key         Path to Enum
   * @param mapper Transformer to enum
   * @param <E>         EnumType
   * @throws SimplixValidationException if an internal error occurs
   * @return Serialized Enum
   */
  @Nullable
  default <E extends Enum<E>> E getRawEnum(
          final String key,
          final Function<String, E> mapper) {
    return getRawEnum(splitPath(key), mapper);
  }

  /**
   * Serialize an Enum from entry in the data-structure
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param <E>      EnumType
   * @return Serialized Enum
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   */
  default <E extends Enum<E>> Optional<E> findEnum(
          final String[] key,
          final Class<E> enumType) {
    return Optional.ofNullable(getRawEnum(key, enumType));
  }

  /**
   * Serialize an Enum from entry in the data-structure
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param <E>      EnumType
   * @return Serialized Enum
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   */
  default <E extends Enum<E>> Optional<E> findEnum(
          final String key,
          final Class<E> enumType) {
    return findEnum(splitPath(key), enumType);
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses an extra mapper to skip simple scenarios. (like capitalization)
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param mapper   Mapper for some simple scenarios
   * @param <E>      EnumType
   * @return Serialized Enum
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   */
  default <E extends Enum<E>> Optional<E> findEnum(
          final String[] key,
          final Class<E> enumType,
          final Function<String, String> mapper) {
    return Optional.ofNullable(getRawEnum(key, enumType, mapper));
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses an extra mapper to skip simple scenarios. (like capitalization)
   *
   * @param key      Path to Enum
   * @param enumType Class of the Enum
   * @param mapper   Mapper for some simple scenarios
   * @param <E>      EnumType
   * @return Serialized Enum
   * @throws SimplixValidationException if an internal error occurs
   * @throws IllegalArgumentException if no enum match
   */
  default <E extends Enum<E>> Optional<E> findEnum(
          final String key,
          final Class<E> enumType,
          final Function<String, String> mapper) {
    return findEnum(splitPath(key), enumType, mapper);
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses a mapper to get the enum.
   *
   * @param key         Path to Enum
   * @param mapper Transformer to enum
   * @param <E>         EnumType
   * @return Serialized Enum
   * @throws SimplixValidationException if an internal error occurs
   */
  default <E extends Enum<E>> Optional<E> findEnum(
          final String[] key,
          final Function<String, E> mapper) {
    return Optional.ofNullable(getRawEnum(key, mapper));
  }

  /**
   * Serialize an Enum from entry in the data-structure.<br>
   * Uses a mapper to get the enum.
   *
   * @param key         Path to Enum
   * @param mapper Transformer to enum
   * @param <E>         EnumType
   * @return Serialized Enum
   * @throws SimplixValidationException if an internal error occurs
   */
  default <E extends Enum<E>> Optional<E> findEnum(
          final String key,
          final Function<String, E> mapper) {
    return findEnum(splitPath(key), mapper);
  }

  // ----------------------------------------------------------------------------------------------------
  // Serializable Methods
  // ----------------------------------------------------------------------------------------------------

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   * @see #setSerializable(String, Object, Class)
   */
  default <T> void setSerializable(@NotNull final String key, @NotNull final T value) {
    try {
      final Object data = SimplixSerializerManager.serialize(value);
      set(key, data);
    } catch (final Throwable throwable) {
      throw SimplixProviders.exceptionHandler().create(
              throwable,
              "Can't serialize: '" + key + "'",
              "Class: '" + value.getClass().getName() + "'",
              "Package: '" + value.getClass().getPackage() + "'");
    }
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.<br>
   * This ensures to serialize with the given type.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   * @see #setSerializable(String, Object)
   */
  default <T> void setSerializable(@NotNull final String[] key, @NotNull final T value, @NotNull final Class<T> type) {
    try {
      final Object data = SimplixSerializerManager.serialize(value, type);
      set(key, data);
    } catch (final Throwable throwable) {
      throw SimplixProviders.exceptionHandler().create(
              throwable,
              "Can't serialize: '" + createPath(key) + "'",
              "Class: '" + value.getClass().getName() + "'",
              "Package: '" + value.getClass().getPackage() + "'");
    }
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.<br>
   * This ensures to serialize with the given type.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   * @see #setSerializable(String, Object)
   */
  default <T> void setSerializable(@NotNull final String key, @NotNull final T value, @NotNull final Class<T> type) {
    setSerializable(splitPath(key), value, type);
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   */
  default <T> void setSerializableList(@NotNull final String[] key, @NotNull final List<T> value, @NotNull final Class<T> type) {
    try {
      final Object data = SimplixSerializerManager.serializeList(value, type);
      set(key, data);
    } catch (final Throwable throwable) {
      throw SimplixProviders.exceptionHandler().create(
              throwable,
              "Can't serialize list: '" + createPath(key) + "'",
              "Class: '" + value.getClass().getName() + "'",
              "Package: '" + value.getClass().getPackage() + "'");
    }
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   */
  default <T> void setSerializableList(@NotNull final String key, @NotNull final List<T> value, @NotNull final Class<T> type) {
    setSerializableList(splitPath(key), value, type);
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   */
  default <T> void setSerializableListFiltered(@NotNull final String[] key, @NotNull final List<T> value, @NotNull final Class<T> type) {
    try {
      final Object data = SimplixSerializerManager.serializeListFiltered(value, type);
      set(key, data);
    } catch (final Throwable throwable) {
      throw SimplixProviders.exceptionHandler().create(
              throwable,
              "Can't serialize list: '" + createPath(key) + "'",
              "Class: '" + value.getClass().getName() + "'",
              "Package: '" + value.getClass().getPackage() + "'");
    }
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   */
  default <T> void setSerializableListFiltered(@NotNull final String key, @NotNull final List<T> value, @NotNull final Class<T> type) {
    setSerializableListFiltered(splitPath(key), value, type);
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   */
  default <T> void setSerializableMap(@NotNull final String[] key, @NotNull final Map<String, T> value, @NotNull final Class<T> type) {
    try {
      final Object data = SimplixSerializerManager.serializeMap(value, type);
      set(key, data);
    } catch (final Throwable throwable) {
      throw SimplixProviders.exceptionHandler().create(
              throwable,
              "Can't serialize map: '" + createPath(key) + "'",
              "Class: '" + value.getClass().getName() + "'",
              "Package: '" + value.getClass().getPackage() + "'");
    }
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   */
  default <T> void setSerializableMap(@NotNull final String key, @NotNull final Map<String, T> value, @NotNull final Class<T> type) {
    setSerializableMap(splitPath(key), value, type);
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   */
  default <T> void setSerializableMapFiltered(@NotNull final String[] key, @NotNull final Map<String, T> value, @NotNull final Class<T> type) {
    try {
      final Object data = SimplixSerializerManager.serializeMapFiltered(value, type);
      set(key, data);
    } catch (final Throwable throwable) {
      throw SimplixProviders.exceptionHandler().create(
              throwable,
              "Can't serialize map: '" + createPath(key) + "'",
              "Class: '" + value.getClass().getName() + "'",
              "Package: '" + value.getClass().getPackage() + "'");
    }
  }

  /**
   * Method to deserialize a class using the {@link SimplixSerializerManager}. You will need to register
   * your serializable in the {@link SimplixSerializerManager} before.
   *
   * @param key   The key your value should be associated with.
   * @param value The value you want to set in your data-structure.
   * @throws SimplixValidationException if an error is found
   */
  default <T> void setSerializableMapFiltered(@NotNull final String key, @NotNull final Map<String, T> value, @NotNull final Class<T> type) {
    setSerializableMapFiltered(splitPath(key), value, type);
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.
   *
   * @return Serialized instance of class.
   */
  @Nullable
  default <T> T getSerializable(final String[] key, final Object data, final Class<T> type) {
    if (contains(key)) {
      final Object raw = get(key);
      if (raw != null) {
        return SimplixSerializerManager.deserialize(raw, data, type);
      }
    }
    return null;
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.
   *
   * @return Serialized instance of class.
   */
  @Nullable
  default <T> T getSerializable(final String[] key, final Class<T> type) {
    if (contains(key)) {
      final Object raw = get(key);
      if (raw != null) {
        return SimplixSerializerManager.deserialize(raw, type);
      }
    }
    return null;
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.
   *
   * @return Serialized instance of class.
   */
  @Nullable
  default <T> T getSerializable(final String key, final Class<T> type) {
    return getSerializable(splitPath(key), type);
  }

  @NotNull
  default <T> T getOrDefSerializable(final String[] key, final Object data, final Class<T> type, final T def) {
    if (contains(key)) {
      final Object raw = get(key);
      if (raw != null) {
        return SimplixSerializerManager.deserialize(raw, data, type);
      }
    }
    return def;
  }

  @NotNull
  default <T> T getOrDefSerializable(final String[] key, final Class<T> type, final T def) {
    if (contains(key)) {
      final Object raw = get(key);
      if (raw != null) {
        return SimplixSerializerManager.deserialize(raw, type);
      }
    }
    return def;
  }

  @NotNull
  default <T> T getOrDefSerializable(final String key, final Class<T> type, final T def) {
    return getOrDefSerializable(splitPath(key), type, def);
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.
   *
   * @return Serialized instance of class.
   * @throws NullPointerException if no serializer for the given class is found
   * @throws ClassCastException if the data does not match
   */
  @NotNull
  default <T> Optional<T> findSerializable(final String[] key, final Object data, final Class<T> type) {
    if (contains(key)) {
      final Object raw = get(key);
      if (raw != null) {
        return Optional.of(SimplixSerializerManager.deserialize(raw, data, type));
      }
    }
    return Optional.empty();
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.
   *
   * @return Serialized instance of class.
   * @throws NullPointerException if no serializer for the given class is found
   * @throws ClassCastException if the data does not match
   */
  @NotNull
  default <T> Optional<T> findSerializable(final String[] key, final Class<T> type) {
    if (contains(key)) {
      final Object raw = get(key);
      if (raw != null) {
        return Optional.of(SimplixSerializerManager.deserialize(raw, type));
      }
    }
    return Optional.empty();
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.
   *
   * @return Serialized instance of class.
   * @throws NullPointerException if no serializer for the given class is found
   * @throws ClassCastException if the data does not match
   */
  @NotNull
  default <T> Optional<T> findSerializable(final String key, final Class<T> type) {
    return findSerializable(splitPath(key), type);
  }

  @NotNull
  default <T> List<T> getSerializableList(final String[] key, final Object data, final Class<T> type) {
    final List<?> rawList = getList(key);
    return SimplixSerializerManager.deserializeList(rawList, data, type);
  }

  @NotNull
  default <T> List<T> getSerializableList(final String[] key, final Class<T> type) {
    final List<?> rawList = getList(key);
    return SimplixSerializerManager.deserializeList(rawList, type);
  }

  @NotNull
  default <T> List<T> getSerializableList(final String key, final Class<T> type) {
    return getSerializableList(splitPath(key), type);
  }

  @NotNull
  default <T> List<T> getSerializableListFiltered(final String[] key, final Object data, final Class<T> type) {
    final List<?> rawList = getList(key);
    return SimplixSerializerManager.deserializeListFiltered(rawList, data, type);
  }

  @NotNull
  default <T> List<T> getSerializableListFiltered(final String[] key, final Class<T> type) {
    final List<?> rawList = getList(key);
    return SimplixSerializerManager.deserializeListFiltered(rawList, type);
  }

  @NotNull
  default <T> List<T> getSerializableListFiltered(final String key, final Class<T> type) {
    return getSerializableListFiltered(splitPath(key), type);
  }

  @NotNull
  default <T> Map<String, T> getSerializableMap(final String[] key, final Object data, final Class<T> type) {
    final Map<?, ?> rawMap = getMap(key);
    return SimplixSerializerManager.deserializeMap(rawMap, data, type);
  }

  @NotNull
  default <T> Map<String, T> getSerializableMap(final String[] key, final Class<T> type) {
    final Map<?, ?> rawMap = getMap(key);
    return SimplixSerializerManager.deserializeMap(rawMap, type);
  }

  @NotNull
  default <T> Map<String, T> getSerializableMap(final String key, final Class<T> type) {
    return getSerializableMap(splitPath(key), type);
  }

  @NotNull
  default <T> Map<String, T> getSerializableMapFiltered(final String[] key, final Object data, final Class<T> type) {
    final Map<?, ?> rawMap = getMap(key);
    return SimplixSerializerManager.deserializeMapFiltered(rawMap, data, type);
  }

  @NotNull
  default <T> Map<String, T> getSerializableMapFiltered(final String[] key, final Class<T> type) {
    final Map<?, ?> rawMap = getMap(key);
    return SimplixSerializerManager.deserializeMapFiltered(rawMap, type);
  }

  @NotNull
  default <T> Map<String, T> getSerializableMapFiltered(final String key, final Class<T> type) {
    return getSerializableMapFiltered(splitPath(key), type);
  }

  // ----------------------------------------------------------------------------------------------------
  // Advanced methods to save time.
  // ----------------------------------------------------------------------------------------------------

  /**
   * Returns the value for key in the data-structure, if it exists, else the specified default value.
   *
   * @param key Key to data in our data-structure.
   * @param def Default value, if data-structure doesn't contain key.
   * @param <T> Type of default-value.
   * @throws ClassCastException if the type is not valid
   */
  default <T> T getOrDefault(final String[] key, @NotNull final T def) {
    final Object raw = get(key);
    return raw == null ? def : ClassWrapper.getFromDef(raw, def);
  }

  /**
   * Returns the value for key in the data-structure, if it exists, else the specified default value.
   *
   * @param key Key to data in our data-structure.
   * @param def Default value, if data-structure doesn't contain key.
   * @param <T> Type of default-value.
   * @throws ClassCastException if the type is not valid
   */
  default <T> T getOrDefault(final String key, @NotNull final T def) {
    final Object raw = get(key);
    return raw == null ? def : ClassWrapper.getFromDef(raw, def);
  }

  /**
   * Get a List from a data-structure and use a mapper for simple values.<br>
   * This is for simple values, for complex implementations use a custom serializer instead.
   *
   * @param key        Path to List in data structure.
   * @param mapper     Mapper to parse the list content.
   * @param rawDefault Default raw List to set and map.
   */
  @NotNull
  default <T> List<T> getOrSetList(final String[] key, final Function<String, T> mapper, List<String> rawDefault) {
    return getOrDefault(key, rawDefault).stream().map(mapper).collect(Collectors.toList());
  }

  /**
   * Get a List from a data-structure and use a mapper for simple values.<br>
   * This is for simple values, for complex implementations use a custom serializer instead.
   *
   * @param key        Path to List in data structure.
   * @param mapper     Mapper to parse the list content.
   * @param rawDefault Default raw List to set and map.
   */
  @NotNull
  default <T> List<T> getOrSetList(final String key, final Function<String, T> mapper, List<String> rawDefault) {
    return getOrSetList(splitPath(key), mapper, rawDefault);
  }

  /**
   * Get a List from a data-structure and use a mapper for simple values.<br>
   * Also filters the null results.<br>
   * This is for simple values, for complex implementations use a custom serializer instead.
   *
   * @param key        Path to List in data structure.
   * @param mapper     Mapper to parse the list content.
   * @param rawDefault Default raw List to set and map.
   */
  @NotNull
  default <T> List<T> getOrSetListFiltered(final String[] key, final Function<String, T> mapper, List<String> rawDefault) {
    return getOrSetDefault(key, rawDefault).stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * Get a List from a data-structure and use a mapper for simple values.<br>
   * Also filters the null results.<br>
   * This is for simple values, for complex implementations use a custom serializer instead.
   *
   * @param key        Path to List in data structure.
   * @param mapper     Mapper to parse the list content.
   * @param rawDefault Default raw List to set and map.
   */
  @NotNull
  default <T> List<T> getOrSetListFiltered(final String key, final Function<String, T> mapper, List<String> rawDefault) {
    return getOrSetListFiltered(splitPath(key), mapper, rawDefault);
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.<br>
   * If the key doesn't yet exist, it will be created in the data-structure, set to def and afterward returned.
   *
   * @return Serialized instance of class.
   */
  @NotNull
  default <T> T getOrSetSerializable(final String[] key, final Object data, final Class<T> type, final T def) {
    if (contains(key)) {
      Object raw = get(key);
      if (raw != null) {
        return SimplixSerializerManager.deserialize(raw, data, type);
      }
    }
    setSerializable(key, def, type);
    return def;
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.<br>
   * If the key doesn't yet exist, it will be created in the data-structure, set to def and afterward returned.
   *
   * @return Serialized instance of class.
   */
  @NotNull
  default <T> T getOrSetSerializable(final String[] key, final Class<T> type, final T def) {
    if (contains(key)) {
      Object raw = get(key);
      if (raw != null) {
        return SimplixSerializerManager.deserialize(raw, type);
      }
    }
    setSerializable(key, def, type);
    return def;
  }

  /**
   * Method to serialize a Class using the {@link SimplixSerializerManager}.<br>
   * You will need to register your serializable in the {@link SimplixSerializerManager} before.<br>
   * If the key doesn't yet exist, it will be created in the data-structure, set to def and afterward returned.
   *
   * @return Serialized instance of class.
   */
  @NotNull
  default <T> T getOrSetSerializable(final String key, final Class<T> type, final T def) {
    return getOrSetSerializable(splitPath(key), type, def);
  }

  /**
   * Sets a value in the data-structure if the key doesn't yet exist.
   * Has nothing to do with Bukkit's 'addDefault' method.
   *
   * @param key   Key of data to set in our data-structure.
   * @param value Value to set.
   */
  default void setDefault(final String[] key, final Object value) {
    if (!contains(key)) {
      set(key, value);
    }
  }

  /**
   * Sets a value in the data-structure if the key doesn't yet exist.
   * Has nothing to do with Bukkit's 'addDefault' method.
   *
   * @param key   Key of data to set in our data-structure.
   * @param value Value to set.
   */
  default void setDefault(final String key, final Object value) {
    setDefault(splitPath(key), value);
  }

  /**
   * Mix of setDefault and getDefault.
   * <p>Gets the value of the key in the data structure, casted to the type of the specified default def.
   * If the key doesn't yet exist, it will be created in the data-structure, set to def and afterward returned.</p>
   *
   * @param key Key to set the value
   * @param def Value to set or return.
   */
  default <T> T getOrSetDefault(final String[] key, final T def) {
    final Object raw = get(key);
    //Key is not yet present in data-structure
    if (raw == null) {
      set(key, def);
      return def;
    } else {
      return ClassWrapper.getFromDef(raw, def);
    }
  }

  /**
   * Mix of setDefault and getDefault.
   * <p>Gets the value of the key in the data structure, casted to the type of the specified default def.
   * If the key doesn't yet exist, it will be created in the data-structure, set to def and afterward returned.</p>
   *
   * @param key Key to set the value
   * @param def Value to set or return.
   */
  default <T> T getOrSetDefault(final String key, final T def) {
    return getOrSetDefault(splitPath(key), def);
  }
}
