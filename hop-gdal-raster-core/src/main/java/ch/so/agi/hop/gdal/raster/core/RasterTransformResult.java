package ch.so.agi.hop.gdal.raster.core;

public record RasterTransformResult(
    boolean success,
    String message,
    long elapsedMs,
    String input,
    String output,
    String detailsJson) {

  public static RasterTransformResult success(long elapsedMs, String input, String output, String detailsJson) {
    return new RasterTransformResult(true, "OK", elapsedMs, input, output, detailsJson);
  }

  public static RasterTransformResult failure(
      long elapsedMs, String input, String output, String message, String detailsJson) {
    return new RasterTransformResult(false, message, elapsedMs, input, output, detailsJson);
  }
}
