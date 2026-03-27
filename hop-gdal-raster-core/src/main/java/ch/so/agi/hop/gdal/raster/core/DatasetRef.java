package ch.so.agi.hop.gdal.raster.core;

import java.util.Objects;

public record DatasetRef(DatasetRefType type, String value) {
  private static final String VSICURL_PREFIX = "/vsicurl/";

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

  public String toGdalIdentifier() {
    return switch (type) {
      case LOCAL_FILE -> value;
      case HTTP_URL -> VSICURL_PREFIX + value;
      case GDAL_VSI -> value;
    };
  }
}
