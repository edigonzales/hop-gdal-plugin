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
}
