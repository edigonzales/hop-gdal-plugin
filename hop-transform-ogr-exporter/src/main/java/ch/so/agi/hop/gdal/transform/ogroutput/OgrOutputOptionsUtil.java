package ch.so.agi.hop.gdal.transform.ogroutput;

import ch.so.agi.gdal.ffm.OgrWriteMode;
import ch.so.agi.hop.gdal.ogr.core.OgrOptionTextUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OgrOutputOptionsUtil {

  static final String FORCE_GEOMETRY_AUTO = "AUTO";
  private static final String CSV_FORMAT = "CSV";
  private static final String GEOMETRY_OPTION = "GEOMETRY";
  private static final String CSV_GEOMETRY_AS_WKT = "AS_WKT";
  private static final String CSV_GEOMETRY_NONE = "NONE";
  static final List<String> FORCE_GEOMETRY_VALUES =
      List.of(
          FORCE_GEOMETRY_AUTO,
          "POINT",
          "LINESTRING",
          "POLYGON",
          "MULTIPOINT",
          "MULTILINESTRING",
          "MULTIPOLYGON",
          "GEOMETRYCOLLECTION");

  private OgrOutputOptionsUtil() {}

  static List<String> splitCsvOrSemicolon(String raw) {
    return OgrOptionTextUtil.splitCsvOrSemicolon(raw);
  }

  static Map<String, String> parseKeyValueOptions(String raw) {
    return OgrOptionTextUtil.parseKeyValueOptions(raw);
  }

  static Map<String, String> resolveEffectiveLayerCreationOptions(
      String format, Map<String, String> layerOptions) {
    Map<String, String> safeOptions = layerOptions == null ? Map.of() : layerOptions;
    if (!isCsvFormat(format)) {
      return safeOptions.isEmpty() ? Map.of() : Map.copyOf(safeOptions);
    }

    if (findOptionValue(safeOptions, GEOMETRY_OPTION) != null) {
      return safeOptions.isEmpty() ? Map.of() : Map.copyOf(safeOptions);
    }

    Map<String, String> effectiveOptions = new LinkedHashMap<>(safeOptions);
    effectiveOptions.put(GEOMETRY_OPTION, CSV_GEOMETRY_AS_WKT);
    return Map.copyOf(effectiveOptions);
  }

  static boolean shouldWriteGeometry(String format, Map<String, String> layerOptions) {
    if (!isCsvFormat(format)) {
      return true;
    }

    String geometryMode = findOptionValue(layerOptions, GEOMETRY_OPTION);
    return geometryMode == null || !CSV_GEOMETRY_NONE.equalsIgnoreCase(geometryMode);
  }

  static String resolveFormatSelection(String currentSelection, List<String> availableFormats) {
    if (availableFormats == null || availableFormats.isEmpty()) {
      return "";
    }

    String normalizedCurrent = trimToNull(currentSelection);
    if (normalizedCurrent != null) {
      for (String availableFormat : availableFormats) {
        if (availableFormat.equals(normalizedCurrent)) {
          return availableFormat;
        }
      }
      for (String availableFormat : availableFormats) {
        if (availableFormat.equalsIgnoreCase(normalizedCurrent)) {
          return availableFormat;
        }
      }
    }

    return availableFormats.getFirst();
  }

  static String trimToNull(String value) {
    return OgrOptionTextUtil.trimToNull(value);
  }

  static OgrWriteMode parseWriteMode(String raw) {
    try {
      return OgrWriteMode.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid write mode: " + raw, e);
    }
  }

  static int parseForcedGeometryTypeCode(String raw) {
    String normalized = trimToNull(raw);
    if (normalized == null || FORCE_GEOMETRY_AUTO.equalsIgnoreCase(normalized)) {
      return -1;
    }

    return switch (normalized.toUpperCase(java.util.Locale.ROOT)) {
      case "POINT" -> 1;
      case "LINESTRING" -> 2;
      case "POLYGON" -> 3;
      case "MULTIPOINT" -> 4;
      case "MULTILINESTRING" -> 5;
      case "MULTIPOLYGON" -> 6;
      case "GEOMETRYCOLLECTION" -> 7;
      default -> throw new IllegalArgumentException("Unsupported forced geometry type: " + raw);
    };
  }

  static void validateForceGeometryType(String raw) {
    String normalized = trimToNull(raw);
    if (normalized == null) {
      return;
    }

    Set<String> allowed =
        Set.of(
            "AUTO",
            "POINT",
            "LINESTRING",
            "POLYGON",
            "MULTIPOINT",
            "MULTILINESTRING",
            "MULTIPOLYGON",
            "GEOMETRYCOLLECTION");
    if (!allowed.contains(normalized.toUpperCase(java.util.Locale.ROOT))) {
      throw new IllegalArgumentException("Unsupported forced geometry type: " + raw);
    }
  }

  private static boolean isCsvFormat(String format) {
    String normalized = trimToNull(format);
    return normalized != null && CSV_FORMAT.equalsIgnoreCase(normalized);
  }

  private static String findOptionValue(Map<String, String> options, String optionName) {
    if (options == null || options.isEmpty()) {
      return null;
    }

    for (Map.Entry<String, String> entry : options.entrySet()) {
      if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(optionName)) {
        return trimToNull(entry.getValue());
      }
    }
    return null;
  }
}
