package de.leonhard.storage.internal.serialize;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface SimplixSerializerHolder<T> {

  /**
   * Get our serializable from data in data-structure.
   *
   * @param obj Data to deserialize our class from.
   * @throws ClassCastException Exception thrown when deserialization failed.
   */
  T deserialize(@NonNull final Object obj) throws ClassCastException;

  /**
   * Get our serializable from data in data-structure.
   *
   * @param obj Data to deserialize our class from.
   * @param data Extra object to be used for deserialize.
   * @throws ClassCastException Exception thrown when deserialization failed.
   */
  default T deserialize(@NonNull final Object obj, @Nullable final Object data) throws ClassCastException {
    return deserialize(obj);
  }

  /**
   * Save our serializable to data-structure.
   *
   * @throws ClassCastException Exception thrown when serialization failed.
   */
  Object serialize(@NonNull final T t) throws ClassCastException;

  Class<T> getClazz();
}
