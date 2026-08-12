package ch.so.agi.hop.gdal.transform.ogrinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ch.so.agi.gdal.ffm.OgrGeometry;
import com.atolcd.hop.gis.geometry.curve.CircularString;
import com.atolcd.hop.gis.geometry.curve.CurveGeometrySupport;
import com.atolcd.hop.gis.geometry.curve.CurvePolygon;
import com.atolcd.hop.gis.geometry.curve.CurveWkbReader;
import com.atolcd.hop.gis.geometry.curve.MultiCurve;
import com.atolcd.hop.gis.geometry.curve.MultiSurface;
import java.util.HexFormat;
import java.util.List;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

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

  @Test
  void shouldDecodeOgrMultiCurveWithoutStrokingNestedCircularString() throws Exception {
    OgrInput input = newStandaloneInput();
    CurvePolygon curvePolygon = curvePolygon();
    CircularString arc = (CircularString) curvePolygon.getCurveRings().getFirst();
    LineString line =
        curvePolygon
            .getFactory()
            .createLineString(
                new Coordinate[] {new Coordinate(10, 0), new Coordinate(12, 2)});
    MultiCurve expected = new MultiCurve(List.of(line, arc), curvePolygon.getFactory());
    OgrGeometry ogrGeometry = OgrGeometry.fromWkb(CurveGeometrySupport.writeWkb(expected), 2056);

    Geometry geometry = input.toJtsGeometry(ogrGeometry);

    MultiCurve multiCurve = assertInstanceOf(MultiCurve.class, geometry);
    assertEquals(2056, multiCurve.getSRID());
    assertEquals(2, multiCurve.getCurves().size());
    CircularString decodedArc =
        assertInstanceOf(CircularString.class, multiCurve.getCurves().get(1));
    assertEquals(5, decodedArc.getControlPoints().length);
  }

  @Test
  void shouldDecodeOgrMultiSurfaceWithoutStrokingNestedCurvePolygon() throws Exception {
    OgrInput input = newStandaloneInput();
    CurvePolygon curvePolygon = curvePolygon();
    Polygon linearPolygon =
        curvePolygon
            .getFactory()
            .createPolygon(
                new Coordinate[] {
                  new Coordinate(10, 10),
                  new Coordinate(14, 10),
                  new Coordinate(14, 14),
                  new Coordinate(10, 14),
                  new Coordinate(10, 10)
                });
    MultiSurface expected =
        new MultiSurface(List.of(linearPolygon, curvePolygon), curvePolygon.getFactory());
    OgrGeometry ogrGeometry = OgrGeometry.fromWkb(CurveGeometrySupport.writeWkb(expected), 2056);

    Geometry geometry = input.toJtsGeometry(ogrGeometry);

    MultiSurface multiSurface = assertInstanceOf(MultiSurface.class, geometry);
    assertEquals(2056, multiSurface.getSRID());
    assertEquals(2, multiSurface.getSurfaces().size());
    CurvePolygon decodedCurvePolygon =
        assertInstanceOf(CurvePolygon.class, multiSurface.getSurfaces().get(1));
    assertInstanceOf(CircularString.class, decodedCurvePolygon.getCurveRings().getFirst());
  }

  private static CurvePolygon curvePolygon() {
    return (CurvePolygon) new CurveWkbReader().read(CURVE_POLYGON_WKB);
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
