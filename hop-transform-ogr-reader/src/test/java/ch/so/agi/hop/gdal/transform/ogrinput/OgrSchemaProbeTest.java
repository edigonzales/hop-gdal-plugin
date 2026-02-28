package ch.so.agi.hop.gdal.transform.ogrinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrFieldType;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class OgrSchemaProbeTest {

  @Test
  void shouldResolveFirstLayerWhenRequestedLayerIsBlank() {
    List<OgrLayerDefinition> layers =
        List.of(
            new OgrLayerDefinition("layer_a", 1, List.of()),
            new OgrLayerDefinition("layer_b", 2, List.of()));

    OgrLayerDefinition selected = OgrSchemaProbe.resolveLayer(layers, "  ");
    assertEquals("layer_a", selected.name());
  }

  @Test
  void shouldResolveLayerCaseInsensitive() {
    List<OgrLayerDefinition> layers =
        List.of(
            new OgrLayerDefinition("Parcel", 1, List.of()),
            new OgrLayerDefinition("Road", 2, List.of()));

    OgrLayerDefinition selected = OgrSchemaProbe.resolveLayer(layers, "parcel");
    assertEquals("Parcel", selected.name());
  }

  @Test
  void shouldFailResolvingUnknownLayer() {
    List<OgrLayerDefinition> layers = List.of(new OgrLayerDefinition("LayerA", 1, List.of()));
    assertThrows(IllegalArgumentException.class, () -> OgrSchemaProbe.resolveLayer(layers, "LayerX"));
  }

  @Test
  void shouldFormatLayerFieldPreview() {
    OgrLayerDefinition layer =
        new OgrLayerDefinition(
            "buildings",
            3,
            List.of(
                new OgrFieldDefinition("name", OgrFieldType.STRING),
                new OgrFieldDefinition("height", OgrFieldType.REAL)));

    String preview = OgrSchemaProbe.formatFieldPreview(layer);

    assertTrue(preview.contains("Layer: buildings"));
    assertTrue(preview.contains("Geometry type code: 3"));
    assertTrue(preview.contains("- name (STRING)"));
    assertTrue(preview.contains("- height (REAL)"));
  }

  @Test
  void shouldFormatPreviewForLayerWithoutAttributes() {
    OgrLayerDefinition layer = new OgrLayerDefinition("empty", 0, List.of());
    String preview = OgrSchemaProbe.formatFieldPreview(layer);
    assertTrue(preview.contains("(none)"));
  }
}
