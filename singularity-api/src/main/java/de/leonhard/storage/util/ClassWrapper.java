package de.leonhard.storage.util;

import de.leonhard.storage.internal.DataStorage;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;
import lombok.val;

@SuppressWarnings("unchecked")
@UtilityClass
public class ClassWrapper {

  /**
   * Method to map the contents with a function.
   *
   * @param map   Map to use
   * @param key   Mapper for the keys
   * @param value Mapper for the values
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromTypeMapper(final Map<String, ?> map, final Function<Object, K> key, final Function<Object, V> value) {
    final Map<K, V> re = new HashMap<>();
    for (Map.Entry<String, ?> e : map.entrySet()) {
      re.put(key.apply(e.getKey()), value.apply(e.getValue()));
    }
    return re;
  }

  /**
   * Method to map the contents with a function.
   *
   * @param map   Map to use
   * @param key   Mapper for the keys
   * @param value Mapper for the values
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromTypeMapperStr(final Map<String, ?> map, final Function<String, K> key, final Function<Object, V> value) {
    final Map<K, V> re = new HashMap<>();
    for (Map.Entry<String, ?> e : map.entrySet()) {
      re.put(key.apply(e.getKey()), value.apply(e.getValue()));
    }
    return re;
  }

  /**
   * Method to map the contents to the given datatypes.
   *
   * @param map   Map to use
   * @param key   Type of key
   * @param value Type of value
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromType(final Map<String, ?> map, final K key, final V value) {
    return getMapFromTypeMapper(map, getFunction(key), getFunction(value));
  }

  /**
   * Method to map the contents to the given datatypes.
   *
   * @param map   Map to use
   * @param key   Class of key
   * @param value Class of value
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromType(final Map<String, ?> map, final Class<K> key, final Class<V> value) {
    return getMapFromTypeMapper(map, getFunction(key), getFunction(value));
  }

  /**
   * Method to map the keys with a function, and the values to a given datatype.
   *
   * @param map    Map to use
   * @param mapper Mapper for the keys
   * @param value  Type of value
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromType(final Map<String, ?> map, final Function<String, K> mapper, final V value) {
    return getMapFromTypeMapperStr(map, mapper, getFunction(value));
  }

  /**
   * Method to map the keys with a function, and the values to a given datatype.
   *
   * @param map    Map to use
   * @param mapper Mapper for the keys
   * @param clazz  Class of value
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromType(final Map<String, ?> map, final Function<String, K> mapper, final Class<V> clazz) {
    return getMapFromTypeMapperStr(map, mapper, getFunction(clazz));
  }

  /**
   * Method to map the contents with a function.
   *
   * @param map    Map to use
   * @param mapper Mapper for the values
   * @return Map with mapped content
   */
  public <T> Map<String, T> getMapFromType(final Map<String, ?> map, final Function<Object, T> mapper) {
    final Map<String, T> re = new HashMap<>();
    for (Map.Entry<String, ?> e : map.entrySet()) {
      re.put(e.getKey(), mapper.apply(e.getValue()));
    }
    return re;
  }

  /**
   * Method to map the values to a given datatype.
   *
   * @param map  Map to use
   * @param type Type of value
   * @return Map with mapped content
   */
  public <T> Map<String, T> getMapFromType(final Map<String, ?> map, final T type) {
    return getMapFromType(map, getFunction(type));
  }

  /**
   * Method to map the values to a given datatype.
   *
   * @param map   Map to use
   * @param clazz Class of value
   * @return Map with mapped content
   */
  public <T> Map<String, T> getMapFromType(final Map<String, ?> map, final Class<T> clazz) {
    return getMapFromType(map, getFunction(clazz));
  }

