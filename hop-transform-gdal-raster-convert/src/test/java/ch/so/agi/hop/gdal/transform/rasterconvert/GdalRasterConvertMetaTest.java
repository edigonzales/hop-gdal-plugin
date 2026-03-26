package ch.so.agi.hop.gdal.transform.rasterconvert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterConvertMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterConvertMeta meta = new GdalRasterConvertMeta();
    meta.setDefault();

    assertEquals("GTiff", meta.getOutputFormat());
    assertEquals("FAIL_IF_EXISTS", meta.getOutputWriteMode());
    assertEquals("LOCAL_FILE", meta.getInputSourceMode());
    assertTrue(meta.isFailOnError());
  }

  @Test
  void setOutputWriteModeNormalizesValues() {
    GdalRasterConvertMeta meta = new GdalRasterConvertMeta();
    meta.setDefault();

    meta.setOutputWriteMode("OVERWRITE");
    assertEquals("OVERWRITE", meta.getOutputWriteMode());

    meta.setOutputWriteMode("append");
    assertEquals("APPEND", meta.getOutputWriteMode());

    meta.setOutputWriteMode("unsupported");
    assertEquals("FAIL_IF_EXISTS", meta.getOutputWriteMode());
  }

  @Test
  void checkShouldRejectHttpUrlOutputMode() {
    GdalRasterConvertMeta meta = new GdalRasterConvertMeta();
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
