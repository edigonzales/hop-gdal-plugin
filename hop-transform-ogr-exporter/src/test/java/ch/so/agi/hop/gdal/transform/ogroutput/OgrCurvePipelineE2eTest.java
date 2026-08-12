package ch.so.agi.hop.gdal.transform.ogroutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrGeometry;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import ch.so.agi.gdal.ffm.OgrLayerReader;
import ch.so.agi.gdal.ffm.OgrLayerWriteSpec;
import ch.so.agi.gdal.ffm.OgrLayerWriter;
import ch.so.agi.gdal.ffm.OgrWriteMode;
import ch.so.agi.hop.gdal.transform.ogrinput.OgrInputMeta;
import com.atolcd.hop.gis.geometry.curve.CircularString;
import com.atolcd.hop.gis.geometry.curve.CompoundCurve;
import com.atolcd.hop.gis.geometry.curve.CurveGeometrySupport;
import com.atolcd.hop.gis.geometry.curve.CurvePolygon;
import com.atolcd.hop.gis.geometry.curve.MultiCurve;
import com.atolcd.hop.gis.geometry.curve.MultiSurface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.HopEnvironment;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineHopMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.engines.local.LocalPipelineEngine;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

@EnabledIfSystemProperty(named = "hop.gdal.native.e2e", matches = "true")
class OgrCurvePipelineE2eTest {
  private static final GeometryFactory FACTORY = new GeometryFactory();
  private static final String SOURCE_LAYER = "source_curves";
  private static final String TARGET_LAYER = "roundtrip_curves";

  @TempDir Path tempDir;

  @BeforeAll
  static void initializeHop() throws Exception {
    HopEnvironment.init();
  }

  @Test
  void shouldPreserveAllSupportedTwoDimensionalSqlMmCurveTypesThroughHopAndGdal()
      throws Exception {
    for (CurveCase curveCase : curveCases()) {
      Path source = tempDir.resolve(curveCase.name() + "-source.gpkg");
      Path target = tempDir.resolve(curveCase.name() + "-target.gpkg");

      writeSourceGeoPackage(source, curveCase);
      runHopPipeline(source, target, curveCase.name());
      verifyTargetGeoPackage(target, curveCase);
    }
  }

  private static List<CurveCase> curveCases() {
    CircularString circularString =
        new CircularString(
            new Coordinate[] {
              new Coordinate(0, 0), new Coordinate(2, 2), new Coordinate(4, 0)
            },
            FACTORY);

    LineString compoundLine =
        FACTORY.createLineString(
            new Coordinate[] {new Coordinate(0, 0), new Coordinate(4, 0)});
    CircularString compoundArc =
        new CircularString(
            new Coordinate[] {
              new Coordinate(4, 0), new Coordinate(6, 2), new Coordinate(4, 4)
            },
            FACTORY);
    CompoundCurve compoundCurve = new CompoundCurve(List.of(compoundLine, compoundArc), FACTORY);

    CircularString curveRing =
        new CircularString(
            new Coordinate[] {
              new Coordinate(0, 0),
              new Coordinate(4, 0),
              new Coordinate(4, 4),
              new Coordinate(0, 4),
              new Coordinate(0, 0)
            },
            FACTORY);
    CurvePolygon curvePolygon = new CurvePolygon(List.of(curveRing), FACTORY);

    LineString multiCurveLine =
        FACTORY.createLineString(
            new Coordinate[] {new Coordinate(10, 0), new Coordinate(12, 2)});
    CircularString multiCurveArc =
        new CircularString(
            new Coordinate[] {
              new Coordinate(20, 0), new Coordinate(22, 2), new Coordinate(24, 0)
            },
            FACTORY);
    MultiCurve multiCurve = new MultiCurve(List.of(multiCurveLine, multiCurveArc), FACTORY);

    Polygon linearPolygon =
        FACTORY.createPolygon(
            new Coordinate[] {
              new Coordinate(10, 10),
              new Coordinate(14, 10),
              new Coordinate(14, 14),
              new Coordinate(10, 14),
              new Coordinate(10, 10)
            });
    MultiSurface multiSurface = new MultiSurface(List.of(linearPolygon, curvePolygon), FACTORY);

    return List.of(
        new CurveCase("circularstring", 8, circularString, OgrCurvePipelineE2eTest::verifyCircularString),
        new CurveCase("compoundcurve", 9, compoundCurve, OgrCurvePipelineE2eTest::verifyCompoundCurve),
        new CurveCase("curvepolygon", 10, curvePolygon, OgrCurvePipelineE2eTest::verifyCurvePolygon),
        new CurveCase("multicurve", 11, multiCurve, OgrCurvePipelineE2eTest::verifyMultiCurve),
        new CurveCase("multisurface", 12, multiSurface, OgrCurvePipelineE2eTest::verifyMultiSurface));
  }

  private static void writeSourceGeoPackage(Path source, CurveCase curveCase) throws Exception {
    Files.deleteIfExists(source);
    try (OgrDataSource dataSource = Ogr.create(source, "GPKG", OgrWriteMode.FAIL_IF_EXISTS);
        OgrLayerWriter writer =
            dataSource.openWriter(
                new OgrLayerWriteSpec(SOURCE_LAYER, curveCase.ogrType(), List.of()))) {
      writer.write(
          new OgrFeature(
              1L,
              Map.of(),
              OgrGeometry.fromWkb(CurveGeometrySupport.writeWkb(curveCase.geometry()))));
    }
  }

