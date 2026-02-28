package ch.so.agi.hop.gdal.transform.ogrinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrFieldType;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import java.util.List;
import org.apache.hop.core.row.IValueMeta;
import org.junit.jupiter.api.Test;

class OgrInputTransformLogicTest {

  @Test
  void shouldSelectAllFieldsWhenAttributeSelectionIsBlank() {
    OgrLayerDefinition layerDefinition =
        new OgrLayerDefinition(
            "test",
            0,
            List.of(
                new OgrFieldDefinition("name", OgrFieldType.STRING),
                new OgrFieldDefinition("height", OgrFieldType.REAL)));

    List<OgrFieldDefinition> selected = OgrInput.selectProjectedFields(layerDefinition, " ");
    assertEquals(2, selected.size());
    assertEquals("name", selected.get(0).name());
    assertEquals("height", selected.get(1).name());
  }

  @Test
  void shouldSelectConfiguredFieldsCaseInsensitiveInLayerOrder() {
    OgrLayerDefinition layerDefinition =
        new OgrLayerDefinition(
            "test",
            0,
            List.of(
                new OgrFieldDefinition("fid_ext", OgrFieldType.INTEGER64),
                new OgrFieldDefinition("name", OgrFieldType.STRING),
                new OgrFieldDefinition("height", OgrFieldType.REAL)));

    List<OgrFieldDefinition> selected = OgrInput.selectProjectedFields(layerDefinition, "HEIGHT;NAME");
    assertEquals(2, selected.size());
    assertEquals("name", selected.get(0).name());
    assertEquals("height", selected.get(1).name());
  }

  @Test
  void shouldRejectUnknownConfiguredFields() {
    OgrLayerDefinition layerDefinition =
        new OgrLayerDefinition(
            "test", 0, List.of(new OgrFieldDefinition("name", OgrFieldType.STRING)));

    assertThrows(
        IllegalArgumentException.class,
        () -> OgrInput.selectProjectedFields(layerDefinition, "name,missing"));
  }

  @Test
  void shouldMapIntegerFieldToHopIntegerValueMeta() {
    IValueMeta valueMeta =
        OgrInput.toHopValueMeta(new OgrFieldDefinition("id", OgrFieldType.INTEGER64));
    assertEquals(IValueMeta.TYPE_INTEGER, valueMeta.getType());
  }

  @Test
  void shouldMapRealFieldToHopNumberValueMeta() {
    IValueMeta valueMeta =
        OgrInput.toHopValueMeta(new OgrFieldDefinition("length", OgrFieldType.REAL));
    assertEquals(IValueMeta.TYPE_NUMBER, valueMeta.getType());
  }

  @Test
  void shouldMapStringLikeFieldToHopStringValueMeta() {
    IValueMeta valueMeta =
        OgrInput.toHopValueMeta(new OgrFieldDefinition("name", OgrFieldType.STRING));
    assertEquals(IValueMeta.TYPE_STRING, valueMeta.getType());
  }
}