  /**
   * Method to map the contents with a function, filtering out content that does not match.
   *
   * @param map   Map to cast
   * @param key   Mapper for the keys
   * @param value Mapper for the values
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromTypeFilterMapper(final Map<String, ?> map, final Function<Object, K> key, final Function<Object, V> value) {
    final Map<K, V> re = new HashMap<>();
    for (Map.Entry<String, ?> e : map.entrySet()) {
      try {
        re.put(key.apply(e.getKey()), value.apply(e.getValue()));
      } catch (ClassCastException ignored) {
      }
    }
    return re;
  }

  /**
   * Method to map the contents with a function, filtering out content that does not match.
   *
   * @param map   Map to cast
   * @param key   Mapper for the keys
   * @param value Mapper for the values
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromTypeFilterMapperStr(final Map<String, ?> map, final Function<String, K> key, final Function<Object, V> value) {
    final Map<K, V> re = new HashMap<>();
    for (Map.Entry<String, ?> e : map.entrySet()) {
      try {
        re.put(key.apply(e.getKey()), value.apply(e.getValue()));
      } catch (ClassCastException ignored) {
      }
    }
    return re;
  }

  /**
   * Method to map the keys with a function, and the values to a given datatype, filtering out content that does not match.
   *
   * @param map   Map to use
   * @param key   Type of key
   * @param value Type of value
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromTypeFilter(final Map<String, ?> map, final K key, final V value) {
    return getMapFromTypeFilterMapper(map, getFunction(key), getFunction(value));
  }

  /**
   * Method to map the keys with a function, and the values to a given datatype, filtering out content that does not match.
   *
   * @param map   Map to use
   * @param key   Type of key
   * @param value Type of value
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromTypeFilter(final Map<String, ?> map, final Class<K> key, final Class<V> value) {
    return getMapFromTypeFilterMapper(map, getFunction(key), getFunction(value));
  }

  /**
   * Method to map the keys with a function, and the values to a given datatype, filtering out content that does not match.
   *
   * @param map    Map to use
   * @param mapper Mapper for the keys
   * @param value  Type of value
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromTypeFilter(final Map<String, ?> map, final Function<String, K> mapper, final V value) {
    return getMapFromTypeFilterMapperStr(map, mapper, getFunction(value));
  }

  /**
   * Method to map the keys with a function, and the values to a given datatype, filtering out content that does not match.
   *
   * @param map    Map to use
   * @param mapper Mapper for the keys
   * @param clazz  Class of value
   * @return Map with mapped content
   */
  public <K, V> Map<K, V> getMapFromTypeFilter(final Map<String, ?> map, final Function<String, K> mapper, final Class<V> clazz) {
    return getMapFromTypeFilterMapperStr(map, mapper, getFunction(clazz));
  }

  /**
   * Method to map the values with a function, filtering out content that does not match.
   *
   * @param map    Map to use
   * @param mapper Mapper for the values
   * @return Map with mapped content
   */
  public <T> Map<String, T> getMapFromTypeFilter(final Map<String, ?> map, final Function<Object, T> mapper) {
    final Map<String, T> re = new HashMap<>();
    for (Map.Entry<String, ?> e : map.entrySet()) {
      try {
        re.put(e.getKey(), mapper.apply(e.getValue()));
      } catch (ClassCastException ignored) {
      }
    }
    return re;
  }

  /**
   * Method to map the values to a given datatype, filtering out content that does not match.
   *
   * @param map  Map to use
   * @param type Type of value
   * @return Map with mapped content
   */
  public <T> Map<String, T> getMapFromTypeFilter(final Map<String, ?> map, final T type) {
    return getMapFromTypeFilter(map, getFunction(type));
  }

  /**
   * Method to map the values to a given datatype, filtering out content that does not match.
   *
   * @param map   Map to use
   * @param clazz Class of value
   * @return Map with mapped content
   */
  public <T> Map<String, T> getMapFromTypeFilter(final Map<String, ?> map, final Class<T> clazz) {
    return getMapFromTypeFilter(map, getFunction(clazz));
  }

