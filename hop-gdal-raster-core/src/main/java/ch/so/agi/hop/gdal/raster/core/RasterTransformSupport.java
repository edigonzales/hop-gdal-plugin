package ch.so.agi.hop.gdal.raster.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.hop.core.row.IRowMeta;

public final class RasterTransformSupport {
  private RasterTransformSupport() {}

  public static DatasetRef resolveDatasetRef(
      String sourceMode,
      String valueMode,
      String constantValue,
      String fieldName,
      Object[] row,
      IRowMeta rowMeta,
      Function<String, String> constantResolver) {
    ValueOrField<String> valueOrField =
        new ValueOrField<>(
            ValueOrField.Mode.fromValue(valueMode),
            trimToNull(constantResolver.apply(constantValue)),
            trimToNull(fieldName));
    String resolved = valueOrField.resolve(row, rowMeta, RasterTransformSupport::trimToNull);
    if (resolved == null || resolved.isBlank()) {
      throw new IllegalArgumentException("Dataset reference must not be blank");
    }
    return new DatasetRef(DatasetRefType.fromValue(sourceMode), resolved);
  }

  public static RemoteAccessSpec remoteAccessSpec(
      String authType,
      String username,
      String password,
      String bearerToken,
      String headerName,
      String headerValue,
      String gdalConfigOptions,
      Function<String, String> constantResolver) {
    return new RemoteAccessSpec(
        new AuthConfigSpec(
            AuthConfigSpec.AuthType.fromValue(constantResolver.apply(authType)),
            trimToNull(constantResolver.apply(username)),
            trimToNull(constantResolver.apply(password)),
            trimToNull(constantResolver.apply(bearerToken)),
            trimToNull(constantResolver.apply(headerName)),
            trimToNull(constantResolver.apply(headerValue))),
        new GdalConfigOptions(CreationOptionParser.parseKeyValueMap(constantResolver.apply(gdalConfigOptions))));
  }

  public static List<DatasetRef> resolveDatasetRefs(
      String interpretationMode, String listText, Function<String, String> constantResolver) {
    return resolveDatasetRefs(
        interpretationMode, "CONSTANT", listText, null, null, null, constantResolver);
  }

  public static List<DatasetRef> resolveDatasetRefs(
      String interpretationMode,
      String valueMode,
      String listText,
      String listField,
      Object[] row,
      IRowMeta rowMeta,
      Function<String, String> constantResolver) {
    ValueOrField<String> valueOrField =
        new ValueOrField<>(
            ValueOrField.Mode.fromValue(valueMode),
            trimToNull(constantResolver.apply(listText)),
            trimToNull(listField));
    String resolved = valueOrField.resolve(row, rowMeta, RasterTransformSupport::trimToNull);
    if (resolved == null || resolved.isBlank()) {
      throw new IllegalArgumentException("Input raster list must not be blank");
    }

    List<String> values = splitDatasetList(resolved);
    if (values.isEmpty()) {
      throw new IllegalArgumentException("Input raster list must not be empty");
    }
    DatasetRefType type = DatasetRefType.fromValue(interpretationMode);
    return values.stream().map(value -> new DatasetRef(type, value)).toList();
  }

  private static List<String> splitDatasetList(String resolved) {
    List<String> values = new ArrayList<>();
    String trimmed = resolved.trim();
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
      String inner = trimmed.substring(1, trimmed.length() - 1);
      for (String token : inner.split(",")) {
        String cleaned = token.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
          cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (!cleaned.isBlank()) {
          values.add(cleaned);
        }
      }
      return values;
    }

    for (String token : trimmed.split("[\\n;]+")) {
      String cleaned = token.trim();
      if (!cleaned.isBlank()) {
        values.add(cleaned);
      }
    }
    return values;
  }

  public static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
