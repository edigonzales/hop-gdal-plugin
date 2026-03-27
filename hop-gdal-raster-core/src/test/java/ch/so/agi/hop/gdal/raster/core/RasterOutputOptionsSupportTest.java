package ch.so.agi.hop.gdal.raster.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class RasterOutputOptionsSupportTest {
  @Test
  void normalizeConfiguredWriteModeCanonicalizesKnownValues() {
    assertEquals(
        RasterOutputOptionsSupport.WRITE_MODE_APPEND,
        RasterOutputOptionsSupport.normalizeConfiguredWriteMode("append"));
    assertEquals(
        RasterOutputOptionsSupport.WRITE_MODE_UPDATE,
        RasterOutputOptionsSupport.normalizeConfiguredWriteMode("update"));
  }

  @Test
  void addRasterAlgorithmWriteModeArgsMapsOverwriteAndAppend() {
    List<String> args = new ArrayList<>();
    RasterOutputOptionsSupport.addRasterAlgorithmWriteModeArgs(
        args, RasterOutputOptionsSupport.WRITE_MODE_OVERWRITE);
    RasterOutputOptionsSupport.addRasterAlgorithmWriteModeArgs(
        args, RasterOutputOptionsSupport.WRITE_MODE_APPEND);
    assertEquals(List.of("--overwrite", "--append"), args);
  }

  @Test
  void addVectorRasterizeWriteModeArgsMapsUpdateFamily() {
    List<String> args = new ArrayList<>();
    RasterOutputOptionsSupport.addVectorRasterizeWriteModeArgs(
        args, RasterOutputOptionsSupport.WRITE_MODE_UPDATE);
    RasterOutputOptionsSupport.addVectorRasterizeWriteModeArgs(
        args, RasterOutputOptionsSupport.WRITE_MODE_ADD);
    assertEquals(List.of("--update", "--add"), args);
  }

  @Test
  void applyCompressionPresetSkipsDefault() {
    LinkedHashMap<String, String> creationOptions = new LinkedHashMap<>();
    RasterOutputOptionsSupport.applyCompressionPreset(
        creationOptions, RasterOutputOptionsSupport.COMPRESSION_DEFAULT);
    assertTrue(creationOptions.isEmpty());
  }
}
