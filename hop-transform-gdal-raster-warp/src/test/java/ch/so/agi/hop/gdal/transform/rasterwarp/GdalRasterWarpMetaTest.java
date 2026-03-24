package ch.so.agi.hop.gdal.transform.rasterwarp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GdalRasterWarpMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterWarpMeta meta = new GdalRasterWarpMeta();
    meta.setDefault();
    assertEquals("near", meta.getResamplingMethod());
  }
}
