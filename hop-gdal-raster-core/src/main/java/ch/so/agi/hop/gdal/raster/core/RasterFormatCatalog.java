package ch.so.agi.hop.gdal.raster.core;

import ch.so.agi.gdal.ffm.Gdal;
import ch.so.agi.gdal.ffm.RasterDriverInfo;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RasterFormatCatalog {
  private static final List<String> COMMON_FORMATS = List.of("GTiff", "COG", "VRT", "PNG", "JPEG", "MEM");
  private static final List<String> FALLBACK_COMPRESS = List.of("NONE", "LZW", "DEFLATE", "ZSTD", "JPEG");
  private static volatile Snapshot cachedSnapshot;

  private RasterFormatCatalog() {}

  public static List<String> outputFormats() {
    return snapshot().outputFormats();
  }

  public static List<String> compressionOptions(String driverShortName) {
    if (driverShortName == null || driverShortName.isBlank()) {
      return List.of();
    }
    return snapshot().compressionByFormat().getOrDefault(driverShortName.trim().toUpperCase(Locale.ROOT), List.of());
  }

  static void clearCacheForTests() {
    cachedSnapshot = null;
  }

  static List<String> orderFormats(List<String> formats) {
    LinkedHashMap<String, String> byUpper = new LinkedHashMap<>();
    for (String format : formats) {
      if (format == null || format.isBlank()) {
        continue;
      }
      String normalized = format.trim();
      byUpper.putIfAbsent(normalized.toUpperCase(Locale.ROOT), normalized);
    }

    List<String> ordered = new ArrayList<>();
    for (String common : COMMON_FORMATS) {
      String hit = byUpper.remove(common.toUpperCase(Locale.ROOT));
      if (hit != null) {
        ordered.add(hit);
      }
    }

    List<String> rest = new ArrayList<>(byUpper.values());
    rest.sort(String::compareToIgnoreCase);
    ordered.addAll(rest);
    return List.copyOf(ordered);
  }

  private static Snapshot snapshot() {
    Snapshot local = cachedSnapshot;
    if (local != null) {
      return local;
    }
    synchronized (RasterFormatCatalog.class) {
      if (cachedSnapshot == null) {
        cachedSnapshot = loadSnapshot();
      }
      return cachedSnapshot;
    }
  }

  private static Snapshot loadSnapshot() {
    try {
      return OgrBindingsClassLoaderSupport.withPluginContextClassLoader(RasterFormatCatalog::loadFromBindings);
    } catch (Throwable ignored) {
      return fallbackSnapshot();
    }
  }

  private static Snapshot loadFromBindings() {
    List<RasterDriverInfo> drivers = Gdal.listWritableRasterDrivers();
    List<String> orderedFormats = orderFormats(drivers.stream().map(RasterDriverInfo::shortName).toList());
    if (orderedFormats.isEmpty()) {
      return fallbackSnapshot();
    }

    Map<String, List<String>> compressionByFormat = new LinkedHashMap<>();
    for (String format : orderedFormats) {
      compressionByFormat.put(
          format.toUpperCase(Locale.ROOT), deduplicateIgnoreCase(Gdal.listCompressionOptions(format)));
    }

    return new Snapshot(List.copyOf(orderedFormats), Map.copyOf(compressionByFormat));
  }

  private static Snapshot fallbackSnapshot() {
    Map<String, List<String>> compressionByFormat = new LinkedHashMap<>();
    compressionByFormat.put("GTIFF", FALLBACK_COMPRESS);
    compressionByFormat.put("COG", FALLBACK_COMPRESS);
    return new Snapshot(COMMON_FORMATS, Map.copyOf(compressionByFormat));
  }

  private static List<String> deduplicateIgnoreCase(List<String> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> ordered = new LinkedHashSet<>();
    LinkedHashSet<String> seenUpper = new LinkedHashSet<>();
    for (String value : values) {
      if (value == null || value.isBlank()) {
        continue;
      }
      String normalized = value.trim();
      String upper = normalized.toUpperCase(Locale.ROOT);
      if (seenUpper.add(upper)) {
        ordered.add(normalized);
      }
    }
    return List.copyOf(ordered);
  }

  private record Snapshot(List<String> outputFormats, Map<String, List<String>> compressionByFormat) {
    private Snapshot {
      outputFormats = List.copyOf(outputFormats);
      compressionByFormat = Map.copyOf(compressionByFormat);
    }
  }
}
