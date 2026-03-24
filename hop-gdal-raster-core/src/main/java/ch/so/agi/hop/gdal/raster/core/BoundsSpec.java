package ch.so.agi.hop.gdal.raster.core;

import java.util.List;

public record BoundsSpec(double minX, double minY, double maxX, double maxY) {
  public static BoundsSpec parse(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String[] parts = text.split("[,;]");
    if (parts.length != 4) {
      throw new IllegalArgumentException("Bounds must contain four numbers: minX,minY,maxX,maxY");
    }
    double minX = Double.parseDouble(parts[0].trim());
    double minY = Double.parseDouble(parts[1].trim());
    double maxX = Double.parseDouble(parts[2].trim());
    double maxY = Double.parseDouble(parts[3].trim());
    if (maxX < minX || maxY < minY) {
      throw new IllegalArgumentException("Bounds max values must be >= min values");
    }
    return new BoundsSpec(minX, minY, maxX, maxY);
  }

  public List<String> toWarpArgs() {
    return List.of("-te", Double.toString(minX), Double.toString(minY), Double.toString(maxX), Double.toString(maxY));
  }
}
