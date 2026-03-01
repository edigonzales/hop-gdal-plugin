package ch.so.agi.hop.gdal.transform.ogroutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaDate;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.row.value.ValueMetaString;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;

class OgrOutputTransformLogicTest {

  @Test
  void shouldResolveAllAttributesExceptGeometryWhenSelectionIsBlank() {
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaInteger("id"));
    rowMeta.addValueMeta(new ValueMetaString("name"));
    rowMeta.addValueMeta(new ValueMetaString("geom"));

    List<String> names = OgrOutput.resolveAttributeNames(rowMeta, "geom", " ");
    assertEquals(List.of("id", "name"), names);
  }

  @Test
  void shouldResolveSelectedAttributesInInputOrder() {
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaInteger("id"));
    rowMeta.addValueMeta(new ValueMetaString("name"));
    rowMeta.addValueMeta(new ValueMetaString("kind"));
    rowMeta.addValueMeta(new ValueMetaString("geom"));

    List<String> names = OgrOutput.resolveAttributeNames(rowMeta, "geom", "kind,id");
    assertEquals(List.of("id", "kind"), names);
  }

  @Test
  void shouldRejectUnknownSelectedAttributes() {
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaInteger("id"));
    rowMeta.addValueMeta(new ValueMetaString("geom"));

    assertThrows(
        IllegalArgumentException.class,
        () -> OgrOutput.resolveAttributeNames(rowMeta, "geom", "id,missing"));
  }

  @Test
  void shouldMapHopTypesToOgrTypes() {
    assertEquals(
        ch.so.agi.gdal.ffm.OgrFieldType.INTEGER64,
        OgrOutput.mapHopTypeToOgrType(new ValueMetaInteger("id")));
    assertEquals(
        ch.so.agi.gdal.ffm.OgrFieldType.REAL,
        OgrOutput.mapHopTypeToOgrType(new ValueMetaNumber("n")));
    assertEquals(
        ch.so.agi.gdal.ffm.OgrFieldType.INTEGER,
        OgrOutput.mapHopTypeToOgrType(new ValueMetaBoolean("flag")));
    assertEquals(
        ch.so.agi.gdal.ffm.OgrFieldType.STRING,
        OgrOutput.mapHopTypeToOgrType(new ValueMetaDate("d")));
    assertEquals(
        ch.so.agi.gdal.ffm.OgrFieldType.STRING,
        OgrOutput.mapHopTypeToOgrType(new ValueMetaString("s")));
  }

  @Test
  void shouldResolveGeometryTypeCodeFromForcedValue() {
    assertEquals(3, OgrOutput.resolveGeometryTypeCode("POLYGON", null));
  }

  @Test
  void shouldResolveGeometryTypeCodeFromGeometryInstance() {
    GeometryFactory factory = new GeometryFactory();
    assertEquals(
        1,
        OgrOutput.resolveGeometryTypeCode(
            "AUTO", factory.createPoint(new org.locationtech.jts.geom.Coordinate(1, 1))));
    assertEquals(
        7,
        OgrOutput.resolveGeometryTypeCode(
            "AUTO", factory.createGeometryCollection(new org.locationtech.jts.geom.Geometry[0])));
  }
}
