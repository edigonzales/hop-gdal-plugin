package ch.so.agi.hop.gdal.transform.rasterclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterClipMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    assertEquals("BOUNDING_BOX", meta.getClipMode());
  }

  @Test
  void checkShouldRejectHttpUrlOutputMode() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
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
