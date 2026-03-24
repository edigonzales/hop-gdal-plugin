package ch.so.agi.hop.gdal.transform.rasterconvert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GdalRasterConvertMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterConvertMeta meta = new GdalRasterConvertMeta();
    meta.setDefault();

    assertEquals("GTiff", meta.getOutputFormat());
    assertEquals("LOCAL_FILE", meta.getInputSourceMode());
    assertTrue(meta.isFailOnError());
  }
}
