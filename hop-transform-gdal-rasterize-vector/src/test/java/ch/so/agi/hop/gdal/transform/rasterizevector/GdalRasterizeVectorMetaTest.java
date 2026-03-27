package ch.so.agi.hop.gdal.transform.rasterizevector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterizeVectorMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterizeVectorMeta meta = new GdalRasterizeVectorMeta();
    meta.setDefault();
    assertEquals("GTiff", meta.getOutputFormat());
    assertEquals("FAIL_IF_EXISTS", meta.getOutputWriteMode());
  }

  @Test
  void outputWriteModeNormalizesValues() {
    GdalRasterizeVectorMeta meta = new GdalRasterizeVectorMeta();
    meta.setDefault();
    meta.setOutputWriteMode("OVERWRITE");
    assertEquals("OVERWRITE", meta.getOutputWriteMode());
    meta.setOutputWriteMode("UPDATE");
    assertEquals("UPDATE", meta.getOutputWriteMode());
    meta.setOutputWriteMode("add");
    assertEquals("ADD", meta.getOutputWriteMode());
  }

  @Test
  void checkShouldRejectAppendWriteMode() {
    GdalRasterizeVectorMeta meta = new GdalRasterizeVectorMeta();
    meta.setDefault();
    meta.setBounds("0,0,1,1");
    meta.setResolutionX("1");
    meta.setResolutionY("1");
    meta.setOutputValue("/tmp/out.tif");
    meta.setInputValue("/tmp/in.gpkg");
    meta.setOutputWriteMode("APPEND");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals("Rasterize vector write mode is not supported: APPEND", remarks.getFirst().getText());
  }

  @Test
  void checkShouldRejectHttpUrlOutputMode() {
    GdalRasterizeVectorMeta meta = new GdalRasterizeVectorMeta();
    meta.setDefault();
    meta.setBounds("0,0,1,1");
    meta.setResolutionX("1");
    meta.setResolutionY("1");
    meta.setOutputValue("/tmp/out.tif");
    meta.setOutputSourceMode("HTTP_URL");
    meta.setInputValue("/tmp/in.gpkg");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals(
        "HTTP/HTTPS output is not supported; use LOCAL_FILE or GDAL_VSI",
        remarks.getFirst().getText());
  }
}
