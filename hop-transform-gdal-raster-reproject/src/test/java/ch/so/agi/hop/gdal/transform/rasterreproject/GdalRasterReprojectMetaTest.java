package ch.so.agi.hop.gdal.transform.rasterreproject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GdalRasterReprojectMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterReprojectMeta meta = new GdalRasterReprojectMeta();
    meta.setDefault();
    assertEquals("NONE", meta.getSizingMode());
  }
}
