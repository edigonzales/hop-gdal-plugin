package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import ch.so.agi.hop.gdal.raster.core.RasterResultFields;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.row.value.ValueMetaString;

final class GdalRasterZonalStatsRowFields {
  private static final String FIELD_PREFIX = "zs_";
  private static final List<TechnicalFieldSpec> TECHNICAL_FIELD_SPECS =
      List.of(
          new TechnicalFieldSpec(
              RasterResultFields.SUCCESS, "gdal_upstream_success", "gdal_zs_success", IValueMeta.TYPE_BOOLEAN),
          new TechnicalFieldSpec(
              RasterResultFields.MESSAGE, "gdal_upstream_message", "gdal_zs_message", IValueMeta.TYPE_STRING),
          new TechnicalFieldSpec(
              RasterResultFields.ELAPSED_MS,
              "gdal_upstream_elapsed_ms",
              "gdal_zs_elapsed_ms",
              IValueMeta.TYPE_INTEGER),
          new TechnicalFieldSpec(
              RasterResultFields.INPUT, "gdal_upstream_input", "gdal_zs_input", IValueMeta.TYPE_STRING),
          new TechnicalFieldSpec(
              RasterResultFields.OUTPUT, "gdal_upstream_output", "gdal_zs_output", IValueMeta.TYPE_STRING),
          new TechnicalFieldSpec(
              RasterResultFields.DETAILS_JSON,
              "gdal_upstream_details_json",
              "gdal_zs_details_json",
              IValueMeta.TYPE_STRING));
  private static final Set<String> JSON_STATS =
      Set.of("center_x", "center_y", "coverage", "frac", "unique", "values");

  private GdalRasterZonalStatsRowFields() {}

  static List<RowFieldSpec> describe(String statsText, String bandsText) {
    List<String> stats = GdalRasterZonalStatsOptions.parseStats(statsText);
    List<Integer> bands = GdalRasterZonalStatsOptions.parseBands(bandsText);
    boolean singleBand = bands.size() <= 1;
    List<RowFieldSpec> fields = new ArrayList<>();

    if (bands.isEmpty()) {
      for (String stat : stats) {
        fields.add(new RowFieldSpec(fieldName(stat, null, true), stat, null, JSON_STATS.contains(stat)));
      }
      return List.copyOf(fields);
    }

    if (singleBand) {
      Integer band = bands.getFirst();
      for (String stat : stats) {
        fields.add(new RowFieldSpec(fieldName(stat, band, true), stat, band, JSON_STATS.contains(stat)));
      }
      return List.copyOf(fields);
    }

    for (Integer band : bands) {
      for (String stat : stats) {
        fields.add(new RowFieldSpec(fieldName(stat, band, false), stat, band, JSON_STATS.contains(stat)));
      }
    }
    return List.copyOf(fields);
  }

  static void appendValueMetas(IRowMeta rowMeta, List<RowFieldSpec> specs) throws HopTransformException {
    for (RowFieldSpec spec : specs) {
      if (rowMeta.indexOfValue(spec.fieldName()) >= 0) {
        throw new HopTransformException("Row output field already exists: " + spec.fieldName());
      }
      rowMeta.addValueMeta(spec.createValueMeta());
    }
  }

  static void renameUpstreamTechnicalValueMetas(IRowMeta rowMeta) throws HopTransformException {
    validateReservedTechnicalFieldCollisions(rowMeta);
    for (TechnicalFieldSpec spec : TECHNICAL_FIELD_SPECS) {
      int index = rowMeta.indexOfValue(spec.baseName());
      if (index < 0) {
        continue;
      }
      IValueMeta renamed = rowMeta.getValueMeta(index).clone();
      renamed.setName(spec.upstreamName());
      rowMeta.setValueMeta(index, renamed);
    }
  }

  static void appendZonalStatsTechnicalValueMetas(IRowMeta rowMeta) throws HopTransformException {
    for (TechnicalFieldSpec spec : TECHNICAL_FIELD_SPECS) {
      if (rowMeta.indexOfValue(spec.zonalStatsName()) >= 0) {
        throw new HopTransformException("Row output field already exists: " + spec.zonalStatsName());
      }
      rowMeta.addValueMeta(spec.createZonalStatsValueMeta());
    }
  }

