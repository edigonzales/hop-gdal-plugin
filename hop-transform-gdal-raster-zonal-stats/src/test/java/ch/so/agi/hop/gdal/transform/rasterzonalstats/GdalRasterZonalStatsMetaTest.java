package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterZonalStatsMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();

    assertEquals("GPKG", meta.getOutputFormat());
    assertEquals("FAIL_IF_EXISTS", meta.getOutputWriteMode());
    assertEquals("mean", meta.getStats());
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
}
