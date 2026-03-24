package ch.so.agi.hop.gdal.raster.core;

import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrFieldType;
import ch.so.agi.gdal.ffm.OgrLayerWriteSpec;
import ch.so.agi.gdal.ffm.OgrWriteMode;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.locationtech.jts.geom.Geometry;

public final class VsiVectorDatasetSupport {
  private VsiVectorDatasetSupport() {}

  public static DatasetRef writeSingleGeometryDataset(
      Geometry geometry, String layerName, Map<String, Object> attributes) throws Exception {
    String normalizedLayer = (layerName == null || layerName.isBlank()) ? "features" : layerName.trim();
    String vsiPath = "/vsimem/" + normalizedLayer + "-" + UUID.randomUUID() + ".geojson";

    List<OgrFieldDefinition> fields = new ArrayList<>();
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
      fields.add(new OgrFieldDefinition(entry.getKey(), toFieldType(entry.getValue())));
    }

    OgrLayerWriteSpec spec =
        new OgrLayerWriteSpec(normalizedLayer, toGeometryTypeCode(geometry), fields)
            .withWriteMode(OgrWriteMode.OVERWRITE);

    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () -> {
          try (OgrDataSource dataSource =
              Ogr.create(
                  ch.so.agi.gdal.ffm.DatasetRef.gdalVsi(vsiPath),
                  "GeoJSON",
                  OgrWriteMode.OVERWRITE,
                  Map.of())) {
            try (var writer = dataSource.openWriter(spec)) {
              writer.write(
                  new OgrFeature(
                      -1L,
                      new LinkedHashMap<>(attributes),
                      HopGeometrySupport.toOgrGeometry(geometry)));
            }
          }
        });

    return new DatasetRef(DatasetRefType.GDAL_VSI, vsiPath);
  }

  private static OgrFieldType toFieldType(Object value) {
    if (value instanceof Float || value instanceof Double) {
      return OgrFieldType.REAL;
    }
    if (value instanceof Number) {
      return OgrFieldType.INTEGER64;
    }
    return OgrFieldType.STRING;
  }

  private static int toGeometryTypeCode(Geometry geometry) {
    String geometryType = geometry.getGeometryType().toUpperCase();
    return switch (geometryType) {
      case "POINT" -> 1;
      case "LINESTRING" -> 2;
      case "POLYGON" -> 3;
      case "MULTIPOINT" -> 4;
      case "MULTILINESTRING" -> 5;
      case "MULTIPOLYGON" -> 6;
      case "GEOMETRYCOLLECTION" -> 7;
      default -> 0;
    };
  }
}
