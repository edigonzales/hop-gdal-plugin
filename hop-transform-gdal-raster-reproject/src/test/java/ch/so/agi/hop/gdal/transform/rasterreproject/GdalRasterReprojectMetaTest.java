package ch.so.agi.hop.gdal.transform.rasterreproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterReprojectMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterReprojectMeta meta = new GdalRasterReprojectMeta();
    meta.setDefault();
    assertEquals("NONE", meta.getSizingMode());
    assertEquals("FAIL_IF_EXISTS", meta.getOutputWriteMode());
  }

  @Test
  void outputWriteModeNormalizesValues() {
    GdalRasterReprojectMeta meta = new GdalRasterReprojectMeta();
    meta.setDefault();
    meta.setOutputWriteMode("OVERWRITE");
    assertEquals("OVERWRITE", meta.getOutputWriteMode());
    meta.setOutputWriteMode("APPEND");
    assertEquals("APPEND", meta.getOutputWriteMode());
  }

  @Test
  void checkShouldRejectHttpUrlOutputMode() {
    GdalRasterReprojectMeta meta = new GdalRasterReprojectMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setOutputValue("/tmp/out.tif");
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
