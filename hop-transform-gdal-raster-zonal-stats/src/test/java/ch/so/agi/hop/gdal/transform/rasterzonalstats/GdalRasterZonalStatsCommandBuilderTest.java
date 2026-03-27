package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class GdalRasterZonalStatsCommandBuilderTest {
  @Test
  void autoIncludeFieldsUsesAllZoneAttributes() throws Exception {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/ndom.tif");
    meta.setZonesValue("/tmp/buildings.gpkg");
    meta.setOutputValue("/tmp/buildings_height.gpkg");

    GdalRasterZonalStatsCommandBuilder builder =
        new GdalRasterZonalStatsCommandBuilder(
            (zones, requestedLayerName, remoteAccess) ->
                new ZoneLayerMetadata("buildings", List.of("egid", "name")));

    GdalRasterZonalStatsCommandBuilder.BuildRequest request =
        builder.build(meta, new Object[0], null, value -> value);

    assertEquals("/tmp/ndom.tif", request.input().value());
    assertEquals("/tmp/buildings.gpkg", request.zones().value());
    assertEquals("/tmp/buildings_height.gpkg", request.output().value());
    assertIterableEquals(
        List.of(
            "--output-format",
            "GPKG",
            "--zones-layer",
            "buildings",
            "--output-layer",
            "stats",
            "--stat",
            "mean",
            "--pixels",
            "default",
            "--strategy",
            "feature",
            "--include-field",
            "egid",
            "--include-field",
            "name"),
        request.args());
  }

  @Test
  void explicitIncludeFieldsMustExistInSelectedLayer() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/ndom.tif");
    meta.setZonesValue("/tmp/buildings.gpkg");
    meta.setOutputValue("/tmp/buildings_height.gpkg");
    meta.setIncludeFields("egid,missing");

    GdalRasterZonalStatsCommandBuilder builder =
        new GdalRasterZonalStatsCommandBuilder(
            (zones, requestedLayerName, remoteAccess) ->
                new ZoneLayerMetadata("buildings", List.of("egid", "name")));

    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.build(meta, new Object[0], null, value -> value));

    assertEquals("Zone field was not found in layer 'buildings': missing", error.getMessage());
  }

  @Test
  void multiLayerZonesDatasetWithoutLayerFailsCleanly() {
    GdalRasterZonalStatsMeta meta = new GdalRasterZonalStatsMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/ndom.tif");
    meta.setZonesValue("/tmp/buildings.gpkg");
    meta.setOutputValue("/tmp/buildings_height.gpkg");

    GdalRasterZonalStatsCommandBuilder builder =
        new GdalRasterZonalStatsCommandBuilder(
            (DatasetRef zones, String requestedLayerName, RemoteAccessSpec remoteAccess) -> {
              throw new IllegalArgumentException(
                  "Zones layer is required because the dataset contains multiple layers: "
                      + zones.value());
            });

    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.build(meta, new Object[0], null, value -> value));

    assertEquals(
        "Zones layer is required because the dataset contains multiple layers: /tmp/buildings.gpkg",
        error.getMessage());
  }
}
