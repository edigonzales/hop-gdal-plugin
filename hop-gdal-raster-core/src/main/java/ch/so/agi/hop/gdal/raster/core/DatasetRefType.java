package ch.so.agi.hop.gdal.raster.core;

public enum DatasetRefType {
  LOCAL_FILE,
  HTTP_URL,
  GDAL_VSI;

  public static DatasetRefType fromValue(String value) {
    if (value == null || value.isBlank()) {
      return LOCAL_FILE;
    }
    return switch (value.trim().toUpperCase()) {
      case "HTTP_URL", "HTTP/HTTPS URL" -> HTTP_URL;
      case "GDAL_VSI", "EXPLICIT GDAL/VSI PATH" -> GDAL_VSI;
      default -> LOCAL_FILE;
    };
  }
}
