package ch.so.agi.hop.gdal.transform.rasterbuildvrt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterBuildVrtMetaTest {
  @Test
  void defaultsShouldMatchRasterMosaicExpectations() {
    GdalRasterBuildVrtMeta meta = new GdalRasterBuildVrtMeta();
    meta.setDefault();

    assertEquals("LOCAL_FILE", meta.getInputInterpretationMode());
    assertEquals("CONSTANT", meta.getInputListValueMode());
    assertEquals("LOCAL_FILE", meta.getOutputSourceMode());
    assertEquals("AVERAGE", meta.getResolutionStrategy());
    assertTrue(meta.isFailOnError());
    assertTrue(meta.isAddResultFields());
    assertFalse(meta.isSeparateBands());
  }

  @Test
  void checkShouldRejectMissingInputList() {
    GdalRasterBuildVrtMeta meta = new GdalRasterBuildVrtMeta();
    meta.setDefault();
    meta.setOutputValue("/tmp/out.vrt");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }

  @Test
  void checkShouldRejectHttpUrlOutputMode() {
    GdalRasterBuildVrtMeta meta = new GdalRasterBuildVrtMeta();
    meta.setDefault();
    meta.setInputListValue("/tmp/a.tif");
    meta.setOutputValue("/tmp/out.vrt");
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
