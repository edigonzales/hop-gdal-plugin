package ch.so.agi.hop.gdal.raster.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RasterTransformSupportTest {
  @Test
  void resolveOutputDatasetRefRejectsHttpUrl() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                RasterTransformSupport.resolveOutputDatasetRef(
                    "HTTP_URL",
                    "CONSTANT",
                    "https://example.com/out.tif",
                    null,
                    null,
                    null,
                    value -> value));

    assertEquals("HTTP/HTTPS output is not supported; use LOCAL_FILE or GDAL_VSI", ex.getMessage());
  }

  @Test
  void resolveOutputDatasetRefAllowsGdalVsi() {
    DatasetRef ref =
        RasterTransformSupport.resolveOutputDatasetRef(
            "GDAL_VSI", "CONSTANT", "/vsimem/out.tif", null, null, null, value -> value);

    assertEquals(DatasetRefType.GDAL_VSI, ref.type());
    assertEquals("/vsimem/out.tif", ref.value());
  }
}