  static List<String> resolveZoneAttributeNames(
      IRowMeta rowMeta, String geometryField, String includeFieldsText) {
    int geometryIndex = rowMeta.indexOfValue(geometryField);
    if (geometryIndex < 0) {
      throw new IllegalArgumentException("Geometry field was not found: " + geometryField);
    }

    List<String> requested = GdalRasterZonalStatsOptions.parseFields(includeFieldsText);
    LinkedHashSet<String> fieldNames = new LinkedHashSet<>();

    if (requested.isEmpty()) {
      for (int i = 0; i < rowMeta.size(); i++) {
        String name = rowMeta.getValueMeta(i).getName();
        if (i == geometryIndex || isTechnicalField(name)) {
          continue;
        }
        fieldNames.add(name);
      }
      return List.copyOf(fieldNames);
    }

    for (String requestedName : requested) {
      int index = rowMeta.indexOfValue(requestedName);
      if (index < 0) {
        throw new IllegalArgumentException("Zone field was not found in input row: " + requestedName);
      }
      String actualName = rowMeta.getValueMeta(index).getName();
      if (index == geometryIndex) {
        throw new IllegalArgumentException("Geometry field cannot be used as a zone attribute: " + actualName);
      }
      if (isTechnicalField(actualName)) {
        throw new IllegalArgumentException("Technical field cannot be used as a zone attribute: " + actualName);
      }
      fieldNames.add(actualName);
    }
    return List.copyOf(fieldNames);
  }

  static Map<String, Object> normalizeZoneAttributes(
      Object[] row, IRowMeta rowMeta, List<String> attributeNames) {
    LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    for (String attributeName : attributeNames) {
      int index = rowMeta.indexOfValue(attributeName);
      if (index < 0) {
        throw new IllegalArgumentException("Zone field was not found in input row: " + attributeName);
      }
      attributes.put(
          rowMeta.getValueMeta(index).getName(),
          normalizeAttributeValue(rowMeta.getValueMeta(index), row[index]));
    }
    return attributes;
  }

  static Map<String, Object> extractFieldValues(List<RowFieldSpec> specs, Map<String, Object> attributes) {
    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
    for (RowFieldSpec spec : specs) {
      values.put(spec.fieldName(), spec.normalizeValue(resolveAttribute(spec, attributes)));
    }
    return values;
  }

  static String toJson(Object value) {
    StringBuilder builder = new StringBuilder();
    appendJson(builder, value);
    return builder.toString();
  }

  private static Object resolveAttribute(RowFieldSpec spec, Map<String, Object> attributes) {
    for (String candidate : spec.candidateOutputNames()) {
      if (attributes.containsKey(candidate)) {
        return attributes.get(candidate);
      }
    }
    return null;
  }

  private static Object normalizeAttributeValue(IValueMeta valueMeta, Object value) {
    if (value == null) {
      return null;
    }
    return switch (valueMeta.getType()) {
      case IValueMeta.TYPE_DATE, IValueMeta.TYPE_TIMESTAMP ->
          value instanceof java.util.Date date ? date.toInstant().toString() : value.toString();
      case IValueMeta.TYPE_BIGNUMBER, IValueMeta.TYPE_NUMBER ->
          value instanceof Number number ? number.doubleValue() : value;
      case IValueMeta.TYPE_INTEGER -> value instanceof Number number ? number.longValue() : value;
      default -> value;
    };
  }

  private static boolean isTechnicalField(String fieldName) {
    if (fieldName == null) {
      return false;
    }
    for (TechnicalFieldSpec spec : TECHNICAL_FIELD_SPECS) {
      if (fieldName.equalsIgnoreCase(spec.baseName())
          || fieldName.equalsIgnoreCase(spec.upstreamName())
          || fieldName.equalsIgnoreCase(spec.zonalStatsName())) {
        return true;
      }
    }
    return false;
  }

  static void applyZonalStatsTechnicalValues(
      Object[] outputRow, int startIndex, ch.so.agi.hop.gdal.raster.core.RasterTransformResult technical) {
    int index = startIndex;
    outputRow[index++] = technical.success();
    outputRow[index++] = technical.message();
    outputRow[index++] = technical.elapsedMs();
    outputRow[index++] = technical.input();
    outputRow[index++] = technical.output();
    outputRow[index] = technical.detailsJson();
  }

