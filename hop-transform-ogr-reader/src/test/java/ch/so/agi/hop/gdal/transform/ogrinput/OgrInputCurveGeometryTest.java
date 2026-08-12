package ch.so.agi.hop.gdal.transform.ogrinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ch.so.agi.gdal.ffm.OgrGeometry;
import com.atolcd.hop.gis.geometry.curve.CircularString;
import com.atolcd.hop.gis.geometry.curve.CurvePolygon;
import java.util.HexFormat;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class OgrInputCurveGeometryTest {
  private static final byte[] CURVE_POLYGON_WKB =
      HexFormat.of()
          .parseHex(
              "010A00000001000000010800000005000000"
                  + "00000000000000000000000000000000"
                  + "00000000000010400000000000000000"
                  + "00000000000010400000000000001040"
                  + "00000000000000000000000000001040"
                  + "00000000000000000000000000000000");

  @Test
  void shouldDecodeOgrCurvePolygonWithoutStrokingAndPreserveSrid() throws Exception {
    OgrInput input = newStandaloneInput();
    OgrGeometry ogrGeometry = OgrGeometry.fromWkb(CURVE_POLYGON_WKB, 2056);

    Geometry geometry = input.toJtsGeometry(ogrGeometry);

    CurvePolygon polygon = assertInstanceOf(CurvePolygon.class, geometry);
    assertEquals(2056, polygon.getSRID());
    CircularString ring = assertInstanceOf(CircularString.class, polygon.getCurveRings().getFirst());
    assertEquals(5, ring.getControlPoints().length);
    assertEquals(4.0, ring.getControlPoints()[1].x);
    assertEquals(4.0, ring.getControlPoints()[2].y);
  }

  private static OgrInput newStandaloneInput() {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    PipelineMeta pipelineMeta = new PipelineMeta();
    TransformMeta transformMeta = new TransformMeta("ogr-input-curve-test", meta);
    pipelineMeta.addTransform(transformMeta);
    return new OgrInput(transformMeta, meta, new OgrInputData(), 0, pipelineMeta, null);
  }
}
