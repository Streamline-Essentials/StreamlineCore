package de.leonhard.storage.internal.serialize;

public interface SimplixSerializableLike {

  Object serialized() throws ClassCastException;

  default Object serializedParsed() throws ClassCastException {
    return SimplixSerializerManager.resolveAll(serialized());
  }
}
