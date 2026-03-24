package ch.so.agi.hop.gdal.transform.rasterinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterInfoMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterInfoMeta meta = new GdalRasterInfoMeta();
    meta.setDefault();

    assertEquals("LOCAL_FILE", meta.getInputSourceMode());
    assertEquals("CONSTANT", meta.getInputValueMode());
    assertTrue(meta.isFailOnError());
    assertTrue(meta.isAddResultFields());
  }

  @Test
  void checkRejectsMissingInput() {
    GdalRasterInfoMeta meta = new GdalRasterInfoMeta();
    meta.setDefault();
    List<ICheckResult> remarks = new ArrayList<>();

    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }
}
