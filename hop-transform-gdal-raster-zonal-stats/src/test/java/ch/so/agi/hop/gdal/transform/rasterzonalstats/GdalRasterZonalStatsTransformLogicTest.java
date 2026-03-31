package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.DatasetRefType;
import ch.so.agi.hop.gdal.raster.core.RasterGdalClient;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

class GdalRasterZonalStatsTransformLogicTest {

  @Test
  void rowOutputShouldAppendScalarAndJsonStats() throws Exception {
    RecordingRasterGdalClient gdalClient = new RecordingRasterGdalClient();
    RecordingRowZoneDatasetFactory zoneDatasetFactory = new RecordingRowZoneDatasetFactory();
    TestRowStatsReader statsReader =
        new TestRowStatsReader(Map.of("mean", 42.5D, "values", List.of(40, 45)));
    TestGdalRasterZonalStatsTransform transform =
        newRowOutputTransform(gdalClient, zoneDatasetFactory, statsReader);

    transform.setInputRowMeta(createInputRowMeta());
    transform.setRows(
        List.<Object[]>of(
            new Object[] {
              7L,
              new GeometryFactory().createPoint(new Coordinate(2600000, 1200000)),
              "/tmp/clipped.tif"
            }));

    runToCompletion(transform);

    assertEquals(1, transform.outputRows.size());
    Object[] outputRow = transform.outputRows.getFirst();
    assertEquals(7L, outputRow[0]);
    assertInstanceOf(Point.class, outputRow[1]);
    assertEquals("/tmp/clipped.tif", outputRow[2]);
    assertEquals(42.5D, outputRow[3]);
    assertEquals("[40,45]", outputRow[4]);
    assertEquals(Boolean.TRUE, outputRow[5]);
    assertEquals("OK", outputRow[6]);
    assertTrue(transform.lastOutputRowMeta.indexOfValue("gdal_zs_success") >= 0);
    assertEquals(1, gdalClient.rasterZonalStatsCalls);
    assertEquals("/tmp/clipped.tif", gdalClient.lastRasterInput.value());
    assertEquals("/vsimem/row-zones.geojson", gdalClient.lastZonesInput.value());
    assertEquals("id", zoneDatasetFactory.lastAttributes.keySet().iterator().next());
    assertEquals(7L, zoneDatasetFactory.lastAttributes.get("id"));
    assertEquals("/tmp/clipped.tif", zoneDatasetFactory.lastAttributes.get("raster_path"));
    assertInstanceOf(Point.class, zoneDatasetFactory.lastGeometry);
  }

  @Test
  void rowOutputShouldAddBandPrefixesForMultipleBands() throws Exception {
    RecordingRasterGdalClient gdalClient = new RecordingRasterGdalClient();
    TestGdalRasterZonalStatsTransform transform =
        newRowOutputTransform(
            gdalClient,
            new RecordingRowZoneDatasetFactory(),
            new TestRowStatsReader(Map.of("b1_mean", 12.0D, "b3_mean", 34.0D)));
    transform.meta.setStats("mean");
    transform.meta.setBands("1,3");

    RowMeta rowMeta = createInputRowMeta();
    transform.setInputRowMeta(rowMeta);

    RowMeta outputMeta = (RowMeta) rowMeta.clone();
    transform.meta.getFields(outputMeta, "origin", null, null, null, null);
    assertTrue(outputMeta.indexOfValue("zs_b1_mean") >= 0);
    assertTrue(outputMeta.indexOfValue("zs_b3_mean") >= 0);

    transform.setRows(
        List.<Object[]>of(
            new Object[] {
              1L,
              new GeometryFactory().createPoint(new Coordinate(0, 0)),
              "/tmp/clipped.tif"
            }));
    runToCompletion(transform);

    Object[] outputRow = transform.outputRows.getFirst();
    assertEquals(12.0D, outputRow[3]);
    assertEquals(34.0D, outputRow[4]);
  }

