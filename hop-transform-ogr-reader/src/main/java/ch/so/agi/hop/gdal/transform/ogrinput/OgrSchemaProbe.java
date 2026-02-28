package ch.so.agi.hop.gdal.transform.ogrinput;

import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class OgrSchemaProbe {

  private OgrSchemaProbe() {}

  static List<OgrLayerDefinition> readLayers(Path filePath, Map<String, String> openOptions) {
    Objects.requireNonNull(filePath, "filePath must not be null");
    Objects.requireNonNull(openOptions, "openOptions must not be null");

    return OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () -> {
          try (OgrDataSource dataSource = Ogr.open(filePath, openOptions)) {
            return dataSource.listLayers();
          }
        });
  }

  static OgrLayerDefinition resolveLayer(List<OgrLayerDefinition> layers, String requestedLayerName) {
    if (layers == null || layers.isEmpty()) {
      throw new IllegalArgumentException("Datasource has no layers.");
    }

    String normalized = OgrInputOptionsUtil.trimToNull(requestedLayerName);
    if (normalized == null) {
      return layers.getFirst();
    }

    for (OgrLayerDefinition layer : layers) {
      if (layer.name().equals(normalized)) {
        return layer;
      }
    }
    for (OgrLayerDefinition layer : layers) {
      if (layer.name().equalsIgnoreCase(normalized)) {
        return layer;
      }
    }

    throw new IllegalArgumentException("Layer not found: " + normalized);
  }

  static String formatFieldPreview(OgrLayerDefinition layerDefinition) {
    Objects.requireNonNull(layerDefinition, "layerDefinition must not be null");

    StringBuilder preview = new StringBuilder();
    preview
        .append("Layer: ")
        .append(layerDefinition.name())
        .append(System.lineSeparator())
        .append("Geometry type code: ")
        .append(layerDefinition.geometryType())
        .append(System.lineSeparator())
        .append("Fields:")
        .append(System.lineSeparator());

    if (layerDefinition.fields().isEmpty()) {
      preview.append("  (none)");
      return preview.toString();
    }

    for (OgrFieldDefinition field : layerDefinition.fields()) {
      preview
          .append("  - ")
          .append(field.name())
          .append(" (")
          .append(field.type().name())
          .append(')')
          .append(System.lineSeparator());
    }
    return preview.toString().trim();
  }
}
