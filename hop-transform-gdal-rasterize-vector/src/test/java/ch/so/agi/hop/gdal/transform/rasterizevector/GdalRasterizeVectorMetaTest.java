package ch.so.agi.hop.gdal.transform.rasterizevector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterizeVectorMetaTest {
  @Test
  void defaultsShouldMatchRasterizeUseCase() {
    GdalRasterizeVectorMeta meta = new GdalRasterizeVectorMeta();
    meta.setDefault();

    assertEquals("DATASET_LAYER", meta.getVectorInputMode());
    assertEquals("CONSTANT_VALUE", meta.getBurnStrategy());
    assertEquals("GTiff", meta.getOutputFormat());
    assertEquals("BOUNDS_RESOLUTION", meta.getGridMode());
    assertTrue(meta.isFailOnError());
    assertTrue(meta.isAddResultFields());
    assertFalse(meta.isAllTouched());
  }

  @Test
  void checkShouldRejectMissingGeometryFieldInHopMode() {
    GdalRasterizeVectorMeta meta = new GdalRasterizeVectorMeta();
    meta.setDefault();
    meta.setVectorInputMode("HOP_GEOMETRY_FIELD");
    meta.setOutputValue("/tmp/out.tif");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }
}