  @Test
  void rowOutputShouldMarkRowFailedWhenGeometryIsNullAndFailOnErrorIsDisabled() throws Exception {
    RecordingRasterGdalClient gdalClient = new RecordingRasterGdalClient();
    TestGdalRasterZonalStatsTransform transform =
        newRowOutputTransform(
            gdalClient,
            new RecordingRowZoneDatasetFactory(),
            new TestRowStatsReader(Map.of("mean", 42.5D)));
    transform.meta.setFailOnError(false);

    transform.setInputRowMeta(createInputRowMeta());
    transform.setRows(List.<Object[]>of(new Object[] {7L, null, "/tmp/clipped.tif"}));

    runToCompletion(transform);

    assertEquals(1, transform.outputRows.size());
    Object[] outputRow = transform.outputRows.getFirst();
    assertNull(outputRow[3]);
    assertNull(outputRow[4]);
    assertEquals(Boolean.FALSE, outputRow[5]);
    assertEquals("Geometry field resolved to null", outputRow[6]);
    assertEquals(0, gdalClient.rasterZonalStatsCalls);
  }

  @Test
  void rowOutputShouldPreserveBusinessFieldsAndRenameUpstreamTechnicalFields() throws Exception {
    RecordingRasterGdalClient gdalClient = new RecordingRasterGdalClient();
    RecordingRowZoneDatasetFactory zoneDatasetFactory = new RecordingRowZoneDatasetFactory();
    TestGdalRasterZonalStatsTransform transform =
        newRowOutputTransform(
            gdalClient, zoneDatasetFactory, new TestRowStatsReader(Map.of("mean", 16.4D)));
    transform.meta.setInputField("gdal_output");

    RowMeta rowMeta = createBusinessInputRowMeta();
    transform.setInputRowMeta(rowMeta);
    transform.setRows(
        List.<Object[]>of(
            new Object[] {
              2622L,
              "0ad71992-0df1-492c-8875-b7348f23187b",
              "Gebaeude",
              new GeometryFactory().createPoint(new Coordinate(2600000, 1200000)),
              Boolean.TRUE,
              "OK",
              "/tmp/clipped.tif"
            }));

    runToCompletion(transform);

    Object[] outputRow = transform.outputRows.getFirst();
    assertEquals(2622L, outputRow[transform.lastOutputRowMeta.indexOfValue("fid")]);
    assertEquals(
        "0ad71992-0df1-492c-8875-b7348f23187b",
        outputRow[transform.lastOutputRowMeta.indexOfValue("T_Ili_Tid")]);
    assertEquals("Gebaeude", outputRow[transform.lastOutputRowMeta.indexOfValue("art_txt")]);
    assertInstanceOf(Point.class, outputRow[transform.lastOutputRowMeta.indexOfValue("geom")]);
    assertEquals(
        Boolean.TRUE, outputRow[transform.lastOutputRowMeta.indexOfValue("gdal_upstream_success")]);
    assertEquals("OK", outputRow[transform.lastOutputRowMeta.indexOfValue("gdal_upstream_message")]);
    assertEquals(
        "/tmp/clipped.tif",
        outputRow[transform.lastOutputRowMeta.indexOfValue("gdal_upstream_output")]);
    assertEquals(16.4D, outputRow[transform.lastOutputRowMeta.indexOfValue("zs_mean")]);
    assertEquals(Boolean.TRUE, outputRow[transform.lastOutputRowMeta.indexOfValue("gdal_zs_success")]);
    assertTrue(transform.lastOutputRowMeta.indexOfValue("gdal_output") < 0);
    assertTrue(transform.lastOutputRowMeta.indexOfValue("gdal_success") < 0);
    assertEquals(3, zoneDatasetFactory.lastAttributes.size());
    assertEquals(2622L, zoneDatasetFactory.lastAttributes.get("fid"));
    assertEquals(
        "0ad71992-0df1-492c-8875-b7348f23187b", zoneDatasetFactory.lastAttributes.get("T_Ili_Tid"));
    assertEquals("Gebaeude", zoneDatasetFactory.lastAttributes.get("art_txt"));
  }