  /**
   * Method to map the content of a list with a mapper.
   *
   * @param list   List to map
   * @param mapper Mapper for the content
   * @return List with mapped content
   */
  public <T> List<T> getListFromType(final List<?> list, final Function<Object, T> mapper) {
    return list.stream().map(mapper).collect(Collectors.toList());
  }

  /**
   * Method to map the content of a list to a given datatype.
   *
   * @param list List to map
   * @param type Type of content
   * @return List with mapped content
   */
  public <T> List<T> getListFromType(final List<?> list, final T type) {
    return getListFromType(list, getFunction(type));
  }

  /**
   * Method to map the content of a list to a given datatype.
   *
   * @param list  List to map
   * @param clazz Class of content
   * @return List with mapped content
   */
  public <T> List<T> getListFromType(final List<?> list, final Class<T> clazz) {
    return getListFromType(list, getFunction(clazz));
  }

  /**
   * Method to map the content of a list with a mapper, filtering out content that does not match.
   *
   * @param list   List to map
   * @param mapper Mapper for the content
   * @return List with mapped content
   */
  public <T> List<T> getListFromTypeFilter(final List<?> list, final Function<Object, T> mapper) {
    return list.stream().map(o -> {
      try {
        return mapper.apply(o);
      } catch (ClassCastException ignored) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }

  /**
   * Method to map the content of a list to a given datatype, filtering out content that does not match.
   *
   * @param list List to map
   * @param type Type of content
   * @return List with mapped content
   */
  public <T> List<T> getListFromTypeFilter(final List<?> list, final T type) {
    return getListFromTypeFilter(list, getFunction(type));
  }

  /**
   * Method to map the content of a list to a given datatype, filtering out content that does not match.
   *
   * @param list  List to map
   * @param clazz Class of content
   * @return List with mapped content
   */
  public <T> List<T> getListFromTypeFilter(final List<?> list, final Class<T> clazz) {
    return getListFromTypeFilter(list, getFunction(clazz));
  }

  /**
   * Method to map an object to a given datatype.
   * Used for example in {@link DataStorage} to map the results of get() to for example a String.
   *
   * @param obj Object to cast
   * @param def Type of result
   * @return Mapped object
   */
  public <T> T getFromDef(final Object obj, final T def) {
    if (def instanceof Integer) {
      return (T) INTEGER.getInt(obj);
    } else if (def instanceof Float) {
      return (T) FLOAT.getFloat(obj);
    } else if (def instanceof Double) {
      return (T) DOUBLE.getDouble(obj);
    } else if (def instanceof Long) {
      return (T) LONG.getLong(obj);
    } else if (def instanceof Boolean) {
      return (T) (Boolean) obj.toString().equalsIgnoreCase("true");
    } else if (def instanceof String[]) {
      return (T) STRING.getStringArray(obj);
    } else if (def instanceof Long[] || def instanceof long[]) {
      return (T) LONG.getLongArray(obj);
    } else if (def instanceof Double[] || def instanceof double[]) {
      return (T) DOUBLE.getDoubleArray(obj);
    } else if (def instanceof Float[] || def instanceof float[]) {
      return (T) FLOAT.getFloatArray(obj);
    } else if (def instanceof Short[] || def instanceof short[]) {
      return (T) SHORT.getShortArray(obj);
    } else if (def instanceof Byte[] || def instanceof byte[]) {
      return (T) BYTE.getByteArray(obj);
    }
    return (T) obj;
  }

  /**
   * Method to map an object to a given datatype.
   * Used for example in {@link DataStorage} to map the results of get() to for example a String.
   *
   * @param obj   Object to cast
   * @param clazz Class of result
   * @return Casted object
   */
  public <T> T getFromDef(final Object obj, final Class<T> clazz) {
    if (clazz == int.class || clazz == Integer.class) {
      return (T) INTEGER.getInt(obj);
    } else if (clazz == float.class || clazz == Float.class) {
      return (T) FLOAT.getFloat(obj);
    } else if (clazz == double.class || clazz == Double.class) {
      return (T) DOUBLE.getDouble(obj);
    } else if (clazz == long.class || clazz == Long.class) {
      return (T) LONG.getLong(obj);
    } else if (clazz == boolean.class || clazz == Boolean.class) {
      return (T) (Boolean) obj.toString().equalsIgnoreCase("true");
    } else if (clazz == String[].class) {
      return (T) STRING.getStringArray(obj);
    } else if (clazz == Double[].class || clazz == double[].class) {
      return (T) DOUBLE.getDoubleArray(obj);
    } else if (clazz == Float[].class || clazz == float[].class) {
      return (T) FLOAT.getFloatArray(obj);
    } else if (clazz == Integer[].class || clazz == int[].class) {
      return (T) INTEGER.getIntArray(obj);
    } else if (clazz == Short[].class || clazz == short[].class) {
      return (T) SHORT.getShortArray(obj);
    } else if (clazz == Byte[].class || clazz == byte[].class) {
      return (T) BYTE.getByteArray(obj);
    }
    return (T) obj;
  }

  /**
   * Method to get a function to map an object to a given datatype.
   *
   * @param def Type for the function
   * @return Function to map the object
   */
  public <T> Function<Object, T> getFunction(final T def) {
    if (def instanceof Integer) {
      return o -> (T) INTEGER.getInt(o);
    } else if (def instanceof Float) {
      return o -> (T) FLOAT.getFloat(o);
    } else if (def instanceof Double) {
      return o -> (T) DOUBLE.getDouble(o);
    } else if (def instanceof Long) {
      return o -> (T) LONG.getLong(o);
    } else if (def instanceof Boolean) {
      return o -> (T) (Boolean) o.toString().equalsIgnoreCase("true");
    } else if (def instanceof String[]) {
      return o -> (T) STRING.getStringArray(o);
    } else if (def instanceof Long[] || def instanceof long[]) {
      return o -> (T) LONG.getLongArray(o);
    } else if (def instanceof Double[] || def instanceof double[]) {
      return o -> (T) DOUBLE.getDoubleArray(o);
    } else if (def instanceof Float[] || def instanceof float[]) {
      return o -> (T) FLOAT.getFloatArray(o);
    } else if (def instanceof Short[] || def instanceof short[]) {
      return o -> (T) SHORT.getShortArray(o);
    } else if (def instanceof Byte[] || def instanceof byte[]) {
      return o -> (T) BYTE.getByteArray(o);
    }
    return o -> (T) o;
  }

  /**
   * Method to get a function to map an object to a given datatype.
   *
   * @param clazz Class for the function
   * @return Function to map the object
   */
  public <T> Function<Object, T> getFunction(final Class<T> clazz) {
    if (clazz == int.class || clazz == Integer.class) {
      return o -> (T) INTEGER.getInt(o);
    } else if (clazz == float.class || clazz == Float.class) {
      return o -> (T) FLOAT.getFloat(o);
    } else if (clazz == double.class || clazz == Double.class) {
      return o -> (T) DOUBLE.getDouble(o);
    } else if (clazz == long.class || clazz == Long.class) {
      return o -> (T) LONG.getLong(o);
    } else if (clazz == boolean.class || clazz == Boolean.class) {
      return o -> (T) (Boolean) o.toString().equalsIgnoreCase("true");
    } else if (clazz == String[].class) {
      return o -> (T) STRING.getStringArray(o);
    } else if (clazz == Double[].class || clazz == double[].class) {
      return o -> (T) DOUBLE.getDoubleArray(o);
    } else if (clazz == Float[].class || clazz == float[].class) {
      return o -> (T) FLOAT.getFloatArray(o);
    } else if (clazz == Integer[].class || clazz == int[].class) {
      return o -> (T) INTEGER.getIntArray(o);
    } else if (clazz == Short[].class || clazz == short[].class) {
      return o -> (T) SHORT.getShortArray(o);
    } else if (clazz == Byte[].class || clazz == byte[].class) {
      return o -> (T) BYTE.getByteArray(o);
    }
    return o -> (T) o;
  }

  @UtilityClass
  public class LONG {

    public Long[] getLongArray(final Object obj) {
      if (obj instanceof List) {
        val list = (List<Long>) obj;
        return list.toArray(new Long[0]);
      }

      return new Long[0];
    }

    public Long getLong(final Object obj) {
      if (obj instanceof Number) {
        return ((Number) obj).longValue();
      } else if (obj instanceof String) {
        return Long.parseLong((String) obj);
      } else {
        return Long.parseLong(obj.toString());
      }
    }
  }

  @UtilityClass
  public class DOUBLE {

    public Double[] getDoubleArray(final Object obj) {
      if (obj instanceof List) {
        val list = (List<Double>) obj;
        return list.toArray(new Double[0]);
      }

      return new Double[0];
    }

    public Double getDouble(final Object obj) {
      if (obj instanceof Number) {
        return ((Number) obj).doubleValue();
      } else if (obj instanceof String) {
        return Double.parseDouble((String) obj);
      } else {
        return Double.parseDouble(obj.toString());
      }
    }
  }

  @UtilityClass
  public class FLOAT {

    public Float[] getFloatArray(final Object obj) {
      if (obj instanceof List) {
        val list = (List<Float>) obj;
        return list.toArray(new Float[0]);
      }

      return new Float[0];
    }

    public Float getFloat(final Object obj) {
      if (obj instanceof Number) {
        return ((Number) obj).floatValue();
      } else if (obj instanceof String) {
        return Float.parseFloat((String) obj);
      } else {
        return Float.parseFloat(obj.toString());
      }
    }
  }

  @UtilityClass
  public class INTEGER {

    public Integer[] getIntArray(final Object obj) {
      if (obj instanceof List) {
        val list = (List<Integer>) obj;
        return list.toArray(new Integer[0]);
      }

      return new Integer[0];
    }

    public Integer getInt(final Object obj) {
      if (obj instanceof Number) {
        return ((Number) obj).intValue();
      } else if (obj instanceof String) {
        return Integer.parseInt((String) obj);
      } else {
        return Integer.parseInt(obj.toString());
      }
    }
  }

  @UtilityClass
  @SuppressWarnings("unused")
  public class SHORT {

    public Short[] getShortArray(final Object obj) {
      if (obj instanceof List) {
        val list = (List<Short>) obj;
        return list.toArray(new Short[0]);
      }

      return new Short[0];
    }

    public Short getShort(final Object obj) {
      if (obj instanceof Number) {
        return ((Number) obj).shortValue();
      } else if (obj instanceof String) {
        return Short.parseShort((String) obj);
      } else {
        return Short.parseShort(obj.toString());
      }
    }
  }

  @UtilityClass
  public class BYTE {

    public Byte[] getByteArray(final Object obj) {
      if (obj instanceof List) {
        val list = (List<Byte>) obj;
        return list.toArray(new Byte[0]);
      }

      return new Byte[0];
    }

    public Byte getByte(final Object obj) {
      if (obj instanceof Number) {
        return ((Number) obj).byteValue();
      } else if (obj instanceof STRING) {
        return Byte.parseByte(obj.toString());
      } else {
        return Byte.parseByte(obj.toString());
      }
    }
  }

  @UtilityClass
  public class STRING {

    public String[] getStringArray(final Object obj) {
      if (obj instanceof List) {
        val list = (List<String>) obj;
        return list.toArray(new String[0]);
      }

      return new String[0];
    }

    public String getString(final Object obj) {
      if (obj instanceof Collection && ((Collection<?>) obj).size() == 1) {
        return ((List<?>) obj).get(0).toString();
      }
      return new String(obj.toString().getBytes(), StandardCharsets.UTF_8);
    }
  }
}
