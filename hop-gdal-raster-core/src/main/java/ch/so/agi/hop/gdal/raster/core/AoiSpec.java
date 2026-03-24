package ch.so.agi.hop.gdal.raster.core;

public record AoiSpec(
    AoiMode mode,
    BoundsSpec bounds,
    String inlinePolygon,
    DatasetRef datasetRef,
    String layerName) {

  public AoiSpec {
    mode = mode == null ? AoiMode.NONE : mode;
  }

  public enum AoiMode {
    NONE,
    BOUNDS,
    INLINE_POLYGON,
    DATASET_LAYER,
    BOUNDS_AND_INLINE_POLYGON,
    BOUNDS_AND_DATASET_LAYER;

    public static AoiMode fromValue(String value) {
      if (value == null || value.isBlank()) {
        return NONE;
      }
      return switch (value.trim().toUpperCase()) {
        case "BOUNDS" -> BOUNDS;
        case "INLINE_POLYGON", "POLYGON_AOI" -> INLINE_POLYGON;
        case "DATASET_LAYER" -> DATASET_LAYER;
        case "BOUNDS_AND_INLINE_POLYGON", "BOUNDS+POLYGON" -> BOUNDS_AND_INLINE_POLYGON;
        case "BOUNDS_AND_DATASET_LAYER", "BOUNDS+DATASET" -> BOUNDS_AND_DATASET_LAYER;
        default -> NONE;
      };
    }
  }
}