  @Test
  void rowOutputShouldInitializeSchemaAfterFirstRowProvidesInputMeta() throws Exception {
    RecordingRasterGdalClient gdalClient = new RecordingRasterGdalClient();
    TestGdalRasterZonalStatsTransform transform =
        newRowOutputTransform(
            gdalClient,
            new RecordingRowZoneDatasetFactory(),
            new TestRowStatsReader(Map.of("mean", 12.5D, "values", List.of(12, 13))));
    transform.setLateInputRowMeta(createInputRowMeta());
    transform.setRows(
        List.<Object[]>of(
            new Object[] {
              7L,
              new GeometryFactory().createPoint(new Coordinate(2600000, 1200000)),
              "/tmp/clipped.tif"
            }));

    runToCompletion(transform);

    Object[] outputRow = transform.outputRows.getFirst();
    assertEquals(7L, outputRow[transform.lastOutputRowMeta.indexOfValue("id")]);
    assertEquals("/tmp/clipped.tif", outputRow[transform.lastOutputRowMeta.indexOfValue("raster_path")]);
    assertEquals(12.5D, outputRow[transform.lastOutputRowMeta.indexOfValue("zs_mean")]);
    assertEquals("[12,13]", outputRow[transform.lastOutputRowMeta.indexOfValue("zs_values")]);
    assertEquals(Boolean.TRUE, outputRow[transform.lastOutputRowMeta.indexOfValue("gdal_zs_success")]);
  }

  @Test
  void rowOutputShouldFailFastWhenReaderReturnsNonNumericScalarValue() {
    RecordingRasterGdalClient gdalClient = new RecordingRasterGdalClient();
    TestGdalRasterZonalStatsTransform transform =
        newRowOutputTransform(
            gdalClient,
            new RecordingRowZoneDatasetFactory(),
            new TestRowStatsReader(Map.of("mean", "not-a-number")));
    transform.setInputRowMeta(createInputRowMeta());
    transform.setRows(
        List.<Object[]>of(
            new Object[] {
              7L,
              new GeometryFactory().createPoint(new Coordinate(2600000, 1200000)),
              "/tmp/clipped.tif"
            }));

    assertThrows(HopException.class, transform::processRow);
  }

  private static TestGdalRasterZonalStatsTransform newRowOutputTransform(
      RecordingRasterGdalClient gdalClient,
      RecordingRowZoneDatasetFactory zoneDatasetFactory,
      TestRowStatsReader statsReader) {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setOutputMode(GdalRasterZonalStatsMeta.OUTPUT_MODE_ROW_FIELDS);
    meta.setZonesInputMode(GdalRasterZonalStatsMeta.ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD);
    meta.setGeometryField("geom");
    meta.setInputValueMode("FIELD");
    meta.setInputField("raster_path");
    meta.setStats("mean,values");
    TransformContext context = createTransformContext(meta);
    return new TestGdalRasterZonalStatsTransform(meta, context, gdalClient, zoneDatasetFactory, statsReader);
  }

