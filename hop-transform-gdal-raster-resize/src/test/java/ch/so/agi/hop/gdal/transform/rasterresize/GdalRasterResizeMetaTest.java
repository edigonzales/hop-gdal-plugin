package ch.so.agi.hop.gdal.transform.rasterresize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GdalRasterResizeMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterResizeMeta meta = new GdalRasterResizeMeta();
    meta.setDefault();
    assertEquals("SIZE", meta.getSizingMode());
  }
}