  private static void validateReservedTechnicalFieldCollisions(IRowMeta rowMeta)
      throws HopTransformException {
    for (TechnicalFieldSpec spec : TECHNICAL_FIELD_SPECS) {
      if (rowMeta.indexOfValue(spec.upstreamName()) >= 0) {
        throw new HopTransformException("Row output field already exists: " + spec.upstreamName());
      }
      if (rowMeta.indexOfValue(spec.zonalStatsName()) >= 0) {
        throw new HopTransformException("Row output field already exists: " + spec.zonalStatsName());
      }
    }
  }

  private static String fieldName(String stat, Integer band, boolean singleBand) {
    if (singleBand) {
      return FIELD_PREFIX + stat;
    }
    return FIELD_PREFIX + "b" + band + "_" + stat;
  }

  private static void appendJson(StringBuilder builder, Object value) {
    if (value == null) {
      builder.append("null");
      return;
    }
    if (value instanceof Number || value instanceof Boolean) {
      builder.append(value);
      return;
    }
    if (value instanceof Map<?, ?> map) {
      builder.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (!first) {
          builder.append(',');
        }
        first = false;
        appendJson(builder, entry.getKey() == null ? "null" : entry.getKey().toString());
        builder.append(':');
        appendJson(builder, entry.getValue());
      }
      builder.append('}');
      return;
    }
    if (value instanceof Iterable<?> iterable) {
      builder.append('[');
      boolean first = true;
      for (Object item : iterable) {
        if (!first) {
          builder.append(',');
        }
        first = false;
        appendJson(builder, item);
      }
      builder.append(']');
      return;
    }
    if (value.getClass().isArray()) {
      builder.append('[');
      int length = Array.getLength(value);
      for (int i = 0; i < length; i++) {
        if (i > 0) {
          builder.append(',');
        }
        appendJson(builder, Array.get(value, i));
      }
      builder.append(']');
      return;
    }
    builder.append('"').append(escapeJson(value.toString())).append('"');
  }

  private static String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\f", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  record RowFieldSpec(String fieldName, String statName, Integer band, boolean jsonField) {
    org.apache.hop.core.row.IValueMeta createValueMeta() {
      return jsonField ? new ValueMetaString(fieldName) : new ValueMetaNumber(fieldName);
    }

    List<String> candidateOutputNames() {
      List<String> names = new ArrayList<>();
      if (band == null) {
        names.add(statName);
        names.add("b1_" + statName);
        return List.copyOf(names);
      }
      if (band == 1) {
        names.add(statName);
      }
      names.add("b" + band + "_" + statName);
      names.add("band" + band + "_" + statName);
      names.add(statName + "_" + band);
      return List.copyOf(new LinkedHashSet<>(names));
    }

    Object normalizeValue(Object rawValue) {
      if (rawValue == null) {
        return null;
      }
      if (jsonField) {
        return GdalRasterZonalStatsRowFields.toJson(rawValue);
      }
      if (rawValue instanceof Number number) {
        return number.doubleValue();
      }
      if (rawValue instanceof Boolean bool) {
        return bool ? 1D : 0D;
      }
      if (rawValue instanceof String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
          return null;
        }
        try {
          return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(
              "Expected numeric zonal stats value for field '" + fieldName + "': " + rawValue,
              e);
        }
      }
      throw new IllegalArgumentException(
          "Expected numeric zonal stats value for field '"
              + fieldName
              + "' but received "
              + rawValue.getClass().getName());
    }
  }

  private record TechnicalFieldSpec(
      String baseName, String upstreamName, String zonalStatsName, int valueType) {
    IValueMeta createZonalStatsValueMeta() {
      return switch (valueType) {
        case IValueMeta.TYPE_BOOLEAN -> new ValueMetaBoolean(zonalStatsName);
        case IValueMeta.TYPE_INTEGER -> new ValueMetaInteger(zonalStatsName);
        default -> new ValueMetaString(zonalStatsName);
      };
    }
  }
}
