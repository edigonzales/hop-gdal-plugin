package ch.so.agi.hop.gdal.transform.ogroutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrGeometry;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import ch.so.agi.gdal.ffm.OgrLayerReader;
import ch.so.agi.gdal.ffm.OgrLayerWriteSpec;
import ch.so.agi.gdal.ffm.OgrLayerWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaDate;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

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

  @Test
  void shouldDelayWriterInitializationForAutoGeometryWhenGeometryIsMissing() {
    assertTrue(OgrOutput.shouldDelayWriterInitialization(true, -1, null));
    assertFalse(
        OgrOutput.shouldDelayWriterInitialization(
            true,
            -1,
            new GeometryFactory().createPoint(new Coordinate(1, 1))));
    assertFalse(OgrOutput.shouldDelayWriterInitialization(true, 3, null));
    assertFalse(OgrOutput.shouldDelayWriterInitialization(false, -1, null));
  }

  @Test
  void shouldAcceptNullGeometryValue() throws HopTransformException {
    OgrOutput output = newStandaloneOutput();
    assertNull(output.toJtsGeometry(null));
  }

  @Test
  void shouldConvertForeignLoadedLineStringAndPreserveSrid() throws Exception {
    OgrOutput output = newStandaloneOutput();

    try (ForeignGeometry foreignGeometry = createForeignLineString(2056)) {
      Geometry geometry = output.toJtsGeometry(foreignGeometry.geometry());

      assertInstanceOf(LineString.class, geometry);
      assertEquals(2056, geometry.getSRID());
      assertEquals(0.0, geometry.getCoordinate().x);
      assertEquals(1.0, geometry.getCoordinates()[1].x);
    }
  }

  @Test
  void shouldRejectUnsupportedGeometryValueWithClassLoaderDetails() {
    OgrOutput output = newStandaloneOutput();

    HopTransformException exception =
        assertThrows(HopTransformException.class, () -> output.toJtsGeometry(new Object()));
    assertTrue(exception.getMessage().contains("java.lang.Object"));
    assertTrue(exception.getMessage().contains("classLoader="));
  }

  @Test
  void shouldDefaultCsvGeometryOptionToAsWkt() {
    Map<String, String> options = OgrOutput.resolveEffectiveLayerOptions("CSV", Map.of());

    assertEquals("AS_WKT", options.get("GEOMETRY"));
    assertTrue(OgrOutput.shouldWriteGeometry("CSV", options));
  }

  @Test
  void shouldKeepExplicitCsvGeometryOption() {
    Map<String, String> options =
        OgrOutput.resolveEffectiveLayerOptions("CSV", Map.of("GEOMETRY", "AS_WKT"));

    assertEquals("AS_WKT", options.get("GEOMETRY"));
    assertTrue(OgrOutput.shouldWriteGeometry("CSV", options));
  }

  @Test
  void shouldSuppressGeometryWriteWhenCsvGeometryIsNone() {
    Map<String, String> options =
        OgrOutput.resolveEffectiveLayerOptions("CSV", Map.of("geometry", "NONE"));

    assertEquals("NONE", options.get("geometry"));
    assertFalse(OgrOutput.shouldWriteGeometry("CSV", options));
    assertEquals(0, OgrOutput.resolveEffectiveGeometryTypeCode("CSV", options, 3));
  }

  @Test
  void shouldNotInjectCsvGeometryOptionForOtherFormats() {
    Map<String, String> options = OgrOutput.resolveEffectiveLayerOptions("GPKG", Map.of());

    assertTrue(options.isEmpty());
    assertTrue(OgrOutput.shouldWriteGeometry("GPKG", options));
    assertEquals(3, OgrOutput.resolveEffectiveGeometryTypeCode("GPKG", options, 3));
  }

  @Test
  void shouldBufferRowsUntilFirstGeometryAndPreserveOrder() throws Exception {
    RecordingDataSource dataSource = new RecordingDataSource();
    TestOgrOutput output = newBufferedOutput(dataSource);
    GeometryFactory geometryFactory = new GeometryFactory();

    output.setInputRowMeta(createInputRowMeta());
    output.setRows(
        List.of(
            new Object[] {1L, null},
            new Object[] {2L, geometryFactory.createPoint(new Coordinate(2600000, 1200000))}));

    runToCompletion(output);

    assertEquals(1, dataSource.openWriterCalls);
    assertNotNull(dataSource.lastWriteSpec);
    assertEquals(1, dataSource.lastWriteSpec.geometryTypeCode());
    assertEquals(2, dataSource.writer.features.size());
    assertEquals(List.of(1L, 2L), featureIds(dataSource.writer.features));
    assertNull(dataSource.writer.features.get(0).geometry());
    assertInstanceOf(Point.class, decodeGeometry(dataSource.writer.features.get(1).geometry()));
    assertEquals(2, output.getLinesOutput());
    assertTrue(dataSource.closed);
    assertTrue(dataSource.writer.closed);
  }

  @Test
  void shouldWriteAllRowsWhenEveryGeometryIsNull() throws Exception {
    RecordingDataSource dataSource = new RecordingDataSource();
    TestOgrOutput output = newBufferedOutput(dataSource);

    output.setInputRowMeta(createInputRowMeta());
    output.setRows(List.of(new Object[] {1L, null}, new Object[] {2L, null}));

    runToCompletion(output);

    assertEquals(1, dataSource.openWriterCalls);
    assertNotNull(dataSource.lastWriteSpec);
    assertEquals(0, dataSource.lastWriteSpec.geometryTypeCode());
    assertEquals(2, dataSource.writer.features.size());
    assertTrue(dataSource.writer.features.stream().allMatch(feature -> feature.geometry() == null));
    assertEquals(List.of(1L, 2L), featureIds(dataSource.writer.features));
    assertEquals(2, output.getLinesOutput());
  }

  private static OgrOutput newStandaloneOutput() {
    OgrOutputMeta meta = new OgrOutputMeta();
    meta.setDefault();
    PipelineMeta pipelineMeta = new PipelineMeta();
    TransformMeta transformMeta = new TransformMeta("ogr-output-test", meta);
    pipelineMeta.addTransform(transformMeta);
    return new OgrOutput(
        transformMeta,
        meta,
        new OgrOutputData(),
        0,
        pipelineMeta,
        null);
  }

  private static TestOgrOutput newBufferedOutput(RecordingDataSource dataSource) {
    OgrOutputMeta meta = new OgrOutputMeta();
    meta.setDefault();
    meta.setFileName("test.gpkg");
    meta.setFormat("GPKG");
    meta.setGeometryField("geom");
    meta.setWriteMode("OVERWRITE");

    return new TestOgrOutput(meta, dataSource);
  }

  private static RowMeta createInputRowMeta() {
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaInteger("id"));
    rowMeta.addValueMeta(new ValueMetaString("geom"));
    return rowMeta;
  }

  private static void runToCompletion(TestOgrOutput output) throws HopException {
    while (output.processRow()) {
      // drain all queued rows
    }
  }

  private static List<Long> featureIds(List<OgrFeature> features) {
    List<Long> ids = new ArrayList<>(features.size());
    for (OgrFeature feature : features) {
      ids.add((Long) feature.attributes().get("id"));
    }
    return ids;
  }

  private static Geometry decodeGeometry(OgrGeometry ogrGeometry) throws ParseException {
    Geometry geometry = new WKBReader().read(ogrGeometry.ewkb());
    ogrGeometry.srid().ifPresent(geometry::setSRID);
    return geometry;
  }

  private static ForeignGeometry createForeignLineString(int srid) throws Exception {
    URL jtsUrl = Geometry.class.getProtectionDomain().getCodeSource().getLocation();
    URLClassLoader classLoader =
        new URLClassLoader(new URL[] {jtsUrl}, ClassLoader.getPlatformClassLoader());

    Class<?> coordinateClass = Class.forName("org.locationtech.jts.geom.Coordinate", true, classLoader);
    Object coordinates = Array.newInstance(coordinateClass, 2);
    Array.set(coordinates, 0, coordinateClass.getConstructor(double.class, double.class).newInstance(0d, 0d));
    Array.set(coordinates, 1, coordinateClass.getConstructor(double.class, double.class).newInstance(1d, 1d));

    Class<?> geometryFactoryClass =
        Class.forName("org.locationtech.jts.geom.GeometryFactory", true, classLoader);
    Object geometryFactory = geometryFactoryClass.getConstructor().newInstance();
    Object lineString =
        geometryFactoryClass.getMethod("createLineString", coordinates.getClass()).invoke(geometryFactory, coordinates);
    lineString.getClass().getMethod("setSRID", int.class).invoke(lineString, srid);

    return new ForeignGeometry(classLoader, lineString);
  }

  private record ForeignGeometry(URLClassLoader classLoader, Object geometry) implements AutoCloseable {
    @Override
    public void close() throws Exception {
      classLoader.close();
    }
  }

  private static final class RecordingDataSource implements OgrDataSource {
    private final RecordingLayerWriter writer = new RecordingLayerWriter();
    private OgrLayerWriteSpec lastWriteSpec;
    private int openWriterCalls;
    private boolean closed;

    @Override
    public List<OgrLayerDefinition> listLayers() {
      return List.of();
    }

    @Override
    public OgrLayerReader openReader(String layerName, Map<String, String> readerOptions) {
      throw new UnsupportedOperationException("Reader is not used by exporter tests");
    }

    @Override
    public OgrLayerWriter openWriter(OgrLayerWriteSpec writeSpec) {
      this.lastWriteSpec = writeSpec;
      this.openWriterCalls++;
      return writer;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  private static final class RecordingLayerWriter implements OgrLayerWriter {
    private final List<OgrFeature> features = new ArrayList<>();
    private boolean closed;

    @Override
    public void write(OgrFeature feature) {
      features.add(feature);
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  private static final class TestOgrOutput extends OgrOutput {
    private final OgrDataSource dataSource;
    private final Deque<Object[]> rows = new ArrayDeque<>();

    private TestOgrOutput(OgrOutputMeta meta, OgrDataSource dataSource) {
      this(meta, dataSource, createTransformContext(meta));
    }

    private TestOgrOutput(
        OgrOutputMeta meta, OgrDataSource dataSource, TransformContext transformContext) {
      super(
          transformContext.transformMeta(),
          meta,
          new OgrOutputData(),
          0,
          transformContext.pipelineMeta(),
          null);
      this.dataSource = dataSource;
    }

    private void setRows(List<Object[]> inputRows) {
      rows.clear();
      rows.addAll(inputRows);
    }

    @Override
    public Object[] getRow() {
      return rows.pollFirst();
    }

    @Override
    protected OgrDataSource createDataSource(
        String resolvedFileName,
        String resolvedFormat,
        ch.so.agi.gdal.ffm.OgrWriteMode writeMode,
        Map<String, String> datasetOptions) {
      return dataSource;
    }

    @Override
    public boolean isBasic() {
      return false;
    }
  }

  private static TransformContext createTransformContext(OgrOutputMeta meta) {
    PipelineMeta pipelineMeta = new PipelineMeta();
    TransformMeta transformMeta = new TransformMeta("ogr-output-test", meta);
    pipelineMeta.addTransform(transformMeta);
    return new TransformContext(pipelineMeta, transformMeta);
  }

  private record TransformContext(PipelineMeta pipelineMeta, TransformMeta transformMeta) {}
}
