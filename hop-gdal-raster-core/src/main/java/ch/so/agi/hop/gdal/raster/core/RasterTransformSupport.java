package ch.so.agi.hop.gdal.raster.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
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

  public static String resolveValue(
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
    return valueOrField.resolve(row, rowMeta, RasterTransformSupport::trimToNull);
  }

  public static String resolveRequiredValue(
      String valueMode,
      String constantValue,
      String fieldName,
      Object[] row,
      IRowMeta rowMeta,
      Function<String, String> constantResolver,
      String label) {
    String resolved = resolveValue(valueMode, constantValue, fieldName, row, rowMeta, constantResolver);
    if (resolved == null || resolved.isBlank()) {
      throw new IllegalArgumentException(label + " must not be blank");
    }
    return resolved;
  }

  public static DatasetRef resolveOutputDatasetRef(
      String sourceMode,
      String valueMode,
      String constantValue,
      String fieldName,
      Object[] row,
      IRowMeta rowMeta,
      Function<String, String> constantResolver) {
    DatasetRef datasetRef =
        resolveDatasetRef(sourceMode, valueMode, constantValue, fieldName, row, rowMeta, constantResolver);
    validateOutputDatasetRef(datasetRef);
    return datasetRef;
  }

  public static DatasetRef resolveConstantDatasetRef(String sourceMode, String value) {
    String resolvedValue = trimToNull(value);
    if (resolvedValue == null) {
      throw new IllegalArgumentException("Dataset reference must not be blank");
    }
    return new DatasetRef(DatasetRefType.fromValue(sourceMode), resolvedValue);
  }

  public static DatasetRef inferDatasetRef(String value) {
    String resolvedValue = trimToNull(value);
    if (resolvedValue == null) {
      throw new IllegalArgumentException("Dataset reference must not be blank");
    }
    return new DatasetRef(inferDatasetRefType(resolvedValue), resolvedValue);
  }

  public static void validateOutputSourceMode(String sourceMode) {
    if (DatasetRefType.fromValue(sourceMode) == DatasetRefType.HTTP_URL) {
      throw new IllegalArgumentException(
          "HTTP/HTTPS output is not supported; use LOCAL_FILE or GDAL_VSI");
    }
  }

  public static void validateOutputDatasetRef(DatasetRef datasetRef) {
    if (datasetRef.type() == DatasetRefType.HTTP_URL) {
      throw new IllegalArgumentException(
          "HTTP/HTTPS output is not supported; use LOCAL_FILE or GDAL_VSI");
    }
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

  public static List<DatasetRef> resolveLocalDirectoryGlobDatasetRefs(
      String directoryValueMode,
      String directoryValue,
      String directoryField,
      String globPattern,
      Object[] row,
      IRowMeta rowMeta,
      Function<String, String> constantResolver) {
    String resolvedDirectory =
        resolveRequiredValue(
            directoryValueMode,
            directoryValue,
            directoryField,
            row,
            rowMeta,
            constantResolver,
            "Input directory");
    String resolvedPattern = trimToNull(constantResolver.apply(globPattern));
    if (resolvedPattern == null) {
      throw new IllegalArgumentException("Glob pattern must not be blank");
    }

    Path directory = Path.of(resolvedDirectory);
    if (!Files.exists(directory)) {
      throw new IllegalArgumentException("Input directory does not exist: " + directory);
    }
    if (!Files.isDirectory(directory)) {
      throw new IllegalArgumentException("Input directory is not a directory: " + directory);
    }

    PathMatcher matcher = directory.getFileSystem().getPathMatcher("glob:" + resolvedPattern);
    List<String> values = new ArrayList<>();
    try (var stream = Files.list(directory)) {
      stream
          .filter(Files::isRegularFile)
          .filter(path -> matcher.matches(path.getFileName()))
          .map(Path::toString)
          .sorted()
          .forEach(values::add);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Unable to list input directory " + directory + ": " + e.getMessage(), e);
    }

    if (values.isEmpty()) {
      throw new IllegalArgumentException(
          "No input rasters matched pattern '" + resolvedPattern + "' in directory " + directory);
    }
    return values.stream().map(value -> new DatasetRef(DatasetRefType.LOCAL_FILE, value)).toList();
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

  private static DatasetRefType inferDatasetRefType(String value) {
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (normalized.startsWith("/vsi")) {
      return DatasetRefType.GDAL_VSI;
    }
    if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
      return DatasetRefType.HTTP_URL;
    }
    return DatasetRefType.LOCAL_FILE;
  }
}