  private static void runHopPipeline(Path source, Path target, String caseName) throws Exception {
    Files.deleteIfExists(target);

    OgrInputMeta inputMeta = new OgrInputMeta();
    inputMeta.setDefault();
    inputMeta.setFileName(source.toString());
    inputMeta.setLayerName(SOURCE_LAYER);
    inputMeta.setIncludeFid(false);
    inputMeta.setGeometryFieldName("geometry");

    OgrOutputMeta outputMeta = new OgrOutputMeta();
    outputMeta.setDefault();
    outputMeta.setFileName(target.toString());
    outputMeta.setFormat("GPKG");
    outputMeta.setLayerName(TARGET_LAYER);
    outputMeta.setGeometryField("geometry");
    outputMeta.setWriteMode("FAIL_IF_EXISTS");
    outputMeta.setForceGeometryType("AUTO");

    TransformMeta inputTransform =
        new TransformMeta("OGR_INPUT_TRANSFORM", "ogr-input-" + caseName, inputMeta);
    TransformMeta outputTransform =
        new TransformMeta("OGR_OUTPUT_TRANSFORM", "ogr-output-" + caseName, outputMeta);

    PipelineMeta pipelineMeta = new PipelineMeta();
    pipelineMeta.setName("curve-e2e-" + caseName);
    pipelineMeta.addTransform(inputTransform);
    pipelineMeta.addTransform(outputTransform);
    pipelineMeta.addPipelineHop(new PipelineHopMeta(inputTransform, outputTransform));

    Pipeline pipeline = new LocalPipelineEngine(pipelineMeta);
    pipeline.prepareExecution();
    pipeline.startThreads();
    pipeline.waitUntilFinished();

    assertNotNull(pipeline.getResult());
    assertEquals(
        0L,
        pipeline.getResult().getNrErrors(),
        "Hop pipeline failed for SQL/MM curve case " + caseName);
  }

  private static void verifyTargetGeoPackage(Path target, CurveCase curveCase) throws Exception {
    try (OgrDataSource dataSource = Ogr.open(target)) {
      List<OgrLayerDefinition> layers = dataSource.listLayers();
      assertEquals(1, layers.size(), "Unexpected output layer count for " + curveCase.name());

      OgrLayerDefinition layer = layers.getFirst();
      assertEquals(TARGET_LAYER, layer.name());
      assertEquals(
          curveCase.ogrType(),
          baseType(layer.geometryType()),
          "OGR layer geometry type changed for " + curveCase.name());

      try (OgrLayerReader reader = dataSource.openReader(layer.name(), Map.of())) {
        Iterator<OgrFeature> iterator = reader.iterator();
        OgrFeature feature = iterator.next();
        assertNotNull(feature.geometry());

        Geometry decoded = CurveGeometrySupport.readWkb(feature.geometry().ewkb());
        curveCase.verifier().verify(decoded);
        assertFalse(iterator.hasNext(), "Expected exactly one output feature for " + curveCase.name());
      }
    }
  }

  private static int baseType(int rawType) {
    int type = rawType & 0x1fff_ffff;
    return type >= 1000 ? type % 1000 : type;
  }

  private static void verifyCircularString(Geometry geometry) {
    CircularString circularString = assertInstanceOf(CircularString.class, geometry);
    assertEquals(3, circularString.getControlPoints().length);
    assertEquals(2.0, circularString.getControlPoints()[1].x);
    assertEquals(2.0, circularString.getControlPoints()[1].y);
  }

  private static void verifyCompoundCurve(Geometry geometry) {
    CompoundCurve compoundCurve = assertInstanceOf(CompoundCurve.class, geometry);
    assertEquals(2, compoundCurve.getComponents().size());
    assertInstanceOf(LineString.class, compoundCurve.getComponents().get(0));
    CircularString arc =
        assertInstanceOf(CircularString.class, compoundCurve.getComponents().get(1));
    assertEquals(3, arc.getControlPoints().length);
  }

  private static void verifyCurvePolygon(Geometry geometry) {
    CurvePolygon curvePolygon = assertInstanceOf(CurvePolygon.class, geometry);
    assertEquals(1, curvePolygon.getCurveRings().size());
    CircularString ring =
        assertInstanceOf(CircularString.class, curvePolygon.getCurveRings().getFirst());
    assertEquals(5, ring.getControlPoints().length);
  }

  private static void verifyMultiCurve(Geometry geometry) {
    MultiCurve multiCurve = assertInstanceOf(MultiCurve.class, geometry);
    assertEquals(2, multiCurve.getCurves().size());
    assertInstanceOf(LineString.class, multiCurve.getCurves().get(0));
    CircularString arc = assertInstanceOf(CircularString.class, multiCurve.getCurves().get(1));
    assertEquals(3, arc.getControlPoints().length);
  }

  private static void verifyMultiSurface(Geometry geometry) {
    MultiSurface multiSurface = assertInstanceOf(MultiSurface.class, geometry);
    assertEquals(2, multiSurface.getSurfaces().size());
    assertInstanceOf(Polygon.class, multiSurface.getSurfaces().get(0));
    CurvePolygon curvePolygon =
        assertInstanceOf(CurvePolygon.class, multiSurface.getSurfaces().get(1));
    assertInstanceOf(CircularString.class, curvePolygon.getCurveRings().getFirst());
  }

  private record CurveCase(
      String name, int ogrType, Geometry geometry, GeometryVerifier verifier) {}

  @FunctionalInterface
  private interface GeometryVerifier {
    void verify(Geometry geometry);
  }
}
