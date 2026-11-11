package de.leonhard.storage.internal.editor.yaml;

import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public final class YamlParser {

  private static final String FOOTER = "FOOTER";

  private final YamlEditor yamlEditor;

  public List<String> parseLines(
      final List<String> comments,
      final List<String> updated) {
    final List<String> out = new ArrayList<>();
    final Map<String, List<String>> parsed = assignCommentsToKey(comments);

    // Blank lines of the old file are kept as comments of the line below them. A blank line the
    // new text writes itself (inside a multi-line text) is already in place, so a kept blank line
    // right after it is the same one and is dropped; otherwise every save would add one more.
    boolean afterWrittenBlank = false;
    for (final String line : updated) {
      val rawList = getKeyAndRemove(line, parsed);
      if (rawList == null || rawList.isEmpty()) {

        out.add(line);
        afterWrittenBlank = line.trim().isEmpty();
        continue;
      }

      Collections.reverse(rawList);
      for (final String comment : rawList) {
        if (afterWrittenBlank && comment.trim().isEmpty()) {
          continue;
        }
        out.add(comment);
      }
      if (!line.equals(FOOTER)) {
        out.add(line);
      }
      afterWrittenBlank = line.trim().isEmpty();
    }

    return out;
  }

  private List<String> getKeyAndRemove(final String line, final Map<String, List<String>> data) {
    // Blank lines carry no key: multi-line texts are written with one for every line break.
    if (line.trim().isEmpty()) {
      return new ArrayList<>();
    }

    final String key = keyOf(line);
    for (val entry : data.entrySet()) {
      final String entryKey = keyOf(entry.getKey());

      // using substring since indentation might differ a bit
      if (key.equals(entryKey) || (!key.isEmpty() && key.substring(1).equals(entryKey))) {
        data.remove(entry.getKey());
        return entry.getValue();
      }
    }

    return new ArrayList<>();
  }

  /**
   * The part of a line before its first colon, or the whole line when it has none. Unlike
   * {@code split(":")[0]}, a line that starts with a colon gives an empty key instead of throwing.
   */
  private static String keyOf(final String line) {
    final int colon = line.indexOf(':');
    return colon < 0 ? line : line.substring(0, colon);
  }

  public Map<String, List<String>> assignCommentsToKey() {
    return assignCommentsToKey(this.yamlEditor.read());
  }

  /**
   * Method to map the #-comments in your YAML-File to the according key
   *
   * @param lines Initial lines to read
   * @return Mapped comments (Key, comments)
   */
  public Map<String, List<String>> assignCommentsToKey(final List<String> lines) {
    final Map<String, List<String>> out = new HashMap<>();

    String currentKey = FOOTER;
    for (int i = lines.size() - 1; i >= 0; i--) {
      final String line = lines.get(i);
      if (!line.trim().startsWith("#") && !line.isEmpty()) {
        currentKey = line;
      } else {
        final List<String> storage = out.get(keyOf(currentKey));

        if (storage == null) {
          out.put(
              keyOf(currentKey),
              new ArrayList<>(Collections.singletonList(line)));
        } else if (!storage.contains(line)) {
          storage.add(line);
        }
      }
    }
    return out;
  }
}
