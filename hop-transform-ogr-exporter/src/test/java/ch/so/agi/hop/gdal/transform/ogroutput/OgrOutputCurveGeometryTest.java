package ch.so.agi.hop.gdal.transform.ogroutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import ch.so.agi.gdal.ffm.OgrGeometry;
import com.atolcd.hop.gis.geometry.curve.CircularString;
import com.atolcd.hop.gis.geometry.curve.CompoundCurve;
import com.atolcd.hop.gis.geometry.curve.CurveGeometrySupport;
import com.atolcd.hop.gis.geometry.curve.CurvePolygon;
import com.atolcd.hop.gis.geometry.curve.CurveWkbReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HexFormat;
import java.util.List;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class OgrOutputCurveGeometryTest {
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
  void shouldResolveSqlMmCurveGeometryTypeCodes() {
    CurvePolygon polygon = curvePolygon();
    CircularString ring = (CircularString) polygon.getCurveRings().getFirst();
    CompoundCurve compoundCurve = new CompoundCurve(List.of(ring), polygon.getFactory());

    assertEquals(8, OgrOutput.resolveGeometryTypeCode("AUTO", ring));
    assertEquals(9, OgrOutput.resolveGeometryTypeCode("AUTO", compoundCurve));
    assertEquals(10, OgrOutput.resolveGeometryTypeCode("AUTO", polygon));
  }

  @Test
  void shouldEncodeCurvePolygonForOgrWithoutStrokingAndPreserveSrid() throws Exception {
    OgrOutput output = newStandaloneOutput();
    CurvePolygon polygon = curvePolygon();
    polygon.setSRID(2056);

    OgrGeometry ogrGeometry = output.toOgrGeometry(polygon);
    Geometry decoded = CurveGeometrySupport.readWkb(ogrGeometry.ewkb());

    CurvePolygon decodedPolygon = assertInstanceOf(CurvePolygon.class, decoded);
    assertEquals(2056, decodedPolygon.getSRID());
    CircularString ring =
        assertInstanceOf(CircularString.class, decodedPolygon.getCurveRings().getFirst());
    assertEquals(5, ring.getControlPoints().length);
  }

  @Test
  void shouldPreserveForeignClassLoaderCurveGeometry() throws Exception {
    OgrOutput output = newStandaloneOutput();
    URL jtsUrl = Geometry.class.getProtectionDomain().getCodeSource().getLocation();
    URL curveUrl = CurveGeometrySupport.class.getProtectionDomain().getCodeSource().getLocation();

    try (URLClassLoader classLoader =
        new URLClassLoader(new URL[] {jtsUrl, curveUrl}, ClassLoader.getPlatformClassLoader())) {
      Class<?> readerClass =
          Class.forName("com.atolcd.hop.gis.geometry.curve.CurveWkbReader", true, classLoader);
      Object reader = readerClass.getConstructor().newInstance();
      Object foreignCurve = readerClass.getMethod("read", byte[].class).invoke(reader, CURVE_POLYGON_WKB);
      foreignCurve.getClass().getMethod("setSRID", int.class).invoke(foreignCurve, 2056);

      Geometry localGeometry = output.toJtsGeometry(foreignCurve);

      CurvePolygon localPolygon = assertInstanceOf(CurvePolygon.class, localGeometry);
      assertEquals(2056, localPolygon.getSRID());
      assertInstanceOf(CircularString.class, localPolygon.getCurveRings().getFirst());
    }
  }

  private static CurvePolygon curvePolygon() {
    return (CurvePolygon) new CurveWkbReader().read(CURVE_POLYGON_WKB);
  }

  private static OgrOutput newStandaloneOutput() {
    OgrOutputMeta meta = new OgrOutputMeta();
    meta.setDefault();
    PipelineMeta pipelineMeta = new PipelineMeta();
    TransformMeta transformMeta = new TransformMeta("ogr-output-curve-test", meta);
    pipelineMeta.addTransform(transformMeta);
    return new OgrOutput(transformMeta, meta, new OgrOutputData(), 0, pipelineMeta, null);
  }
}
