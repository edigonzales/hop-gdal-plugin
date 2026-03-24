package ch.so.agi.hop.gdal.raster.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CreationOptionParser {
  private CreationOptionParser() {}

  public static List<String> parse(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    for (String token : text.split("[\\n;,]+")) {
      if (token == null) {
        continue;
      }
      String trimmed = token.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (!trimmed.contains("=")) {
        throw new IllegalArgumentException("Expected creation/config option in key=value form: " + trimmed);
      }
      values.add(trimmed);
    }
    return List.copyOf(values);
  }

  public static Map<String, String> parseKeyValueMap(String text) {
    LinkedHashMap<String, String> values = new LinkedHashMap<>();
    for (String token : parse(text)) {
      int index = token.indexOf('=');
      values.put(token.substring(0, index).trim(), token.substring(index + 1).trim());
    }
    return Map.copyOf(values);
  }
}
