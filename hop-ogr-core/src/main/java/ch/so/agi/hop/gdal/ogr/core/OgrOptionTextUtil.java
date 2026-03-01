package ch.so.agi.hop.gdal.ogr.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OgrOptionTextUtil {

  private OgrOptionTextUtil() {}

  public static List<String> splitCsvOrSemicolon(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }

    String[] parts = raw.split("[,;]");
    Set<String> values = new LinkedHashSet<>();
    for (String part : parts) {
      String trimmed = trimToNull(part);
      if (trimmed != null) {
        values.add(trimmed);
      }
    }
    return List.copyOf(values);
  }

  public static Map<String, String> parseKeyValueOptions(String raw) {
    if (raw == null || raw.isBlank()) {
      return Map.of();
    }

    Map<String, String> parsed = new LinkedHashMap<>();
    List<String> entries = splitEntries(raw);
    for (String entry : entries) {
      int idx = entry.indexOf('=');
      if (idx < 1) {
        throw new IllegalArgumentException("Invalid option entry (expected key=value): " + entry);
      }

      String key = entry.substring(0, idx).trim();
      String value = entry.substring(idx + 1).trim();
      if (key.isEmpty()) {
        throw new IllegalArgumentException("Invalid option entry (empty key): " + entry);
      }
      parsed.put(key, value);
    }
    return Map.copyOf(parsed);
  }

  public static Long parsePositiveLong(String raw, String description) {
    String trimmed = trimToNull(raw);
    if (trimmed == null) {
      return null;
    }

    try {
      long parsed = Long.parseLong(trimmed);
      if (parsed <= 0) {
        throw new IllegalArgumentException(description + " must be > 0: " + raw);
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(description + " must be a number: " + raw, e);
    }
  }

  public static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public static void validateMutuallyExclusive(
      String firstValue, String secondValue, String firstLabel, String secondLabel) {
    if (trimToNull(firstValue) != null && trimToNull(secondValue) != null) {
      throw new IllegalArgumentException(
          "Only one option can be set: either '" + firstLabel + "' or '" + secondLabel + "'.");
    }
  }

  private static List<String> splitEntries(String raw) {
    String normalized = raw.replace('\r', '\n').replace(';', '\n');
    String[] split = normalized.split("\n");
    List<String> entries = new ArrayList<>(split.length);
    for (String line : split) {
      String trimmed = trimToNull(line);
      if (trimmed != null) {
        entries.add(trimmed);
      }
    }
    return entries;
  }
}
