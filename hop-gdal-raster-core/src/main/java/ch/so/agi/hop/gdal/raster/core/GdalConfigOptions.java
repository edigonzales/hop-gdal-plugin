package ch.so.agi.hop.gdal.raster.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record GdalConfigOptions(Map<String, String> values) {
  public GdalConfigOptions {
    Objects.requireNonNull(values, "values must not be null");
    values = Map.copyOf(new LinkedHashMap<>(values));
  }

  public static GdalConfigOptions empty() {
    return new GdalConfigOptions(Map.of());
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }
}
