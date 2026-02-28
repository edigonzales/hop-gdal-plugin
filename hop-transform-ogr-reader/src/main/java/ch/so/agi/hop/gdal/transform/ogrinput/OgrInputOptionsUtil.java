package ch.so.agi.hop.gdal.transform.ogrinput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OgrInputOptionsUtil {

  private OgrInputOptionsUtil() {}

  static List<String> splitCsvOrSemicolon(String raw) {
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

  static Map<String, String> parseKeyValueOptions(String raw) {
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

  static Long parsePositiveLimit(String raw) {
    String trimmed = trimToNull(raw);
    if (trimmed == null) {
      return null;
    }

    try {
      long parsed = Long.parseLong(trimmed);
      if (parsed <= 0) {
        throw new IllegalArgumentException("Feature limit must be > 0: " + raw);
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Feature limit must be a number: " + raw, e);
    }
  }

  static void validateSpatialFilterExclusivity(String bbox, String polygonWkt) {
    if (trimToNull(bbox) != null && trimToNull(polygonWkt) != null) {
      throw new IllegalArgumentException(
          "Only one spatial filter can be set (BBOX or polygon WKT).");
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

  static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
