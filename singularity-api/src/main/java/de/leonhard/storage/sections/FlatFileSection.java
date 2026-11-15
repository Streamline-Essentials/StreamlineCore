package de.leonhard.storage.sections;

import de.leonhard.storage.internal.DataStorage;
import de.leonhard.storage.internal.FlatFile;

import java.util.Set;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FlatFileSection implements DataStorage {

  protected final FlatFile flatFile;

  private final String[] pathPrefix;

  public String getPathPrefix() {
    return flatFile.createPath(pathPrefix);
  }
  
  public FlatFileSection getSection(final String pathPrefix) {
    return getSection(splitPath(pathPrefix));
  }

  public FlatFileSection getSection(final String[] pathPrefix) {
    return new FlatFileSection(this.flatFile, createFinalKey(pathPrefix));
  }

  @Override
  public Set<String> singleLayerKeySet() {
    return flatFile.singleLayerKeySet(pathPrefix);
  }

  @Override
  public Set<String> singleLayerKeySet(final String[] key) {
    return flatFile.singleLayerKeySet(createFinalKey(key));
  }

  @Override
  public Set<String> keySet() {
    return flatFile.keySet(pathPrefix);
  }

  @Override
  public Set<String> keySet(final String[] key) {
    return flatFile.keySet(createFinalKey(key));
  }

  @Override
  public void remove(final String[] key) {
    flatFile.remove(createFinalKey(key));
  }

  @Override
  public void setRaw(final String[] key, final Object value) {
    flatFile.setRaw(createFinalKey(key), value);
  }

  @Override
  public boolean contains(final String[] key) {
    return flatFile.contains(createFinalKey(key));
  }

  @Override
  public String pathSeparator() {
    return flatFile.pathSeparator();
  }

  @Override
  public Object get(final String[] key) {
    return flatFile.get(createFinalKey(key));
  }

  private String[] createFinalKey(final String[] key) {
    return (pathPrefix == null || pathPrefix.length == 0) ? key : concatenatePath(this.pathPrefix, key);
  }
}
