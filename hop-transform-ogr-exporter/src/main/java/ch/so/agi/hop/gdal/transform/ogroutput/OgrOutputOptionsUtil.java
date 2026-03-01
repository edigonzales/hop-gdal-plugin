package ch.so.agi.hop.gdal.transform.ogroutput;

import ch.so.agi.gdal.ffm.OgrWriteMode;
import ch.so.agi.hop.gdal.ogr.core.OgrOptionTextUtil;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OgrOutputOptionsUtil {

  static final String FORCE_GEOMETRY_AUTO = "AUTO";
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
}
