package ch.so.agi.hop.gdal.raster.core;

import java.util.Objects;

public record DatasetRef(DatasetRefType type, String value) {
  public DatasetRef {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(value, "value must not be null");
    value = value.trim();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value must not be blank");
    }
  }

  public boolean isRemote() {
    return type == DatasetRefType.HTTP_URL || type == DatasetRefType.GDAL_VSI;
  }
}
