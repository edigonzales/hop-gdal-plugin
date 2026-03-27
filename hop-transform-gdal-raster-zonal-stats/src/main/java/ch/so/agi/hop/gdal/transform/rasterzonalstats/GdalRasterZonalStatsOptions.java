package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class GdalRasterZonalStatsOptions {
  static final String DEFAULT_STAT = "mean";
  static final String DEFAULT_PIXEL_MODE = "default";
  static final String DEFAULT_STRATEGY = "feature";
  private static final List<String> COMMON_VECTOR_FORMATS =
      List.of("GPKG", "GeoJSON", "FlatGeoBuf", "ESRI Shapefile", "CSV", "MEM");
  private static final LinkedHashSet<String> SUPPORTED_STATS =
      new LinkedHashSet<>(
          List.of(
              "center_x",
              "center_y",
              "count",
              "coverage",
              "frac",
              "max",
              "max_center_x",
              "max_center_y",
              "mean",
              "min",
              "min_center_x",
              "min_center_y",
              "minority",
              "mode",
              "stdev",
              "sum",
              "unique",
              "values",
              "variance",
              "variety"));
  private static final LinkedHashSet<String> PIXEL_MODES =
      new LinkedHashSet<>(List.of("default", "fractional", "all-touched"));
  private static final LinkedHashSet<String> STRATEGIES =
      new LinkedHashSet<>(List.of("feature", "raster"));

  private GdalRasterZonalStatsOptions() {}

  static List<String> fallbackOutputFormats() {
    return COMMON_VECTOR_FORMATS;
  }

  static List<String> orderOutputFormats(List<String> formats) {
    LinkedHashSet<String> ordered = new LinkedHashSet<>();
    LinkedHashSet<String> seenUpper = new LinkedHashSet<>();
    for (String common : COMMON_VECTOR_FORMATS) {
      for (String format : formats) {
        if (format == null || format.isBlank()) {
          continue;
        }
        if (common.equalsIgnoreCase(format.trim())
            && seenUpper.add(format.trim().toUpperCase(Locale.ROOT))) {
          ordered.add(format.trim());
        }
      }
    }

    List<String> remaining = new ArrayList<>();
    for (String format : formats) {
      if (format == null || format.isBlank()) {
        continue;
      }
      String trimmed = format.trim();
      if (seenUpper.add(trimmed.toUpperCase(Locale.ROOT))) {
        remaining.add(trimmed);
      }
    }
    remaining.sort(String::compareToIgnoreCase);
    ordered.addAll(remaining);
    return List.copyOf(ordered);
  }

  static List<String> parseStats(String text) {
    List<String> raw = parseNames(text);
    if (raw.isEmpty()) {
      raw = List.of(DEFAULT_STAT);
    }

    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String value : raw) {
      String candidate = value.toLowerCase(Locale.ROOT);
      if (!SUPPORTED_STATS.contains(candidate)) {
        throw new IllegalArgumentException("Unsupported zonal statistic: " + value);
      }
      normalized.add(candidate);
    }
    return List.copyOf(normalized);
  }

  static List<Integer> parseBands(String text) {
    List<String> raw = parseNames(text);
    if (raw.isEmpty()) {
      return List.of();
    }

    LinkedHashSet<Integer> bands = new LinkedHashSet<>();
    for (String value : raw) {
      int band;
      try {
        band = Integer.parseInt(value);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Band must be an integer: " + value, e);
      }
      if (band <= 0) {
        throw new IllegalArgumentException("Band must be >= 1: " + value);
      }
      bands.add(band);
    }
    return List.copyOf(bands);
  }

  static List<String> parseFields(String text) {
    return parseNames(text);
  }

  static String normalizePixels(String value) {
    return normalizeChoice(value, PIXEL_MODES, DEFAULT_PIXEL_MODE, "Unsupported pixel inclusion mode: ");
  }

  static String normalizeStrategy(String value) {
    return normalizeChoice(value, STRATEGIES, DEFAULT_STRATEGY, "Unsupported zonal stats strategy: ");
  }

  private static String normalizeChoice(
      String value, LinkedHashSet<String> allowed, String defaultValue, String errorPrefix) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (!allowed.contains(normalized)) {
      throw new IllegalArgumentException(errorPrefix + value);
    }
    return normalized;
  }

  private static List<String> parseNames(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    LinkedHashSet<String> values = new LinkedHashSet<>();
    for (String token : text.split("[\\n;,]+")) {
      if (token == null) {
        continue;
      }
      String trimmed = token.trim();
      if (!trimmed.isEmpty()) {
        values.add(trimmed);
      }
    }
    return List.copyOf(values);
  }
}
