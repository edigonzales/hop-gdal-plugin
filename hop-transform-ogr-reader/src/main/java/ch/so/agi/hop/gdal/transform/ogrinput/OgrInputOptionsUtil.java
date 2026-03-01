package ch.so.agi.hop.gdal.transform.ogrinput;

import ch.so.agi.hop.gdal.ogr.core.OgrOptionTextUtil;
import java.util.List;
import java.util.Map;

final class OgrInputOptionsUtil {

  private OgrInputOptionsUtil() {}

  static List<String> splitCsvOrSemicolon(String raw) {
    return OgrOptionTextUtil.splitCsvOrSemicolon(raw);
  }

  static Map<String, String> parseKeyValueOptions(String raw) {
    return OgrOptionTextUtil.parseKeyValueOptions(raw);
  }

  static Long parsePositiveLimit(String raw) {
    return OgrOptionTextUtil.parsePositiveLong(raw, "Feature limit");
  }

  static void validateSpatialFilterExclusivity(String bbox, String polygonWkt) {
    OgrOptionTextUtil.validateMutuallyExclusive(bbox, polygonWkt, "bbox", "polygonWkt");
  }

  static String trimToNull(String value) {
    return OgrOptionTextUtil.trimToNull(value);
  }
}