  private static RowMeta createInputRowMeta() {
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaInteger("id"));
    rowMeta.addValueMeta(new ValueMetaString("geom"));
    rowMeta.addValueMeta(new ValueMetaString("raster_path"));
    return rowMeta;
  }

  private static RowMeta createBusinessInputRowMeta() {
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaInteger("fid"));
    rowMeta.addValueMeta(new ValueMetaString("T_Ili_Tid"));
    rowMeta.addValueMeta(new ValueMetaString("art_txt"));
    rowMeta.addValueMeta(new ValueMetaString("geom"));
    rowMeta.addValueMeta(new ValueMetaBoolean("gdal_success"));
    rowMeta.addValueMeta(new ValueMetaString("gdal_message"));
    rowMeta.addValueMeta(new ValueMetaString("gdal_output"));
    return rowMeta;
  }

  private static void runToCompletion(TestGdalRasterZonalStatsTransform transform) throws HopException {
    while (transform.processRow()) {
      // drain rows
    }
  }

  private static TransformContext createTransformContext(GdalRasterZonalStatsMeta meta) {
    PipelineMeta pipelineMeta = new PipelineMeta();
    TransformMeta transformMeta = new TransformMeta("zonal-stats-test", meta);
    pipelineMeta.addTransform(transformMeta);
    return new TransformContext(pipelineMeta, transformMeta);
  }

  private record TransformContext(PipelineMeta pipelineMeta, TransformMeta transformMeta) {}

  private static final class RecordingRasterGdalClient implements RasterGdalClient {
    private int rasterZonalStatsCalls;
    private DatasetRef lastRasterInput;
    private DatasetRef lastZonesInput;

    @Override
    public String rasterInfo(DatasetRef input, RemoteAccessSpec remoteAccess, List<String> args) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rasterConvert(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rasterClip(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rasterReproject(
        DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rasterResize(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rasterMosaic(
        List<DatasetRef> inputs, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rasterZonalStats(
        DatasetRef rasterInput,
        DatasetRef zonesInput,
        DatasetRef output,
        RemoteAccessSpec remoteAccess,
        List<String> args) {
      this.rasterZonalStatsCalls++;
      this.lastRasterInput = rasterInput;
      this.lastZonesInput = zonesInput;
    }

    @Override
    public void vectorRasterize(
        DatasetRef vectorInput, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args) {
      throw new UnsupportedOperationException();
    }
  }

  private static final class RecordingRowZoneDatasetFactory
      implements GdalRasterZonalStatsTransform.RowZoneDatasetFactory {
    private Geometry lastGeometry;
    private Map<String, Object> lastAttributes = Map.of();

    @Override
    public GdalRasterZonalStatsTransform.PreparedZoneDataset create(
        Geometry geometry, String layerName, Map<String, Object> attributes) {
      this.lastGeometry = geometry;
      this.lastAttributes = new LinkedHashMap<>(attributes);
      return new GdalRasterZonalStatsTransform.PreparedZoneDataset(
          new DatasetRef(DatasetRefType.GDAL_VSI, "/vsimem/row-zones.geojson"),
          layerName,
          List.copyOf(attributes.keySet()));
    }
  }

  private static final class TestRowStatsReader implements GdalRasterZonalStatsTransform.RowStatsReader {
    private final Map<String, Object> values;

    private TestRowStatsReader(Map<String, Object> values) {
      this.values = values;
    }

    @Override
    public Map<String, Object> read(DatasetRef outputDataset) {
      return values;
    }
  }

  private static final class TestGdalRasterZonalStatsTransform extends GdalRasterZonalStatsTransform {
    private final Deque<Object[]> rows = new ArrayDeque<>();
    private final List<Object[]> outputRows = new ArrayList<>();
    private final GdalRasterZonalStatsMeta meta;
    private IRowMeta lastOutputRowMeta;
    private RowMeta lateInputRowMeta;

    private TestGdalRasterZonalStatsTransform(
        GdalRasterZonalStatsMeta meta,
        TransformContext context,
        RasterGdalClient gdalClient,
        RowZoneDatasetFactory zoneDatasetFactory,
        RowStatsReader rowStatsReader) {
      super(
          context.transformMeta(),
          meta,
          new GdalRasterZonalStatsData(),
          0,
          context.pipelineMeta(),
          null,
          gdalClient,
          new OgrZonesMetadataInspector(),
          zoneDatasetFactory,
          rowStatsReader);
      this.meta = meta;
    }

    private void setRows(List<Object[]> inputRows) {
      rows.clear();
      rows.addAll(inputRows);
    }

    private void setLateInputRowMeta(RowMeta inputRowMeta) {
      this.lateInputRowMeta = inputRowMeta;
    }

    @Override
    public Object[] getRow() {
      Object[] row = rows.pollFirst();
      if (row != null && lateInputRowMeta != null && getInputRowMeta() == null) {
        setInputRowMeta(lateInputRowMeta);
        lateInputRowMeta = null;
      }
      return row;
    }

    @Override
    public void putRow(org.apache.hop.core.row.IRowMeta rowMeta, Object[] row) {
      lastOutputRowMeta = rowMeta.clone();
      outputRows.add(row);
    }

    @Override
    protected DatasetRef createRowModeOutputDatasetRef() {
      return new DatasetRef(DatasetRefType.GDAL_VSI, "/vsimem/row-stats-output.geojson");
    }

    @Override
    public boolean isBasic() {
      return false;
    }
  }
}
