package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaString;
import org.junit.jupiter.api.Test;

class GdalRasterZonalStatsMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();

    assertEquals("GPKG", meta.getOutputFormat());
    assertEquals("FAIL_IF_EXISTS", meta.getOutputWriteMode());
    assertEquals("mean", meta.getStats());
    assertEquals(GdalRasterZonalStatsMeta.OUTPUT_MODE_VECTOR_DATASET, meta.getOutputMode());
    assertEquals(GdalRasterZonalStatsMeta.ZONES_INPUT_MODE_DATASET_LAYER, meta.getZonesInputMode());
  }

  @Test
  void checkRequiresZonesDataset() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setOutputValue("/tmp/out.gpkg");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals("Zones dataset is required", remarks.getFirst().getText());
  }

  @Test
  void checkRejectsUnsupportedWriteMode() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setZonesValue("/tmp/zones.gpkg");
    meta.setOutputValue("/tmp/out.gpkg");
    meta.setOutputWriteMode("APPEND");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals("Raster zonal stats write mode is not supported: APPEND", remarks.getFirst().getText());
  }

  @Test
  void checkRejectsHttpUrlOutputMode() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setZonesValue("/tmp/zones.gpkg");
    meta.setOutputValue("/tmp/out.gpkg");
    meta.setOutputSourceMode("HTTP_URL");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals(
        "HTTP/HTTPS output is not supported; use LOCAL_FILE or GDAL_VSI",
        remarks.getFirst().getText());
  }

  @Test
  void rowOutputMetadataAddsExpectedStatsFields() throws Exception {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setOutputMode(GdalRasterZonalStatsMeta.OUTPUT_MODE_ROW_FIELDS);
    meta.setZonesInputMode(GdalRasterZonalStatsMeta.ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD);
    meta.setGeometryField("geom");
    meta.setStats("mean,values");
    meta.setBands("1,3");

    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaString("geom"));
    rowMeta.addValueMeta(new ValueMetaString("raster_path"));
    rowMeta.addValueMeta(new ValueMetaString("gdal_output"));
    rowMeta.addValueMeta(new ValueMetaBoolean("gdal_success"));

    meta.getFields(rowMeta, "origin", null, null, null, null);

    assertTrue(rowMeta.indexOfValue("zs_b1_mean") >= 0);
    assertTrue(rowMeta.indexOfValue("zs_b1_values") >= 0);
    assertTrue(rowMeta.indexOfValue("zs_b3_mean") >= 0);
    assertTrue(rowMeta.indexOfValue("zs_b3_values") >= 0);
    assertTrue(rowMeta.indexOfValue("gdal_upstream_output") >= 0);
    assertTrue(rowMeta.indexOfValue("gdal_upstream_success") >= 0);
    assertTrue(rowMeta.indexOfValue("gdal_output") < 0);
    assertTrue(rowMeta.indexOfValue("gdal_success") < 0);
    assertTrue(rowMeta.indexOfValue("gdal_zs_success") >= 0);
  }

  @Test
  void rowOutputCheckRequiresGeometryField() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setOutputMode(GdalRasterZonalStatsMeta.OUTPUT_MODE_ROW_FIELDS);
    meta.setZonesInputMode(GdalRasterZonalStatsMeta.ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD);
    meta.setInputValue("/tmp/in.tif");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals("Geometry field is required", remarks.getFirst().getText());
  }

  @Test
  void rowOutputCheckRejectsMissingRasterInputField() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setOutputMode(GdalRasterZonalStatsMeta.OUTPUT_MODE_ROW_FIELDS);
    meta.setZonesInputMode(GdalRasterZonalStatsMeta.ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD);
    meta.setGeometryField("geom");
    meta.setInputValueMode("FIELD");
    meta.setInputField("raster_path");

    RowMeta prev = new RowMeta();
    prev.addValueMeta(new ValueMetaString("geom"));

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, prev, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals("Input field was not found: raster_path", remarks.getFirst().getText());
  }

  @Test
  void rowOutputCheckRejectsFieldCollisions() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setOutputMode(GdalRasterZonalStatsMeta.OUTPUT_MODE_ROW_FIELDS);
    meta.setZonesInputMode(GdalRasterZonalStatsMeta.ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD);
    meta.setGeometryField("geom");
    meta.setInputValue("/tmp/in.tif");

    RowMeta prev = new RowMeta();
    prev.addValueMeta(new ValueMetaString("geom"));
    prev.addValueMeta(new ValueMetaString("zs_mean"));

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, prev, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertTrue(remarks.getFirst().getText().contains("Row output field already exists: zs_mean"));
  }

  @Test
  void rowOutputMetadataRejectsFieldCollisions() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setOutputMode(GdalRasterZonalStatsMeta.OUTPUT_MODE_ROW_FIELDS);
    meta.setZonesInputMode(GdalRasterZonalStatsMeta.ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD);

    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaString("zs_mean"));

    Exception exception =
        assertThrows(Exception.class, () -> meta.getFields(rowMeta, "origin", null, null, null, null));

    assertTrue(exception.getMessage().contains("Row output field already exists: zs_mean"));
  }

  @Test
  void rowOutputMetadataRejectsReservedTechnicalFieldCollisions() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setOutputMode(GdalRasterZonalStatsMeta.OUTPUT_MODE_ROW_FIELDS);
    meta.setZonesInputMode(GdalRasterZonalStatsMeta.ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD);

    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaString("geom"));
    rowMeta.addValueMeta(new ValueMetaString("gdal_upstream_output"));

    Exception exception =
        assertThrows(Exception.class, () -> meta.getFields(rowMeta, "origin", null, null, null, null));

    assertTrue(exception.getMessage().contains("Row output field already exists: gdal_upstream_output"));
  }
}
