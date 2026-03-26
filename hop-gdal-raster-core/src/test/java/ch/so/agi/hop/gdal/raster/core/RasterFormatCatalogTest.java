package ch.so.agi.hop.gdal.raster.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RasterFormatCatalogTest {
  @Test
  void shouldOrderCommonFormatsFirst() {
    List<String> ordered =
        RasterFormatCatalog.orderFormats(List.of("AAIGrid", "PNG", "GTiff", "COG", "Zarr", "JPEG"));

    assertEquals(List.of("GTiff", "COG", "PNG", "JPEG", "AAIGrid", "Zarr"), ordered);
  }

  @Test
  void shouldDeduplicateIgnoringCaseWhilePreservingFirstEntry() {
    List<String> ordered = RasterFormatCatalog.orderFormats(List.of("gtiff", "GTiff", "cog", "COG", "MEM"));

    assertEquals(List.of("gtiff", "cog", "MEM"), ordered);
  }
}
