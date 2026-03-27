package ch.so.agi.hop.gdal.raster.core;

import java.util.LinkedHashMap;
import java.util.List;

public final class RasterOutputOptionsSupport {
  public static final String WRITE_MODE_FAIL_IF_EXISTS = "FAIL_IF_EXISTS";
  public static final String WRITE_MODE_OVERWRITE = "OVERWRITE";
  public static final String WRITE_MODE_APPEND = "APPEND";
  public static final String WRITE_MODE_UPDATE = "UPDATE";
  public static final String WRITE_MODE_ADD = "ADD";
  public static final String COMPRESSION_DEFAULT = "DEFAULT";

  private RasterOutputOptionsSupport() {}

  public static String[] rasterAlgorithmWriteModes() {
    return new String[] {WRITE_MODE_FAIL_IF_EXISTS, WRITE_MODE_OVERWRITE, WRITE_MODE_APPEND};
  }

  public static String[] vectorRasterizeWriteModes() {
    return new String[] {
      WRITE_MODE_FAIL_IF_EXISTS, WRITE_MODE_OVERWRITE, WRITE_MODE_UPDATE, WRITE_MODE_ADD
    };
  }

  public static String[] vectorAlgorithmWriteModes() {
    return new String[] {WRITE_MODE_FAIL_IF_EXISTS, WRITE_MODE_OVERWRITE};
  }

  public static String normalizeConfiguredWriteMode(String mode) {
    if (mode == null) {
      return null;
    }
    if (mode.isBlank()) {
      return "";
    }
    if (WRITE_MODE_OVERWRITE.equalsIgnoreCase(mode)) {
      return WRITE_MODE_OVERWRITE;
    }
    if (WRITE_MODE_APPEND.equalsIgnoreCase(mode)) {
      return WRITE_MODE_APPEND;
    }
    if (WRITE_MODE_UPDATE.equalsIgnoreCase(mode)) {
      return WRITE_MODE_UPDATE;
    }
    if (WRITE_MODE_ADD.equalsIgnoreCase(mode)) {
      return WRITE_MODE_ADD;
    }
    if (WRITE_MODE_FAIL_IF_EXISTS.equalsIgnoreCase(mode)) {
      return WRITE_MODE_FAIL_IF_EXISTS;
    }
    return mode.trim();
  }

  public static void validateWriteMode(String writeMode, String[] allowedModes, String operationLabel) {
    String normalized = normalizeConfiguredWriteMode(writeMode);
    if (normalized == null || normalized.isBlank()) {
      return;
    }
    for (String allowedMode : allowedModes) {
      if (allowedMode.equalsIgnoreCase(normalized)) {
        return;
      }
    }
    throw new IllegalArgumentException(operationLabel + " write mode is not supported: " + writeMode);
  }

  public static void addRasterAlgorithmWriteModeArgs(List<String> args, String writeMode) {
    String normalized = normalizeConfiguredWriteMode(writeMode);
    if (WRITE_MODE_OVERWRITE.equalsIgnoreCase(normalized)) {
      args.add("--overwrite");
      return;
    }
    if (WRITE_MODE_APPEND.equalsIgnoreCase(normalized)) {
      args.add("--append");
    }
  }

  public static void addVectorRasterizeWriteModeArgs(List<String> args, String writeMode) {
    String normalized = normalizeConfiguredWriteMode(writeMode);
    if (WRITE_MODE_OVERWRITE.equalsIgnoreCase(normalized)) {
      args.add("--overwrite");
      return;
    }
    if (WRITE_MODE_UPDATE.equalsIgnoreCase(normalized)) {
      args.add("--update");
      return;
    }
    if (WRITE_MODE_ADD.equalsIgnoreCase(normalized)) {
      args.add("--add");
    }
  }

  public static void addVectorAlgorithmWriteModeArgs(List<String> args, String writeMode) {
    String normalized = normalizeConfiguredWriteMode(writeMode);
    if (WRITE_MODE_OVERWRITE.equalsIgnoreCase(normalized)) {
      args.add("--overwrite");
    }
  }

  public static void applyCompressionPreset(LinkedHashMap<String, String> creationOptions, String compressionPreset) {
    if (compressionPreset == null || compressionPreset.isBlank() || COMPRESSION_DEFAULT.equalsIgnoreCase(compressionPreset)) {
      return;
    }
    creationOptions.put("COMPRESS", compressionPreset);
  }
}
