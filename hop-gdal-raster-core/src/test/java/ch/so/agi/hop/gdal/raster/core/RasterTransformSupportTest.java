package ch.so.agi.hop.gdal.raster.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

  @Test
  void resolveRequiredValueReadsFieldValues() {
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaString("clip_value"));

    String resolved =
        RasterTransformSupport.resolveRequiredValue(
            "FIELD",
            null,
            "clip_value",
            new Object[] {"1,2,3,4"},
            rowMeta,
            value -> value,
            "Clip parameter");

    assertEquals("1,2,3,4", resolved);
  }

  @Test
  void resolveLocalDirectoryGlobDatasetRefsSortsMatches(@TempDir Path tempDir) throws Exception {
    Files.writeString(tempDir.resolve("b.tif"), "b");
    Files.writeString(tempDir.resolve("a.tif"), "a");
    Files.writeString(tempDir.resolve("ignore.vrt"), "x");

    List<DatasetRef> refs =
        RasterTransformSupport.resolveLocalDirectoryGlobDatasetRefs(
            "CONSTANT", tempDir.toString(), null, "*.tif", null, null, value -> value);

    assertEquals(2, refs.size());
    assertEquals(tempDir.resolve("a.tif").toString(), refs.get(0).value());
    assertEquals(tempDir.resolve("b.tif").toString(), refs.get(1).value());
    assertEquals(DatasetRefType.LOCAL_FILE, refs.get(0).type());
  }

  @Test
  void resolveLocalDirectoryGlobDatasetRefsRejectsNoMatches(@TempDir Path tempDir) {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                RasterTransformSupport.resolveLocalDirectoryGlobDatasetRefs(
                    "CONSTANT", tempDir.toString(), null, "*.tif", null, null, value -> value));

    assertEquals(
        "No input rasters matched pattern '*.tif' in directory " + tempDir, ex.getMessage());
  }
}
