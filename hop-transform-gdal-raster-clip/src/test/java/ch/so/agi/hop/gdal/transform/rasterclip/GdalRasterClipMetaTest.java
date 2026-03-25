package ch.so.agi.hop.gdal.transform.rasterclip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GdalRasterClipMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    assertEquals("BOUNDING_BOX", meta.getClipMode());
  }
}
